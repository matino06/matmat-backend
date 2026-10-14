package com.example.EduSprint.storage;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Storage keys that belong to one account and must never be served by key: students' answer photos
 * from mock exams and creators' own-course materials (and their rendered pages). They are reachable only
 * through short-lived presigned URLs from endpoints that check the account.
 */
public final class PrivateStorageKeys {

    private static final List<String> PRIVATE_PREFIXES = List.of("mock-exam-answers/", "own-courses/");

    private PrivateStorageKeys() {
    }

    /**
     * True for a private key, and for anything that is not a plain key: empty, or with an empty, "." or ".."
     * path segment (an HTTP client may normalise those, so the signed key and the fetched object could differ).
     */
    public static boolean isPrivate(String key) {
        if (key == null || key.isBlank()) return true;
        // Whether the key arrives decoded depends on the servlet setup, so judge the decoded form too.
        return isPrivateAsIs(key) || isPrivateAsIs(decode(key));
    }

    private static String decode(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static boolean isPrivateAsIs(String key) {
        if (key == null || key.isBlank()) return true;
        String k = key;
        while (k.startsWith("/")) k = k.substring(1);
        for (String segment : k.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return true;
        }
        for (String prefix : PRIVATE_PREFIXES) {
            if (k.startsWith(prefix)) return true;
        }
        return false;
    }
}
