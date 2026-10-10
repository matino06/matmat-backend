package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiChatRequestDTO;
import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.dto.AiUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AiConversation;
import com.example.EduSprint.entity.AiMessage;
import com.example.EduSprint.entity.AiPrompt;
import com.example.EduSprint.entity.ExplanationStep;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.AiConversationRepository;
import com.example.EduSprint.repository.AiMessageRepository;
import com.example.EduSprint.repository.AiPromptRepository;
import com.example.EduSprint.repository.ExplanationStepRepository;
import com.example.EduSprint.repository.TaskRepository;
import com.example.EduSprint.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final int MAX_IMAGES = 4;
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_HISTORY_MESSAGE_CHARS = 4000;
    private static final int MAX_QUOTE_CHARS = 6000;
    private static final int MAX_QUESTION_CHARS = 4000;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(24);
    // Dok model razmišlja nema delti; ping održava vezu i otkriva da je učenik zaustavio odgovor
    // (prekinutu vezu Spring primijeti tek pri sljedećem pisanju), pa se poziv modela prekida.
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);
    // ~8 MB dekodirano; base64 je 4/3 veći
    private static final int MAX_DATA_URI_BASE64_CHARS = 8 * 1024 * 1024 * 4 / 3 + 4;
    private static final Pattern IMAGE_DATA_URI = Pattern.compile("^data:image/[A-Za-z0-9.+-]+;base64,(.+)$",
            Pattern.DOTALL);
    // "Matematika A razina" → A
    private static final Pattern COURSE_LEVEL = Pattern.compile("\\b([AB]) razina\\b");
    private static final Pattern IMG_SRC = Pattern.compile("<img[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final String GENERIC_ERROR = "Došlo je do pogreške. Pokušaj ponovo.";
    // Model koji razmišlja može potrošiti cijeli max_tokens na razmišljanje i ne napisati ni riječ.
    private static final String NO_ANSWER_ERROR =
            "AI je predugo razmišljao i nije stigao odgovoriti. Pokušaj ponovo ili postavi jednostavnije pitanje.";

    static final String DEFAULT_BASE_PROMPT = """
            Ti si MatMat AI asistent, tutor koji pomaže učenicima u pripremi za državnu maturu.
            Odgovaraj na hrvatskom jeziku, jasno i jednostavno, i što kraće osim ako učenik ne traži više detalja.
            Ne daj samo konačni rezultat — vodi učenika korak po korak i objasni zašto se nešto radi.
            Drži se gradiva državne mature i oslanjaj se na službeno objašnjenje zadatka.
            Odgovor formatiraj u Markdownu: koristi **podebljano**, liste i naslove gdje ima smisla.
            Matematičke izraze piši u LaTeX-u: inline izraze unutar \\( i \\), a izdvojene jednadžbe unutar $$ $$.
            Ne koristi jednostruke znakove $ za matematičke izraze.""";

    private final OpenRouterClient openRouterClient;
    private final AiPromptRepository aiPromptRepository;
    private final AiConversationRepository aiConversationRepository;
    private final AiMessageRepository aiMessageRepository;
    private final TaskRepository taskRepository;
    private final ExplanationStepRepository explanationStepRepository;
    private final StorageService storageService;
    private final TransactionTemplate transactionTemplate;
    private final AiSettingsService aiSettingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Razgovori koji se ne spremaju (ispit / općenito) i dalje ulaze u dnevni limit.
    private final Map<Long, Deque<Instant>> unsavedQuestions = new ConcurrentHashMap<>();

    public AiChatService(@Qualifier("chatOpenRouterClient") OpenRouterClient openRouterClient,
                         AiPromptRepository aiPromptRepository,
                         AiConversationRepository aiConversationRepository,
                         AiMessageRepository aiMessageRepository,
                         TaskRepository taskRepository,
                         ExplanationStepRepository explanationStepRepository,
                         StorageService storageService,
                         TransactionTemplate transactionTemplate,
                         AiSettingsService aiSettingsService) {
        this.openRouterClient = openRouterClient;
        this.aiPromptRepository = aiPromptRepository;
        this.aiConversationRepository = aiConversationRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.taskRepository = taskRepository;
        this.explanationStepRepository = explanationStepRepository;
        this.storageService = storageService;
        this.transactionTemplate = transactionTemplate;
        this.aiSettingsService = aiSettingsService;
    }

    /**
     * Sinkrono pripremi razgovor (limit, kontekst, spremanje user poruke) pa vrati SSE stream odgovora.
     * Greške prije streama (429, 404, 400) idu kao obični HTTP statusi.
     */
    public Flux<ServerSentEvent<String>> chat(Account account, AiChatRequestDTO req) {
        PreparedChat prepared = transactionTemplate.execute(status -> prepare(account, req));
        return stream(prepared);
    }

    public AiUsageDTO usage(Account account) {
        int limit = aiSettingsService.current().chatDailyLimit();
        Deque<Instant> unsaved = unsavedQuestions.computeIfAbsent(account.getAccountId(), id -> new ArrayDeque<>());
        synchronized (unsaved) {
            return usage(account.getAccountId(), unsaved, limit);
        }
    }

    @Transactional
    public void rate(Account account, Long messageId, Short rating) {
        if (rating == null || (rating != 1 && rating != -1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating mora biti -1 ili 1");
        }
        AiMessage message = aiMessageRepository
                .findByMessageIdAndConversation_Account_AccountId(messageId, account.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!AiMessage.ROLE_ASSISTANT.equals(message.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ocijeniti se može samo odgovor asistenta");
        }
        message.setRating(rating);
        aiMessageRepository.save(message);
    }

    // ---------------------------------------------------------------- priprema

    private PreparedChat prepare(Account account, AiChatRequestDTO req) {
        String question = truncate(req.question() == null ? "" : req.question().strip(), MAX_QUESTION_CHARS);
        AiChatRequestDTO.Quote quote = req.quote();
        String quoteText = quote != null && quote.text() != null ? truncate(quote.text().strip(), MAX_QUOTE_CHARS) : "";
        if (question.isEmpty() && quoteText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pitanje je prazno");
        }
        if (question.isEmpty()) {
            question = "Objasni mi ovaj dio.";
        }

        AiSettingsDTO settings = aiSettingsService.current();
        boolean persisted = req.taskId() != null;
        Instant unsavedSlot = checkRateLimit(account.getAccountId(), persisted, settings.chatDailyLimit());

        Task task = null;
        AiConversation conversation = null;
        if (persisted) {
            if (req.conversationId() != null) {
                conversation = aiConversationRepository
                        .findByConversationIdAndAccount_AccountId(req.conversationId(), account.getAccountId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Razgovor ne postoji"));
                if (!conversation.getTask().getId().equals(req.taskId().intValue())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Razgovor ne pripada ovom zadatku");
                }
                task = conversation.getTask();
            } else {
                task = taskRepository.findById(req.taskId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zadatak ne postoji"));
                conversation = aiConversationRepository.save(
                        new AiConversation(account, task, account.getCurrentCourse()));
            }
        }

        String subject = account.getCurrentCourse() != null ? account.getCurrentCourse().getSubject() : null;
        AiPrompt basePrompt = aiPromptRepository.findFirstBySubjectIsNullAndIsActiveTrue().orElse(null);
        AiPrompt subjectPrompt = subject == null ? null
                : aiPromptRepository.findFirstBySubjectAndIsActiveTrue(subject).orElse(null);

        List<ExplanationStep> steps = task == null ? List.of() : explanationStepRepository.findByTaskOrderByStepNumberAsc(task);
        // Kod probne mature razina ovisi o ispitu, a ne o tečaju koji učenik trenutno ima.
        boolean exam = quote != null && "exam".equals(quote.source());
        String level = exam ? null : examLevel(account.getCurrentCourse() != null ? account.getCurrentCourse().getCourseName() : null);
        String systemPrompt = buildSystemPrompt(
                basePrompt != null ? basePrompt.getContent() : DEFAULT_BASE_PROMPT,
                subjectPrompt != null ? subjectPrompt.getContent() : null,
                level, task, steps, quote, req.solutionRevealed());

        // Slike: one koje je učenik označio + one iz zadatka.
        // Iz citata su dopušteni i data: URI-ji (npr. fotografija rukom pisanog odgovora s probne mature).
        Set<String> imageKeys = new LinkedHashSet<>();
        if (quote != null && quote.imageUrls() != null) {
            for (String url : quote.imageUrls()) {
                if (url != null && url.startsWith("data:image/")) {
                    imageKeys.add(url);
                } else {
                    addImageKey(imageKeys, url);
                }
            }
        }
        if (task != null) {
            extractImageSources(task.getTaskText()).forEach(url -> addImageKey(imageKeys, url));
            extractImageSources(task.getExplanation()).forEach(url -> addImageKey(imageKeys, url));
            for (ExplanationStep step : steps) {
                extractImageSources(step.getExplanation()).forEach(url -> addImageKey(imageKeys, url));
                addImageKey(imageKeys, step.getImageName());
            }
        }
        List<String> imageDataUris = loadImages(imageKeys);

        String userContent = buildUserContent(question, quote, quoteText);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", settings.chatModel());
        body.put("stream", true);
        body.put("max_tokens", settings.chatMaxTokens());
        body.putObject("usage").put("include", true);
        ObjectNode reasoning = body.putObject("reasoning").put("exclude", true);
        if (settings.chatReasoningEffort() != null) reasoning.put("effort", settings.chatReasoningEffort());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);

        if (persisted) {
            // Zaustavljen odgovor ostaje u povijesti (učenik ga je vidio); pitanje koje je ostalo
            // bez ijedne riječi odgovora ne ulazi, da model ne dobije dva pitanja zaredom.
            List<AiMessage> previous = aiMessageRepository.findByConversation_ConversationIdAndStatusInOrderByCreatedAtAsc(
                    conversation.getConversationId(), List.of(AiMessage.STATUS_OK, AiMessage.STATUS_ABORTED));
            for (AiMessage m : lastN(previous, MAX_HISTORY_MESSAGES)) {
                if (AiMessage.ROLE_USER.equals(m.getRole()) && !AiMessage.STATUS_OK.equals(m.getStatus())) continue;
                // Starije prazne poruke asistenta spremljene su kao "ok"; provideri prazan sadržaj mogu odbiti.
                if (m.getContent() == null || m.getContent().isBlank()) continue;
                messages.addObject().put("role", m.getRole()).put("content", m.getContent());
            }
        } else if (req.history() != null) {
            for (AiChatRequestDTO.HistoryMessage h : lastN(req.history(), MAX_HISTORY_MESSAGES)) {
                if (h == null || h.content() == null || h.content().isBlank()) continue;
                String role = AiMessage.ROLE_ASSISTANT.equals(h.role()) ? AiMessage.ROLE_ASSISTANT : AiMessage.ROLE_USER;
                messages.addObject().put("role", role).put("content", truncate(h.content(), MAX_HISTORY_MESSAGE_CHARS));
            }
        }

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode parts = userMessage.putArray("content");
        parts.addObject().put("type", "text").put("text", userContent + imageNote(imageDataUris.size(), quote));
        for (String uri : imageDataUris) {
            ObjectNode part = parts.addObject();
            part.put("type", "image_url");
            part.putObject("image_url").put("url", uri);
        }

        Long userMessageId = null;
        if (persisted) {
            userMessageId = aiMessageRepository.save(new AiMessage(conversation, AiMessage.ROLE_USER, userContent))
                    .getMessageId();
            conversation.setUpdatedAt(Instant.now());
            aiConversationRepository.save(conversation);
        }

        return new PreparedChat(
                body,
                settings.chatModel(),
                conversation != null ? conversation.getConversationId() : null,
                basePrompt != null ? basePrompt.getPromptId() : null,
                subjectPrompt != null ? subjectPrompt.getPromptId() : null,
                imageDataUris.size(),
                account.getAccountId(),
                userMessageId,
                unsavedSlot);
    }

    /**
     * Baci 429 ako je limit potrošen. Za razgovor koji se ne sprema zauzme mjesto u unsavedQuestions
     * i vrati ga, da se može vratiti ako AI ne odgovori.
     */
    private Instant checkRateLimit(Long accountId, boolean persisted, int dailyLimit) {
        Deque<Instant> unsaved = unsavedQuestions.computeIfAbsent(accountId, id -> new ArrayDeque<>());
        synchronized (unsaved) {
            if (usage(accountId, unsaved, dailyLimit).remaining() == 0) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Dosegnut je limit od " + dailyLimit + " pitanja u 24 sata.");
            }
            if (persisted) {
                return null;
            }
            Instant slot = Instant.now();
            unsaved.addLast(slot);
            return slot;
        }
    }

    /** Poziva se unutar synchronized (unsaved). Limit je klizni: pitanje se oslobađa 24 h nakon postavljanja. */
    private AiUsageDTO usage(Long accountId, Deque<Instant> unsaved, int limit) {
        Instant since = Instant.now().minus(RATE_LIMIT_WINDOW);
        while (!unsaved.isEmpty() && unsaved.peekFirst().isBefore(since)) {
            unsaved.pollFirst();
        }
        List<Instant> asked = new ArrayList<>(aiMessageRepository.findQuestionTimesSince(accountId, since));
        asked.addAll(unsaved);
        int remaining = Math.max(0, limit - asked.size());
        Instant resetAt = null;
        if (remaining == 0 && limit > 0) {
            Collections.sort(asked);
            // Ako je limit u međuvremenu snižen, mora isteći više pitanja prije nego se jedno oslobodi.
            resetAt = asked.get(asked.size() - limit).plus(RATE_LIMIT_WINDOW);
        }
        return new AiUsageDTO(limit, remaining, resetAt);
    }

    /** AI nije odgovorio: pitanje se vraća u limit. */
    private void refundUnsaved(PreparedChat prepared) {
        Deque<Instant> unsaved = unsavedQuestions.get(prepared.accountId());
        if (unsaved == null) return;
        synchronized (unsaved) {
            unsaved.remove(prepared.unsavedSlot());
        }
    }

    /** Razina mature iz naziva tečaja ("Matematika B razina" → "osnovna (B)"); null ako je tečaj nema. */
    static String examLevel(String courseName) {
        if (courseName == null) return null;
        Matcher m = COURSE_LEVEL.matcher(courseName);
        if (!m.find()) return null;
        return "A".equals(m.group(1)) ? "viša (A)" : "osnovna (B)";
    }

    /** Nazivi dijelova konteksta navedeni su u base promptu u bazi; mijenjaju se zajedno. */
    static String buildSystemPrompt(String basePrompt, String subjectPrompt, String level, Task task,
                                    List<ExplanationStep> steps, AiChatRequestDTO.Quote quote, Boolean solutionRevealed) {
        StringBuilder sb = new StringBuilder(basePrompt);
        if (subjectPrompt != null) {
            sb.append("\n\n").append(subjectPrompt);
        }
        sb.append("\n\nTekst zadatka i objašnjenja zapisan je u LaTeX-u (MathJax) i može sadržavati HTML oznake.");
        if (level != null) {
            sb.append("\n\nRAZINA: ").append(level);
        }
        if (task != null) {
            if (task.getObjective() != null) {
                sb.append("\n\nCILJ UČENJA: ").append(task.getObjective().getObjectiveName());
            }
            sb.append("\n\nZADATAK:\n").append(nullSafe(task.getTaskText()));
            if (task.getExplanation() != null && !task.getExplanation().isBlank()) {
                sb.append("\n\nOBJAŠNJENJE:\n").append(task.getExplanation());
            }
            if (!steps.isEmpty()) {
                sb.append("\n\nKORACI OBJAŠNJENJA:");
                for (ExplanationStep step : steps) {
                    sb.append("\n").append(step.getStepNumber()).append(". ").append(nullSafe(step.getExplanation()));
                }
            }
            if (solutionRevealed != null) {
                sb.append("\n\nUČENIK JE OTVORIO RJEŠENJE: ").append(solutionRevealed ? "da" : "ne");
            }
        } else if (quote != null && "exam".equals(quote.source())) {
            sb.append("\n\nUčenik pita o pitanju s probne državne mature koju je već riješio i koja je ocijenjena. ")
              .append("U njegovoj poruci je pitanje, njegov odgovor, službeno rješenje i komentar ocjenjivača.");
        } else {
            sb.append("\n\nUčenik trenutno ne rješava konkretan zadatak — odgovaraj na općenita pitanja iz gradiva mature.");
        }
        return sb.toString();
    }

    static String buildUserContent(String question, AiChatRequestDTO.Quote quote, String quoteText) {
        List<String> imageLabels = quotedImageLabels(quote);
        if (quoteText.isEmpty() && imageLabels.isEmpty()) {
            return question;
        }
        String source = quote.source() == null ? "" : quote.source();
        StringBuilder sb = new StringBuilder();
        if (!quoteText.isEmpty()) {
            String label = switch (source) {
                case "solution" -> "Označio sam ovaj dio rješenja:";
                case "exam" -> "Pitanje s probne mature (s mojim odgovorom, rješenjem i komentarom ocjenjivača):";
                default -> "Označio sam ovaj dio zadatka:";
            };
            sb.append(label).append("\n");
            for (String line : quoteText.split("\n", -1)) {
                sb.append("> ").append(line).append("\n");
            }
        }
        // Naziv označene slike ostaje u spremljenoj poruci, pa model u sljedećim pitanjima zna o kojoj je slici riječ.
        String where = switch (source) {
            case "solution" -> "rješenja";
            case "exam" -> "pitanja s mature";
            default -> "zadatka";
        };
        for (String image : imageLabels) {
            sb.append("Označio sam sliku iz ").append(where).append(": ").append(image).append("\n");
        }
        return sb.append("\n").append(question).toString();
    }

    private static List<String> quotedImageLabels(AiChatRequestDTO.Quote quote) {
        List<String> labels = new ArrayList<>();
        if (quote == null || quote.imageUrls() == null) return labels;
        for (String url : quote.imageUrls()) {
            if (url == null || url.isBlank()) continue;
            if (url.startsWith("data:")) {
                labels.add("exam".equals(quote.source()) ? "fotografija mog rukom pisanog odgovora" : "priložena fotografija");
                continue;
            }
            String key = toStorageKey(url);
            if (key != null) {
                labels.add(key.substring(key.lastIndexOf('/') + 1));
            }
        }
        return labels;
    }

    private static String imageNote(int count, AiChatRequestDTO.Quote quote) {
        if (count == 0) {
            return "";
        }
        boolean exam = quote != null && "exam".equals(quote.source());
        return exam
                ? "\n\n(Priložene slike su iz ovog pitanja s mature — slika uz zadatak, moj rukom pisani odgovor i/ili službeno rješenje. Pogledaj ih izravno.)"
                : "\n\n(Priložene slike su slike iz zadatka/rješenja na koje se odnose <img> oznake. Pogledaj ih izravno umjesto da opisuješ URL.)";
    }

    // ---------------------------------------------------------------- slike

    static List<String> extractImageSources(String html) {
        List<String> result = new ArrayList<>();
        if (html == null) return result;
        Matcher m = IMG_SRC.matcher(html);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    /**
     * Prihvaća samo slike iz našeg storagea: URL-ove koji idu preko ImageControllera ("/image/...")
     * ili gola imena datoteka. Proizvoljni vanjski URL-ovi se ignoriraju.
     */
    static String toStorageKey(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.strip();
        if (u.startsWith("data:")) return null;
        int idx = u.indexOf("/image/");
        String key;
        if (idx >= 0) {
            key = u.substring(idx + "/image/".length());
        } else if (!u.contains("://") && !u.startsWith("//")) {
            key = u.startsWith("/") ? u.substring(1) : u;
        } else {
            return null;
        }
        int q = key.indexOf('?');
        if (q >= 0) key = key.substring(0, q);
        int h = key.indexOf('#');
        if (h >= 0) key = key.substring(0, h);
        key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        if (key.isBlank() || key.contains("..")) return null;
        return key;
    }

    private static void addImageKey(Set<String> keys, String url) {
        String key = toStorageKey(url);
        if (key != null) keys.add(key);
    }

    /** Vraća bajtove slike iz data:image/...;base64 URI-ja, ili null ako nije ispravan ili je prevelik. */
    static byte[] decodeImageDataUri(String uri) {
        if (uri == null || uri.length() > MAX_DATA_URI_BASE64_CHARS + 100) return null;
        Matcher m = IMAGE_DATA_URI.matcher(uri);
        if (!m.matches()) return null;
        try {
            // strogi dekoder; samo prijelomi redaka/razmaci se uklanjaju
            byte[] bytes = Base64.getDecoder().decode(m.group(1).replaceAll("\\s", ""));
            return bytes.length == 0 ? null : bytes;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> loadImages(Set<String> sources) {
        List<String> uris = new ArrayList<>();
        for (String source : sources) {
            if (uris.size() >= MAX_IMAGES) break;
            boolean dataUri = source.startsWith("data:");
            String label = dataUri ? source.substring(0, Math.min(source.indexOf(',') + 1, 40)) + "… (" + source.length() + " chars)" : source;
            try {
                byte[] bytes;
                if (dataUri) {
                    bytes = decodeImageDataUri(source);
                    if (bytes == null) {
                        throw new IllegalArgumentException("invalid or too large data URI");
                    }
                } else {
                    try (InputStream in = storageService.loadAsResource(source).getInputStream()) {
                        bytes = in.readAllBytes();
                    }
                }
                uris.add(ImageNormalizer.toJpegDataUri(bytes));
            } catch (Exception e) {
                log.warn("AI chat: image '{}' could not be attached: {}", label, e.getMessage());
            }
        }
        return uris;
    }

    // ---------------------------------------------------------------- stream

    private Flux<ServerSentEvent<String>> stream(PreparedChat prepared) {
        long startNanos = System.nanoTime();
        StringBuilder full = new StringBuilder();
        AtomicReference<JsonNode> usage = new AtomicReference<>();
        AtomicReference<String> respondedModel = new AtomicReference<>(prepared.model());
        AtomicBoolean finished = new AtomicBoolean(false);
        // finish_reason "length": odgovor je udario u max_tokens i prekinut je.
        AtomicBoolean truncated = new AtomicBoolean(false);

        ObjectNode meta = objectMapper.createObjectNode();
        meta.putPOJO("conversationId", prepared.conversationId());
        meta.put("imagesAttached", prepared.imagesAttached());

        Flux<ServerSentEvent<String>> deltas = openRouterClient.stream(prepared.body())
                .doOnNext(chunk -> {
                    if (chunk.has("error")) {
                        throw new RuntimeException("OpenRouter stream error: " + chunk.path("error"));
                    }
                    JsonNode u = chunk.path("usage");
                    if (u.isObject()) usage.set(u);
                    if (chunk.hasNonNull("model")) respondedModel.set(chunk.get("model").asText());
                    if ("length".equals(chunk.path("choices").path(0).path("finish_reason").asText(null))) {
                        truncated.set(true);
                    }
                })
                .map(chunk -> chunk.path("choices").path(0).path("delta").path("content").asText(""))
                .filter(text -> !text.isEmpty())
                .doOnNext(full::append)
                .map(text -> event("delta", objectMapper.createObjectNode().put("text", text)));

        Mono<ServerSentEvent<String>> done = Mono.fromCallable(() -> {
                    finished.set(true);
                    if (full.toString().isBlank()) {
                        // Nijedna riječ odgovora: greška, ne "ok", da prazna poruka ne uđe u povijest razgovora.
                        saveAssistant(prepared, full.toString(), AiMessage.STATUS_ERROR, usage.get(), respondedModel.get(), startNanos);
                        settleQuestion(prepared, AiMessage.STATUS_ERROR);
                        return event("error", objectMapper.createObjectNode()
                                .put("message", truncated.get() ? NO_ANSWER_ERROR : GENERIC_ERROR));
                    }
                    Long messageId = saveAssistant(prepared, full.toString(), AiMessage.STATUS_OK, usage.get(), respondedModel.get(), startNanos);
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.putPOJO("messageId", messageId);
                    payload.put("truncated", truncated.get());
                    return event("done", payload);
                })
                .subscribeOn(Schedulers.boundedElastic());

        Flux<ServerSentEvent<String>> answer = Flux.just(event("meta", meta))
                .concatWith(deltas)
                .concatWith(done)
                .onErrorResume(e -> Mono.fromCallable(() -> {
                            log.error("AI chat failed", e);
                            if (finished.compareAndSet(false, true)) {
                                saveAssistant(prepared, full.toString(), AiMessage.STATUS_ERROR, usage.get(), respondedModel.get(), startNanos);
                                settleQuestion(prepared, AiMessage.STATUS_ERROR);
                            }
                            return event("error", objectMapper.createObjectNode().put("message", GENERIC_ERROR));
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnCancel(() -> {
                    if (finished.compareAndSet(false, true)) {
                        Schedulers.boundedElastic().schedule(() -> {
                            saveAssistant(prepared, full.toString(), AiMessage.STATUS_ABORTED, usage.get(), respondedModel.get(), startNanos);
                            // Zaustavljeno prije ijedne riječi: pitanje ne ulazi u povijest, ali se broji u limit.
                            if (full.toString().isBlank()) settleQuestion(prepared, AiMessage.STATUS_ABORTED);
                        });
                    }
                });

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL)
                .map(i -> ServerSentEvent.<String>builder().comment("ping").build());
        // done i error su uvijek zadnji događaj; finished je tada već postavljen, pa otkazivanje
        // koje takeUntil pošalje odgovoru ne sprema ništa.
        return Flux.merge(answer, heartbeat)
                .takeUntil(e -> "done".equals(e.event()) || "error".equals(e.event()));
    }

    /**
     * Pitanje na koje AI nije odgovorio: kod greške se ne broji u limit, a u oba slučaja
     * ne ulazi u povijest razgovora (ona uzima samo "ok" pitanja).
     */
    private void settleQuestion(PreparedChat prepared, String status) {
        if (AiMessage.STATUS_ERROR.equals(status) && prepared.unsavedSlot() != null) {
            refundUnsaved(prepared);
        }
        if (prepared.userMessageId() == null) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(tx -> aiMessageRepository.findById(prepared.userMessageId())
                    .ifPresent(m -> m.setStatus(status)));
        } catch (Exception e) {
            log.error("AI chat: failed to mark question {} as {}", prepared.userMessageId(), status, e);
        }
    }

    private Long saveAssistant(PreparedChat prepared, String content, String status, JsonNode usage,
                               String respondedModel, long startNanos) {
        if (prepared.conversationId() == null) {
            return null;
        }
        try {
            return transactionTemplate.execute(tx -> {
                AiConversation conversation = aiConversationRepository.getReferenceById(prepared.conversationId());
                AiMessage message = new AiMessage(conversation, AiMessage.ROLE_ASSISTANT, content == null ? "" : content);
                message.setStatus(status);
                message.setModel(respondedModel);
                if (prepared.basePromptId() != null) {
                    message.setBasePrompt(aiPromptRepository.getReferenceById(prepared.basePromptId()));
                }
                if (prepared.subjectPromptId() != null) {
                    message.setSubjectPrompt(aiPromptRepository.getReferenceById(prepared.subjectPromptId()));
                }
                if (usage != null) {
                    if (usage.hasNonNull("prompt_tokens")) message.setInputTokens(usage.get("prompt_tokens").asInt());
                    if (usage.hasNonNull("completion_tokens")) message.setOutputTokens(usage.get("completion_tokens").asInt());
                    if (usage.hasNonNull("cost")) message.setCostUsd(usage.get("cost").decimalValue().setScale(6, RoundingMode.HALF_UP));
                }
                message.setLatencyMs((int) ((System.nanoTime() - startNanos) / 1_000_000));
                AiMessage saved = aiMessageRepository.save(message);

                AiConversation managed = aiConversationRepository.findById(prepared.conversationId()).orElseThrow();
                managed.setUpdatedAt(Instant.now());
                return saved.getMessageId();
            });
        } catch (Exception e) {
            log.error("AI chat: failed to save assistant message for conversation {}", prepared.conversationId(), e);
            return null;
        }
    }

    // ---------------------------------------------------------------- util

    private ServerSentEvent<String> event(String name, JsonNode data) {
        return ServerSentEvent.<String>builder().event(name).data(data.toString()).build();
    }

    private static <T> List<T> lastN(List<T> list, int n) {
        return list.size() <= n ? list : list.subList(list.size() - n, list.size());
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private record PreparedChat(ObjectNode body, String model, Long conversationId, Long basePromptId, Long subjectPromptId,
                                int imagesAttached, Long accountId, Long userMessageId, Instant unsavedSlot) {
    }
}
