package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

// Popravak plana nakon provjera (ticket „Kako pouzdano provjeriti nultu pretpostavku“): model je samo
// mapirao pojmove na stavke koje ih uče, a presudu i popravak radi kod.
//  - pojam se uči u lekciji ili njezinim preduvjetima → u redu;
//  - uči se u RANIJOJ lekciji koja nije preduvjet → doda se preduvjet (lekcije se otključavaju po preduvjetima);
//  - uči se u KASNIJOJ lekciji → lekcija ide iza nje (preduvjet + novi poredak), ako to ne stvara krug;
//    inače stavka koja koristi pojam seli u tu kasniju lekciju, a tek kad ne može (lekcija bi ostala gotovo prazna,
//    model nije rekao koja stavka) model prepravi stavku tako da ne koristi taj pojam;
//  - „nigdje“ → upozorenje „rupa“.
// Ostalo što se ne da popraviti postaje upozorenje; zatim plan ide kreatoru na pregled („nacrt“) uz e-mail.
// Ozbiljno (crveno) se pri izradi ne predaje kreatoru dok se ne pokuša popraviti: lekcije s neriješenim
// prekršajem i krugovi preduvjeta ponovno se provjeravaju i popravljaju, najviše REPAIR_ROUNDS puta.
@Component
public class PlanCheckRepairStep implements BuildStep {

    public static final String KIND = "plan_check_repair";
    static final int REPAIR_ROUNDS = 2;
    static final int MIN_ITEMS_LEFT = 2;

    static final String REWRITE = """
            Prepravi stavku znanja iz %s tako da više NE koristi pojam „%s“ (učenik ga u tom trenutku još ne zna),
            a i dalje uči isto. Ako bez pojma ne ide, izostavi samo dio rečenice koji ga spominje.
            Vrati JSON {"tekst":"…"}.""";

    enum Verdict { OK, ADD_PREREQUISITE, LATER, NOWHERE }

    record Judgement(Verdict verdict, int teacher) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final OwnCourseLlm llm;
    private final PlanSupport plan;
    private final TransactionTemplate transactions;
    private final OwnCourseMailer mailer;
    private final PlanFlow flow;

    public PlanCheckRepairStep(NamedParameterJdbcTemplate jdbc, OwnCourseLlm llm, PlanSupport plan,
                               TransactionTemplate transactions, OwnCourseMailer mailer, PlanFlow flow) {
        this.flow = flow;
        this.jdbc = jdbc;
        this.llm = llm;
        this.plan = plan;
        this.transactions = transactions;
        this.mailer = mailer;
    }

    @Override
    public String kind() {
        return KIND;
    }

    // Presuda za jedan pojam: lesson i teachers su indeksi lekcija redom učenja.
    static Judgement judge(int lesson, List<String> taughtBy, List<Integer> teachers, List<Set<Integer>> pre) {
        if (taughtBy.contains("predznanje")) return new Judgement(Verdict.OK, -1);
        if (teachers.isEmpty()) {
            return new Judgement(taughtBy.contains("nigdje") ? Verdict.NOWHERE : Verdict.OK, -1);
        }
        Set<Integer> known = closure(lesson, pre);
        known.add(lesson);
        for (int t : teachers) if (known.contains(t)) return new Judgement(Verdict.OK, t);
        int earliest = Collections.min(teachers);
        return new Judgement(earliest < lesson ? Verdict.ADD_PREREQUISITE : Verdict.LATER, earliest);
    }

    static Set<Integer> closure(int lesson, List<Set<Integer>> pre) {
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>(pre.get(lesson));
        while (!stack.isEmpty()) {
            int x = stack.pop();
            if (seen.add(x)) stack.addAll(pre.get(x));
        }
        return seen;
    }

    private record Warning(String kind, Long objectiveId, Long prerequisiteId, String text, String detail, boolean serious) {
    }

    // Prijave istog para lekcija skupljaju se u jedno upozorenje; model isti pojam zna napisati na dva načina
    // („homogeno tijelo/tvar“, „homogena tvar/tijelo“), pa se pojmovi istih riječi broje jednom.
    private static final class Finding {
        final String kind;
        final int lesson;
        final int other;
        final boolean later;
        final boolean serious;
        final Map<String, String> concepts = new LinkedHashMap<>();

