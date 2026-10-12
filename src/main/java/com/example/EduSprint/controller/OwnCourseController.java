package com.example.EduSprint.controller;

import com.example.EduSprint.dto.OwnCourseCreateDTO;
import com.example.EduSprint.dto.OwnCourseDTO;
import com.example.EduSprint.dto.OwnCourseDeleteDTO;
import com.example.EduSprint.dto.QuotaUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.OwnCourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Vlastiti predmeti kreatora. Sve rute traže račun s is_creator; tuđi predmet je 404.
@RestController
@RequestMapping("/own-courses")
public class OwnCourseController {

    private final AccountService accountService;
    private final OwnCourseService ownCourseService;

    public OwnCourseController(AccountService accountService, OwnCourseService ownCourseService) {
        this.accountService = accountService;
        this.ownCourseService = ownCourseService;
    }

    private Account account(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return accountService.getAccount(principal.getEmail());
    }

    @GetMapping
    public List<OwnCourseDTO> list(Authentication authentication) {
        return ownCourseService.list(account(authentication));
    }

    @GetMapping("/quota")
    public QuotaUsageDTO quota(Authentication authentication) {
        return ownCourseService.quota(account(authentication));
    }

    @PostMapping("/{courseId}/retry")
    public ResponseEntity<Void> retry(Authentication authentication, @PathVariable Long courseId) {
        ownCourseService.retryFailed(account(authentication), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<OwnCourseDTO> create(Authentication authentication, @RequestBody OwnCourseCreateDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownCourseService.create(account(authentication), body));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long courseId,
                                       @RequestBody OwnCourseDeleteDTO body) {
        ownCourseService.delete(account(authentication), courseId, body == null ? null : body.confirmName());
        return ResponseEntity.noContent().build();
    }
}
