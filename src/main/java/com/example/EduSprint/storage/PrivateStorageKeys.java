package com.example.EduSprint.storage;

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

    /** True for a private key, and for anything that is not a plain key (empty, "..", leading slashes). */
    public static boolean isPrivate(String key) {
        if (key == null || key.isBlank() || key.contains("..")) return true;
        String k = key;
        while (k.startsWith("/")) k = k.substring(1);
        for (String prefix : PRIVATE_PREFIXES) {
            if (k.startsWith(prefix)) return true;
        }
        return false;
    }
}
