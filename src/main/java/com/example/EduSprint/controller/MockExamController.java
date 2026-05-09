package com.example.EduSprint.controller;

import com.example.EduSprint.dto.MockExamDetailDTO;
import com.example.EduSprint.dto.MockExamSummaryDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.MockExamService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mock-exam")
public class MockExamController {

    private final MockExamService mockExamService;
    private final AccountService accountService;

    public MockExamController(MockExamService mockExamService, AccountService accountService) {
        this.mockExamService = mockExamService;
        this.accountService = accountService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<MockExamSummaryDTO>> getAvailableExams(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        return ResponseEntity.ok(mockExamService.getAvailableExamsForAccount(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockExamDetailDTO> getExamById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mockExamService.getExamById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
