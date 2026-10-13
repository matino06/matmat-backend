package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Skice u objašnjenju: model ih piše kao blokove ```skica s JSON opisom (isti tipovi kao skica zadatka), a frontend
// ih crta na tom mjestu. Svaki blok provjerava isti kod koji crta (SketchChecker); neispravan se samo izbaci, jer
// objašnjenje bez skice i dalje vrijedi. Brojevi s oznaka provjeravaju se prema tekstu objašnjenja bez blokova,
// kao na frontendu.
@Component
public class ExplanationSketches {

    static final Pattern BLOCK = Pattern.compile("```skica[ \\t]*\\r?\\n(.*?)\\r?\\n?```", Pattern.DOTALL);
    static final int MAX = 2;

    public record Result(String explanation, int kept, int dropped) {
        // npr. „, 2 skice u objašnjenju (1 izbačena)“ za izlaz posla
        String summary() {
            if (kept == 0 && dropped == 0) return "";
            return ", " + kept + (kept == 1 ? " skica" : " skice") + " u objašnjenju" + (dropped == 0 ? "" : " (" + dropped + " izbačeno)");
        }
    }

    private final SketchChecker sketches;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExplanationSketches(SketchChecker sketches) {
        this.sketches = sketches;
    }

    // Objašnjenje bez blokova skica: za provjere teksta i za brojeve koje skica smije koristiti.
    static String strip(String explanation) {
        return BLOCK.matcher(explanation == null ? "" : explanation).replaceAll("").replaceAll("\\n{3,}", "\n\n");
    }

    // Ostavlja najviše MAX ispravnih skica, svaku kao blok s JSON-om u jednom retku; ostale izbaci.
    public Result validate(String explanation) {
        String text = strip(explanation);
        Matcher m = BLOCK.matcher(explanation == null ? "" : explanation);
        StringBuilder sb = new StringBuilder();
        int kept = 0, dropped = 0;
        while (m.find()) {
            String block = "";
            try {
                JsonNode spec = mapper.readTree(m.group(1).strip());
                String json = mapper.writeValueAsString(spec);
                if (kept < MAX && spec.isObject() && sketches.problems(json, text).isEmpty()) {
                    block = "```skica\n" + json + "\n```";
                    kept++;
                } else {
                    dropped++;
                }
            } catch (Exception e) {
                dropped++;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(block));
        }
        m.appendTail(sb);
        return new Result(sb.toString().replaceAll("\\n{3,}", "\n\n"), kept, dropped);
    }
}
