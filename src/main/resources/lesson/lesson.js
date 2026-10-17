// GENERIRANO iz matmat-frontend (scripts/lesson/paket-za-backend.mjs). Ne mijenjati ručno.
var MatMatLesson = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // <stdin>
  var stdin_exports = {};
  __export(stdin_exports, {
    VERZIJA: () => VERZIJA,
    korisnickiPrompt: () => korisnickiPrompt,
    popravakPrompt: () => popravakPrompt,
    predlozakPrompt: () => predlozakPrompt,
    sustavPrompt: () => sustavPrompt,
    validateLesson: () => validateLesson
  });

  // src/lib/sketch/taskSketch.js
  var W = 380;
  var H = 260;
  var PAD = 34;
  var esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  var f = (n) => Math.round(n * 10) / 10;
  var num = (n) => String(Math.round(n * 1e3) / 1e3).replace(".", ",");
  var line = (a, b, cls = "ln") => `<line class="${cls}" x1="${f(a[0])}" y1="${f(a[1])}" x2="${f(b[0])}" y2="${f(b[1])}"/>`;
  var label = (p, t, cls = "lbl", anchor = "middle") => `<text class="${cls}" x="${f(p[0])}" y="${f(p[1])}" text-anchor="${anchor}" dominant-baseline="middle">${esc(t)}</text>`;
  var dot = (p, cls = "pt") => `<circle class="${cls}" cx="${f(p[0])}" cy="${f(p[1])}" r="3.5"/>`;
  var poly = (pts, cls) => `<polygon class="${cls}" points="${pts.map((p) => p.map(f).join(",")).join(" ")}"/>`;
  var arrow = (a, b, cls) => `<line class="arr ${cls}" x1="${f(a[0])}" y1="${f(a[1])}" x2="${f(b[0])}" y2="${f(b[1])}" marker-end="url(#mm-head-${cls})"/>`;
  var defs = `<defs>${["acc", "c1", "c2", "c3", "c4"].map((c) => `<marker id="mm-head-${c}" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path class="head ${c}" d="M0,0 L10,5 L0,10 z"/></marker>`).join("")}</defs>`;
  var svg = (body, h = H) => `<svg viewBox="0 0 ${W} ${h}" xmlns="http://www.w3.org/2000/svg" role="img">${defs}${body}</svg>`;
  function fit(points, w = W, h = H, pad = PAD) {
    const xs = points.map((p) => p[0]);
    const ys = points.map((p) => p[1]);
    const [x0, x1, y0, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...ys), Math.max(...ys)];
    const s = Math.min((w - 2 * pad) / (x1 - x0 || 1), (h - 2 * pad) / (y1 - y0 || 1));
    const ox = (w - s * (x1 - x0)) / 2;
    const oy = (h - s * (y1 - y0)) / 2;
    return (p) => [ox + (p[0] - x0) * s, h - (oy + (p[1] - y0) * s)];
  }
  function axesFit([x0, x1], [y0, y1]) {
    const l = 48, r = 18, t = 18, b = 34;
    return (p) => [l + (p[0] - x0) / (x1 - x0) * (W - l - r), H - b - (p[1] - y0) / (y1 - y0) * (H - t - b)];
  }
  var away = (p, c, d = 16) => {
    const dx = p[0] - c[0];
    const dy = p[1] - c[1];
    const l = Math.hypot(dx, dy) || 1;
    return [p[0] + dx / l * d, p[1] + dy / l * d];
  };
  var mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
  function sideLabel(p, q, g, t, cls) {
    const m = mid(p, q);
    let n = [-(q[1] - p[1]), q[0] - p[0]];
    const l = Math.hypot(...n) || 1;
    n = [n[0] / l, n[1] / l];
    if (n[0] * (m[0] - g[0]) + n[1] * (m[1] - g[1]) < 0) n = [-n[0], -n[1]];
    const anchor = n[0] > 0.35 ? "start" : n[0] < -0.35 ? "end" : "middle";
    return label([m[0] + n[0] * 16, m[1] + n[1] * 18], t, cls, anchor);
  }
  function niceStep(range) {
    const raw = range / 6;
    const p = Math.pow(10, Math.floor(Math.log10(raw)));
    for (const k of [1, 2, 5, 10]) if (k * p >= raw) return k * p;
    return 10 * p;
  }
  var ticks = (a, b, step) => {
    const out = [];
    for (let v = Math.ceil(a / step - 1e-9) * step; v <= b + 1e-9; v += step) out.push(Math.round(v / step) * step);
    return out;
  };
  function normalize(text) {
    return String(text ?? "").replace(/\\[,;: ]/g, "").replace(/\{,\}/g, ",").replace(/(\d)\.(\d)/g, "$1,$2").replace(/(\d)\s(?=\d{3}\b)/g, "$1");
  }
  function numbersInText(labels, text) {
    const norm = normalize(text);
    const out = [];
    for (const l of labels) {
      for (const n of String(l).match(/\d+(?:[.,]\d+)?/g) ?? []) {
        const v = n.replace(".", ",");
        const ok = new RegExp(`(^|[^\\d,])${v.replace(",", "\\,")}(?![\\d])`).test(norm);
        out.push({ ok, text: `broj ${n} iz oznake „${l}“ ${ok ? "postoji" : "NE postoji"} u zadatku ili rješenju` });
      }
    }
    return out;
  }
  var fail = (text) => ({ ok: false, text });
  var positive = (...vs) => vs.every((v) => typeof v === "number" && isFinite(v) && v > 0);
  function triangle(s, text) {
    const { a, b, c } = s.stranice ?? {};
    if (!positive(a, b, c)) return { checks: [fail("stranice trokuta moraju biti pozitivni brojevi")] };
    const checks = [];
    const ok3 = a + b > c && a + c > b && b + c > a;
    checks.push({ ok: ok3, text: `stranice ${a}, ${b} i ${c} ${ok3 ? "čine" : "NE čine"} trokut` });
    if (s.pravi_kut) {
      const [p, q, h] = s.pravi_kut === "C" ? [a, b, c] : s.pravi_kut === "B" ? [a, c, b] : [b, c, a];
      const ok = Math.abs(p * p + q * q - h * h) < 0.02 * h * h;
      checks.push({ ok, text: `pravi kut u ${s.pravi_kut}: ${p}² + ${q}² ${ok ? "=" : "≠"} ${h}²` });
    }
    checks.push(...numbersInText(Object.values(s.oznake ?? {}), text));
    if (checks.some((x) => !x.ok)) return { checks };
    const A = [0, 0];
    const B = [c, 0];
    const cx = (b * b + c * c - a * a) / (2 * c);
    const C = [cx, Math.sqrt(Math.max(0, b * b - cx * cx))];
    const T = fit([A, B, C]);
    const [pA, pB, pC] = [T(A), T(B), T(C)];
    const g = [(pA[0] + pB[0] + pC[0]) / 3, (pA[1] + pB[1] + pC[1]) / 3];
    let body = poly([pA, pB, pC], "shape");
    if (s.pravi_kut === "C") {
      const u = (p) => {
        const d = [p[0] - pC[0], p[1] - pC[1]];
        const l = Math.hypot(...d);
        return [d[0] / l * 14, d[1] / l * 14];
      };
      const [ua, ub] = [u(pA), u(pB)];
      body += `<path class="ln thin" d="M${f(pC[0] + ua[0])},${f(pC[1] + ua[1])} L${f(pC[0] + ua[0] + ub[0])},${f(pC[1] + ua[1] + ub[1])} L${f(pC[0] + ub[0])},${f(pC[1] + ub[1])}"/>`;
    }
    if (s.kutovi?.["α"]) {
      const r = 26;
      const t2 = Math.atan2(-(pC[1] - pA[1]), pC[0] - pA[0]);
      body += `<path class="ln thin acc" d="M${f(pA[0] + r)},${f(pA[1])} A${r},${r} 0 0 0 ${f(pA[0] + r * Math.cos(t2))},${f(pA[1] - r * Math.sin(t2))}"/>`;
      body += label([pA[0] + r + 6, pA[1] - 9], `α = ${s.kutovi["α"]}`, "lbl small acc-t", "start");
    }
    body += label(away(pA, g), "A") + label(away(pB, g), "B") + label(away(pC, g), "C");
    const side = (p, q, key) => s.oznake?.[key] ? sideLabel(p, q, g, `${key} = ${s.oznake[key]}`, "lbl dim-t") : "";
    body += side(pB, pC, "a") + side(pA, pC, "b") + side(pA, pB, "c");
    return { svg: svg(body), checks };
  }
  function circle(s, text) {
    const r = s.r;
    const L = s.tetiva?.duljina;
    if (!positive(r)) return { checks: [fail("polumjer mora biti pozitivan broj")] };
    const checks = [];
    if (L) checks.push({ ok: L <= 2 * r, text: `tetiva ${L} ${L <= 2 * r ? "≤" : ">"} promjer ${2 * r}` });
    checks.push(...numbersInText(Object.values(s.oznake ?? {}), text));
    if (checks.some((x) => !x.ok)) return { checks };
    const T = fit([[-r, -r], [r, r]]);
    const sc = T([1, 0])[0] - T([0, 0])[0];
    const pS = T([0, 0]);
    let body = `<circle class="shape" cx="${f(pS[0])}" cy="${f(pS[1])}" r="${f(r * sc)}"/>` + dot(pS) + label([pS[0], pS[1] - 14], "S");
    if (L) {
      const d = Math.sqrt(r * r - (L / 2) ** 2);
      const [pA, pB, pM] = [T([-L / 2, -d]), T([L / 2, -d]), T([0, -d])];
      body += line(pA, pB, "ln acc") + line(pS, pA) + line(pS, pM, "ln dash") + dot(pA) + dot(pB);
      body += label(away(pA, pS), "A") + label(away(pB, pS), "B");
      if (s.oznake?.r) body += sideLabel(pS, pA, [pS[0] + 40, pS[1] + 40], `r = ${s.oznake.r}`, "lbl dim-t");
      if (s.oznake?.tetiva) body += label([pM[0], pM[1] + 16], `${s.oznake.tetiva}`, "lbl acc-t");
      if (s.oznake?.d) body += label([pS[0] + 8, (pS[1] + pM[1]) / 2], `d = ${s.oznake.d}`, "lbl dim-t", "start");
    } else {
      const pR = T([r, 0]);
      body += line(pS, pR);
      if (s.oznake?.r) body += label([(pS[0] + pR[0]) / 2, pS[1] - 12], `r = ${s.oznake.r}`, "lbl dim-t");
    }
    return { svg: svg(body), checks };
  }
  function compile(expr) {
    const js = String(expr).replace(/\^/g, "**").replace(/(\d)\s*([a-z(])/g, "$1*$2").replace(/\)\s*\(/g, ")*(");
    if (!/^[0-9x+\-*/().,\s]*$/.test(js.replace(/\b(sqrt|sin|cos|abs)\b/g, ""))) throw new Error("izraz funkcije nije dopušten");
    return new Function("x", `const {sqrt, sin, cos, abs} = Math; return (${js.replace(/,/g, ".")});`);
  }
  function axes(T, [x0, x1], [y0, y1], xName, yName) {
    const sx = niceStep(x1 - x0);
    const sy = niceStep(y1 - y0);
    let body = "";
    for (const x of ticks(x0, x1, sx)) body += line(T([x, y0]), T([x, y1]), "grid");
    for (const y of ticks(y0, y1, sy)) body += line(T([x0, y]), T([x1, y]), "grid");
    const ox = x0 <= 0 && x1 >= 0 ? 0 : x0;
    const oy = y0 <= 0 && y1 >= 0 ? 0 : y0;
    body += line(T([x0, oy]), T([x1, oy]), "axis") + line(T([ox, y0]), T([ox, y1]), "axis");
    for (const x of ticks(x0, x1, sx)) if (x !== ox) body += label([T([x, oy])[0], T([x, oy])[1] + 12], num(x), "tick");
    for (const y of ticks(y0, y1, sy)) if (y !== oy) body += label([T([ox, y])[0] - 8, T([ox, y])[1]], num(y), "tick", "end");
    body += label([T([x1, oy])[0], T([x1, oy])[1] + 24], xName ?? "x", "tick", "end");
    body += label([T([ox, y1])[0] + 8, T([ox, y1])[1] + 2], yName ?? "y", "tick", "start");
    return body;
  }
  function graph(s, text) {
    if (!Array.isArray(s.x) || !Array.isArray(s.y) || !(s.x[1] > s.x[0]) || !(s.y[1] > s.y[0])) {
      return { checks: [fail("raspon osi mora biti [od, do] uz od < do")] };
    }
    const fns = (s.funkcije ?? []).map((fn) => ({ ...fn, f: compile(fn.izraz) }));
    if (!fns.length) return { checks: [fail("graf nema nijednu funkciju")] };
    const checks = (s.tocke ?? []).map((p) => {
      const fn = fns.find((q) => q.oznaka === p.na) ?? fns[0];
      const ok = Math.abs(fn.f(p.x) - p.y) < 1e-6 * Math.max(1, Math.abs(p.y));
      return { ok, text: `točka (${num(p.x)}, ${num(p.y)}) ${ok ? "leži" : "NE leži"} na ${fn.oznaka ?? "f"}(x) = ${fn.izraz}` };
    });
    checks.push(...numbersInText((s.tocke ?? []).filter((p) => p.zadana).map((p) => `${p.x} ${p.y}`), text));
    if (checks.some((x) => !x.ok)) return { checks };
    const T = axesFit(s.x, s.y);
    let body = axes(T, s.x, s.y, s.os_x, s.os_y);
    fns.forEach((fn, i) => {
      const pts = [];
      for (let k = 0; k <= 240; k++) {
        const x = s.x[0] + (s.x[1] - s.x[0]) * k / 240;
        const y = fn.f(x);
        if (isFinite(y) && y >= s.y[0] - 1e-9 && y <= s.y[1] + 1e-9) pts.push(T([x, y]));
      }
      if (pts.length < 2) return;
      body += `<polyline class="curve c${i + 1}" points="${pts.map((p) => p.map(f).join(",")).join(" ")}"/>`;
      const end = pts[pts.length - 1];
      if (fn.oznaka) body += label([end[0] - 6, end[1] + (end[1] < 30 ? 14 : -10)], fn.oznaka, `lbl c${i + 1}-t`, "end");
    });
    for (const p of s.tocke ?? []) {
      const q = T([p.x, p.y]);
      body += dot(q, "pt acc-f");
      if (p.oznaka) body += label([q[0] + 8, q[1] + (q[1] < 40 ? 12 : -12)], p.oznaka, "lbl small", "start");
    }
    return { svg: svg(body), checks };
  }
  function coords(s, text) {
    if (!Array.isArray(s.x) || !Array.isArray(s.y) || !(s.x[1] > s.x[0]) || !(s.y[1] > s.y[0])) {
      return { checks: [fail("raspon osi mora biti [od, do] uz od < do")] };
    }
    const P = Object.fromEntries((s.tocke ?? []).map((p) => [p.oznaka, p]));
    const checks = [];
    for (const m of s.polovista ?? []) {
      const [a, b] = (m.od ?? []).map((k) => P[k]);
      const ok = !!(a && b && P[m.tocka]) && P[m.tocka].x === (a.x + b.x) / 2 && P[m.tocka].y === (a.y + b.y) / 2;
      checks.push({ ok, text: `${m.tocka} ${ok ? "jest" : "NIJE"} polovište dužine ${(m.od ?? []).join("")}` });
    }
    for (const [a, b] of s.duzine ?? []) {
      if (!P[a] || !P[b]) checks.push(fail(`dužina ${a}${b} spaja točku koja ne postoji`));
    }
    checks.push(...numbersInText((s.tocke ?? []).filter((p) => p.zadana).map((p) => `${p.x} ${p.y}`), text));
    if (checks.some((x) => !x.ok)) return { checks };
    const T = fit([[s.x[0], s.y[0]], [s.x[1], s.y[1]]]);
    let body = axes(T, s.x, s.y, s.os_x, s.os_y);
    for (const [a, b] of s.duzine ?? []) body += line(T([P[a].x, P[a].y]), T([P[b].x, P[b].y]), "ln acc");
    for (const p of s.tocke ?? []) {
      const q = T([p.x, p.y]);
      body += dot(q, p.zadana ? "pt" : "pt acc-f");
      body += label([q[0] + 6, q[1] - 13], `${p.oznaka}(${p.zadana ? `${num(p.x)}, ${num(p.y)}` : "?"})`, "lbl small", "start");
    }
    return { svg: svg(body), checks };
  }
  function rectangle(s, text) {
    const { a, b, mreza: mreza2 } = s;
    if (!positive(a, b)) return { checks: [fail("stranice pravokutnika moraju biti pozitivni brojevi")] };
    const checks = [...numbersInText(Object.values(s.oznake ?? {}), text)];
    if (mreza2) {
      const n = a / mreza2 * (b / mreza2);
      checks.push({ ok: positive(mreza2) && n <= 900, text: `mreža ${num(mreza2)}: ${Math.round(n)} polja (najviše 900)` });
    }
    if (checks.some((x) => !x.ok)) return { checks };
    const T = fit([[0, 0], [a, b]], W, H, 52);
    let body = "";
    if (mreza2) {
      for (let x = mreza2; x < a - 1e-9; x += mreza2) body += line(T([x, 0]), T([x, b]), "grid strong");
      for (let y = mreza2; y < b - 1e-9; y += mreza2) body += line(T([0, y]), T([a, y]), "grid strong");
    }
    body = poly([T([0, 0]), T([a, 0]), T([a, b]), T([0, b])], "shape") + body + poly([T([0, 0]), T([a, 0]), T([a, b]), T([0, b])], "outline");
    if (s.oznake?.a) body += label([T([a / 2, 0])[0], T([a / 2, 0])[1] + 16], s.oznake.a, "lbl dim-t");
    if (s.oznake?.b) body += label([T([a, b / 2])[0] + 8, T([a, b / 2])[1]], s.oznake.b, "lbl dim-t", "start");
    return { svg: svg(body), checks };
  }
  function cuboid(s, text) {
    const { a, b, c, razina: razina2 } = s;
    if (!positive(a, b, c)) return { checks: [fail("bridovi kvadra moraju biti pozitivni brojevi")] };
    const checks = [...numbersInText(Object.values(s.oznake ?? {}), text)];
    if (razina2 != null) checks.push({ ok: razina2 > 0 && razina2 <= c, text: `razina ${num(razina2)} ${razina2 <= c ? "≤" : ">"} visina ${num(c)}` });
    if (checks.some((x) => !x.ok)) return { checks };
    const k = Math.SQRT1_2 * 0.5;
    const P = (x, y, z) => [x + y * k, z + y * k];
    const corners = [];
    for (const x of [0, a]) for (const y of [0, b]) for (const z of [0, c]) corners.push(P(x, y, z));
    const T = fit(corners, W, H, 40);
    const p = (x, y, z) => T(P(x, y, z));
    let body = "";
    if (razina2 != null) {
      const h = razina2;
      body += poly([p(0, 0, 0), p(a, 0, 0), p(a, b, 0), p(a, b, h), p(0, b, h), p(0, 0, h)], "liquid");
      body += poly([p(0, 0, h), p(a, 0, h), p(a, b, h), p(0, b, h)], "liquid top");
    }
    body += line(p(0, b, 0), p(a, b, 0), "ln dash thin") + line(p(0, b, 0), p(0, 0, 0), "ln dash thin") + line(p(0, b, 0), p(0, b, c), "ln dash thin");
    const vis = [
      [p(0, 0, 0), p(a, 0, 0)],
      [p(a, 0, 0), p(a, 0, c)],
      [p(a, 0, c), p(0, 0, c)],
      [p(0, 0, c), p(0, 0, 0)],
      [p(a, 0, 0), p(a, b, 0)],
      [p(a, b, 0), p(a, b, c)],
      [p(a, b, c), p(a, 0, c)],
      [p(0, 0, c), p(0, b, c)],
      [p(0, b, c), p(a, b, c)]
    ];
    for (const [u, v] of vis) body += line(u, v);
    if (s.oznake?.a) body += label([mid(p(0, 0, 0), p(a, 0, 0))[0], mid(p(0, 0, 0), p(a, 0, 0))[1] + 16], s.oznake.a, "lbl dim-t");
    if (s.oznake?.b) body += label([mid(p(a, 0, 0), p(a, b, 0))[0] + 10, mid(p(a, 0, 0), p(a, b, 0))[1] + 6], s.oznake.b, "lbl dim-t", "start");
    if (s.oznake?.c) body += label([p(0, 0, c / 2)[0] - 8, p(0, 0, c / 2)[1]], s.oznake.c, "lbl dim-t", "end");
    if (razina2 != null && s.oznake?.razina) body += label([p(a, 0, razina2 / 2)[0] - 8, p(a, 0, razina2 / 2)[1]], s.oznake.razina, "lbl acc-t", "end");
    return { svg: svg(body), checks };
  }
  function scale(s, text) {
    const { od, do: to, razmak } = s;
    const values = (s.vrijednosti ?? []).filter((v) => typeof v === "number");
    const checks = [];
    if (!(typeof od === "number" && typeof to === "number" && to > od && positive(razmak))) {
      return { checks: [fail("ljestvica treba od < do i pozitivan razmak")] };
    }
    const n = (to - od) / razmak;
    const maxTicks = { menzura: 36, ravnalo: 64, vaga: 90 }[s.instrument] ?? 36;
    checks.push({ ok: Math.abs(n - Math.round(n)) < 1e-6, text: `od ${num(od)} do ${num(to)} ima ${num(n)} razmaka (mora biti cijeli broj)` });
    checks.push({ ok: n <= maxTicks, text: `${num(n)} podjeljaka ${n <= maxTicks ? "je" : "NIJE"} čitljivo (najviše ${maxTicks}); povećaj razmak ili smanji raspon` });
    if (!values.length) checks.push(fail("ljestvica nema nijednu vrijednost za očitati"));
    for (const v of values) checks.push({ ok: v >= od && v <= to, text: `vrijednost ${num(v)} ${v >= od && v <= to ? "je" : "NIJE"} na ljestvici` });
    checks.push(...numbersInText(values.map(num), text));
    if (!["menzura", "vaga", "ravnalo"].includes(s.instrument)) checks.push(fail(`nepoznat instrument „${s.instrument}“`));
    if (checks.some((x) => !x.ok)) return { checks };
    const big = s.oznaka_svakih && s.oznaka_svakih >= razmak ? s.oznaka_svakih : razmak * Math.max(1, Math.round(niceStep(to - od) / razmak));
    const isBig = (v) => Math.abs((v - od) / big - Math.round((v - od) / big)) < 1e-6;
    const unit = s.jedinica ? ` ${s.jedinica}` : "";
    if (s.instrument === "menzura") {
      const draw = (cx, v, tag) => {
        const y0 = 40, y1 = 220, w = 54;
        const Y = (u) => y1 - (u - od) / (to - od) * (y1 - y0);
        let b = `<rect class="liquid" x="${cx - w / 2}" y="${f(Y(v))}" width="${w}" height="${f(y1 + 12 - Y(v))}" rx="6"/>`;
        b += `<path class="ln" d="M${cx - w / 2},${y0 - 14} V${y1 + 6} Q${cx - w / 2},${y1 + 14} ${cx - w / 2 + 8},${y1 + 14} H${cx + w / 2 - 8} Q${cx + w / 2},${y1 + 14} ${cx + w / 2},${y1 + 6} V${y0 - 14}"/>`;
        for (let i = 0; i <= Math.round(n); i++) {
          const u = od + i * razmak;
          const long = isBig(u);
          b += line([cx - w / 2, Y(u)], [cx - w / 2 + (long ? 16 : 9), Y(u)], "ln thin");
          if (long) b += label([cx - w / 2 - 6, Y(u)], num(u), "tick", "end");
        }
        b += line([cx - w / 2, Y(v)], [cx + w / 2, Y(v)], "ln acc");
        if (tag) b += label([cx, y1 + 30], tag, "lbl small");
        return b;
      };
      let body2 = values.length > 1 ? draw(120, values[0], "prije") + draw(270, values[1], "poslije") : draw(190, values[0]);
      if (unit) body2 += label([W - 16, 30], s.jedinica, "tick", "end");
      return { svg: svg(body2, 270), checks };
    }
    if (s.instrument === "vaga") {
      const c = [W / 2, 200], R = 150;
      const ang = (u) => Math.PI * (1 - (u - od) / (to - od));
      const at = (u, r) => [c[0] + r * Math.cos(ang(u)), c[1] - r * Math.sin(ang(u))];
      let body2 = `<path class="ln" d="M${f(c[0] - R)},${c[1]} A${R},${R} 0 0 1 ${f(c[0] + R)},${c[1]}"/>`;
      for (let i = 0; i <= Math.round(n); i++) {
        const u = od + i * razmak;
        const long = isBig(u);
        body2 += line(at(u, R), at(u, R - (long ? 16 : 9)), "ln thin");
        if (long) body2 += label(at(u, R - 30), num(u), "tick");
      }
      body2 += line(c, at(values[0], R - 6), "ln needle") + `<circle class="pt" cx="${c[0]}" cy="${c[1]}" r="5"/>`;
      if (unit) body2 += label([c[0], c[1] + 22], s.jedinica, "tick");
      return { svg: svg(body2, 240), checks };
    }
    const x0 = 30, x1 = W - 30, y = 150;
    const X = (u) => x0 + (u - od) / (to - od) * (x1 - x0);
    let body = `<rect class="ruler" x="${x0 - 10}" y="${y}" width="${x1 - x0 + 20}" height="46" rx="4"/>`;
    for (let i = 0; i <= Math.round(n); i++) {
      const u = od + i * razmak;
      const long = isBig(u);
      body += line([X(u), y], [X(u), y + (long ? 16 : 8)], "ln thin");
      if (long) body += label([X(u), y + 28], num(u), "tick");
    }
    if (values.length > 1) {
      const [a, b] = [Math.min(values[0], values[1]), Math.max(values[0], values[1])];
      body += `<rect class="object" x="${f(X(a))}" y="${y - 34}" width="${f(X(b) - X(a))}" height="30" rx="3"/>`;
      body += line([X(a), y - 40], [X(a), y + 18], "ln dash acc") + line([X(b), y - 40], [X(b), y + 18], "ln dash acc");
    } else {
      body += arrow([X(values[0]), y - 40], [X(values[0]), y - 4], "acc");
    }
    if (unit) body += label([x1 + 6, y + 40], s.jedinica, "tick", "end");
    return { svg: svg(body, 220), checks };
  }
  function forces(s, text) {
    const th = (s.podloga?.kut ?? 0) * Math.PI / 180;
    const flat = s.podloga?.vrsta === "vodoravna" || !s.podloga?.kut;
    const G = (s.sile ?? []).find((x) => x.smjer === "dolje");
    const N = (s.sile ?? []).find((x) => x.smjer === "okomito_od_podloge");
    const checks = [];
    if (!(s.sile ?? []).length || !(s.sile ?? []).every((x) => positive(x.iznos))) checks.push(fail("svaka sila treba pozitivan iznos"));
    if (G && N && !flat) {
      const exp = G.iznos * Math.cos(th);
      const ok = Math.abs(N.iznos - exp) <= 0.03 * exp;
      checks.push({ ok, text: `N = G·cos ${f(s.podloga.kut)}° = ${exp.toFixed(1)} N ${ok ? "slaže se s" : "NE slaže se s"} ${N.iznos} N` });
    }
    checks.push(...numbersInText((s.sile ?? []).filter((x) => x.zadana).map((x) => `${x.iznos}`), text));
    if (checks.some((x) => !x.ok)) return { checks };
    const angleGiven = !flat && numbersInText([`${s.podloga.kut}`], text).every((x) => x.ok);
    const L = 300;
    const base = flat ? [40, 150] : [40, 225];
    const along = flat ? [1, 0] : [Math.cos(th), -Math.sin(th)];
    const normal = flat ? [0, -1] : [-Math.sin(th), -Math.cos(th)];
    let body = "";
    if (flat) {
      body += line([20, base[1]], [W - 20, base[1]], "ln");
    } else {
      const top = [base[0] + L * Math.cos(th), base[1] - L * Math.sin(th)];
      body += poly([base, top, [top[0], base[1]]], "shape fill");
      body += `<path class="ln thin" d="M${base[0] + 34},${base[1]} A34,34 0 0 0 ${f(base[0] + 34 * Math.cos(th))},${f(base[1] - 34 * Math.sin(th))}"/>`;
      body += label([base[0] + 50, base[1] - 10], angleGiven ? `${num(s.podloga.kut)}°` : "α", "lbl small", "start");
    }
    const at = flat ? [W / 2, base[1]] : [base[0] + along[0] * L * 0.48, base[1] + along[1] * L * 0.48];
    const bw = 46, bh = 32;
    const corners = [[0, 0], [bw, 0], [bw, bh], [0, bh]].map(([u, v]) => [
      at[0] + along[0] * (u - bw / 2) + normal[0] * v,
      at[1] + along[1] * (u - bw / 2) + normal[1] * v
    ]);
    body += poly(corners, "box");
    const c = [at[0] + normal[0] * (bh / 2), at[1] + normal[1] * (bh / 2)];
    const max = Math.max(...s.sile.map((x) => x.iznos));
    const len = (v) => 30 + v / max * 70;
    const dir = {
      dolje: [0, 1],
      gore: [0, -1],
      okomito_od_podloge: normal,
      uz_kosinu: along,
      niz_kosinu: [-along[0], -along[1]],
      desno: [1, 0],
      lijevo: [-1, 0]
    };
    s.sile.forEach((x, i) => {
      const d = dir[x.smjer] ?? [0, 1];
      const end = [c[0] + d[0] * len(x.iznos), c[1] + d[1] * len(x.iznos)];
      const cls = `c${i % 4 + 1}`;
      body += arrow(c, end, cls);
      body += label([end[0] + d[0] * 14 + (d[1] === 1 && d[0] === 0 ? 14 : 0), end[1] + d[1] * 14], x.oznaka, `lbl ${cls}-t`);
    });
    if (s.rastav && G && !flat) {
      const gp = G.iznos * Math.sin(th);
      const gn = G.iznos * Math.cos(th);
      const e1 = [c[0] - along[0] * len(gp), c[1] - along[1] * len(gp)];
      const e2 = [c[0] - normal[0] * len(gn), c[1] - normal[1] * len(gn)];
      body += arrow(c, e1, "acc").replace('class="arr', 'class="arr dash') + label([e1[0] - along[0] * 16, e1[1] - along[1] * 16 - 6], "G∥", "lbl small acc-t");
      body += arrow(c, e2, "acc").replace('class="arr', 'class="arr dash') + label([e2[0] - normal[0] * 16 + 4, e2[1] - normal[1] * 16], "G⊥", "lbl small acc-t", "start");
    }
    return { svg: svg(body), checks };
  }
  var resistorH = (x, y, lbl) => `<rect class="box" x="${x - 22}" y="${y - 9}" width="44" height="18"/>` + label([x, y - 20], lbl, "lbl small");
  var resistorV = (x, y, lbl, left = false) => `<rect class="box" x="${x - 9}" y="${y - 22}" width="18" height="44"/>` + label([x + (left ? -14 : 14), y], lbl, "lbl small", left ? "end" : "start");
  function circuit(s, text) {
    const parts = s.spoj?.serijski ?? [];
    const series = parts.filter((e) => e.R);
    const par = parts.find((e) => e.paralelno)?.paralelno ?? [];
    const labels = [s.izvor?.oznaka, ...series.map((e) => e.R), ...par.map((e) => e.R)].filter(Boolean);
    const checks = numbersInText(labels, text);
    checks.push({ ok: par.length <= 3, text: `paralelnih grana: ${par.length} (najviše 3)` });
    if (!series.length && !par.length) checks.push(fail("strujni krug nema nijedan otpornik"));
    if (checks.some((x2) => !x2.ok)) return { checks };
    const [x0, x1, y0, y1] = [70, par.length ? 225 : 320, 55, 215];
    const wire = (a, b) => line(a, b, "ln");
    const ym = (y0 + y1) / 2;
    let body = wire([x0, y0], [x0, ym - 8]) + wire([x0, ym + 8], [x0, y1]);
    body += line([x0 - 16, ym - 8], [x0 + 16, ym - 8], "ln thick") + line([x0 - 8, ym + 8], [x0 + 8, ym + 8], "ln thick");
    if (s.izvor?.oznaka) body += label([x0 + 24, ym], s.izvor.oznaka, "lbl small", "start");
    const step = (x1 - x0) / (series.length + 1);
    let x = x0;
    series.forEach((e, i) => {
      const cx = x0 + step * (i + 1);
      body += wire([x, y0], [cx - 22, y0]) + resistorH(cx, y0, e.R);
      x = cx + 22;
    });
    body += wire([x, y0], [x1, y0]);
    if (par.length) {
      const gap = 75;
      par.forEach((e, i) => {
        const bx = x1 + i * gap;
        if (i) body += wire([x1 + (i - 1) * gap, y0], [bx, y0]) + wire([x1 + (i - 1) * gap, y1], [bx, y1]);
        body += wire([bx, y0], [bx, ym - 22]) + resistorV(bx, ym, e.R, i === 0 && par.length > 1) + wire([bx, ym + 22], [bx, y1]);
        body += dot([bx, y0], "pt") + dot([bx, y1], "pt");
      });
    } else body += wire([x1, y0], [x1, y1]);
    const am = (s.instrumenti ?? []).find((i) => i.vrsta === "A");
    const ax = (x0 + x1) / 2;
    if (am) {
      body += wire([x0, y1], [ax - 13, y1]) + wire([ax + 13, y1], [x1, y1]);
      body += `<circle class="meter" cx="${ax}" cy="${y1}" r="13"/>` + label([ax, y1 + 1], "A", "lbl small");
    } else body += wire([x0, y1], [x1, y1]);
    return { svg: svg(body), checks };
  }
  var RENDERERS = {
    trokut: triangle,
    kruznica: circle,
    graf: graph,
    koordinatni: coords,
    pravokutnik: rectangle,
    kvadar: cuboid,
    ljestvica: scale,
    sile: forces,
    strujni_krug: circuit
  };
  var SKETCH_TYPES = Object.keys(RENDERERS);
  function renderSketch(spec, text) {
    if (!spec || typeof spec !== "object") return { svg: null, checks: [] };
    const r = RENDERERS[spec.tip];
    if (!r) return { svg: null, checks: [fail(`nepoznat tip skice „${spec.tip}“`)] };
    try {
      const out = r(spec, text ?? "");
      return { svg: out.checks.every((c) => c.ok) && out.svg ? out.svg : null, checks: out.checks };
    } catch (e) {
      return { svg: null, checks: [fail(`opis se ne može nacrtati: ${e.message}`)] };
    }
  }

  // src/lib/lesson/core/number.js
  function parseNumber(input, unit = "") {
    if (typeof input === "number") return Number.isFinite(input) ? input : null;
    if (typeof input !== "string") return null;
    let s = input.trim().replace(/[−–]/g, "-").replace(/\u00a0/g, " ");
    if (unit && s.endsWith(unit)) s = s.slice(0, -unit.length).trim();
    s = s.replace(/\s+/g, "");
    if (!s) return null;
    const frac = s.match(/^(-?\d+(?:[.,]\d+)?)\/(\d+(?:[.,]\d+)?)$/);
    if (frac) {
      const d = toNumber(frac[2]);
      return d ? toNumber(frac[1]) / d : null;
    }
    if (/^-?[1-9]\d{0,2}\.\d{3}$/.test(s)) s = s.replace(".", "");
    const lastSep = Math.max(s.lastIndexOf(","), s.lastIndexOf("."));
    if (lastSep >= 0 && (s.match(/[.,]/g) ?? []).length > 1) {
      s = s.slice(0, lastSep).replace(/[.,]/g, "") + "." + s.slice(lastSep + 1);
    }
    return toNumber(s);
  }
  function toNumber(s) {
    if (!/^-?\d*(?:[.,]\d+)?$/.test(s) || s === "-" || s === "") return null;
    const n = Number(s.replace(",", "."));
    return Number.isFinite(n) ? n : null;
  }
  function tolerance(value, given) {
    if (typeof given === "number" && given >= 0) return given;
    return Math.max(1e-9, Math.abs(value) * 5e-3);
  }
  function near(a, b, tol) {
    return typeof a === "number" && typeof b === "number" && Math.abs(a - b) <= tol + 1e-12;
  }
  function formatNumber(n, digits = 3) {
    if (typeof n !== "number" || !Number.isFinite(n)) return "";
    const lead = n === 0 ? 0 : Math.max(0, -Math.floor(Math.log10(Math.abs(n))) - 1);
    const d = Math.min(12, Math.max(digits, lead + 3));
    let t = n.toFixed(d);
    if (t.includes(".")) t = t.replace(/\.?0+$/, "");
    if (t === "-0") t = "0";
    return t.replace("-", "−").replace(".", ",");
  }
  var isNum = (v) => typeof v === "number" && Number.isFinite(v);

  // src/lib/lesson/katalog/zajednicko.js
  var POGLEDAJ = "pogledaj";
  var ODGOVOR = "odgovor";
  var isText = (v) => typeof v === "string" && v.trim().length > 0;
  function problems() {
    const list = [];
    const need = (ok, msg) => {
      if (!ok) list.push(msg);
      return ok;
    };
    return { list, need };
  }
  var ANSWER_FIELDS = ["odgovor", "tocno", "tocna", "krivo", "cilj", "praznine", "ocitaj"];
  function noAnswerFields(spec, need) {
    for (const f2 of ANSWER_FIELDS) need(!(f2 in spec), `korak „${spec.tip}“ je za gledanje, pa ne smije imati polje „${f2}“ (odgovor ide u zaseban korak „Tvoj red“)`);
  }
  function validateKrivo(krivo, answer, tol, need, where = "krivo") {
    if (krivo == null) return;
    if (!need(Array.isArray(krivo), `${where} mora biti popis`)) return;
    krivo.forEach((k, i) => {
      need(isNum(k?.vrijednost), `${where}[${i}].vrijednost mora biti broj`);
      need(isText(k?.poruka), `${where}[${i}].poruka mora biti tekst`);
      if (isNum(k?.vrijednost) && isNum(answer)) need(!near(k.vrijednost, answer, tol), `${where}[${i}].vrijednost ${formatNumber(k.vrijednost)} jednaka je točnom odgovoru`);
    });
  }
  function checkNumber({ input, answer, tol, unit, krivo, savjet }) {
    const n = parseNumber(input, unit);
    if (n == null) return { tocno: false, poruka: "Upiši broj (decimalni zarez je u redu, npr. 1,5).", prazno: true };
    const t = tolerance(answer, tol);
    if (near(n, answer, t)) return { tocno: true };
    const hit = (krivo ?? []).find((k) => near(n, k.vrijednost, tolerance(k.vrijednost, k.tolerancija)));
    if (hit) return { tocno: false, poruka: hit.poruka };
    return { tocno: false, poruka: savjet || "Nije točno. Provjeri račun i jedinicu." };
  }
  var KRIVO_OPIS = `krivo?: [{vrijednost, poruka}] – tipične krive vrijednosti i poruka baš za tu grešku (npr. 220 = „to je završno očitanje, a ne volumen tijela“)`;
  var withUnit = (n, unit) => `${formatNumber(n)}${unit ? ` ${unit}` : ""}`;

  // src/lib/lesson/katalog/tekst.js
  var pojam = {
    tip: "pojam",
    uloga: () => POGLEDAJ,
    opis: `Kratko objašnjenje (1–3 rečenice): novi pojam svakodnevnim riječima, pa tek onda formula. Po želji uz skicu.
Polja: tekst (markdown), skica? (opis skice, isti tipovi kao skica uz zadatak).`,
    primjer: { tip: "pojam", tekst: "**Površina** govori koliko plohe nešto prekriva. Mjerimo je kvadratićima iste veličine." },
    validate(s, ctx) {
      const { list, need } = problems();
      need(isText(s.tekst), "pojam treba tekst");
      noAnswerFields(s, need);
      if (s.skica != null) sketchProblems(s.skica, ctx, need);
      return list;
    }
  };
  var postupak = {
    tip: "postupak",
    uloga: () => POGLEDAJ,
    opis: `Račun ili postupak koji se otkriva redak po redak (učenik klikne za sljedeći redak). Za riješeni primjer.
Polja: uvod? (markdown), koraci: [{racun: LaTeX bez $, napomena?: kratko objašnjenje retka}] (1–6 redaka).`,
    primjer: {
      tip: "postupak",
      uvod: "Od završnog očitanja oduzmemo početno:",
      koraci: [
        { racun: "V = V_2 - V_1", napomena: "volumen tijela je samo povećanje" },
        { racun: "V = 220\\ \\text{mL} - 140\\ \\text{mL} = 80\\ \\text{mL}" }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      if (need(Array.isArray(s.koraci) && s.koraci.length >= 1, "postupak treba barem jedan korak u „koraci“")) {
        need(s.koraci.length <= 6, "postupak smije imati najviše 6 redaka");
        s.koraci.forEach((k, i) => {
          need(isText(k?.racun), `koraci[${i}].racun mora biti LaTeX tekst`);
          need(!/^\s*\$|\$\s*$/.test(k?.racun ?? ""), `koraci[${i}].racun piši bez $ (dodaje ih aplikacija)`);
        });
      }
      noAnswerFields(s, need);
      return list;
    }
  };
  var skica = {
    tip: "skica",
    uloga: () => POGLEDAJ,
    opis: `Statična skica s kratkim tekstom. Polja: skica (opis skice: trokut, kruznica, graf, koordinatni, pravokutnik, kvadar,
ljestvica, sile, strujni_krug – isti format kao skica uz zadatak), tekst? (markdown).`,
    primjer: { tip: "skica", tekst: "Papir prekriven kvadratićima od 1 cm.", skica: { tip: "pravokutnik", a: 6, b: 6, mreza: 1, oznake: { a: "6 cm", b: "6 cm" } } },
    validate(s, ctx) {
      const { list, need } = problems();
      if (need(s.skica && typeof s.skica === "object", "skica treba polje „skica“")) sketchProblems(s.skica, ctx, need);
      noAnswerFields(s, need);
      return list;
    }
  };
  function sketchProblems(spec, ctx, need) {
    const r = renderSketch(spec, ctx?.text ?? "");
    if (!r.svg) need(false, `skica se ne može nacrtati: ${r.checks.filter((c) => !c.ok).map((c) => c.text).join("; ") || "neispravan opis"}`);
  }

  // src/lib/lesson/core/units.js
  var UNITS = {
    duljina: [
      ["km", 1e3],
      ["m", 1],
      ["dm", 0.1],
      ["cm", 0.01],
      ["mm", 1e-3]
    ],
    povrsina: [
      ["km²", 1e6],
      ["ha", 1e4],
      ["a", 100],
      ["m²", 1],
      ["dm²", 0.01],
      ["cm²", 1e-4],
      ["mm²", 1e-6]
    ],
    volumen: [
      ["m³", 1],
      ["hL", 0.1],
      ["dm³", 1e-3],
      ["L", 1e-3],
      ["dL", 1e-4],
      ["cL", 1e-5],
      ["cm³", 1e-6],
      ["mL", 1e-6],
      ["mm³", 1e-9]
    ],
    masa: [
      ["t", 1e3],
      ["kg", 1],
      ["dag", 0.01],
      ["g", 1e-3],
      ["mg", 1e-6]
    ],
    vrijeme: [
      ["h", 3600],
      ["min", 60],
      ["s", 1]
    ],
    gustoca: [
      ["g/cm³", 1e3],
      ["kg/dm³", 1e3],
      ["kg/m³", 1]
    ]
  };
  var ALIASES = { "m2": "m²", "cm2": "cm²", "mm2": "mm²", "dm2": "dm²", "km2": "km²", "m3": "m³", "cm3": "cm³", "dm3": "dm³", "mm3": "mm³", l: "L", ml: "mL", dl: "dL", cl: "cL", hl: "hL", "g/cm3": "g/cm³", "kg/m3": "kg/m³", "kg/dm3": "kg/dm³" };
  var normUnit = (u) => typeof u === "string" ? ALIASES[u.trim()] ?? u.trim() : u;
  function factor(velicina, unit) {
    const row = UNITS[velicina]?.find(([u]) => u === normUnit(unit));
    return row ? row[1] : null;
  }
  function convert(velicina, value, from, to) {
    const a = factor(velicina, from);
    const b = factor(velicina, to);
    if (a == null || b == null) return null;
    return value * a / b;
  }
  var unitIndex = (velicina, unit) => (UNITS[velicina] ?? []).findIndex(([u]) => u === normUnit(unit));
  function toBase(velicina, values, units) {
    const vs = [].concat(values);
    const us = [].concat(units);
    if (!vs.length || vs.length !== us.length) return null;
    let sum = 0;
    for (let i = 0; i < vs.length; i++) {
      const f2 = factor(velicina, us[i]);
      if (f2 == null || typeof vs[i] !== "number" || !Number.isFinite(vs[i])) return null;
      sum += vs[i] * f2;
    }
    return sum;
  }
  function fromBase(velicina, base, unit) {
    const f2 = factor(velicina, unit);
    return f2 == null || base == null ? null : base / f2;
  }
  function parseQuantity(text) {
    const t = String(text ?? "").trim().replace(/\$/g, "").replace(/\\text\{([^}]*)\}/g, "$1").replace(/\\[ ,;]/g, " ").replace(/\^2/g, "²").replace(/\^3/g, "³");
    const re = /(-?\d+(?:[.,]\d+)?)\s*([^\s\d]+)/g;
    const parts = [...t.matchAll(re)];
    if (!parts.length || t.replace(re, "").trim()) return null;
    const units = parts.map((m) => normUnit(m[2]));
    const values = parts.map((m) => parseNumber(m[1]));
    for (const velicina of Object.keys(UNITS)) {
      const base = toBase(velicina, values, units);
      if (base != null) return { velicina, base };
    }
    return null;
  }
  function staircase(velicina, from, to) {
    const list = UNITS[velicina] ?? [];
    const idx = [...[].concat(from), ...[].concat(to)].map((u) => unitIndex(velicina, u));
    if (!idx.length || idx.some((i) => i < 0)) return [];
    return list.slice(Math.min(...idx), Math.max(...idx) + 1).map(([u, f2]) => ({ jedinica: u, faktor: f2 }));
  }
  function typicalMistakes(velicina, value, from, to) {
    const right = convert(velicina, value, from, to);
    if (right == null || right === value) return [];
    const out = [];
    const ratio = right / value;
    const wrongDir = value / ratio;
    out.push({ vrijednost: wrongDir, poruka: `Pretvorba je u krivom smjeru. Iz ${from} u ${to} broj ${ratio > 1 ? "raste" : "pada"}, jer je ${ratio > 1 ? `${to} manja` : `${to} veća`} jedinica.` });
    const power = { povrsina: 2, volumen: 3 }[velicina];
    const special = ["L", "dL", "cL", "mL", "hL", "ha", "a"];
    if (power && !special.includes(normUnit(from)) && !special.includes(normUnit(to))) {
      const asLength = value * 10 ** (Math.log10(ratio) / power);
      out.push({
        vrijednost: asLength,
        poruka: power === 2 ? "Ovo je pretvorba kao za duljinu. Kod površine svaki korak između susjednih jedinica množi se sa 100, ne s 10." : "Ovo je pretvorba kao za duljinu. Kod volumena svaki korak između susjednih jedinica množi se s 1000, ne s 10."
      });
    }
    return out.filter((m) => Number.isFinite(m.vrijednost) && Math.abs(m.vrijednost - right) > 1e-9 * Math.max(1, Math.abs(right)));
  }

  // src/lib/lesson/katalog/pitanja.js
  var broj = {
    tip: "broj",
    uloga: () => ODGOVOR,
    opis: `Brojčani odgovor. Polja: tekst (pitanje, markdown), odgovor (broj), jedinica? (npr. "cm³"), tolerancija? (apsolutna; zadano 0,5 %),
${KRIVO_OPIS}, savjet? (poruka za ostale krive odgovore).`,
    primjer: {
      tip: "broj",
      tekst: "Koliki je volumen kamena?",
      odgovor: 80,
      jedinica: "cm³",
      krivo: [{ vrijednost: 220, poruka: "220 cm³ je završno očitanje: uključuje i vodu koja je već bila u menzuri." }],
      savjet: "Volumen kamena je razlika dvaju očitanja."
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "broj treba pitanje u „tekst“");
      need(isNum(s.odgovor), "broj treba brojčani „odgovor“");
      validateKrivo(s.krivo, s.odgovor, tolerance(s.odgovor, s.tolerancija), need);
      return list;
    },
    check: (s, input) => checkNumber({ input, answer: s.odgovor, tol: s.tolerancija, unit: s.jedinica, krivo: s.krivo, savjet: s.savjet }),
    tocanOdgovor: (s) => withUnit(s.odgovor, s.jedinica),
    brojcaniOdgovor: (s) => s.odgovor
  };
  var izbor = {
    tip: "izbor",
    uloga: () => ODGOVOR,
    opis: `Jedan točan odgovor od 2–5 ponuđenih (aplikacija ih miješa). Polja: tekst, opcije: [{tekst, tocno?: true, poruka?}];
točno je točno jedna opcija; svaka kriva opcija neka ima poruku za grešku koju predstavlja.`,
    primjer: {
      tip: "izbor",
      tekst: "Što znači riječ *srednja* u „srednja gustoća“?",
      opcije: [
        { tekst: "Ukupna masa podijeljena ukupnim volumenom", tocno: true },
        { tekst: "Zbroj gustoća dijelova podijeljen s 2", poruka: "Tako bi svaki dio vrijedio jednako, a to ovisi i o tome koliko prostora zauzima." }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "izbor treba pitanje u „tekst“");
      if (need(Array.isArray(s.opcije) && s.opcije.length >= 2 && s.opcije.length <= 5, "izbor treba 2–5 opcija")) {
        need(s.opcije.filter((o) => o?.tocno === true).length === 1, "izbor mora imati točno jednu opciju s tocno: true");
        s.opcije.forEach((o, i) => need(isText(o?.tekst), `opcije[${i}].tekst mora biti tekst`));
        need(new Set(s.opcije.map((o) => o?.tekst)).size === s.opcije.length, "opcije se ne smiju ponavljati");
      }
      return list;
    },
    check(s, index) {
      const o = s.opcije[index];
      if (!o) return { tocno: false, poruka: "Odaberi jednu opciju.", prazno: true };
      return o.tocno ? { tocno: true } : { tocno: false, poruka: o.poruka || s.savjet || "Nije točno." };
    },
    tocanOdgovor: (s) => s.opcije.find((o) => o.tocno)?.tekst ?? ""
  };
  var vise_tocnih = {
    tip: "vise_tocnih",
    uloga: () => ODGOVOR,
    opis: `Označi sve točne (3–6 opcija, barem jedna točna). Polja: tekst, opcije: [{tekst, tocno: true|false, poruka?}].`,
    primjer: {
      tip: "vise_tocnih",
      tekst: "Koja su tijela homogena?",
      opcije: [
        { tekst: "komad čistog željeza", tocno: true },
        { tekst: "spužva", tocno: false, poruka: "Spužva ima šupljine sa zrakom, pa tvar nije jednoliko raspoređena." },
        { tekst: "čaša čiste vode", tocno: true }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "vise_tocnih treba pitanje u „tekst“");
      if (need(Array.isArray(s.opcije) && s.opcije.length >= 3 && s.opcije.length <= 6, "vise_tocnih treba 3–6 opcija")) {
        need(s.opcije.some((o) => o?.tocno === true), "vise_tocnih treba barem jednu točnu opciju");
        need(s.opcije.every((o) => typeof o?.tocno === "boolean"), "svaka opcija u vise_tocnih treba tocno: true ili false");
        s.opcije.forEach((o, i) => need(isText(o?.tekst), `opcije[${i}].tekst mora biti tekst`));
      }
      return list;
    },
    check(s, chosen) {
      const set = new Set(chosen ?? []);
      if (!set.size) return { tocno: false, poruka: "Označi barem jednu opciju.", prazno: true };
      const wrongPick = s.opcije.findIndex((o, i) => set.has(i) && !o.tocno);
      if (wrongPick >= 0) return { tocno: false, poruka: s.opcije[wrongPick].poruka || "Jedna od označenih opcija nije točna." };
      const missed = s.opcije.findIndex((o, i) => !set.has(i) && o.tocno);
      if (missed >= 0) return { tocno: false, poruka: "Sve što si označio je točno, ali nedostaje još barem jedna točna opcija." };
      return { tocno: true };
    },
    tocanOdgovor: (s) => s.opcije.filter((o) => o.tocno).map((o) => o.tekst).join("; ")
  };
  var tocno_netocno = {
    tip: "tocno_netocno",
    uloga: () => ODGOVOR,
    opis: `Je li tvrdnja točna? Polja: tekst (tvrdnja), tocno (true|false), poruka (zašto, prikazuje se nakon krivog odgovora).`,
    primjer: { tip: "tocno_netocno", tekst: "Ako je tijelo samo djelomično uronjeno, porast razine vode jednak je cijelom volumenu tijela.", tocno: false, poruka: "Vodu istiskuje samo uronjeni dio tijela." },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "tocno_netocno treba tvrdnju u „tekst“");
      need(typeof s.tocno === "boolean", "tocno_netocno treba tocno: true ili false");
      need(isText(s.poruka), "tocno_netocno treba „poruka“ (zašto)");
      return list;
    },
    check: (s, value) => typeof value !== "boolean" ? { tocno: false, poruka: "Odaberi Točno ili Netočno.", prazno: true } : value === s.tocno ? { tocno: true } : { tocno: false, poruka: s.poruka },
    tocanOdgovor: (s) => s.tocno ? "Točno" : "Netočno"
  };
  var praznina = {
    tip: "praznina",
    uloga: () => ODGOVOR,
    opis: `Rečenica ili račun s prazninama. Polja: tekst (markdown s oznakama [[1]], [[2]] … na mjestu praznina; svaka oznaka točno jednom;
praznina ne smije biti unutar $…$), praznine: [{odgovor: broj ili tekst, jedinica?, tolerancija?, opcije?: [tekstovi za padajući izbor], ${KRIVO_OPIS}}].
Tekstualni odgovor traži „opcije“ (učenik bira, ne tipka).`,
    primjer: {
      tip: "praznina",
      tekst: "$1\\ \\text{cm}^2$ ima [[1]] mm², pa je $3600\\ \\text{mm}^2$ = [[2]] cm².",
      praznine: [
        { odgovor: 100, krivo: [{ vrijednost: 10, poruka: "Kvadratić od 1 cm ima 10 redova po 10 mm², a ne samo jedan red." }] },
        { odgovor: 36, krivo: [{ vrijednost: 360, poruka: "Dijeli se sa 100, ne s 10." }] }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      if (!need(isText(s.tekst), "praznina treba „tekst“ s oznakama [[1]] …")) return list;
      if (!need(Array.isArray(s.praznine) && s.praznine.length >= 1 && s.praznine.length <= 4, "praznina treba 1–4 praznine")) return list;
      s.praznine.forEach((p, i) => {
        const tag = `[[${i + 1}]]`;
        const count = s.tekst.split(tag).length - 1;
        need(count === 1, `oznaka ${tag} mora se u tekstu pojaviti točno jednom (sada ${count}×)`);
        need(!insideMath(s.tekst, tag), `oznaka ${tag} je unutar $…$; praznina mora biti izvan formule`);
        if (isNum(p?.odgovor)) validateKrivo(p.krivo, p.odgovor, tolerance(p.odgovor, p.tolerancija), need, `praznine[${i}].krivo`);
        else if (need(isText(p?.odgovor), `praznine[${i}].odgovor mora biti broj ili tekst`)) {
          need(Array.isArray(p.opcije) && p.opcije.includes(p.odgovor), `praznine[${i}] s tekstualnim odgovorom treba „opcije“ koje ga sadrže`);
        }
      });
      need(!/\[\[(\d+)\]\]/.test(s.tekst.replace(/\[\[(\d+)\]\]/g, (m, n) => Number(n) <= s.praznine.length ? "" : m)), "tekst ima oznaku praznine bez zapisa u „praznine“");
      return list;
    },
    check(s, values) {
      for (let i = 0; i < s.praznine.length; i++) {
        const p = s.praznine[i];
        const v = values?.[i];
        const r = isNum(p.odgovor) ? checkNumber({ input: v ?? "", answer: p.odgovor, tol: p.tolerancija, unit: p.jedinica, krivo: p.krivo, savjet: s.savjet }) : v === p.odgovor ? { tocno: true } : { tocno: false, poruka: v ? s.savjet || "Nije točno." : "Odaberi odgovor.", prazno: !v };
        if (!r.tocno) return { ...r, praznina: i, poruka: s.praznine.length > 1 ? `Praznina ${i + 1}: ${r.poruka}` : r.poruka };
      }
      return { tocno: true };
    },
    // zadnja brojčana praznina je obično konačni rezultat podzadatka
    brojcaniOdgovor: (s) => s.praznine.map((p) => p.odgovor).filter(isNum).at(-1),
    tocanOdgovor: (s) => s.praznine.map((p, i) => `${i + 1}. ${isNum(p.odgovor) ? withUnit(p.odgovor, p.jedinica) : p.odgovor}`).join(", ")
  };
  function insideMath(text, tag) {
    const at = text.indexOf(tag);
    if (at < 0) return false;
    const before = text.slice(0, at).replace(/\$\$/g, "");
    return (before.match(/\$/g) ?? []).length % 2 === 1;
  }
  var parovi = {
    tip: "parovi",
    uloga: () => ODGOVOR,
    opis: `Spoji parove (3–5). Polja: tekst, parovi: [{lijevo, desno}] u točnom paru; desna strana se miješa. Npr. veličina ↔ jedinica,
ili jednaki iznosi u različitim jedinicama ("27 dm" ↔ "2 m 7 dm", "3 m" ↔ "300 cm"; tada aplikacija provjeri da su parovi jednaki).`,
    primjer: {
      tip: "parovi",
      tekst: "Spoji veličinu s njezinom jedinicom.",
      parovi: [
        { lijevo: "masa", desno: "kg" },
        { lijevo: "volumen", desno: "m³" },
        { lijevo: "gustoća", desno: "kg/m³" }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "parovi treba „tekst“");
      if (need(Array.isArray(s.parovi) && s.parovi.length >= 3 && s.parovi.length <= 5, "parovi treba 3–5 parova")) {
        s.parovi.forEach((p, i) => need(isText(p?.lijevo) && isText(p?.desno), `parovi[${i}] treba „lijevo“ i „desno“`));
        need(new Set(s.parovi.map((p) => p?.desno)).size === s.parovi.length, "desne strane parova moraju biti različite");
        const iznosi = s.parovi.map((p) => [parseQuantity(p?.lijevo), parseQuantity(p?.desno)]);
        if (iznosi.every(([l, d]) => l && d)) {
          iznosi.forEach(([l, d], i) => need(l.velicina === d.velicina && Math.abs(l.base - d.base) <= 1e-9 * Math.max(1, Math.abs(l.base)), `parovi[${i}]: „${s.parovi[i].lijevo}“ i „${s.parovi[i].desno}“ nisu jednaki iznosi`));
          need(new Set(iznosi.map(([l]) => `${l.velicina}:${l.base.toPrecision(9)}`)).size === iznosi.length, "dva para imaju isti iznos, pa spajanje nije jednoznačno");
        }
      }
      return list;
    },
    // odgovor: za svaki lijevi indeks i odabrani desni indeks (iz izvornog popisa)
    check(s, map) {
      if (!Array.isArray(map) || map.some((j) => j == null)) return { tocno: false, poruka: "Spoji sve parove.", prazno: true };
      const wrong = map.findIndex((j, i) => j !== i);
      return wrong < 0 ? { tocno: true } : { tocno: false, poruka: s.savjet || `„${s.parovi[wrong].lijevo}“ nije spojeno s pravim parom.` };
    },
    tocanOdgovor: (s) => s.parovi.map((p) => `${p.lijevo} – ${p.desno}`).join("; ")
  };
  var redoslijed = {
    tip: "redoslijed",
    uloga: () => ODGOVOR,
    opis: `Poredaj 3–6 stavki (koraci postupka, vrijednosti po veličini). Polja: tekst, stavke: [tekstovi u TOČNOM redoslijedu] (aplikacija ih miješa), savjet?.`,
    primjer: { tip: "redoslijed", tekst: "Poredaj korake mjerenja volumena kamena.", stavke: ["Očitaj početnu razinu vode", "Potpuno uroni kamen", "Očitaj novu razinu", "Oduzmi početno od završnog očitanja"] },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "redoslijed treba „tekst“");
      if (need(Array.isArray(s.stavke) && s.stavke.length >= 3 && s.stavke.length <= 6, "redoslijed treba 3–6 stavki")) {
        need(s.stavke.every(isText), "svaka stavka mora biti tekst");
        need(new Set(s.stavke).size === s.stavke.length, "stavke se ne smiju ponavljati");
      }
      return list;
    },
    check(s, order) {
      if (!Array.isArray(order) || order.length !== s.stavke.length) return { tocno: false, poruka: "Poredaj sve stavke.", prazno: true };
      const wrong = order.findIndex((j, i) => j !== i);
      return wrong < 0 ? { tocno: true } : { tocno: false, poruka: s.savjet || `Na ${wrong + 1}. mjestu treba biti nešto drugo.` };
    },
    tocanOdgovor: (s) => s.stavke.map((t, i) => `${i + 1}. ${t}`).join(" → ")
  };
  var razvrstaj = {
    tip: "razvrstaj",
    uloga: () => ODGOVOR,
    opis: `Razvrstaj 3–8 stavki u 2–3 skupine. Polja: tekst, kategorije: [nazivi], stavke: [{tekst, kategorija: indeks skupine, poruka?: zašto}].`,
    primjer: {
      tip: "razvrstaj",
      tekst: "Razvrstaj tijela.",
      kategorije: ["Homogeno", "Nehomogeno"],
      stavke: [
        { tekst: "komad čistog željeza", kategorija: 0 },
        { tekst: "spužva", kategorija: 1, poruka: "Spužva ima šupljine sa zrakom." },
        { tekst: "figurica od aluminija i plastike", kategorija: 1, poruka: "Aluminij i plastika imaju različite gustoće." }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "razvrstaj treba „tekst“");
      const nk = Array.isArray(s.kategorije) ? s.kategorije.length : 0;
      need(nk >= 2 && nk <= 3, "razvrstaj treba 2–3 kategorije");
      if (need(Array.isArray(s.stavke) && s.stavke.length >= 3 && s.stavke.length <= 8, "razvrstaj treba 3–8 stavki")) {
        s.stavke.forEach((st, i) => {
          need(isText(st?.tekst), `stavke[${i}].tekst mora biti tekst`);
          need(Number.isInteger(st?.kategorija) && st.kategorija >= 0 && st.kategorija < nk, `stavke[${i}].kategorija mora biti indeks kategorije (0–${nk - 1})`);
        });
        need(new Set(s.stavke.map((st) => st?.kategorija)).size >= 2, "stavke moraju biti u barem dvije različite kategorije");
      }
      return list;
    },
    check(s, picks) {
      if (!Array.isArray(picks) || picks.some((k) => k == null) || picks.length !== s.stavke.length) return { tocno: false, poruka: "Razvrstaj sve stavke.", prazno: true };
      const wrong = picks.findIndex((k, i) => k !== s.stavke[i].kategorija);
      if (wrong < 0) return { tocno: true };
      const st = s.stavke[wrong];
      return { tocno: false, poruka: st.poruka ? `„${st.tekst}“: ${st.poruka}` : `„${st.tekst}“ nije u pravoj skupini.` };
    },
    tocanOdgovor: (s) => s.kategorije.map((k, ki) => `**${k}:** ${s.stavke.filter((st) => st.kategorija === ki).map((st) => st.tekst).join(", ")}`).join("; ")
  };

  // src/lib/lesson/core/expr.js
  var FUNCS = {
    sqrt: Math.sqrt,
    abs: Math.abs,
    sin: Math.sin,
    cos: Math.cos,
    tan: Math.tan,
    ln: Math.log,
    log: Math.log10,
    exp: Math.exp,
    round: Math.round
  };
  var CONSTS = { pi: Math.PI, e: Math.E };
  function tokenize(src) {
    const out = [];
    const re = /\s*(?:(\d+(?:[.,]\d+)?(?:e[+-]?\d+)?)|([A-Za-z_][A-Za-z_0-9]*)|(\*\*|[-+*/^(),]))/y;
    let i = 0;
    while (i < src.length) {
      re.lastIndex = i;
      const m = re.exec(src);
      if (!m || m[0].length === 0) {
        if (/^\s*$/.test(src.slice(i))) break;
        throw new Error(`neočekivan znak „${src[i]}“`);
      }
      i = re.lastIndex;
      if (m[1]) out.push({ t: "num", v: Number(m[1].replace(",", ".")) });
      else if (m[2]) out.push({ t: "id", v: m[2] });
      else out.push({ t: "op", v: m[3] === "**" ? "^" : m[3] });
    }
    return out;
  }
  function parse(tokens) {
    let p = 0;
    const peek = () => tokens[p];
    const eat = (v) => {
      const tk = tokens[p];
      if (!tk || tk.v !== v) throw new Error(`očekivano „${v}“`);
      p++;
    };
    const expr = () => {
      let n = term();
      while (peek()?.t === "op" && (peek().v === "+" || peek().v === "-")) {
        const op = tokens[p++].v;
        n = { op, a: n, b: term() };
      }
      return n;
    };
    const term = () => {
      let n = unary();
      while (peek()?.t === "op" && (peek().v === "*" || peek().v === "/")) {
        const op = tokens[p++].v;
        n = { op, a: n, b: unary() };
      }
      return n;
    };
    const unary = () => {
      if (peek()?.t === "op" && (peek().v === "-" || peek().v === "+")) {
        const op = tokens[p++].v;
        const a = unary();
        return op === "-" ? { op: "neg", a } : a;
      }
      return power();
    };
    const power = () => {
      const base = atom();
      if (peek()?.t === "op" && peek().v === "^") {
        p++;
        return { op: "^", a: base, b: unary() };
      }
      return base;
    };
    const atom = () => {
      const tk = tokens[p++];
      if (!tk) throw new Error("izraz je nepotpun");
      if (tk.t === "num") return { num: tk.v };
      if (tk.t === "id") {
        if (peek()?.v === "(") {
          if (!FUNCS[tk.v]) throw new Error(`nepoznata funkcija „${tk.v}“`);
          eat("(");
          const a = expr();
          eat(")");
          return { fn: tk.v, a };
        }
        return { id: tk.v };
      }
      if (tk.v === "(") {
        const a = expr();
        eat(")");
        return a;
      }
      throw new Error(`neočekivano „${tk.v}“`);
    };
    const tree = expr();
    if (p < tokens.length) throw new Error(`višak „${tokens[p].v}“`);
    return tree;
  }
  function evaluate(n, vars) {
    if ("num" in n) return n.num;
    if ("id" in n) {
      if (n.id in vars) return vars[n.id];
      if (n.id in CONSTS) return CONSTS[n.id];
      throw new Error(`nepoznata varijabla „${n.id}“`);
    }
    if (n.fn) return FUNCS[n.fn](evaluate(n.a, vars));
    const a = evaluate(n.a, vars);
    if (n.op === "neg") return -a;
    const b = evaluate(n.b, vars);
    switch (n.op) {
      case "+":
        return a + b;
      case "-":
        return a - b;
      case "*":
        return a * b;
      case "/":
        return a / b;
      case "^":
        return a ** b;
    }
    throw new Error(`nepoznata operacija ${n.op}`);
  }
  function variables(n, out = /* @__PURE__ */ new Set()) {
    if ("id" in n && !(n.id in CONSTS)) out.add(n.id);
    if (n.a) variables(n.a, out);
    if (n.b) variables(n.b, out);
    return out;
  }
  function compile2(src) {
    if (typeof src !== "string" || !src.trim()) throw new Error("izraz je prazan");
    const tree = parse(tokenize(src));
    return { fn: (vars = {}) => evaluate(tree, vars), vars: [...variables(tree)], tree };
  }
  var FUNKCIJE = Object.keys(FUNCS);
  var KONSTANTE = Object.keys(CONSTS);

  // src/lib/lesson/katalog/istrazivanje.js
  var ID = /^[A-Za-z_][A-Za-z_0-9]*$/;
  var NACIN_ISTRAZI = `nacin "istrazi" je korak za gledanje: učenik se igra, ništa se ne provjerava i nema polja s odgovorom`;
  var klizac = {
    tip: "klizac",
    uloga: () => POGLEDAJ,
    istrazi: () => true,
    opis: `Istraživanje (nije zadatak): klizači i rezultat koji se računa uživo. Polja: tekst, klizaci: [{id (npr. "m"), oznaka (npr. "masa m"),
min, max, korak, pocetak, jedinica?}] (1–3), izrazi: [{oznaka (markdown, npr. "$\\\\rho$"), izraz (npr. "m/V", varijable su id-jevi klizača; + - * / ^ sqrt),
jedinica?, decimale?}] (1–2).`,
    primjer: {
      tip: "klizac",
      tekst: "Mijenjaj masu i volumen i gledaj gustoću.",
      klizaci: [
        { id: "m", oznaka: "masa $m$", min: 20, max: 200, korak: 10, pocetak: 60, jedinica: "g" },
        { id: "V", oznaka: "volumen $V$", min: 10, max: 100, korak: 10, pocetak: 40, jedinica: "cm³" }
      ],
      izrazi: [{ oznaka: "gustoća $\\rho = m/V$", izraz: "m/V", jedinica: "g/cm³", decimale: 2 }]
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "klizac treba „tekst“ (što učenik istražuje)");
      noAnswerFields(s, need);
      const ids = [];
      if (need(Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 3, "klizac treba 1–3 klizača")) {
        s.klizaci.forEach((k, i) => {
          need(ID.test(k?.id ?? ""), `klizaci[${i}].id mora biti ime varijable (slova, bez razmaka)`);
          ids.push(k?.id);
          const ok = need([k?.min, k?.max, k?.korak, k?.pocetak].every(isNum), `klizaci[${i}] treba brojeve min, max, korak, pocetak`);
          if (ok) {
            need(k.max > k.min && k.korak > 0, `klizaci[${i}]: mora biti min < max i korak > 0`);
            need(k.pocetak >= k.min && k.pocetak <= k.max, `klizaci[${i}].pocetak mora biti između min i max`);
            need((k.max - k.min) / k.korak <= 1e3, `klizaci[${i}] ima previše koraka (najviše 1000)`);
          }
        });
        need(new Set(ids).size === ids.length, "id-jevi klizača moraju biti različiti");
      }
      if (need(Array.isArray(s.izrazi) && s.izrazi.length >= 1 && s.izrazi.length <= 2, "klizac treba 1–2 izraza")) {
        s.izrazi.forEach((iz, i) => {
          try {
            const c = compile2(iz?.izraz);
            const unknown = c.vars.filter((v) => !ids.includes(v));
            need(!unknown.length, `izrazi[${i}] koristi nepoznate varijable: ${unknown.join(", ")}`);
          } catch (e) {
            need(false, `izrazi[${i}].izraz: ${e.message}`);
          }
        });
      }
      return list;
    }
  };
  var INSTRUMENTI = { ravnalo: 64, menzura: 36, vaga: 90, termometar: 60 };
  var mjerilo = {
    tip: "mjerilo",
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Mjerni instrument: instrument ("ravnalo" | "menzura" | "vaga" | "termometar"), od, do, razmak (vrijednost jednog podjeljka),
oznaka_svakih (svakih koliko je broj na ljestvici), jedinica, tekst, nacin:
- "procitaj": učenik očita prikazanu vrijednost (vrijednost = što instrument pokazuje; mora biti na crtici); ${KRIVO_OPIS} (pogreška od jednog podjeljka dodaje se sama)
- "postavi": učenik povuče razinu/kazaljku na vrijednost (vrijednost = cilj);
- "istrazi": ${NACIN_ISTRAZI}; vrijednost = početak; za menzuru po želji uranjanje: {nakon, gumb} (gumb npr. "Uroni kamen").
Pazi: (do − od)/razmak cijeli broj i najviše ${Object.entries(INSTRUMENTI).map(([k, v]) => `${k} ${v}`).join(", ")} podjeljaka.`,
    primjer: { tip: "mjerilo", nacin: "procitaj", instrument: "menzura", od: 0, do: 300, razmak: 20, oznaka_svakih: 100, jedinica: "mL", vrijednost: 140, tekst: "Očitaj volumen vode u menzuri." },
    validate(s) {
      const { list, need } = problems();
      need(["istrazi", "procitaj", "postavi"].includes(s.nacin), "mjerilo.nacin mora biti istrazi, procitaj ili postavi");
      if (!need(s.instrument in INSTRUMENTI, `mjerilo.instrument mora biti jedan od: ${Object.keys(INSTRUMENTI).join(", ")}`)) return list;
      if (!need([s.od, s.do, s.razmak, s.vrijednost].every(isNum), "mjerilo treba brojeve od, do, razmak i vrijednost")) return list;
      need(isText(s.tekst), "mjerilo treba „tekst“");
      const n = (s.do - s.od) / s.razmak;
      need(s.do > s.od && s.razmak > 0, "mjerilo: mora biti od < do i razmak > 0");
      need(Math.abs(n - Math.round(n)) < 1e-6, `od ${s.od} do ${s.do} s razmakom ${s.razmak} nije cijeli broj podjeljaka`);
      need(n <= INSTRUMENTI[s.instrument], `${formatNumber(n)} podjeljaka je previše za ${s.instrument} (najviše ${INSTRUMENTI[s.instrument]}); povećaj razmak ili smanji raspon`);
      if (s.oznaka_svakih != null) need(isNum(s.oznaka_svakih) && aligned(s.oznaka_svakih, s.razmak), "oznaka_svakih mora biti višekratnik razmaka");
      need(s.vrijednost >= s.od && s.vrijednost <= s.do, "vrijednost mora biti na ljestvici (između od i do)");
      if (s.nacin !== "istrazi") need(aligned(s.vrijednost - s.od, s.razmak), "vrijednost mora biti točno na crtici (višekratnik razmaka od početka), inače se ne može pouzdano očitati");
      if (s.uranjanje != null) {
        need(s.nacin === "istrazi" && s.instrument === "menzura", "uranjanje postoji samo za menzuru u načinu istrazi");
        need(isNum(s.uranjanje?.nakon) && s.uranjanje.nakon > s.vrijednost && s.uranjanje.nakon <= s.do, "uranjanje.nakon mora biti veće od vrijednosti i na ljestvici");
        need(isText(s.uranjanje?.gumb), "uranjanje treba tekst gumba (npr. „Uroni kamen“)");
      }
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      else validateKrivo(s.krivo, s.vrijednost, mjeriloTol(s), need);
      return list;
    },
    check(s, input) {
      if (s.nacin === "postavi") {
        if (!isNum(input)) return { tocno: false, poruka: "Povuci na vrijednost.", prazno: true };
        if (near(input, s.vrijednost, mjeriloTol(s))) return { tocno: true };
        return { tocno: false, poruka: offByDivisions(input, s) };
      }
      const auto = [s.vrijednost + s.razmak, s.vrijednost - s.razmak].filter((v) => v >= s.od && v <= s.do).map((v) => ({ vrijednost: v, poruka: `${withUnit(v, s.jedinica)} je jedan podjeljak ${v > s.vrijednost ? "previše" : "premalo"}. Kreni od najbliže označene crte i broji razmake od po ${withUnit(s.razmak, s.jedinica)}.` }));
      return checkNumber({ input, answer: s.vrijednost, tol: mjeriloTol(s), unit: s.jedinica, krivo: [...s.krivo ?? [], ...auto], savjet: s.savjet || `Jedan podjeljak vrijedi ${withUnit(s.razmak, s.jedinica)}. Kreni od najbliže označene crte.` });
    },
    tocanOdgovor: (s) => withUnit(s.vrijednost, s.jedinica),
    brojcaniOdgovor: (s) => s.nacin === "istrazi" ? null : s.vrijednost
  };
  var aligned = (v, step) => Math.abs(v / step - Math.round(v / step)) < 1e-6;
  var mjeriloTol = (s) => isNum(s.tolerancija) ? s.tolerancija : s.razmak * 0.25;
  function offByDivisions(v, s) {
    const d = Math.round((v - s.vrijednost) / s.razmak);
    if (d === 0) return "Gotovo! Pomakni još malo.";
    return `Pokazuje ${withUnit(v, s.jedinica)}, a treba ${withUnit(s.vrijednost, s.jedinica)}: ${Math.abs(d)} ${Math.abs(d) === 1 ? "podjeljak" : "podjeljka"} ${d > 0 ? "previše" : "premalo"}.`;
  }
  var cellKey = ([r, c]) => `${r},${c}`;
  var mreza = {
    tip: "mreza",
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Mreža kvadratića: redaka, stupaca (najviše 20×20), velicina (stranica kvadratića), jedinica ("mm" | "cm" | "dm" | "m"), tekst, nacin:
- "istrazi": ${NACIN_ISTRAZI}; učenik dodirom označava kvadratiće i vidi površinu; oznacene? = unaprijed označeni [[redak, stupac], …] (od 0);
- "prebroji": na mreži je lik od oznacene (cijeli kvadratići) i polovice ([[r,c]] polovični kvadratići na rubu); učenik upiše površinu u jedinica².
  Točan odgovor računa aplikacija: (cijeli + polovice/2) · velicina². Krive vrijednosti „polovice kao cijeli“ i „polovice izostavljene“ dodaju se same;
- "oznaci": učenik označi kvadratiće ukupne površine cilj (u jedinica²).`,
    primjer: { tip: "mreza", nacin: "prebroji", redaka: 6, stupaca: 8, velicina: 1, jedinica: "cm", oznacene: [[1, 2], [1, 3], [2, 2], [2, 3], [2, 4]], polovice: [[1, 4], [3, 3]], tekst: "Kolika je površina otiska?" },
    validate(s) {
      const { list, need } = problems();
      need(["istrazi", "prebroji", "oznaci"].includes(s.nacin), "mreza.nacin mora biti istrazi, prebroji ili oznaci");
      const dims = need(Number.isInteger(s.redaka) && Number.isInteger(s.stupaca) && s.redaka >= 1 && s.stupaca >= 1 && s.redaka <= 20 && s.stupaca <= 20, "mreza treba cijele brojeve redaka i stupaca (1–20)");
      need(isNum(s.velicina) && s.velicina > 0, "mreza.velicina mora biti pozitivan broj");
      need(["mm", "cm", "dm", "m"].includes(s.jedinica), "mreza.jedinica mora biti mm, cm, dm ili m");
      need(isText(s.tekst), "mreza treba „tekst“");
      const cells = (name) => {
        const v = s[name];
        if (v == null) return [];
        if (!need(Array.isArray(v) && v.every((c) => Array.isArray(c) && c.length === 2 && c.every(Number.isInteger)), `${name} mora biti popis [redak, stupac]`)) return [];
        if (dims) need(v.every(([r, c]) => r >= 0 && c >= 0 && r < s.redaka && c < s.stupaca), `${name}: kvadratić izvan mreže`);
        return v.map(cellKey);
      };
      const full = cells("oznacene");
      const half = cells("polovice");
      need((/* @__PURE__ */ new Set([...full, ...half])).size === full.length + half.length, "isti kvadratić je i cijeli i polovica (ili se ponavlja)");
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      if (s.nacin === "prebroji") {
        need(full.length + half.length > 0, "mreza prebroji treba lik: oznacene i/ili polovice");
        validateKrivo(s.krivo, mrezaAnswer(s), tolerance(mrezaAnswer(s)), need);
      }
      if (s.nacin === "oznaci") {
        if (need(isNum(s.cilj) && s.cilj > 0, "mreza oznaci treba cilj (površinu)")) {
          const n = s.cilj / (s.velicina * s.velicina);
          need(Math.abs(n - Math.round(n)) < 1e-6, "cilj mora biti cijeli broj kvadratića");
          if (dims) need(n <= s.redaka * s.stupaca, "cilj je veći od cijele mreže");
        }
      }
      return list;
    },
    // prebroji: upisani broj; oznaci: broj označenih kvadratića
    check(s, input) {
      const unit = `${s.jedinica}²`;
      if (s.nacin === "oznaci") {
        if (!isNum(input) || input === 0) return { tocno: false, poruka: "Označi kvadratiće dodirom.", prazno: true };
        const area = input * s.velicina * s.velicina;
        if (near(area, s.cilj, 1e-9)) return { tocno: true };
        return { tocno: false, poruka: `Označeno je ${withUnit(area, unit)}, a treba ${withUnit(s.cilj, unit)}.` };
      }
      const nf = (s.oznacene ?? []).length;
      const nh = (s.polovice ?? []).length;
      const a = s.velicina * s.velicina;
      const auto = nh ? [
        { vrijednost: (nf + nh) * a, poruka: "Polovice si brojao kao cijele kvadratiće. Dvije polovice zajedno čine jedan kvadratić." },
        { vrijednost: nf * a, poruka: "Polovice si izostavio. I one pokrivaju dio plohe: dvije polovice čine jedan kvadratić." }
      ] : [];
      return checkNumber({ input, answer: mrezaAnswer(s), unit, krivo: [...s.krivo ?? [], ...auto], savjet: s.savjet || `Prebroji cijele kvadratiće, dodaj polovice (dvije čine jedan) i pomnoži površinom jednog kvadratića (${withUnit(a, unit)}).` });
    },
    tocanOdgovor: (s) => s.nacin === "oznaci" ? withUnit(s.cilj, `${s.jedinica}²`) : withUnit(mrezaAnswer(s), `${s.jedinica}²`),
    brojcaniOdgovor: (s) => s.nacin === "oznaci" ? s.cilj : s.nacin === "prebroji" ? mrezaAnswer(s) : null
  };
  var mrezaAnswer = (s) => ((s.oznacene ?? []).length + (s.polovice ?? []).length / 2) * s.velicina * s.velicina;
  var ocitaj_graf = {
    tip: "ocitaj_graf",
    uloga: (s) => s.nacin === "prikaz" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "prikaz",
    opis: `Graf funkcija u koordinatnom sustavu: x: [od, do], y: [od, do], os_x, os_y (npr. "V (cm³)"), funkcije: [{izraz (u varijabli x, npr. "2.4*x"), oznaka}] (1–3),
tocke?: [{x, y, oznaka}] (istaknute točke), tekst, nacin:
- "prikaz": korak za gledanje (dodirom se vide vrijednosti), bez odgovora;
- "ocitaj": učenik očita vrijednost: ocitaj: {oznaka: oznaka funkcije, x} (traži y) ili {oznaka, y} (traži x); jedinica?.
  Točan odgovor računa aplikacija iz funkcije; krive vrijednosti „s druge funkcije“ i „s druge osi“ dodaju se same. Vrijednost neka je na crtici mreže.`,
    primjer: { tip: "ocitaj_graf", nacin: "ocitaj", x: [0, 60], y: [0, 150], os_x: "V (cm³)", os_y: "m (g)", funkcije: [{ izraz: "2.4*x", oznaka: "A" }, { izraz: "0.8*x", oznaka: "B" }], ocitaj: { oznaka: "A", x: 20 }, jedinica: "g", tekst: "Kolika je masa uzorka A volumena 20 cm³?" },
    validate(s) {
      const { list, need } = problems();
      need(["prikaz", "ocitaj"].includes(s.nacin), "ocitaj_graf.nacin mora biti prikaz ili ocitaj");
      const axesOk = need(Array.isArray(s.x) && Array.isArray(s.y) && s.x.length === 2 && s.y.length === 2 && [...s.x, ...s.y].every(isNum) && s.x[1] > s.x[0] && s.y[1] > s.y[0], "ocitaj_graf treba x i y kao [od, do] uz od < do");
      need(isText(s.tekst), "ocitaj_graf treba „tekst“");
      const fns = {};
      if (need(Array.isArray(s.funkcije) && s.funkcije.length >= 1 && s.funkcije.length <= 3, "ocitaj_graf treba 1–3 funkcije")) {
        s.funkcije.forEach((fn, i) => {
          try {
            const c = compile2(fn?.izraz);
            need(c.vars.every((v) => v === "x"), `funkcije[${i}] smije koristiti samo varijablu x`);
            fns[fn?.oznaka ?? `f${i}`] = (x) => c.fn({ x });
          } catch (e) {
            need(false, `funkcije[${i}].izraz: ${e.message}`);
          }
        });
      }
      if (s.nacin === "prikaz") noAnswerFields(s, need);
      if (s.nacin === "ocitaj" && axesOk) {
        const q = s.ocitaj;
        if (need(q && fns[q.oznaka], "ocitaj.oznaka mora biti oznaka jedne od funkcija")) {
          const a = grafAnswer(s, fns);
          need(isNum(a), "vrijednost za očitati nije na grafu (izvan raspona osi)");
          validateKrivo(s.krivo, a, grafTol(s), need);
        }
      }
      return list;
    },
    check(s, input) {
      const fns = graphFns(s);
      const answer = grafAnswer(s, fns);
      const q = s.ocitaj;
      const auto = [];
      for (const fn of s.funkcije) {
        if (fn.oznaka === q.oznaka) continue;
        const other = grafAnswer({ ...s, ocitaj: { ...q, oznaka: fn.oznaka } }, fns);
        if (isNum(other)) auto.push({ vrijednost: other, poruka: `To je vrijednost s grafa ${fn.oznaka}, a traži se graf ${q.oznaka}.` });
      }
      const given = "x" in q ? q.x : q.y;
      auto.push({ vrijednost: given, poruka: `Pročitao si vrijednost na krivoj osi. Kreni od ${"x" in q ? s.os_x : s.os_y} = ${formatNumber(given)} do grafa, pa očitaj na drugoj osi.` });
      return checkNumber({ input, answer, tol: grafTol(s), unit: s.jedinica, krivo: [...s.krivo ?? [], ...auto], savjet: s.savjet || "Dodirni graf na zadanoj vrijednosti i prati crtu do druge osi." });
    },
    tocanOdgovor: (s) => withUnit(grafAnswer(s, graphFns(s)), s.jedinica),
    brojcaniOdgovor: (s) => s.nacin === "ocitaj" ? grafAnswer(s, graphFns(s)) : null
  };
  function graphFns(s) {
    return Object.fromEntries(
      s.funkcije.map((fn) => {
        const c = compile2(fn.izraz);
        return [fn.oznaka, (x) => c.fn({ x })];
      })
    );
  }
  var grafTol = (s) => isNum(s.tolerancija) ? s.tolerancija : ("x" in (s.ocitaj ?? {}) ? s.y[1] - s.y[0] : s.x[1] - s.x[0]) * 0.02;
  function grafAnswer(s, fns) {
    const q = s.ocitaj;
    const f2 = fns[q.oznaka];
    if ("x" in q) {
      const y = f2(q.x);
      return q.x >= s.x[0] && q.x <= s.x[1] && y >= s.y[0] - 1e-9 && y <= s.y[1] + 1e-9 ? y : null;
    }
    let [a, b] = s.x;
    const g = (x) => f2(x) - q.y;
    if (g(a) * g(b) > 0) return null;
    for (let i = 0; i < 80; i++) {
      const m = (a + b) / 2;
      if (g(a) * g(m) <= 0) b = m;
      else a = m;
    }
    return (a + b) / 2;
  }
  var lanac = (s) => [].concat(s.do);
  var izvor = (s) => ({ vrijednosti: [].concat(s.vrijednost), jedinice: [].concat(s.od).map(normUnit) });
  var zapisIzvora = (s) => {
    const { vrijednosti, jedinice } = izvor(s);
    return vrijednosti.map((v, i) => withUnit(v, jedinice[i])).join(" ");
  };
  function pretvorbaRezultati(s) {
    const { vrijednosti, jedinice } = izvor(s);
    const base = toBase(s.velicina, vrijednosti, jedinice);
    return lanac(s).map((u) => fromBase(s.velicina, base, u));
  }
  var prepisano = (velicina, od, cilj) => unitIndex(velicina, cilj) > unitIndex(velicina, od) ? `Prepisao si broj iz ${od}. Kad promijeniš jedinicu, mijenja se i broj: ${cilj} je manja jedinica, pa ih treba više.` : `Prepisao si broj iz ${od}. Kad promijeniš jedinicu, mijenja se i broj: ${cilj} je veća jedinica, pa ih treba manje.`;
  function drugeJedinice(velicina, tocno, cilj) {
    const base = toBase(velicina, tocno, cilj);
    return UNITS[velicina].filter(([u]) => u !== cilj).map(([u]) => ({ vrijednost: fromBase(velicina, base, u), poruka: `To je isti iznos u ${u}, a ovdje ga treba zapisati u ${cilj}.` }));
  }
  var pretvorba_jedinica = {
    tip: "pretvorba_jedinica",
    uloga: () => ODGOVOR,
    opis: `Pretvorba jedinica: velicina (${Object.keys(UNITS).join(" | ")}), vrijednost, od, do (jedinice, npr. "mm²" → "cm²"), tekst?, stube? (true: prikaži stube
jedinica s faktorima – za korak pomoći, ne za prvi pokušaj). Točan rezultat i tipične greške (faktor kao za duljinu, krivi smjer) računa aplikacija.
Lanac: do je popis 2–4 jedinice ("8 m = … dm = … cm = … mm": vrijednost 8, od "m", do ["dm", "cm", "mm"]); učenik upisuje svaku.
Mješoviti zapis: vrijednost i od su popisi iste duljine, od veće jedinice prema manjoj ("2 m 7 dm": vrijednost [2, 7], od ["m", "dm"]).
Jedinice: ${Object.entries(UNITS).map(([k, v]) => `${k}: ${v.map(([u]) => u).join(", ")}`).join("; ")}.`,
    primjer: { tip: "pretvorba_jedinica", velicina: "povrsina", vrijednost: 3600, od: "mm²", do: "cm²", tekst: "Pretvori površinu papira u cm²." },
    validate(s) {
      const { list, need } = problems();
      if (!need(s.velicina in UNITS, `pretvorba_jedinica.velicina mora biti jedna od: ${Object.keys(UNITS).join(", ")}`)) return list;
      const names = UNITS[s.velicina].map(([u]) => u);
      const { vrijednosti, jedinice } = izvor(s);
      const cilj = lanac(s);
      if (!need([...jedinice, ...cilj].every((u) => names.includes(normUnit(u))), `jedinice od/do moraju biti iz popisa za ${s.velicina}: ${names.join(", ")}`)) return list;
      need(vrijednosti.length === jedinice.length, "kod mješovitog zapisa vrijednost i od moraju biti popisi iste duljine");
      need(vrijednosti.every(isNum), "pretvorba_jedinica treba brojčanu vrijednost");
      need(cilj.length >= 1 && cilj.length <= 4, "do je jedna jedinica ili lanac od 2–4 jedinice");
      if (jedinice.length > 1) {
        need(jedinice.length <= 3, "mješoviti zapis ima najviše 3 dijela");
        need(jedinice.every((u, i) => i === 0 || unitIndex(s.velicina, u) > unitIndex(s.velicina, jedinice[i - 1])), "mješoviti zapis ide od veće jedinice prema manjoj (2 m 7 dm)");
      }
      const sve = [jedinice.length === 1 ? jedinice[0] : null, ...cilj.map(normUnit)].filter(Boolean);
      need(new Set(sve).size === sve.length, "od i do moraju biti različite jedinice, a lanac bez ponavljanja");
      return list;
    },
    // input: broj (jedna jedinica u „do“) ili popis brojeva (lanac)
    check(s, input) {
      const cilj = lanac(s).map(normUnit);
      const tocni = pretvorbaRezultati(s);
      const unosi = cilj.length === 1 ? [Array.isArray(input) ? input[0] : input] : [].concat(input ?? []);
      const { vrijednosti, jedinice } = izvor(s);
      for (let i = 0; i < cilj.length; i++) {
        const [prije, odJ] = i > 0 ? [tocni[i - 1], cilj[i - 1]] : jedinice.length === 1 ? [vrijednosti[0], jedinice[0]] : [null, null];
        const krivo = [
          ...i === cilj.length - 1 ? s.krivo ?? [] : [],
          ...prije == null ? [] : typicalMistakes(s.velicina, prije, odJ, cilj[i]),
          ...prije == null ? [] : [{ vrijednost: prije, poruka: prepisano(s.velicina, odJ, cilj[i]) }],
          ...drugeJedinice(s.velicina, tocni[i], cilj[i])
        ].filter((k) => Math.abs(k.vrijednost - tocni[i]) > 1e-9 * Math.max(1, Math.abs(tocni[i])));
        const r = checkNumber({ input: unosi[i] ?? "", answer: tocni[i], unit: cilj[i], krivo, savjet: s.savjet || "Pogledaj koliko je koraka između jedinica i koliki je faktor svakog koraka." });
        if (!r.tocno) return cilj.length > 1 ? { ...r, praznina: i, poruka: `${cilj[i]}: ${r.poruka}` } : r;
      }
      return { tocno: true };
    },
    brojcaniOdgovor: (s) => pretvorbaRezultati(s).at(-1),
    tocanOdgovor: (s) => [zapisIzvora(s), ...pretvorbaRezultati(s).map((v, i) => withUnit(v, normUnit(lanac(s)[i])))].join(" = "),
    stube: (s) => staircase(s.velicina, s.od, s.do)
  };
  var STANJA = ["cvrsto", "tekuce", "plin"];
  var cestice = {
    tip: "cestice",
    uloga: (s) => s.nacin === "prikaz" ? POGLEDAJ : ODGOVOR,
    istrazi: () => false,
    opis: `Čestični crtež: 2–4 kutije s česticama koje se gibaju kao u tvari. kutije: [{oznaka, stanje: "cvrsto" | "tekuce" | "plin", poruka?}], tekst, nacin:
- "prikaz": korak za gledanje (usporedba stanja);
- "odaberi": učenik dodirne kutiju koja odgovara pitanju; tocna = indeks kutije; poruka kutije = zašto nije ta.`,
    primjer: { tip: "cestice", nacin: "odaberi", tekst: "U kojoj su kutiji čestice tekućine?", kutije: [{ oznaka: "A", stanje: "cvrsto", poruka: "Čestice samo titraju na mjestu: to je čvrsto tijelo." }, { oznaka: "B", stanje: "tekuce" }, { oznaka: "C", stanje: "plin", poruka: "Čestice su daleko i slobodno lete: to je plin." }], tocna: 1 },
    validate(s) {
      const { list, need } = problems();
      need(["prikaz", "odaberi"].includes(s.nacin), "cestice.nacin mora biti prikaz ili odaberi");
      need(isText(s.tekst), "cestice treba „tekst“");
      if (need(Array.isArray(s.kutije) && s.kutije.length >= 2 && s.kutije.length <= 4, "cestice treba 2–4 kutije")) {
        s.kutije.forEach((k, i) => {
          need(STANJA.includes(k?.stanje), `kutije[${i}].stanje mora biti cvrsto, tekuce ili plin`);
          need(isText(k?.oznaka), `kutije[${i}].oznaka mora biti tekst`);
        });
      }
      if (s.nacin === "prikaz") noAnswerFields(s, need);
      if (s.nacin === "odaberi") need(Number.isInteger(s.tocna) && s.tocna >= 0 && s.tocna < (s.kutije?.length ?? 0), "cestice odaberi treba tocna = indeks kutije");
      return list;
    },
    check(s, index) {
      if (!Number.isInteger(index)) return { tocno: false, poruka: "Dodirni jednu kutiju.", prazno: true };
      return index === s.tocna ? { tocno: true } : { tocno: false, poruka: s.kutije[index]?.poruka || s.savjet || "Nije ta kutija. Pogledaj kako su čestice raspoređene i kako se gibaju." };
    },
    tocanOdgovor: (s) => `kutija ${s.kutije[s.tocna]?.oznaka}`
  };
  var ISHODI = ["pliva", "lebdi", "tone"];
  var ishod = (rt, rf) => Math.abs(rt - rf) <= rf * 5e-3 ? "lebdi" : rt < rf ? "pliva" : "tone";
  var uronjeno = {
    tip: "uronjeno",
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Tijela u tekućini: tekucina: {naziv, gustoca (g/cm³)}, tijela: [{naziv, gustoca (g/cm³)}] (1–4), tekst, nacin:
- "istrazi": ${NACIN_ISTRAZI}; učenik spušta tijela u tekućinu i vidi plivaju li;
- "predvidi": za svako tijelo učenik odabere pliva / lebdi / tone. Točan odgovor računa aplikacija iz gustoća.`,
    primjer: { tip: "uronjeno", nacin: "predvidi", tekst: "Što će se dogoditi sa svakim tijelom u vodi?", tekucina: { naziv: "voda", gustoca: 1 }, tijela: [{ naziv: "drvena kocka", gustoca: 0.6 }, { naziv: "željezni uteg", gustoca: 7.9 }] },
    validate(s) {
      const { list, need } = problems();
      need(["istrazi", "predvidi"].includes(s.nacin), "uronjeno.nacin mora biti istrazi ili predvidi");
      need(isText(s.tekst), "uronjeno treba „tekst“");
      need(isText(s.tekucina?.naziv) && isNum(s.tekucina?.gustoca) && s.tekucina.gustoca > 0, "uronjeno treba tekucina: {naziv, gustoca > 0}");
      if (need(Array.isArray(s.tijela) && s.tijela.length >= 1 && s.tijela.length <= 4, "uronjeno treba 1–4 tijela")) {
        s.tijela.forEach((t, i) => need(isText(t?.naziv) && isNum(t?.gustoca) && t.gustoca > 0, `tijela[${i}] treba naziv i gustoću > 0`));
      }
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      return list;
    },
    ishodi: (s) => s.tijela.map((t) => ishod(t.gustoca, s.tekucina.gustoca)),
    check(s, picks) {
      if (!Array.isArray(picks) || picks.length !== s.tijela.length || picks.some((p) => !ISHODI.includes(p))) return { tocno: false, poruka: "Odaberi ishod za svako tijelo.", prazno: true };
      const rf = s.tekucina.gustoca;
      for (let i = 0; i < s.tijela.length; i++) {
        const t = s.tijela[i];
        const right = ishod(t.gustoca, rf);
        if (picks[i] !== right) {
          const cmp = right === "pliva" ? "manja od" : right === "tone" ? "veća od" : "jednaka";
          return { tocno: false, poruka: `${t.naziv}: gustoća ${withUnit(t.gustoca, "g/cm³")} je ${cmp} ${right === "lebdi" ? "gustoći" : "gustoće"} tekućine (${withUnit(rf, "g/cm³")}), pa tijelo ${right}. Odlučuje gustoća, a ne masa ili veličina.` };
        }
      }
      return { tocno: true };
    },
    tocanOdgovor: (s) => s.tijela.map((t) => `${t.naziv}: ${ishod(t.gustoca, s.tekucina.gustoca)}`).join("; ")
  };

  // src/lib/lesson/core/izraz.js
  var ZNAKOVI = [
    [/[−–—]/g, "-"],
    [/[×·⋅∙]/g, "*"],
    [/[÷:]/g, "/"],
    [/π/g, " pi "],
    [/∞/g, " inf "],
    [/²/g, "^2"],
    [/³/g, "^3"],
    [/√\s*\(/g, " sqrt("],
    [/√\s*(\d+(?:[.,]\d+)?|[A-Za-z]+)/g, " sqrt($1)"],
    [/(\d),(\d)/g, "$1.$2"],
    // stupnjevi su samo oznaka mjere: „70°“ je 70
    [/°/g, ""]
  ];
  function rastavi(rijec, poznato) {
    const out = [];
    let i = 0;
    while (i < rijec.length) {
      const dio = poznato.filter((p) => rijec.startsWith(p, i)).sort((a, b) => b.length - a.length)[0];
      if (!dio) return null;
      out.push(dio);
      i += dio.length;
    }
    return out;
  }
  function pripremi(unos, varijable = []) {
    let s = String(unos ?? "");
    for (const [re, z] of ZNAKOVI) s = s.replace(re, z);
    const poznato = [...FUNKCIJE, ...KONSTANTE, ...varijable, "inf"];
    const tokeni = [];
    for (const m of s.matchAll(/\s*(?:(\d+(?:\.\d+)?)|([A-Za-z]+)|(\*\*|[-+*/^()]))|\s*(\S)/g)) {
      if (m[4]) throw new Error(`ne razumijem znak „${m[4]}“`);
      if (m[1]) tokeni.push({ t: "num", v: m[1] });
      else if (m[2]) {
        const dijelovi = poznato.includes(m[2]) ? [m[2]] : rastavi(m[2], poznato);
        if (!dijelovi) throw new Error(`ne razumijem „${m[2]}“`);
        for (const d of dijelovi) tokeni.push({ t: FUNKCIJE.includes(d) ? "fn" : "id", v: d });
      } else if (m[3]) tokeni.push({ t: "op", v: m[3] });
    }
    if (!tokeni.length) throw new Error("upiši izraz");
    const out = [];
    for (let i = 0; i < tokeni.length; i++) {
      const tk = tokeni[i];
      const prev = out.at(-1);
      const zavrsava = prev && (prev.t === "num" || prev.t === "id" || prev.v === ")");
      const pocinje = tk.t === "num" || tk.t === "id" || tk.t === "fn" || tk.v === "(";
      if (zavrsava && pocinje) out.push({ t: "op", v: "*" });
      out.push(tk);
      if (tk.t === "fn" && tokeni[i + 1]?.v !== "(") {
        const arg = [];
        while (tokeni[i + 1] && (tokeni[i + 1].t === "num" || tokeni[i + 1].t === "id") && arg.length < 2) arg.push(tokeni[++i]);
        if (!arg.length) throw new Error(`iza „${tk.v}“ treba broj ili zagrada`);
        out.push({ t: "op", v: "(" });
        arg.forEach((a, k) => {
          if (k) out.push({ t: "op", v: "*" });
          out.push(a);
        });
        out.push({ t: "op", v: ")" });
      }
    }
    return out.map((t) => t.v).join(" ");
  }
  var NAREDBE = {
    pi: " pi ",
    infty: " inf ",
    cdot: "*",
    times: "*",
    ast: "*",
    div: "/",
    exponentialE: " e ",
    sin: " sin ",
    cos: " cos ",
    tan: " tan ",
    tg: " tan ",
    ln: " ln ",
    log: " log ",
    exp: " exp ",
    vert: "|",
    lvert: "|",
    rvert: "|",
    mid: "|",
    ",": "",
    ";": "",
    ":": "",
    "!": "",
    " ": "",
    quad: "",
    qquad: "",
    degree: "",
    "%": "/100"
  };
  var OGRADE = { "(": "(", ")": ")", "[": "(", "]": ")", "|": "|", ".": "", "\\{": "(", "\\}": ")", "\\vert": "|", "\\lvert": "|", "\\rvert": "|", "\\langle": "(", "\\rangle": ")" };
  function izLatexa(latex) {
    const s = String(latex ?? "").replace(/\{,\}/g, ".");
    let i = 0;
    const naredba = () => {
      const m = /^\\([A-Za-z]+|.)/.exec(s.slice(i));
      i += m[0].length;
      return m[1];
    };
    const grupa = () => {
      while (s[i] === " ") i++;
      if (s[i] === "{") {
        let dubina = 0;
        const od = ++i;
        for (; i < s.length; i++) {
          if (s[i] === "{") dubina++;
          else if (s[i] === "}" && dubina-- === 0) break;
        }
        if (i >= s.length) throw new Error("nedostaje }");
        return s.slice(od, i++);
      }
      if (s[i] === "\\") {
        const od = i;
        naredba();
        return s.slice(od, i);
      }
      return s[i++] ?? "";
    };
    const ograda = () => {
      while (s[i] === " ") i++;
      for (const k of Object.keys(OGRADE).sort((a, b) => b.length - a.length)) {
        if (s.startsWith(k, i) && !(k.length > 1 && /[A-Za-z]/.test(s[i + k.length] ?? ""))) {
          i += k.length;
          return OGRADE[k];
        }
      }
      throw new Error("ne razumijem zagradu");
    };
    let out = "";
    while (i < s.length) {
      const c = s[i];
      if (c === "\\") {
        const n = naredba();
        if (n === "frac" || n === "dfrac" || n === "tfrac") out += `((${izLatexa(grupa())})/(${izLatexa(grupa())}))`;
        else if (n === "sqrt") {
          let korijen = null;
          if (s[i] === "[") {
            const kraj2 = s.indexOf("]", i);
            korijen = s.slice(i + 1, kraj2);
            i = kraj2 + 1;
          }
          const pod = izLatexa(grupa());
          out += korijen ? `((${pod})^(1/(${izLatexa(korijen)})))` : ` sqrt(${pod})`;
        } else if (["left", "right", "mleft", "mright", "bigl", "bigr", "Bigl", "Bigr"].includes(n)) out += ograda();
        else if (["operatorname", "mathrm", "text", "mathit", "textrm"].includes(n)) out += ` ${grupa()} `;
        else if (n === "placeholder") throw new Error("dovrši izraz (ostalo je prazno mjesto)");
        else if (n === "log" && s[i] === "_") {
          i++;
          out += ` ((1)/(log(${izLatexa(grupa())})))*log `;
        } else if (n in NAREDBE) out += NAREDBE[n];
        else throw new Error(`ne razumijem „\\${n}“`);
      } else if (c === "^") {
        i++;
        const g = grupa();
        if (g.trim() !== "\\circ") out += `^(${izLatexa(g)})`;
      } else if (c === "_") throw new Error("indeks ovdje ne pripada (baza ide samo uz log)");
      else if (c === "{") out += `(${izLatexa(grupa())})`;
      else {
        out += c;
        i++;
      }
    }
    let otvoreno = false;
    return out.replace(/\|/g, () => {
      otvoreno = !otvoreno;
      return otvoreno ? " abs(" : ")";
    });
  }
  function ucitajLatex(latex, varijable = []) {
    try {
      return ucitaj(izLatexa(latex), varijable);
    } catch (e) {
      return { greska: e.message };
    }
  }
  function ucitaj(unos, varijable = []) {
    try {
      const izraz2 = pripremi(unos, varijable);
      if (/\binf\b/.test(izraz2)) throw new Error("beskonačnost ovdje nije broj");
      const c = compile2(izraz2);
      const strane = c.vars.filter((v) => !varijable.includes(v));
      if (strane.length) throw new Error(`ne razumijem „${strane[0]}“`);
      return { izraz: izraz2, fn: c.fn, vars: c.vars, tex: tex(c.tree) };
    } catch (e) {
      return { greska: e.message };
    }
  }
  var UZORCI = [0.731, -1.37, 2.19, 3.71, -0.43, 1.618, -2.9, 0.217];
  function jednako(a, b, varijable = [], rel = 1e-9) {
    const isto = (x, y) => Math.abs(x - y) <= rel * Math.max(1, Math.abs(x), Math.abs(y));
    if (!varijable.length) {
      const [x, y] = [a.fn({}), b.fn({})];
      return Number.isFinite(x) && Number.isFinite(y) && isto(x, y);
    }
    let valjanih = 0;
    for (let k = 0; k < UZORCI.length; k++) {
      const v = Object.fromEntries(varijable.map((n, j) => [n, UZORCI[(k + 3 * j) % UZORCI.length] + j * 0.113]));
      const [x, y] = [a.fn(v), b.fn(v)];
      if (Number.isFinite(x) !== Number.isFinite(y)) return false;
      if (!Number.isFinite(x)) continue;
      if (!isto(x, y)) return false;
      valjanih++;
    }
    return valjanih >= 3;
  }
  function vrijednost(izraz2) {
    const r = ucitaj(String(izraz2));
    if (r.greska || r.vars.length) return null;
    const v = r.fn({});
    return Number.isFinite(v) ? v : null;
  }
  var RAZINA = { "+": 1, "-": 1, "*": 2, "/": 2, neg: 3, "^": 4 };
  var razina = (n) => n.op ? RAZINA[n.op] : 5;
  var zagrade = (t) => `\\left(${t}\\right)`;
  var omot = (n, min) => razina(n) < min ? zagrade(tex(n)) : tex(n);
  var TEX_FN = { sin: "\\sin", cos: "\\cos", tan: "\\tan", ln: "\\ln", log: "\\log", exp: "\\exp" };
  function broj2(n) {
    const t = String(Number(n.toPrecision(12)));
    return t.replace(".", "{,}");
  }
  function pocinjeSlovom(n) {
    if ("id" in n || n.fn) return true;
    if (n.op === "^") return pocinjeSlovom(n.a);
    if (n.op === "*") return pocinjeSlovom(n.a);
    return false;
  }
  function tex(n) {
    if ("num" in n) return broj2(n.num);
    if ("id" in n) return n.id === "pi" ? "\\pi " : n.id === "e" ? "e" : n.id.length > 1 ? `\\mathrm{${n.id}}` : n.id;
    if (n.fn) {
      if (n.fn === "sqrt") return `\\sqrt{${tex(n.a)}}`;
      if (n.fn === "abs") return `\\left|${tex(n.a)}\\right|`;
      return `${TEX_FN[n.fn] ?? `\\mathrm{${n.fn}}`}${zagrade(tex(n.a))}`;
    }
    switch (n.op) {
      case "+":
        return `${tex(n.a)} + ${n.b.op === "neg" ? zagrade(tex(n.b)) : tex(n.b)}`;
      case "-":
        return `${tex(n.a)} - ${n.b.op === "neg" ? zagrade(tex(n.b)) : omot(n.b, 2)}`;
      case "*": {
        const a = omot(n.a, 2);
        const b = n.b.op === "neg" ? zagrade(tex(n.b)) : omot(n.b, 3);
        const spoj = !("num" in n.b) && n.b.op !== "neg" && (pocinjeSlovom(n.b) || razina(n.b) < 3);
        return spoj ? `${a}${b}` : `${a} \\cdot ${b}`;
      }
      case "/":
        return `\\frac{${tex(n.a)}}{${tex(n.b)}}`;
      case "^":
        return `${razina(n.a) < 5 || n.a.fn ? zagrade(tex(n.a)) : tex(n.a)}^{${tex(n.b)}}`;
      case "neg":
        return `-${razina(n.a) < 3 && n.a.op !== "/" ? zagrade(tex(n.a)) : tex(n.a)}`;
    }
    return "";
  }
  function texIzraza(izraz2, varijable = []) {
    const r = ucitaj(String(izraz2), varijable);
    return r.greska ? String(izraz2) : r.tex;
  }

  // src/lib/lesson/core/skup.js
  var EPS = 1e-9;
  var isti = (x, y) => x === y || Number.isFinite(x) && Number.isFinite(y) && Math.abs(x - y) <= EPS * Math.max(1, Math.abs(x));
  function kraj(t) {
    const s = t.trim().replace(/[−–]/g, "-").replace(/\s+/g, "");
    if (/^-(∞|inf)$/.test(s)) return { v: -Infinity, tex: "-\\infty" };
    if (/^\+?(∞|inf)$/.test(s)) return { v: Infinity, tex: "+\\infty" };
    const v = vrijednost(s);
    if (v == null) throw new Error(`kraj intervala „${t.trim()}“ nije broj`);
    return { v, tex: ucitaj(s).tex };
  }
  function parseSkup(src) {
    const s = String(src ?? "").trim();
    const op = [...s.matchAll(/\s*(\\|∖|∩)\s*/g)].at(-1);
    if (op) {
      const lijevo = parseSkup(s.slice(0, op.index));
      const desno = parseSkup(s.slice(op.index + op[0].length));
      return op[1] === "∩" ? presjek(lijevo, desno) : razlika(lijevo, desno);
    }
    return parseUnije(s);
  }
  function parseUnije(s) {
    if (/^(∅|\{\s*\}|prazan skup)$/i.test(s)) return [];
    if (/^(R|ℝ)$/.test(s)) return [{ a: -Infinity, b: Infinity, ua: false, ub: false, ta: "-\\infty", tb: "+\\infty" }];
    const dijelovi = s.split(/∪|\s+U\s+|\bU\b/).map((d) => d.trim());
    if (dijelovi.some((d) => !d)) throw new Error("prazan dio unije");
    const out = [];
    for (const d of dijelovi) {
      if (d.startsWith("{")) {
        if (!d.endsWith("}")) throw new Error(`„${d}“: skup točaka mora završiti s }`);
        for (const t of d.slice(1, -1).split(";")) {
          const k = kraj(t);
          if (!Number.isFinite(k.v)) throw new Error("točka ne može biti beskonačna");
          out.push({ a: k.v, b: k.v, ua: true, ub: true, ta: k.tex, tb: k.tex });
        }
        continue;
      }
      const [o, z] = [d[0], d.at(-1)];
      if (!"<⟨([".includes(o) || !">⟩)]".includes(z)) throw new Error(`„${d}“: interval počinje s ⟨ ili [ i završava s ⟩ ili ]`);
      const unutra = d.slice(1, -1);
      const krajevi = unutra.includes(";") ? unutra.split(";") : unutra.split(",");
      if (krajevi.length !== 2) throw new Error(`„${d}“: interval ima dva kraja odvojena zarezom`);
      const [a, b] = krajevi.map(kraj);
      const ua = o === "[";
      const ub = z === "]";
      if (!(a.v < b.v)) throw new Error(`„${d}“: lijevi kraj mora biti manji od desnog`);
      if (ua && !Number.isFinite(a.v) || ub && !Number.isFinite(b.v)) throw new Error(`„${d}“: beskonačnost nikad nije uključena, uz nju ide ⟨ ili ⟩`);
      out.push({ a: a.v, b: b.v, ua, ub, ta: a.tex, tb: b.tex });
    }
    return sredi(out);
  }
  function sredi(dijelovi) {
    const d = dijelovi.map((x) => ({ ...x })).sort((x, y) => x.a - y.a || (x.ua === y.ua ? 0 : x.ua ? -1 : 1));
    const out = [];
    for (const x of d) {
      const cur = out.at(-1);
      if (cur && (x.a < cur.b - EPS || isti(x.a, cur.b) && (cur.ub || x.ua))) {
        if (isti(x.a, cur.a)) cur.ua ||= x.ua;
        if (x.b > cur.b + EPS) Object.assign(cur, { b: x.b, ub: x.ub, tb: x.tb });
        else if (isti(x.b, cur.b)) cur.ub ||= x.ub;
      } else out.push(x);
    }
    return out;
  }
  function komplement(skup) {
    const out = [];
    let a = { v: -Infinity, u: false, t: "-\\infty" };
    for (const d of sredi(skup)) {
      if (d.a > a.v || isti(d.a, a.v) && a.u && !d.ua && Number.isFinite(d.a)) out.push({ a: a.v, b: d.a, ua: a.u, ub: !d.ua, ta: a.t, tb: d.ta });
      a = { v: d.b, u: !d.ub, t: d.tb };
    }
    if (a.v < Infinity) out.push({ a: a.v, b: Infinity, ua: a.u, ub: false, ta: a.t, tb: "+\\infty" });
    return sredi(out.filter((d) => d.a < d.b || isti(d.a, d.b) && d.ua && d.ub));
  }
  function presjek(s1, s2) {
    const out = [];
    for (const x of s1) {
      for (const y of s2) {
        const [a, ua, ta] = x.a > y.a || isti(x.a, y.a) && !x.ua ? [x.a, x.ua, x.ta] : [y.a, y.ua, y.ta];
        const [b, ub, tb] = x.b < y.b || isti(x.b, y.b) && !x.ub ? [x.b, x.ub, x.tb] : [y.b, y.ub, y.tb];
        if (a < b - EPS || isti(a, b) && ua && ub) out.push({ a, b, ua, ub, ta, tb });
      }
    }
    return sredi(out);
  }
  var razlika = (s1, s2) => presjek(s1, komplement(s2));
  function tekstSkupaIzLatexa(latex) {
    let s = String(latex ?? "").replace(/\{,\}/g, ".").replace(/\\(?:left|right|mleft|mright)\s*\./g, "").replace(/\\(?:left|right|mleft|mright|bigl|bigr|Bigl|Bigr)(?![A-Za-z])\s*/g, "").replace(/\\langle(?![A-Za-z])|⟨/g, "<").replace(/\\rangle(?![A-Za-z])|⟩/g, ">").replace(/\\lbrack(?![A-Za-z])/g, "[").replace(/\\rbrack(?![A-Za-z])/g, "]").replace(/\\lbrace(?![A-Za-z])|\\\{/g, "⦃").replace(/\\rbrace(?![A-Za-z])|\\\}/g, "⦄").replace(/\\cup(?![A-Za-z])|∪/g, "U").replace(/\\cap(?![A-Za-z])/g, "∩").replace(/\\setminus(?![A-Za-z])|\\backslash(?![A-Za-z])|\\smallsetminus(?![A-Za-z])/g, "∖").replace(/\\(?:emptyset|varnothing)(?![A-Za-z])/g, "∅").replace(/\\mathbb\{R\}|\\R(?![A-Za-z])|ℝ/g, "R").replace(/\\infty(?![A-Za-z])/g, "∞").replace(/^\s*x\s*\\in(?![A-Za-z])/, "").trim();
    if (!s) throw new Error("upiši skup");
    if (/\\placeholder/.test(s)) throw new Error("dovrši zapis (ostalo je prazno mjesto)");
    let i = 0;
    const kraj2 = (odvajaci) => {
      let t = "";
      let dub = 0;
      let zag = 0;
      while (i < s.length) {
        const c = s[i];
        if (s.startsWith("\\sqrt[", i)) {
          const j = s.indexOf("]", i);
          if (j < 0) throw new Error("nedostaje ] u korijenu");
          t += s.slice(i, j + 1);
          i = j + 1;
          continue;
        }
        if (c === "{") dub++;
        else if (c === "}") dub--;
        else if (dub === 0) {
          if (c === "(") zag++;
          else if (c === ")" && zag > 0) zag--;
          else if (zag === 0 && (odvajaci.includes(c) || ">])⦄".includes(c))) break;
        }
        t += c;
        i++;
      }
      const k = t.trim();
      if (!k) throw new Error("nedostaje kraj intervala");
      if (/^[-+]?∞$/.test(k)) return k;
      return izLatexa(odvajaci === ";" ? k.replace(/(\d),(\d)/g, "$1.$2") : k).replace(/\s+/g, "");
    };
    const odvajac = () => {
      const zatvara = s.slice(i).search(/[>\])⦄]/);
      return s.slice(i, zatvara < 0 ? void 0 : i + zatvara).includes(";") ? ";" : ",;";
    };
    let out = "";
    while (i < s.length) {
      const c = s[i];
      if (c === " ") i++;
      else if ("<[(".includes(c)) {
        i++;
        const od = odvajac();
        const a = kraj2(od);
        if (![",", ";"].includes(s[i])) throw new Error("odvoji krajeve intervala zarezom ili točkom-zarezom");
        i++;
        const b = kraj2(od);
        const z = s[i++];
        if (!z || !">])".includes(z)) throw new Error("zatvori interval s ⟩ ili ]");
        out += `${c}${a};${b}${z}`;
      } else if (c === "⦃") {
        i++;
        const od = odvajac();
        const tocke = [kraj2(od)];
        while ([",", ";"].includes(s[i])) {
          i++;
          tocke.push(kraj2(od));
        }
        if (s[i++] !== "⦄") throw new Error("zatvori skup točaka sa }");
        out += `{${tocke.join(";")}}`;
      } else if ("U∩∖".includes(c)) {
        out += ` ${c} `;
        i++;
      } else if (c === "∅" || c === "R") {
        out += c;
        i++;
      } else throw new Error(`ne razumijem „${c}“ (skup piši kao ⟨a, b⟩, [a, b], {a}, ∅ ili ℝ, a dijelove spoji s ∪)`);
    }
    return out;
  }
  function ucitajSkupLatex(latex) {
    try {
      return { skup: parseSkup(tekstSkupaIzLatexa(latex)) };
    } catch (e) {
      return { greska: e.message };
    }
  }
  function jednakiSkupovi(s1, s2) {
    if (s1.length !== s2.length) return false;
    return s1.every((x, i) => {
      const y = s2[i];
      return isti(x.a, y.a) && isti(x.b, y.b) && (x.ua === y.ua || !Number.isFinite(x.a)) && (x.ub === y.ub || !Number.isFinite(x.b));
    });
  }
  function sadrzi(skup, x) {
    return skup.some((d) => (x > d.a || d.ua && isti(x, d.a)) && (x < d.b || d.ub && isti(x, d.b)));
  }
  function texSkupa(skup) {
    if (!skup.length) return "\\emptyset";
    if (skup.length === 1 && skup[0].a === -Infinity && skup[0].b === Infinity) return "\\mathbb{R}";
    return skup.map((d) => isti(d.a, d.b) ? `\\{${d.ta}\\}` : `${d.ua ? "\\left[" : "\\left\\langle"} ${d.ta},\\ ${d.tb} ${d.ub ? "\\right]" : "\\right\\rangle"}`).join(" \\cup ");
  }

  // src/lib/lesson/katalog/matematika.js
  var ID2 = /^[A-Za-z_][A-Za-z_0-9]*$/;
  var PRIBLIZNO = 0.01;
  var varijableIzraza = (s) => s.varijable ?? [];
  function poljeOdgovor(s, p) {
    return ucitaj(String(p.odgovor), varijableIzraza(s));
  }
  function provjeriPolje(s, p, unos) {
    const vars = varijableIzraza(s);
    if (!String(unos ?? "").trim()) return { tocno: false, poruka: "Upiši odgovor.", prazno: true };
    const u = ucitajLatex(String(p.iza ?? "").includes("%") ? String(unos).replace(/\\%/g, "") : unos, vars);
    if (u.greska) return { tocno: false, poruka: `Ne mogu pročitati izraz: ${u.greska}.`, prazno: true };
    const tocan = poljeOdgovor(s, p);
    if (jednako(u, tocan, vars)) return { tocno: true };
    for (const k of p.krivo ?? []) {
      const kr = ucitaj(String(k.odgovor), vars);
      if (!kr.greska && jednako(u, kr, vars)) return { tocno: false, poruka: k.poruka };
    }
    if (!vars.length && !s.priblizno && jednako(u, tocan, vars, PRIBLIZNO)) {
      return { tocno: false, poruka: "To je približna vrijednost. Upiši točnu (npr. s π, korijenom ili razlomkom)." };
    }
    if (s.priblizno && jednako(u, tocan, vars, PRIBLIZNO / 2)) return { tocno: true };
    return { tocno: false, poruka: p.savjet || s.savjet || "Nije točno." };
  }
  var izraz = {
    tip: "izraz",
    samoRucno: true,
    uloga: () => ODGOVOR,
    opis: `Matematički odgovor koji učenik upisuje u matematičko polje s tipkovnicom probne mature (razlomak, korijen, π, potencija
se odmah iscrtavaju). Polja: tekst, polja: [{oznaka
(markdown ispred polja, npr. "$x_1 =$"), iza? (markdown iza polja, npr. "$> 0$"), odgovor (izraz, npr. "pi/6", "2-sqrt(3)", "x^2-4*x+3"), krivo?: [{odgovor, poruka}], savjet?}] (1–4),
varijable?: ["x"] (ako je odgovor izraz s varijablom), bilo_kojim_redom?: true (npr. dvije nultočke), priblizno?: true (prihvati decimalni
zapis), graf?: graf_funkcije "prikaz" koji se crta iznad polja. Točno je sve što ima istu vrijednost.`,
    primjer: {
      tip: "izraz",
      tekst: "Koje su nultočke funkcije $f(x) = x^2 - 4x + 3$?",
      polja: [
        { oznaka: "$x_1 =$", odgovor: "1", krivo: [{ odgovor: "-1", poruka: "Pazi na predznak u formuli: $-b$ je $+4$." }] },
        { oznaka: "$x_2 =$", odgovor: "3" }
      ],
      bilo_kojim_redom: true
    },
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "izraz treba „tekst“ (pitanje)");
      const vars = varijableIzraza(s);
      need(Array.isArray(vars) && vars.every((v) => /^[a-z]$/.test(v) && v !== "e"), 'varijable su popis slova, npr. ["x"]');
      if (s.graf != null) {
        need(s.graf?.nacin === "prikaz", 'izraz.graf mora biti graf_funkcije s nacin "prikaz"');
        for (const p of graf_funkcije.validate({ tip: "graf_funkcije", tekst: "-", ...s.graf, nacin: "prikaz" })) list.push(`graf: ${p}`);
      }
      if (!need(Array.isArray(s.polja) && s.polja.length >= 1 && s.polja.length <= 4, "izraz treba 1–4 polja")) return list;
      const tocni = [];
      s.polja.forEach((p, i) => {
        if (p?.oznaka != null) need(isText(p.oznaka), `polja[${i}].oznaka mora biti tekst`);
        if (p?.iza != null) need(isText(p.iza), `polja[${i}].iza mora biti tekst`);
        const t = ucitaj(String(p?.odgovor ?? ""), vars);
        if (!need(!t.greska && p?.odgovor != null, `polja[${i}].odgovor nije ispravan izraz: ${t.greska ?? "nedostaje"}`)) return;
        if (!vars.length) need(Number.isFinite(t.fn({})), `polja[${i}].odgovor nema vrijednost`);
        tocni.push(t);
        for (const [j, k] of (p.krivo ?? []).entries()) {
          const kr = ucitaj(String(k?.odgovor ?? ""), vars);
          if (!need(!kr.greska, `polja[${i}].krivo[${j}].odgovor nije ispravan izraz`)) continue;
          need(isText(k.poruka), `polja[${i}].krivo[${j}] treba „poruka“`);
          need(!jednako(kr, t, vars), `polja[${i}].krivo[${j}] jednak je točnom odgovoru`);
        }
      });
      if (s.bilo_kojim_redom && tocni.length > 1) need(new Set(tocni.map((t) => t.fn({ x: 0.731 }))).size === tocni.length, "uz bilo_kojim_redom odgovori moraju biti različiti");
      return list;
    },
    check(s, unosi) {
      const polja = s.polja;
      const vals = polja.map((_, i) => unosi?.[i] ?? "");
      const prazno = vals.findIndex((v) => !String(v).trim());
      if (prazno >= 0) return { tocno: false, poruka: polja.length > 1 ? "Upiši sve odgovore." : "Upiši odgovor.", prazno: true, polje: prazno };
      if (s.bilo_kojim_redom && polja.length > 1) {
        const slobodna = polja.map((_, i) => i);
        for (let i = 0; i < vals.length; i++) {
          const k = slobodna.findIndex((j) => provjeriPolje(s, polja[j], vals[i]).tocno);
          if (k >= 0) {
            slobodna.splice(k, 1);
            continue;
          }
          const r = slobodna.map((j) => provjeriPolje(s, polja[j], vals[i])).find((x) => x.poruka && !x.prazno && x.poruka !== (s.savjet || "Nije točno.")) ?? provjeriPolje(s, polja[slobodna[0]], vals[i]);
          return { ...r, polje: i, poruka: `${polja.length > 1 ? `${i + 1}. odgovor: ` : ""}${r.poruka}` };
        }
        return { tocno: true };
      }
      for (let i = 0; i < polja.length; i++) {
        const r = provjeriPolje(s, polja[i], vals[i]);
        if (!r.tocno) return { ...r, polje: i, poruka: polja.length > 1 ? `${i + 1}. odgovor: ${r.poruka}` : r.poruka };
      }
      return { tocno: true };
    },
    tocanOdgovor: (s) => s.polja.map((p) => `${p.oznaka ? `${p.oznaka} ` : ""}$${texIzraza(p.odgovor, varijableIzraza(s))}$`).join(", "),
    // ključ za usporedbu blizanca s pitanjem: iste vrijednosti = isti odgovor
    kljucOdgovora: (s) => s.polja.map((p) => {
      const t = poljeOdgovor(s, p);
      return t.greska ? "?" : String(Number(t.fn({ x: 0.731, y: 1.37 }).toPrecision(9)));
    }).sort().join("|")
  };
  function tockePravca(s) {
    return (s.tocke ?? []).map((t) => ({ v: vrijednost(String(t)), tex: texIzraza(String(t)) })).filter((t) => t.v != null).sort((a, b) => a.v - b.v);
  }
  function skupIzOdabira(s, odabir) {
    const t = tockePravca(s);
    const out = [];
    (odabir?.dijelovi ?? []).forEach((da, i) => {
      if (!da) return;
      const a = i === 0 ? { v: -Infinity, tex: "-\\infty" } : t[i - 1];
      const b = i === t.length ? { v: Infinity, tex: "+\\infty" } : t[i];
      out.push({ a: a.v, b: b.v, ua: false, ub: false, ta: a.tex, tb: b.tex });
    });
    (odabir?.tocke ?? []).forEach((da, i) => {
      if (da && t[i]) out.push({ a: t[i].v, b: t[i].v, ua: true, ub: true, ta: t[i].tex, tb: t[i].tex });
    });
    return sredi(out);
  }
  function odabirIzSkupa(s, skup) {
    const t = tockePravca(s).map((x) => x.v);
    const sredine = [t.length ? t[0] - 1 : 0, ...t.slice(1).map((v, i) => (t[i] + v) / 2), t.length ? t.at(-1) + 1 : 0];
    if (!t.length) return { dijelovi: [sadrzi(skup, 0)], tocke: [] };
    return { dijelovi: sredine.map((x) => sadrzi(skup, x)), tocke: t.map((x) => sadrzi(skup, x)) };
  }
  var skupOdgovora = (s) => parseSkup(s.odgovor);
  function provjeriZapisSkupa(s, skup, latex) {
    if (!String(latex ?? "").trim()) return { tocno: false, poruka: "Upiši skup.", prazno: true };
    const u = ucitajSkupLatex(latex);
    if (u.greska) return { tocno: false, poruka: `Ne mogu pročitati zapis: ${u.greska}.`, prazno: true };
    const njegov = u.skup;
    if (jednakiSkupovi(njegov, skup)) return { tocno: true };
    for (const k of s.krivo ?? []) if (jednakiSkupovi(njegov, parseSkup(k.odgovor))) return { tocno: false, poruka: k.poruka };
    const blizu = (x, y) => x === y || Math.abs(x - y) < 1e-9;
    if (njegov.length === skup.length && njegov.every((d, i) => blizu(d.a, skup[i].a) && blizu(d.b, skup[i].b))) {
      return { tocno: false, poruka: "Krajevi su dobri, ali zagrade nisu. ⟨ ⟩ znači da kraj nije u skupu (kod < i >, i uvijek uz ∞), a [ ] da jest (kod ≤ i ≥)." };
    }
    if (skup.length && jednakiSkupovi(njegov, komplement(skup))) {
      return { tocno: false, poruka: "To je upravo suprotan dio pravca. Provjeri traži li se gdje je izraz veći ili manji od nule." };
    }
    return { tocno: false, poruka: s.savjet || "Nije točno. Provjeri krajeve: uvrsti po jedan broj iz svakog dijela i pogledaj vrijedi li uvjet." };
  }
  var interval = {
    tip: "interval",
    samoRucno: true,
    uloga: (s) => s.nacin === "prikaz" ? POGLEDAJ : ODGOVOR,
    opis: `Skup na brojevnom pravcu ili zapis skupa. Polja: tekst, tocke: [brojevi ili izrazi, npr. 1, "2-sqrt(3)"] (1–6 istaknutih točaka), nacin:
