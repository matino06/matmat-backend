package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Interaktivne lekcije: prompt i provjera speca istim kodom kao frontend (resources/lesson/lesson.js, paket iz
// matmat-frontend: scripts/lesson/paket-za-backend.mjs), u GraalJS-u. Ono što ovdje prođe, aplikacija sigurno
// prikaže i zna provjeriti. Kontekst se stvara jednom i koristi redom (GraalJS kontekst nije višenitni).
@Component
public class LessonUi {

    public record Check(List<String> errors, List<String> warnings) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private Context context;
    private Value lib;

    public synchronized String system() {
        return lib().getMember("sustavPrompt").execute().asString();
    }

    // izvor: subject, level, objective_name, task_text, sketch, results, explanation (kao lab-out/source.json)
    public synchronized String user(Map<String, Object> source) throws IOException {
        return call("korisnickiPrompt", mapper.writeValueAsString(source)).asString();
    }

    public synchronized String repair(List<String> errors) throws IOException {
        return call("popravakPrompt", mapper.writeValueAsString(errors)).asString();
    }

    // results: JSON rezultata izvornog zadatka ({"a":"…"}), za upozorenje kad pitanje ne odgovara zadatku; smije biti null.
    public synchronized Check check(String lessonJson, String resultsJson) {
        Value r = context().eval("js", """
                (lesson, results) => {
                  let parsed, res = null;
                  try { parsed = JSON.parse(lesson); } catch (e) { return { greske: ['lekcija nije ispravan JSON'], upozorenja: [] }; }
                  try { res = results ? JSON.parse(results) : null; } catch (e) {}
                  return MatMatLesson.validateLesson(parsed, { rezultati: res });
                }""").execute(lessonJson, resultsJson);
        return new Check(strings(r.getMember("greske")), strings(r.getMember("upozorenja")));
    }

    private Value call(String fn, String jsonArg) {
        return context().eval("js", "(name, json) => MatMatLesson[name](JSON.parse(json))").execute(fn, jsonArg);
    }

    private static List<String> strings(Value array) {
        List<String> out = new ArrayList<>();
        for (long i = 0; i < array.getArraySize(); i++) out.add(array.getArrayElement(i).asString());
        return out;
    }

    private Value lib() {
        if (lib == null) lib = context().getBindings("js").getMember("MatMatLesson");
        return lib;
    }

    private Context context() {
        if (context == null) {
            try (InputStream in = getClass().getResourceAsStream("/lesson/lesson.js")) {
                if (in == null) throw new IllegalStateException("lesson.js nije u resursima");
                String code = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Engine engine = Engine.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
                context = Context.newBuilder("js").engine(engine).build();
                context.eval("js", code);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return context;
    }
}
