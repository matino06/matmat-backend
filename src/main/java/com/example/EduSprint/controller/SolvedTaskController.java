package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.SolvedTask;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.SolvedTaskRepository;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountObjectiveService;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.LearningObjectiveService;
import com.example.EduSprint.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/solved-task")
public class SolvedTaskController {

    public final SolvedTaskRepository solvedTaskRepository;
    public final AccountService accountService;
    public final TaskService taskService;
    public final AccountObjectiveService accountObjectiveService;
    public final LearningObjectiveService learningObjectiveService;

    private final com.example.EduSprint.service.OwnCourseVariationService variations;

    public SolvedTaskController(SolvedTaskRepository solvedTaskRepository, AccountService accountService, TaskService taskService, AccountObjectiveService accountObjectiveService, LearningObjectiveService learningObjectiveService,
                                com.example.EduSprint.service.OwnCourseVariationService variations) {
        this.variations = variations;
        this.solvedTaskRepository = solvedTaskRepository;
        this.accountService = accountService;
        this.taskService = taskService;
        this.accountObjectiveService = accountObjectiveService;
        this.learningObjectiveService = learningObjectiveService;
    }

    @Transactional
    @PostMapping("/set-new")
    public ResponseEntity<String> saveNewSolvedTask(Authentication authentication, @RequestBody Map<String, Object> taskData) {
        try {
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            Long taskId = ((Number) taskData.get("taskId")).longValue();
            Short q = ((Number) taskData.get("q")).shortValue();
            String startTimeString = (String) taskData.get("startTime");
            String endTimeString = (String) taskData.get("endTime");
            String device = (String) taskData.get("device");
            Short tempo = ((Number) taskData.get("tempo")).shortValue();

            if (q < 0 || q > 5) {
                return ResponseEntity.badRequest()
                        .body("Q has to be between 0 i 5");
            }

            // Craeting new Solved Task and Saving it
            Account account = accountService.getAccount(authPrincipal.getEmail());
            Course course = account.getCurrentCourse();
            Task task = taskService.getTaskById(taskId);
            Instant startTime = Instant.parse(startTimeString);
            Instant endTime = Instant.parse(endTimeString);
            Short currentProgress = learningObjectiveService.calculateExamProgress(account, task.getObjective(), q);

            SolvedTask solvedTask = new SolvedTask(account, task, q, startTime, endTime, course, currentProgress, device, tempo);
            solvedTaskRepository.save(solvedTask);

            accountObjectiveService.updateAccountObjective(solvedTask);
            try {
                variations.onSolved(taskId); // vlastiti predmet: izradi sljedeću varijaciju zadatka
            } catch (RuntimeException e) {
                System.out.println("Sljedeća varijacija nije pokrenuta: " + e.getMessage());
            }

            account.setCurrentTask(null);

            if (accountService.saveAccount(account)) {
                return new ResponseEntity<>("New Solved Task Successfully Saved!", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Something went wrong!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (DateTimeParseException e) {
            return new ResponseEntity<>("Invalid timestamp format (use ISO-8601)", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("An unexpected error occurred on the server");
        }
    }

    // Days until the task's objective comes back for each rating q = 0..5, so the rating picker
    // can show the real SM-2 interval before the user commits to a rating.
    @GetMapping("/next-intervals/{taskId}")
    public ResponseEntity<short[]> getNextIntervals(Authentication authentication, @PathVariable Long taskId) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(authPrincipal.getEmail());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        short[] intervals = accountObjectiveService.previewIntervals(account, task.getObjective());
        if (intervals == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(intervals);
    }
}
