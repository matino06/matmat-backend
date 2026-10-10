package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiChatRequestDTO;
import com.example.EduSprint.entity.ExplanationStep;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.Task;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void decodesImageDataUris() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        String b64 = Base64.getEncoder().encodeToString(png);
        assertArrayEquals(png, AiChatService.decodeImageDataUri("data:image/png;base64," + b64));
        // prijelomi redaka u base64 dijelu se toleriraju
        assertArrayEquals(png, AiChatService.decodeImageDataUri("data:image/jpeg;base64," + b64.substring(0, 4) + "\r\n" + b64.substring(4)));
        assertNull(AiChatService.decodeImageDataUri("data:text/plain;base64," + b64));
        assertNull(AiChatService.decodeImageDataUri("data:image/png;base64,!!!nije-base64!!!"));
        assertNull(AiChatService.decodeImageDataUri("data:image/png;base64," + "A".repeat(12_000_000)));
        assertNull(AiChatService.decodeImageDataUri(null));
    }

    @Test
    void quotedImageNamesAreKeptInUserMessage() {
        AiChatRequestDTO.Quote quote = new AiChatRequestDTO.Quote("solution", null,
                List.of("https://api.matmat.online/api/image/valjak_skica_i_oplosje.png"));
        assertEquals("Označio sam sliku iz rješenja: valjak_skica_i_oplosje.png\n\nŠto slika prikazuje?",
                AiChatService.buildUserContent("Što slika prikazuje?", quote, ""));
        assertEquals("Pitanje", AiChatService.buildUserContent("Pitanje", null, ""));
    }

    @Test
    void levelComesFromCourseName() {
        assertEquals("viša (A)", AiChatService.examLevel("Matematika A razina"));
        assertEquals("osnovna (B)", AiChatService.examLevel("Matematika B razina"));
        assertNull(AiChatService.examLevel("Fizika"));
        assertNull(AiChatService.examLevel("Ekonomska Matematika EFZG"));
        assertNull(AiChatService.examLevel(null));
    }

    @Test
    void taskContextUsesTheLabelsFromTheBasePrompt() {
        LearningObjective objective = new LearningObjective();
        objective.setObjectiveName("Logaritamske jednadžbe");
        Task task = new Task();
        task.setObjective(objective);
        task.setTaskText("Riješi \\(\\log(x + 3) + \\log x = 1\\).");
        task.setExplanation("Uvjeti: \\(x > 0\\). Rješenje je \\(x = 2\\).");
        ExplanationStep step = new ExplanationStep();
        step.setStepNumber((short) 1);
        step.setExplanation("Zapiši uvjete.");

        String prompt = AiChatService.buildSystemPrompt("BASE", "SUBJECT", "osnovna (B)", task, List.of(step), null, false);

        assertTrue(prompt.startsWith("BASE\n\nSUBJECT"));
        assertTrue(prompt.contains("RAZINA: osnovna (B)"));
        assertTrue(prompt.contains("CILJ UČENJA: Logaritamske jednadžbe"));
        assertTrue(prompt.contains("ZADATAK:\nRiješi"));
        assertTrue(prompt.contains("OBJAŠNJENJE:\nUvjeti"));
        assertTrue(prompt.contains("KORACI OBJAŠNJENJA:\n1. Zapiši uvjete."));
        assertTrue(prompt.contains("UČENIK JE OTVORIO RJEŠENJE: ne"));
        assertTrue(AiChatService.buildSystemPrompt("BASE", null, null, task, List.of(), null, true)
                .contains("UČENIK JE OTVORIO RJEŠENJE: da"));
        // Bez podatka (npr. stariji frontend) model odgovara kao prije.
        assertFalse(AiChatService.buildSystemPrompt("BASE", null, null, task, List.of(), null, null)
                .contains("OTVORIO RJEŠENJE"));
    }

    @Test
    void examContextHasNoTaskParts() {
        AiChatRequestDTO.Quote quote = new AiChatRequestDTO.Quote("exam", "Pitanje 3…", List.of());
        String prompt = AiChatService.buildSystemPrompt("BASE", null, null, null, List.of(), quote, false);
        assertTrue(prompt.contains("probne državne mature"));
        assertFalse(prompt.contains("ZADATAK:"));
        assertFalse(prompt.contains("RAZINA:"));
        assertFalse(prompt.contains("OTVORIO RJEŠENJE"));
    }
}
