package com.example.EduSprint.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Provjera opisa skice istim kodom kojim ga frontend crta (resources/sketch/taskSketch.js, kopija iz
// matmat-frontend src/lib/sketch/taskSketch.js), u GraalJS-u. Ono što prođe ovdje, aplikacija sigurno nacrta.
// Kontekst se stvara jednom i koristi redom (GraalJS kontekst nije višenitni).
@Component
public class SketchChecker {

    private Context context;
    private Value check;

    // Problemi opisa (prazno = skica se može nacrtati). text = zadatak, objašnjenje i rezultati.
    public synchronized List<String> problems(String specJson, String text) {
        Value result = check().execute(specJson, text == null ? "" : text);
        List<String> out = new ArrayList<>();
        for (long i = 0; i < result.getArraySize(); i++) out.add(result.getArrayElement(i).asString());
        return out;
    }

    private Value check() {
        if (check == null) {
            try (InputStream in = getClass().getResourceAsStream("/sketch/taskSketch.js")) {
                if (in == null) throw new IllegalStateException("taskSketch.js nije u resursima");
                // ES modul → skripta: izvozi postaju globalne funkcije
                String code = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("export const ", "const ").replace("export function ", "function ");
                Engine engine = Engine.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
                context = Context.newBuilder("js").engine(engine).build();
                context.eval("js", code);
                check = context.eval("js", """
                        (spec, text) => {
                          let parsed;
                          try { parsed = JSON.parse(spec); } catch (e) { return ['opis skice nije ispravan JSON']; }
                          const r = renderSketch(parsed, text);
                          if (r.svg) return [];
                          const bad = r.checks.filter(c => !c.ok).map(c => c.text);
                          return bad.length ? bad : ['skica se ne može nacrtati'];
                        }""");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return check;
    }
}
