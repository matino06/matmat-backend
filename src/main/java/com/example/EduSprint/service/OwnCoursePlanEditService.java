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

import java.util.*;

// Izmjene plana vlastitog predmeta dok je „nacrt“ (ticket „Pregled i uređivanje plana“).
// Nakon potvrde smije se samo preimenovati lekcija. Svaka izmjena odmah vrijedi; upozorenja osvježava
// ponovna provjera koju frontend traži kad kreator napravi pauzu (recheck).
@Service
public class OwnCoursePlanEditService {

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
        return queue.notDone(courseId, PlanZeroStep.KIND, PlanCoverageStep.KIND, PlanCheckRepairStep.KIND, PlanNewLessonStep.KIND) > 0;
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
