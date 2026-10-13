package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

// Izmjene plana vlastitog predmeta dok je „nacrt“ (ticket „Pregled i uređivanje plana“).
// Nakon potvrde smije se samo preimenovati lekcija. Svaka izmjena odmah vrijedi; upozorenja osvježava
// ponovna provjera koju frontend traži kad kreator napravi pauzu (recheck).
@Service
public class OwnCoursePlanEditService {

    private static final java.time.ZoneId ZAGREB = java.time.ZoneId.of("Europe/Zagreb");

    static final int MAX_NAME = 200;

    private final CourseRepository courseRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final BuildJobQueue queue;
    private final PlanFlow flow;

    public OwnCoursePlanEditService(CourseRepository courseRepository, NamedParameterJdbcTemplate jdbc,
                                    BuildJobQueue queue, PlanFlow flow) {
        this.courseRepository = courseRepository;
        this.jdbc = jdbc;
        this.queue = queue;
        this.flow = flow;
    }

    // --- lekcije ---

    @Transactional
    public void rename(Account account, long courseId, long objectiveId, String name) {
        Course course = own(account, courseId);
        if (!Set.of(PlanFinalizeStep.STATUS_DRAFT, "potvrden").contains(course.getStatus())) throw conflict();
        lessonPosition(courseId, objectiveId);
        jdbc.update("UPDATE learning_objective SET objective_name = :n WHERE objective_id = :o",
                new MapSqlParameterSource("o", objectiveId).addValue("n", validName(name)));
    }

    @Transactional
    public void move(Account account, long courseId, long objectiveId, int direction) {
        draft(account, courseId);
        int p = lessonPosition(courseId, objectiveId);
        int q = p + (direction < 0 ? -1 : 1);
        Long other = objectiveAt(courseId, q);
        if (other == null) return;
        setPosition(courseId, objectiveId, q);
        setPosition(courseId, other, p);
    }

