package com.example.EduSprint.controller;


import com.example.EduSprint.dto.ProgressSummaryDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.LearningObjectiveService;
import com.example.EduSprint.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/progress")
public class ProgressController {
    public final ProgressService progressService;
    public final AccountService accountService;
    public final LearningObjectiveService learningObjectiveService;

    public ProgressController(ProgressService progressService, AccountService accountService, LearningObjectiveService learningObjectiveService) {
        this.progressService = progressService;
        this.accountService = accountService;
        this.learningObjectiveService = learningObjectiveService;
    }

    @GetMapping
    public ResponseEntity<ProgressSummaryDTO> getSummary(Authentication authentication) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(authPrincipal.getEmail());

        ProgressSummaryDTO summary = progressService.getProgressSummary(account);
        return ResponseEntity.ok(summary);
    }

    // Exam readiness (0-100) for the account's current course, weighted by the exam catalogue.
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Short>> getReadiness(Authentication authentication) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(authPrincipal.getEmail());

        return ResponseEntity.ok(Map.of("readiness", learningObjectiveService.calculateExamProgress(account)));
    }
}