- "odaberi": učenik bira dijelove pravca i pune/prazne točke, a ispod vidi zapis; odgovor: "⟨-inf, 1⟩ U ⟨3, inf⟩" (⟨ ⟩ ili < > za otvoren, [ ]
  za zatvoren kraj, U za uniju, {2} ili {1; 2} za točke, ∅, R, R \\ {-4} za razliku, ∩ za presjek); svaki konačni kraj mora biti među tockama;
  krivo?: [{odgovor (skup), poruka}];
- "upisi": učenik sam piše zapis skupa u matematičko polje (tipkovnica sa ⟨ ⟩ [ ] ∪ ∞ ℝ); oznaka? (ispred polja, zadano "$x \\in$"),
  odgovor i krivo kao gore, tocke nisu potrebne; točan je svaki zapis istog skupa (npr. ℝ∖{-4} i ⟨-∞, -4⟩ ∪ ⟨-4, +∞⟩);
- "prikaz": pokazuje skup (odgovor) bez pitanja: korak za gledanje.`,
    primjer: { tip: "interval", nacin: "odaberi", tekst: "Za koje je $x$ parabola iznad osi $x$?", tocke: [1, 3], odgovor: "<-inf, 1> U <3, inf>", krivo: [{ odgovor: "<1, 3>", poruka: "Između 1 i 3 parabola je ispod osi." }] },
    validate(s) {
      const { list, need } = problems();
      need(["odaberi", "upisi", "prikaz"].includes(s.nacin), "interval.nacin mora biti odaberi, upisi ili prikaz");
      if (s.oznaka != null) need(isText(s.oznaka), "interval.oznaka mora biti tekst");
      need(isText(s.tekst), "interval treba „tekst“");
      const raw = s.tocke ?? [];
      const t = tockePravca(s);
      need(Array.isArray(raw) && raw.length <= 6 && t.length === raw.length, "tocke su 0–6 brojeva ili izraza bez varijabli");
      need(new Set(t.map((x) => x.v.toPrecision(9))).size === t.length, "tocke moraju biti različite");
      let skup = null;
      try {
        skup = skupOdgovora(s);
      } catch (e) {
        need(false, `odgovor: ${e.message}`);
      }
      if (skup && s.nacin !== "upisi") {
        const krajevi = skup.flatMap((d) => [d.a, d.b]).filter(Number.isFinite);
        const nema = krajevi.filter((k) => !t.some((x) => Math.abs(x.v - k) < 1e-9));
        need(!nema.length, `kraj ${nema[0]} nije među tockama, pa ga učenik ne može označiti`);
      }
      if (s.nacin === "prikaz") need(!("krivo" in s), 'interval "prikaz" je za gledanje, pa nema „krivo“');
      for (const [j, k] of (s.krivo ?? []).entries()) {
        try {
          const kr = parseSkup(k?.odgovor);
          need(isText(k?.poruka), `krivo[${j}] treba „poruka“`);
          if (skup) need(!jednakiSkupovi(kr, skup), `krivo[${j}] jednak je točnom odgovoru`);
        } catch (e) {
          need(false, `krivo[${j}].odgovor: ${e.message}`);
        }
      }
      return list;
    },
    check(s, odabir) {
      const skup = skupOdgovora(s);
      if (s.nacin === "upisi") return provjeriZapisSkupa(s, skup, odabir);
      const njegov = skupIzOdabira(s, odabir);
      if (jednakiSkupovi(njegov, skup)) return { tocno: true };
      for (const k of s.krivo ?? []) if (jednakiSkupovi(njegov, parseSkup(k.odgovor))) return { tocno: false, poruka: k.poruka };
      const tocan = odabirIzSkupa(s, skup);
      const isti2 = (a, b) => a.length === b.length && a.every((x, i) => x === b[i]);
      if (isti2(odabir.dijelovi, tocan.dijelovi)) {
        return { tocno: false, poruka: "Dijelovi pravca su dobri, ali krajevi nisu. Kod < i > granica nije u rješenju (prazan kružić, ⟨ ⟩), a kod ≤ i ≥ jest (pun kružić, [ ])." };
      }
      if (tocan.dijelovi.length > 1 && isti2(odabir.dijelovi, tocan.dijelovi.map((x) => !x))) {
        return { tocno: false, poruka: "Odabrao si upravo suprotne dijelove. Provjeri traži li se gdje je izraz veći ili manji od nule." };
      }
      return { tocno: false, poruka: s.savjet || "Nije točno. Za svaki dio pravca provjeri vrijedi li nejednakost (npr. uvrsti jedan broj iz tog dijela)." };
    },
    tocanOdgovor: (s) => `$${texSkupa(skupOdgovora(s))}$`,
    kljucOdgovora: (s) => {
      try {
        return texSkupa(skupOdgovora(s)).replace(/\s+/g, "");
      } catch {
        return "?";
      }
    }
  };
  function vrijednostiKlizaca(k) {
    if (Array.isArray(k.vrijednosti)) return k.vrijednosti.map((v) => ({ v: vrijednost(String(v)), tex: texIzraza(String(v)), izvor: v }));
    const out = [];
    if (![k.min, k.max, k.korak].every(isNum) || k.korak <= 0 || k.max <= k.min) return out;
    for (let v = k.min; v <= k.max + k.korak * 1e-6 && out.length <= 400; v += k.korak) {
      const r = Number((Math.round(v / k.korak) * k.korak).toPrecision(10));
      out.push({ v: r, tex: texIzraza(String(r)), izvor: r });
    }
    return out;
  }
  var indeksVrijednosti = (k, v) => {
    const x = vrijednost(String(v));
    return vrijednostiKlizaca(k).findIndex((o) => x != null && Math.abs(o.v - x) < 1e-9);
  };
  function funkcijeGrafa(s, parametri = {}) {
    return (s.funkcije ?? []).map((fn) => {
      const c = compile2(fn.izraz);
      const [od, d] = [fn.od != null ? vrijednost(String(fn.od)) : -Infinity, fn.do != null ? vrijednost(String(fn.do)) : Infinity];
      return { ...fn, od, do: d, f: (x) => x < od - 1e-12 || x > d + 1e-12 ? NaN : c.fn({ ...parametri, x }) };
    });
  }
  var KOSO = 0.5 * Math.SQRT1_2;
  function tockaCrteza(t) {
    if (!Array.isArray(t) || t.length !== 2 && t.length !== 3) return null;
    const v = t.map((x) => vrijednost(String(x)));
    if (v.some((x) => x == null)) return null;
    return t.length === 2 ? v : [v[0] + KOSO * v[1], v[2] + KOSO * v[1]];
  }
  var tockaIzPolja = (t) => tockaCrteza(t?.z != null ? [t.x, t.y, t.z] : [t?.x, t?.y]);
  var CRTEZ = { W: 400, rub: { L: 34, R: 14, T: 14, B: 26 }, rubBezOsi: { L: 14, R: 14, T: 14, B: 14 }, minH: 180, maxH: 380, maxHBezOsi: 460 };
  function okvirCrteza(s) {
    const rub = s.osi === false ? CRTEZ.rubBezOsi : CRTEZ.rub;
    const sirina = CRTEZ.W - rub.L - rub.R;
    const zeljena = sirina / (s.x[1] - s.x[0]) * (s.y[1] - s.y[0]) + rub.T + rub.B;
    const H2 = Math.round(Math.min(s.osi === false ? CRTEZ.maxHBezOsi : CRTEZ.maxH, Math.max(CRTEZ.minH, zeljena)));
    return { ...rub, W: CRTEZ.W, H: H2, jednako: Math.abs(H2 - zeljena) < 2 };
  }
  var ima = (s) => ["likovi", "duzine", "kruznice", "kutovi", "vektori", "tekstovi"].some((k) => (s[k] ?? []).length);
  var pocetniParametri = (s) => Object.fromEntries((s.klizaci ?? []).map((k) => [k.id, vrijednost(String(k.pocetak))]));
  var ciljniParametri = (s) => Object.fromEntries(Object.entries(s.cilj ?? {}).map(([id, v]) => [id, vrijednost(String(v))]));
  function istiGrafovi(s, p1, p2) {
    const [f2] = funkcijeGrafa(s, p1);
    const [g] = funkcijeGrafa(s, p2);
    let n = 0;
    for (let k = 0; k <= 60; k++) {
      const x = s.x[0] + (s.x[1] - s.x[0]) * (k + 0.37) / 61;
      const [a, b] = [f2.f(x), g.f(x)];
      if (!Number.isFinite(a) || !Number.isFinite(b)) continue;
      if (Math.abs(a - b) > 1e-6 * Math.max(1, Math.abs(a))) return false;
      n++;
    }
    return n > 10;
  }
  var NACINI_GRAFA = ["prikaz", "istrazi", "poklopi"];
  var POLOZAJI = ["gore", "dolje", "lijevo", "desno", "gore-desno", "gore-lijevo", "dolje-desno", "dolje-lijevo"];
  var graf_funkcije = {
    tip: "graf_funkcije",
    samoRucno: true,
    uloga: (s) => s.nacin === "poklopi" ? ODGOVOR : POGLEDAJ,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Graf funkcije u koordinatnom sustavu. Polja: tekst, x: [od, do], y: [od, do], korak_x?, korak_y? (mreža; inače 1), pi_x?: k (crtice na
x-osi u koracima π/k), funkcije: [{izraz (u x i parametrima), oznaka?, isprekidano?}] (1–3), tocke?: [{x, y, oznaka?}],
crte?: [{y | x, oznaka?}] (vodoravna/okomita isprekidana crta), strelice?: [{od: [x, y], do: [x, y], oznaka?}],
istakni?: {znak: ">0" | "<0" | ">=0" | "<=0"} (dio grafa prve funkcije iznad/ispod osi i odgovarajući dio osi x),
formula?: LaTeX s {id} parametara ({id:k} za koeficijent: 1 se ne piše; npr. "f(x) = {A:k}\\\\cos({B}x) + {D}"), klizaci?: [{id, oznaka, min, max, korak, pocetak} ili
{id, oznaka, vrijednosti: ["pi/6", …], pocetak}] (1–4), nacin:
Isti crtež crta i geometriju (točka je [x, y] ili [x, y, z] za tijelo u prostoru, z je visina, y dubina, kosa projekcija; koordinate
smiju biti izrazi): osi?: false (bez osi i brojeva), mreza?: false (bez mreže), funkcije tada smiju biti 0, funkcija.od?/do? (graf samo na
[od, do]), tocke[].z?, tocke[].polozaj? ("gore", "dolje", "lijevo", "desno", "gore-desno"…), tocke[].prazna? (prazan kružić),
likovi?: [{tocke: [[x, y], …], ispuna?, isprekidano?}], duzine?: [{od, do, oznaka?, isprekidano?, istaknuto?, strana?: 1 | -1}],
kruznice?: [{sredina: [x, y], r, isprekidano?, ispuna?}], kutovi?: [{vrh, od, do, oznaka?, pravi?}] (manji kut između krakova vrh→od i
vrh→do), vektori?: [{od, do, oznaka?}], tekstovi?: [{x, y, z?, tekst}], os_x?, os_y? (imena osi, npr. "Re" i "Im"). Oznake na crtežu su kratak tekst (smije $\\alpha$, $\\sqrt{3}$,
$x_1$, $\\vec{a}$, $90^\\circ$). Uz geometriju raspon x i y mora čuvati omjer (validator javi ako bi se krug izobličio).
- "prikaz": samo graf (bez klizača); "istrazi": klizači mijenjaju graf (korak za gledanje);
- "poklopi": učenik klizačima poklopi svoj graf s isprekidanim grafom iz zadatka: cilj: {id: vrijednost}; klizac.poruka? kad taj parametar ne valja.`,
    primjer: {
      tip: "graf_funkcije",
      nacin: "poklopi",
      tekst: "Pomiči klizač dok se tvoj graf ne poklopi s isprekidanim.",
      x: [-1, 13],
      y: [-5, 3],
      funkcije: [{ izraz: "A*cos(pi/6*x+pi/3)-1", oznaka: "f" }],
      klizaci: [{ id: "A", oznaka: "$A$", min: -4, max: 4, korak: 1, pocetak: 1 }],
      formula: "f(x) = {A:k}\\cos\\left(\\frac{\\pi}{6}x + \\frac{\\pi}{3}\\right) - 1",
      cilj: { A: 3 }
    },
    validate(s) {
      const { list, need } = problems();
      need(NACINI_GRAFA.includes(s.nacin), `graf_funkcije.nacin mora biti ${NACINI_GRAFA.join(", ")}`);
      need(isText(s.tekst), "graf_funkcije treba „tekst“");
      const osi = need(Array.isArray(s.x) && Array.isArray(s.y) && [...s.x, ...s.y].every(isNum) && s.x[1] > s.x[0] && s.y[1] > s.y[0], "x i y su [od, do] uz od < do");
      for (const k of ["korak_x", "korak_y"]) if (s[k] != null) need(isNum(s[k]) && s[k] > 0, `${k} mora biti pozitivan broj`);
      if (osi) {
        const kx = s.pi_x ? Math.PI / s.pi_x : s.korak_x ?? 1;
        need((s.x[1] - s.x[0]) / kx <= 40 && (s.y[1] - s.y[0]) / (s.korak_y ?? 1) <= 40, "mreža ima previše crta (najviše 40 po osi): povećaj korak_x/korak_y");
      }
      const klizaci = s.klizaci ?? [];
      const ids = klizaci.map((k) => k?.id);
      if (s.nacin === "prikaz") need(!klizaci.length, "graf „prikaz“ nema klizače (za njih je „istrazi“ ili „poklopi“)");
      else need(klizaci.length >= 1 && klizaci.length <= 4, `graf „${s.nacin}“ treba 1–4 klizača`);
      klizaci.forEach((k, i) => {
        need(ID2.test(k?.id ?? "") && k.id !== "x" && k.id !== "pi" && k.id !== "e", `klizaci[${i}].id mora biti ime parametra (ne x)`);
        need(isText(k?.oznaka), `klizaci[${i}] treba „oznaka“`);
        const v = vrijednostiKlizaca(k);
        if (need(v.length >= 2 && v.every((o) => o.v != null), `klizaci[${i}] treba min, max, korak ili popis vrijednosti (barem 2)`)) {
          need(indeksVrijednosti(k, k.pocetak) >= 0, `klizaci[${i}].pocetak mora biti jedna od vrijednosti klizača`);
        }
      });
      need(new Set(ids).size === ids.length, "id-jevi klizača moraju biti različiti");
      const geometrija = ima(s);
      const najmanje = geometrija && s.nacin === "prikaz" ? 0 : 1;
      if (need(Array.isArray(s.funkcije ?? (najmanje ? null : [])) && (s.funkcije ?? []).length >= najmanje && (s.funkcije ?? []).length <= 3, `graf_funkcije treba ${najmanje}–3 funkcije`)) {
        (s.funkcije ?? []).forEach((fn, i) => {
          for (const k of ["od", "do"]) if (fn?.[k] != null) need(vrijednost(String(fn[k])) != null, `funkcije[${i}].${k} mora biti broj`);
          try {
            const c = compile2(fn?.izraz);
            const strane = c.vars.filter((v) => v !== "x" && !ids.includes(v));
            need(!strane.length, `funkcije[${i}] koristi nepoznate parametre: ${strane.join(", ")}`);
          } catch (e) {
            need(false, `funkcije[${i}].izraz: ${e.message}`);
          }
        });
      }
      for (const [i, t] of (s.tocke ?? []).entries()) {
        need(tockaIzPolja(t) != null, `tocke[${i}] treba x i y (i z za prostor), brojeve ili izraze`);
        if (t?.polozaj != null) need(POLOZAJI.includes(t.polozaj), `tocke[${i}].polozaj mora biti jedno od: ${POLOZAJI.join(", ")}`);
      }
      for (const k of ["os_x", "os_y"]) if (s[k] != null) need(isText(s[k]), `${k} mora biti tekst`);
      const tocka = (t, gdje) => need(tockaCrteza(t) != null, `${gdje} mora biti točka [x, y] ili [x, y, z]`);
      for (const [i, l] of (s.likovi ?? []).entries()) {
        if (need(Array.isArray(l?.tocke) && l.tocke.length >= 3, `likovi[${i}] treba barem 3 točke`)) l.tocke.forEach((t, j) => tocka(t, `likovi[${i}].tocke[${j}]`));
      }
      for (const [i, d] of (s.duzine ?? []).entries()) {
        tocka(d?.od, `duzine[${i}].od`);
        tocka(d?.do, `duzine[${i}].do`);
        if (d?.strana != null) need(d.strana === 1 || d.strana === -1, `duzine[${i}].strana je 1 ili -1`);
      }
      for (const [i, k] of (s.kruznice ?? []).entries()) {
        tocka(k?.sredina, `kruznice[${i}].sredina`);
        need(vrijednost(String(k?.r)) > 0, `kruznice[${i}].r mora biti pozitivan broj`);
      }
      for (const [i, k] of (s.kutovi ?? []).entries()) for (const z of ["vrh", "od", "do"]) tocka(k?.[z], `kutovi[${i}].${z}`);
      for (const [i, v] of (s.vektori ?? []).entries()) for (const z of ["od", "do"]) tocka(v?.[z], `vektori[${i}].${z}`);
      for (const [i, t] of (s.tekstovi ?? []).entries()) need(tockaIzPolja(t) != null && isText(t?.tekst), `tekstovi[${i}] treba x, y i tekst`);
      if (osi && (geometrija || s.osi === false)) {
        const o = okvirCrteza(s);
        if (!o.jednako) {
          const omjer = (s.y[1] - s.y[0]) / (s.x[1] - s.x[0]);
          need(false, `raspon x i y ne čuva omjer (y/x = ${omjer.toFixed(2)}), pa bi se krug ili kut izobličio: ${o.H >= (s.osi === false ? CRTEZ.maxHBezOsi : CRTEZ.maxH) ? "proširi x ili smanji y" : "proširi y ili smanji x"}`);
        }
      }
      for (const [i, c] of (s.crte ?? []).entries()) need("x" in (c ?? {}) !== "y" in (c ?? {}) && vrijednost(String(c.x ?? c.y)) != null, `crte[${i}] treba točno jedno od x ili y`);
      for (const [i, st] of (s.strelice ?? []).entries()) need([...st?.od ?? [], ...st?.do ?? []].length === 4 && [...st.od, ...st.do].every((v) => vrijednost(String(v)) != null), `strelice[${i}] treba od: [x, y] i do: [x, y]`);
      if (s.istakni != null) need([">0", "<0", ">=0", "<=0"].includes(s.istakni?.znak), "istakni.znak mora biti >0, <0, >=0 ili <=0");
      if (s.nacin === "poklopi") {
        if (need(s.cilj && typeof s.cilj === "object" && ids.every((id) => id in s.cilj), "„poklopi“ treba cilj s vrijednošću svakog klizača") && osi && !list.length) {
          klizaci.forEach((k, i) => need(indeksVrijednosti(k, s.cilj[k.id]) >= 0, `cilj.${k.id} mora biti jedna od vrijednosti klizača ${i + 1}`));
          need(!istiGrafovi(s, pocetniParametri(s), ciljniParametri(s)), "graf se na početku već poklapa s ciljem: promijeni pocetak klizača");
        }
      } else noAnswerFields(s, need);
      return list;
    },
    check(s, parametri) {
      if (istiGrafovi(s, parametri, ciljniParametri(s))) return { tocno: true };
      const cilj = ciljniParametri(s);
      const los = (s.klizaci ?? []).find((k) => Math.abs((parametri?.[k.id] ?? NaN) - cilj[k.id]) > 1e-9);
      return { tocno: false, poruka: los?.poruka || "Grafovi se još ne poklapaju. Usporedi najviše i najniže točke i razmak između vrhova." };
    },
    tocanOdgovor: (s) => (s.klizaci ?? []).map((k) => `$${k.id} = ${texIzraza(String(s.cilj?.[k.id]))}$`).join(", "),
    kljucOdgovora: (s) => s.nacin === "poklopi" ? JSON.stringify(ciljniParametri(s)) + (s.funkcije?.[0]?.izraz ?? "") : null
  };

  // src/lib/lesson/katalog/index.js
  var KATALOG = Object.fromEntries(
    [pojam, postupak, skica, klizac, broj, izbor, vise_tocnih, tocno_netocno, praznina, parovi, redoslijed, razvrstaj, mjerilo, mreza, ocitaj_graf, pretvorba_jedinica, cestice, uronjeno, izraz, interval, graf_funkcije].map((w) => [w.tip, w])
  );
  var ZA_PROMPT = Object.fromEntries(Object.entries(KATALOG).filter(([, w]) => !w.samoRucno));
  var widget = (spec) => KATALOG[spec?.tip];
  var uloga = (spec) => widget(spec)?.uloga(spec);
  var istrazuje = (spec) => uloga(spec) === POGLEDAJ && !!widget(spec)?.istrazi?.(spec);
  var tocanOdgovor = (spec) => widget(spec)?.tocanOdgovor?.(spec) ?? "";

  // src/lib/lesson/katalog/vjezba.js
  var VJEZBA_OPIS = `vjezba (po želji, samo kad lekcija uči pretvaranje jedinica): {"velicina": "duljina" | "povrsina" | "volumen" | "masa" | "vrijeme",
"jedinice": [2–7 jedinica iz lekcije i izvornog zadatka, npr. ["km", "m", "dm", "cm", "mm"]], "lanac"?: true (i lanci "8 m = … dm = … cm"),
"mjesovito"?: true (i "2 m 7 dm = … cm")}. Zadatke s brojevima sastavlja aplikacija; ti ih ne pišeš.`;
  var MIN_ZADATAKA = 4;
  var MAX_ZADATAKA = 8;
  function validateVjezba(v) {
    const { list, need } = problems();
    if (!need(v && typeof v === "object", "vjezba mora biti objekt")) return list;
    if (!need(v.velicina in UNITS, `vjezba.velicina mora biti jedna od: ${Object.keys(UNITS).join(", ")}`)) return list;
    const names = UNITS[v.velicina].map(([u]) => u);
    if (!need(Array.isArray(v.jedinice) && v.jedinice.length >= 2 && v.jedinice.length <= 7, "vjezba.jedinice treba 2–7 jedinica")) return list;
    need(v.jedinice.every((u) => names.includes(normUnit(u))), `vjezba.jedinice moraju biti iz popisa za ${v.velicina}: ${names.join(", ")}`);
    need(new Set(v.jedinice.map(normUnit)).size === v.jedinice.length, "vjezba.jedinice se ne smiju ponavljati");
    if (v.zadataka != null) need(Number.isInteger(v.zadataka) && v.zadataka >= MIN_ZADATAKA && v.zadataka <= MAX_ZADATAKA, `vjezba.zadataka je ${MIN_ZADATAKA}–${MAX_ZADATAKA}`);
    if (v.tekst != null) need(isText(v.tekst), "vjezba.tekst mora biti tekst");
    if (!list.length) {
      const zadaci = generirajVjezbu(v, "provjera");
      need(zadaci.length >= MIN_ZADATAKA, "od tih jedinica ne mogu se sastaviti zadaci; dodaj jedinice koje su susjedne na stubama");
      for (const z of zadaci) for (const p of pretvorba_jedinica.validate(z)) need(false, `vjezba: ${p}`);
    }
    return list;
  }
  function rng(seed) {
    let s = 2166136261;
    for (const ch of String(seed)) s = Math.imul(s ^ ch.charCodeAt(0), 16777619);
    s = s >>> 0 || 1;
    return () => {
      s ^= s << 13;
      s ^= s >>> 17;
      s ^= s << 5;
      return (s >>> 0) / 4294967296;
    };
  }
  function lijepBroj(rand) {
    const r = rand();
    if (r < 0.4) return 2 + Math.floor(rand() * 98);
    if (r < 0.7) return Math.round((1 + rand() * 9) * 10) / 10;
    return (1 + Math.floor(rand() * 99)) * 10 ** (1 + Math.floor(rand() * 2));
  }
  function lijepRezultat(x) {
    if (!isNum(x) || x < 1e-3 || x > 1e6) return false;
    return Math.abs(Number(x.toPrecision(4)) - x) <= 1e-9 * x;
  }
  function generirajVjezbu(v, seed) {
    const rand = rng(`${seed}|${JSON.stringify(v)}`);
    const jed = [...new Set((v.jedinice ?? []).map(normUnit))].filter((u) => unitIndex(v.velicina, u) >= 0).sort((a, b) => unitIndex(v.velicina, a) - unitIndex(v.velicina, b));
    if (jed.length < 2) return [];
    const n = Math.min(MAX_ZADATAKA, Math.max(MIN_ZADATAKA, v.zadataka ?? 6));
    const vrste = ["jedna", "jedna", ...v.lanac && jed.length >= 3 ? ["lanac"] : [], ...v.mjesovito ? ["mjesovito"] : []];
    const out = [];
    const vidjeno = /* @__PURE__ */ new Set();
    for (let pokusaj = 0; out.length < n && pokusaj < 400; pokusaj++) {
      const vrsta = out.length < 2 ? "jedna" : vrste[Math.floor(rand() * vrste.length)];
      const z = sastavi(v.velicina, jed, vrsta, rand);
      if (!z) continue;
      const kljuc = JSON.stringify([z.vrijednost, z.od, z.do]);
      if (vidjeno.has(kljuc)) continue;
      vidjeno.add(kljuc);
      out.push({ tip: "pretvorba_jedinica", velicina: v.velicina, ...z, tekst: "" });
    }
    return out;
  }
  function sastavi(velicina, jed, vrsta, rand) {
    const pick = (arr) => arr[Math.floor(rand() * arr.length)];
    if (vrsta === "lanac") {
      const duljina = jed.length >= 4 && rand() < 0.5 ? 4 : 3;
      const start = Math.floor(rand() * (jed.length - duljina + 1));
      const niz = jed.slice(start, start + duljina);
      if (rand() < 0.5) niz.reverse();
      const vrijednost3 = lijepBroj(rand);
      const base = toBase(velicina, vrijednost3, niz[0]);
      const rez2 = niz.slice(1).map((u) => fromBase(velicina, base, u));
      return rez2.every(lijepRezultat) ? { vrijednost: vrijednost3, od: niz[0], do: niz.slice(1) } : null;
    }
    if (vrsta === "mjesovito") {
      const i2 = Math.floor(rand() * (jed.length - 1));
      const [veca, manja] = [jed[i2], jed[i2 + 1]];
      const omjer = Math.round(toBase(velicina, 1, veca) / toBase(velicina, 1, manja));
      if (!(omjer >= 10 && omjer <= 1e3)) return null;
      const a = 1 + Math.floor(rand() * 9);
      const b = 1 + Math.floor(rand() * (omjer - 1));
      const cilj2 = rand() < 0.6 || !jed[i2 + 2] ? manja : jed[i2 + 2];
      const rez2 = fromBase(velicina, toBase(velicina, [a, b], [veca, manja]), cilj2);
      return lijepRezultat(rez2) ? { vrijednost: [a, b], od: [veca, manja], do: cilj2 } : null;
    }
    const i = Math.floor(rand() * jed.length);
    const cilj = pick(jed.filter((u, j) => j !== i && Math.abs(j - i) <= 2));
    const od = jed[i];
    const vrijednost2 = lijepBroj(rand);
    const rez = fromBase(velicina, toBase(velicina, vrijednost2, od), cilj);
    return lijepRezultat(rez) ? { vrijednost: vrijednost2, od, do: cilj } : null;
  }

  // src/lib/lesson/validateLesson.js
  var VERZIJA = 1;
  var MAX_POMOC = 4;
  function validateLesson(lekcija, { rezultati, skica: skica2 } = {}) {
    const greske = [];
    const upozorenja = [];
    const err = (ok, msg) => {
      if (!ok) greske.push(msg);
      return ok;
    };
    const warn = (ok, msg) => {
      if (!ok) upozorenja.push(msg);
    };
    if (!err(lekcija && typeof lekcija === "object", "lekcija mora biti JSON objekt")) return { greske, upozorenja };
    err(lekcija.verzija === VERZIJA, `verzija mora biti ${VERZIJA}`);
    const text = allText(lekcija);
    const z = lekcija.zadatak;
    if (err(z && typeof z === "object", "nedostaje „zadatak“")) {
      err(isText(z.tekst), "zadatak.tekst mora opisati situaciju i podatke");
      if (z.skica != null) sketchProblems(z.skica, { text }, (ok, msg) => err(ok, `zadatak.skica: ${msg}`));
      if (z.graf != null) {
        if (err(z.graf?.tip === "graf_funkcije" && z.graf.nacin === "prikaz", 'zadatak.graf mora biti graf_funkcije s nacin "prikaz"')) {
          for (const p of KATALOG.graf_funkcije.validate(z.graf)) greske.push(`zadatak.graf: ${p}`);
        }
      }
    }
    const pz = lekcija.podzadaci;
    if (err(Array.isArray(pz) && pz.length >= 1 && pz.length <= 3, "lekcija treba 1–3 podzadatka")) {
      const ids = pz.map((p) => p?.id);
      err(new Set(ids).size === ids.length, "id-jevi podzadataka moraju biti različiti");
      pz.forEach((p, i) => {
        const at = `podzadaci[${i}]${p?.id ? ` (${p.id})` : ""}`;
        err(["a", "b", "c"].includes(p?.id), `${at}.id mora biti "a", "b" ili "c"`);
        err(isText(p?.naslov), `${at} treba kratak naslov`);
        err(isText(p?.zasto), `${at} treba „zasto“: 1–2 rečenice za učenika koji je odmah točno odgovorio`);
        err(isText(p?.objasnjenje), `${at} treba „objasnjenje“ (cijelo objašnjenje, skriveno dok ga učenik ne otvori)`);
        if (p?.graf != null && err(p.graf?.tip === "graf_funkcije" && p.graf.nacin === "prikaz", `${at}.graf mora biti graf_funkcije s nacin "prikaz"`)) {
          for (const g of KATALOG.graf_funkcije.validate(p.graf)) greske.push(`${at}.graf: ${g}`);
        }
        step(p?.pitanje, `${at}.pitanje`, ODGOVOR);
        err(!p?.pitanje?.stube, `${at}.pitanje: stube jedinica otkrivaju postupak; smiju samo u koraku pomoći`);
        if (p?.blizanac != null) {
          step(p.blizanac, `${at}.blizanac`, ODGOVOR);
          err(JSON.stringify(p.blizanac) !== JSON.stringify(p.pitanje), `${at}.blizanac je isti kao pitanje; treba nove brojeve ili novu situaciju`);
          warn(p.blizanac?.tip === p.pitanje?.tip, `${at}.blizanac je drugog tipa nego pitanje; blizanac provjerava istu vještinu`);
          const kljuc = widget(p.blizanac)?.kljucOdgovora;
          if (kljuc && p.blizanac.tip === p.pitanje?.tip) {
            err(kljuc(p.blizanac) == null || kljuc(p.blizanac) !== kljuc(p.pitanje), `${at}.blizanac ima isti odgovor kao pitanje, pa bi ga učenik samo prepisao; odaberi brojeve s kojima i rezultat bude drukčiji`);
          }
          const isti2 = quantityAnswers(p.blizanac);
          err(
            !isti2.length || !isti2.every((b) => quantityAnswers(p.pitanje).some((a) => Math.abs(a - b) <= Math.max(1e-9, Math.abs(a) * 5e-3))),
            `${at}.blizanac ima isti rezultat kao pitanje (${isti2.map(formatNumber2).join("; ")}), pa bi ga učenik samo prepisao; odaberi brojeve s kojima i rezultat bude drukčiji`
          );
        } else {
          warn(false, `${at} nema blizanca: nakon pomoći učenik ponovno rješava isto pitanje`);
        }
        if (err(Array.isArray(p?.pomoc) && p.pomoc.length >= 1 && p.pomoc.length <= MAX_POMOC, `${at}.pomoc treba 1–${MAX_POMOC} koraka`)) {
          p.pomoc.forEach((k, j) => step(k, `${at}.pomoc[${j}]`));
          warn(p.pomoc.some((k) => uloga(k) === ODGOVOR), `${at}.pomoc nema nijedan korak „Tvoj red“: pomoć bi trebala prepustiti učeniku barem jedan mali korak`);
          warn(uloga(p.pomoc[0]) === POGLEDAJ, `${at}.pomoc ne počinje objašnjenjem: učenik koji je pogriješio najprije treba vidjeti kako se to radi`);
        }
        const r = rezultati?.[p?.id];
        if (r != null && p?.pitanje) {
          const a = widget(p.pitanje)?.brojcaniOdgovor?.(p.pitanje);
          if (isNum(a)) warn(numbersIn(String(r)).some((n) => Math.abs(n - a) <= Math.max(1e-9, Math.abs(a) * 5e-3)), `${at}: točan odgovor ${a} ne pojavljuje se u rezultatima izvornog zadatka („${r}“); provjeri je li pitanje isto`);
        }
      });
    }
    for (const oblik of genderForms(text)) err(false, `„${oblik}“: učeniku se obraćamo u muškom rodu („zbrojio si“), bez ženskih i dvojnih oblika`);
    if (Array.isArray(pz)) {
      sourceSketch(lekcija, parseMaybe(skica2), err);
      sourceResults(pz, rezultati, err, warn);
    }
    if (lekcija.vjezba != null) for (const p of validateVjezba(lekcija.vjezba)) greske.push(p);
    if (err(Array.isArray(lekcija.zapamti) && lekcija.zapamti.length >= 1 && lekcija.zapamti.length <= 4, "zapamti treba 1–4 kratke natuknice")) {
      lekcija.zapamti.forEach((t, i) => err(isText(t), `zapamti[${i}] mora biti tekst`));
    }
    function step(spec, at, mora) {
      if (!err(spec && typeof spec === "object" && KATALOG[spec.tip], `${at}: nepoznat ili nedostaje tip widgeta („${spec?.tip}“); dopušteni: ${Object.keys(ZA_PROMPT).join(", ")}`)) return;
      const role = uloga(spec);
      if (mora === ODGOVOR) err(role === ODGOVOR, `${at} mora biti korak s odgovorom („Tvoj red“), a ${spec.tip}${spec.nacin ? ` (${spec.nacin})` : ""} je korak za gledanje`);
      for (const p of KATALOG[spec.tip].validate(spec, { text })) greske.push(`${at} (${spec.tip}): ${p}`);
      if (istrazuje(spec)) warn(/istraž|pomi|dodir|povuci|mijenjaj|igraj/i.test(spec.tekst ?? ""), `${at}: korak za istraživanje neka u tekstu kaže što učenik može dirati`);
    }
    return { greske, upozorenja };
  }
  function sourceSketch(lekcija, izvor2, err) {
    if (!izvor2?.tip || lekcija.zadatak?.skica) return;
    const pitanja = lekcija.podzadaci.map((p) => p?.pitanje).filter(Boolean);
    if (izvor2.tip === "ljestvica") {
      err(
        pitanja.some((q) => q.tip === "mjerilo" && q.nacin !== "istrazi" && (!izvor2.instrument || q.instrument === izvor2.instrument)),
        `izvorni zadatak očitava se s instrumenta (${izvor2.instrument ?? "ljestvica"}), a lekcija ga ne prikazuje: neka očitanje bude pitanje (mjerilo "procitaj") ili stavi ljestvicu u zadatak.skica`
      );
    } else if (!(izvor2.tip === "graf" && pitanja.some((q) => q.tip === "ocitaj_graf"))) {
      err(false, `izvorni zadatak ima skicu (${izvor2.tip}), a lekcija je nema: učenik ne vidi lik. Stavi istu skicu u zadatak.skica (izostavi samo oznaku koju učenik treba izračunati)`);
    }
  }
  function sourceResults(pz, rezultati, err, warn) {
    if (!rezultati || typeof rezultati !== "object") return;
    const odgovori = pz.flatMap((p) => answerNumbers(p?.pitanje));
    const odgovoreno = (n) => odgovori.some((a) => Math.abs(a - n) <= Math.max(1e-9, Math.abs(n) * 5e-3));
    for (const [id, r] of Object.entries(rezultati)) {
      if (typeof r !== "string" && typeof r !== "number") continue;
      for (const dio of answerParts(String(r))) {
        const brojevi = numbersIn(dio);
        const nedostaje = brojevi.filter((n) => !odgovoreno(n));
        if (!nedostaje.length) continue;
        const poruka = `rezultat ${id}) izvornog zadatka („${dio.trim()}“): ${nedostaje.map(formatNumber2).join(", ")} nije točan odgovor nijednog pitanja`;
        if (nedostaje.length === brojevi.length) err(false, `${poruka}. Učenik taj rezultat mora sam dobiti u pitanju podzadatka, ne samo u pomoći ili u tekstu. Ako podzadatak ima više rezultata, pitanje neka bude praznina s njima`);
        else warn(false, `${poruka}; učenik ga vidi samo u objašnjenju`);
      }
    }
  }
  function answerNumbers(spec) {
    if (!spec || !widget(spec)) return [];
    if (spec.tip === "pretvorba_jedinica") return pretvorbaRezultati(spec).filter(isNum);
    if (spec.tip === "praznina") return (spec.praznine ?? []).flatMap((p) => isNum(p?.odgovor) ? [p.odgovor] : numbersIn(String(p?.odgovor ?? "")));
    const out = [];
    try {
      const a = widget(spec).brojcaniOdgovor?.(spec);
      if (isNum(a)) out.push(a);
      out.push(...numbersIn(String(tocanOdgovor(spec))));
    } catch {
    }
    return out;
  }
  function quantityAnswers(spec) {
    if (spec?.tip === "pretvorba_jedinica") return answerNumbers(spec);
    if (spec?.tip === "praznina") return (spec.praznine ?? []).filter((p) => p?.jedinica && isNum(p.odgovor)).map((p) => p.odgovor);
    const a = spec?.jedinica && widget(spec) ? widget(spec).brojcaniOdgovor?.(spec) : null;
    return isNum(a) ? [a] : [];
  }
  function answerParts(r) {
    const bezObrazlozenja = (t) => t.split(/\bjer\b|\bzato\b|\(/i)[0];
    const [prvi, ...ostali] = r.split(";");
    return [prvi, ...ostali.filter((t) => /^\s*(?:[^:\d]{1,30}:\s*)?-?\d/.test(t))].map(bezObrazlozenja);
  }
  function genderForms(text) {
    const oblici = [
      ...text.matchAll(/(?<![\p{L}\d])\p{Ll}{3,}\/\p{Ll}{2,}(?![\p{L}\d³²])/gu),
      ...text.matchAll(/(?<!\p{L})si \p{L}+la(?!\p{L})|(?<!\p{L})\p{L}+la si(?!\p{L})/gu)
    ].map((m) => m[0]);
    return [...new Set(oblici)];
  }
  function parseMaybe(v) {
    if (typeof v !== "string") return v ?? null;
    try {
      return JSON.parse(v);
    } catch {
      return null;
    }
  }
  function formatNumber2(n) {
    return String(Number(n.toPrecision(4))).replace(".", ",");
  }
  function numbersIn(text) {
    text = text.replace(/\^\{?-?\d+\}?/g, "").replace(/(\d)[ \u00a0](?=\d{3}(?!\d))/g, "$1");
    return (text.match(/-?\d+(?:[.,]\d+)?(?:\/\d+)?/g) ?? []).map((s) => parseNumber(s)).filter(isNum);
  }
  function allText(l) {
    const out = [];
    const walk = (v) => {
      if (typeof v === "string") out.push(v);
      else if (typeof v === "number") out.push(String(v));
      else if (Array.isArray(v)) v.forEach(walk);
      else if (v && typeof v === "object") {
        for (const [k, x] of Object.entries(v)) if (k !== "skica") walk(x);
      }
    };
    walk(l);
    return out.join("\n");
  }

  // src/lib/lesson/prompt.js
  var PRAVILA = `Pretvaraš zadatak i njegovo objašnjenje u interaktivnu lekciju za MatMat, aplikaciju u kojoj učenik uči kroz zadatke.
Lekciju crta gotova aplikacija iz tvog JSON-a; smiješ koristiti samo widgete iz kataloga ispod, s točno opisanim poljima.

KAKO LEKCIJA RADI („zadatak prvo“):
- Na vrhu je kartica sa zadatkom (situacija i podaci). Zatim podzadaci a), b), c), jedan po jedan.
- Svaki podzadatak počinje pitanjem na koje učenik odmah odgovara. Točan odgovor u prvom pokušaju: kratka potvrda i „zasto“
  (1–2 rečenice), cijelo objašnjenje je skriveno iza gumba. Učenik koji zna sve gotov je s 3 odgovora.
- Krivi odgovor: poruka baš za tu grešku, pa kratka pomoć (1–4 mala koraka) samo za taj podzadatak, pa ponovni pokušaj na
  blizancu (isto pitanje s drugim brojevima ili drugim tijelom). Gumb „Ne znam“ vodi ravno u pomoć.
- Na kraju je „Zapamti“ (1–4 kratke natuknice), a u lekciji koja uči pretvaranje jedinica i „Vježbaj još“: neobavezni
  zadaci pretvorbe koje aplikacija sama sastavi iz jedinica koje navedeš u „vjezba“.

ULOGE KORAKA (vrlo važno):
- Svaki korak ima jednu ulogu: „Pogledaj“ (objašnjenje, primjer ili istraživanje – ništa se ne pita) ili „Tvoj red“ (pitanje).
  Nikad ne miješaj: korak za gledanje nema odgovor, a pitanje je uvijek zaseban korak. Učenik iz 7. razreda zbunio se kad je
  mreža za istraživanje izgledala kao zadatak.
- Korak za istraživanje (klizač, mreža "istrazi", mjerilo "istrazi", uronjeno "istrazi") u tekstu kaže što učenik može
  dirati i što će vidjeti, npr. „Pomiči klizač i gledaj kako se mijenja gustoća.“
- pitanje i blizanac moraju biti koraci „Tvoj red“.

POMOĆ (kao riješeni primjer koji se postupno prepušta učeniku):
- Prvi korak pomoći objasni ono što je učenik vjerojatno krivo shvatio („Pogledaj“): pojam, postupak, skica ili istraživanje.
- Zatim prepusti učeniku mali korak („Tvoj red“), npr. samo očitanje ili samo pretvorbu, s porukama za tipične greške.
- Najviše 4 koraka, kratko. Pomoć ne smije otkriti odgovor na blizanca.
- Blizanac ima drukčiji rezultat od pitanja, a ne samo druge podatke (8 dm · 3 dm · 2 dm = 48 dm³ kao u pitanju učenik bi
  samo prepisao). Isti smije ostati samo faktor bez jedinice („10 puta manja“). Aplikacija to provjerava.

WIDGETI UMJESTO TEKSTA:
- Učenik neka vještinu napravi sam u widgetu, a ne da samo upiše broj koji mu tekst već skoro daje. Ako izvorni podzadatak traži
  očitavanje s instrumenta, grafa ili slike, pitanje JE to očitavanje (mjerilo "procitaj", ocitaj_graf "ocitaj", mreza "prebroji");
  nikad ne piši očitanje u tekst („voda pokazuje 140 mL“). Pretvorba jedinica je pretvorba_jedinica (i lanac „8 m = … dm =
  … cm“ i mješoviti zapis „2 m 7 dm“), spajanje jednakih iznosa („27 dm“ ↔ „2 m 7 dm“) parovi, razvrstavanje razvrstaj,
  redoslijed koraka redoslijed, plivanje i tonjenje uronjeno, stanja tvari cestice.
- Najviše 3 podzadatka (a, b, c), kao u izvornom zadatku. Kad podzadatak traži dva rezultata (npr. u g/cm³ i kg/m³), pitanje
  je praznina s dvije praznine, a ne novi podzadatak.
- zadatak.tekst je samo situacija i podaci, bez pitanja a), b), c): pitanja dolaze u podzadacima.

SKICA UZ ZADATAK (ako je izvor ima, učenik mora vidjeti situaciju; aplikacija to provjerava):
- Lik, tijelo ili graf (trokut, pravokutnik, kvadar, koordinatni, graf…) ide u zadatak.skica: isti crtež i iste oznake,
  osim oznake koju učenik treba izračunati (nju izostavi). Tekst zadatka ne smije govoriti o crtežu koji se ne vidi.
- Duljinu koju učenik računa iz lika (npr. DE = 5 dm − 2 dm) ne prenosi na ravnalo da je samo očita: pitanje je taj račun,
  a lik je u zadatku. Mjerilo je za instrument koji postoji u zadatku, ne za zaobilaženje računa.
- Instrument s kojeg se očitava (ljestvica: menzura, vaga, termometar) ne ide u zadatak (odgovor bi bio nacrtan), nego
  očitanje postaje pitanje (mjerilo "procitaj"). Kad izvor ima dva očitanja (prije i poslije uranjanja), učenik barem jedno
  očita sam, a rezultat iz njih (npr. volumen kamena kao razliku) uvijek računa sam.

SADRŽAJ:
- Sve što izvorno objašnjenje uči mora se naučiti: u pitanju, u „zasto“, u pomoći ili u „Zapamti“. Brojevi i rezultati
  moraju se slagati s izvornim zadatkom; smiješ prilagoditi pitanje widgetu, ali ne mijenjati fiziku ni rezultate.
- Svaki broj iz REZULTATI učenik sam dobiva kao točan odgovor na pitanje (glavni put), u istoj jedinici kao u rezultatu.
  Pomoć i blizanac vidi samo učenik koji griješi, pa rezultat koji je samo ondje većina nikad ne izračuna. Ako podzadatak ima
  više rezultata (npr. 50 cm³ i 50 000 mm³, ili A i B), pitanje je praznina sa svim vrijednostima. Ne piši rezultat u tekst
  sljedećeg pitanja prije nego ga je učenik izračunao. Praznina ima najviše 4 mjesta: kad rezultata ima više, u pitanje
  stavi glavne vrijednosti (barem jednu iz svakog dijela rezultata), a ostale u „zasto“. Aplikacija to provjerava.
- Za tipične greške piši „krivo“ s porukom baš za tu grešku (npr. „220 mL je završno očitanje, a ne volumen kamena“).
- Piši kratko, laički i prirodno, za učenika kojem predmet ne ide. Novi pojam prvo svakodnevnim riječima, pa formula.
- Učeniku se obraćaš u muškom rodu, kao cijela aplikacija: „Zbrojio si“, „Jesi li siguran?“. Nikad ženski rod
  („provela si“) ni dvojni oblici („sam/sama“, „prebrojio/la“). Aplikacija to provjerava.
- Matematiku piši u $…$ (KaTeX). Decimalni zarez u tekstu (1,5); u JSON brojevima točka (1.5).
- Ne crtaj ništa znakovima; za crtež koristi skicu ili widget.

IZLAZ: samo JSON objekt, bez objašnjenja oko njega:
{
  "verzija": ${VERZIJA},
  "zadatak": {"tekst": "situacija i podaci (markdown)", "skica"?: opis skice},
  "podzadaci": [
    {"id": "a", "naslov": "kratko", "pitanje": <korak Tvoj red>, "zasto": "…", "objasnjenje": "markdown",
     "pomoc": [<korak>, …], "blizanac": <korak Tvoj red>}
  ],
  "zapamti": ["…"],
  "vjezba"?: {…}
}

${VJEZBA_OPIS}
Dodaj je kad god lekcija uči pretvaranje jedinica neke veličine (i kad je pretvorba samo dio zadatka): navedi jedinice iz
lekcije i susjedne koje izvorni zadatak ili razina koriste, uključi lanac i mješoviti zapis kad ih izvor spominje ili su
prirodni za tu veličinu (duljina, masa). U lekciji bez pretvorbe jedinica izostavi „vjezba“.`;
  function sustavPrompt() {
    const katalog = Object.values(ZA_PROMPT).map((w) => {
      const uloge = new Set([void 0, "istrazi", "prikaz", "procitaj", "odaberi"].map((nacin) => w.uloga({ nacin })));
      const uloga2 = uloge.size > 1 ? "ovisi o načinu" : uloge.has(ODGOVOR) ? "Tvoj red" : "Pogledaj";
      return `### ${w.tip} (${uloga2})
${w.opis}
Primjer: ${JSON.stringify(w.primjer)}`;
    }).join("\n\n");
    return `${PRAVILA}

KATALOG WIDGETA:

${katalog}`;
  }
  function korisnickiPrompt(izvor2) {
    return [
      `PREDMET: ${izvor2.subject ?? "?"} · RAZINA: ${izvor2.level === "os78" ? "7.–8. razred osnovne škole" : izvor2.level === "ss" ? "srednja škola" : izvor2.level ?? "?"}`,
      `LEKCIJA: ${izvor2.objective_name}`,
      `ZADATAK:
${izvor2.task_text}`,
      izvor2.sketch ? `SKICA UZ ZADATAK (JSON):
${izvor2.sketch}` : null,
      `REZULTATI:
${izvor2.results}`,
      `OBJAŠNJENJE:
${izvor2.explanation}`
    ].filter(Boolean).join("\n\n");
  }
  function predlozakPrompt(lekcija) {
    return `PREDLOŽAK: interaktivna lekcija za prvi zadatak ove lekcije. ZADATAK gore je druga varijacija istog zadatka (isti
podzadaci, drugi brojevi ili situacija). Napravi istu lekciju za ZADATAK gore:
- zadrži tok, widgete, uloge koraka, broj koraka pomoći i „vjezba“ (ako je ima);
- zadatak, pitanja, odgovore, poruke za „krivo“, skicu, „zasto“ i „objasnjenje“ napiši iznova prema ZADATKU,
  REZULTATIMA i OBJAŠNJENJU gore: situacija, predmeti i brojevi su iz ZADATKA gore, i kad su isti kao u prvom zadatku;
  ono iz prvog zadatka čega u ZADATKU gore nema (brojevi, predmeti, imena) ne smije ostati;
- blizance i primjere u pomoći smisli nove, s brojevima i rezultatima drukčijima i od novog pitanja;
- ako novi zadatak traži nešto čega u predlošku nema (npr. očitanje s instrumenta), prilagodi lekciju prema pravilima gore.
${JSON.stringify(lekcija)}`;
  }
  function popravakPrompt(greske) {
    return `Provjera aplikacije našla je probleme u tvojoj lekciji. Ispravi ih i vrati CIJELU ispravljenu lekciju kao JSON (isti format):
- ${greske.join("\n- ")}`;
  }
  return __toCommonJS(stdin_exports);
})();
