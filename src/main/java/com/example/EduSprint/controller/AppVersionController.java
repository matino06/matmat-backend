package com.example.EduSprint.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Which app versions may still be used, per platform. Public: the apps ask before sign-in.
 * {@code minimum}: an older app shows only "update the app"; {@code latest}: an older one is offered the update.
 * Set on Railway (IOS_MINIMUM_VERSION, IOS_LATEST_VERSION; ANDROID_MINIMUM_VERSION, ANDROID_LATEST_VERSION) only
 * once that version is live on the App Store / Google Play; empty means no requirement.
 */
@RestController
public class AppVersionController {

    private final Versions ios;
    private final Versions android;

    public AppVersionController(@Value("${app.ios.minimum-version:}") String iosMinimum,
                                @Value("${app.ios.latest-version:}") String iosLatest,
                                @Value("${app.android.minimum-version:}") String androidMinimum,
                                @Value("${app.android.latest-version:}") String androidLatest) {
        this.ios = new Versions(blankToNull(iosMinimum), blankToNull(iosLatest));
        this.android = new Versions(blankToNull(androidMinimum), blankToNull(androidLatest));
    }

    public record Versions(String minimum, String latest) {}

    @GetMapping("/app-version/ios")
    public ResponseEntity<Versions> ios() {
        return versions(ios);
    }

    @GetMapping("/app-version/android")
    public ResponseEntity<Versions> android() {
        return versions(android);
    }

    private static ResponseEntity<Versions> versions(Versions versions) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(versions);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
