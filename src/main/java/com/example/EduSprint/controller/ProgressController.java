package com.example.EduSprint.controller;


import com.example.EduSprint.dto.ProgressSummaryDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/progress")
public class ProgressController {
    public final ProgressService progressService;
    public final AccountService accountService;

    public ProgressController(ProgressService progressService, AccountService accountService) {
        this.progressService = progressService;
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ProgressSummaryDTO> getSummary(Authentication authentication) {
        FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(firebasePrincipal.getEmail());

        ProgressSummaryDTO summary = progressService.getProgressSummary(account);
        return ResponseEntity.ok(summary);
    }
}
