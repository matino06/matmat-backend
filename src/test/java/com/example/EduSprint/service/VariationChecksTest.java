package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Na snimljenom izlazu Sola iz prototipa (1. varijacija lekcije o akceleraciji) i njegovim pokvarenim inačicama.
class VariationChecksTest {

    private static final VariationChecks checks = new VariationChecks(new KatexChecker());
    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode recorded() throws Exception {
        return (ObjectNode) mapper.readTree(getClass().getResourceAsStream("/variations/good.json"));
    }

    @Test
    void recordedVariationPasses() throws Exception {
        assertEquals(List.of(), checks.problems(recorded()));
    }

    @Test
    void brokenKatexIsCaught() throws Exception {
        ObjectNode v = recorded();
        v.put("objasnjenje", v.get("objasnjenje").asText().replaceFirst("\\\\textcolor\\{green\\}\\{", "\\\\textcolor{green}{\\\\frac{1}{"));
        assertTrue(checks.problems(v).stream().anyMatch(p -> p.contains("KaTeX")), checks.problems(v).toString());
    }

    @Test
    void missingResultsSectionIsCaught() throws Exception {
        ObjectNode v = recorded();
        String e = v.get("objasnjenje").asText();
        v.put("objasnjenje", e.substring(0, e.indexOf("## ✅ Rezultati")));
        assertTrue(checks.problems(v).stream().anyMatch(p -> p.contains("Rezultati")));
    }

    @Test
    void fourthSubtaskIsCaught() throws Exception {
        ObjectNode v = recorded();
        v.put("zadatak", v.get("zadatak").asText() + "\n\nd) Koliko dugo automobil koči?");
        assertTrue(checks.problems(v).stream().anyMatch(p -> p.contains("podzadataka")));
        ObjectNode w = recorded();
        ((ObjectNode) w.get("rezultati")).put("d", "5 s");
        assertTrue(checks.problems(w).stream().anyMatch(p -> p.contains("podzadataka")));
    }

    @Test
    void latexOutsideFormulasForeignWordsAndLiteralNewlinesAreCaught() throws Exception {
        ObjectNode v = recorded();
        v.put("zadatak", v.get("zadatak").asText() + " Masa je 0{,}5 kg, joten\\n");
        List<String> p = checks.problems(v);
        assertTrue(p.stream().anyMatch(x -> x.contains("LaTeX izvan")), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.contains("Strani")));
        assertTrue(p.stream().anyMatch(x -> x.contains("Doslovni")));
    }

    @Test
    void graphDrawnWithCharactersIsCaughtButMarkdownTableIsNot() throws Exception {
        ObjectNode v = recorded();
        v.put("zadatak", v.get("zadatak").asText() + "\n\nm (g)\n60 |            ● A\n   |           /\n50 |          /\n   |         /\n 0 |●----------");
        assertTrue(checks.problems(v).stream().anyMatch(p -> p.contains("Crtež znakovima")));
        ObjectNode w = recorded();
        w.put("zadatak", w.get("zadatak").asText() + "\n\n| V (cm³) | m (g) |\n|---|---|\n| 10 | 30 |\n| 20 | 60 |\n| 30 | 90 |");
        assertTrue(checks.problems(w).stream().noneMatch(p -> p.contains("Crtež znakovima")), checks.problems(w).toString());
    }
}
