package com.example.EduSprint.controller;

import com.example.EduSprint.dto.SavedRatingDTO;
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
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<?> saveNewSolvedTask(Authentication authentication, @RequestBody Map<String, Object> taskData) {
        try {
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            Long taskId = ((Number) taskData.get("taskId")).longValue();
            Short grade = ((Number) taskData.get("grade")).shortValue();
            String startTimeString = (String) taskData.get("startTime");
            String endTimeString = (String) taskData.get("endTime");
            String device = (String) taskData.get("device");

            if (grade < 1 || grade > 4) {
                return ResponseEntity.badRequest()
                        .body("Grade has to be between 1 and 4");
            }

            // Craeting new Solved Task and Saving it
            Account account = accountService.getAccount(authPrincipal.getEmail());
            Course course = account.getCurrentCourse();
            // Tempo više ne utječe na otključavanje; starije aplikacije ga još šalju, a stupac ga samo bilježi.
            Short tempo = taskData.get("tempo") instanceof Number t ? t.shortValue() : account.getTempo();
            Task task = taskService.getTaskById(taskId);
            Instant startTime = Instant.parse(startTimeString);
            Instant endTime = Instant.parse(endTimeString);
            Short currentProgress = learningObjectiveService.calculateExamProgress(account, task.getObjective(), grade);

            SolvedTask solvedTask = new SolvedTask(account, task, grade, startTime, endTime, course, currentProgress, device, tempo);
            solvedTaskRepository.save(solvedTask);

            SavedRatingDTO saved = savedRating(solvedTask);
            accountObjectiveService.updateAccountObjective(solvedTask);
            try {
                variations.onSolved(taskId); // vlastiti predmet: izradi sljedeću varijaciju zadatka
            } catch (RuntimeException e) {
                System.out.println("Sljedeća varijacija nije pokrenuta: " + e.getMessage());
            }

            account.setCurrentTask(null);

            if (accountService.saveAccount(account)) {
                return ResponseEntity.ok(saved);
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

    // Učenik mijenja ocjenu koju je upravo spremio (interaktivna lekcija sprema predloženu ocjenu čim je gotova).
    // Lekcija se vrati u stanje prije te ocjene pa se primijeni nova, kao da je odmah odabrao nju. 409 ako se ocjena
    // više ne smije mijenjati (vidi AccountObjectiveService.canChangeGrade).
    @Transactional
    @PutMapping("/{solvedTaskId}/grade")
    public ResponseEntity<?> changeGrade(Authentication authentication, @PathVariable Long solvedTaskId, @RequestBody Map<String, Object> body) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(authPrincipal.getEmail());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!(body.get("grade") instanceof Number number) || number.intValue() < 1 || number.intValue() > 4) {
            return ResponseEntity.badRequest().body("Grade has to be between 1 and 4");
        }
        short grade = number.shortValue();

        SolvedTask solved = solvedTaskRepository.findById(solvedTaskId)
                .filter(s -> s.getAccount().getAccountId().equals(account.getAccountId()))
                .orElse(null);
        if (solved == null) {
            return ResponseEntity.notFound().build();
        }
        if (!accountObjectiveService.canChangeGrade(solved, Instant.now())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This rating can no longer be changed");
        }

        accountObjectiveService.restoreBefore(solved);
        if (solved.getGrade() != grade) {
            solved.setGrade(grade);
            solved.setProgress(learningObjectiveService.calculateExamProgress(account, solved.getTask().getObjective(), grade));
            solvedTaskRepository.save(solved);
        }
        SavedRatingDTO saved = savedRating(solved);
        accountObjectiveService.updateAccountObjective(solved);
        return ResponseEntity.ok(saved);
    }

    // Prije nego se ocjena primijeni na lekciju: dani za svaku ocjenu računaju se od stanja prije nje.
    private SavedRatingDTO savedRating(SolvedTask solved) {
        short[] days = accountObjectiveService.previewIntervals(solved.getAccount(), solved.getTask().getObjective(),
                solved.getEndTime(), solved.getCourse());
        return new SavedRatingDTO(solved.getId(), solved.getGrade(), days);
    }

    // Days until the task's objective comes back for each grade 1..4 (index grade - 1), so the rating
    // picker can show the real FSRS interval before the user commits to a rating.
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

    // Is the task's objective already rated (a review) or new? An interactive lesson suggests a stricter rating on a
    // review, where needing the steps means the student forgot.
    @GetMapping("/review-state/{taskId}")
    public ResponseEntity<Map<String, Boolean>> getReviewState(Authentication authentication, @PathVariable Long taskId) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(authPrincipal.getEmail());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("review", accountObjectiveService.isReview(account, task.getObjective())));
    }
}