        Finding(String kind, int lesson, int other, boolean later, boolean serious) {
            this.kind = kind;
            this.lesson = lesson;
            this.other = other;
            this.later = later;
            this.serious = serious;
        }
    }

    static String conceptKey(String concept) {
        return Arrays.stream(concept.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isEmpty()).map(w -> w.length() > 5 ? w.substring(0, 5) : w).sorted().distinct()
                .collect(java.util.stream.Collectors.joining(" "));
    }

    static String quoted(Collection<String> concepts) {
        return concepts.stream().map(x -> "„" + x + "“").collect(java.util.stream.Collectors.joining(", "));
    }

    private static Warning toWarning(Finding f, List<Long> ids, List<String> names) {
        boolean many = f.concepts.size() > 1;
        String who = "„" + names.get(f.lesson) + "“ koristi " + (many ? "pojmove " : "pojam ") + quoted(f.concepts.values());
        if (f.kind.equals("rupa")) {
            return new Warning("rupa", ids.get(f.lesson), null, who + (many ? ", a nijedna lekcija ih ne uči." : ", a nijedna lekcija ga ne uči."), null, false);
        }
        String text = f.later
                ? who + (many ? ", koji se uče tek u lekciji „" : ", koji se uči tek u lekciji „") + names.get(f.other) + "“."
                : who + " iz lekcije „" + names.get(f.other) + "“, koja mu nije preduvjet.";
        return new Warning("nulta", ids.get(f.lesson), ids.get(f.other), text, null, f.serious);
    }