    // Spaja lekciju sa sljedećom: stavke i preduvjeti prelaze na prvu, druga nestaje.
    @Transactional
    public void mergeNext(Account account, long courseId, long objectiveId) {
        draft(account, courseId);
        int p = lessonPosition(courseId, objectiveId);
        Long next = objectiveAt(courseId, p + 1);
        if (next == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nema sljedeće lekcije.");
        merge(courseId, objectiveId, next);
    }

    // Lekcija b se spaja u lekciju a (bilo gdje u planu).
    private void merge(long courseId, long objectiveId, long next) {
        lessonPosition(courseId, next);
        MapSqlParameterSource m = new MapSqlParameterSource("a", objectiveId).addValue("b", next);
        jdbc.update("""
                UPDATE learning_objective a SET objective_name = a.objective_name || ' + ' || b.objective_name,
                       concept = trim(COALESCE(a.concept, '') || ' ' || COALESCE(b.concept, ''))
                FROM learning_objective b WHERE a.objective_id = :a AND b.objective_id = :b""", m);
        jdbc.update("UPDATE knowledge_item SET objective_id = :a WHERE objective_id = :b", m);
        jdbc.update("""
                INSERT INTO objective_prerequisite (objective_id, prerequisite_id)
                SELECT :a, prerequisite_id FROM objective_prerequisite x WHERE x.objective_id = :b AND x.prerequisite_id <> :a
                  AND NOT EXISTS (SELECT 1 FROM objective_prerequisite y WHERE y.objective_id = :a AND y.prerequisite_id = x.prerequisite_id)""", m);
        jdbc.update("""
                INSERT INTO objective_prerequisite (objective_id, prerequisite_id)
                SELECT objective_id, :a FROM objective_prerequisite x WHERE x.prerequisite_id = :b AND x.objective_id <> :a
                  AND NOT EXISTS (SELECT 1 FROM objective_prerequisite y WHERE y.objective_id = x.objective_id AND y.prerequisite_id = :a)""", m);
        removeLesson(courseId, next);
    }

    // Briše lekciju; vraća lekcije koje su je imale kao preduvjet (njih treba ponovno provjeriti).
    @Transactional
    public List<Long> delete(Account account, long courseId, long objectiveId) {
        draft(account, courseId);
        lessonPosition(courseId, objectiveId);
        List<Long> dependents = jdbc.queryForList("SELECT objective_id FROM objective_prerequisite WHERE prerequisite_id = :o",
                new MapSqlParameterSource("o", objectiveId), Long.class);
        jdbc.update("DELETE FROM knowledge_item WHERE objective_id = :o", new MapSqlParameterSource("o", objectiveId));
        removeLesson(courseId, objectiveId);
        return dependents;
    }

    // Nova lekcija iza zadane (ili na početak); stavke, koncept i preduvjete izradi AI (PlanNewLessonStep).
    @Transactional
    public long add(Account account, long courseId, Long afterObjectiveId, String name) {
        draft(account, courseId);
        int position;
        long subfieldId;
        if (afterObjectiveId != null) {
            position = lessonPosition(courseId, afterObjectiveId) + 1;
            subfieldId = jdbc.queryForObject("SELECT subfield_id FROM learning_objective WHERE objective_id = :o",
                    new MapSqlParameterSource("o", afterObjectiveId), Long.class);
        } else {
            position = 1;
            subfieldId = prerequisiteUnit(courseId);
        }
        return insertLesson(account, courseId, subfieldId, position, validName(name));
    }

    @Transactional
    public void addPrerequisite(Account account, long courseId, long objectiveId, long prerequisiteId) {
        draft(account, courseId);
        lessonPosition(courseId, objectiveId);
        lessonPosition(courseId, prerequisiteId);
        if (objectiveId == prerequisiteId) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lekcija ne može biti sama sebi preduvjet.");
        jdbc.update("""
                INSERT INTO objective_prerequisite (objective_id, prerequisite_id)
                SELECT :o, :p WHERE NOT EXISTS (SELECT 1 FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p)""",
                new MapSqlParameterSource("o", objectiveId).addValue("p", prerequisiteId));
    }

    @Transactional
    public void removePrerequisite(Account account, long courseId, long objectiveId, long prerequisiteId) {
        draft(account, courseId);
        lessonPosition(courseId, objectiveId);
        jdbc.update("DELETE FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p",
                new MapSqlParameterSource("o", objectiveId).addValue("p", prerequisiteId));
    }

    // Premješta lekciju iza zadane (ili na početak plana).
    private void moveAfter(long courseId, long objectiveId, Long afterObjectiveId) {
        int p = lessonPosition(courseId, objectiveId);
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId).addValue("o", objectiveId).addValue("p", p);
        jdbc.update("UPDATE course_objective SET position = 0 WHERE course_id = :c AND objective_id = :o", c);
        jdbc.update("UPDATE course_objective SET position = position - 1 WHERE course_id = :c AND position > :p", c);
        int target = afterObjectiveId == null ? 1 : lessonPosition(courseId, afterObjectiveId) + 1;
        jdbc.update("UPDATE course_objective SET position = position + 1 WHERE course_id = :c AND position >= :t AND objective_id <> :o",
                c.addValue("t", target));
        setPosition(courseId, objectiveId, target);
    }

    // --- AI uputa ---

