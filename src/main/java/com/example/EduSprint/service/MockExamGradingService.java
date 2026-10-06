package com.example.EduSprint.service;

import com.example.EduSprint.dto.MockExamAnswerResultDTO;
import com.example.EduSprint.dto.MockExamAnswerSubmissionDTO;
import com.example.EduSprint.dto.MockExamAttemptDetailDTO;
import com.example.EduSprint.dto.MockExamAttemptSummaryDTO;
import com.example.EduSprint.dto.MockExamCriterionScoreDTO;
import com.example.EduSprint.dto.MockExamQuestionImageDTO;
import com.example.EduSprint.dto.MockExamSubmitRequestDTO;
import com.example.EduSprint.dto.MockExamSubmitResponseDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.MockExam;
import com.example.EduSprint.entity.MockExamAnswer;
import com.example.EduSprint.entity.MockExamAttempt;
import com.example.EduSprint.entity.MockExamCriterionScore;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamQuestionImage;
import com.example.EduSprint.entity.MockExamScoringCriterion;
import com.example.EduSprint.repository.MockExamAnswerRepository;
import com.example.EduSprint.repository.MockExamAttemptRepository;
import com.example.EduSprint.repository.MockExamCriterionScoreRepository;
import com.example.EduSprint.repository.MockExamQuestionRepository;
import com.example.EduSprint.repository.MockExamRepository;
import com.example.EduSprint.repository.MockExamScoringCriterionRepository;
import com.example.EduSprint.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

@Service
public class MockExamGradingService {

    private static final String STATUS_NOT_REQUIRED = "not_needed";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_DONE = "graded";
    private static final String STATUS_FAILED = "error";

    private static final String TYPE_MULTIPLE_CHOICE = "multiple_choice";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String TYPE_SHORT_ANSWER_GRAPH = "short_answer_graph";
    private static final String TYPE_EXTENDED_ANSWER = "extended_answer";

    private static final String ANSWER_KEY_PREFIX = "mock-exam-answers/";

    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final MockExamAttemptRepository mockExamAttemptRepository;
    private final MockExamAnswerRepository mockExamAnswerRepository;
    private final MockExamScoringCriterionRepository mockExamScoringCriterionRepository;
    private final MockExamCriterionScoreRepository mockExamCriterionScoreRepository;
    private final StorageService storageService;
    private final AiGradingService aiGradingService;
    private final MockExamGradingService self;

