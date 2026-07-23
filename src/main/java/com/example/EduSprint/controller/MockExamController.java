package com.example.EduSprint.controller;

import com.example.EduSprint.dto.MockExamAttemptDetailDTO;
import com.example.EduSprint.dto.MockExamAttemptSummaryDTO;
import com.example.EduSprint.dto.MockExamDetailDTO;
import com.example.EduSprint.dto.MockExamSubmitRequestDTO;
import com.example.EduSprint.dto.MockExamSubmitResponseDTO;
import com.example.EduSprint.dto.MockExamSummaryDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.MockExamGradingService;
import com.example.EduSprint.service.MockExamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock-exam")
public class MockExamController {

    private final MockExamService mockExamService;
    private final MockExamGradingService mockExamGradingService;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    public MockExamController(MockExamService mockExamService,
                              MockExamGradingService mockExamGradingService,
                              AccountService accountService,
                              ObjectMapper objectMapper) {
        this.mockExamService = mockExamService;
        this.mockExamGradingService = mockExamGradingService;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/available")
    public ResponseEntity<List<MockExamSummaryDTO>> getAvailableExams(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
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

    @PostMapping(value = "/{examId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MockExamSubmitResponseDTO> submit(
            @PathVariable Long examId,
            @RequestPart("data") String dataJson,
            MultipartHttpServletRequest request,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());

        MockExamSubmitRequestDTO body;
        try {
            body = objectMapper.readValue(dataJson, MockExamSubmitRequestDTO.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, MultipartFile> files = request.getFileMap();

        try {
            MockExamSubmitResponseDTO response = mockExamGradingService.submit(account, examId, body, files);
            if ("pending".equals(response.getGradingStatus())) {
                mockExamGradingService.triggerAsyncGradingForAttempt(response.getAttemptId());
            }
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/attempts")
    public ResponseEntity<List<MockExamAttemptSummaryDTO>> getMyAttempts(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        return ResponseEntity.ok(mockExamGradingService.getAttemptsForAccount(account));
    }

    @PostMapping("/attempts/{attemptId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> retryFailedAnswers(
            @PathVariable Long attemptId,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        try {
            mockExamGradingService.retryFailedAnswers(attemptId, account);
            mockExamGradingService.triggerAsyncGradingForAttempt(attemptId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<MockExamAttemptDetailDTO> getAttemptDetail(
            @PathVariable Long attemptId,
            Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        try {
            return ResponseEntity.ok(mockExamGradingService.getAttemptDetail(attemptId, account));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
