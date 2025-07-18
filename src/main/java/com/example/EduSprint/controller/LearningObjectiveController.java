package com.example.EduSprint.controller;

import com.example.EduSprint.dto.ObjectiveWithStatusDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.SubfieldOfStudy;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.LearningObjectiveService;
import com.example.EduSprint.service.SubfieldOfStudyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/learning-objective")
public class LearningObjectiveController {

    public final LearningObjectiveService learningObjectiveService;
    public final SubfieldOfStudyService subfieldOfStudyService;
    public final AccountService accountService;
    private final LearningObjectiveRepository learningObjectiveRepository;

    public LearningObjectiveController(LearningObjectiveService learningObjectiveService, SubfieldOfStudyService subfieldOfStudyService, LearningObjectiveRepository learningObjectiveRepository, AccountService accountService) {
        this.learningObjectiveService = learningObjectiveService;
        this.subfieldOfStudyService = subfieldOfStudyService;
        this.accountService = accountService;
        this.learningObjectiveRepository = learningObjectiveRepository;
    }

    @GetMapping("/{subfieldId}")
    public List<LearningObjective> getLearningObjectives(@PathVariable Long subfieldId) {
        SubfieldOfStudy subfield = subfieldOfStudyService.findById(subfieldId);
        return learningObjectiveRepository.findAllBySubfield(subfield);
    }

    @GetMapping("/get-objectives-with-task")
    public List<LearningObjective> getLearningObjectivesWithTask(@RequestParam Long subfieldId) {
        List<LearningObjective> learningObjectives = learningObjectiveRepository.findObjectivesBySubfieldWithTasks(subfieldId);

        return learningObjectives;
    }

    @GetMapping("/get-objectives-with-status")
    public ResponseEntity<List<ObjectiveWithStatusDTO>> getLearningObjectivesWithStatus(Authentication authentication) {
        FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(firebasePrincipal.getEmail());

        List<Object[]> objectivesWithUnlockStatus = learningObjectiveRepository.findObjectivesWithUnlockStatus(account.getAccountId());
        List<ObjectiveWithStatusDTO> results = objectivesWithUnlockStatus.stream()
                .map(arr -> new ObjectiveWithStatusDTO((String) arr[0], (String) arr[1], (Long) arr[2], (String) arr[3], (Boolean) arr[4], (Boolean) arr[5]))
                .collect(Collectors.toList());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
