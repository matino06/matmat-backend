package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.TaskRepository;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.*;
import com.example.EduSprint.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final AccountService accountService;
    private final StorageService storageService;
    private final LearningObjectiveService learningObjectiveService;
    private final ExplanationStepService explanationStepService;

    public TaskController(TaskRepository taskRepository, TaskService taskService, AccountService accountService, StorageService storageService, LearningObjectiveService learningObjectiveService, ExplanationStepService explanationStepService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.accountService = accountService;
        this.storageService = storageService;
        this.learningObjectiveService = learningObjectiveService;
        this.explanationStepService = explanationStepService;
    }

    @GetMapping("/get-new")
    public ResponseEntity getNewTask(Authentication authentication) {
        try {
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            String email = authPrincipal.getEmail();
            Account account = accountService.getAccount(email);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Task task = account.getCurrentTask();

            if (task == null) {
                task = taskService.getNewTask(account);
                account.setCurrentTask(task);
                accountService.saveAccount(account);
            }

            if (task == null) {
                return ResponseEntity.ok("No more tasks for today!");
            }

            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Optional<Task> task = taskRepository.findById(id);

        if (task.isPresent()) {
            return ResponseEntity.ok(task.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/get-new-from-field/{fieldId}")
    public ResponseEntity getNewTaskFromField(Authentication authentication, @PathVariable Long fieldId) {
        try {
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            String email = authPrincipal.getEmail();
            Account account = accountService.getAccount(email);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Task task = taskService.getNewTaskFromField(account, fieldId);

            if (task == null) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/get-from-objective/{objectiveId}")
    public ResponseEntity getTaskFromObjective(Authentication authentication, @PathVariable Long objectiveId) {
        try {
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            String email = authPrincipal.getEmail();
            Account account = accountService.getAccount(email);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Task task = taskService.getTaskFromObjective(account, objectiveId);

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Get All Tasks with given ObjectiveId
    @GetMapping("/get-all-tasks")
    public ResponseEntity<List<Task>> getAllTasks(@RequestParam Long objectiveId) {
        return new ResponseEntity<>(taskRepository.findTasksByObjectiveId(objectiveId), HttpStatus.OK);
    }

    @Transactional
    @PostMapping("/create-new")
    public ResponseEntity<String> createNewTask(
            Authentication authentication,
            @RequestParam("taskText") String taskText,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("learningObjectiveId") String learningObjectiveId,
            @RequestParam(value = "explanation", required = true) String explanation,
            @RequestParam(value = "stepsImages", required = false) MultipartFile[] stepsImages
    ) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        System.out.println(principal.getEmail());
        if (!principal.getEmail().equals("matino0546@gmail.com")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            LearningObjective learningObjective = learningObjectiveService.findById(Long.parseLong(learningObjectiveId));

            Task task = new Task();

            task.setTaskText(taskText);
            task.setObjective(learningObjective);
            task.setExplanation(explanation);

            if (image != null && !image.isEmpty()) {
                storageService.store(image);
            }

            taskRepository.save(task);

            if (stepsImages != null) {
                for (MultipartFile stepImage : stepsImages) {
                    storageService.store(stepImage);
                }
            }

            return new ResponseEntity<>("Task created", HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println("Sve" + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
