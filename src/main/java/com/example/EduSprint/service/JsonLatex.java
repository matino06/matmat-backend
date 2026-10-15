package com.example.EduSprint.service;

import java.util.Set;

// LaTeX u JSON-u koji piše model: backslash često nije udvostručen. „\(“ je tada neispravan JSON (parser javlja
// „Unrecognized character escape“), a „\frac“, „\theta“, „\times“ su ispravni escapeovi koji se tiho pročitaju kao
// form feed, tab… pa se formula pokvari. Popravak udvostruči backslash unutar stringa kad ne počinje ispravan JSON
// escape ili kad počinje poznatu LaTeX naredbu; ispravno udvostručeni „\\“ ostaje kakav jest.
final class JsonLatex {

    // Naredbe koje počinju slovom ispravnog JSON escapea (b, f, n, r, t, u). Bez \nu, \ne i sličnih kratkih, jer bi
    // se zamijenile s novim retkom ispred riječi („\nu tablici“).
    private static final Set<String> COMMANDS = Set.of(
            "frac", "dfrac", "tfrac", "forall", "flat",
            "text", "textbf", "textit", "textrm", "times", "theta", "tau", "tan", "tanh", "to", "triangle", "tilde",
            "beta", "bar", "binom", "bf", "big", "Big", "bigg", "boxed", "bullet", "bmod",
            "neq", "nabla", "not", "newline", "nolimits",
            "rho", "right", "rightarrow", "rm", "rfloor", "rangle", "rceil",
            "underline", "uparrow", "upsilon", "underbrace");

    private JsonLatex() {
    }

    static String fix(String json) {
        StringBuilder out = new StringBuilder(json.length() + 16);
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (!inString) {
                if (c == '"') inString = true;
                out.append(c);
                continue;
            }
            if (c == '"') {
                inString = false;
                out.append(c);
            } else if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '\\' || next == '"' || next == '/' || (validEscape(json, i + 1) && !command(json, i + 1))) {
                    out.append(c).append(next); // ispravan escape: prepiše se netaknut
                    i++;
                } else {
                    out.append("\\\\"); // LaTeX: backslash postaje doslovni znak
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean validEscape(String s, int at) {
        char e = s.charAt(at);
        if ("bfnrt".indexOf(e) >= 0) return true;
        if (e != 'u' || at + 4 >= s.length()) return false;
        for (int k = at + 1; k <= at + 4; k++) if (Character.digit(s.charAt(k), 16) < 0) return false;
        return true;
    }

    private static boolean command(String s, int at) {
        int end = at;
        while (end < s.length() && isAsciiLetter(s.charAt(end))) end++;
        return COMMANDS.contains(s.substring(at, end));
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
