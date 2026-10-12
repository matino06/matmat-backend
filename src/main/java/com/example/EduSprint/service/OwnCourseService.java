package com.example.EduSprint.service;

import com.example.EduSprint.dto.OwnCourseCreateDTO;
import com.example.EduSprint.dto.OwnCourseDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Vlastiti predmeti kreatora: popis, izrada i potpuno brisanje.
@Service
public class OwnCourseService {

    public static final Set<String> SUBJECTS = Set.of("matematika", "fizika", "kemija", "biologija", "informatika", "ostalo");
    public static final Set<String> LEVELS = Set.of("os78", "ss");
    public static final String STATUS_NEW = "nov";
    static final int MAX_NAME_LENGTH = 100;

    private final CourseRepository courseRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public OwnCourseService(CourseRepository courseRepository, NamedParameterJdbcTemplate jdbc) {
        this.courseRepository = courseRepository;
        this.jdbc = jdbc;
    }

    public List<OwnCourseDTO> list(Account account) {
        requireCreator(account);
        return courseRepository.findByOwnerAccountIdOrderByCourseIdDesc(account.getAccountId())
                .stream().map(OwnCourseDTO::of).toList();
    }

    @Transactional
    public OwnCourseDTO create(Account account, OwnCourseCreateDTO input) {
        requireCreator(account);
        OwnCourseCreateDTO valid = validate(input);
        if (courseRepository.existsByOwnerAccountIdAndCourseNameIgnoreCase(account.getAccountId(), valid.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Već imaš predmet s tim nazivom.");
        }
        Course course = new Course(valid.name(), null);
        course.setOwnerAccountId(account.getAccountId());
        course.setSubject(valid.subject());
        course.setLevel(valid.level());
        course.setStatus(STATUS_NEW);
        return OwnCourseDTO.of(courseRepository.save(course));
    }

    // Briše predmet i sve što mu pripada: lekcije, zadatke, napredak, razgovore s AI-jem.
    // Materijali na S3 i poslovi izrade dodaju se ovdje kad nastanu (ticketi o uploadu i redu poslova).
    @Transactional
    public void delete(Account account, Long courseId, String confirmName) {
        requireCreator(account);
        Course course = courseRepository.findCourseByCourseId(courseId)
                .filter(c -> c.isOwnedBy(account.getAccountId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!confirmMatches(course.getCourseName(), confirmName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Naziv predmeta nije točno prepisan.");
        }
        Long fallback = courseRepository.findFirstByOwnerAccountIdIsNullOrderByCourseIdAsc()
                .map(Course::getCourseId).orElse(null);
        deleteCourseData(courseId, fallback);
    }

    private void deleteCourseData(Long courseId, Long fallbackCourseId) {
        MapSqlParameterSource c = new MapSqlParameterSource("c", courseId).addValue("fallback", fallbackCourseId);
        // Vlastiti predmet ništa ne dijeli sa službenima, pa su sve njegove lekcije, cjeline i područja samo njegovi.
        List<Long> objectives = jdbc.queryForList("SELECT objective_id FROM course_objective WHERE course_id = :c", c, Long.class);
        List<Long> subfields = ids("SELECT DISTINCT subfield_id FROM learning_objective WHERE objective_id IN (:ids)", objectives);
        List<Long> fields = ids("SELECT DISTINCT field_id FROM subfield_of_study WHERE subfield_id IN (:ids)", subfields);
        List<Long> tasks = ids("SELECT task_id FROM task WHERE objective_id IN (:ids)", objectives);
        List<Long> conversations = new ArrayList<>(
                jdbc.queryForList("SELECT conversation_id FROM ai_conversation WHERE course_id = :c", c, Long.class));
        conversations.addAll(ids("SELECT conversation_id FROM ai_conversation WHERE task_id IN (:ids)", tasks));

        update("UPDATE account SET current_task_id = NULL WHERE current_task_id IN (:ids)", tasks);
        jdbc.update("UPDATE account SET current_course_id = :fallback WHERE current_course_id = :c", c);
        update("DELETE FROM ai_message WHERE conversation_id IN (:ids)", conversations);
        update("DELETE FROM ai_conversation WHERE conversation_id IN (:ids)", conversations);
        jdbc.update("DELETE FROM solved_task WHERE course_id = :c", c);
        update("DELETE FROM solved_task WHERE task_id IN (:ids)", tasks);
        update("DELETE FROM explanation_step WHERE task_id IN (:ids)", tasks);
        update("DELETE FROM task WHERE task_id IN (:ids)", tasks);
        update("DELETE FROM account_objective WHERE objective_id IN (:ids)", objectives);
        update("DELETE FROM weak_objectives WHERE objective_id IN (:ids)", objectives);
        update("DELETE FROM objective_prerequisite WHERE objective_id IN (:ids) OR prerequisite_id IN (:ids)", objectives);
        jdbc.update("DELETE FROM course_objective WHERE course_id = :c", c);
        update("DELETE FROM learning_objective WHERE objective_id IN (:ids)", objectives);
        update("DELETE FROM subfield_of_study WHERE subfield_id IN (:ids)", subfields);
        jdbc.update("DELETE FROM course_field_weight WHERE course_id = :c", c);
        update("DELETE FROM field_of_study WHERE field_id IN (:ids)", fields);
        jdbc.update("DELETE FROM user_course_goal WHERE course_id = :c", c);
        jdbc.update("DELETE FROM course WHERE course_id = :c", c);
    }

    private List<Long> ids(String sql, List<Long> in) {
        return in.isEmpty() ? List.of() : jdbc.queryForList(sql, new MapSqlParameterSource("ids", in), Long.class);
    }

    private void update(String sql, List<Long> in) {
        if (!in.isEmpty()) jdbc.update(sql, new MapSqlParameterSource("ids", in));
    }

    static void requireCreator(Account account) {
        if (account == null || !Boolean.TRUE.equals(account.getIsCreator())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    static OwnCourseCreateDTO validate(OwnCourseCreateDTO input) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nedostaju podaci o predmetu.");
        }
        String name = input.name() == null ? "" : input.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Naziv predmeta mora imati od 1 do " + MAX_NAME_LENGTH + " znakova.");
        }
        String subject = input.subject() == null ? "" : input.subject().trim().toLowerCase(Locale.ROOT);
        if (!SUBJECTS.contains(subject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nepoznat predmet.");
        }
        String level = input.level() == null ? "" : input.level().trim().toLowerCase(Locale.ROOT);
        if (!LEVELS.contains(level)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nepoznata razina.");
        }
        return new OwnCourseCreateDTO(name, subject, level);
    }

    static boolean confirmMatches(String courseName, String typed) {
        return typed != null && courseName.trim().equals(typed.trim());
    }
}
