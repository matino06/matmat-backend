package com.example.EduSprint.service;

import com.example.EduSprint.dto.AiChatRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
}
