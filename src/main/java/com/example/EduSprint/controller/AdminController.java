package com.example.EduSprint.controller;

import com.example.EduSprint.dto.AdminCourseDTO;
import com.example.EduSprint.dto.AdminCourseGraphDTO;
import com.example.EduSprint.dto.AdminObjectiveNodeDTO;
import com.example.EduSprint.dto.AdminPrerequisiteEdgeDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountService;
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

    public AdminController(AccountService accountService, CourseService courseService,
                          CourseRepository courseRepository, LearningObjectiveRepository learningObjectiveRepository) {
        this.accountService = accountService;
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.learningObjectiveRepository = learningObjectiveRepository;
    }

    // Returns true only for accounts flagged as admin. Any lookup failure counts as "not admin".
    private boolean isAdmin(Authentication authentication) {
        try {
            FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
            Account account = accountService.getAccount(principal.getEmail());
            return account != null && Boolean.TRUE.equals(account.getIsAdmin());
        } catch (RuntimeException e) {
            return false;
        }
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
                        ((Number) row[4]).longValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    // Full dependency graph for one course: objective nodes + prerequisite edges.
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
}
