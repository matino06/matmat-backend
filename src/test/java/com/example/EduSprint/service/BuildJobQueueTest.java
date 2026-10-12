package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildJobQueueTest {

    @Test
    void jobFailsForGoodAfterThreeAttempts() {
        assertEquals(BuildJobQueue.WAITING, BuildJobQueue.statusAfterFailure(1));
        assertEquals(BuildJobQueue.WAITING, BuildJobQueue.statusAfterFailure(2));
        assertEquals(BuildJobQueue.FAILED, BuildJobQueue.statusAfterFailure(3));
    }

    @Test
    void retriesBackOff() {
        assertEquals(Duration.ofSeconds(30), BuildJobQueue.backoff(1));
        assertEquals(Duration.ofSeconds(60), BuildJobQueue.backoff(2));
        assertEquals(Duration.ofSeconds(30), BuildJobQueue.backoff(0));
    }
}
