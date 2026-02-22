package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.UserCourseGoal;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountObjectiveService;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.CourseService;
import com.example.EduSprint.service.UserCourseGoalService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    public final AccountRepository accountRepository;
    public final AccountObjectiveService accountObjectiveService;
    public final AccountService accountService;
    public final CourseService courseService;
    public final UserCourseGoalService userCourseGoalService;

    public AccountController(AccountRepository accountRepository, AccountService accountService, AccountObjectiveService accountObjectiveService, CourseService courseService, UserCourseGoalService userCourseGoalService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.accountObjectiveService = accountObjectiveService;
        this.courseService = courseService;
        this.userCourseGoalService = userCourseGoalService;
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

    @GetMapping("/tempo")
    public ResponseEntity<Short> getTempo(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());

        return new ResponseEntity<>(account.getTempo(), HttpStatus.OK);
    }

    @PostMapping("/tempo")
    public ResponseEntity<?> setTempo(Authentication authentication, @RequestBody Short tempo) {
        try {
            FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
            Account account = accountService.getAccount(principal.getEmail());
            account.setTempo(tempo);
            accountRepository.save(account);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>("Account not found", HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            return new ResponseEntity<>("Database error", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/goals")
    public ResponseEntity<List<UserCourseGoal>> getGoals(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());

        return new ResponseEntity<>(account.getCourseGoals(), HttpStatus.OK);
    }

    @PostMapping("/goals")
    public ResponseEntity<Void> updateUserCourseGoal(Authentication authentication, @RequestBody Map<String, Object> userCourseGoalsNew) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());

        List<UserCourseGoal> userCourseGoals = account.getCourseGoals();

        List<Map<String, Object>> courseGoals =
                (List<Map<String, Object>>) userCourseGoalsNew.get("userCourseGoalsNew");

        for (Map<String, Object> goal : courseGoals) {

            Map<String, Object> course = (Map<String, Object>) goal.get("course");
            Integer courseIdInt = (Integer) course.get("courseId");
            Long courseId = courseIdInt.longValue();
            Integer dailyGoal = (Integer) goal.get("dailyGoal");


            userCourseGoals.stream()
                    .filter(cg -> cg.getCourse().getCourseId().equals(courseId))
                    .findFirst()
                    .ifPresent(cg -> {
                        cg.setDailyGoal(dailyGoal);
                        userCourseGoalService.saveUserCourseGoal(cg);
                    });
        }

        return ResponseEntity.ok().build();

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
