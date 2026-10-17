package com.example.EduSprint.security;

import com.example.EduSprint.controller.ApiExceptionHandler;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.AiConsentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiConsentInterceptorTest {

    @RestController
    static class Ai {
        @PostMapping(value = "/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String chat() { return "data: ok\n\n"; }

        @GetMapping("/ai/usage")
        String usage() { return "{}"; }
    }

    private final AccountService accounts = mock(AccountService.class);
    private final AiConsentService consents = mock(AiConsentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "accountId", 43L);
        when(accounts.getAccount("ucenik@example.com")).thenReturn(account);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal("google-oauth2|1", "ucenik@example.com", "Učenik"), null, Collections.emptyList()));
        mvc = MockMvcBuilders.standaloneSetup(new Ai())
                .addMappedInterceptors(new String[]{"/ai/chat", "/ai/usage"}, new AiConsentInterceptor(accounts, consents))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void iosWithoutConsentIsRejectedWithAMessage() throws Exception {
        when(consents.isGranted(43L)).thenReturn(false);
        mvc.perform(post("/ai/chat").header(AiConsentInterceptor.CLIENT_HEADER, "ios").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden());
        mvc.perform(post("/ai/chat").header(AiConsentInterceptor.CLIENT_HEADER, "ios"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Za AI uključi dozvolu u Profilu."));
    }

    @Test
    void iosWithConsentGoesThrough() throws Exception {
        when(consents.isGranted(43L)).thenReturn(true);
        mvc.perform(post("/ai/chat").header(AiConsentInterceptor.CLIENT_HEADER, "ios")).andExpect(status().isOk());
    }

    @Test
    void theWebWithoutConsentIsRejectedAndToldToUsePostavke() throws Exception {
        when(consents.isGranted(43L)).thenReturn(false);
        mvc.perform(post("/ai/chat"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Za AI uključi dozvolu u Postavkama."));
    }

    @Test
    void theWebWithConsentGoesThrough() throws Exception {
        when(consents.isGranted(43L)).thenReturn(true);
        mvc.perform(post("/ai/chat")).andExpect(status().isOk());
    }

    @Test
    void readingIsAllowed() throws Exception {
        when(consents.isGranted(43L)).thenReturn(false);
        mvc.perform(get("/ai/usage").header(AiConsentInterceptor.CLIENT_HEADER, "ios")).andExpect(status().isOk());
    }
}
