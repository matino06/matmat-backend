package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiChatServiceTest {

    @Test
    void extractsImgSourcesFromTaskHtml() {
        String html = "Zadan je graf <img src=\"https://api.matmat.online/api/image/graf1.png\" alt=\"g\"> i "
                + "<IMG alt='x' SRC='slika 2.jpg'/> tekst";
        assertEquals(List.of("https://api.matmat.online/api/image/graf1.png", "slika 2.jpg"),
                AiChatService.extractImageSources(html));
        assertEquals(List.of(), AiChatService.extractImageSources(null));
    }

    @Test
    void acceptsOnlyOwnStorageImages() {
        assertEquals("graf1.png", AiChatService.toStorageKey("https://api.matmat.online/api/image/graf1.png?v=2"));
        assertEquals("tasks/slika 2.jpg", AiChatService.toStorageKey("/api/image/tasks/slika%202.jpg"));
        assertEquals("graf1.png", AiChatService.toStorageKey("graf1.png"));
        assertNull(AiChatService.toStorageKey("https://evil.example.com/x.png"));
        assertNull(AiChatService.toStorageKey("data:image/png;base64,AAAA"));
        assertNull(AiChatService.toStorageKey("/api/image/../secret"));
        assertNull(AiChatService.toStorageKey(null));
    }
}
