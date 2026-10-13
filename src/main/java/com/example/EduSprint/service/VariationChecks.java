package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Provjere varijacije kodom (ticket „Kako se generiraju zadaci“, prototip loop_luna.code_checks i sol4_write.checks).
// Ako ijedna ne prođe, varijacija se izrađuje ponovno.
@Component
public class VariationChecks {

    static final int MAX_SUBTASKS = 3;
    private static final Pattern RESULTS = Pattern.compile("##\\s*✅\\s*Rezultati\\s*\\n(.*)$", Pattern.DOTALL);
    private static final Pattern FOURTH_SUBTASK = Pattern.compile("(?m)^\\s*(\\*\\*)?d\\)");
    private static final Pattern RAW_LATEX = Pattern.compile(".{0,20}(?:\\{,\\}|\\\\text|\\\\cdot|\\\\frac).{0,10}");
    private static final Pattern FOREIGN_SCRIPT = Pattern.compile("\\S*[\\u0400-\\u04FF\\u3000-\\u9FFF\\uAC00-\\uD7AF]\\S*");
    // riječi koje su se u mjerenjima pojavile usred hrvatskog teksta (finski)
    private static final Pattern FOREIGN_WORDS = Pattern.compile("\\b(?:joten|olla|yhdessä|ratkaistaan|ensin|että|kuin|eli|noin)\\b");

    private final KatexChecker katex;

    public VariationChecks(KatexChecker katex) {
        this.katex = katex;
    }

    // Popis problema; prazan znači da je varijacija prošla.
    public List<String> problems(JsonNode v) {
        String task = v.path("zadatak").asText("");
        String explanation = ExplanationSketches.strip(v.path("objasnjenje").asText("")); // skice provjerava ExplanationSketches
        List<String> out = new ArrayList<>();
        if (task.isBlank() || explanation.isBlank()) {
            out.add("Nedostaje zadatak ili objašnjenje.");
            return out;
        }
        String text = task + "\n\n" + explanation;
        List<String> broken = katex.errors(text);
        if (!broken.isEmpty()) out.add("Formule koje se ne mogu prikazati (KaTeX): " + String.join("; ", broken.subList(0, Math.min(3, broken.size()))));
        Matcher r = RESULTS.matcher(explanation);
        if (!r.find() || r.group(1).strip().length() < 10) out.add("Nedostaje ili je prazan zadnji odjeljak „## ✅ Rezultati“.");
        if (v.path("rezultati").size() > MAX_SUBTASKS || FOURTH_SUBTASK.matcher(task).find()) {
            out.add("Previše podzadataka; najviše su tri, a), b), c).");
        }
        List<String> raw = rawLatex(text);
        if (!raw.isEmpty()) out.add("LaTeX izvan formula: " + raw.subList(0, Math.min(2, raw.size())));
        String all = text + "\n" + v.path("rezultati");
        List<String> foreign = new ArrayList<>(find(FOREIGN_SCRIPT, all));
        foreign.addAll(find(FOREIGN_WORDS, all));
        if (!foreign.isEmpty()) out.add("Strani jezik ili pismo: " + foreign.subList(0, Math.min(4, foreign.size())));
        if (all.contains("\\n")) out.add("Doslovni \\n u tekstu.");
        if (textDrawing(text)) out.add("Crtež znakovima u tekstu (graf ili shema); podatke treba zadati tablicom ili riječima.");
        return out;
    }

    // Graf ili shema „nacrtana“ znakovima: blok koda ili barem tri retka koja su većinom crtice, kose crte i točke.
    static boolean textDrawing(String text) {
        if (text.contains("```")) return true;
        int lines = 0;
        for (String line : text.split("\n")) {
            String t = line.strip();
            if (t.length() < 4 || t.startsWith("|") && t.endsWith("|")) continue; // markdown tablica je dopuštena
            long filled = t.chars().filter(ch -> !Character.isWhitespace(ch)).count();
            long drawn = t.chars().filter(ch -> "|/\\_-●○*.".indexOf(ch) >= 0).count();
            if (drawn * 2 >= filled && t.matches(".*[|/\\●].*")) lines++;
        }
        return lines >= 3;
    }

    // LaTeX naredbe izvan \( \), $$ $$ i $ $.
    static List<String> rawLatex(String text) {
        String s = text.replaceAll("(?s)\\$\\$.*?\\$\\$", "").replaceAll("(?s)\\\\\\(.*?\\\\\\)", "").replaceAll("\\$[^$\\n]+\\$", "");
        return find(RAW_LATEX, s);
    }

    private static List<String> find(Pattern p, String s) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) out.add(m.group());
        return out;
    }
}