    @Override
    public String run(BuildJob job) {
        boolean recheck = job.input() != null && plan.readJson(job.input()).path("recheck").asBoolean(false);
        int repair = job.input() == null ? 0 : plan.readJson(job.input()).path("repair").asInt(0);
        boolean lastRound = recheck || repair >= REPAIR_ROUNDS;
        // „Popravi“ uz upozorenje: za te lekcije popravak radi kao pri izradi, za ostale samo javlja
        Set<Long> fix = new HashSet<>();
        if (job.input() != null) plan.readJson(job.input()).path("fix").forEach(n -> fix.add(n.asLong()));
        boolean reorder = !recheck || !fix.isEmpty();
        long courseId = job.courseId();
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        PlanSupport.CourseInfo course = plan.course(courseId);

        // lekcije redom učenja
        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT lo.objective_id, lo.objective_name FROM course_objective co
                JOIN learning_objective lo ON lo.objective_id = co.objective_id WHERE co.course_id = :c ORDER BY co.position""", c)) {
            ids.add(((Number) r.get("objective_id")).longValue());
            names.add((String) r.get("objective_name"));
        }
        Map<Long, Integer> index = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) index.put(ids.get(i), i);
        List<Set<Integer>> pre = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) pre.add(new HashSet<>());
        jdbc.query("SELECT objective_id, prerequisite_id FROM objective_prerequisite WHERE objective_id IN (:o)",
                new MapSqlParameterSource("o", ids.isEmpty() ? List.of(-1L) : ids), rs -> {
                    Integer a = index.get(rs.getLong(1)), b = index.get(rs.getLong(2));
                    if (a != null && b != null && !a.equals(b)) pre.get(a).add(b);
                });
        Map<Long, Integer> itemLesson = new HashMap<>();
        Map<Long, Integer> lessonItems = new HashMap<>();
        Set<Long> reliedOn = new HashSet<>(); // stavke na kojima gradi neka druga stavka: ne sele se
        jdbc.query("""
                SELECT item_id, objective_id, prerequisites FROM knowledge_item
                WHERE course_id = :c AND objective_id IS NOT NULL AND duplicate_of IS NULL""", c, rs -> {
            Integer l = index.get(rs.getLong(2));
            if (l != null) {
                itemLesson.put(rs.getLong(1), l);
                lessonItems.merge(rs.getLong(2), 1, Integer::sum);
            }
            plan.readJson(rs.getString(3)).forEach(n -> reliedOn.add(n.asLong()));
        });
        // presude cijelog kruga gledaju gdje su stavke bile na početku: stavka premještena u ovom krugu ne povlači
        // za sobom stavke koje se na nju oslanjaju
        Map<Long, Integer> itemLessonAtStart = new HashMap<>(itemLesson);
        List<Map<String, Object>> zeroFindings = jdbc.queryForList("""
                SELECT finding_id, objective_id, concept, item_id, taught_by FROM zero_finding WHERE course_id = :c ORDER BY finding_id""", c);
        for (Map<String, Object> f : zeroFindings) {
            Integer lesson = index.get(((Number) f.get("objective_id")).longValue());
            for (JsonNode t : plan.readJson((String) f.get("taught_by"))) {
                Integer l = t.canConvertToLong() || t.asText().matches("\\d+") ? itemLessonAtStart.get(t.asLong()) : null;
                if (l != null && !l.equals(lesson)) reliedOn.add(t.asLong()); // pojam iz nje koristi druga lekcija
            }
        }

        List<Warning> warnings = new ArrayList<>();
        Map<String, Finding> findings = new LinkedHashMap<>();
        Set<Integer> toVerify = new TreeSet<>(); // lekcije koje se ponovno provjeravaju prije predaje kreatoru
        Set<String> seenFindings = new HashSet<>();
        int added = 0, moved = 0, rewritten = 0;
        Set<Long> movedItems = new HashSet<>();
        for (Map<String, Object> f : zeroFindings) {
            Integer lesson = index.get(((Number) f.get("objective_id")).longValue());
            String concept = (String) f.get("concept");
            if (lesson == null || !seenFindings.add(lesson + "|" + conceptKey(concept))) continue;
            if (f.get("item_id") != null && movedItems.contains(((Number) f.get("item_id")).longValue())) {
                // stavka je u ovom krugu već preselila (model joj je prijavio više pojmova): nova lekcija se ponovno provjerava
                jdbc.update("DELETE FROM zero_finding WHERE finding_id = :f", new MapSqlParameterSource("f", f.get("finding_id")));
                continue;
            }
            List<String> taughtBy = new ArrayList<>();
            List<Integer> teachers = new ArrayList<>();
            for (JsonNode t : plan.readJson((String) f.get("taught_by"))) {
                taughtBy.add(t.asText());
                Integer l = t.canConvertToLong() || t.asText().matches("\\d+") ? itemLessonAtStart.get(t.asLong()) : null;
                if (l != null) teachers.add(l);
            }
            Judgement j = judge(lesson, taughtBy, teachers, pre);
            switch (j.verdict()) {
                case OK -> { }
                case NOWHERE -> collect(findings, "rupa", lesson, -1, false, false, concept);
                case ADD_PREREQUISITE, LATER -> {
                    if (recheck && !fix.contains(ids.get(lesson))) {
                        // nakon izmjena kreatora samo javljamo: preduvjet je možda namjerno maknut
                        boolean later = j.verdict() == Verdict.LATER;
                        collect(findings, "nulta", lesson, j.teacher(), later, later, concept);
                    } else if (!PlanRepair.createsCycle(pre, lesson, j.teacher())) {
                        pre.get(lesson).add(j.teacher());
                        added++;
                    } else if (f.get("item_id") != null && moveItem(((Number) f.get("item_id")).longValue(), lesson, j.teacher(),
                            ids, itemLesson, lessonItems, reliedOn)) {
                        moved++;
                        movedItems.add(((Number) f.get("item_id")).longValue());
                        jdbc.update("DELETE FROM zero_finding WHERE objective_id = :o AND lower(concept) = lower(:k)",
                                new MapSqlParameterSource("o", ids.get(lesson)).addValue("k", concept));
                        toVerify.add(lesson);
                        toVerify.add(j.teacher());
                    } else if (f.get("item_id") != null ? rewrite(job, course, ((Number) f.get("item_id")).longValue(), concept)
                            : rewriteMentions(job, course, ids.get(lesson), concept)) {
                        rewritten++;
                        // nalaz je riješen: da ga kasnija provjera ne prijavi ponovno, briše se; lekcija se još jednom provjerava
                        jdbc.update("DELETE FROM zero_finding WHERE objective_id = :o AND lower(concept) = lower(:k)",
                                new MapSqlParameterSource("o", ids.get(lesson)).addValue("k", concept));
                        toVerify.add(lesson);
                    } else {
                        collect(findings, "nulta", lesson, j.teacher(), true, true, concept);
                        toVerify.add(lesson);
                    }
                }
            }
        }

        for (Finding f : findings.values()) warnings.add(toWarning(f, ids, names));

        // Pri izradi se lekcije slažu ispočetka; nakon izmjena kreatora redoslijed ostaje njegov i samo se javlja.
        PlanRepair.Order order = reorder ? PlanRepair.topoOrder(pre) : currentOrder(pre);
        List<long[]> dropped = new ArrayList<>();
        for (int[] b : order.backwards()) {
            int lesson = order.order().get(b[0]), prerequisite = order.order().get(b[1]);
            if (!lastRound) {
                // krug preduvjeta pri izradi: brid se miče, a provjera nulte pretpostavke odluči treba li lekciji
                // stvarno taj pojam (ako treba, idući krug je premjesti ili prepravi stavku)
                dropped.add(new long[]{ids.get(lesson), ids.get(prerequisite)});
                pre.get(lesson).remove(prerequisite);
                toVerify.add(lesson);
                continue;
            }
            warnings.add(new Warning("redoslijed", ids.get(lesson), ids.get(prerequisite),
                    "„" + names.get(lesson) + "“ dolazi prije svog preduvjeta „" + names.get(prerequisite) + "“.", null, true));
        }
        boolean anotherRound = !lastRound && !toVerify.isEmpty();
        Set<String> ignored = new HashSet<>(jdbc.queryForList("""
                SELECT kind || '|' || COALESCE(objective_id, 0) || '|' || COALESCE(prerequisite_id, 0) || '|' || text
                FROM plan_warning WHERE course_id = :c AND ignored""", c, String.class));
        for (int i = 0; i < ids.size(); i++) {
            int n = lessonItems.getOrDefault(ids.get(i), 0);
            if (n > PlanRepair.MAX_ITEMS) {
                warnings.add(new Warning("velicina", ids.get(i), null,
                        "„" + names.get(i) + "“ ima " + n + " stavki znanja; lekcija bi trebala imati najviše " + PlanRepair.MAX_ITEMS + ".", null, false));
            } else if (n > 0 && n < PlanRepair.MIN_ITEMS) {
                // npr. nakon premještanja stavki u kasniju lekciju: kreator je može spojiti sa susjednom
                warnings.add(new Warning("velicina", ids.get(i), null,
                        "„" + names.get(i) + "“ ima samo " + n + (n == 1 ? " stavku" : " stavke") + " znanja; lekcija bi trebala imati barem "
                                + PlanRepair.MIN_ITEMS + ".", null, false));
            }
        }
        for (Map<String, Object> t : jdbc.queryForList(
                "SELECT number, summary, missing FROM exam_task WHERE course_id = :c AND missing IS NOT NULL ORDER BY exam_task_id", c)) {
            warnings.add(new Warning("ispit", null, null, "Plan ne pokriva zadatak s ispita"
                    + (t.get("number") == null || t.get("number").toString().isBlank() ? "" : " (" + t.get("number") + ")")
                    + ": " + t.get("summary"), (String) t.get("missing"), false));
        }

        transactions.executeWithoutResult(tx -> {
            for (int k = 0; reorder && k < order.order().size(); k++) {
                jdbc.update("UPDATE course_objective SET position = :p WHERE course_id = :c AND objective_id = :o",
                        new MapSqlParameterSource("c", courseId).addValue("p", k + 1).addValue("o", ids.get(order.order().get(k))));
            }
            for (int i = 0; i < ids.size(); i++) {
                for (int p : pre.get(i)) {
                    jdbc.update("""
                            INSERT INTO objective_prerequisite (objective_id, prerequisite_id)
                            SELECT :o, :p WHERE NOT EXISTS (SELECT 1 FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p)""",
                            new MapSqlParameterSource("o", ids.get(i)).addValue("p", ids.get(p)));
                }
            }
            for (long[] d : dropped) {
                jdbc.update("DELETE FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p",
                        new MapSqlParameterSource("o", d[0]).addValue("p", d[1]));
            }
            updateWeights(courseId);
            if (anotherRound) return; // upozorenja i predaja kreatoru tek nakon idućeg kruga
            jdbc.update("DELETE FROM plan_warning WHERE course_id = :c", new MapSqlParameterSource("c", courseId));
            for (Warning w : warnings) {
                jdbc.update("""
                        INSERT INTO plan_warning (course_id, kind, objective_id, prerequisite_id, text, detail, serious, ignored)
                        VALUES (:c, :k, :o, :p, :t, :d, :s, :i)""",
                        new MapSqlParameterSource("c", courseId).addValue("k", w.kind()).addValue("o", w.objectiveId())
                                .addValue("p", w.prerequisiteId()).addValue("t", w.text()).addValue("d", w.detail())
                                .addValue("s", w.serious()).addValue("i", ignored.contains(w.kind() + "|"
                                        + (w.objectiveId() == null ? 0 : w.objectiveId()) + "|"
                                        + (w.prerequisiteId() == null ? 0 : w.prerequisiteId()) + "|" + w.text())));
            }
            if (!recheck) {
                jdbc.update("UPDATE course SET status = :s WHERE course_id = :c",
                        new MapSqlParameterSource("c", courseId).addValue("s", PlanFinalizeStep.STATUS_DRAFT));
            }
        });
        if (anotherRound) {
            List<Long> lessons = toVerify.stream().map(ids::get).toList();
            flow.repairRound(courseId, job.accountId(), repair + 1, lessons);
            return added + " preduvjeta dodano, " + moved + " stavki premješteno, " + rewritten + " stavki prepravljeno, " + dropped.size()
                    + " bridova kruga maknuto; ponovna provjera " + lessons.size() + " lekcija (krug " + (repair + 1) + ")";
        }
        if (!recheck) mailer.planReady(courseId);
        return added + " preduvjeta dodano, " + moved + " stavki premješteno, " + rewritten + " stavki prepravljeno, "
                + warnings.size() + " upozorenja";
    }

    private static void collect(Map<String, Finding> findings, String kind, int lesson, int other, boolean later,
                                boolean serious, String concept) {
        Finding f = findings.computeIfAbsent(kind + "|" + lesson + "|" + other + "|" + later,
                k -> new Finding(kind, lesson, other, later, serious));
        f.concepts.putIfAbsent(conceptKey(concept), concept);
    }

    // Trenutni poredak bez premještanja: brid prema kasnijoj lekciji je „backwards“.
    static PlanRepair.Order currentOrder(List<Set<Integer>> pre) {
        List<Integer> order = new ArrayList<>();
        List<List<Integer>> prerequisites = new ArrayList<>();
        List<int[]> backwards = new ArrayList<>();
        for (int i = 0; i < pre.size(); i++) {
            order.add(i);
            List<Integer> p = new ArrayList<>(pre.get(i));
            Collections.sort(p);
            prerequisites.add(p);
            for (int q : p) if (q > i) backwards.add(new int[]{i, q});
        }
        return new PlanRepair.Order(order, prerequisites, backwards);
    }

    // Težine spremnosti po području: udio zadataka s ispita koje područje pokriva. Bez ispita ostaje udio lekcija.
    private void updateWeights(long courseId) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        Map<Long, Long> fieldOf = new HashMap<>();
        jdbc.query("""
                SELECT co.objective_id, s.field_id FROM course_objective co
                JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id WHERE co.course_id = :c""", c,
                rs -> { fieldOf.put(rs.getLong(1), rs.getLong(2)); });
        Map<Long, Integer> tasksPerField = new HashMap<>();
        int touches = 0;
        for (String lessons : jdbc.queryForList("SELECT lessons FROM exam_task WHERE course_id = :c AND lessons IS NOT NULL", c, String.class)) {
            Set<Long> fields = new HashSet<>();
            plan.readJson(lessons).forEach(n -> { Long f = fieldOf.get(n.asLong()); if (f != null) fields.add(f); });
            for (Long f : fields) tasksPerField.merge(f, 1, Integer::sum);
            touches += fields.size();
        }
        if (touches == 0) return;
        jdbc.update("DELETE FROM course_field_weight WHERE course_id = :c", c);
        for (Map.Entry<Long, Integer> e : tasksPerField.entrySet()) {
            jdbc.update("INSERT INTO course_field_weight (course_id, field_id, weight_percent) VALUES (:c, :f, :w)",
                    new MapSqlParameterSource("c", courseId).addValue("f", e.getKey())
                            .addValue("w", BigDecimal.valueOf(100.0 * e.getValue() / touches).setScale(1, RoundingMode.HALF_UP)));
        }
    }

    // Stavka koja koristi pojam iz kasnije lekcije seli se u tu lekciju, cijela: tamo učenik pojam već zna, a lekcija
    // iz koje odlazi zadržava barem MIN_ITEMS_LEFT stavki. Prepravljanje bi iz stavke izbacilo sadržaj (Stockov sustav
    // iz nazivlja, hidrolizu iz otopina soli). Seli se samo stavka na kojoj ništa ne gradi: osnovna definicija koju
    // usput spominje kasniji pojam („monomi iste vrste … zbrajaju se izlučivanjem“) odvukla bi za sobom lekcije koje je
    // trebaju, pa se ona prepravlja.
    private boolean moveItem(long itemId, int lesson, int teacher, List<Long> ids, Map<Long, Integer> itemLesson,
                             Map<Long, Integer> lessonItems, Set<Long> reliedOn) {
        Integer from = itemLesson.get(itemId);
        if (from == null || from != lesson || reliedOn.contains(itemId)
                || lessonItems.getOrDefault(ids.get(lesson), 0) <= MIN_ITEMS_LEFT) return false;
        jdbc.update("""
                UPDATE knowledge_item SET objective_id = :t,
                    position = (SELECT COALESCE(max(position), 0) + 1 FROM knowledge_item WHERE objective_id = :t)
                WHERE item_id = :i""", new MapSqlParameterSource("i", itemId).addValue("t", ids.get(teacher)));
        itemLesson.put(itemId, teacher);
        lessonItems.merge(ids.get(lesson), -1, Integer::sum);
        lessonItems.merge(ids.get(teacher), 1, Integer::sum);
        return true;
    }

    // Model nije rekao koja stavka koristi pojam: prepravljaju se stavke lekcije u kojima se pojam spominje (najviše dvije).
    private boolean rewriteMentions(BuildJob job, PlanSupport.CourseInfo course, long objectiveId, String concept) {
        List<String> stems = Arrays.stream(conceptKey(concept).split(" ")).filter(w -> w.length() >= 4).toList();
        if (stems.isEmpty()) return false;
        int done = 0;
        for (Map<String, Object> item : jdbc.queryForList(
                "SELECT item_id, text FROM knowledge_item WHERE objective_id = :o AND duplicate_of IS NULL ORDER BY position",
                new MapSqlParameterSource("o", objectiveId))) {
            String text = String.valueOf(item.get("text")).toLowerCase(Locale.ROOT);
            if (done < 2 && stems.stream().anyMatch(text::contains) && rewrite(job, course, ((Number) item.get("item_id")).longValue(), concept)) {
                done++;
            }
        }
        return done > 0;
    }

    private boolean rewrite(BuildJob job, PlanSupport.CourseInfo course, long itemId, String concept) {
        try {
            String text = jdbc.queryForObject("SELECT text FROM knowledge_item WHERE item_id = :i",
                    new MapSqlParameterSource("i", itemId), String.class);
            String fixed = plan.json(llm.chat(job, plan.planModel, REWRITE.formatted(course.describe(), concept), text,
                    OwnCourseLlm.Options.of("low", 1000)).content()).path("tekst").asText("").strip();
            if (fixed.isEmpty() || fixed.equals(text)) return false;
            jdbc.update("UPDATE knowledge_item SET text = :t WHERE item_id = :i",
                    new MapSqlParameterSource("i", itemId).addValue("t", fixed));
            return true;
        } catch (OwnCourseLlm.GlobalLimitException e) {
            throw e;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
