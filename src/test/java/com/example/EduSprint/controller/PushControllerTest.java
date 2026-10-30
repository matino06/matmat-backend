package com.example.EduSprint.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PushControllerTest {

    @Test
    void deviceTokensAreHex() {
        assertTrue(PushController.validToken("a1".repeat(32)));
        assertFalse(PushController.validToken("a1".repeat(10)));
        assertFalse(PushController.validToken("zz".repeat(32)));
        assertFalse(PushController.validToken(null));
    }
}
