package com.example.EduSprint.controller;

import com.example.EduSprint.dto.StudyMapFieldDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.FieldOfStudy;
import com.example.EduSprint.repository.FieldOfStudyRepository;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.LearningObjectiveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/field-of-study")
public class FieldOfStudyController {
    public final FieldOfStudyRepository fieldOfStudyRepository;
    public final LearningObjectiveService learningObjectiveService;
    public final AccountService accountService;

    public FieldOfStudyController(FieldOfStudyRepository fieldOfStudyRepository, LearningObjectiveService learningObjectiveService, AccountService accountService) {
        this.fieldOfStudyRepository = fieldOfStudyRepository;
        this.learningObjectiveService = learningObjectiveService;
        this.accountService = accountService;
    }

    @GetMapping
    public List<FieldOfStudy> getAllFieldsOfStudy() {
        return fieldOfStudyRepository.findAll();
    }

    @GetMapping("/map")
    public ResponseEntity<List<StudyMapFieldDTO>> getStudyMap(Authentication authentication) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(authPrincipal.getEmail());
        List<StudyMapFieldDTO> studyMap = learningObjectiveService.getStudyMap(account);

        return new ResponseEntity<>(studyMap, HttpStatus.OK);
    }
}