    // Uputa ide u red poslova (model plana); kreator prijedlog dobiva pollanjem i tek ga prihvaćanjem primjenjuje.
    public long instruct(Account account, long courseId, String text) {
        draft(account, courseId);
        String t = text == null ? "" : text.strip();
        if (t.isEmpty() || t.length() > 1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uputa mora imati od 1 do 1000 znakova.");
        Long id = queue.enqueue(courseId, account.getAccountId(), PlanInstructionStep.KIND, "uputa " + System.nanoTime(),
                BuildJobQueue.PRIORITY_WAITED_ON, true, toJson(Map.of("text", t)));
        if (id == null) throw new ResponseStatusException(HttpStatus.CONFLICT);
        return id;
    }

    // „Dodaj lekciju“ uz nepokriven zadatak s ispita: uputa koju frontend nakon prijedloga odmah prihvati.
    public long instructForWarning(Account account, long courseId, long warningId) {
        own(account, courseId);
        List<Map<String, Object>> w = jdbc.queryForList(
                "SELECT text, detail FROM plan_warning WHERE warning_id = :w AND course_id = :c AND kind = 'ispit'",
                new MapSqlParameterSource("w", warningId).addValue("c", courseId));
        if (w.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return instruct(account, courseId, "Dodaj JEDNU lekciju koja uči ono što nedostaje za ovaj zadatak, na pravo mjesto u planu "
                + "(iza lekcija koje koristi). Ništa drugo ne mijenjaj. " + w.get(0).get("text")
                + (w.get(0).get("detail") == null ? "" : " Nedostaje: " + w.get(0).get("detail")));
    }

    public Map<String, Object> instruction(Account account, long courseId, long jobId) {
        own(account, courseId);
        Map<String, Object> job = instructionJob(courseId, jobId);
        Map<String, Object> out = new HashMap<>();
        out.put("status", job.get("status"));
        out.put("error", job.get("error"));
        out.put("proposal", job.get("output") == null ? null : fromJson((String) job.get("output")));
        return out;
    }

    // Primjenjuje prijedlog (samo jednom); vraća lekcije koje treba ponovno provjeriti.
    @Transactional
    public List<Long> accept(Account account, long courseId, long jobId) {
        draft(account, courseId);
        Map<String, Object> job = instructionJob(courseId, jobId);
        if (!BuildJobQueue.DONE.equals(job.get("status")) || job.get("output") == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prijedlog još nije gotov.");
        }
        Map<String, Object> proposal = fromJson((String) job.get("output"));
        if (Boolean.TRUE.equals(proposal.get("primijenjeno"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prijedlog je već primijenjen.");
        }
        List<Long> changed = new ArrayList<>();
        Set<Long> present = new HashSet<>(jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c",
                new MapSqlParameterSource("c", courseId), Long.class));
        for (Object o : (List<?>) proposal.getOrDefault("operacije", List.of())) {
            Map<?, ?> op = (Map<?, ?>) o;
            Long a = num(op.get("a")), b = num(op.get("b")), id = num(op.get("id")), after = num(op.get("iza"));
            switch (String.valueOf(op.get("op"))) {
                case "spoji" -> { if (present.contains(a) && present.contains(b)) { merge(courseId, a, b); present.remove(b); changed.add(a); } }
                case "preimenuj" -> { if (present.contains(id)) { jdbc.update("UPDATE learning_objective SET objective_name = :n WHERE objective_id = :o",
                        new MapSqlParameterSource("o", id).addValue("n", validName(String.valueOf(op.get("naziv"))))); changed.add(id); } }
                case "obrisi" -> { if (present.contains(id)) {
                    changed.addAll(jdbc.queryForList("SELECT objective_id FROM objective_prerequisite WHERE prerequisite_id = :o",
                            new MapSqlParameterSource("o", id), Long.class));
                    jdbc.update("DELETE FROM knowledge_item WHERE objective_id = :o", new MapSqlParameterSource("o", id));
                    removeLesson(courseId, id); present.remove(id); } }
                case "dodaj" -> {
                    int position = after != null && present.contains(after) ? lessonPosition(courseId, after) + 1 : 1;
                    long subfield = after != null && present.contains(after)
                            ? jdbc.queryForObject("SELECT subfield_id FROM learning_objective WHERE objective_id = :o",
                                    new MapSqlParameterSource("o", after), Long.class)
                            : prerequisiteUnit(courseId);
                    present.add(insertLesson(account, courseId, subfield, position, validName(String.valueOf(op.get("naziv")))));
                }
                case "premjesti" -> { if (present.contains(id) && (after == null || present.contains(after))) { moveAfter(courseId, id, after); changed.add(id); } }
                default -> { }
            }
        }
        proposal.put("primijenjeno", true);
        jdbc.update("UPDATE build_job SET output = :o WHERE job_id = :j",
                new MapSqlParameterSource("j", jobId).addValue("o", toJson(proposal)));
        changed.removeIf(x -> !present.contains(x));
        return changed;
    }

    private Map<String, Object> instructionJob(long courseId, long jobId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status, output, error FROM build_job WHERE job_id = :j AND course_id = :c AND kind = :k",
                new MapSqlParameterSource("j", jobId).addValue("c", courseId).addValue("k", PlanInstructionStep.KIND));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return rows.get(0);
    }

    private static Long num(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String s) {
        try {
            return mapper.readValue(s, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // --- potvrda plana ---

    public static final int FIRST_LESSONS_NOW = 5;
    public static final int MAX_LESSONS_WITHOUT_BATCH = 50;

    // Potvrda zaključava plan i pokreće izradu 1. varijacija (ne troše kvotu). Do 50 lekcija sve idu odmah,
    // prvih 5 s prioritetom; iznad 50 prvih 5 odmah, a ostale kao batch.
    @Transactional
    public void confirm(Account account, long courseId, boolean seriousAccepted) {
        Course course = draft(account, courseId);
        if (course.getLearningMode() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prvo odaberi način učenja.");
        if (checking(courseId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Provjera plana još traje; pričekaj da završi.");
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        Integer serious = jdbc.queryForObject(
                "SELECT count(*) FROM plan_warning WHERE course_id = :c AND serious AND NOT ignored", c, Integer.class);
        if (serious != null && serious > 0 && !seriousAccepted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Označi da si pregledao ozbiljna upozorenja.");
        }
        List<Long> lessons = jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c ORDER BY position", c, Long.class);
        if (lessons.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan nema lekcija.");
        course.setStatus(TaskVariationStep.STATUS_CONFIRMED);
        courseRepository.save(course);
        // Učenje ide redom plana: preduvjet koji je u planu iza lekcije (krug ili upozorenje o redoslijedu koje je
        // kreator prihvatio) zauvijek bi zaključao lekciju, pa se takvi bridovi uklanjaju.
        jdbc.update("""
                DELETE FROM objective_prerequisite op USING course_objective a, course_objective p
                WHERE a.course_id = :c AND p.course_id = :c AND a.objective_id = op.objective_id
                  AND p.objective_id = op.prerequisite_id AND p.position >= a.position""", c);
        // Ponavljanje radi preko account_objective: kreator dobiva redove za sve lekcije (kao novi račun za službene).
        jdbc.update("""
                INSERT INTO account_objective (account_id, objective_id, ef, i, n, last_solved_date)
                SELECT :a, co.objective_id, 2.5, 0, 0, now() FROM course_objective co
                WHERE co.course_id = :c AND NOT EXISTS (
                    SELECT 1 FROM account_objective ao WHERE ao.account_id = :a AND ao.objective_id = co.objective_id)""",
                new MapSqlParameterSource("c", courseId).addValue("a", account.getAccountId()));
        boolean batch = lessons.size() > MAX_LESSONS_WITHOUT_BATCH;
        for (int i = 0; i < lessons.size(); i++) {
            if (batch && i >= FIRST_LESSONS_NOW) break;
            queue.enqueue(courseId, account.getAccountId(), TaskVariationStep.KIND, "v1 " + lessons.get(i),
                    i < FIRST_LESSONS_NOW ? BuildJobQueue.PRIORITY_WAITED_ON : BuildJobQueue.PRIORITY_BACKGROUND, false,
                    TaskVariationStep.input(lessons.get(i), 1));
        }
        if (batch) {
            queue.enqueue(courseId, account.getAccountId(), VariationBatchStep.KIND, "v1", BuildJobQueue.PRIORITY_BACKGROUND, false,
                    "{\"objectiveIds\":" + lessons.subList(FIRST_LESSONS_NOW, lessons.size()) + "}");
        }
    }

    // --- način učenja ---

    public static final String MODE_FULL = "ucenje_gradiva";
    public static final String MODE_QUICK = "brza_priprema";
    public static final int MAX_QUICK_LESSONS = 50;

    public Map<String, Object> learningMode(Account account, long courseId) {
        Course course = own(account, courseId);
        Map<String, Object> out = new HashMap<>();
        out.put("mode", course.getLearningMode());
        out.put("examDate", course.getExamDate() == null ? null : course.getExamDate().toString());
        out.put("conceptLessons", conceptLessons(courseId));
        out.put("maxQuickLessons", MAX_QUICK_LESSONS);
        List<Map<String, Object>> job = jdbc.queryForList(
                "SELECT status FROM build_job WHERE course_id = :c AND kind = :k ORDER BY job_id DESC LIMIT 1",
                new MapSqlParameterSource("c", courseId).addValue("k", PlanApplicationStep.KIND));
        out.put("applications", job.isEmpty() ? null : job.get(0).get("status"));
        return out;
    }

    // Način učenja bira se jednom, dok je plan nacrt i kad je poznat broj lekcija koncepata; poslije se ne mijenja.
    @Transactional
    public void chooseLearningMode(Account account, long courseId, String mode, LocalDate examDate) {
        Course course = draft(account, courseId);
        if (course.getLearningMode() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Način učenja je već odabran i ne može se promijeniti.");
        }
        if (!MODE_FULL.equals(mode) && !MODE_QUICK.equals(mode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nepoznat način učenja.");
        if (examDate != null && examDate.isBefore(LocalDate.now(ZAGREB))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datum ispita ne može biti u prošlosti.");
        }
        if (MODE_QUICK.equals(mode)) {
            if (examDate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Za brzu pripremu upiši datum testa.");
            int n = conceptLessons(courseId);
            if (n > MAX_QUICK_LESSONS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gradivo ima " + n
                    + " lekcija; brza priprema moguća je do " + MAX_QUICK_LESSONS
                    + ". Podijeli materijal na manje dijelove ili odaberi učenje gradiva.");
        }
        course.setLearningMode(mode);
        course.setExamDate(examDate);
        courseRepository.save(course);
        if (MODE_QUICK.equals(mode)) {
            queue.enqueue(courseId, account.getAccountId(), PlanApplicationStep.KIND, "primjena", BuildJobQueue.PRIORITY_WAITED_ON, true, null);
        }
    }

    private int conceptLessons(long courseId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                WHERE co.course_id = :c AND lo.kind IS DISTINCT FROM :k""",
                new MapSqlParameterSource("c", courseId).addValue("k", PlanApplicationStep.LESSON_KIND), Integer.class);
        return n == null ? 0 : n;
    }

    // --- pretpostavljeno predznanje ---

    public List<Map<String, Object>> assumedKnowledge(Account account, long courseId) {
        own(account, courseId);
        return jdbc.queryForList("SELECT assumed_id AS \"assumedId\", text FROM assumed_knowledge WHERE course_id = :c ORDER BY assumed_id",
                new MapSqlParameterSource("c", courseId));
    }

    @Transactional
    public void addAssumed(Account account, long courseId, String text) {
        draft(account, courseId);
        String t = text == null ? "" : text.strip();
        if (t.isEmpty() || t.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upiši što učenik već zna (do 500 znakova).");
        jdbc.update("INSERT INTO assumed_knowledge (course_id, text) VALUES (:c, :t)",
                new MapSqlParameterSource("c", courseId).addValue("t", t));
    }

    @Transactional
    public void removeAssumed(Account account, long courseId, long assumedId) {
        draft(account, courseId);
        jdbc.update("DELETE FROM assumed_knowledge WHERE assumed_id = :a AND course_id = :c",
                new MapSqlParameterSource("a", assumedId).addValue("c", courseId));
    }

    // „Ipak to ne znam“: stavka predznanja postaje lekcija na početku plana.
    @Transactional
    public long notKnown(Account account, long courseId, long assumedId) {
        draft(account, courseId);
        List<String> text = jdbc.queryForList("SELECT text FROM assumed_knowledge WHERE assumed_id = :a AND course_id = :c",
                new MapSqlParameterSource("a", assumedId).addValue("c", courseId), String.class);
        if (text.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        jdbc.update("DELETE FROM assumed_knowledge WHERE assumed_id = :a", new MapSqlParameterSource("a", assumedId));
        String name = text.get(0).length() > MAX_NAME ? text.get(0).substring(0, MAX_NAME) : text.get(0);
        return insertLesson(account, courseId, prerequisiteUnit(courseId), 1, name);
    }

    // --- upozorenja i ponovna provjera ---

    public void ignoreWarning(Account account, long courseId, long warningId) {
        own(account, courseId);
        jdbc.update("UPDATE plan_warning SET ignored = true WHERE warning_id = :w AND course_id = :c",
                new MapSqlParameterSource("w", warningId).addValue("c", courseId));
    }

    // Ponovna provjera nakon izmjena: promijenjene lekcije i one koje o njima izravno ovise.
    public void recheck(Account account, long courseId, Collection<Long> changed) {
        draft(account, courseId);
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        Set<Long> inCourse = new HashSet<>(jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c", c, Long.class));
        Set<Long> targets = new TreeSet<>();
        for (Long o : changed == null ? List.<Long>of() : changed) if (inCourse.contains(o)) targets.add(o);
        if (!targets.isEmpty()) {
            targets.addAll(jdbc.queryForList("SELECT objective_id FROM objective_prerequisite WHERE prerequisite_id IN (:o)",
                    new MapSqlParameterSource("o", new ArrayList<>(targets)), Long.class).stream().filter(inCourse::contains).toList());
        }
        flow.recheck(courseId, account.getAccountId(), System.currentTimeMillis(), targets);
    }

    public boolean checking(long courseId) {
        return queue.notDone(courseId, PlanZeroStep.KIND, PlanCoverageStep.KIND, PlanCheckRepairStep.KIND, PlanNewLessonStep.KIND,
                PlanApplicationStep.KIND) > 0;
    }

    // --- pomoćno ---

    private long insertLesson(Account account, long courseId, long subfieldId, int position, String name) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId).addValue("p", position);
        jdbc.update("UPDATE course_objective SET position = position + 1 WHERE course_id = :c AND position >= :p", c);
        Long id = jdbc.queryForObject("""
                INSERT INTO learning_objective (subfield_id, objective_name, concept, kind) VALUES (:s, :n, '', 'koncept')
                RETURNING objective_id""", new MapSqlParameterSource("s", subfieldId).addValue("n", name), Long.class);
        jdbc.update("INSERT INTO course_objective (course_id, objective_id, position) VALUES (:c, :o, :p)", c.addValue("o", id));
        queue.enqueue(courseId, account.getAccountId(), PlanNewLessonStep.KIND, "nova lekcija " + id,
                BuildJobQueue.PRIORITY_WAITED_ON, true, "{\"objectiveId\":" + id + "}");
        return id;
    }

    private void removeLesson(long courseId, long objectiveId) {
        MapSqlParameterSource o = new MapSqlParameterSource("o", objectiveId).addValue("c", courseId);
        int p = lessonPosition(courseId, objectiveId);
        jdbc.update("DELETE FROM objective_prerequisite WHERE objective_id = :o OR prerequisite_id = :o", o);
        jdbc.update("DELETE FROM plan_warning WHERE objective_id = :o OR prerequisite_id = :o", o);
        jdbc.update("DELETE FROM zero_finding WHERE objective_id = :o", o);
        jdbc.update("DELETE FROM course_objective WHERE objective_id = :o AND course_id = :c", o);
        jdbc.update("DELETE FROM learning_objective WHERE objective_id = :o", o);
        jdbc.update("UPDATE course_objective SET position = position - 1 WHERE course_id = :c AND position > :p", o.addValue("p", p));
    }

    // Cjelina za lekcije iz predznanja: „Predznanje koje nedostaje“ u prvom području plana.
    private long prerequisiteUnit(long courseId) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId);
        Long fieldId = jdbc.queryForObject("""
                SELECT s.field_id FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id WHERE co.course_id = :c ORDER BY co.position LIMIT 1""", c, Long.class);
        List<Long> existing = jdbc.queryForList("""
                SELECT DISTINCT s.subfield_id FROM course_objective co JOIN learning_objective lo ON lo.objective_id = co.objective_id
                JOIN subfield_of_study s ON s.subfield_id = lo.subfield_id
                WHERE co.course_id = :c AND s.subfield_name = 'Predznanje koje nedostaje'""", c, Long.class);
        if (!existing.isEmpty()) return existing.get(0);
        return jdbc.queryForObject("INSERT INTO subfield_of_study (field_id, subfield_name) VALUES (:f, 'Predznanje koje nedostaje') RETURNING subfield_id",
                new MapSqlParameterSource("f", fieldId), Long.class);
    }

    private int lessonPosition(long courseId, long objectiveId) {
        List<Integer> p = jdbc.queryForList("SELECT position FROM course_objective WHERE course_id = :c AND objective_id = :o",
                new MapSqlParameterSource("c", courseId).addValue("o", objectiveId), Integer.class);
        if (p.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p.get(0);
    }

    private Long objectiveAt(long courseId, int position) {
        List<Long> o = jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c AND position = :p",
                new MapSqlParameterSource("c", courseId).addValue("p", position), Long.class);
        return o.isEmpty() ? null : o.get(0);
    }

    private void setPosition(long courseId, long objectiveId, int position) {
        jdbc.update("UPDATE course_objective SET position = :p WHERE course_id = :c AND objective_id = :o",
                new MapSqlParameterSource("c", courseId).addValue("o", objectiveId).addValue("p", position));
    }

    static String validName(String name) {
        String n = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (n.isEmpty() || n.length() > MAX_NAME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Naziv lekcije mora imati od 1 do " + MAX_NAME + " znakova.");
        }
        return n;
    }

    private Course own(Account account, long courseId) {
        OwnCourseService.requireCreator(account);
        return courseRepository.findCourseByCourseId(courseId)
                .filter(c -> c.isOwnedBy(account.getAccountId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // Plan se mijenja samo dok nije potvrđen (potvrdom se zaključava).
    private Course draft(Account account, long courseId) {
        Course course = own(account, courseId);
        if (!PlanFinalizeStep.STATUS_DRAFT.equals(course.getStatus())) throw conflict();
        return course;
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Plan je potvrđen i više se ne može mijenjati.");
    }
}
