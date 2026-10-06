package com.example.EduSprint.controller;

import com.example.EduSprint.dto.AiChatRequestDTO;
import com.example.EduSprint.dto.AiRatingRequestDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.AiChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AccountService accountService;

    public AiChatController(AiChatService aiChatService, AccountService accountService) {
        this.aiChatService = aiChatService;
        this.accountService = accountService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AiChatRequestDTO request, Authentication authentication) {
        return aiChatService.chat(currentAccount(authentication), request);
    }

    @PostMapping("/message/{messageId}/rating")
    public ResponseEntity<Void> rate(@PathVariable Long messageId,
                                     @RequestBody AiRatingRequestDTO request,
                                     Authentication authentication) {
        aiChatService.rate(currentAccount(authentication), messageId, request.rating());
        return ResponseEntity.noContent().build();
    }

    private Account currentAccount(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        Account account = accountService.getAccount(principal.getEmail());
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return account;
    }
}
