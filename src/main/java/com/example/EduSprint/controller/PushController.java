package com.example.EduSprint.controller;

import com.example.EduSprint.security.AuthPrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.PushService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** The apps' device tokens (APNs on iOS, FCM on Android) and push settings (PushService). */
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushService push;
    private final AccountService accounts;

    public PushController(PushService push, AccountService accounts) {
        this.push = push;
        this.accounts = accounts;
    }

    /** {@code platform}: "ios" (the default, as the iOS app sends none) or "android". */
    public record DeviceRequest(String token, Boolean sandbox, String platform) {}

    @PutMapping("/device")
    public ResponseEntity<Void> register(Authentication authentication, @RequestBody DeviceRequest request) {
        String platform = request.platform() == null ? "ios" : request.platform();
        if (!validToken(request.token(), platform)) return ResponseEntity.badRequest().build();
        push.registerDevice(accountId(authentication), request.token(), Boolean.TRUE.equals(request.sandbox()), platform);
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

    /**
     * APNs device tokens are hex, 64 characters today; FCM registration tokens about 160 characters of letters, digits,
     * "_", "-" and ":". A little room for longer ones; a token goes into the path of DELETE /device/{token}.
     */
    static boolean validToken(String token, String platform) {
        if (token == null) return false;
        return switch (platform) {
            case "ios" -> token.matches("[0-9a-fA-F]{64,200}");
            case "android" -> token.matches("[A-Za-z0-9_:\\-]{100,400}");
            default -> false;
        };
    }

    private long accountId(Authentication authentication) {
        return accounts.getAccount(((AuthPrincipal) authentication.getPrincipal()).getEmail()).getAccountId();
    }
}
