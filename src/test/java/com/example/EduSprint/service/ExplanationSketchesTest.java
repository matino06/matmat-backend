package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplanationSketchesTest {

    private final ExplanationSketches sketches = new ExplanationSketches(new SketchChecker());

    private static final String GRID = "{\"tip\":\"pravokutnik\",\"a\":6,\"b\":4,\"mreza\":1,\"oznake\":{\"a\":\"6 cm\",\"b\":\"4 cm\"}}";

    @Test
    void validSketchStaysAsOneLineBlock() {
        String explanation = "Pravokutnik 6 cm i 4 cm prekrijemo kvadratićima:\n\n```skica\n{\n  \"tip\": \"pravokutnik\", \"a\": 6, \"b\": 4, \"mreza\": 1,\n"
                + "  \"oznake\": {\"a\": \"6 cm\", \"b\": \"4 cm\"}\n}\n```\n\nU svakom redu je 6 kvadratića.";
        ExplanationSketches.Result r = sketches.validate(explanation);
        assertEquals(1, r.kept());
        assertEquals(0, r.dropped());
        assertTrue(r.explanation().contains("```skica\n" + GRID + "\n```"));
        assertEquals(", 1 skica u objašnjenju", r.summary());
    }

    @Test
    void sketchThatCannotBeDrawnOrIsNotJsonIsDroppedAndTheTextStays() {
        String explanation = "Uvod 6 cm i 4 cm.\n\n```skica\n{\"tip\":\"balon\"}\n```\n\nSredina.\n\n```skica\nnije json\n```\n\nKraj.";
        ExplanationSketches.Result r = sketches.validate(explanation);
        assertEquals(0, r.kept());
        assertEquals(2, r.dropped());
        assertEquals("Uvod 6 cm i 4 cm.\n\nSredina.\n\nKraj.", r.explanation());
    }

    @Test
    void numbersOnTheSketchMustAppearInTheExplanationItself() {
        // 6 cm i 4 cm postoje samo u JSON-u bloka, ne u tekstu objašnjenja
        ExplanationSketches.Result r = sketches.validate("Površina pravokutnika.\n\n```skica\n" + GRID + "\n```");
        assertEquals(0, r.kept());
    }

    @Test
    void atMostTwoSketchesAreKept() {
        String block = "\n\n```skica\n" + GRID + "\n```\n\n";
        ExplanationSketches.Result r = sketches.validate("Stranice 6 cm i 4 cm." + block + "a" + block + "b" + block + "c");
        assertEquals(2, r.kept());
        assertEquals(1, r.dropped());
    }

    @Test
    void sketchBlocksDoNotCountAsTextDrawing() throws Exception {
        VariationChecks checks = new VariationChecks(new KatexChecker());
        String explanation = "## 🟩 Površina\n**Kolika je površina?**\n\nStranice 6 cm i 4 cm.\n\n```skica\n" + GRID
                + "\n```\n\n## ✅ Rezultati\na) 24 cm² je površina.";
        var v = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(java.util.Map.of(
                "zadatak", "a) Kolika je površina pravokutnika?", "objasnjenje", explanation, "rezultati", java.util.Map.of("a", "24 cm²"))));
        assertTrue(checks.problems(v).isEmpty(), checks.problems(v).toString());
    }
}
