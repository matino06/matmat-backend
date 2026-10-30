package com.example.EduSprint.controller;

import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.PushService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** The iOS app's device token and push settings (PushService). */
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushService push;
    private final AccountService accounts;

    public PushController(PushService push, AccountService accounts) {
        this.push = push;
        this.accounts = accounts;
    }

    public record DeviceRequest(String token, Boolean sandbox) {}

    @PutMapping("/device")
    public ResponseEntity<Void> register(Authentication authentication, @RequestBody DeviceRequest request) {
        if (!validToken(request.token())) return ResponseEntity.badRequest().build();
        push.registerDevice(accountId(authentication), request.token(), Boolean.TRUE.equals(request.sandbox()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/device/{token}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable String token) {
        push.removeDevice(accountId(authentication), token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public PushService.Settings settings(Authentication authentication) {
        return push.settings(accountId(authentication));
    }

    @PutMapping("/settings")
    public ResponseEntity<Void> saveSettings(Authentication authentication, @RequestBody PushService.Settings settings) {
        Integer minute = settings.reminderMinute();
        if (minute != null && (minute < 0 || minute > 1439)) return ResponseEntity.badRequest().build();
        push.saveSettings(accountId(authentication), settings);
        return ResponseEntity.noContent().build();
    }

    /** APNs device tokens are hex, 64 characters today; a little room for longer ones. */
    static boolean validToken(String token) {
        return token != null && token.matches("[0-9a-fA-F]{64,200}");
    }

    private long accountId(Authentication authentication) {
        return accounts.getAccount(((AuthPrincipal) authentication.getPrincipal()).getEmail()).getAccountId();
    }
}
