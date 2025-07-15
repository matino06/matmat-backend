package com.example.EduSprint.controller;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountObjectiveService;
import com.example.EduSprint.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    public final AccountRepository accountRepository;
    public final AccountObjectiveService accountObjectiveService;
    public final AccountService accountService;

    public AccountController(AccountRepository accountRepository, AccountService accountService, AccountObjectiveService accountObjectiveService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.accountObjectiveService = accountObjectiveService;
    }

    @GetMapping("/exists")
    public ResponseEntity<String> accountExists(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        if (!accountRepository.existsByEmail(principal.getEmail())) {
            return new ResponseEntity<>("Account does not exist", HttpStatus.OK);
        }
        return new ResponseEntity<>("Account exists", HttpStatus.OK);

    }

    @PostMapping("/create")
    public ResponseEntity<String> createAccount(Authentication authentication) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        boolean accountCreated = accountService.createAccount(principal.getEmail(), principal.getName());

        if (accountCreated) {
            return new ResponseEntity<>("Account created", HttpStatus.CREATED);
        }
        return new ResponseEntity<>("Account creation failed", HttpStatus.BAD_REQUEST);
    }
}
