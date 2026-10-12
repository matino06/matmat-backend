package com.example.EduSprint.service;

import com.example.EduSprint.dto.BuildJob;
import com.example.EduSprint.storage.MaterialStorage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MaterialStepsTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final MaterialStorage storage = mock(MaterialStorage.class);
    private final BuildJobQueue queue = mock(BuildJobQueue.class);
    private final OwnCourseLlm llm = mock(OwnCourseLlm.class);

    private static File twoPagePdf() throws Exception {
        File f = Files.createTempFile("test", ".pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= 2; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                    cs.newLineAtOffset(72, 700);
                    cs.showText("Stranica " + i + ": F = ma");
                    cs.endText();
                }
            }
            doc.save(f);
        }
        return f;
    }

    @Test
    void pdfIsSplitIntoPageImagesWithTextLayerAndReadJobs() throws Exception {
        BuildJob job = new BuildJob(1, 5, 7, SplitMaterialStep.KIND, null, 1, true, 1, "{\"materialId\":9}");
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(new HashMap<>(Map.of(
                "s3_key", "own-courses/7/5/materials/abc.pdf", "content_type", "application/pdf")));
        when(jdbc.queryForList(startsWith("SELECT 1"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(List.of());
        when(jdbc.queryForList(contains("INSERT INTO material_page"), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(100L), List.of(101L));
        when(storage.download(anyString())).thenReturn(twoPagePdf());

        String out = new SplitMaterialStep(jdbc, storage, queue).run(job);

        assertEquals("2 stranica", out);
        verify(storage).put(eq("own-courses/7/5/pages/9/1.png"), any(byte[].class), eq("image/png"));
        verify(storage).put(eq("own-courses/7/5/pages/9/2.png"), any(byte[].class), eq("image/png"));
        verify(jdbc, times(2)).queryForList(contains("INSERT INTO material_page"),
                argThat((SqlParameterSource p) -> String.valueOf(p.getValue("t")).contains("F = ma")), eq(Long.class));
        verify(queue).enqueue(5, 7, ReadPageStep.KIND, "stranica 1 materijala 9", 1, true, "{\"pageId\":100}");
        verify(queue).enqueue(5, 7, ReadPageStep.KIND, "stranica 2 materijala 9", 1, true, "{\"pageId\":101}");
    }

    @Test
    void photoIsOnePageReadFromTheOriginal() throws Exception {
        BuildJob job = new BuildJob(1, 5, 7, SplitMaterialStep.KIND, null, 1, true, 1, "{\"materialId\":9}");
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(new HashMap<>(Map.of(
                "s3_key", "own-courses/7/5/materials/x.jpg", "content_type", "image/jpeg")));
        when(jdbc.queryForList(contains("INSERT INTO material_page"), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(100L));

        assertEquals("1 stranica", new SplitMaterialStep(jdbc, storage, queue).run(job));
        verify(storage, never()).download(anyString());
        verify(queue).enqueue(5, 7, ReadPageStep.KIND, "stranica 1 materijala 9", 1, true, "{\"pageId\":100}");
    }

    @Test
    void readPageSavesMarkdownAndKind() throws Exception {
        BuildJob job = new BuildJob(2, 5, 7, ReadPageStep.KIND, null, 1, true, 1, "{\"pageId\":100}");
        Map<String, Object> page = new HashMap<>();
        page.put("image_key", "own-courses/7/5/pages/9/1.png");
        page.put("text_layer", "Stranica 1: F = ma");
        when(jdbc.queryForMap(anyString(), any(SqlParameterSource.class))).thenReturn(page);
        when(storage.bytes(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(llm.chatWithImage(eq(job), eq(ReadPageStep.MODEL), anyString(), contains("TEKSTNI SLOJ"), any(), eq("image/png"), any()))
                .thenReturn(new OwnCourseLlm.Result("```json\n{\"markdown\":\"Stranica 1: \\\\(F = ma\\\\)\",\"vrsta\":\"Skripta\"}\n```",
                        new BigDecimal("0.0004"), 0));

        assertEquals("skripta", new ReadPageStep(jdbc, storage, llm).run(job));
        verify(jdbc).update(contains("UPDATE material_page"),
                argThat((SqlParameterSource p) -> "Stranica 1: \\(F = ma\\)".equals(p.getValue("md")) && "skripta".equals(p.getValue("k"))));
    }

    @Test
    void unknownKindBecomesOstalo() {
        assertEquals("rjesenja", ReadPageStep.normaliseKind("rješenja"));
        assertEquals("ispit", ReadPageStep.normaliseKind(" Ispit "));
        assertEquals("ostalo", ReadPageStep.normaliseKind("naslovnica"));
        assertEquals("ostalo", ReadPageStep.normaliseKind(null));
        assertEquals("image/jpeg", ReadPageStep.mimeType("a/b.jpg"));
        assertEquals("image/png", ReadPageStep.mimeType("a/b.PNG"));
    }
}
