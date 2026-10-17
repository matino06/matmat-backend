package com.example.EduSprint.dto;

/** Body of DELETE /account. The iOS app sends a fresh Sign in with Apple code, so the backend can revoke Apple access. */
public record AccountDeleteRequest(String appleAuthorizationCode) {
}
