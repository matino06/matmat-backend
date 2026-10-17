package com.example.EduSprint.security;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.AiConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects AI requests (chat, own-course building, re-grading with AI) without AI consent: 403 {"message": …}, which
 * says where to turn it on ({@code X-MatMat-Client: ios}: Profil; the web: Postavke).
 */
@Component
public class AiConsentInterceptor implements HandlerInterceptor {

    public static final String CLIENT_HEADER = "X-MatMat-Client";

    private final AccountService accountService;
    private final AiConsentService consents;

    public AiConsentInterceptor(AccountService accountService, AiConsentService consents) {
        this.accountService = accountService;
        this.consents = consents;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equals(request.getMethod())) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) return true;
        Account account = accountService.getAccount(principal.getEmail());
        if (!consents.isGranted(account.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AiConsentService.missingMessage(request.getHeader(CLIENT_HEADER)));
        }
        return true;
    }
}
