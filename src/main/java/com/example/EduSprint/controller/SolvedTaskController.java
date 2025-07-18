package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.SolvedTask;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.SolvedTaskRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountObjectiveService;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
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

    public SolvedTaskController(SolvedTaskRepository solvedTaskRepository, AccountService accountService, TaskService taskService, AccountObjectiveService accountObjectiveService) {
        this.solvedTaskRepository = solvedTaskRepository;
        this.accountService = accountService;
        this.taskService = taskService;
        this.accountObjectiveService = accountObjectiveService;
    }

    @Transactional
    @PostMapping("/set-new")
    public ResponseEntity<String> saveNewSolvedTask(Authentication authentication, @RequestBody Map<String, Object> taskData) {
        try {
            FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();
            Long taskId = ((Number) taskData.get("taskId")).longValue();
            Short q = ((Number) taskData.get("q")).shortValue();
            String startTimeString = (String) taskData.get("startTime");
            String endTimeString = (String) taskData.get("endTime");

            if (q < 0 || q > 5) {
                return ResponseEntity.badRequest()
                        .body("Q has to be between 0 i 5");
            }

            // Craeting new Solved Task and Saving it
            Account account = accountService.getAccount(firebasePrincipal.getEmail());
            Task task = taskService.getTaskById(taskId);
            Instant startTime = Instant.parse(startTimeString);
            Instant endTime = Instant.parse(endTimeString);
            SolvedTask solvedTask = new SolvedTask(account, task, q, startTime, endTime);
            solvedTaskRepository.save(solvedTask);

            accountObjectiveService.updateAccountObjective(solvedTask);

            account.setCurrentTask(null);

            if (accountService.saveAccount(account)) {
                short countWeakObjectives = (short) account.getWeakObjectives().size();
                if (countWeakObjectives < account.getCountWeakObjectives() || countWeakObjectives == 5) {
                    account.setCountWeakObjectives(countWeakObjectives);
                    accountService.saveAccount(account);
                }
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
}