    public MockExamGradingService(MockExamRepository mockExamRepository,
                                  MockExamQuestionRepository mockExamQuestionRepository,
                                  MockExamAttemptRepository mockExamAttemptRepository,
                                  MockExamAnswerRepository mockExamAnswerRepository,
                                  MockExamScoringCriterionRepository mockExamScoringCriterionRepository,
                                  MockExamCriterionScoreRepository mockExamCriterionScoreRepository,
                                  StorageService storageService,
                                  AiGradingService aiGradingService,
                                  @Lazy MockExamGradingService self) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.mockExamAttemptRepository = mockExamAttemptRepository;
        this.mockExamAnswerRepository = mockExamAnswerRepository;
        this.mockExamScoringCriterionRepository = mockExamScoringCriterionRepository;
        this.mockExamCriterionScoreRepository = mockExamCriterionScoreRepository;
        this.storageService = storageService;
        this.aiGradingService = aiGradingService;
        this.self = self;
    }

    @Transactional
    public MockExamSubmitResponseDTO submit(Account account,
                                            Long examId,
                                            MockExamSubmitRequestDTO request,
                                            Map<String, MultipartFile> filesByPartName) {
        MockExam exam = mockExamRepository.findById(examId)
                .filter(e -> Boolean.TRUE.equals(e.getIsPublished()))
                .orElseThrow(() -> new EntityNotFoundException("Mock exam not found: " + examId));

        List<MockExamQuestion> allQuestions = mockExamQuestionRepository
                .findByExam_ExamIdOrderBySortOrderAsc(examId);
        Map<Long, MockExamQuestion> questionsById = new HashMap<>();
        short maxScore = 0;
        for (MockExamQuestion q : allQuestions) {
            questionsById.put(q.getQuestionId(), q);
            if (q.getQuestionType() != null) {
                maxScore += q.getPoints();
            }
        }

        MockExamAttempt attempt = new MockExamAttempt();
        attempt.setAccount(account);
        attempt.setExam(exam);
        Instant now = Instant.now();
        attempt.setStartedAt(now);
        attempt.setSubmittedAt(now);
        attempt.setIsCompleted(true);
        attempt.setMaxScore(maxScore);
        attempt.setTotalScore((short) 0);
        attempt = mockExamAttemptRepository.save(attempt);

        int runningTotal = 0;
        boolean anyPending = false;
        List<MockExamAnswerSubmissionDTO> submissions = request.getAnswers() == null
                ? List.of() : request.getAnswers();

        for (MockExamAnswerSubmissionDTO sub : submissions) {
            MockExamQuestion question = questionsById.get(sub.getQuestionId());
            if (question == null || question.getQuestionType() == null) {
                continue;
            }

            MockExamAnswer answer = new MockExamAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setAnsweredAt(now);
            answer.setScoreAwarded((short) 0);

            String type = question.getQuestionType();
            switch (type) {
                case TYPE_MULTIPLE_CHOICE -> {
                    answer.setSelectedOption(sub.getSelectedOption());
                    boolean correct = sub.getSelectedOption() != null
                            && sub.getSelectedOption().equalsIgnoreCase(question.getCorrectOption());
                    answer.setIsCorrect(correct);
                    Short awarded = correct ? question.getPoints() : (short) 0;
                    answer.setScoreAwarded(awarded);
                    answer.setAiGradingStatus(STATUS_NOT_REQUIRED);
                    runningTotal += awarded;
                }
                case TYPE_SHORT_ANSWER -> {
                    answer.setAnswerText(sub.getAnswerText());
                    answer.setAiGradingStatus(STATUS_PENDING);
                    anyPending = true;
                }
                case TYPE_SHORT_ANSWER_GRAPH, TYPE_EXTENDED_ANSWER -> {
                    String partName = sub.getImagePartName();
                    MultipartFile file = partName == null ? null : filesByPartName.get(partName);
                    if (file != null && !file.isEmpty()) {
                        String key = ANSWER_KEY_PREFIX + attempt.getAttemptId() + "/"
                                + question.getQuestionId() + extensionFromMime(file.getContentType(), file.getOriginalFilename());
                        storageService.store(file, key);
                        answer.setAnswerImageFilename(key);
                    }
                    answer.setAiGradingStatus(STATUS_PENDING);
                    anyPending = true;
                }
                default -> {
                    continue;
                }
            }

            mockExamAnswerRepository.save(answer);
        }

        attempt.setTotalScore((short) runningTotal);
        mockExamAttemptRepository.save(attempt);

        return new MockExamSubmitResponseDTO(
                attempt.getAttemptId(),
                anyPending ? STATUS_PENDING : STATUS_DONE
        );
    }

    public void triggerAsyncGradingForAttempt(Long attemptId) {
        List<MockExamAnswer> pending = mockExamAnswerRepository.findByAttempt_AttemptId(attemptId).stream()
                .filter(a -> STATUS_PENDING.equals(a.getAiGradingStatus()))
                .toList();
        for (MockExamAnswer a : pending) {
            self.gradeAnswerAsync(a.getAnswerId());
        }
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void gradeAnswerAsync(Long answerId) {
        MockExamAnswer answer = mockExamAnswerRepository.findById(answerId).orElse(null);
        if (answer == null || !STATUS_PENDING.equals(answer.getAiGradingStatus())) {
            return;
        }
        MockExamQuestion question = answer.getQuestion();
        String parentText = question.getParent() != null ? question.getParent().getQuestionText() : null;
        try {
            String type = question.getQuestionType();
            switch (type) {
                case TYPE_SHORT_ANSWER -> {
                    AiGradingService.AiGradeResult r = callWithRetry(() ->
                            aiGradingService.gradeShortAnswer(question, answer.getAnswerText(), parentText));
                    answer.setScoreAwarded(clamp(r.score(), question.getPoints()));
                    answer.setAiFeedback(r.feedback());
                    answer.setIsCorrect(r.isCorrect());
                    answer.setAiGradingStatus(STATUS_DONE);
                }
                case TYPE_SHORT_ANSWER_GRAPH -> {
                    byte[] bytes = readAnswerImage(answer);
                    String mime = guessMime(answer.getAnswerImageFilename());
                    AiGradingService.AiGradeResult r = callWithRetry(() ->
                            aiGradingService.gradeImageAnswer(question, bytes, mime, parentText));
                    answer.setScoreAwarded(clamp(r.score(), question.getPoints()));
                    answer.setAiFeedback(r.feedback());
                    answer.setIsCorrect(r.isCorrect());
                    answer.setAiGradingStatus(STATUS_DONE);
                }
                case TYPE_EXTENDED_ANSWER -> {
                    List<MockExamScoringCriterion> criteria = mockExamScoringCriterionRepository
                            .findByQuestion_QuestionIdOrderByCriterionOrderAsc(question.getQuestionId());
                    byte[] bytes = readAnswerImage(answer);
                    String mime = guessMime(answer.getAnswerImageFilename());
                    AiGradingService.ExtendedAiGradeResult r = callWithRetry(() ->
                            aiGradingService.gradeExtendedAnswer(question, criteria, bytes, mime, parentText));

                    int total = 0;
                    Map<Long, MockExamScoringCriterion> byId = new HashMap<>();
                    criteria.forEach(c -> byId.put(c.getCriterionId(), c));
                    for (AiGradingService.CriterionGrade cg : r.criterionScores()) {
                        MockExamScoringCriterion crit = byId.get(cg.criterionId());
                        if (crit == null) continue;
                        MockExamCriterionScore cs = new MockExamCriterionScore();
                        cs.setAnswer(answer);
                        cs.setCriterion(crit);
                        cs.setPointsAwarded(cg.pointsAwarded());
                        cs.setAiFeedback(cg.feedback());
                        mockExamCriterionScoreRepository.save(cs);
                        total += cg.pointsAwarded();
                    }
                    answer.setScoreAwarded(clamp((short) total, question.getPoints()));
                    answer.setAiFeedback(r.overallFeedback());
                    answer.setIsCorrect(answer.getScoreAwarded() != null
                            && answer.getScoreAwarded().equals(question.getPoints()));
                    answer.setAiGradingStatus(STATUS_DONE);
                }
                default -> answer.setAiGradingStatus(STATUS_DONE);
            }
        } catch (Exception e) {
            answer.setAiGradingStatus(STATUS_FAILED);
            String msg = e.getMessage();
            answer.setAiFeedback(msg != null ? truncate(msg, 1000) : "AI grading failed");
        }
        mockExamAnswerRepository.save(answer);
        recalculateAttemptTotal(answer.getAttempt().getAttemptId());
    }

    private void recalculateAttemptTotal(Long attemptId) {
        List<MockExamAnswer> all = mockExamAnswerRepository.findByAttempt_AttemptId(attemptId);
        int total = 0;
        for (MockExamAnswer a : all) {
            if (a.getScoreAwarded() != null) {
                total += a.getScoreAwarded();
            }
        }
        MockExamAttempt attempt = mockExamAttemptRepository.findById(attemptId).orElse(null);
        if (attempt != null) {
            attempt.setTotalScore((short) total);
            mockExamAttemptRepository.save(attempt);
        }
    }

    @Transactional
    public void retryFailedAnswers(Long attemptId, Account account) {
        mockExamAttemptRepository
                .findByAttemptIdAndAccount_AccountId(attemptId, account.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found: " + attemptId));

        List<MockExamAnswer> failed = mockExamAnswerRepository.findByAttempt_AttemptId(attemptId).stream()
                .filter(a -> STATUS_FAILED.equals(a.getAiGradingStatus()))
                .toList();

        for (MockExamAnswer a : failed) {
            a.setAiGradingStatus(STATUS_PENDING);
            a.setAiFeedback(null);
            mockExamAnswerRepository.save(a);
        }
        // Trigger se poziva iz controllera NAKON što ova transakcija commitá
    }

    @Transactional(readOnly = true)
    public List<MockExamAttemptSummaryDTO> getAttemptsForAccount(Account account) {
        return mockExamAttemptRepository
                .findByAccount_AccountIdOrderBySubmittedAtDesc(account.getAccountId())
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MockExamAttemptDetailDTO getAttemptDetail(Long attemptId, Account account) {
        MockExamAttempt attempt = mockExamAttemptRepository
                .findByAttemptIdAndAccount_AccountId(attemptId, account.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found: " + attemptId));

        MockExam exam = attempt.getExam();

        List<MockExamQuestion> allQuestions = mockExamQuestionRepository
                .findByExam_ExamIdOrderBySortOrderAsc(exam.getExamId());

        Map<Long, MockExamAnswer> answersByQuestionId = new HashMap<>();
        mockExamAnswerRepository.findByAttempt_AttemptId(attemptId)
                .forEach(a -> answersByQuestionId.put(a.getQuestion().getQuestionId(), a));

        List<MockExamAnswerResultDTO> answerDTOs = new ArrayList<>();
        for (MockExamQuestion q : allQuestions) {
            if (q.getQuestionType() == null) {
                answerDTOs.add(toContainerDTO(q));
                continue;
            }
            MockExamAnswer a = answersByQuestionId.get(q.getQuestionId());
            answerDTOs.add(toAnswerResultDTO(q, a));
        }

        return new MockExamAttemptDetailDTO(
                attempt.getAttemptId(),
                exam.getExamId(),
                exam.getTitle(),
                exam.getSubtitle(),
                exam.getYear(),
                exam.getTerm(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getTotalScore(),
                attempt.getMaxScore(),
                computeGradingStatus(attempt.getAttemptId()),
                answerDTOs
        );
    }

    private MockExamAttemptSummaryDTO toSummaryDTO(MockExamAttempt attempt) {
        MockExam exam = attempt.getExam();
        return new MockExamAttemptSummaryDTO(
                attempt.getAttemptId(),
                exam.getExamId(),
                exam.getTitle(),
                exam.getYear(),
                exam.getTerm(),
                attempt.getSubmittedAt(),
                attempt.getTotalScore(),
                attempt.getMaxScore(),
                computeGradingStatus(attempt.getAttemptId())
        );
    }

    private String computeGradingStatus(Long attemptId) {
        long pending = mockExamAnswerRepository
                .countByAttempt_AttemptIdAndAiGradingStatus(attemptId, STATUS_PENDING);
        if (pending > 0) return STATUS_PENDING;
        long failed = mockExamAnswerRepository
                .countByAttempt_AttemptIdAndAiGradingStatus(attemptId, STATUS_FAILED);
        if (failed > 0) return STATUS_FAILED;
        return STATUS_DONE;
    }

    private MockExamAnswerResultDTO toContainerDTO(MockExamQuestion q) {
        List<MockExamQuestionImageDTO> questionImages = extractQuestionImages(q);
        return new MockExamAnswerResultDTO(
                q.getQuestionId(), q.getQuestionNumber(), null,
                q.getQuestionText(), (short) 0,
                questionImages,
                null, null, null, null, null, null,
                null, null, List.of(),
                null, (short) 0, null,
                null, null, null,
                List.of()
        );
    }

    private static final String IMAGE_CONTEXT_ANSWER = "answer";

    private List<MockExamQuestionImageDTO> extractQuestionImages(MockExamQuestion q) {
        if (q.getImages() == null) return List.of();
        return q.getImages().stream()
                .filter(img -> !IMAGE_CONTEXT_ANSWER.equals(img.getImageContext()))
                .sorted(java.util.Comparator.comparing(MockExamQuestionImage::getSortOrder))
                .map(img -> new MockExamQuestionImageDTO(
                        img.getImageUrl(), img.getAltText(), img.getImageContext(), img.getSortOrder()))
                .toList();
    }

    private List<MockExamQuestionImageDTO> extractCorrectAnswerImages(MockExamQuestion q) {
        if (q.getImages() == null) return List.of();
        return q.getImages().stream()
                .filter(img -> IMAGE_CONTEXT_ANSWER.equals(img.getImageContext()))
                .sorted(java.util.Comparator.comparing(MockExamQuestionImage::getSortOrder))
                .map(img -> new MockExamQuestionImageDTO(
                        img.getImageUrl(), img.getAltText(), img.getImageContext(), img.getSortOrder()))
                .toList();
    }

    private MockExamAnswerResultDTO toAnswerResultDTO(MockExamQuestion q, MockExamAnswer a) {
        List<MockExamQuestionImageDTO> questionImages = extractQuestionImages(q);
        List<MockExamQuestionImageDTO> correctAnswerImages = extractCorrectAnswerImages(q);

        if (a == null) {
            return new MockExamAnswerResultDTO(
                    q.getQuestionId(),
                    q.getQuestionNumber(),
                    q.getQuestionType(),
                    q.getQuestionText(),
                    q.getPoints(),
                    questionImages,
                    null, q.getCorrectOption(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                    null, q.getCorrectAnswer(),
                    correctAnswerImages,
                    null, (short) 0, false,
                    STATUS_NOT_REQUIRED, null,
                    q.getSolutionExplanation(),
                    List.of()
            );
        }

        List<MockExamCriterionScore> criterionScores = mockExamCriterionScoreRepository
                .findByAnswer_AnswerId(a.getAnswerId());
        List<MockExamCriterionScoreDTO> criterionDTOs = criterionScores.stream()
                .map(cs -> new MockExamCriterionScoreDTO(
                        cs.getCriterion().getCriterionId(),
                        cs.getCriterion().getDescription(),
                        cs.getCriterion().getPoints(),
                        cs.getPointsAwarded(),
                        cs.getAiFeedback()))
                .toList();

        String imageUrl = null;
        if (a.getAnswerImageFilename() != null) {
            try {
                Resource res = storageService.loadAsResource(a.getAnswerImageFilename());
                imageUrl = res != null && res.getURL() != null ? res.getURL().toString() : null;
            } catch (Exception ignored) {
            }
        }

        return new MockExamAnswerResultDTO(
                q.getQuestionId(),
                q.getQuestionNumber(),
                q.getQuestionType(),
                q.getQuestionText(),
                q.getPoints(),
                questionImages,
                a.getSelectedOption(),
                q.getCorrectOption(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                a.getAnswerText(),
                q.getCorrectAnswer(),
                correctAnswerImages,
                imageUrl,
                a.getScoreAwarded(),
                a.getIsCorrect(),
                a.getAiGradingStatus(),
                a.getAiFeedback(),
                q.getSolutionExplanation(),
                criterionDTOs
        );
    }

    private <T> T callWithRetry(Callable<T> action) throws Exception {
        try {
            return action.call();
        } catch (Exception e) {
            if (!isTransient(e)) {
                throw e;
            }
            Thread.sleep(2000);
            return action.call();
        }
    }

    private static boolean isTransient(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause()) {
            if (c instanceof WebClientResponseException wcre) {
                int status = wcre.getStatusCode().value();
                return status == 408 || status == 429 || status >= 500;
            }
            if (c instanceof WebClientRequestException) return true;
            if (c instanceof TimeoutException) return true;
            if (c instanceof SocketTimeoutException) return true;
        }
        return false;
    }

    private byte[] readAnswerImage(MockExamAnswer answer) throws Exception {
        Resource res = storageService.loadAsResource(answer.getAnswerImageFilename());
        try (InputStream in = res.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static String extensionFromMime(String mime, String originalFilename) {
        if (mime != null) {
            if (mime.contains("png")) return ".png";
            if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
            if (mime.contains("webp")) return ".webp";
            if (mime.contains("gif")) return ".gif";
        }
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return originalFilename.substring(dot).toLowerCase();
            }
        }
        return ".png";
    }

    private static String guessMime(String filename) {
        if (filename == null) return "image/png";
        String f = filename.toLowerCase();
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".gif")) return "image/gif";
        return "image/png";
    }

    private static Short clamp(Short value, Short max) {
        if (value == null) return 0;
        int v = Math.max(0, value);
        int m = max == null ? Integer.MAX_VALUE : max;
        return (short) Math.min(v, m);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
