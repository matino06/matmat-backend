package com.example.EduSprint.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PushControllerTest {

    @Test
    void deviceTokensAreHex() {
        assertTrue(PushController.validToken("a1".repeat(32), "ios"));
        assertFalse(PushController.validToken("a1".repeat(10), "ios"));
        assertFalse(PushController.validToken("zz".repeat(32), "ios"));
        assertFalse(PushController.validToken(null, "ios"));
    }

    @Test
    void fcmTokensAreTheirOwnShape() {
        String fcm = "dK3x_Q1aRZ2:APA91b" + "Hq7-x".repeat(28);
        assertTrue(PushController.validToken(fcm, "android"));
        assertFalse(PushController.validToken(fcm, "ios"));
        assertFalse(PushController.validToken("short:token", "android"));
        assertFalse(PushController.validToken(fcm.replace('_', '/'), "android"));
        assertFalse(PushController.validToken("a1".repeat(32), "windows"));
    }
}
