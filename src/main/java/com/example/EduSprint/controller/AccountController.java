package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountObjectiveService;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    public final AccountRepository accountRepository;
    public final AccountObjectiveService accountObjectiveService;
    public final AccountService accountService;
    public final CourseService courseService;

    public AccountController(AccountRepository accountRepository, AccountService accountService, AccountObjectiveService accountObjectiveService, CourseService courseService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.accountObjectiveService = accountObjectiveService;
        this.courseService = courseService;
    }

    @GetMapping("/exists")
    public ResponseEntity<String> accountExists(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        if (!accountRepository.existsByEmail(principal.getEmail())) {
            System.out.println("Ne postoji");
            return new ResponseEntity<>("Account does not exist", HttpStatus.OK);
        }
        return new ResponseEntity<>("Account exists", HttpStatus.OK);
    }

    @GetMapping("/notification-settings")
    public ResponseEntity<Map<String, Boolean>> getNotificationSettings(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Boolean> settings = new HashMap<>();
        settings.put("learningRemindersEnabled", account.isLearningRemindersEnabled());
        settings.put("featureAnnouncementsEnabled", account.isFeatureAnnouncementsEnabled());
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/notification-settings")
    public ResponseEntity<Void> updateNotificationSettings(Authentication authentication, @RequestBody Map<String, Boolean> settings) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Boolean learningReminders = settings.get("learningRemindersEnabled");
        Boolean featureAnnouncements = settings.get("featureAnnouncementsEnabled");
        if (learningReminders == null || featureAnnouncements == null) {
            return ResponseEntity.badRequest().build();
        }
        accountService.updateNotificationSettings(principal.getEmail(), learningReminders, featureAnnouncements);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create")
    public ResponseEntity<String> createAccount(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        boolean accountCreated = accountService.createAccount(principal.getEmail(), principal.getName());

        if (accountCreated) {
            return new ResponseEntity<>("Account created", HttpStatus.CREATED);
        }
        return new ResponseEntity<>("Account creation failed", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/current-course")
    public ResponseEntity<Course> getCurrentCourse(Authentication authentication) {
        FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();
        String email = firebasePrincipal.getEmail();
        Account account = accountService.getAccount(email);

        if (account == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Course course = account.getCurrentCourse();
        return new ResponseEntity<>(course, HttpStatus.OK);
    }

    @PostMapping("/current-course")
    public ResponseEntity<?> updateCourse(Authentication authentication, @RequestBody Map<String, Object> courseData) {
        FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();
        String email = firebasePrincipal.getEmail();
        Account account = accountService.getAccount(email);

        if (account == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Long courseId = Long.parseLong(courseData.get("courseId").toString());
        Course course = courseService.getCourseById(courseId);
        account.setCurrentCourse(course);
        account.setCurrentTask(null);
        accountRepository.save(account);

        return ResponseEntity.ok().build();
    }
}
