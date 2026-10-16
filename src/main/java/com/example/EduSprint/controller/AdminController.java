package com.example.EduSprint.controller;

import com.example.EduSprint.dto.AdminCourseDTO;
import com.example.EduSprint.dto.AiReportDTO;
import com.example.EduSprint.dto.AdminCourseGraphDTO;
import com.example.EduSprint.dto.AdminMockExamDetailDTO;
import com.example.EduSprint.dto.AdminMockExamQuestionDTO;
import com.example.EduSprint.dto.AdminMockExamQuestionUpdateDTO;
import com.example.EduSprint.dto.AdminMockExamSummaryDTO;
import com.example.EduSprint.dto.AdminObjectiveNodeDTO;
import com.example.EduSprint.dto.AdminPrerequisiteEdgeDTO;
import com.example.EduSprint.dto.AiSettingsDTO;
import com.example.EduSprint.dto.AiSettingsUpdateDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.AdminMockExamService;
import com.example.EduSprint.service.AiReportService;
import com.example.EduSprint.service.AiSettingsService;
import com.example.EduSprint.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AccountService accountService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final LearningObjectiveRepository learningObjectiveRepository;
    private final AiSettingsService aiSettingsService;
    private final AdminMockExamService adminMockExamService;
    private final com.example.EduSprint.service.AdminOwnCourseService ownCourses;
    private final AiReportService aiReportService;

    public AdminController(AccountService accountService, CourseService courseService,
                          CourseRepository courseRepository, LearningObjectiveRepository learningObjectiveRepository,
                          AiSettingsService aiSettingsService, AdminMockExamService adminMockExamService,
                          com.example.EduSprint.service.AdminOwnCourseService ownCourses,
                          AiReportService aiReportService) {
        this.ownCourses = ownCourses;
        this.aiReportService = aiReportService;
        this.accountService = accountService;
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.aiSettingsService = aiSettingsService;
        this.adminMockExamService = adminMockExamService;
    }

    // Returns true only for accounts flagged as admin. Any lookup failure counts as "not admin".
    private boolean isAdmin(Authentication authentication) {
        return adminAccount(authentication) != null;
    }

    // The caller's account if it is flagged as admin, otherwise null.
    private Account adminAccount(Authentication authentication) {
        try {
            AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
            Account account = accountService.getAccount(principal.getEmail());
            return account != null && Boolean.TRUE.equals(account.getIsAdmin()) ? account : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Current AI settings (models, daily chat limit, max tokens, reasoning effort). API keys are not exposed.
    @GetMapping("/ai-settings")
    public ResponseEntity<AiSettingsDTO> getAiSettings(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(aiSettingsService.current());
    }

    // Replaces all AI settings at once; takes effect on the next AI request.
    @PutMapping("/ai-settings")
    public ResponseEntity<AiSettingsDTO> updateAiSettings(Authentication authentication,
                                                          @RequestBody AiSettingsUpdateDTO request) {
        Account admin = adminAccount(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(aiSettingsService.update(admin, request));
    }

    // All courses with objective/task counts, for the dashboard course list.
    @GetMapping("/courses")
    public ResponseEntity<List<AdminCourseDTO>> getCourses(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AdminCourseDTO> courses = courseRepository.findAllCoursesWithCounts().stream()
                .map(row -> new AdminCourseDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        row[5] == null ? null : row[5].toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    // Full dependency graph for one course: objective nodes + prerequisite edges.
    // Datum ispita službenog predmeta (npr. ljetni rok mature); do njega se ponavljanje sažima. Prazno briše datum.
    @PutMapping("/courses/{courseId}/exam-date")
    public ResponseEntity<String> setExamDate(Authentication authentication, @PathVariable Long courseId,
                                              @RequestBody java.util.Map<String, String> body) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Course course = courseRepository.findCourseByCourseId(courseId).orElse(null);
        if (course == null) return ResponseEntity.notFound().build();
        if (course.getOwnerAccountId() != null) {
            return ResponseEntity.badRequest().body("Datum ispita vlastitog predmeta upisuje kreator.");
        }
        String value = body.get("examDate");
        try {
            course.setExamDate(value == null || value.isBlank() ? null : java.time.LocalDate.parse(value));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Neispravan datum.");
        }
        courseRepository.save(course);
        return ResponseEntity.ok(value == null ? "" : value);
    }

    // --- vlastiti predmeti kreatora (samo za čitanje) ---

    @GetMapping("/own-courses")
    public ResponseEntity<List<java.util.Map<String, Object>>> ownCourses(Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ownCourses.courses());
    }

    @GetMapping("/own-courses/{courseId}/events")
    public ResponseEntity<List<java.util.Map<String, Object>>> ownCourseEvents(Authentication authentication, @PathVariable Long courseId) {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ownCourses.events(courseId));
    }

    @GetMapping("/creators")
    public ResponseEntity<List<java.util.Map<String, Object>>> creators(Authentication authentication) {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(ownCourses.creators());
    }

    // {email, isCreator?, monthlyQuotaUsd?}: izostavljeno polje ostaje kakvo jest.
    @PutMapping("/creators")
    public ResponseEntity<Void> updateCreator(Authentication authentication, @RequestBody java.util.Map<String, Object> body) {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Object quota = body.get("monthlyQuotaUsd");
        ownCourses.updateCreator((String) body.get("email"), (Boolean) body.get("isCreator"),
                quota == null ? null : new java.math.BigDecimal(quota.toString()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/{courseId}/graph")
    public ResponseEntity<AdminCourseGraphDTO> getCourseGraph(Authentication authentication, @PathVariable Long courseId) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Course course;
        try {
            course = courseService.getCourseById(courseId);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }

        List<AdminObjectiveNodeDTO> nodes = learningObjectiveRepository.findCourseObjectiveNodes(courseId).stream()
                .map(row -> new AdminObjectiveNodeDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        row[2] != null ? ((Number) row[2]).longValue() : null,
                        (String) row[3],
                        row[4] != null ? ((Number) row[4]).longValue() : null,
                        (String) row[5],
                        ((Number) row[6]).longValue()))
                .collect(Collectors.toList());

        List<AdminPrerequisiteEdgeDTO> edges = learningObjectiveRepository.findCoursePrerequisiteEdges(courseId).stream()
                .map(row -> new AdminPrerequisiteEdgeDTO(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AdminCourseGraphDTO(course.getCourseId(), course.getCourseName(), nodes, edges));
    }

    // All mock exams of the admin's current course, unpublished ones included.
    @GetMapping("/mock-exams")
    public ResponseEntity<List<AdminMockExamSummaryDTO>> getMockExams(Authentication authentication) {
        Account admin = adminAccount(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminMockExamService.listForCourse(admin.getCurrentCourse()));
    }

    // One mock exam with answers and solution explanations, published or not.
    @GetMapping("/mock-exams/{examId}")
    public ResponseEntity<AdminMockExamDetailDTO> getMockExam(Authentication authentication, @PathVariable Long examId) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminMockExamService.getExam(examId));
    }

    // Replaces the text fields of one question (text, options, answer, explanation, notes).
    @PutMapping("/mock-exams/questions/{questionId}")
    public ResponseEntity<AdminMockExamQuestionDTO> updateMockExamQuestion(Authentication authentication,
                                                                           @PathVariable Long questionId,
                                                                           @RequestBody AdminMockExamQuestionUpdateDTO request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminMockExamService.updateQuestion(questionId, request));
    }

    // Reports of AI answers (chat and mock exam grading), newest first.
    @GetMapping("/ai-reports")
    public ResponseEntity<List<AiReportDTO>> aiReports(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(aiReportService.list());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, String>> error(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }
}
