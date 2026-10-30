package com.example.EduSprint.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Which iOS app versions may still be used. Public: the app asks before sign-in.
 * {@code minimum}: an older app shows only "update the app"; {@code latest}: an older one is offered the update.
 * Set on Railway (IOS_MINIMUM_VERSION, IOS_LATEST_VERSION) only once that version is live on the App Store;
 * empty means no requirement.
 */
@RestController
public class AppVersionController {

    private final String minimum;
    private final String latest;

    public AppVersionController(@Value("${app.ios.minimum-version:}") String minimum,
                                @Value("${app.ios.latest-version:}") String latest) {
        this.minimum = blankToNull(minimum);
        this.latest = blankToNull(latest);
    }

    public record IosVersions(String minimum, String latest) {}

    @GetMapping("/app-version/ios")
    public ResponseEntity<IosVersions> ios() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(new IosVersions(minimum, latest));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
