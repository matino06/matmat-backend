package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Kemijski widgeti za maturu (samo ručno pisane lekcije): svaki primjer iz kataloga, složen u lekcije, mora proći
// provjeru i u GraalJS-u, a ne samo u pregledniku. Datoteku generira frontend iz primjera kataloga.
class LessonUiKemijaTest {

    private static final LessonUi ui = new LessonUi();

    @Test
    void everyChemistryWidgetExamplePassesValidation() throws Exception {
        JsonNode lekcije;
        try (InputStream in = getClass().getResourceAsStream("/lesson/kemija-widgeti.json")) {
            lekcije = new ObjectMapper().readTree(in);
        }
        assertTrue(lekcije.size() >= 50, "premalo lekcija: " + lekcije.size());
        List<String> greske = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = lekcije.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            LessonUi.Check c = ui.check(e.getValue().toString(), null, null);
            if (!c.ok()) greske.add(e.getKey() + ": " + c.errors());
        }
        assertEquals(List.of(), greske);
    }
}
