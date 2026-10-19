package com.example.EduSprint.controller;

import com.example.EduSprint.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private final StorageService storage = mock(StorageService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ImageController(storage)).build();

    @Test
    void contentImagesStayPublic() throws Exception {
        when(storage.loadAsResource("kalkulator_complex.png")).thenReturn(new ByteArrayResource(new byte[]{1}));

        mvc.perform(get("/image/kalkulator_complex.png")).andExpect(status().isOk());
    }

    @Test
    void studentAnswerPhotosAreNotServed() throws Exception {
        mvc.perform(get("/image/mock-exam-answers/12/345.jpg")).andExpect(status().isNotFound());
        verify(storage, never()).loadAsResource(anyString());
    }

    @Test
    void encodedOrDottedPathsToPrivateKeysAreNotServed() throws Exception {
        mvc.perform(get("/image/%6Dock-exam-answers/12/345.jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/image/./mock-exam-answers/12/345.jpg")).andExpect(status().isNotFound());
        verify(storage, never()).loadAsResource(anyString());
    }

    @Test
    void contentImagesInFoldersStayPublic() throws Exception {
        when(storage.loadAsResource("tasks/graf1.png")).thenReturn(new ByteArrayResource(new byte[]{1}));

        mvc.perform(get("/image/tasks/graf1.png")).andExpect(status().isOk());
    }

    @Test
    void creatorMaterialsAreNotServed() throws Exception {
        mvc.perform(get("/image/own-courses/7/3/materials/skripta.pdf")).andExpect(status().isNotFound());
        mvc.perform(get("/image/own-courses/7/3/pages/9/1.png")).andExpect(status().isNotFound());
        verify(storage, never()).loadAsResource(anyString());
    }

    @Test
    void filesAreServedWithTheTypeOfTheirExtension() throws Exception {
        when(storage.loadAsResource("tasks/graf1.png")).thenReturn(new ByteArrayResource(new byte[]{1}));

        mvc.perform(get("/image/tasks/graf1.png")).andExpect(content().contentType("image/png"));
    }

    // Browsers ask for a video in pieces (Range: bytes=0-); without the right type the answer was 406 and the video never played.
    @Test
    void videosAreServedInPieces() throws Exception {
        when(storage.loadAsResource("fizika_matura/video/v15.mp4")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3, 4}));

        mvc.perform(get("/image/fizika_matura/video/v15.mp4").header("Range", "bytes=0-1"))
                .andExpect(status().isPartialContent())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(header().string("Content-Range", "bytes 0-1/4"));
    }
}
