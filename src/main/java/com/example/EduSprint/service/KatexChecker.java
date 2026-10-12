package com.example.EduSprint.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Prikazuje li KaTeX sve formule u tekstu: katex.min.js u GraalJS-u, bez Nodea na serveru.
// Kontekst se stvara jednom (učitavanje KaTeX-a traje) i koristi redom, jer GraalJS kontekst nije višenitni.
@Component
public class KatexChecker {

    static final Pattern FORMULA = Pattern.compile("\\$\\$([\\s\\S]+?)\\$\\$|\\\\\\(([\\s\\S]+?)\\\\\\)");

    private Context context;
    private Value render;

    // Greške za formule koje se ne mogu prikazati (izvor formule :: poruka), prazno ako je sve u redu.
    public synchronized List<String> errors(String text) {
        List<String> errors = new ArrayList<>();
        Matcher m = FORMULA.matcher(text);
        while (m.find()) {
            boolean display = m.group(1) != null;
            String source = display ? m.group(1) : m.group(2);
            try {
                render().execute(source, display);
            } catch (PolyglotException e) {
                errors.add(cut(source.strip(), 80) + " :: " + cut(String.valueOf(e.getMessage()), 80));
            }
        }
        return errors;
    }

    private Value render() {
        if (render == null) {
            try (var in = getClass().getResourceAsStream("/katex/katex.min.js")) {
                if (in == null) throw new IllegalStateException("katex.min.js nije u resursima");
                Engine engine = Engine.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
                context = Context.newBuilder("js").engine(engine).build();
                context.eval(Source.newBuilder("js", new InputStreamReader(in, StandardCharsets.UTF_8), "katex.min.js").build());
                render = context.eval("js", "(src, display) => katex.renderToString(src, {throwOnError: true, displayMode: display})");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return render;
    }

    private static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }
}
