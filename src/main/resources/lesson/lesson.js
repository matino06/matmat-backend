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
    const { a, b, mreza: mreza3 } = s;
    if (!positive(a, b)) return { checks: [fail("stranice pravokutnika moraju biti pozitivni brojevi")] };
    const checks = [...numbersInText(Object.values(s.oznake ?? {}), text)];
    if (mreza3) {
      const n = a / mreza3 * (b / mreza3);
      checks.push({ ok: positive(mreza3) && n <= 900, text: `mreža ${num(mreza3)}: ${Math.round(n)} polja (najviše 900)` });
    }
    if (checks.some((x) => !x.ok)) return { checks };
    const T = fit([[0, 0], [a, b]], W, H, 52);
    let body = "";
    if (mreza3) {
      for (let x = mreza3; x < a - 1e-9; x += mreza3) body += line(T([x, 0]), T([x, b]), "grid strong");
      for (let y = mreza3; y < b - 1e-9; y += mreza3) body += line(T([0, y]), T([a, y]), "grid strong");
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
      const c = [W / 2, 200], R2 = 150;
      const ang = (u) => Math.PI * (1 - (u - od) / (to - od));
      const at = (u, r) => [c[0] + r * Math.cos(ang(u)), c[1] - r * Math.sin(ang(u))];
      let body2 = `<path class="ln" d="M${f(c[0] - R2)},${c[1]} A${R2},${R2} 0 0 1 ${f(c[0] + R2)},${c[1]}"/>`;
      for (let i = 0; i <= Math.round(n); i++) {
        const u = od + i * razmak;
        const long = isBig(u);
        body2 += line(at(u, R2), at(u, R2 - (long ? 16 : 9)), "ln thin");
        if (long) body2 += label(at(u, R2 - 30), num(u), "tick");
      }
      body2 += line(c, at(values[0], R2 - 6), "ln needle") + `<circle class="pt" cx="${c[0]}" cy="${c[1]}" r="5"/>`;
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
    const G3 = (s.sile ?? []).find((x) => x.smjer === "dolje");
    const N = (s.sile ?? []).find((x) => x.smjer === "okomito_od_podloge");
    const checks = [];
    if (!(s.sile ?? []).length || !(s.sile ?? []).every((x) => positive(x.iznos))) checks.push(fail("svaka sila treba pozitivan iznos"));
    if (G3 && N && !flat) {
      const exp = G3.iznos * Math.cos(th);
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
    const len2 = (v) => 30 + v / max * 70;
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
      const end = [c[0] + d[0] * len2(x.iznos), c[1] + d[1] * len2(x.iznos)];
      const cls = `c${i % 4 + 1}`;
      body += arrow(c, end, cls);
      body += label([end[0] + d[0] * 14 + (d[1] === 1 && d[0] === 0 ? 14 : 0), end[1] + d[1] * 14], x.oznaka, `lbl ${cls}-t`);
    });
    if (s.rastav && G3 && !flat) {
      const gp = G3.iznos * Math.sin(th);
      const gn = G3.iznos * Math.cos(th);
      const e1 = [c[0] - along[0] * len2(gp), c[1] - along[1] * len2(gp)];
      const e2 = [c[0] - normal[0] * len2(gn), c[1] - normal[1] * len2(gn)];
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
    s = s.replace(/\s+/g, "").replace(/^\+(?=\d)/, "");
    if (!s) return null;
    const sci = parseSci(s);
    if (sci !== void 0) return sci;
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
  var SUP = {
    "⁰": "0",
    "¹": "1",
    "²": "2",
    "³": "3",
    "⁴": "4",
    "⁵": "5",
    "⁶": "6",
    "⁷": "7",
    "⁸": "8",
    "⁹": "9",
    "⁻": "-"
  };
  function parseSci(s) {
    const t = s.replace(/[⁰¹²³⁴⁵⁶⁷⁸⁹⁻]+$/, (m2) => "^" + [...m2].map((c) => SUP[c]).join("")).replace(/\\cdot|\\times/g, "·");
    const m = t.match(/^(-?)(?:(\d+(?:[.,]\d+)?)[·*x×⋅])?10\^[{(]?([+-]?\d+)[})]?$/) ?? t.match(/^(-?)(\d+(?:[.,]\d+)?)[eE]([+-]?\d+)$/);
    if (!m) return void 0;
    const mant = m[2] == null ? 1 : toNumber(m[2]);
    if (mant == null) return null;
    const n = Number(`${m[1]}${mant}e${Number(m[3])}`);
    return Number.isFinite(n) ? n : null;
  }
  function toNumber(s) {
    if (!/^-?\d*(?:[.,]\d+)?$/.test(s) || s === "-" || s === "") return null;
    const n = Number(s.replace(",", "."));
    return Number.isFinite(n) ? n : null;
  }
  function tolerance(value, given) {
    if (typeof given === "number" && given >= 0) return given;
    return value === 0 ? 1e-9 : Math.abs(value) * 5e-3;
  }
  function near(a, b, tol2) {
    return typeof a === "number" && typeof b === "number" && // rezerva za greške zaokruživanja u računu, razmjerna veličini brojeva (ne apsolutna)
    Math.abs(a - b) <= tol2 + 1e-12 * Math.max(Math.abs(a), Math.abs(b));
  }
  function formatNumber(n, digits = 3) {
    if (typeof n !== "number" || !Number.isFinite(n)) return "";
    if (n !== 0 && (Math.abs(n) >= 1e9 || Math.abs(n) < 1e-6))
      return formatSci(n);
    const lead = n === 0 ? 0 : Math.max(0, -Math.floor(Math.log10(Math.abs(n))) - 1);
    const d = Math.min(12, Math.max(digits, lead + 3));
    let t = n.toFixed(d);
    if (t.includes(".")) t = t.replace(/\.?0+$/, "");
    if (t === "-0") t = "0";
    return t.replace("-", "−").replace(".", ",");
  }
  var SUPERSKRIPT = {
    0: "⁰",
    1: "¹",
    2: "²",
    3: "³",
    4: "⁴",
    5: "⁵",
    6: "⁶",
    7: "⁷",
    8: "⁸",
    9: "⁹",
    "-": "⁻"
  };
  function formatSci(n, sig = 3) {
    if (typeof n !== "number" || !Number.isFinite(n)) return "";
    if (n === 0) return "0";
    const r = Number(n.toPrecision(sig));
    const e = Math.floor(Math.log10(Math.abs(r)));
    if (e >= -3 && e < 6) return formatNumber(r, Math.max(0, sig - 1 - e));
    const [m] = r.toExponential(sig - 1).split("e");
    const mant = String(Number(m)).replace("-", "−").replace(".", ",");
    return `${mant}·10${[...String(e)].map((c) => SUPERSKRIPT[c]).join("")}`;
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
  function validateKrivo(krivo, answer, tol2, need, where = "krivo") {
    if (krivo == null) return;
    if (!need(Array.isArray(krivo), `${where} mora biti popis`)) return;
    krivo.forEach((k, i) => {
      need(isNum(k?.vrijednost), `${where}[${i}].vrijednost mora biti broj`);
      need(isText(k?.poruka), `${where}[${i}].poruka mora biti tekst`);
      if (isNum(k?.vrijednost) && isNum(answer)) need(!near(k.vrijednost, answer, tol2), `${where}[${i}].vrijednost ${formatNumber(k.vrijednost)} jednaka je točnom odgovoru`);
    });
  }
  function checkNumber({ input, answer, tol: tol2, unit, krivo, savjet: savjet3 }) {
    const n = parseNumber(input, unit);
    if (n == null) return { tocno: false, poruka: "Upiši broj (decimalni zarez je u redu, npr. 1,5).", prazno: true };
    const t = tolerance(answer, tol2);
    if (near(n, answer, t)) return { tocno: true };
    const hit = (krivo ?? []).find((k) => near(n, k.vrijednost, tolerance(k.vrijednost, k.tolerancija)));
    if (hit) return { tocno: false, poruka: hit.poruka };
    const decimalno = Number.isInteger(answer) ? null : /^\s*[-−]?[1-9]\d{0,2}\.\d{3}\s*$/.test(String(input).replace(unit, "")) ? parseNumber(String(input).replace(".", ","), unit) : null;
    if (decimalno != null && near(decimalno, answer, t)) return { tocno: true };
    return { tocno: false, poruka: savjet3 || "Nije točno. Provjeri račun i jedinicu." };
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
    ],
    // fizika (matura)
    brzina: [
      ["km/s", 1e3],
      ["m/s", 1],
      ["km/h", 1 / 3.6],
      ["cm/s", 0.01]
    ],
    sila: [
      ["MN", 1e6],
      ["kN", 1e3],
      ["N", 1],
      ["mN", 1e-3]
    ],
    tlak: [
      ["MPa", 1e6],
      ["bar", 1e5],
      ["kPa", 1e3],
      ["hPa", 100],
      ["mbar", 100],
      ["Pa", 1]
    ],
    energija: [
      ["kWh", 36e5],
      ["MJ", 1e6],
      ["kJ", 1e3],
      ["J", 1],
      ["mJ", 1e-3]
    ],
    snaga: [
      ["MW", 1e6],
      ["kW", 1e3],
      ["W", 1],
      ["mW", 1e-3]
    ],
    naboj: [
      ["C", 1],
      ["mC", 1e-3],
      ["µC", 1e-6],
      ["nC", 1e-9],
      ["pC", 1e-12]
    ],
    kapacitet: [
      ["F", 1],
      ["mF", 1e-3],
      ["µF", 1e-6],
      ["nF", 1e-9],
      ["pF", 1e-12]
    ],
    struja: [
      ["kA", 1e3],
      ["A", 1],
      ["mA", 1e-3],
      ["µA", 1e-6]
    ],
    napon: [
      ["MV", 1e6],
      ["kV", 1e3],
      ["V", 1],
      ["mV", 1e-3]
    ],
    otpor: [
      ["MΩ", 1e6],
      ["kΩ", 1e3],
      ["Ω", 1],
      ["mΩ", 1e-3]
    ],
    magnetsko_polje: [
      ["T", 1],
      ["mT", 1e-3],
      ["µT", 1e-6]
    ],
    magnetski_tok: [
      ["Wb", 1],
      ["mWb", 1e-3],
      ["µWb", 1e-6]
    ],
    frekvencija: [
      ["GHz", 1e9],
      ["MHz", 1e6],
      ["kHz", 1e3],
      ["Hz", 1]
    ],
    protok: [
      ["m³/s", 1],
      ["m³/h", 1 / 3600],
      ["L/s", 1e-3],
      ["L/min", 1e-3 / 60]
    ],
    molarna_masa: [
      ["kg/mol", 1],
      ["g/mol", 1e-3]
    ]
  };
  var ALIASES = {
    m2: "m²",
    cm2: "cm²",
    mm2: "mm²",
    dm2: "dm²",
    km2: "km²",
    m3: "m³",
    cm3: "cm³",
    dm3: "dm³",
    mm3: "mm³",
    l: "L",
    ml: "mL",
    dl: "dL",
    cl: "cL",
    hl: "hL",
    "g/cm3": "g/cm³",
    "kg/m3": "kg/m³",
    "kg/dm3": "kg/dm³",
    "km/hr": "km/h",
    kmh: "km/h",
    μC: "µC",
    uC: "µC",
    μF: "µF",
    uF: "µF",
    μA: "µA",
    uA: "µA",
    μT: "µT",
    uT: "µT",
    μWb: "µWb",
    ohm: "Ω",
    Ohm: "Ω",
    kohm: "kΩ",
    kOhm: "kΩ",
    Mohm: "MΩ",
    MOhm: "MΩ",
    Ω: "Ω",
    kΩ: "kΩ",
    MΩ: "MΩ",
    hz: "Hz",
    "m3/s": "m³/s",
    "m3/h": "m³/h",
    "l/s": "L/s",
    "l/min": "L/min",
    khz: "kHz",
    mhz: "MHz"
  };
  var normUnit = (u) => typeof u === "string" ? ALIASES[u.trim()] ?? u.trim() : u;
  function factor(velicina3, unit) {
    const row = UNITS[velicina3]?.find(([u]) => u === normUnit(unit));
    return row ? row[1] : null;
  }
  function convert(velicina3, value, from, to) {
    const a = factor(velicina3, from);
    const b = factor(velicina3, to);
    if (a == null || b == null) return null;
    return value * a / b;
  }
  var unitIndex = (velicina3, unit) => (UNITS[velicina3] ?? []).findIndex(([u]) => u === normUnit(unit));
  function toBase(velicina3, values, units) {
    const vs = [].concat(values);
    const us = [].concat(units);
    if (!vs.length || vs.length !== us.length) return null;
    let sum = 0;
    for (let i = 0; i < vs.length; i++) {
      const f2 = factor(velicina3, us[i]);
      if (f2 == null || typeof vs[i] !== "number" || !Number.isFinite(vs[i]))
        return null;
      sum += vs[i] * f2;
    }
    return sum;
  }
  function fromBase(velicina3, base, unit) {
    const f2 = factor(velicina3, unit);
    return f2 == null || base == null ? null : base / f2;
  }
  function parseQuantity(text) {
    const t = String(text ?? "").trim().replace(/\$/g, "").replace(/\\text\{([^}]*)\}/g, "$1").replace(/\\[ ,;]/g, " ").replace(/\^2/g, "²").replace(/\^3/g, "³");
    const re = /(-?\d+(?:[.,]\d+)?)\s*([^\s\d]+)/g;
    const parts = [...t.matchAll(re)];
    if (!parts.length || t.replace(re, "").trim()) return null;
    const units = parts.map((m) => normUnit(m[2]));
    const values = parts.map((m) => parseNumber(m[1]));
    for (const velicina3 of Object.keys(UNITS)) {
      const base = toBase(velicina3, values, units);
      if (base != null) return { velicina: velicina3, base };
    }
    return null;
  }
  function staircase(velicina3, from, to) {
    const list = UNITS[velicina3] ?? [];
    const idx = [...[].concat(from), ...[].concat(to)].map(
      (u) => unitIndex(velicina3, u)
    );
    if (!idx.length || idx.some((i) => i < 0)) return [];
    return list.slice(Math.min(...idx), Math.max(...idx) + 1).map(([u, f2]) => ({ jedinica: u, faktor: f2 }));
  }
  function typicalMistakes(velicina3, value, from, to) {
    const right = convert(velicina3, value, from, to);
    if (right == null || right === value) return [];
    const out = [];
    const ratio = right / value;
    const wrongDir = value / ratio;
    out.push({
      vrijednost: wrongDir,
      poruka: `Pretvorba je u krivom smjeru. Iz ${from} u ${to} broj ${ratio > 1 ? "raste" : "pada"}, jer je ${ratio > 1 ? `${to} manja` : `${to} veća`} jedinica.`
    });
    const power = { povrsina: 2, volumen: 3 }[velicina3];
    const special = ["L", "dL", "cL", "mL", "hL", "ha", "a"];
    if (power && !special.includes(normUnit(from)) && !special.includes(normUnit(to))) {
      const asLength = value * 10 ** (Math.log10(ratio) / power);
      out.push({
        vrijednost: asLength,
        poruka: power === 2 ? "Ovo je pretvorba kao za duljinu. Kod površine svaki korak između susjednih jedinica množi se sa 100, ne s 10." : "Ovo je pretvorba kao za duljinu. Kod volumena svaki korak između susjednih jedinica množi se s 1000, ne s 10."
      });
    }
    return out.filter(
      (m) => Number.isFinite(m.vrijednost) && Math.abs(m.vrijednost - right) > 1e-9 * Math.max(1, Math.abs(right))
    );
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
    round: Math.round,
    // n! (samo za prirodne brojeve i 0)
    fact: (n) => {
      if (!Number.isInteger(n) || n < 0 || n > 170) return NaN;
      let f2 = 1;
      for (let k = 2; k <= n; k++) f2 *= k;
      return f2;
    }
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
  var klizacVrijednost = (s, vals) => {
    try {
      const v = compile2(s.izrazi[s.cilj.izraz ?? 0].izraz).fn(vals);
      return Number.isFinite(v) ? v : null;
    } catch {
      return null;
    }
  };
  var klizacTol = (s) => tolerance(
    s.cilj.vrijednost,
    s.cilj.tolerancija ?? Math.max(Math.abs(s.cilj.vrijednost) * 0.01, 1e-9)
  );
  function klizacRjesenje(s) {
    const ks = s.klizaci;
    const koraci = ks.map((k) => Math.round((k.max - k.min) / k.korak) + 1);
    if (koraci.reduce((a, b) => a * b, 1) > 2e5) return void 0;
    const t = klizacTol(s);
    const idx = ks.map(() => 0);
    for (; ; ) {
      const vals = Object.fromEntries(
        ks.map((k, i2) => [
          k.id,
          Number((k.min + idx[i2] * k.korak).toPrecision(12))
        ])
      );
      const v = klizacVrijednost(s, vals);
      if (v != null && near(v, s.cilj.vrijednost, t)) return vals;
      let i = 0;
      while (i < ks.length && ++idx[i] >= koraci[i]) idx[i++] = 0;
      if (i === ks.length) return null;
    }
  }
  var klizac = {
    tip: "klizac",
    uloga: (s) => s?.nacin === "postavi" ? ODGOVOR : POGLEDAJ,
    istrazi: (s) => s?.nacin !== "postavi",
    opis: `Istraživanje (nije zadatak): klizači i rezultat koji se računa uživo. Polja: tekst, klizaci: [{id (npr. "m"), oznaka (npr. "masa m"),
min, max, korak, pocetak, jedinica?}] (1–3), izrazi: [{oznaka (markdown, npr. "$\\\\rho$"), izraz (npr. "m/V", varijable su id-jevi klizača; + - * / ^ sqrt),
jedinica?, decimale?}] (1–2).`,
    primjer: {
      tip: "klizac",
      tekst: "Mijenjaj masu i volumen i gledaj gustoću.",
      klizaci: [
        {
          id: "m",
          oznaka: "masa $m$",
          min: 20,
          max: 200,
          korak: 10,
          pocetak: 60,
          jedinica: "g"
        },
        {
          id: "V",
          oznaka: "volumen $V$",
          min: 10,
          max: 100,
          korak: 10,
          pocetak: 40,
          jedinica: "cm³"
        }
      ],
      izrazi: [
        {
          oznaka: "gustoća $\\rho = m/V$",
          izraz: "m/V",
          jedinica: "g/cm³",
          decimale: 2
        }
      ]
    },
    primjeri: [
      {
        tip: "klizac",
        nacin: "postavi",
        tekst: "Transformator: primarna zavojnica ima 1000 zavoja i napon 230 V. Namjesti broj zavoja sekundarne zavojnice tako da sekundarni napon bude 23 V.",
        klizaci: [
          {
            id: "N2",
            oznaka: "zavoji sekundara $N_2$",
            min: 0,
            max: 500,
            korak: 10,
            pocetak: 250
          }
        ],
        izrazi: [
          {
            oznaka: "sekundarni napon $U_2 = U_1 \\cdot \\frac{N_2}{N_1}$",
            izraz: "230*N2/1000",
            jedinica: "V",
            decimale: 1
          }
        ],
        cilj: { izraz: 0, vrijednost: 23, skriveno: true }
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "klizac treba „tekst“ (što učenik istražuje)");
      need(
        s.nacin == null || ["istrazi", "postavi"].includes(s.nacin),
        "klizac.nacin mora biti istrazi ili postavi"
      );
      if (s.nacin !== "postavi") noAnswerFields(s, need);
      const ids = [];
      if (need(
        Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 3,
        "klizac treba 1–3 klizača"
      )) {
        s.klizaci.forEach((k, i) => {
          need(
            ID.test(k?.id ?? ""),
            `klizaci[${i}].id mora biti ime varijable (slova, bez razmaka)`
          );
          ids.push(k?.id);
          const ok = need(
            [k?.min, k?.max, k?.korak, k?.pocetak].every(isNum),
            `klizaci[${i}] treba brojeve min, max, korak, pocetak`
          );
          if (ok) {
            need(
              k.max > k.min && k.korak > 0,
              `klizaci[${i}]: mora biti min < max i korak > 0`
            );
            need(
              k.pocetak >= k.min && k.pocetak <= k.max,
              `klizaci[${i}].pocetak mora biti između min i max`
            );
            need(
              (k.max - k.min) / k.korak <= 1e3,
              `klizaci[${i}] ima previše koraka (najviše 1000)`
            );
          }
        });
        need(
          new Set(ids).size === ids.length,
          "id-jevi klizača moraju biti različiti"
        );
      }
      if (need(
        Array.isArray(s.izrazi) && s.izrazi.length >= 1 && s.izrazi.length <= 2,
        "klizac treba 1–2 izraza"
      )) {
        s.izrazi.forEach((iz, i) => {
          try {
            const c = compile2(iz?.izraz);
            const unknown = c.vars.filter((v) => !ids.includes(v));
            need(
              !unknown.length,
              `izrazi[${i}] koristi nepoznate varijable: ${unknown.join(", ")}`
            );
          } catch (e) {
            need(false, `izrazi[${i}].izraz: ${e.message}`);
          }
        });
      }
      if (s.nacin === "postavi" && !list.length) {
        const c = s.cilj;
        if (need(
          c && isNum(c.vrijednost),
          "klizac (postavi) treba cilj: {izraz, vrijednost}"
        )) {
          need(
            c.izraz == null || Number.isInteger(c.izraz) && c.izraz >= 0 && c.izraz < s.izrazi.length,
            "cilj.izraz mora biti indeks izraza"
          );
          if (!list.length) {
            const r = klizacRjesenje(s);
            need(
              r !== null,
              `cilj ${formatNumber(c.vrijednost)} se ne može postići ni jednim položajem klizača (provjeri min, max i korak)`
            );
            const start = klizacVrijednost(
              s,
              Object.fromEntries(s.klizaci.map((k) => [k.id, k.pocetak]))
            );
            need(
              start == null || !near(start, c.vrijednost, klizacTol(s)),
              "početni položaj klizača već postiže cilj"
            );
          }
        }
      }
      return list;
    },
    rjesenje: (s) => klizacRjesenje(s),
    vrijednost: (s, vals) => klizacVrijednost(s, vals),
    check(s, vals) {
      if (!vals || typeof vals !== "object")
        return { tocno: false, poruka: "Pomakni klizač.", prazno: true };
      const v = klizacVrijednost(s, vals);
      const iz = s.izrazi[s.cilj.izraz ?? 0];
      if (v != null && near(v, s.cilj.vrijednost, klizacTol(s)))
        return { tocno: true };
      if (v == null)
        return {
          tocno: false,
          poruka: "Za ovaj položaj klizača rezultat se ne može izračunati."
        };
      return {
        tocno: false,
        poruka: `Sada je rezultat ${withUnit(v, iz.jedinica)}, a treba ${withUnit(s.cilj.vrijednost, iz.jedinica)}. ${v < s.cilj.vrijednost ? "Treba više." : "Treba manje."}`
      };
    },
    tocanOdgovor(s) {
      const r = klizacRjesenje(s);
      return r ? s.klizaci.map((k) => `${k.oznaka} = ${withUnit(r[k.id], k.jedinica)}`).join(", ") : withUnit(s.cilj.vrijednost, s.izrazi[s.cilj.izraz ?? 0].jedinica);
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
    primjer: {
      tip: "mjerilo",
      nacin: "procitaj",
      instrument: "menzura",
      od: 0,
      do: 300,
      razmak: 20,
      oznaka_svakih: 100,
      jedinica: "mL",
      vrijednost: 140,
      tekst: "Očitaj volumen vode u menzuri."
    },
    validate(s) {
      const { list, need } = problems();
      need(
        ["istrazi", "procitaj", "postavi"].includes(s.nacin),
        "mjerilo.nacin mora biti istrazi, procitaj ili postavi"
      );
      if (!need(
        s.instrument in INSTRUMENTI,
        `mjerilo.instrument mora biti jedan od: ${Object.keys(INSTRUMENTI).join(", ")}`
      ))
        return list;
      if (!need(
        [s.od, s.do, s.razmak, s.vrijednost].every(isNum),
        "mjerilo treba brojeve od, do, razmak i vrijednost"
      ))
        return list;
      need(isText(s.tekst), "mjerilo treba „tekst“");
      const n = (s.do - s.od) / s.razmak;
      need(
        s.do > s.od && s.razmak > 0,
        "mjerilo: mora biti od < do i razmak > 0"
      );
      need(
        Math.abs(n - Math.round(n)) < 1e-6,
        `od ${s.od} do ${s.do} s razmakom ${s.razmak} nije cijeli broj podjeljaka`
      );
      need(
        n <= INSTRUMENTI[s.instrument],
        `${formatNumber(n)} podjeljaka je previše za ${s.instrument} (najviše ${INSTRUMENTI[s.instrument]}); povećaj razmak ili smanji raspon`
      );
      if (s.oznaka_svakih != null)
        need(
          isNum(s.oznaka_svakih) && aligned(s.oznaka_svakih, s.razmak),
          "oznaka_svakih mora biti višekratnik razmaka"
        );
      need(
        s.vrijednost >= s.od && s.vrijednost <= s.do,
        "vrijednost mora biti na ljestvici (između od i do)"
      );
      if (s.nacin !== "istrazi")
        need(
          aligned(s.vrijednost - s.od, s.razmak),
          "vrijednost mora biti točno na crtici (višekratnik razmaka od početka), inače se ne može pouzdano očitati"
        );
      if (s.uranjanje != null) {
        need(
          s.nacin === "istrazi" && s.instrument === "menzura",
          "uranjanje postoji samo za menzuru u načinu istrazi"
        );
        need(
          isNum(s.uranjanje?.nakon) && s.uranjanje.nakon > s.vrijednost && s.uranjanje.nakon <= s.do,
          "uranjanje.nakon mora biti veće od vrijednosti i na ljestvici"
        );
        need(
          isText(s.uranjanje?.gumb),
          "uranjanje treba tekst gumba (npr. „Uroni kamen“)"
        );
      }
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      else validateKrivo(s.krivo, s.vrijednost, mjeriloTol(s), need);
      return list;
    },
    check(s, input) {
      if (s.nacin === "postavi") {
        if (!isNum(input))
          return { tocno: false, poruka: "Povuci na vrijednost.", prazno: true };
        if (near(input, s.vrijednost, mjeriloTol(s))) return { tocno: true };
        return { tocno: false, poruka: offByDivisions(input, s) };
      }
      const auto = [s.vrijednost + s.razmak, s.vrijednost - s.razmak].filter((v) => v >= s.od && v <= s.do).map((v) => ({
        vrijednost: v,
        poruka: `${withUnit(v, s.jedinica)} je jedan podjeljak ${v > s.vrijednost ? "previše" : "premalo"}. Kreni od najbliže označene crte i broji razmake od po ${withUnit(s.razmak, s.jedinica)}.`
      }));
      return checkNumber({
        input,
        answer: s.vrijednost,
        tol: mjeriloTol(s),
        unit: s.jedinica,
        krivo: [...s.krivo ?? [], ...auto],
        savjet: s.savjet || `Jedan podjeljak vrijedi ${withUnit(s.razmak, s.jedinica)}. Kreni od najbliže označene crte.`
      });
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
    primjer: {
      tip: "mreza",
      nacin: "prebroji",
      redaka: 6,
      stupaca: 8,
      velicina: 1,
      jedinica: "cm",
      oznacene: [
        [1, 2],
        [1, 3],
        [2, 2],
        [2, 3],
        [2, 4]
      ],
      polovice: [
        [1, 4],
        [3, 3]
      ],
      tekst: "Kolika je površina otiska?"
    },
    validate(s) {
      const { list, need } = problems();
      need(
        ["istrazi", "prebroji", "oznaci"].includes(s.nacin),
        "mreza.nacin mora biti istrazi, prebroji ili oznaci"
      );
      const dims = need(
        Number.isInteger(s.redaka) && Number.isInteger(s.stupaca) && s.redaka >= 1 && s.stupaca >= 1 && s.redaka <= 20 && s.stupaca <= 20,
        "mreza treba cijele brojeve redaka i stupaca (1–20)"
      );
      need(
        isNum(s.velicina) && s.velicina > 0,
        "mreza.velicina mora biti pozitivan broj"
      );
      need(
        ["mm", "cm", "dm", "m"].includes(s.jedinica),
        "mreza.jedinica mora biti mm, cm, dm ili m"
      );
      need(isText(s.tekst), "mreza treba „tekst“");
      const cells = (name) => {
        const v = s[name];
        if (v == null) return [];
        if (!need(
          Array.isArray(v) && v.every(
            (c) => Array.isArray(c) && c.length === 2 && c.every(Number.isInteger)
          ),
          `${name} mora biti popis [redak, stupac]`
        ))
          return [];
        if (dims)
          need(
            v.every(
              ([r, c]) => r >= 0 && c >= 0 && r < s.redaka && c < s.stupaca
            ),
            `${name}: kvadratić izvan mreže`
          );
        return v.map(cellKey);
      };
      const full = cells("oznacene");
      const half = cells("polovice");
      need(
        (/* @__PURE__ */ new Set([...full, ...half])).size === full.length + half.length,
        "isti kvadratić je i cijeli i polovica (ili se ponavlja)"
      );
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      if (s.nacin === "prebroji") {
        need(
          full.length + half.length > 0,
          "mreza prebroji treba lik: oznacene i/ili polovice"
        );
        validateKrivo(s.krivo, mrezaAnswer(s), tolerance(mrezaAnswer(s)), need);
      }
      if (s.nacin === "oznaci") {
        if (need(isNum(s.cilj) && s.cilj > 0, "mreza oznaci treba cilj (površinu)")) {
          const n = s.cilj / (s.velicina * s.velicina);
          need(
            Math.abs(n - Math.round(n)) < 1e-6,
            "cilj mora biti cijeli broj kvadratića"
          );
          if (dims)
            need(n <= s.redaka * s.stupaca, "cilj je veći od cijele mreže");
        }
      }
      return list;
    },
    // prebroji: upisani broj; oznaci: broj označenih kvadratića
    check(s, input) {
      const unit = `${s.jedinica}²`;
      if (s.nacin === "oznaci") {
        if (!isNum(input) || input === 0)
          return {
            tocno: false,
            poruka: "Označi kvadratiće dodirom.",
            prazno: true
          };
        const area = input * s.velicina * s.velicina;
        if (near(area, s.cilj, 1e-9)) return { tocno: true };
        return {
          tocno: false,
          poruka: `Označeno je ${withUnit(area, unit)}, a treba ${withUnit(s.cilj, unit)}.`
        };
      }
      const nf = (s.oznacene ?? []).length;
      const nh = (s.polovice ?? []).length;
      const a = s.velicina * s.velicina;
      const auto = nh ? [
        {
          vrijednost: (nf + nh) * a,
          poruka: "Polovice si brojao kao cijele kvadratiće. Dvije polovice zajedno čine jedan kvadratić."
        },
        {
          vrijednost: nf * a,
          poruka: "Polovice si izostavio. I one pokrivaju dio plohe: dvije polovice čine jedan kvadratić."
        }
      ] : [];
      return checkNumber({
        input,
        answer: mrezaAnswer(s),
        unit,
        krivo: [...s.krivo ?? [], ...auto],
        savjet: s.savjet || `Prebroji cijele kvadratiće, dodaj polovice (dvije čine jedan) i pomnoži površinom jednog kvadratića (${withUnit(a, unit)}).`
      });
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
    primjer: {
      tip: "ocitaj_graf",
      nacin: "ocitaj",
      x: [0, 60],
      y: [0, 150],
      os_x: "V (cm³)",
      os_y: "m (g)",
      funkcije: [
        { izraz: "2.4*x", oznaka: "A" },
        { izraz: "0.8*x", oznaka: "B" }
      ],
      ocitaj: { oznaka: "A", x: 20 },
      jedinica: "g",
      tekst: "Kolika je masa uzorka A volumena 20 cm³?"
    },
    validate(s) {
      const { list, need } = problems();
      need(
        ["prikaz", "ocitaj"].includes(s.nacin),
        "ocitaj_graf.nacin mora biti prikaz ili ocitaj"
      );
      const axesOk = need(
        Array.isArray(s.x) && Array.isArray(s.y) && s.x.length === 2 && s.y.length === 2 && [...s.x, ...s.y].every(isNum) && s.x[1] > s.x[0] && s.y[1] > s.y[0],
        "ocitaj_graf treba x i y kao [od, do] uz od < do"
      );
      need(isText(s.tekst), "ocitaj_graf treba „tekst“");
      const fns = {};
      if (need(
        Array.isArray(s.funkcije) && s.funkcije.length >= 1 && s.funkcije.length <= 3,
        "ocitaj_graf treba 1–3 funkcije"
      )) {
        s.funkcije.forEach((fn, i) => {
          try {
            const c = compile2(fn?.izraz);
            need(
              c.vars.every((v) => v === "x"),
              `funkcije[${i}] smije koristiti samo varijablu x`
            );
            fns[fn?.oznaka ?? `f${i}`] = (x) => c.fn({ x });
          } catch (e) {
            need(false, `funkcije[${i}].izraz: ${e.message}`);
          }
        });
      }
      if (s.nacin === "prikaz") noAnswerFields(s, need);
      if (s.nacin === "ocitaj" && axesOk) {
        const q = s.ocitaj;
        if (need(
          q && fns[q.oznaka],
          "ocitaj.oznaka mora biti oznaka jedne od funkcija"
        )) {
          const a = grafAnswer(s, fns);
          need(
            isNum(a),
            "vrijednost za očitati nije na grafu (izvan raspona osi)"
          );
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
        const other = grafAnswer(
          { ...s, ocitaj: { ...q, oznaka: fn.oznaka } },
          fns
        );
        if (isNum(other))
          auto.push({
            vrijednost: other,
            poruka: `To je vrijednost s grafa ${fn.oznaka}, a traži se graf ${q.oznaka}.`
          });
      }
      const given = "x" in q ? q.x : q.y;
      auto.push({
        vrijednost: given,
        poruka: `Pročitao si vrijednost na krivoj osi. Kreni od ${"x" in q ? s.os_x : s.os_y} = ${formatNumber(given)} do grafa, pa očitaj na drugoj osi.`
      });
      return checkNumber({
        input,
        answer,
        tol: grafTol(s),
        unit: s.jedinica,
        krivo: [...s.krivo ?? [], ...auto],
        savjet: s.savjet || "Dodirni graf na zadanoj vrijednosti i prati crtu do druge osi."
      });
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
  var izvor = (s) => ({
    vrijednosti: [].concat(s.vrijednost),
    jedinice: [].concat(s.od).map(normUnit)
  });
  var zapisIzvora = (s) => {
    const { vrijednosti: vrijednosti2, jedinice } = izvor(s);
    return vrijednosti2.map((v, i) => withUnit(v, jedinice[i])).join(" ");
  };
  function pretvorbaRezultati(s) {
    const { vrijednosti: vrijednosti2, jedinice } = izvor(s);
    const base = toBase(s.velicina, vrijednosti2, jedinice);
    return lanac(s).map((u) => fromBase(s.velicina, base, u));
  }
  var prepisano = (velicina3, od, cilj) => unitIndex(velicina3, cilj) > unitIndex(velicina3, od) ? `Prepisao si broj iz ${od}. Kad promijeniš jedinicu, mijenja se i broj: ${cilj} je manja jedinica, pa ih treba više.` : `Prepisao si broj iz ${od}. Kad promijeniš jedinicu, mijenja se i broj: ${cilj} je veća jedinica, pa ih treba manje.`;
  function drugeJedinice(velicina3, tocno, cilj) {
    const base = toBase(velicina3, tocno, cilj);
    return UNITS[velicina3].filter(([u]) => u !== cilj).map(([u]) => ({
      vrijednost: fromBase(velicina3, base, u),
      poruka: `To je isti iznos u ${u}, a ovdje ga treba zapisati u ${cilj}.`
    }));
  }
  var PROMPT_VELICINE = [
    "duljina",
    "povrsina",
    "volumen",
    "masa",
    "vrijeme",
    "gustoca"
  ];
  var pretvorba_jedinica = {
    tip: "pretvorba_jedinica",
    uloga: () => ODGOVOR,
    opis: `Pretvorba jedinica: velicina (${PROMPT_VELICINE.join(" | ")}), vrijednost, od, do (jedinice, npr. "mm²" → "cm²"), tekst?, stube? (true: prikaži stube
jedinica s faktorima – za korak pomoći, ne za prvi pokušaj). Točan rezultat i tipične greške (faktor kao za duljinu, krivi smjer) računa aplikacija.
Lanac: do je popis 2–4 jedinice ("8 m = … dm = … cm = … mm": vrijednost 8, od "m", do ["dm", "cm", "mm"]); učenik upisuje svaku.
Mješoviti zapis: vrijednost i od su popisi iste duljine, od veće jedinice prema manjoj ("2 m 7 dm": vrijednost [2, 7], od ["m", "dm"]).
Jedinice: ${PROMPT_VELICINE.map((k) => `${k}: ${UNITS[k].map(([u]) => u).join(", ")}`).join("; ")}.`,
    primjer: {
      tip: "pretvorba_jedinica",
      velicina: "povrsina",
      vrijednost: 3600,
      od: "mm²",
      do: "cm²",
      tekst: "Pretvori površinu papira u cm²."
    },
    validate(s) {
      const { list, need } = problems();
      if (!need(
        s.velicina in UNITS,
        `pretvorba_jedinica.velicina mora biti jedna od: ${Object.keys(UNITS).join(", ")}`
      ))
        return list;
      const names = UNITS[s.velicina].map(([u]) => u);
      const { vrijednosti: vrijednosti2, jedinice } = izvor(s);
      const cilj = lanac(s);
      if (!need(
        [...jedinice, ...cilj].every((u) => names.includes(normUnit(u))),
        `jedinice od/do moraju biti iz popisa za ${s.velicina}: ${names.join(", ")}`
      ))
        return list;
      need(
        vrijednosti2.length === jedinice.length,
        "kod mješovitog zapisa vrijednost i od moraju biti popisi iste duljine"
      );
      need(
        vrijednosti2.every(isNum),
        "pretvorba_jedinica treba brojčanu vrijednost"
      );
      need(
        cilj.length >= 1 && cilj.length <= 4,
        "do je jedna jedinica ili lanac od 2–4 jedinice"
      );
      if (jedinice.length > 1) {
        need(jedinice.length <= 3, "mješoviti zapis ima najviše 3 dijela");
        need(
          jedinice.every(
            (u, i) => i === 0 || unitIndex(s.velicina, u) > unitIndex(s.velicina, jedinice[i - 1])
          ),
          "mješoviti zapis ide od veće jedinice prema manjoj (2 m 7 dm)"
        );
      }
      const sve = [
        jedinice.length === 1 ? jedinice[0] : null,
        ...cilj.map(normUnit)
      ].filter(Boolean);
      need(
        new Set(sve).size === sve.length,
        "od i do moraju biti različite jedinice, a lanac bez ponavljanja"
      );
      return list;
    },
    // input: broj (jedna jedinica u „do“) ili popis brojeva (lanac)
    check(s, input) {
      const cilj = lanac(s).map(normUnit);
      const tocni = pretvorbaRezultati(s);
      const unosi = cilj.length === 1 ? [Array.isArray(input) ? input[0] : input] : [].concat(input ?? []);
      const { vrijednosti: vrijednosti2, jedinice } = izvor(s);
      for (let i = 0; i < cilj.length; i++) {
        const [prije, odJ] = i > 0 ? [tocni[i - 1], cilj[i - 1]] : jedinice.length === 1 ? [vrijednosti2[0], jedinice[0]] : [null, null];
        const krivo = [
          ...i === cilj.length - 1 ? s.krivo ?? [] : [],
          ...prije == null ? [] : typicalMistakes(s.velicina, prije, odJ, cilj[i]),
          ...prije == null ? [] : [
            {
              vrijednost: prije,
              poruka: prepisano(s.velicina, odJ, cilj[i])
            }
          ],
          ...drugeJedinice(s.velicina, tocni[i], cilj[i])
        ].filter(
          (k) => Math.abs(k.vrijednost - tocni[i]) > 1e-9 * Math.max(1, Math.abs(tocni[i]))
        );
        const r = checkNumber({
          input: unosi[i] ?? "",
          answer: tocni[i],
          unit: cilj[i],
          krivo,
          savjet: s.savjet || "Pogledaj koliko je koraka između jedinica i koliki je faktor svakog koraka."
        });
        if (!r.tocno)
          return cilj.length > 1 ? { ...r, praznina: i, poruka: `${cilj[i]}: ${r.poruka}` } : r;
      }
      return { tocno: true };
    },
    brojcaniOdgovor: (s) => pretvorbaRezultati(s).at(-1),
    tocanOdgovor: (s) => [
      zapisIzvora(s),
      ...pretvorbaRezultati(s).map(
        (v, i) => withUnit(v, normUnit(lanac(s)[i]))
      )
    ].join(" = "),
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
    primjer: {
      tip: "cestice",
      nacin: "odaberi",
      tekst: "U kojoj su kutiji čestice tekućine?",
      kutije: [
        {
          oznaka: "A",
          stanje: "cvrsto",
          poruka: "Čestice samo titraju na mjestu: to je čvrsto tijelo."
        },
        { oznaka: "B", stanje: "tekuce" },
        {
          oznaka: "C",
          stanje: "plin",
          poruka: "Čestice su daleko i slobodno lete: to je plin."
        }
      ],
      tocna: 1
    },
    validate(s) {
      const { list, need } = problems();
      need(
        ["prikaz", "odaberi"].includes(s.nacin),
        "cestice.nacin mora biti prikaz ili odaberi"
      );
      need(isText(s.tekst), "cestice treba „tekst“");
      if (need(
        Array.isArray(s.kutije) && s.kutije.length >= 2 && s.kutije.length <= 4,
        "cestice treba 2–4 kutije"
      )) {
        s.kutije.forEach((k, i) => {
          need(
            STANJA.includes(k?.stanje),
            `kutije[${i}].stanje mora biti cvrsto, tekuce ili plin`
          );
          need(isText(k?.oznaka), `kutije[${i}].oznaka mora biti tekst`);
        });
      }
      if (s.nacin === "prikaz") noAnswerFields(s, need);
      if (s.nacin === "odaberi")
        need(
          Number.isInteger(s.tocna) && s.tocna >= 0 && s.tocna < (s.kutije?.length ?? 0),
          "cestice odaberi treba tocna = indeks kutije"
        );
      return list;
    },
    check(s, index) {
      if (!Number.isInteger(index))
        return { tocno: false, poruka: "Dodirni jednu kutiju.", prazno: true };
      return index === s.tocna ? { tocno: true } : {
        tocno: false,
        poruka: s.kutije[index]?.poruka || s.savjet || "Nije ta kutija. Pogledaj kako su čestice raspoređene i kako se gibaju."
      };
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
    primjer: {
      tip: "uronjeno",
      nacin: "predvidi",
      tekst: "Što će se dogoditi sa svakim tijelom u vodi?",
      tekucina: { naziv: "voda", gustoca: 1 },
      tijela: [
        { naziv: "drvena kocka", gustoca: 0.6 },
        { naziv: "željezni uteg", gustoca: 7.9 }
      ]
    },
    validate(s) {
      const { list, need } = problems();
      need(
        ["istrazi", "predvidi"].includes(s.nacin),
        "uronjeno.nacin mora biti istrazi ili predvidi"
      );
      need(isText(s.tekst), "uronjeno treba „tekst“");
      need(
        isText(s.tekucina?.naziv) && isNum(s.tekucina?.gustoca) && s.tekucina.gustoca > 0,
        "uronjeno treba tekucina: {naziv, gustoca > 0}"
      );
      if (need(
        Array.isArray(s.tijela) && s.tijela.length >= 1 && s.tijela.length <= 4,
        "uronjeno treba 1–4 tijela"
      )) {
        s.tijela.forEach(
          (t, i) => need(
            isText(t?.naziv) && isNum(t?.gustoca) && t.gustoca > 0,
            `tijela[${i}] treba naziv i gustoću > 0`
          )
        );
      }
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      return list;
    },
    ishodi: (s) => s.tijela.map((t) => ishod(t.gustoca, s.tekucina.gustoca)),
    check(s, picks) {
      if (!Array.isArray(picks) || picks.length !== s.tijela.length || picks.some((p) => !ISHODI.includes(p)))
        return {
          tocno: false,
          poruka: "Odaberi ishod za svako tijelo.",
          prazno: true
        };
      const rf = s.tekucina.gustoca;
      for (let i = 0; i < s.tijela.length; i++) {
        const t = s.tijela[i];
        const right = ishod(t.gustoca, rf);
        if (picks[i] !== right) {
          const cmp = right === "pliva" ? "manja od" : right === "tone" ? "veća od" : "jednaka";
          return {
            tocno: false,
            poruka: `${t.naziv}: gustoća ${withUnit(t.gustoca, "g/cm³")} je ${cmp} ${right === "lebdi" ? "gustoći" : "gustoće"} tekućine (${withUnit(rf, "g/cm³")}), pa tijelo ${right}. Odlučuje gustoća, a ne masa ili veličina.`
          };
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
    // „70°“: oznaka mjere (70), a unutar sin/cos/tg kut u stupnjevima (vidi pripremi)
    [/°/g, " ° "],
    [/(\d+)\s*!/g, " fact($1)"],
    // hrvatski zapis tangensa: „tg x“
    [/\btg/g, " tan "]
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
  function pripremi(unos, varijable2 = []) {
    let s = String(unos ?? "");
    for (const [re, z] of ZNAKOVI) s = s.replace(re, z);
    const poznato = [...FUNKCIJE, ...KONSTANTE, ...varijable2, "inf"];
    const tokeni = [];
    const trig = /sin|cos|tan/.test(s);
    for (const m of s.matchAll(/\s*(?:(\d+(?:\.\d+)?)|([A-Za-z]+)|(\*\*|[-+*/^()]))|\s*(\S)/g)) {
      if (m[4] === "°") {
        tokeni.push({ t: "deg", v: trig ? "* (pi / 180)" : "" });
        continue;
      }
      if (m[4]) throw new Error(`ne razumijem znak „${m[4]}“`);
      if (m[1]) tokeni.push({ t: "num", v: m[1] });
      else if (m[2]) {
        const dijelovi2 = poznato.includes(m[2]) ? [m[2]] : rastavi(m[2], poznato);
        if (!dijelovi2) throw new Error(`ne razumijem „${m[2]}“`);
        for (const d of dijelovi2) tokeni.push({ t: FUNKCIJE.includes(d) ? "fn" : "id", v: d });
      } else if (m[3]) tokeni.push({ t: "op", v: m[3] });
    }
    if (!tokeni.length) throw new Error("upiši izraz");
    const out = [];
    for (let i = 0; i < tokeni.length; i++) {
      const tk = tokeni[i];
      const prev = out.at(-1);
      const zavrsava = prev && (prev.t === "num" || prev.t === "id" || prev.t === "deg" || prev.v === ")");
      const pocinje = tk.t === "num" || tk.t === "id" || tk.t === "fn" || tk.v === "(";
      if (zavrsava && pocinje) out.push({ t: "op", v: "*" });
      out.push(tk);
      if (tk.t === "fn" && tokeni[i + 1]?.v !== "(") {
        const jednostavan = (t) => t && (t.t === "num" || t.t === "id");
        const komad = (j) => {
          if (jednostavan(tokeni[j])) return tokeni.slice(j, j + 1);
          if (tokeni[j]?.v !== "(") return null;
          for (let d = 0, k = j; k < tokeni.length; k++) {
            d += tokeni[k].v === "(" ? 1 : tokeni[k].v === ")" ? -1 : 0;
            if (d === 0) return tokeni.slice(j, k + 1);
          }
          return null;
        };
        let eksponent = null;
        if (tokeni[i + 1]?.v === "^" && komad(i + 2)) {
          eksponent = komad(i + 2);
          i += 1 + eksponent.length;
        }
        const arg = [];
        if (tokeni[i + 1]?.v === "(") {
          const z = komad(i + 1);
          if (z) arg.push(z);
          i += z ? z.length : 0;
        } else
          while (jednostavan(tokeni[i + 1]) && arg.length < 2) {
            const dio = [tokeni[++i]];
            const pot = tokeni[i + 1]?.v === "^" && komad(i + 2);
            if (pot) {
              dio.push(tokeni[i + 1], ...pot);
              i += 1 + pot.length;
            }
            if (tokeni[i + 1]?.t === "deg") dio.push(tokeni[++i]);
            arg.push(dio);
          }
        if (!arg.length) throw new Error(`iza „${tk.v}“ treba broj ili zagrada`);
        if (eksponent) out.splice(out.length - 1, 0, { t: "op", v: "(" });
        out.push({ t: "op", v: "(" });
        arg.forEach((a, k) => {
          if (k) out.push({ t: "op", v: "*" });
          out.push(...a);
        });
        out.push({ t: "op", v: ")" });
        if (eksponent) out.push({ t: "op", v: ")" }, { t: "op", v: "^" }, ...eksponent);
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
    degree: " ° ",
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
        const pocetak = i;
        const n = naredba();
        if (n === "frac" || n === "dfrac" || n === "tfrac") {
          const [gore, dolje] = [grupa(), grupa()];
          const cijeli = /(?:^|[^\w.^_}\\])(\d+)\s*$/.exec(s.slice(0, pocetak));
          if (cijeli && /^\s*\d+\s*$/.test(gore) && /^\s*\d+\s*$/.test(dolje)) {
            out = out.replace(/\d+\s*$/, "") + `((${cijeli[1]})+(${gore})/(${dolje}))`;
          } else out += `((${izLatexa(gore)})/(${izLatexa(dolje)}))`;
        } else if (n === "sqrt") {
          let korijen = null;
          if (s[i] === "[") {
            const kraj3 = s.indexOf("]", i);
            korijen = s.slice(i + 1, kraj3);
            i = kraj3 + 1;
          }
          const pod = izLatexa(grupa());
          out += korijen ? `((${pod})^(1/(${izLatexa(korijen)})))` : ` sqrt(${pod})`;
        } else if (["left", "right", "mleft", "mright", "bigl", "bigr", "Bigl", "Bigr"].includes(n)) out += ograda();
        else if (["operatorname", "mathrm", "text", "mathit", "textrm"].includes(n)) out += ` ${grupa()} `;
        else if (n === "pm" || n === "mp") throw new Error("umjesto ± upiši jedan predznak (+ ili −)");
        else if (n === "placeholder") throw new Error("dovrši izraz (ostalo je prazno mjesto)");
        else if (n === "binom" || n === "dbinom" || n === "tbinom") {
          const [gore, dolje] = [izLatexa(grupa()), izLatexa(grupa())];
          out += ` ((fact(${gore}))/((fact(${dolje}))*(fact((${gore})-(${dolje})))))`;
        } else if (n === "log" && s[i] === "_") {
          i++;
          out += ` ((1)/(log(${izLatexa(grupa())})))*log `;
        } else if (n in NAREDBE) out += NAREDBE[n];
        else throw new Error(`ne razumijem „\\${n}“`);
      } else if (c === "^") {
        i++;
        const g = grupa();
        out += g.trim() === "\\circ" ? " ° " : `^(${izLatexa(g)})`;
      } else if (c === "!") {
        i++;
        const t = out.trimEnd();
        let od = t.length;
        if (t.endsWith(")")) {
          let dub = 0;
          for (od = t.length - 1; od >= 0; od--) {
            if (t[od] === ")") dub++;
            else if (t[od] === "(" && --dub === 0) break;
          }
        } else od = t.search(/[A-Za-z0-9.]+$/);
        if (od < 0 || od >= t.length) throw new Error("ispred ! treba broj ili zagrada");
        out = `${t.slice(0, od)} fact(${t.slice(od)})`;
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
  function ucitajLatex(latex, varijable2 = []) {
    try {
      return ucitaj(izLatexa(latex), varijable2);
    } catch (e) {
      return { greska: e.message };
    }
  }
  function ucitaj(unos, varijable2 = []) {
    try {
      const izraz2 = pripremi(unos, varijable2);
      if (/\binf\b/.test(izraz2)) throw new Error("beskonačnost ovdje nije broj");
      const c = compile2(izraz2);
      const strane = c.vars.filter((v) => !varijable2.includes(v));
      if (strane.length) throw new Error(`ne razumijem „${strane[0]}“`);
      return { izraz: izraz2, fn: c.fn, vars: c.vars, tex: tex(c.tree) };
    } catch (e) {
      return { greska: e.message };
    }
  }
  var UZORCI = [0.731, -1.37, 2.19, 3.71, -0.43, 1.618, -2.9, 0.217];
  function jednako(a, b, varijable2 = [], rel = 1e-9) {
    const isto = (x, y) => Math.abs(x - y) <= rel * Math.max(1, Math.abs(x), Math.abs(y));
    if (!varijable2.length) {
      const [x, y] = [a.fn({}), b.fn({})];
      return Number.isFinite(x) && Number.isFinite(y) && isto(x, y);
    }
    let valjanih = 0;
    for (let k = 0; k < UZORCI.length; k++) {
      const v = Object.fromEntries(varijable2.map((n, j) => [n, UZORCI[(k + 3 * j) % UZORCI.length] + j * 0.113]));
      const [x, y] = [a.fn(v), b.fn(v)];
      if (!Number.isFinite(x) || !Number.isFinite(y)) continue;
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
    if (n.fn === "fact") return razina(n.a) < 5 || pocinjeSlovom(n.a);
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
      if (n.fn === "fact") return `${razina(n.a) < 5 ? zagrade(tex(n.a)) : tex(n.a)}!`;
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
  function texIzraza(izraz2, varijable2 = []) {
    const r = ucitaj(String(izraz2), varijable2);
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
    const dijelovi2 = s.split(/∪|\s+U\s+|\bU\b/).map((d) => d.trim());
    if (dijelovi2.some((d) => !d)) throw new Error("prazan dio unije");
    const out = [];
    for (const d of dijelovi2) {
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
      const unutra2 = d.slice(1, -1);
      const krajevi2 = unutra2.includes(";") ? unutra2.split(";") : unutra2.split(",");
      if (krajevi2.length !== 2) throw new Error(`„${d}“: interval ima dva kraja odvojena zarezom`);
      const [a, b] = krajevi2.map(kraj);
      const ua = o === "[";
      const ub = z === "]";
      if (!(a.v < b.v)) throw new Error(`„${d}“: lijevi kraj mora biti manji od desnog`);
      if (ua && !Number.isFinite(a.v) || ub && !Number.isFinite(b.v)) throw new Error(`„${d}“: beskonačnost nikad nije uključena, uz nju ide ⟨ ili ⟩`);
      out.push({ a: a.v, b: b.v, ua, ub, ta: a.tex, tb: b.tex });
    }
    return sredi(out);
  }
  function sredi(dijelovi2) {
    const d = dijelovi2.map((x) => ({ ...x })).sort((x, y) => x.a - y.a || (x.ua === y.ua ? 0 : x.ua ? -1 : 1));
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
    const kraj3 = (odvajaci) => {
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
        const a = kraj3(od);
        if (![",", ";"].includes(s[i])) throw new Error("odvoji krajeve intervala zarezom ili točkom-zarezom");
        i++;
        const b = kraj3(od);
        const z = s[i++];
        if (!z || !">])".includes(z)) throw new Error("zatvori interval s ⟩ ili ]");
        out += `${c}${a};${b}${z}`;
      } else if (c === "⦃") {
        i++;
        const od = odvajac();
        const tocke = [kraj3(od)];
        while ([",", ";"].includes(s[i])) {
          i++;
          tocke.push(kraj3(od));
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
  function istaObitelj(u, tocan, vars) {
    if (vars.length !== 1 || vars[0] !== "k" || u.greska || tocan.greska) return false;
    const pravac = (f2) => {
      const [a, d] = [f2.fn({ k: 0 }), f2.fn({ k: 1 }) - f2.fn({ k: 0 })];
      const linearan = [-2.5, 0.37, 3].every((k) => Math.abs(f2.fn({ k }) - (a + d * k)) <= 1e-9 * Math.max(1, Math.abs(a), Math.abs(d * k)));
      return linearan && Number.isFinite(a) && Number.isFinite(d) && d !== 0 ? { a, d } : null;
    };
    const [x, y] = [pravac(u), pravac(tocan)];
    if (!x || !y || Math.abs(Math.abs(x.d) - Math.abs(y.d)) > 1e-9 * Math.abs(y.d)) return false;
    const pomak = (x.a - y.a) / y.d;
    return Math.abs(pomak - Math.round(pomak)) < 1e-9;
  }
  function provjeriPolje(s, p, unos) {
    const vars = varijableIzraza(s);
    if (!String(unos ?? "").trim()) return { tocno: false, poruka: "Upiši odgovor.", prazno: true };
    const u = ucitajLatex(String(p.iza ?? "").includes("%") ? String(unos).replace(/\\%/g, "") : unos, vars);
    if (u.greska) return { tocno: false, poruka: `Ne mogu pročitati izraz: ${u.greska}.`, prazno: true };
    const tocan = poljeOdgovor(s, p);
    if (jednako(u, tocan, vars) || istaObitelj(u, tocan, vars)) return { tocno: true };
    for (const k of p.krivo ?? []) {
      const kr = ucitaj(String(k.odgovor), vars);
      const rel = /\d\.\d/.test(String(k.odgovor)) && !vars.length ? 2e-3 : 1e-9;
      if (!kr.greska && jednako(u, kr, vars, rel) && !jednako(u, tocan, vars, rel)) return { tocno: false, poruka: k.poruka };
    }
    if (!vars.length && !s.priblizno && jednako(u, tocan, vars, PRIBLIZNO)) {
      const t = tocan.fn({});
      const decimalan = Math.abs(t * 1e4 - Math.round(t * 1e4)) < 1e-6;
      return { tocno: false, poruka: decimalan ? "To je zaokružena vrijednost. Upiši točan rezultat, bez zaokruživanja." : "To je približna vrijednost. Upiši točnu (npr. s π, korijenom ili razlomkom)." };
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
        const krivi2 = [];
        for (const [j, k] of (p.krivo ?? []).entries()) {
          const kr = ucitaj(String(k?.odgovor ?? ""), vars);
          if (!need(!kr.greska, `polja[${i}].krivo[${j}].odgovor nije ispravan izraz`)) continue;
          need(isText(k.poruka), `polja[${i}].krivo[${j}] treba „poruka“`);
          need(!jednako(kr, t, vars), `polja[${i}].krivo[${j}] jednak je točnom odgovoru`);
          const isti2 = krivi2.findIndex((x) => jednako(kr, x, vars));
          need(isti2 < 0, `polja[${i}].krivo[${j}] ima istu vrijednost kao krivo[${isti2}], pa se njegova poruka nikad ne pokaže; spoji ih u jednu poruku`);
          krivi2.push(kr);
        }
      });
      const uzorak = Object.fromEntries(vars.map((n, i) => [n, 0.731 + 0.639 * i]));
      if (s.bilo_kojim_redom && tocni.length > 1) need(new Set(tocni.map((t) => t.fn(uzorak))).size === tocni.length, "uz bilo_kojim_redom odgovori moraju biti različiti");
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
          return { ...r, polje: i, poruka: `${polja.length > 1 ? `U ${i + 1}. polju: ` : ""}${r.poruka}` };
        }
        return { tocno: true };
      }
      for (let i = 0; i < polja.length; i++) {
        const r = provjeriPolje(s, polja[i], vals[i]);
        if (!r.tocno) return { ...r, polje: i, poruka: polja.length > 1 ? `U ${i + 1}. polju: ${r.poruka}` : r.poruka };
      }
      return { tocno: true };
    },
    tocanOdgovor: (s) => s.polja.map((p) => `${p.oznaka ? `${p.oznaka} ` : ""}$${texIzraza(p.odgovor, varijableIzraza(s))}$`).join(", "),
    // ključ za usporedbu blizanca s pitanjem: iste vrijednosti = isti odgovor
    kljucOdgovora: (s) => s.polja.map((p) => {
      const t = poljeOdgovor(s, p);
      const v = Object.fromEntries(varijableIzraza(s).map((n, i) => [n, 0.731 + 0.639 * i]));
      return t.greska ? "?" : String(Number(t.fn(v).toPrecision(9)));
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
    if (jednakiSkupovi(njegov, skup) || (s.jos_tocno ?? []).some((t) => jednakiSkupovi(njegov, parseSkup(t)))) return { tocno: true };
    for (const k of s.krivo ?? []) if (jednakiSkupovi(njegov, parseSkup(k.odgovor))) return { tocno: false, poruka: k.poruka };
    const blizu2 = (x, y) => x === y || Math.abs(x - y) < 1e-9;
    if (njegov.length === skup.length && njegov.every((d, i) => blizu2(d.a, skup[i].a) && blizu2(d.b, skup[i].b))) {
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
  odgovor i krivo kao gore, tocke nisu potrebne; jos_tocno?: [skup] za drugi jednako ispravan odgovor (npr. rast na [-1, 1] i ⟨-1, 1⟩); točan je svaki zapis istog skupa (npr. ℝ∖{-4} i ⟨-∞, -4⟩ ∪ ⟨-4, +∞⟩);
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
        const krajevi2 = skup.flatMap((d) => [d.a, d.b]).filter(Number.isFinite);
        const nema = krajevi2.filter((k) => !t.some((x) => Math.abs(x.v - k) < 1e-9));
        need(!nema.length, `kraj ${nema[0]} nije među tockama, pa ga učenik ne može označiti`);
      }
      if (s.nacin === "prikaz") need(!("krivo" in s), 'interval "prikaz" je za gledanje, pa nema „krivo“');
      for (const [j, t2] of (s.jos_tocno ?? []).entries()) {
        try {
          parseSkup(t2);
        } catch (e) {
          need(false, `jos_tocno[${j}]: ${e.message}`);
        }
      }
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
      if (jednakiSkupovi(njegov, skup) || (s.jos_tocno ?? []).some((t) => jednakiSkupovi(njegov, parseSkup(t)))) return { tocno: true };
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
    const v = t.map((x) => typeof x === "number" ? Number.isFinite(x) ? x : null : vrijednost(String(x)));
    if (v.some((x) => x == null)) return null;
    return t.length === 2 ? v : [v[0] + KOSO * v[1], v[2] + KOSO * v[1]];
  }
  var tockaIzPolja = (t) => tockaCrteza(t?.z != null ? [t.x, t.y, t.z] : [t?.x, t?.y]);
  var CRTEZ = { W: 400, rub: { L: 34, R: 14, T: 14, B: 26 }, rubBezOsi: { L: 14, R: 14, T: 14, B: 14 }, minH: 180, maxH: 380, maxHBezOsi: 460 };
  function sirinaBrojevaY(s) {
    const k = s.korak_y ?? 1;
    let n = 1;
    for (let i = Math.ceil(s.y[0] / k - 1e-9); i * k <= s.y[1] + 1e-9 && i - Math.ceil(s.y[0] / k) < 60; i++) n = Math.max(n, String(Number((i * k).toPrecision(10))).length);
    return n;
  }
  function okvirCrteza(s) {
    const osnovni = s.osi === false ? CRTEZ.rubBezOsi : CRTEZ.rub;
    const rub = s.osi === false ? osnovni : { ...osnovni, L: Math.max(osnovni.L, 12 + 6.6 * sirinaBrojevaY(s)) };
    const sirina = CRTEZ.W - rub.L - rub.R;
    const zeljena = sirina / (s.x[1] - s.x[0]) * (s.y[1] - s.y[0]) + rub.T + rub.B;
    const H2 = Math.round(Math.min(s.osi === false ? CRTEZ.maxHBezOsi : CRTEZ.maxH, Math.max(CRTEZ.minH, zeljena)));
    return { ...rub, W: CRTEZ.W, H: H2, jednako: Math.abs(H2 - zeljena) < 2 };
  }
  var ima = (s, polja = ["tijela", "likovi", "duzine", "kruznice", "kutovi"]) => polja.some((k) => (s[k] ?? []).length);
  var imaIstoMjerilo = (s) => ima(s);
  var imaSadrzaj = (s) => ima(s, ["tijela", "likovi", "duzine", "kruznice", "kutovi", "vektori", "tekstovi", "tocke", "crte", "strelice"]);
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
kruznice?: [{sredina: [x, y] ili [x, y, z], r, isprekidano?, ispuna?}] (sa [x, y, z] vodoravna kružnica u prostoru, npr. baza valjka:
elipsa sa stražnjom polovicom isprekidano; ispuna: baza vidljiva odozgo, cijela puna), kutovi?: [{vrh, od, do, oznaka?, pravi?}] (manji kut između krakova vrh→od i
vrh→do), tijela?: [{vrsta: "valjak" | "stozac", sredina: [x, y, z] (središte donje baze), r, h}] (obris, baze i skriveni dio
isprekidano, točno izračunati za kosu projekciju; visinu, polumjer i oznake dodaj kao duzine/tocke), vektori?: [{od, do, oznaka?}], tekstovi?: [{x, y, z?, tekst}], os_x?, os_y? (imena osi, npr. "Re" i "Im"). Oznake na crtežu su kratak tekst (smije $\\alpha$, $\\sqrt{3}$,
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
      const osi2 = need(Array.isArray(s.x) && Array.isArray(s.y) && [...s.x, ...s.y].every(isNum) && s.x[1] > s.x[0] && s.y[1] > s.y[0], "x i y su [od, do] uz od < do");
      for (const k of ["korak_x", "korak_y"]) if (s[k] != null) need(isNum(s[k]) && s[k] > 0, `${k} mora biti pozitivan broj`);
      if (osi2) {
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
      const geometrija = imaIstoMjerilo(s);
      const najmanje = imaSadrzaj(s) && s.nacin === "prikaz" ? 0 : 1;
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
      const tocka3 = (t, gdje) => need(tockaCrteza(t) != null, `${gdje} mora biti točka [x, y] ili [x, y, z]`);
      for (const [i, l] of (s.likovi ?? []).entries()) {
        if (need(Array.isArray(l?.tocke) && l.tocke.length >= 3, `likovi[${i}] treba barem 3 točke`)) l.tocke.forEach((t, j) => tocka3(t, `likovi[${i}].tocke[${j}]`));
      }
      for (const [i, d] of (s.duzine ?? []).entries()) {
        tocka3(d?.od, `duzine[${i}].od`);
        tocka3(d?.do, `duzine[${i}].do`);
        if (d?.strana != null) need(d.strana === 1 || d.strana === -1, `duzine[${i}].strana je 1 ili -1`);
      }
      for (const [i, k] of (s.kruznice ?? []).entries()) {
        tocka3(k?.sredina, `kruznice[${i}].sredina`);
        need(vrijednost(String(k?.r)) > 0, `kruznice[${i}].r mora biti pozitivan broj`);
      }
      for (const [i, t] of (s.tijela ?? []).entries()) {
        need(["valjak", "stozac"].includes(t?.vrsta), `tijela[${i}].vrsta je "valjak" ili "stozac"`);
        need(Array.isArray(t?.sredina) && t.sredina.length === 3 && tockaCrteza(t.sredina) != null, `tijela[${i}].sredina je [x, y, z]`);
        need(vrijednost(String(t?.r)) > 0 && vrijednost(String(t?.h)) > 0, `tijela[${i}] treba pozitivne r i h`);
      }
      for (const [i, k] of (s.kutovi ?? []).entries()) for (const z of ["vrh", "od", "do"]) tocka3(k?.[z], `kutovi[${i}].${z}`);
      for (const [i, v] of (s.vektori ?? []).entries()) for (const z of ["od", "do"]) tocka3(v?.[z], `vektori[${i}].${z}`);
      for (const [i, t] of (s.tekstovi ?? []).entries()) need(tockaIzPolja(t) != null && isText(t?.tekst), `tekstovi[${i}] treba x, y i tekst`);
      if (osi2 && (geometrija || s.osi === false)) {
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
        if (need(s.cilj && typeof s.cilj === "object" && ids.every((id) => id in s.cilj), "„poklopi“ treba cilj s vrijednošću svakog klizača") && osi2 && !list.length) {
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

  // src/lib/lesson/katalog/fizika/_kinematika.js
  var ID3 = /^[A-Za-z_][A-Za-z_0-9]*$/;
  function lijepKorak(raspon2, koliko = 6) {
    if (!(raspon2 > 0)) return 1;
    const sirovo = raspon2 / koliko;
    const p = Math.pow(10, Math.floor(Math.log10(sirovo)));
    for (const k of [1, 2, 2.5, 5, 10]) if (k * p >= sirovo * 0.999) return k * p;
    return 10 * p;
  }
  function lijepRaspon(min, max, koliko = 6) {
    if (!Number.isFinite(min) || !Number.isFinite(max)) return [0, 1];
    if (max - min < 1e-9) {
      const d = Math.max(1, Math.abs(max) * 0.5);
      [min, max] = [min - (min < 0 ? d : 0), max + d];
    }
    const k = lijepKorak(max - min, koliko);
    return [
      Number((Math.floor(min / k + 1e-9) * k).toPrecision(12)),
      Number((Math.ceil(max / k - 1e-9) * k).toPrecision(12))
    ];
  }
  function dijeloviOsi(os, zamjena = "") {
    const t = String(os ?? "").trim();
    const m = t.match(/^(.*?)\s*\(([^()]*)\)\s*$/);
    return m ? { ime: m[1].trim() || zamjena, jedinica: m[2].trim() } : { ime: t || zamjena, jedinica: "" };
  }
  var PREVEDENO = /* @__PURE__ */ new Map();
  function prevedi(izraz2) {
    if (!PREVEDENO.has(izraz2)) {
      try {
        PREVEDENO.set(izraz2, compile2(izraz2));
      } catch (e) {
        PREVEDENO.set(izraz2, { greska: e.message });
      }
    }
    return PREVEDENO.get(izraz2);
  }
  function izracunaj(v, parametri = {}, zadano = NaN) {
    if (v == null) return zadano;
    if (isNum(v)) return v;
    if (typeof v !== "string") return NaN;
    const c = prevedi(v);
    if (c.greska) return NaN;
    try {
      const r = c.fn(parametri);
      return Number.isFinite(r) ? r : NaN;
    } catch {
      return NaN;
    }
  }
  function provjeriVrijednost(v, ids, need, gdje) {
    if (isNum(v)) return true;
    if (typeof v !== "string" || !v.trim())
      return need(false, `${gdje} mora biti broj ili izraz s klizačima`);
    const c = prevedi(v);
    if (!need(!c.greska, `${gdje}: ${c.greska}`)) return false;
    const nepoznate = c.vars.filter((x) => !ids.includes(x));
    return need(
      !nepoznate.length,
      `${gdje} koristi nepoznate varijable: ${nepoznate.join(", ")} (smiju samo id-jevi klizača)`
    );
  }
  var varijable = (v) => typeof v === "string" && !prevedi(v).greska ? prevedi(v).vars : [];
  function provjeriKlizace(klizaci, need, najvise = 3) {
    const ids = [];
    if (klizaci == null) return ids;
    if (!need(
      Array.isArray(klizaci) && klizaci.length <= najvise,
      `klizaci mora biti popis od najviše ${najvise} klizača`
    ))
      return ids;
    klizaci.forEach((k, i) => {
      need(
        ID3.test(k?.id ?? ""),
        `klizaci[${i}].id mora biti ime varijable (slova, bez razmaka)`
      );
      need(
        typeof k?.oznaka === "string" && k.oznaka.trim().length > 0,
        `klizaci[${i}] treba „oznaka“`
      );
      ids.push(k?.id);
      if (need(
        [k?.min, k?.max, k?.korak, k?.pocetak].every(isNum),
        `klizaci[${i}] treba brojeve min, max, korak, pocetak`
      )) {
        need(
          k.max > k.min && k.korak > 0,
          `klizaci[${i}]: mora biti min < max i korak > 0`
        );
        need(
          k.pocetak >= k.min && k.pocetak <= k.max,
          `klizaci[${i}].pocetak mora biti između min i max`
        );
        need(
          (k.max - k.min) / k.korak <= 1e3,
          `klizaci[${i}] ima previše koraka (najviše 1000)`
        );
      }
    });
    need(
      new Set(ids).size === ids.length,
      "id-jevi klizača moraju biti različiti"
    );
    return ids;
  }
  function vrijednostiKlizaca2(k) {
    const n = Math.round((k.max - k.min) / k.korak);
    return Array.from(
      { length: n + 1 },
      (_, i) => Number((k.min + i * k.korak).toPrecision(12))
    );
  }
  var pocetniParametri2 = (s) => Object.fromEntries((s.klizaci ?? []).map((k) => [k.id, k.pocetak]));
  function korijeni(f2, a, b, n = 1500) {
    const out = [];
    const h = (b - a) / n;
    const xs = Array.from({ length: n + 1 }, (_, i) => a + i * h);
    const ys = xs.map((x) => f2(x));
    const skala2 = Math.max(1e-9, ...ys.filter(Number.isFinite).map(Math.abs));
    const eps = skala2 * 1e-9;
    const nula = (y) => Math.abs(y) <= eps;
    const dodaj = (x) => {
      if (!out.some((r) => Math.abs(r - x) < Math.max(1e-6, h * 0.5)))
        out.push(x);
    };
    if (nula(ys[0])) dodaj(a);
    for (let i = 1; i <= n; i++) {
      const [y0, y1] = [ys[i - 1], ys[i]];
      if (!Number.isFinite(y0) || !Number.isFinite(y1)) continue;
      if (nula(y1) && !nula(y0)) {
        let [l, r] = [xs[i - 1], xs[i]];
        for (let k = 0; k < 60; k++) {
          const m = (l + r) / 2;
          if (nula(f2(m))) r = m;
          else if (Math.sign(f2(m)) !== Math.sign(y0)) r = m;
          else l = m;
        }
        dodaj(r);
      } else if (!nula(y0) && !nula(y1) && Math.sign(y0) !== Math.sign(y1)) {
        let [l, r] = [xs[i - 1], xs[i]];
        for (let k = 0; k < 70; k++) {
          const m = (l + r) / 2;
          if (Math.sign(f2(m)) === Math.sign(f2(l))) l = m;
          else r = m;
        }
        dodaj((l + r) / 2);
      } else if (i < n && !nula(y1) && Math.abs(y1) <= Math.abs(y0) && Math.abs(y1) <= Math.abs(ys[i + 1]) && Math.sign(y0) === Math.sign(y1) && Math.sign(ys[i + 1]) === Math.sign(y1)) {
        let [l, r] = [xs[i - 1], xs[i + 1]];
        for (let k = 0; k < 80; k++) {
          const m1 = l + (r - l) * 0.382;
          const m2 = l + (r - l) * 0.618;
          if (Math.abs(f2(m1)) < Math.abs(f2(m2))) r = m2;
          else l = m1;
        }
        const m = (l + r) / 2;
        if (Math.abs(f2(m)) <= skala2 * 1e-7) dodaj(m);
      }
    }
    return out.sort((x, y) => x - y);
  }

  // src/lib/lesson/katalog/fizika/gibanje.js
  var NACINI = ["istrazi", "zaustavi", "postavi"];
  var PROSTORI = ["staza", "vertikalno", "ravnina"];
  var IKONE = ["tocka", "auto", "lopta", "paket"];
  var G = 9.81;
  var T_MAX = 300;
  var ZAUSTAVLJANJE = "zaustavljanje";
  var ravnina = (s) => s.prostor === "ravnina";
  var gOd = (s) => isNum(s.g) ? s.g : G;
  function velicine(s) {
    if (ravnina(s))
      return {
        x: { oznaka: "x", jedinica: "m" },
        y: { oznaka: "y", jedinica: "m" },
        vx: { oznaka: "vₓ", jedinica: "m/s" },
        vy: { oznaka: "v_y", jedinica: "m/s" },
        v: { oznaka: "v", jedinica: "m/s" },
        a: { oznaka: "a", jedinica: "m/s²" }
      };
    return {
      s: { oznaka: s.prostor === "vertikalno" ? "h" : "s", jedinica: "m" },
      v: { oznaka: "v", jedinica: "m/s" },
      a: { oznaka: "a", jedinica: "m/s²" }
    };
  }
  var VRSTE_GRAFA = {
    staza: ["s-t", "v-t", "a-t"],
    vertikalno: ["s-t", "v-t", "a-t"],
    ravnina: ["x-t", "y-t", "vx-t", "vy-t"]
  };
  var BROJACI = {
    staza: ["t", "s", "v", "v_kmh", "a"],
    vertikalno: ["t", "s", "v", "a"],
    ravnina: ["t", "x", "y", "vx", "vy", "v"]
  };
  var CILJ_ZAUSTAVI = {
    staza: ["s", "v", "susret"],
    vertikalno: ["s", "v", "susret"],
    ravnina: ["x", "y", "vx", "vy", "v"]
  };
  var CILJ_POSTAVI = {
    staza: ["kraj", "s", "v", "susret"],
    vertikalno: ["h_max", "vrijeme_leta", "s", "v"],
    ravnina: ["domet", "h_max", "vrijeme_leta", "x", "y", "v"]
  };
  function tijelo1D(t, s, p, g) {
    const vert = s.prostor === "vertikalno";
    const aZad = vert ? -g : 0;
    let [tt, x, v] = [0, izracunaj(t.s0, p, 0), izracunaj(t.v0, p, 0)];
    const segs = [];
    let krajDionica = null;
    let greska = null;
    for (const d of t.dionice ?? []) {
      const a = izracunaj(d.a, p, aZad);
      let t1;
      if (d.do === ZAUSTAVLJANJE)
        t1 = Math.abs(v) < 1e-12 ? tt : a * v < 0 ? tt + Math.abs(v / a) : Infinity;
      else t1 = izracunaj(d.do, p, NaN);
      if (!Number.isFinite(t1) || t1 < tt) {
        greska = d.do === ZAUSTAVLJANJE ? 'dionica "zaustavljanje" nikad ne završi (akceleracija mora biti suprotna brzini)' : "kraj dionice nije iza kraja prethodne";
        segs.push({ t0: tt, t1: Infinity, s0: x, v0: v, a });
        break;
      }
      segs.push({ t0: tt, t1, s0: x, v0: v, a });
      const tau = t1 - tt;
      x += v * tau + a * tau * tau / 2;
      v = d.do === ZAUSTAVLJANJE ? 0 : v + a * tau;
      tt = t1;
      krajDionica = t1;
    }
    if (!segs.length || Number.isFinite(segs.at(-1).t1))
      segs.push({ t0: tt, t1: Infinity, s0: x, v0: v, a: aZad });
    const formula = (tm) => {
      const sg = segs.find((q) => tm < q.t1) ?? segs.at(-1);
      const tau = Math.max(0, tm - sg.t0);
      return {
        s: sg.s0 + sg.v0 * tau + sg.a * tau * tau / 2,
        v: sg.v0 + sg.a * tau,
        a: sg.a
      };
    };
    let kraj3 = null;
    if (vert && s.tlo !== false) {
      for (const q of segs) {
        const taus = [];
        if (Math.abs(q.a) < 1e-12) {
          if (q.v0 < 0) taus.push(-q.s0 / q.v0);
        } else {
          const D = q.v0 * q.v0 - 2 * q.a * q.s0;
          if (D >= 0)
            taus.push((-q.v0 + Math.sqrt(D)) / q.a, (-q.v0 - Math.sqrt(D)) / q.a);
        }
        const ok = taus.filter(
          (tau) => tau > 1e-9 && q.t0 + tau <= q.t1 + 1e-12 && q.v0 + q.a * tau <= 1e-9
        ).sort((a, b) => a - b);
        if (ok.length) {
          kraj3 = q.t0 + ok[0];
          break;
        }
      }
    }
    const stanje3 = (tm) => {
      if (kraj3 != null && tm > kraj3 + 1e-9)
        return { s: 0, v: 0, a: 0, miruje: true };
      return formula(tm);
    };
    const lomovi = segs.map((q) => q.t0).filter((x2) => x2 > 0);
    return {
      dim: 1,
      id: t.id,
      naziv: t.naziv,
      ikona: t.ikona ?? "tocka",
      segs,
      kraj: kraj3,
      krajDionica,
      stanje: stanje3,
      lomovi,
      greska,
      s0: izracunaj(t.s0, p, 0)
    };
  }
  function tijelo2D(t, p, g) {
    const [x0, y0] = [izracunaj(t.x0, p, 0), izracunaj(t.y0, p, 0)];
    let vx, vy;
    if (t.v0 != null || t.kut != null) {
      const [v0, k] = [
        izracunaj(t.v0, p, 0),
        izracunaj(t.kut, p, 0) * Math.PI / 180
      ];
      [vx, vy] = [v0 * Math.cos(k), v0 * Math.sin(k)];
      if (Math.abs(vx) < 1e-12) vx = 0;
    } else [vx, vy] = [izracunaj(t.vx, p, 0), izracunaj(t.vy, p, 0)];
    const kraj3 = g > 0 ? (vy + Math.sqrt(Math.max(0, vy * vy + 2 * g * y0))) / g : null;
    const formula = (tm) => {
      const vyt = vy - g * tm;
      return {
        x: x0 + vx * tm,
        y: y0 + vy * tm - g * tm * tm / 2,
        vx,
        vy: vyt,
        v: Math.hypot(vx, vyt),
        a: g
      };
    };
    const stanje3 = (tm) => {
      if (kraj3 != null && tm > kraj3 + 1e-9)
        return {
          x: x0 + vx * kraj3,
          y: 0,
          vx: 0,
          vy: 0,
          v: 0,
          a: 0,
          miruje: true
        };
      return formula(tm);
    };
    return {
      dim: 2,
      id: t.id,
      naziv: t.naziv,
      ikona: t.ikona ?? "lopta",
      kraj: kraj3,
      stanje: stanje3,
      lomovi: [],
      x0,
      y0,
      vx0: vx,
      vy0: vy
    };
  }
  function modelGibanja(s, parametri = pocetniParametri2(s)) {
    const g = gOd(s);
    const tijela = (s.tijela ?? []).map(
      (t) => ravnina(s) ? tijelo2D(t, parametri, g) : tijelo1D(t, s, parametri, g)
    );
    let tKraj = isNum(s.t_kraj) ? s.t_kraj : null;
    let prirodno = true;
    if (tKraj == null) {
      const krajevi2 = tijela.map((b) => b.kraj ?? b.krajDionica).filter((x) => Number.isFinite(x) && x > 0);
      if (krajevi2.length) tKraj = Math.max(...krajevi2);
      else [tKraj, prirodno] = [10, false];
    }
    return { tijela, g, tKraj: Math.min(T_MAX, Math.max(1e-3, tKraj)), prirodno };
  }
  function skupoviParametara(s) {
    const k = s.klizaci ?? [];
    const out = [pocetniParametri2(s)];
    for (let m = 0; m < 1 << k.length; m++)
      out.push(
        Object.fromEntries(k.map((q, i) => [q.id, m & 1 << i ? q.max : q.min]))
      );
    return out;
  }
  function mrezaParametara(s) {
    let out = [{}];
    for (const k of s.klizaci ?? []) {
      const n = Math.min(6, Math.round((k.max - k.min) / k.korak));
      const vr = [
        .../* @__PURE__ */ new Set([
          k.pocetak,
          ...Array.from(
            { length: n + 1 },
            (_, i) => k.min + (k.max - k.min) * i / n
          )
        ])
      ];
      out = out.flatMap((p) => vr.map((v) => ({ ...p, [k.id]: v })));
    }
    return out;
  }
  function rasponScene(s) {
    const vrijednosti2 = { s: [0], x: [0], y: [0] };
    for (const p of mrezaParametara(s)) {
      const m = modelGibanja(s, p);
      for (const b of m.tijela) {
        for (let i = 0; i <= 160; i++) {
          const st = b.stanje(m.tKraj * i / 160);
          for (const k of ["s", "x", "y"])
            if (Number.isFinite(st[k])) vrijednosti2[k].push(st[k]);
        }
      }
    }
    for (const c of s.crte ?? [])
      for (const k of ["s", "x", "y"])
        if (isNum(c?.[k])) vrijednosti2[k].push(c[k]);
    const r = (k) => {
      const [lo, hi] = [Math.min(...vrijednosti2[k]), Math.max(...vrijednosti2[k])];
      return lijepRaspon(lo, hi + (k === "x" ? 0.02 : 0.06) * (hi - lo));
    };
    if (ravnina(s))
      return { x: isPar(s.x) ? s.x : r("x"), y: isPar(s.y) ? s.y : r("y") };
    return { s: isPar(s.ljestvica) ? s.ljestvica : r("s") };
  }
  var isPar = (v) => Array.isArray(v) && v.length === 2 && v.every(isNum) && v[1] > v[0];
  function funkcijaCilja(m, c) {
    if (c.velicina === "susret")
      return (t) => m.tijela[0].stanje(t).s - m.tijela[1].stanje(t).s;
    const b = m.tijela.find((x) => x.id === c.tijelo) ?? m.tijela[0];
    return (t) => b.stanje(t)[c.velicina] - (c.vrijednost ?? 0);
  }
  function trenuciCilja(s, parametri = pocetniParametri2(s)) {
    const m = modelGibanja(s, parametri);
    if (!s.cilj) return [];
    if (s.cilj.velicina === "susret")
      return korijeni(funkcijaCilja(m, s.cilj), 0, m.tKraj).filter(
        (t) => t > 1e-6
      );
    const b = m.tijela.find((x) => x.id === s.cilj.tijelo) ?? m.tijela[0];
    return korijeni(
      funkcijaCilja(m, s.cilj),
      0,
      Math.min(m.tKraj, b?.kraj ?? Infinity)
    );
  }
  var tolerancijaTrenutka = (s, t) => isNum(s.cilj?.tolerancija) ? s.cilj.tolerancija : Math.max(0.1, 0.02 * Math.abs(t));
  function najvecaVisina(b, tKraj) {
    const kraj3 = Math.min(tKraj, b.kraj ?? Infinity);
    let max = b.stanje(0).s;
    for (const q of b.segs) {
      const ts = [q.t0, Math.min(q.t1, kraj3)];
      if (Math.abs(q.a) > 1e-12) ts.push(q.t0 - q.v0 / q.a);
      for (const tm of ts)
        if (tm >= q.t0 - 1e-12 && tm <= Math.min(q.t1, kraj3) + 1e-12)
          max = Math.max(max, b.stanje(tm).s);
    }
    return max;
  }
  function ishodGibanja(s, parametri) {
    const c = s.cilj ?? {};
    const m = modelGibanja(s, parametri);
    const b = m.tijela.find((x) => x.id === c.tijelo) ?? m.tijela[0];
    if (!b) return null;
    switch (c.velicina) {
      case "domet":
        return b.dim === 2 && Number.isFinite(b.kraj) ? b.vx0 * b.kraj : null;
      case "vrijeme_leta":
        return Number.isFinite(b.kraj) ? b.kraj : null;
      case "h_max":
        return b.dim === 2 ? b.y0 + Math.max(0, b.vy0) ** 2 / (2 * m.g) : najvecaVisina(b, m.tKraj);
      case "kraj":
        return b.stanje(m.tKraj).s;
      case "susret": {
        if (m.tijela.length < 2) return null;
        const t = korijeni(funkcijaCilja(m, c), 0, m.tKraj).find((x) => x > 1e-6);
        return t == null ? null : m.tijela[0].stanje(t).s;
      }
      default: {
        if (!isNum(c.t)) return null;
        const v = b.stanje(Math.min(c.t, m.tKraj))[c.velicina];
        return Number.isFinite(v) ? v : null;
      }
    }
  }
  var tolerancijaCilja = (s) => isNum(s.cilj?.tolerancija) ? s.cilj.tolerancija : Math.max(1e-6, Math.abs(s.cilj?.vrijednost ?? 0) * 0.02);
  function opisCilja(s) {
    const c = s.cilj ?? {};
    const vel = velicine(s);
    const jed2 = (k) => vel[k]?.jedinica ?? "m";
    switch (c.velicina) {
      case "domet":
        return { ime: "domet", jedinica: "m" };
      case "vrijeme_leta":
        return { ime: "vrijeme leta", jedinica: "s" };
      case "h_max":
        return { ime: "najveća visina", jedinica: "m" };
      case "kraj":
        return { ime: "završni položaj", jedinica: "m" };
      case "susret":
        return { ime: "mjesto susreta", jedinica: "m" };
      default:
        return {
          ime: `${vel[c.velicina]?.oznaka ?? c.velicina} u t = ${formatNumber(c.t ?? 0)} s`,
          jedinica: jed2(c.velicina)
        };
    }
  }
  function rjesenjePostavi(s) {
    const k = s.klizaci ?? [];
    if (!k.length || !s.cilj) return null;
    const opcije = k.map(vrijednostiKlizaca2);
    let best = null;
    const idx = k.map(() => 0);
    for (; ; ) {
      const p = Object.fromEntries(k.map((q, i2) => [q.id, opcije[i2][idx[i2]]]));
      const v = ishodGibanja(s, p);
      if (isNum(v)) {
        const d = Math.abs(v - s.cilj.vrijednost);
        if (!best || d < best.odstupanje - 1e-12)
          best = { parametri: p, vrijednost: v, odstupanje: d };
      }
      let i = 0;
      while (i < k.length && ++idx[i] >= opcije[i].length) idx[i++] = 0;
      if (i === k.length) break;
    }
    return best;
  }
  var brojKombinacija = (s) => (s.klizaci ?? []).reduce(
    (n, k) => n * (Math.round((k.max - k.min) / k.korak) + 1),
    1
  );
  var opisTijela = (t, i) => `${t.naziv}${t.id ? ` (${t.id})` : ""}` || `tijelo ${i + 1}`;
  var tKlizaci = (s) => (s.klizaci ?? []).map((k) => `${k.id} = ${withUnit(k.pocetak, k.jedinica)}`).join(", ");
  var gibanje = {
    tip: "gibanje",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Gibanje jednog ili dvaju tijela sa stalnom akceleracijom po dionicama: animacija s tragovima (položaj svakih Δt), strelicama v i a,
brojačima uživo i povezanim grafovima s kursorom vremena (površina ispod v–t boji se uživo). Polja: tekst, nacin, prostor ("staza" vodoravno |
"vertikalno" uvis i slobodni pad, h od tla | "ravnina" hitac: vodoravni, kosi, okomiti), g? (zadano 9,81; 10 kad ga zadatak koristi), tijela (1–2):
- staza / vertikalno: {id, naziv, ikona? (${IKONE.join(" | ")}), s0? (položaj ili visina, m), v0? (m/s, + udesno / uvis), dionice?: [{do (kraj dionice u s,
  ili "zaustavljanje": traje dok tijelo ne stane i dalje miruje), a (m/s²; okomito zadano −g)}] (1–6)}. Nakon zadnje dionice na stazi a = 0, okomito
  slobodni let. Okomito tijelo stane kad padne na tlo (h = 0), osim uz tlo: false.
- ravnina: {id, naziv, ikona?, x0?, y0? (visina izbačaja), vx? i vy? ILI v0? i kut? (° od vodoravne)}; ay = −g, tijelo stane na tlu (y = 0).
Brojčana polja tijela smiju biti izraz s id-jevima klizača („v“, „-a“). Ostalo: t_kraj? (s; zadano prirodni kraj: zadnja dionica ili pad na tlo),
ljestvica?: [od, do] (m; u ravnini x: [od, do], y: [od, do]; zadano automatski), crte?: [{s | x | y, oznaka}] (ciljna visina, crta, kanta),
tragovi? (Δt u s), strelice?: ["v", "a"], brojaci? (staza ${BROJACI.staza.join(", ")}; vertikalno ${BROJACI.vertikalno.join(", ")}; ravnina ${BROJACI.ravnina.join(", ")}),
graf?: {vrste: 1–3 od ${VRSTE_GRAFA.staza.join(", ")} (ravnina: ${VRSTE_GRAFA.ravnina.join(", ")}), povrsina?: true (površina ispod v–t do kursora)},
ubrzanje? (mjerilo vremena animacije; zadano tako da traje 3–8 s), klizaci?: [{id, oznaka, min, max, korak, pocetak, jedinica?}] (do 3). Načini:
- "istrazi": korak za gledanje: ▶/⏸, premotavanje, klizači; bez cilja.
- "zaustavi": učenik zaustavi ili premota vrijeme i zabilježi trenutke; cilj: {tijelo, velicina (staza/vertikalno: s | v | susret; ravnina: x | y |
  vx | vy | v), vrijednost, svi? (zadano true: treba zabilježiti sve trenutke), tolerancija? (s; zadano max(0,1 s, 2 %))}. "susret" (bez tijela i
  vrijednosti): trenutak u kojem su dva tijela na istom mjestu. Trenutke računa aplikacija; poruke „to je tek jedan trenutak“ i „u tom trenutku
  h = 18,4 m“ dodaju se same; krivo?: [{vrijednost (t u s), poruka}]. Bez klizača.
- "postavi": učenik namjesti klizač(e) (1–2) i pusti gibanje; cilj: {tijelo, velicina (staza: kraj (gdje tijelo stane / završni položaj) | s | v |
  susret (mjesto susreta); vertikalno: h_max | vrijeme_leta | s | v; ravnina: domet | h_max | vrijeme_leta | x | y | v), vrijednost, t? (za s, v, x, y),
  tolerancija? (zadano 2 %)}. Ostvarivost cilja i točne vrijednosti klizača računa aplikacija; krivo?: [{vrijednost (vrijednost prvog klizača), poruka}].`,
    primjer: {
      tip: "gibanje",
      nacin: "zaustavi",
      prostor: "vertikalno",
      g: 10,
      tekst: "Pusti lopticu pa zabilježi **svaki** trenutak u kojem je na visini 15 m.",
      tijela: [{ id: "l", naziv: "loptica", ikona: "lopta", s0: 0, v0: 20 }],
      ljestvica: [0, 25],
      crte: [{ s: 15, oznaka: "15 m" }],
      tragovi: 0.5,
      strelice: ["v", "a"],
      brojaci: ["t", "s", "v"],
      graf: { vrste: ["s-t"] },
      cilj: { tijelo: "l", velicina: "s", vrijednost: 15, svi: true }
    },
    primjeri: [
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "staza",
        tekst: "Pokreni auto i prati kursor na grafu. Pomiči vrijeme i gledaj kako površina ispod grafa raste.",
        tijela: [
          {
            id: "a",
            naziv: "automobil",
            ikona: "auto",
            s0: 0,
            v0: 0,
            dionice: [
              { do: 4, a: 5 },
              { do: 10, a: 0 },
              { do: 18, a: -2.5 }
            ]
          }
        ],
        ljestvica: [0, 240],
        tragovi: 1,
        strelice: ["v", "a"],
        brojaci: ["t", "s", "v", "a"],
        graf: { vrste: ["v-t"], povrsina: true }
      },
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "staza",
        tekst: "Automobil 10 s ubrzava, a zatim koči 6 s. Pokreni ga i gledaj tragove (svake sekunde) i strelice $\\vec v$ i $\\vec a$.",
        tijela: [
          {
            id: "a",
            naziv: "automobil",
            ikona: "auto",
            s0: 0,
            v0: 0,
            dionice: [
              { do: 10, a: 3 },
              { do: 16, a: -5 }
            ]
          }
        ],
        tragovi: 1,
        strelice: ["v", "a"],
        brojaci: ["t", "s", "v", "v_kmh", "a"],
        graf: { vrste: ["s-t", "v-t", "a-t"] }
      },
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "staza",
        tekst: "Utrka: sprinter trči 10 m/s, a gepard 30 m/s (108 km/h). Pokreni utrku i usporedi brojače.",
        tijela: [
          { id: "s", naziv: "sprinter", ikona: "tocka", s0: 0, v0: 10 },
          { id: "g", naziv: "gepard", ikona: "tocka", s0: 0, v0: 30 }
        ],
        t_kraj: 10,
        ljestvica: [0, 300],
        tragovi: 1,
        brojaci: ["t", "s", "v", "v_kmh"]
      },
      {
        tip: "gibanje",
        nacin: "zaustavi",
        prostor: "staza",
        tekst: "Zaustavi utrku točno u trenutku kad gepard prijeđe 100 m i zabilježi taj trenutak.",
        tijela: [
          { id: "s", naziv: "sprinter", ikona: "tocka", s0: 0, v0: 10 },
          { id: "g", naziv: "gepard", ikona: "tocka", s0: 0, v0: 30 }
        ],
        t_kraj: 6,
        ljestvica: [0, 180],
        crte: [{ s: 100, oznaka: "100 m" }],
        brojaci: ["t", "s"],
        cilj: { tijelo: "g", velicina: "s", vrijednost: 100 }
      },
      {
        tip: "gibanje",
        nacin: "zaustavi",
        prostor: "staza",
        tekst: "Dva automobila voze jedan prema drugome. Zabilježi trenutak u kojem se susretnu.",
        tijela: [
          { id: "A", naziv: "auto A", ikona: "auto", s0: 0, v0: 20 },
          { id: "B", naziv: "auto B", ikona: "auto", s0: 300, v0: -10 }
        ],
        t_kraj: 14,
        ljestvica: [0, 300],
        brojaci: ["t", "s"],
        graf: { vrste: ["s-t"] },
        cilj: { velicina: "susret" }
      },
      {
        tip: "gibanje",
        nacin: "postavi",
        prostor: "staza",
        tekst: "Vozač vozi 20 m/s, reagira 1 s pa koči. Namjesti usporenje tako da auto stane **točno na crti** (60 m), pa pusti i provjeri.",
        tijela: [
          {
            id: "a",
            naziv: "automobil",
            ikona: "auto",
            s0: 0,
            v0: 20,
            dionice: [
              { do: 1, a: 0 },
              { do: "zaustavljanje", a: "-u" }
            ]
          }
        ],
        ljestvica: [0, 120],
        crte: [{ s: 60, oznaka: "crta" }],
        tragovi: 0.5,
        strelice: ["v", "a"],
        brojaci: ["t", "s", "v"],
        klizaci: [
          {
            id: "u",
            oznaka: "usporenje $|a|$",
            min: 2,
            max: 10,
            korak: 0.5,
            pocetak: 8,
            jedinica: "m/s²"
          }
        ],
        cilj: { tijelo: "a", velicina: "kraj", vrijednost: 60 }
      },
      {
        tip: "gibanje",
        nacin: "postavi",
        prostor: "ravnina",
        g: 10,
        tekst: "Kojom brzinom treba vodoravno izbaciti loptu s visine 5 m da padne u kantu? Namjesti brzinu i pusti loptu.",
        x: [0, 30],
        y: [0, 6],
        tijela: [{ id: "l", naziv: "lopta", x0: 0, y0: 5, vx: "v", vy: 0 }],
        crte: [{ x: 24, oznaka: "kanta" }],
        tragovi: 0.1,
        strelice: ["v"],
        brojaci: ["t", "x", "y"],
        klizaci: [
          {
            id: "v",
            oznaka: "brzina izbačaja",
            min: 5,
            max: 40,
            korak: 1,
            pocetak: 10,
            jedinica: "m/s"
          }
        ],
        cilj: {
          tijelo: "l",
          velicina: "domet",
          vrijednost: 24,
          tolerancija: 0.5
        }
      },
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "ravnina",
        g: 10,
        tekst: "Jedna lopta izbačena je vodoravno brzinom 24 m/s, a druga je u istom trenutku puštena s iste visine. Pokreni i usporedi tragove: koja prije padne?",
        tijela: [
          { id: "b", naziv: "izbačena", x0: 0, y0: 5, vx: 24, vy: 0 },
          { id: "p", naziv: "puštena", x0: 0, y0: 5, vx: 0, vy: 0 }
        ],
        tragovi: 0.1,
        strelice: ["v", "a"],
        brojaci: ["t", "x", "y", "vy"],
        graf: { vrste: ["y-t"] }
      },
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "ravnina",
        tekst: "Kosi hitac: mijenjaj brzinu i kut izbačaja i gledaj domet i najveću visinu.",
        tijela: [{ id: "l", naziv: "lopta", v0: "v", kut: "k" }],
        tragovi: 0.2,
        strelice: ["v", "a"],
        brojaci: ["t", "x", "y", "v"],
        klizaci: [
          {
            id: "v",
            oznaka: "brzina $v_0$",
            min: 5,
            max: 25,
            korak: 1,
            pocetak: 15,
            jedinica: "m/s"
          },
          {
            id: "k",
            oznaka: "kut $\\alpha$",
            min: 10,
            max: 80,
            korak: 5,
            pocetak: 45,
            jedinica: "°"
          }
        ]
      },
      {
        tip: "gibanje",
        nacin: "istrazi",
        prostor: "vertikalno",
        g: 10,
        tekst: "Kamen pada s visine 45 m. Pokreni ga i gledaj tragove svakih 0,5 s: razmaci rastu jer brzina raste.",
        tijela: [{ id: "k", naziv: "kamen", ikona: "lopta", s0: 45, v0: 0 }],
        tragovi: 0.5,
        strelice: ["v", "a"],
        brojaci: ["t", "s", "v"],
        graf: { vrste: ["s-t", "v-t"] }
      }
    ],
    // isti problem za više vrijednosti klizača ispisuje se jednom
    validate: (s) => [...new Set(validiraj(s))],
    check(s, odgovor3) {
      return s.nacin === "postavi" ? checkPostavi(s, odgovor3) : checkZaustavi(s, odgovor3);
    },
    tocanOdgovor(s) {
      if (s.nacin === "zaustavi") {
        const ts = trenuciCilja(s);
        return ts.length === 1 ? `t = ${withUnit(ts[0], "s")}` : `t = ${ts.map((t) => withUnit(t, "s")).join(" i t = ")}`;
      }
      if (s.nacin === "postavi") {
        const r = rjesenjePostavi(s);
        if (!r) return "";
        return s.klizaci.map(
          (k) => `${k.oznaka.replace(/\$/g, "")} = ${withUnit(r.parametri[k.id], k.jedinica)}`
        ).join(", ");
      }
      return "";
    },
    brojcaniOdgovor: (s) => {
      if (s.nacin === "postavi" && s.klizaci?.length === 1)
        return rjesenjePostavi(s)?.parametri[s.klizaci[0].id] ?? null;
      if (s.nacin === "zaustavi") {
        const ts = trenuciCilja(s);
        return ts.length === 1 ? ts[0] : null;
      }
      return null;
    },
    kljucOdgovora: (s) => s.nacin === "zaustavi" ? trenuciCilja(s).map((t) => t.toFixed(3)).join(",") : s.nacin === "postavi" ? JSON.stringify(rjesenjePostavi(s)?.parametri ?? null) : null
  };
  function validiraj(s) {
    const { list, need } = problems();
    need(
      NACINI.includes(s.nacin),
      `gibanje.nacin mora biti ${NACINI.join(", ")}`
    );
    if (!need(
      PROSTORI.includes(s.prostor),
      `gibanje.prostor mora biti ${PROSTORI.join(", ")}`
    ))
      return list;
    need(isText(s.tekst), "gibanje treba „tekst“");
    if (s.g != null)
      need(
        isNum(s.g) && s.g > 0 && s.g <= 30,
        "g mora biti broj između 0 i 30 (m/s²)"
      );
    const ids = provjeriKlizace(s.klizaci, need, 3);
    const rav = ravnina(s);
    if (!need(
      Array.isArray(s.tijela) && s.tijela.length >= 1 && s.tijela.length <= 2,
      "gibanje treba 1–2 tijela"
    ))
      return list;
    const koristeno = /* @__PURE__ */ new Set();
    const polje2 = (v, gdje) => {
      if (v == null) return true;
      varijable(v).forEach((x) => koristeno.add(x));
      return provjeriVrijednost(v, ids, need, gdje);
    };
    s.tijela.forEach((t, i) => {
      const at = `tijela[${i}]`;
      need(
        ID3.test(t?.id ?? ""),
        `${at}.id mora biti kratko ime (slova, bez razmaka)`
      );
      need(isText(t?.naziv), `${at} treba „naziv“ (npr. "automobil")`);
      if (t?.ikona != null)
        need(
          IKONE.includes(t.ikona),
          `${at}.ikona mora biti ${IKONE.join(", ")}`
        );
      const tudja = rav ? ["s0", "dionice"] : ["x0", "y0", "vx", "vy", "kut"];
      for (const f2 of tudja)
        need(
          !(f2 in (t ?? {})),
          `${at}.${f2} ne postoji u prostoru „${s.prostor}“ (${rav ? "u ravnini: x0, y0, vx, vy ili v0 i kut" : "u 1D: s0, v0, dionice"})`
        );
      if (rav) {
        for (const f2 of ["x0", "y0", "vx", "vy", "v0", "kut"])
          polje2(t[f2], `${at}.${f2}`);
        need(
          !((t.v0 != null || t.kut != null) && (t.vx != null || t.vy != null)),
          `${at}: zadaj ili vx i vy, ili v0 i kut (ne oboje)`
        );
        if (t.v0 != null || t.kut != null)
          need(
            t.v0 != null && t.kut != null,
            `${at}: uz v0 treba i kut (u °), i obrnuto`
          );
      } else {
        polje2(t.s0, `${at}.s0`);
        polje2(t.v0, `${at}.v0`);
        if (t.dionice != null && need(
          Array.isArray(t.dionice) && t.dionice.length >= 1 && t.dionice.length <= 6,
          `${at}.dionice mora biti popis od 1–6 dionica`
        )) {
          let prije = 0;
          t.dionice.forEach((d, j) => {
            const dat = `${at}.dionice[${j}]`;
            if (s.prostor === "staza")
              need(d?.a != null, `${dat} treba akceleraciju a`);
            polje2(d?.a, `${dat}.a`);
            if (d?.do === ZAUSTAVLJANJE) return;
            if (!need(
              d?.do != null,
              `${dat} treba „do“ (kraj dionice u s ili "zaustavljanje")`
            ))
              return;
            if (polje2(d.do, `${dat}.do`) && isNum(d.do)) {
              need(
                d.do > prije,
                `${dat}.do mora biti veći od kraja prethodne dionice (${formatNumber(prije)} s)`
              );
              prije = d.do;
            }
          });
        }
      }
    });
    need(
      new Set(s.tijela.map((t) => t?.id)).size === s.tijela.length,
      "id-jevi tijela moraju biti različiti"
    );
    if (list.length) return list;
    (s.klizaci ?? []).forEach(
      (k) => need(
        koristeno.has(k.id),
        `klizač „${k.id}“ se ne koristi ni u jednom tijelu`
      )
    );
    const skupovi = skupoviParametara(s);
    for (const p of skupovi) {
      const m = modelGibanja(s, p);
      const opis = s.klizaci?.length ? ` (uz ${Object.entries(p).map(([k, v]) => `${k} = ${formatNumber(v)}`).join(", ")})` : "";
      for (const [i, b] of m.tijela.entries()) {
        const st = b.stanje(0);
        if (!need(
          Object.values(st).every(
            (v) => typeof v === "boolean" || Number.isFinite(v)
          ),
          `${opisTijela(s.tijela[i], i)}: gibanje se ne može izračunati${opis}`
        ))
          return list;
        if (s.prostor === "vertikalno")
          need(
            st.s >= 0,
            `${opisTijela(s.tijela[i], i)}: početna visina s0 ne smije biti ispod tla${opis}`
          );
        if (rav) {
          need(
            b.y0 >= 0,
            `${opisTijela(s.tijela[i], i)}: y0 ne smije biti ispod tla${opis}`
          );
          need(
            b.kraj > 1e-6,
            `${opisTijela(s.tijela[i], i)}: tijelo je na tlu i ne leti (zadaj y0 > 0 ili vy > 0)${opis}`
          );
        }
        if (b.greska)
          need(false, `${opisTijela(s.tijela[i], i)}: ${b.greska}${opis}`);
      }
      if (!m.prirodno)
        need(
          isNum(s.t_kraj),
          "gibanje nema prirodni kraj (dionice ili pad na tlo), pa treba t_kraj"
        );
    }
    if (s.t_kraj != null)
      need(
        isNum(s.t_kraj) && s.t_kraj > 0 && s.t_kraj <= T_MAX,
        `t_kraj mora biti broj sekundi između 0 i ${T_MAX}`
      );
    if (list.length) return list;
    if (rav) {
      for (const k of ["x", "y"])
        if (s[k] != null) need(isPar(s[k]), `${k} mora biti [od, do] uz od < do`);
      need(
        s.ljestvica == null,
        "u ravnini raspon se zadaje s x: [od, do] i y: [od, do], ne s ljestvica"
      );
    } else {
      if (s.ljestvica != null)
        need(isPar(s.ljestvica), "ljestvica mora biti [od, do] uz od < do (m)");
      need(
        s.x == null && s.y == null,
        "x i y postoje samo u ravnini; ovdje je raspon „ljestvica“"
      );
    }
    if (list.length) return list;
    const r = rasponScene(s);
    const unutra2 = (k, v, rub = 0) => v >= r[k][0] - rub && v <= r[k][1] + rub;
    for (const p of [
      pocetniParametri2(s),
      ...s.nacin === "postavi" && rjesenjePostavi(s) ? [rjesenjePostavi(s).parametri] : []
    ]) {
      const m = modelGibanja(s, p);
      m.tijela.forEach((b, i) => {
        const sirina = rav ? null : (r.s[1] - r.s[0]) * 0.01;
        let van = false;
        for (let j = 0; j <= 100 && !van; j++) {
          const st = b.stanje(m.tKraj * j / 100);
          van = rav ? !unutra2("x", st.x, (r.x[1] - r.x[0]) * 0.01) || !unutra2("y", st.y, (r.y[1] - r.y[0]) * 0.01) : !unutra2("s", st.s, sirina);
        }
        need(
          !van,
          `${opisTijela(s.tijela[i], i)} izlazi izvan prikaza (${rav ? "x, y" : "ljestvica"}); povećaj raspon ili ga izostavi`
        );
      });
    }
    if (s.crte != null && need(
      Array.isArray(s.crte) && s.crte.length <= 4,
      "crte mora biti popis od najviše 4 crte"
    )) {
      const dopusteno = rav ? ["x", "y"] : ["s"];
      s.crte.forEach((c, i) => {
        const kljucevi = dopusteno.filter((k) => c?.[k] != null);
        if (!need(
          kljucevi.length === 1 && isNum(c[kljucevi[0]]),
          `crte[${i}] treba točno jedan broj ${dopusteno.join(" ili ")}`
        ))
          return;
        need(
          unutra2(kljucevi[0] === "s" ? "s" : kljucevi[0], c[kljucevi[0]]),
          `crte[${i}] je izvan prikaza`
        );
        if (c.oznaka != null)
          need(isText(c.oznaka), `crte[${i}].oznaka mora biti tekst`);
      });
    }
    const m0 = modelGibanja(s);
    if (s.tragovi != null)
      need(
        isNum(s.tragovi) && s.tragovi > 0 && m0.tKraj / s.tragovi <= 80,
        "tragovi je razmak u sekundama (> 0), najviše 80 tragova u cijelom gibanju"
      );
    if (s.strelice != null)
      need(
        Array.isArray(s.strelice) && s.strelice.every((x) => ["v", "a"].includes(x)),
        'strelice smije biti ["v"], ["a"] ili ["v", "a"]'
      );
    if (s.brojaci != null)
      need(
        Array.isArray(s.brojaci) && s.brojaci.length >= 1 && s.brojaci.every((x) => BROJACI[s.prostor].includes(x)),
        `brojaci su popis od: ${BROJACI[s.prostor].join(", ")}`
      );
    if (s.graf != null) {
      const v = s.graf?.vrste;
      if (need(
        Array.isArray(v) && v.length >= 1 && v.length <= 3 && v.every((x) => VRSTE_GRAFA[s.prostor].includes(x)) && new Set(v).size === v.length,
        `graf.vrste je 1–3 različitih od: ${VRSTE_GRAFA[s.prostor].join(", ")}`
      )) {
        if (s.graf.povrsina != null)
          need(
            s.graf.povrsina === false || v.some((x) => ["v-t", "vx-t", "vy-t"].includes(x)),
            "graf.povrsina ima smisla samo uz graf brzine (v-t, vx-t ili vy-t)"
          );
      }
    }
    if (s.ubrzanje != null)
      need(
        isNum(s.ubrzanje) && s.ubrzanje > 0 && s.ubrzanje <= 50,
        "ubrzanje je mjerilo vremena animacije (0–50)"
      );
    if (s.nacin === "istrazi") noAnswerFields(s, need);
    if (s.nacin === "zaustavi") validateZaustavi(s, need);
    if (s.nacin === "postavi") validatePostavi(s, need);
    return list;
  }
  function validateZaustavi(s, need) {
    need(
      !s.klizaci?.length,
      "„zaustavi“ nema klizače: trenutke aplikacija računa iz zadanih brojeva"
    );
    const c = s.cilj;
    if (!need(
      c && typeof c === "object",
      "„zaustavi“ treba cilj: {tijelo, velicina, vrijednost}"
    ))
      return;
    if (!need(
      CILJ_ZAUSTAVI[s.prostor].includes(c.velicina),
      `cilj.velicina mora biti ${CILJ_ZAUSTAVI[s.prostor].join(", ")}`
    ))
      return;
    if (c.velicina === "susret") {
      need(s.tijela.length === 2, "cilj „susret“ treba dva tijela");
      need(
        c.tijelo == null && c.vrijednost == null,
        "cilj „susret“ nema tijelo ni vrijednost (traži trenutak kad su tijela na istom mjestu)"
      );
    } else {
      need(
        s.tijela.some((t) => t.id === c.tijelo),
        "cilj.tijelo mora biti id jednog od tijela"
      );
      need(isNum(c.vrijednost), "cilj.vrijednost mora biti broj");
    }
    if (c.svi != null)
      need(typeof c.svi === "boolean", "cilj.svi je true ili false");
    if (c.tolerancija != null)
      need(
        isNum(c.tolerancija) && c.tolerancija > 0,
        "cilj.tolerancija je broj sekundi > 0"
      );
    const ts = trenuciCilja(s);
    if (!need(
      ts.length >= 1,
      "uvjet cilja nikad nije ispunjen u prikazanom vremenu; provjeri vrijednost ili t_kraj"
    ))
      return;
    need(ts.length <= 4, "uvjet je ispunjen u previše trenutaka (najviše 4)");
    for (let i = 1; i < ts.length; i++)
      need(
        ts[i] - ts[i - 1] > tolerancijaTrenutka(s, ts[i]) * 2,
        `trenuci ${formatNumber(ts[i - 1])} s i ${formatNumber(ts[i])} s preblizu su da bi se razlikovali`
      );
    if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
      s.krivo.forEach((k, i) => {
        need(
          isNum(k?.vrijednost) && isText(k?.poruka),
          `krivo[${i}] treba {vrijednost (t u s), poruka}`
        );
        if (isNum(k?.vrijednost))
          need(
            !ts.some(
              (t) => Math.abs(t - k.vrijednost) <= tolerancijaTrenutka(s, t)
            ),
            `krivo[${i}].vrijednost je jedan od točnih trenutaka`
          );
      });
    }
  }
  function validatePostavi(s, need) {
    if (!need(
      Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 2,
      "„postavi“ treba 1–2 klizača"
    ))
      return;
    const c = s.cilj;
    if (!need(
      c && typeof c === "object",
      "„postavi“ treba cilj: {tijelo, velicina, vrijednost}"
    ))
      return;
    if (!need(
      CILJ_POSTAVI[s.prostor].includes(c.velicina),
      `cilj.velicina u prostoru „${s.prostor}“ mora biti ${CILJ_POSTAVI[s.prostor].join(", ")}`
    ))
      return;
    if (c.velicina === "susret")
      need(s.tijela.length === 2, "cilj „susret“ treba dva tijela");
    else
      need(
        s.tijela.some((t) => t.id === c.tijelo),
        "cilj.tijelo mora biti id jednog od tijela"
      );
    need(isNum(c.vrijednost), "cilj.vrijednost mora biti broj");
    if (["s", "v", "x", "y"].includes(c.velicina))
      need(isNum(c.t) && c.t >= 0, `cilj „${c.velicina}“ treba trenutak t (s)`);
    if (c.tolerancija != null)
      need(
        isNum(c.tolerancija) && c.tolerancija > 0,
        "cilj.tolerancija mora biti broj > 0"
      );
    if (!need(
      brojKombinacija(s) <= 4e4,
      "klizači imaju previše kombinacija (najviše 40 000); povećaj korak"
    ))
      return;
    if (!isNum(c.vrijednost)) return;
    const r = rjesenjePostavi(s);
    const tol2 = tolerancijaCilja(s);
    const o = opisCilja(s);
    if (!need(
      r && r.odstupanje <= tol2,
      `cilj (${o.ime} = ${withUnit(c.vrijednost, o.jedinica)}) ne može se postići klizačima${r ? `; najbliže je ${withUnit(r.vrijednost, o.jedinica)}` : ""}`
    ))
      return;
    const p0 = ishodGibanja(s, pocetniParametri2(s));
    need(
      !(isNum(p0) && Math.abs(p0 - c.vrijednost) <= tol2),
      `već početne vrijednosti klizača (${tKlizaci(s)}) ispunjavaju cilj; neka učenik mora nešto promijeniti`
    );
    if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
      s.krivo.forEach((k, i) => {
        need(
          isNum(k?.vrijednost) && isText(k?.poruka),
          `krivo[${i}] treba {vrijednost (prvog klizača), poruka}`
        );
        if (isNum(k?.vrijednost)) {
          const v = ishodGibanja(s, {
            ...r.parametri,
            [s.klizaci[0].id]: k.vrijednost
          });
          need(
            !(isNum(v) && Math.abs(v - c.vrijednost) <= tol2),
            `krivo[${i}].vrijednost ispunjava cilj, pa nije kriva`
          );
        }
      });
    }
  }
  var imeTijela = (s, id) => s.tijela.find((t) => t.id === id)?.naziv ?? "tijelo";
  var veliko = (t) => t.charAt(0).toUpperCase() + t.slice(1);
  var iznos = (n, u) => withUnit(Number(n.toPrecision(3)), u);
  function checkZaustavi(s, odgovor3) {
    const zapisano = (Array.isArray(odgovor3) ? odgovor3 : []).filter(isNum).sort((a, b) => a - b);
    if (!zapisano.length)
      return {
        tocno: false,
        poruka: "Zaustavi gibanje u pravom trenutku i pritisni „Zabilježi trenutak“.",
        prazno: true
      };
    const ts = trenuciCilja(s);
    const c = s.cilj;
    const m = modelGibanja(s);
    const vel = velicine(s);
    const pogodjeni = /* @__PURE__ */ new Set();
    for (const t of zapisano) {
      const i = ts.findIndex((r) => Math.abs(r - t) <= tolerancijaTrenutka(s, r));
      if (i >= 0) {
        if (pogodjeni.has(i))
          return {
            tocno: false,
            poruka: `Isti trenutak (oko ${withUnit(ts[i], "s")}) zabilježio si dva puta. Ukloni jedan.`
          };
        pogodjeni.add(i);
        continue;
      }
      const k = (s.krivo ?? []).find(
        (q) => Math.abs(q.vrijednost - t) <= tolerancijaTrenutka(s, q.vrijednost)
      );
      if (k) return { tocno: false, poruka: k.poruka };
      const najblizi = ts.reduce(
        (a, b2) => Math.abs(b2 - t) < Math.abs(a - t) ? b2 : a,
        ts[0]
      );
      const smjer = najblizi > t ? "Pusti malo dulje." : "Zaustavi malo ranije.";
      if (c.velicina === "susret") {
        const d = Math.abs(m.tijela[0].stanje(t).s - m.tijela[1].stanje(t).s);
        return {
          tocno: false,
          poruka: `U t = ${withUnit(t, "s")} tijela su još ${iznos(d, "m")} jedno od drugoga. ${smjer}`
        };
      }
      const b = m.tijela.find((x) => x.id === c.tijelo);
      const v = b.stanje(t)[c.velicina];
      const { oznaka, jedinica: jedinica4 } = vel[c.velicina];
      return {
        tocno: false,
        poruka: `U t = ${withUnit(t, "s")} ${imeTijela(s, c.tijelo)} ima ${oznaka} = ${iznos(v, jedinica4)}, a traži se ${oznaka} = ${withUnit(c.vrijednost, jedinica4)}. ${smjer}`
      };
    }
    const svi = c.svi !== false;
    if (svi && pogodjeni.size < ts.length) {
      const fali = ts.length - pogodjeni.size;
      const naziv4 = veliko(imeTijela(s, c.tijelo));
      const opis = c.velicina === "susret" ? "Tijela se susreću još jednom." : s.prostor === "vertikalno" && c.velicina === "s" && ts.length === 2 ? `${naziv4} je na visini ${withUnit(c.vrijednost, "m")} dvaput: na putu gore i na putu dolje.` : `${naziv4} ima ${vel[c.velicina].oznaka} = ${withUnit(c.vrijednost, vel[c.velicina].jedinica)} još ${fali === 1 ? "jednom" : `${fali} puta`}.`;
      return {
        tocno: false,
        poruka: `${pogodjeni.size === 1 ? "Taj trenutak je dobar, ali nije jedini." : "Ti trenuci su dobri, ali nisu svi."} ${opis} Pronađi ${fali === 1 ? "i drugi trenutak pa ga" : "i ostale trenutke pa ih"} zabilježi.`
      };
    }
    return { tocno: true };
  }
  function checkPostavi(s, odgovor3) {
    if (!odgovor3 || typeof odgovor3 !== "object")
      return {
        tocno: false,
        poruka: "Namjesti klizač i pritisni Provjeri.",
        prazno: true
      };
    const p = {};
    for (const k of s.klizaci) {
      const v2 = odgovor3[k.id];
      if (!isNum(v2) || v2 < k.min - 1e-9 || v2 > k.max + 1e-9)
        return {
          tocno: false,
          poruka: "Namjesti klizač i pritisni Provjeri.",
          prazno: true
        };
      p[k.id] = v2;
    }
    const c = s.cilj;
    const o = opisCilja(s);
    const v = ishodGibanja(s, p);
    const tol2 = tolerancijaCilja(s);
    if (isNum(v) && Math.abs(v - c.vrijednost) <= tol2) return { tocno: true };
    const k0 = s.klizaci[0];
    const krivo = (s.krivo ?? []).find(
      (q) => Math.abs(q.vrijednost - p[k0.id]) <= k0.korak / 2 + 1e-9
    );
    if (krivo) return { tocno: false, poruka: krivo.poruka };
    if (!isNum(v))
      return {
        tocno: false,
        poruka: c.velicina === "susret" ? "S tim vrijednostima tijela se ne susretnu. Pokušaj drukčije." : "S tim vrijednostima to se ne dogodi. Pokušaj drukčije."
      };
    let savjet3 = "";
    if (s.klizaci.length === 1) {
      const gore = ishodGibanja(s, {
        [k0.id]: Math.min(k0.max, p[k0.id] + k0.korak)
      });
      const dolje = ishodGibanja(s, {
        [k0.id]: Math.max(k0.min, p[k0.id] - k0.korak)
      });
      if (isNum(gore) && isNum(dolje) && Math.abs(gore - dolje) > 1e-12) {
        const udesno = gore > dolje === c.vrijednost > v;
        savjet3 = ` Pomakni klizač „${k0.oznaka.replace(/\$/g, "").trim()}“ ${udesno ? "udesno" : "ulijevo"}.`;
      }
    }
    const razlika2 = iznos(Math.abs(v - c.vrijednost), o.jedinica);
    if (c.velicina === "kraj") {
      const b = modelGibanja(s, p).tijela.find((x) => x.id === c.tijelo);
      const naprijed = c.vrijednost >= b.stanje(0).s;
      return {
        tocno: false,
        poruka: `${veliko(imeTijela(s, c.tijelo))} stane na ${iznos(v, "m")}, ${razlika2} ${v > c.vrijednost === naprijed ? "iza" : "prije"} cilja (${withUnit(c.vrijednost, "m")}).${savjet3}`
      };
    }
    return {
      tocno: false,
      poruka: `${veliko(o.ime)} je ${iznos(v, o.jedinica)}, a treba ${withUnit(c.vrijednost, o.jedinica)} (${razlika2} ${v > c.vrijednost ? "previše" : "premalo"}).${savjet3}`
    };
  }

  // src/lib/lesson/katalog/fizika/crtaj_graf.js
  var NACINI2 = ["izlomljena", "stepenice", "pravac", "istrazi"];
  var VRSTE = ["izlomljena", "stepenice"];
  var EPS2 = 1e-9;
  var isPar2 = (v) => Array.isArray(v) && v.length === 2 && v.every(isNum) && v[1] > v[0];
  var naMrezi = (v, k) => Math.abs(v / k - Math.round(v / k)) < 1e-6;
  var zn = (v, eps) => v > eps ? 1 : v < -eps ? -1 : 0;
  var vrstaGrafa = (s) => s.nacin === "istrazi" ? s.vrsta ?? "izlomljena" : s.nacin;
  var brojVrijednosti = (vrsta, cx) => vrsta === "stepenice" ? cx.length - 1 : cx.length;
  var tolerancijaCrte = (s) => isNum(s.tolerancija) ? s.tolerancija : s.korak_y / 2;
  function osi(s) {
    const ox = dijeloviOsi(s.os_x, "x");
    const oy = dijeloviOsi(s.os_y, "y");
    const meta = (v, zadano) => v && typeof v === "object" ? {
      oznaka: v.oznaka ?? zadano.oznaka,
      jedinica: v.jedinica ?? zadano.jedinica
    } : zadano;
    return {
      x: ox,
      y: oy,
      povrsina: meta(s.povrsina, {
        oznaka: "površina",
        jedinica: [oy.jedinica, ox.jedinica].filter(Boolean).join("·")
      }),
      nagib: meta(s.nagib, {
        oznaka: "nagib",
        jedinica: oy.jedinica && ox.jedinica ? `${oy.jedinica}/${ox.jedinica}` : ""
      })
    };
  }
  function pravacTocaka(s) {
    const t = s.tocke ?? [];
    if (s.kroz_ishodiste) {
      const sxx2 = t.reduce((a, [x]) => a + x * x, 0);
      return { nagib: t.reduce((a, [x, y]) => a + x * y, 0) / sxx2, odsjecak: 0 };
    }
    const n = t.length;
    const [mx, my] = [
      t.reduce((a, [x]) => a + x, 0) / n,
      t.reduce((a, [, y]) => a + y, 0) / n
    ];
    const sxx = t.reduce((a, [x]) => a + (x - mx) ** 2, 0);
    const k = t.reduce((a, [x, y]) => a + (x - mx) * (y - my), 0) / sxx;
    return { nagib: k, odsjecak: my - k * mx };
  }
  var tolNagiba = (s) => isNum(s.tolerancija) ? s.tolerancija : 0.04;
  function pocetneVrijednosti(s) {
    const n = brojVrijednosti(vrstaGrafa(s), s.cvorovi_x ?? []);
    const nula = s.y?.[0] <= 0 && s.y?.[1] >= 0 ? 0 : s.y?.[0] ?? 0;
    const out = Array.isArray(s.pocetak) ? [...s.pocetak] : Array.from({ length: n }, () => nula);
    for (const i of s.fiksni ?? []) out[i] = s.odgovor?.[i];
    return out;
  }
  function pocetniPravac(s) {
    const { nagib, odsjecak } = pravacTocaka(s);
    return {
      nagib: nagib * 0.55,
      odsjecak: s.kroz_ishodiste ? 0 : odsjecak + (s.y[1] - s.y[0]) * 0.12
    };
  }
  function krivi(s, odgovor3) {
    const tol2 = tolerancijaCrte(s);
    return (odgovor3 ?? []).map((v, i) => Math.abs(v - s.odgovor[i]) > tol2 + EPS2 ? i : -1).filter((i) => i >= 0);
  }
  var fx = (s, x) => `${formatNumber(x)}${osi(s).x.jedinica ? ` ${osi(s).x.jedinica}` : ""}`;
  var dionica = (s, i) => `${formatNumber(s.cvorovi_x[i])}–${fx(s, s.cvorovi_x[i + 1])}`;
  var fy = (s, y) => `${formatNumber(y)}${osi(s).y.jedinica ? ` ${osi(s).y.jedinica}` : ""}`;
  var savjet = (s, i) => isText(s.savjeti?.[i]) ? ` ${s.savjeti[i]}` : "";
  var jos = (n) => n > 0 ? ` (Osim toga ${n === 1 ? "još jedna točka nije dobra" : `još ${n} točke nisu dobre`}.)` : "";
  function checkIzlomljena(s, odgovor3) {
    const n = s.cvorovi_x.length;
    if (!Array.isArray(odgovor3) || odgovor3.length !== n || !odgovor3.every(isNum))
      return {
        tocno: false,
        poruka: "Povuci točke grafa na prava mjesta.",
        prazno: true
      };
    const tol2 = tolerancijaCrte(s);
    const k = (s.krivo ?? []).find(
      (q) => q.odgovor.every((v, i2) => Math.abs(v - odgovor3[i2]) <= tol2 + EPS2)
    );
    if (k) return { tocno: false, poruka: k.poruka };
    const losi = krivi(s, odgovor3);
    if (!losi.length) return { tocno: true };
    const j = losi[0];
    const { x, y } = osi(s);
    if (j === 0)
      return {
        tocno: false,
        poruka: `Graf ne počinje dobro: koliki je ${y.ime} na početku (${x.ime} = ${fx(s, s.cvorovi_x[0])})?${savjet(s, 0)}${jos(losi.length - 1)}`
      };
    const i = j - 1;
    const eps = s.korak_y * 1e-6;
    const c = zn(s.odgovor[j] - s.odgovor[i], eps);
    const u = zn(odgovor3[j] - odgovor3[i], eps);
    let poruka2;
    if (c !== u) {
      const opis = c === 0 ? `${y.ime} se ne mijenja, pa graf mora biti vodoravan` : c > 0 ? `${y.ime} raste, pa graf mora ići prema gore` : `${y.ime} se smanjuje, pa graf mora ići prema dolje`;
      poruka2 = `Na dionici ${dionica(s, i)} ${opis}.`;
    } else {
      poruka2 = `Na dionici ${dionica(s, i)} smjer je dobar, ali na kraju dionice (${x.ime} = ${fx(s, s.cvorovi_x[j])}) točka je ${odgovor3[j] > s.odgovor[j] ? "previsoka" : "preniska"}.`;
    }
    return {
      tocno: false,
      poruka: `${poruka2}${savjet(s, i)}${jos(losi.length - 1)}`
    };
  }
  function checkStepenice(s, odgovor3) {
    const n = s.cvorovi_x.length - 1;
    if (!Array.isArray(odgovor3) || odgovor3.length !== n || !odgovor3.every(isNum))
      return {
        tocno: false,
        poruka: "Povuci razinu na svakoj dionici.",
        prazno: true
      };
    const tol2 = tolerancijaCrte(s);
    const k = (s.krivo ?? []).find(
      (q) => q.odgovor.every((v, i2) => Math.abs(v - odgovor3[i2]) <= tol2 + EPS2)
    );
    if (k) return { tocno: false, poruka: k.poruka };
    const losi = krivi(s, odgovor3);
    if (!losi.length) return { tocno: true };
    const i = losi[0];
    const { x, y } = osi(s);
    const eps = s.korak_y * 1e-6;
    const [c, u] = [zn(s.odgovor[i], eps), zn(odgovor3[i], eps)];
    let poruka2;
    if (c !== u) {
      poruka2 = c === 0 ? `Na dionici ${dionica(s, i)} ${y.ime} je nula: razina mora ležati na osi ${x.ime}.` : `Na dionici ${dionica(s, i)} razina mora biti ${c > 0 ? "iznad" : "ispod"} osi ${x.ime} (${c > 0 ? "pozitivna" : "negativna"} vrijednost).`;
    } else {
      poruka2 = `Na dionici ${dionica(s, i)} predznak je dobar, ali razina je ${odgovor3[i] > s.odgovor[i] ? "previsoka" : "preniska"}.`;
    }
    return {
      tocno: false,
      poruka: `${poruka2}${savjet(s, i)}${losi.length > 1 ? ` (Osim toga ${losi.length === 2 ? "još jedna dionica nije dobra" : `još ${losi.length - 1} dionice nisu dobre`}.)` : ""}`
    };
  }
  function checkPravac(s, odgovor3) {
    if (!odgovor3 || !isNum(odgovor3.nagib) || !isNum(odgovor3.odsjecak))
      return {
        tocno: false,
        poruka: "Povuci pravac kroz izmjerene točke.",
        prazno: true
      };
    const p = pravacTocaka(s);
    const { nagib } = osi(s);
    const rel = (odgovor3.nagib - p.nagib) / Math.abs(p.nagib);
    if (Math.abs(rel) > tolNagiba(s) + EPS2) {
      const prestrm = odgovor3.nagib > p.nagib === p.nagib > 0;
      const sada = `${nagib.oznaka} = ${formatSci(odgovor3.nagib)}${nagib.jedinica ? ` ${nagib.jedinica}` : ""}`;
      return {
        tocno: false,
        poruka: `Pravac je ${prestrm ? "prestrm" : "preblag"} (${sada}): točke desno su ${prestrm ? "ispod" : "iznad"} njega. Zakreni ga tako da točke budu podjednako iznad i ispod pravca.`
      };
    }
    if (!s.kroz_ishodiste) {
      const xs = s.tocke.map(([x]) => x);
      const mx = xs.reduce((a, b) => a + b, 0) / xs.length;
      const d = odgovor3.nagib * mx + odgovor3.odsjecak - (p.nagib * mx + p.odsjecak);
      if (Math.abs(d) > (s.y[1] - s.y[0]) * 0.04)
        return {
          tocno: false,
          poruka: `Nagib je dobar, ali pravac je ${d > 0 ? "previsoko" : "prenisko"}: pomakni ga tako da prolazi kroz sredinu točaka.`
        };
    }
    return { tocno: true };
  }
  function validirajGraf(s, vrsta, need, pref = "") {
    const cx = s.cvorovi_x;
    if (!need(
      Array.isArray(cx) && cx.length >= 2 && cx.length <= 7 && cx.every(isNum),
      `${pref}cvorovi_x mora biti popis od 2–7 brojeva`
    ))
      return false;
    need(
      cx.every((x, i) => i === 0 || x > cx[i - 1]),
      `${pref}cvorovi_x moraju rasti`
    );
    if (isPar2(s.x))
      need(
        cx[0] >= s.x[0] - EPS2 && cx.at(-1) <= s.x[1] + EPS2,
        `${pref}cvorovi_x moraju biti unutar osi x`
      );
    return need(
      VRSTE.includes(vrsta),
      `${pref}vrsta mora biti ${VRSTE.join(" ili ")}`
    );
  }
  function validirajVrijednosti(v, n, s, need, ime2) {
    if (!need(
      Array.isArray(v) && v.length === n && v.every(isNum),
      `${ime2} mora imati ${n} brojeva`
    ))
      return false;
    need(
      v.every((y) => y >= s.y[0] - EPS2 && y <= s.y[1] + EPS2),
      `${ime2}: sve vrijednosti moraju biti na osi y (${formatNumber(s.y[0])} do ${formatNumber(s.y[1])})`
    );
    return true;
  }
  function validiraj2(s) {
    const { list, need } = problems();
    if (!need(
      NACINI2.includes(s.nacin),
      `crtaj_graf.nacin mora biti ${NACINI2.join(", ")}`
    ))
      return list;
    need(isText(s.tekst), "crtaj_graf treba „tekst“");
    need(
      isText(s.os_x) && isText(s.os_y),
      'crtaj_graf treba os_x i os_y (npr. "t (s)", "v (m/s)")'
    );
    const osiOk = need(
      isPar2(s.x) && isPar2(s.y),
      "x i y moraju biti [od, do] uz od < do"
    );
    const korakOk = need(
      isNum(s.korak_x) && s.korak_x > 0 && isNum(s.korak_y) && s.korak_y > 0,
      "korak_x i korak_y moraju biti pozitivni brojevi (razmak crta mreže)"
    );
    if (!osiOk || !korakOk) return list;
    need(
      (s.x[1] - s.x[0]) / s.korak_x <= 40 && (s.y[1] - s.y[0]) / s.korak_y <= 40,
      "mreža je pregusta (najviše 40 crta po osi); povećaj korak"
    );
    for (const k of ["povrsina", "nagib"]) {
      const v = s[k];
      if (v != null)
        need(
          typeof v === "boolean" || typeof v === "object" && (v.oznaka == null || isText(v.oznaka)) && (v.jedinica == null || typeof v.jedinica === "string"),
          `${k} je true ili {oznaka?, jedinica?}`
        );
    }
    if (s.tolerancija != null)
      need(
        isNum(s.tolerancija) && s.tolerancija > 0,
        "tolerancija mora biti pozitivan broj"
      );
    const vrsta = vrstaGrafa(s);
    if (s.nacin === "pravac") {
      need(s.povrsina == null, "pravac nema površinu");
      if (need(
        Array.isArray(s.tocke) && s.tocke.length >= 3 && s.tocke.length <= 12 && s.tocke.every(
          (t) => Array.isArray(t) && t.length === 2 && t.every(isNum)
        ),
        "pravac treba tocke: [[x, y], …] (3–12 izmjerenih točaka)"
      )) {
        need(
          s.tocke.every(
            ([x, y]) => x >= s.x[0] && x <= s.x[1] && y >= s.y[0] && y <= s.y[1]
          ),
          "sve izmjerene točke moraju biti unutar osi"
        );
        const p = pravacTocaka(s);
        need(
          Number.isFinite(p.nagib) && Math.abs(p.nagib) > EPS2,
          "kroz točke se ne može provući kosi pravac (sve imaju isti x ili isti y)"
        );
        if (s.tolerancija != null)
          need(
            s.tolerancija < 0.5,
            "tolerancija pravca je relativna (npr. 0.04 = 4 %), manja od 0,5"
          );
        const p0 = pocetniPravac(s);
        need(
          Math.abs(p0.nagib - p.nagib) / Math.abs(p.nagib) > tolNagiba(s),
          "tolerancija je prevelika: početni pravac je već točan"
        );
      }
      if (s.kroz_ishodiste != null)
        need(
          typeof s.kroz_ishodiste === "boolean",
          "kroz_ishodiste je true ili false"
        );
      if (s.kroz_ishodiste)
        need(
          s.x[0] <= 0 && s.y[0] <= 0,
          "pravac kroz ishodište treba ishodište na osima (x i y od 0 ili manje)"
        );
      for (const f2 of ["odgovor", "cvorovi_x", "krivo"])
        need(
          !(f2 in s),
          `pravac nema polje „${f2}“ (točan nagib računa aplikacija iz točaka)`
        );
      return list;
    }
    if (!validirajGraf(s, vrsta, need)) return list;
    const n = brojVrijednosti(vrsta, s.cvorovi_x);
    const dionica3 = s.cvorovi_x.length - 1;
    if (s.savjeti != null)
      need(
        Array.isArray(s.savjeti) && s.savjeti.length === dionica3 && s.savjeti.every((t) => t == null || isText(t)),
        `savjeti je popis od ${dionica3} teksta (po jedan za svaku dionicu, null gdje ne treba)`
      );
    if (s.referenca != null) {
      const r = s.referenca;
      if (need(
        r && typeof r === "object" && isText(r.os_y) && isPar2(r.y),
        "referenca treba os_y i y: [od, do]"
      )) {
        const rv = r.vrsta ?? "izlomljena";
        const rc = r.cvorovi_x ?? s.cvorovi_x;
        if (validirajGraf({ ...r, cvorovi_x: rc, x: s.x }, rv, need, "referenca."))
          validirajVrijednosti(
            r.vrijednosti,
            brojVrijednosti(rv, rc),
            { y: r.y },
            need,
            "referenca.vrijednosti"
          );
        if (r.korak_y != null)
          need(
            isNum(r.korak_y) && r.korak_y > 0 && (r.y[1] - r.y[0]) / r.korak_y <= 40,
            "referenca.korak_y mora biti pozitivan (najviše 40 crta)"
          );
      }
    }
    if (s.nacin === "istrazi") {
      noAnswerFields(s, need);
      need(
        !("pocetak" in s) && !("fiksni" in s),
        "istrazi nema pocetak ni fiksni (graf je zadan)"
      );
      validirajVrijednosti(s.vrijednosti, n, s, need, "vrijednosti");
      return list;
    }
    need(
      s.vrsta == null,
      "vrsta se zadaje samo u načinu istrazi (ovdje je to nacin)"
    );
    need(
      !("vrijednosti" in s),
      "zadani graf (vrijednosti) postoji samo u načinu istrazi; točan graf je u „odgovor“"
    );
    if (!validirajVrijednosti(s.odgovor, n, s, need, "odgovor")) return list;
    need(
      s.odgovor.every((y) => naMrezi(y, s.korak_y)),
      `odgovor mora biti na crtama mreže (višekratnici od korak_y = ${formatNumber(s.korak_y)}), jer se točke lijepe za mrežu`
    );
    if (s.fiksni != null) {
      need(
        Array.isArray(s.fiksni) && s.fiksni.every((i) => Number.isInteger(i) && i >= 0 && i < n) && new Set(s.fiksni).size === s.fiksni.length && s.fiksni.length < n,
        `fiksni je popis indeksa (0–${n - 1}) zadanih ${vrsta === "stepenice" ? "dionica" : "točaka"}; barem jedna mora ostati za učenika`
      );
    }
    if (s.pocetak != null && validirajVrijednosti(s.pocetak, n, s, need, "pocetak"))
      need(
        s.pocetak.every((y) => naMrezi(y, s.korak_y)),
        "pocetak mora biti na crtama mreže"
      );
    if (!list.length)
      need(
        krivi(s, pocetneVrijednosti(s)).length > 0,
        "početni graf je već točan; zadaj pocetak koji se razlikuje od odgovora"
      );
    if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
      s.krivo.forEach((k, i) => {
        if (need(
          Array.isArray(k?.odgovor) && k.odgovor.length === n && k.odgovor.every(isNum) && isText(k?.poruka),
          `krivo[${i}] treba {odgovor: ${n} brojeva, poruka}`
        ))
          need(
            krivi(s, k.odgovor).length > 0,
            `krivo[${i}].odgovor jednak je točnom`
          );
      });
    }
    return list;
  }
  var crtaj_graf = {
    tip: "crtaj_graf",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Učenik crta graf u koordinatnom sustavu. Polja: tekst, nacin, os_x, os_y (npr. "t (s)", "v (m/s)"), x: [od, do], y: [od, do], korak_x, korak_y (mreža;
točke se lijepe za crte korak_y), povrsina?: true | {oznaka, jedinica} (površina ispod grafa, npr. {oznaka: "put", jedinica: "m"}), nagib?: true | {oznaka, jedinica}
(npr. {oznaka: "a", jedinica: "m/s²"}), referenca?: {os_y, y: [od, do], korak_y?, vrsta?, cvorovi_x?, vrijednosti} (zadani graf iznad, s istom osi x, npr. a–t uz crtanje v–t),
savjeti?: [tekst ili null po dionici] (dodaje se poruci kad je ta dionica kriva). Načini:
- "izlomljena": cvorovi_x (2–7 x-koordinata čvorova), odgovor (y svakog čvora, na mreži), fiksni? (indeksi zadanih čvorova, npr. [0] za početak),
  pocetak? (početni y čvorova; zadano 0), tolerancija? (zadano korak_y/2), krivo?: [{odgovor: [y po čvoru], poruka}] (tipičan krivi graf, npr. s–t umjesto v–t).
  Poruka sama kaže koja je dionica kriva i treba li graf ići gore, dolje ili vodoravno.
- "stepenice": cvorovi_x (granice dionica), odgovor (razina na svakoj dionici: jedan broj manje od cvorovi_x), fiksni?, pocetak?, tolerancija?, krivo? (a–t graf).
- "pravac": tocke: [[x, y], …] (3–12 izmjerenih), kroz_ishodiste? (jedno hvatište), tolerancija? (relativna, zadano 0,04), nagib?: {oznaka, jedinica}
  (npr. {oznaka: "k", jedinica: "N/m"}). Točan nagib (najmanji kvadrati) računa aplikacija; uživo se ispisuje nagib učenikova pravca.
- "istrazi": korak za gledanje: vrsta? ("izlomljena" | "stepenice"), cvorovi_x, vrijednosti (zadani graf); učenik vuče kursor, uz povrsina se boji površina
  do kursora, uz nagib se crta trokut nagiba na dionici pod kursorom.`,
    primjer: {
      tip: "crtaj_graf",
      nacin: "izlomljena",
      tekst: "Nacrtaj $v$–$t$ graf: auto 10 s ubrzava iz mirovanja do 30 m/s, a zatim koči i stane za 6 s.",
      os_x: "t (s)",
      os_y: "v (m/s)",
      x: [0, 16],
      y: [0, 35],
      korak_x: 2,
      korak_y: 5,
      cvorovi_x: [0, 10, 16],
      odgovor: [0, 30, 0],
      povrsina: { oznaka: "put", jedinica: "m" },
      savjeti: [
        "Brzina raste od 0 do 30 m/s.",
        "Auto koči do zaustavljanja: na kraju je v = 0."
      ]
    },
    primjeri: [
      {
        tip: "crtaj_graf",
        nacin: "izlomljena",
        tekst: "Iz $a$–$t$ grafa motocikla nacrtaj njegov $v$–$t$ graf. Motocikl kreće iz mirovanja. Površina ispod $a$–$t$ grafa je promjena brzine.",
        os_x: "t (s)",
        os_y: "v (m/s)",
        x: [0, 8],
        y: [0, 15],
        korak_x: 1,
        korak_y: 1,
        cvorovi_x: [0, 4, 8],
        odgovor: [0, 12, 12],
        fiksni: [0],
        referenca: {
          os_y: "a (m/s²)",
          y: [0, 4],
          korak_y: 1,
          vrsta: "stepenice",
          vrijednosti: [3, 0]
        },
        krivo: [
          {
            odgovor: [0, 3, 3],
            poruka: "To je akceleracija, a ne brzina. Brzina raste svake sekunde za 3 m/s, pa nakon 4 s iznosi 4 · 3 m/s."
          }
        ],
        savjeti: [null, "Kad je a = 0, brzina se ne mijenja."]
      },
      {
        tip: "crtaj_graf",
        nacin: "stepenice",
        tekst: "Iz $v$–$t$ grafa automobila nacrtaj $a$–$t$ graf. Akceleracija je nagib $v$–$t$ grafa na svakoj dionici.",
        os_x: "t (s)",
        os_y: "a (m/s²)",
        x: [0, 18],
        y: [-4, 6],
        korak_x: 2,
        korak_y: 0.5,
        cvorovi_x: [0, 4, 10, 18],
        odgovor: [5, 0, -2.5],
        referenca: {
          os_y: "v (m/s)",
          y: [0, 25],
          korak_y: 5,
          vrijednosti: [0, 20, 20, 0]
        },
        savjeti: [
          "Nagib: 20 m/s u 4 s.",
          null,
          "Brzina pada 20 m/s u 8 s; kad tijelo usporava, a je negativna."
        ]
      },
      {
        tip: "crtaj_graf",
        nacin: "pravac",
        tekst: "Provuci pravac kroz izmjerene točke (sila o produljenju opruge). Nagib pravca je konstanta opruge $k$.",
        os_x: "x (m)",
        os_y: "F (N)",
        x: [0, 0.6],
        y: [0, 30],
        korak_x: 0.1,
        korak_y: 5,
        tocke: [
          [0.1, 4.9],
          [0.2, 10.2],
          [0.3, 14.7],
          [0.4, 20.4],
          [0.5, 25]
        ],
        kroz_ishodiste: true,
        nagib: { oznaka: "k", jedinica: "N/m" }
      },
      {
        tip: "crtaj_graf",
        nacin: "istrazi",
        tekst: "Vuci kursor po $v$–$t$ grafu: obojena površina je put prijeđen do tog trenutka, a trokut pokazuje nagib (akceleraciju) dionice.",
        os_x: "t (s)",
        os_y: "v (m/s)",
        x: [0, 18],
        y: [0, 25],
        korak_x: 2,
        korak_y: 5,
        cvorovi_x: [0, 4, 10, 18],
        vrijednosti: [0, 20, 20, 0],
        povrsina: { oznaka: "put", jedinica: "m" },
        nagib: { oznaka: "a", jedinica: "m/s²" }
      }
    ],
    validate: (s) => validiraj2(s),
    check(s, odgovor3) {
      if (s.nacin === "pravac") return checkPravac(s, odgovor3);
      if (s.nacin === "stepenice") return checkStepenice(s, odgovor3);
      return checkIzlomljena(s, odgovor3);
    },
    tocanOdgovor(s) {
      if (s.nacin === "pravac") {
        const { nagib } = osi(s);
        const p = pravacTocaka(s);
        const j = nagib.jedinica ? ` ${nagib.jedinica}` : "";
        const [a, b] = [
          p.nagib * (1 - tolNagiba(s)),
          p.nagib * (1 + tolNagiba(s))
        ].sort((u, v) => u - v);
        return `${nagib.oznaka} ≈ ${formatSci(p.nagib)}${j} (prihvaća se od ${formatSci(a)} do ${formatSci(b)}${j})`;
      }
      if (s.nacin === "stepenice")
        return s.odgovor.map((v, i) => `${dionica(s, i)}: ${fy(s, v)}`).join("; ");
      if (s.nacin === "izlomljena")
        return s.odgovor.map((v, i) => `(${fx(s, s.cvorovi_x[i])}, ${fy(s, v)})`).join(" → ");
      return "";
    },
    brojcaniOdgovor: (s) => s.nacin === "pravac" && Array.isArray(s.tocke) ? pravacTocaka(s).nagib : null,
    kljucOdgovora: (s) => s.nacin === "pravac" ? Array.isArray(s.tocke) ? pravacTocaka(s).nagib.toFixed(4) : null : s.nacin === "istrazi" ? null : JSON.stringify([s.cvorovi_x, s.odgovor])
  };

  // src/lib/lesson/katalog/fizika/_sile.js
  var G_ZEMLJA = 9.81;
  var GRAV_KONST = 6674e-14;
  var rad = (d) => d * Math.PI / 180;
  var normKut = (d) => (d % 360 + 360) % 360;
  var razlikaKuta = (a, b) => {
    const d = normKut(a - b);
    return Math.min(d, 360 - d);
  };
  var kutVektora = (x, y) => normKut(Math.atan2(y, x) * 180 / Math.PI);
  var STRANE = {
    desno: 0,
    "gore-desno": 45,
    gore: 90,
    "gore-lijevo": 135,
    lijevo: 180,
    "dolje-lijevo": 225,
    dolje: 270,
    "dolje-desno": 315
  };
  var KUTOVI_16 = Array.from({ length: 16 }, (_, i) => i * 22.5);
  var RIJECI = {
    0: "udesno",
    45: "gore udesno",
    90: "gore",
    135: "gore ulijevo",
    180: "ulijevo",
    225: "dolje ulijevo",
    270: "dolje",
    315: "dolje udesno"
  };
  function smjerRijecima(kut) {
    const k = normKut(kut);
    const ime2 = Object.keys(RIJECI).find((v) => razlikaKuta(Number(v), k) < 0.5);
    if (ime2 != null) return RIJECI[ime2];
    const vod = k < 90 || k > 270 ? "udesno" : "ulijevo";
    const nagib = Math.round(
      k < 90 ? k : k < 180 ? 180 - k : k < 270 ? k - 180 : 360 - k
    );
    return `${nagib}° ${k < 180 ? "iznad" : "ispod"} vodoravne, ${vod}`;
  }
  var broj3 = (n, sig = 3) => isNum(n) ? Math.abs(n) < 1e-9 ? "0" : formatSci(n, sig) : "—";
  var iznosJed = (n, jed2, sig = 3) => `${broj3(n, sig)}${jed2 ? ` ${jed2}` : ""}`;
  var ID4 = /^[A-Za-z_][A-Za-z_0-9]*$/;
  var klizaciOd = (s) => Array.isArray(s.klizaci) ? s.klizaci : [];
  var pocetneVrijednosti2 = (s) => Object.fromEntries(klizaciOd(s).map((k) => [k.id, k.pocetak]));
  var klizacZa = (s, x) => typeof x === "string" ? klizaciOd(s).find((k) => k.id === x) : void 0;
  function iznos2(s, x, p) {
    if (isNum(x)) return x;
    const k = klizacZa(s, x);
    if (!k) return NaN;
    const v = p && isNum(p[x]) ? p[x] : k.pocetak;
    return v * (isNum(k.faktor) ? k.faktor : 1);
  }
  function validateKlizaci(s, need, { najmanje = 0, najvise = 3 } = {}) {
    if (s.klizaci == null) {
      need(
        najmanje === 0,
        `${s.tip} (${s.nacin}) treba ${najmanje === najvise ? najmanje : `${najmanje}–${najvise}`} klizač(a)`
      );
      return;
    }
    if (!need(
      Array.isArray(s.klizaci) && s.klizaci.length >= najmanje && s.klizaci.length <= najvise,
      `klizaci: ${s.tip} (${s.nacin}) smije imati ${najmanje === najvise ? `točno ${najmanje}` : `${najmanje}–${najvise}`} klizač(a)`
    ))
      return;
    const ids = [];
    s.klizaci.forEach((k, i) => {
      need(
        ID4.test(k?.id ?? ""),
        `klizaci[${i}].id mora biti ime varijable (slova, bez razmaka)`
      );
      need(
        isText(k?.oznaka),
        `klizaci[${i}] treba „oznaka“ (npr. "masa tijela")`
      );
      ids.push(k?.id);
      if (need(
        [k?.min, k?.max, k?.korak, k?.pocetak].every(isNum),
        `klizaci[${i}] treba brojeve min, max, korak, pocetak`
      )) {
        need(
          k.max > k.min && k.korak > 0,
          `klizaci[${i}]: mora biti min < max i korak > 0`
        );
        need(
          k.pocetak >= k.min && k.pocetak <= k.max,
          `klizaci[${i}].pocetak mora biti između min i max`
        );
        const n = (k.max - k.min) / k.korak;
        need(n <= 1e3, `klizaci[${i}] ima previše koraka (najviše 1000)`);
        need(
          Math.abs(n - Math.round(n)) < 1e-6,
          `klizaci[${i}]: (max − min) mora biti višekratnik koraka`
        );
        need(
          Math.abs(
            (k.pocetak - k.min) / k.korak - Math.round((k.pocetak - k.min) / k.korak)
          ) < 1e-6,
          `klizaci[${i}].pocetak mora biti na koraku klizača`
        );
      }
      if (k?.jedinica != null)
        need(
          typeof k.jedinica === "string",
          `klizaci[${i}].jedinica mora biti tekst`
        );
      if (k?.faktor != null)
        need(
          isNum(k.faktor) && k.faktor > 0,
          `klizaci[${i}].faktor mora biti pozitivan broj (SI vrijednost = vrijednost klizača · faktor)`
        );
    });
    need(
      new Set(ids).size === ids.length,
      "id-jevi klizača moraju biti različiti"
    );
  }
  function validatePolje(s, need, ime2, { min = -Infinity, max = Infinity, obavezno = true, opis = "" } = {}) {
    const x = s[ime2];
    if (x == null) {
      need(
        !obavezno,
        `${s.tip} (${s.scena ?? s.nacin}) treba polje „${ime2}“${opis ? ` (${opis})` : ""}`
      );
      return false;
    }
    const raspon2 = `${formatNumber(min)} – ${formatNumber(max)}`.replace("-Infinity", "−∞").replace("Infinity", "∞");
    if (isNum(x))
      return need(
        x >= min && x <= max,
        `${ime2} = ${formatNumber(x)} je izvan smislenih granica (${raspon2})`
      );
    const k = klizacZa(s, x);
    if (!need(k, `${ime2} mora biti broj ili id klizača (nema klizača „${x}“)`))
      return false;
    if (![k.min, k.max].every(isNum)) return false;
    const f2 = isNum(k.faktor) ? k.faktor : 1;
    return need(
      k.min * f2 >= min && k.max * f2 <= max,
      `klizač „${k.id}“ za ${ime2} izlazi iz smislenih granica (${raspon2})`
    );
  }
  function validateKoristeniKlizaci(s, need, koristeni) {
    for (const k of klizaciOd(s))
      need(
        koristeni.includes(k.id),
        `klizač „${k.id}“ ne mijenja ništa u sceni (nijedno polje nije "${k.id}")`
      );
  }
  function krajnjeKombinacije(s) {
    let out = [pocetneVrijednosti2(s)];
    for (const k of klizaciOd(s))
      out = out.flatMap((p) => [
        p,
        { ...p, [k.id]: k.min },
        { ...p, [k.id]: k.max }
      ]);
    return out;
  }
  function analizaCilja(s, k, velicine3) {
    const cilj = s.cilj;
    const razlika2 = (v) => {
      const vel = velicine3({ ...pocetneVrijednosti2(s), [k.id]: v });
      const t = isNum(cilj.vrijednost) ? cilj.vrijednost : vel[cilj.vrijednost];
      return { q: vel[cilj.velicina], t, d: vel[cilj.velicina] - t };
    };
    const n = Math.round((k.max - k.min) / k.korak);
    const vr = Array.from(
      { length: n + 1 },
      (_, i) => Math.round((k.min + i * k.korak) * 1e9) / 1e9
    );
    const d = vr.map((v) => razlika2(v).d);
    let best = 0;
    d.forEach((x, i) => {
      if (Math.abs(x) < Math.abs(d[best])) best = i;
    });
    const lokalno = Math.max(
      best > 0 ? Math.abs(d[best - 1] - d[best]) : 0,
      best < n ? Math.abs(d[best + 1] - d[best]) : 0
    );
    const tol2 = isNum(cilj.tolerancija) ? cilj.tolerancija : Math.max(lokalno * 0.5 * 1.0001, 1e-9);
    const ok = (v) => {
      const x = razlika2(v).d;
      return isNum(x) && Math.abs(x) <= tol2;
    };
    return {
      vrijednosti: vr,
      best: vr[best],
      tol: tol2,
      dostizno: isNum(d[best]) && Math.abs(d[best]) <= tol2,
      ok,
      razlika: razlika2
    };
  }
  function validateCilj(s, need, k, velicine3, meta) {
    const c = s.cilj;
    if (!need(
      c && typeof c === "object",
      `${s.tip} postavi treba cilj: {velicina, vrijednost}`
    ))
      return;
    const imena = Object.keys(meta);
    if (!need(
      imena.includes(c.velicina),
      `cilj.velicina mora biti jedna od: ${imena.join(", ")}`
    ))
      return;
    if (!need(
      isNum(c.vrijednost) || imena.includes(c.vrijednost),
      `cilj.vrijednost mora biti broj ili ime druge veličine (${imena.join(", ")})`
    ))
      return;
    need(
      c.vrijednost !== c.velicina,
      "cilj.vrijednost ne smije biti ista veličina kao cilj.velicina"
    );
    if (c.tolerancija != null)
      need(
        isNum(c.tolerancija) && c.tolerancija >= 0,
        "cilj.tolerancija mora biti nenegativan broj (u jedinici veličine)"
      );
    if (!k || ![k.min, k.max, k.korak, k.pocetak].every(isNum) || !(k.korak > 0) || !(k.max > k.min))
      return;
    let a;
    try {
      a = analizaCilja(s, k, velicine3);
    } catch {
      need(false, "cilj se ne može izračunati iz scene");
      return;
    }
    need(
      a.dostizno,
      `cilj nije ostvariv klizačem „${k.id}“ (od ${formatNumber(k.min)} do ${formatNumber(k.max)}, korak ${formatNumber(k.korak)}): najbliže je ${formatNumber(a.best)}${k.jedinica ? ` ${k.jedinica}` : ""}`
    );
    need(
      !a.ok(k.pocetak),
      `klizač „${k.id}“ na početku (${formatNumber(k.pocetak)}) već ostvaruje cilj, pa bi „Provjeri“ bez pomicanja bio točan`
    );
  }
  function checkCilj(s, odgovor3, velicine3, meta) {
    const k = klizaciOd(s)[0];
    const v = isNum(odgovor3) ? odgovor3 : odgovor3?.[k.id];
    if (!isNum(v))
      return { tocno: false, poruka: "Pomakni klizač.", prazno: true };
    const a = analizaCilja(s, k, velicine3);
    if (a.ok(v)) return { tocno: true };
    const hit = (s.krivo ?? []).find((x) => near(v, x.vrijednost, k.korak * 0.5));
    if (hit) return { tocno: false, poruka: hit.poruka };
    const { q, t } = a.razlika(v);
    const m = meta[s.cilj.velicina];
    const ime2 = String(k.oznaka).replace(/\$/g, "");
    const kl = `${ime2.charAt(0).toUpperCase()}${ime2.slice(1)} = ${withUnit(v, k.jedinica)}`;
    const tren = `${m.naziv} je ${opisVelicine(q, m)}`;
    const treba = isNum(s.cilj.vrijednost) ? `a treba ${iznosJed(t, m.jedinica)}` : `a treba biti koliko i ${meta[s.cilj.vrijednost]?.naziv ?? s.cilj.vrijednost} (${iznosJed(t, m.jedinica)})`;
    return {
      tocno: false,
      poruka: `${kl}: ${tren}, ${treba}.${s.savjet ? ` ${s.savjet}` : ""}`
    };
  }
  function opisVelicine(q, m) {
    if (!isNum(q)) return "—";
    if (m.smjer && Math.abs(q) > 1e-9)
      return `${iznosJed(Math.abs(q), m.jedinica)} ${q > 0 ? m.smjer[0] : m.smjer[1]}`;
    return iznosJed(m.smjer ? Math.abs(q) : q, m.jedinica);
  }
  var NACIN_ISTRAZI2 = `"istrazi": korak za gledanje: učenik pomiče klizače i gleda, ništa se ne provjerava i nema polja s odgovorom (ni cilj ni krivo)`;
  var KLIZACI_OPIS = `klizaci: [{id, oznaka, min, max, korak, pocetak, jedinica?, faktor?}]; brojčano polje speca smije umjesto broja biti id klizača (SI vrijednost = vrijednost klizača · faktor, npr. visina u km uz faktor 1000)`;
  var CILJ_OPIS = `cilj: {velicina, vrijednost (broj ili ime druge veličine scene), tolerancija?}; točno je ono što aplikacija izračuna iz scene, a bez tolerancije prolazi samo najbliži položaj klizača`;

  // src/lib/lesson/katalog/fizika/sile_tijelo.js
  var SCENE = ["kutija", "kolica", "dizalo", "kosina", "kolotura"];
  var NACINI3 = ["nacrtaj", "istrazi", "postavi"];
  var DULJINE = 6;
  var smjerKut = (smjer) => isNum(smjer) ? normKut(smjer) : STRANE[smjer];
  function fizikaSila(s, p = pocetneVrijednosti2(s), v = s.v0 ?? 0) {
    const g = isNum(s.g) ? s.g : G_ZEMLJA;
    const sile = [];
    const sila = (id, oznaka, izvor2, kut, F, tijelo2 = 0) => {
      if (F > 1e-9)
        sile.push({ id, oznaka, izvor: izvor2, kut: normKut(kut), iznos: F, tijelo: tijelo2 });
    };
    const vel = {};
    if (s.scena === "kutija" || s.scena === "kolica") {
      const m = iznos2(s, s.masa, p);
      const mi = s.scena === "kolica" ? 0 : iznos2(s, s.mi ?? 0, p);
      let fx2 = 0;
      let fy2 = 0;
      const vlastite = (s.sile ?? []).map((f2) => ({
        ...f2,
        kut: smjerKut(f2.smjer),
        F: iznos2(s, f2.iznos, p)
      }));
      for (const f2 of vlastite) {
        fx2 += f2.F * Math.cos(rad(f2.kut));
        fy2 += f2.F * Math.sin(rad(f2.kut));
      }
      if (Math.abs(fx2) < 1e-9) fx2 = 0;
      const G3 = m * g;
      const N = Math.max(0, G3 - fy2);
      let tr = 0;
      if (mi > 0 && N > 0) {
        if (Math.abs(v) > 1e-9) tr = -Math.sign(v) * mi * N;
        else if (Math.abs(fx2) <= mi * N) tr = -fx2;
        else tr = -Math.sign(fx2) * mi * N;
      }
      sila("G", "F_g", "Zemlja privlači", 270, G3);
      sila("N", "N", "pod gura", 90, N);
      sila("F_tr", "F_{tr}", "trenje s podom", tr > 0 ? 0 : 180, Math.abs(tr));
      for (const f2 of vlastite) sila(f2.id, f2.oznaka ?? f2.id, f2.izvor, f2.kut, f2.F);
      Object.assign(vel, {
        m,
        G: G3,
        N,
        F_tr: Math.abs(tr),
        F_rez: fx2 + tr,
        a: (fx2 + tr) / m
      });
      for (const f2 of vlastite) vel[f2.id] = f2.F;
      return { sile, vel, a: vel.a, g };
    }
    if (s.scena === "dizalo") {
      const m = iznos2(s, s.masa, p);
      const a = iznos2(s, s.a ?? 0, p);
      const G3 = m * g;
      const N = Math.max(0, m * (g + a));
      sila("G", "F_g", "Zemlja privlači", 270, G3);
      sila("N", "N", "vaga gura", 90, N);
      Object.assign(vel, {
        m,
        G: G3,
        N,
        vaga: s.vaga === "kg" ? N / g : N,
        a,
        F_rez: N - G3
      });
      return { sile, vel, a, g };
    }
    if (s.scena === "kosina") {
      const m = iznos2(s, s.masa, p);
      const kut = iznos2(s, s.kut, p);
      const mi = iznos2(s, s.mi ?? 0, p);
      const G3 = m * g;
      const Fpar = G3 * Math.sin(rad(kut));
      const Fok = G3 * Math.cos(rad(kut));
      const N = Fok;
      let tr = 0;
      let a = 0;
      if (v > 1e-9) tr = mi * N;
      else if (Fpar <= mi * N + 1e-9) tr = Fpar;
      else tr = mi * N;
      a = (Fpar - tr) / m;
      if (Math.abs(a) < 1e-9) a = 0;
      sila("G", "F_g", "Zemlja privlači", 270, G3);
      sila("N", "N", "kosina gura", 90 - kut, N);
      sila("F_tr", "F_{tr}", "trenje s kosinom", 180 - kut, tr);
      Object.assign(vel, { m, kut, G: G3, N, F_par: Fpar, F_okom: Fok, F_tr: tr, a });
      return { sile, vel, a, g, kut };
    }
    if (s.scena === "kolotura") {
      const m1 = iznos2(s, s.m1, p);
      const m2 = iznos2(s, s.m2, p);
      const mi = iznos2(s, s.mi ?? 0, p);
      const G1 = m1 * g;
      const G22 = m2 * g;
      let a = 0;
      let T;
      let tr;
      if (v > 1e-9 || G22 > mi * G1 + 1e-9) {
        a = (G22 - mi * G1) / (m1 + m2);
        tr = mi * G1;
        T = m2 * (g - a);
      } else {
        T = G22;
        tr = G22;
      }
      sila("G1", "F_{g1}", "Zemlja privlači", 270, G1, 0);
      sila("N", "N", "stol gura", 90, G1, 0);
      sila("T1", "T", "nit vuče", 0, T, 0);
      sila("F_tr", "F_{tr}", "trenje sa stolom", 180, tr, 0);
      sila("G2", "F_{g2}", "Zemlja privlači", 270, G22, 1);
      sila("T2", "T", "nit vuče", 90, T, 1);
      Object.assign(vel, { m1, m2, a, T, G1, G2: G22, N: G1, F_tr: tr });
      return { sile, vel, a, g };
    }
    return { sile, vel, a: 0, g };
  }
  var tijelaScene = (s) => s.scena === "kolotura" ? ["tijelo na stolu", "obješeno tijelo"] : [
    s.naziv ?? {
      kutija: "sanduk",
      kolica: "kolica",
      dizalo: "osoba",
      kosina: "tijelo"
    }[s.scena] ?? "tijelo"
  ];
  function velicineScene(s) {
    const F = (naziv4, oznaka) => ({ naziv: naziv4, oznaka, jedinica: "N" });
    if (s.scena === "kutija" || s.scena === "kolica") {
      const out = {
        m: { naziv: "masa", oznaka: "m", jedinica: "kg" },
        G: F("sila teža", "F_g"),
        N: F("sila podloge", "N"),
        F_tr: F("sila trenja", "F_{tr}"),
        F_rez: {
          ...F("rezultantna sila", "F_{rez}"),
          smjer: ["udesno", "ulijevo"]
        },
        a: {
          naziv: "akceleracija",
          oznaka: "a",
          jedinica: "m/s²",
          smjer: ["udesno", "ulijevo"]
        }
      };
      if (s.scena === "kolica") delete out.F_tr;
      for (const f2 of s.sile ?? [])
        if (f2?.id && !(f2.id in out))
          out[f2.id] = F(
            isText(f2.izvor) ? `sila (${f2.izvor})` : "sila",
            f2.oznaka ?? f2.id
          );
      return out;
    }
    if (s.scena === "dizalo")
      return {
        m: { naziv: "masa", oznaka: "m", jedinica: "kg" },
        G: F("sila teža", "F_g"),
        N: F("sila vage na osobu", "N"),
        vaga: {
          naziv: "očitanje vage",
          oznaka: "vaga",
          jedinica: s.vaga === "kg" ? "kg" : "N"
        },
        a: {
          naziv: "akceleracija dizala",
          oznaka: "a",
          jedinica: "m/s²",
          smjer: ["prema gore", "prema dolje"]
        },
        F_rez: {
          ...F("rezultantna sila", "F_{rez}"),
          smjer: ["prema gore", "prema dolje"]
        }
      };
    if (s.scena === "kosina")
      return {
        m: { naziv: "masa", oznaka: "m", jedinica: "kg" },
        kut: { naziv: "nagib kosine", oznaka: "α", jedinica: "°" },
        G: F("sila teža", "F_g"),
        N: F("sila kosine", "N"),
        F_par: F("komponenta sile teže niz kosinu", "F_∥"),
        F_okom: F("komponenta sile teže okomito na kosinu", "F_⊥"),
        F_tr: F("sila trenja", "F_{tr}"),
        a: {
          naziv: "akceleracija niz kosinu",
          oznaka: "a",
          jedinica: "m/s²",
          smjer: ["niz kosinu", "uz kosinu"]
        }
      };
    if (s.scena === "kolotura")
      return {
        m1: { naziv: "masa tijela na stolu", oznaka: "m_1", jedinica: "kg" },
        m2: { naziv: "masa obješenog tijela", oznaka: "m_2", jedinica: "kg" },
        a: { naziv: "akceleracija", oznaka: "a", jedinica: "m/s²" },
        T: F("sila niti", "T"),
        G1: F("sila teža tijela na stolu", "F_{g1}"),
        G2: F("sila teža obješenog tijela", "F_{g2}"),
        N: F("sila stola", "N"),
        F_tr: F("sila trenja", "F_{tr}")
      };
    return {};
  }
  var lazneKarte = (s) => (s.ponudjene ?? []).filter((c) => c?.lazna);
  var praveKarte = (s) => (s.ponudjene ?? []).filter((c) => !c?.lazna);
  function karteNacrtaj(s) {
    const sile = fizikaSila(s).sile;
    return (s.ponudjene ?? []).map((c) => {
      const f2 = sile.find((x) => x.id === c.id);
      return {
        id: c.id,
        oznaka: c.oznaka ?? f2?.oznaka ?? c.id,
        izvor: c.izvor ?? f2?.izvor ?? "",
        lazna: !!c.lazna,
        tijelo: f2?.tijelo ?? null
      };
    });
  }
  function tocanCrtez(s) {
    const sile = fizikaSila(s).sile.filter(
      (f2) => praveKarte(s).some((c) => c.id === f2.id)
    );
    const max = Math.max(...sile.map((f2) => f2.iznos));
    const L = Object.fromEntries(
      sile.map((f2) => [f2.id, Math.max(1, Math.round(DULJINE * f2.iznos / max))])
    );
    for (let i = 0; i < 20; i++) {
      let ok = true;
      for (const o of s.odnosi ?? []) {
        if (!(o.a in L) || !(o.b in L) || odnosVrijedi(L[o.a], o.op, L[o.b]))
          continue;
        ok = false;
        if (o.op === "=") L[o.b] = L[o.a];
        else if (o.op === ">") L[o.a] < DULJINE ? L[o.a]++ : L[o.b]--;
        else L[o.b] < DULJINE ? L[o.b]++ : L[o.a]--;
      }
      if (ok) break;
    }
    return sile.map((f2) => ({
      id: f2.id,
      tijelo: f2.tijelo,
      kut: f2.kut,
      duljina: Math.min(DULJINE, Math.max(1, L[f2.id]))
    }));
  }
  var odnosVrijedi = (a, op, b) => op === ">" ? a > b : op === "<" ? a < b : a === b;
  var OP_RIJEC = { ">": "dulja od", "<": "kraća od", "=": "jednako duga kao" };
  var sto = (c) => `${c.oznaka.includes("$") ? c.oznaka : `$${c.oznaka}$`}${c.izvor ? ` (${c.izvor})` : ""}`;
  function savjetSmjera(id) {
    if (/^G\d?$/.test(id))
      return "Zemlja privlači svako tijelo okomito prema dolje.";
    if (id === "N")
      return "Podloga gura tijelo okomito od sebe, okomito na plohu na kojoj tijelo leži.";
    if (id === "F_tr")
      return "Trenje djeluje uz podlogu, suprotno smjeru u kojem tijelo klizi (ili bi kliznulo).";
    if (/^T\d$/.test(id))
      return "Nit vuče tijelo duž sebe, od tijela prema niti.";
    return "Pogledaj u zadatku kamo ta sila djeluje.";
  }
  function checkNacrtaj(s, odg) {
    if (!Array.isArray(odg) || !odg.length)
      return {
        tocno: false,
        poruka: "Dovuci sile na tijelo (povuci karticu na crtež).",
        prazno: true
      };
    const karte = karteNacrtaj(s);
    const tijela = tijelaScene(s);
    const sile = fizikaSila(s).sile;
    const cards = Object.fromEntries(karte.map((c) => [c.id, c]));
    const pravi = Object.fromEntries(sile.map((f2) => [f2.id, f2]));
    for (const o of odg) {
      const c = cards[o.id];
      if (c?.lazna) {
        const src = (s.ponudjene ?? []).find((x) => x.id === o.id);
        return {
          tocno: false,
          poruka: src?.poruka || `Sila „${c.izvor}“ ne postoji: ne možeš reći koje je to tijelo daje. Svaka sila ima izvor (Zemlja, podloga, uže, ruka…).`
        };
      }
    }
    if (tijela.length > 1)
      for (const o of odg) {
        const f2 = pravi[o.id];
        if (f2 && o.tijelo !== f2.tijelo)
          return {
            tocno: false,
            poruka: `Sila ${sto(cards[o.id])} djeluje na ${tijela[f2.tijelo]}, a ne na ${tijela[o.tijelo] ?? "to tijelo"}.`
          };
      }
    const bezSmjera = odg.find((o) => !isNum(o.kut));
    if (bezSmjera)
      return {
        tocno: false,
        poruka: `Odredi smjer sile ${sto(cards[bezSmjera.id])}: povuci vrh strelice.`,
        prazno: true
      };
    for (const o of odg) {
      const f2 = pravi[o.id];
      if (!f2 || razlikaKuta(o.kut, f2.kut) < 1) continue;
      const obrnuto = razlikaKuta(o.kut, f2.kut + 180) < 1;
      return {
        tocno: false,
        poruka: `Strelica sile ${sto(cards[o.id])} ${obrnuto ? "pokazuje u suprotnom smjeru" : "ne pokazuje pravim smjerom"}. ${savjetSmjera(o.id)}`
      };
    }
    const fali = praveKarte(s).filter((c) => !odg.some((o) => o.id === c.id));
    if (fali.length) {
      const n = fali.length;
      const savjet3 = fali[0].nedostaje || (tijela.length > 1 ? "Za svako tijelo pitaj se: tko ga privlači i što ga sve dodiruje (podloga, nit, ruka)?" : "Pitaj se: tko privlači tijelo i što ga sve dodiruje (podloga, uže, ruka)?");
      return {
        tocno: false,
        poruka: `Fali još ${n} ${n === 1 ? "sila" : "sile"}. ${savjet3}`
      };
    }
    const L = Object.fromEntries(odg.map((o) => [o.id, o.duljina]));
    for (const r of s.odnosi ?? []) {
      if (odnosVrijedi(L[r.a], r.op, L[r.b])) continue;
      return {
        tocno: false,
        poruka: `Sile su dobre, ali strelica ${sto(cards[r.a])} treba biti ${OP_RIJEC[r.op]} strelice ${sto(cards[r.b])}.${r.poruka ? ` ${r.poruka}` : ""} Duljinu mijenjaš povlačenjem vrha.`
      };
    }
    return { tocno: true };
  }
  var OPIS = `Scena s tijelom i silama (fizika, matura). tekst, scena, nacin, g? (zadano 9,81; 10 ako ga zadatak koristi). Sve sile i veličine računa aplikacija iz scene.
Scene i polja (brojčano polje smije biti id klizača):
- "kutija": sanduk na podu: masa (kg), mi? (faktor trenja), v0? (početna brzina u m/s, + udesno: tijelo već klizi, trenje je kinetičko), sile?: [{id, oznaka (npr. "F"), izvor (npr. "uže vuče"), smjer ("desno", "gore-lijevo"… ili kut u °, 0 = desno, 90 = gore), iznos (N), uze?: true (nacrtaj uže)}] (najviše 3). Sila teža, sila podloge N i trenje dodaju se same (id G, N, F_tr).
- "kolica": kao kutija, bez trenja.
- "dizalo": osoba na vagi u dizalu: masa, a (akceleracija dizala u m/s², + prema gore), vaga? ("N" | "kg", što vaga pokazuje; zadano N). Sile G i N = m(g + a).
- "kosina": masa, kut (°, kosina se spušta udesno), mi?, rastav? (true: crtkane komponente sile teže uz i okomito na kosinu). Sile G, N, F_tr.
- "kolotura": m1 na stolu, m2 visi preko koloture, mi? (trenje sa stolom). Sile G1, N, T1, F_tr na tijelu 1 i G2, T2 na tijelu 2.
Veličine (za prikazi i cilj): kutija/kolica m, G, N, F_tr, F_rez, a (+ udesno) i id-jevi vlastitih sila; dizalo m, G, N, vaga, a, F_rez; kosina m, kut, G, N, F_par, F_okom, F_tr, a (niz kosinu); kolotura m1, m2, a, T, G1, G2, N, F_tr.
Načini:
- "nacrtaj" (Tvoj red): učenik dovuče kartice sila na tijelo i okrene strelice (zaključavaju se na 8 strana, smjerove kosine i smjerove sila scene). ponudjene: [{id, oznaka?, izvor?, nedostaje? (savjet kad sila fali)}] su prave sile scene (id iz popisa sila; smjer računa aplikacija; moraju biti sve sile scene) i lažne [{id, oznaka?, izvor, lazna: true, poruka}] (najviše 3, npr. „sila kojom sanduk stoji“). odnosi?: [{a, op (">" | "<" | "="), b, poruka?}] po duljini strelica (moraju se slagati s iznosima sila). Bez klizača.
- ${NACIN_ISTRAZI2}. ${KLIZACI_OPIS} (najviše 3); prikazi?: popis veličina koje se vide uživo (najviše 4). Tijelo se giba s izračunatom akceleracijom.
- "postavi" (Tvoj red): kao istrazi s točno jednim klizačem; ${CILJ_OPIS}. Npr. kolica jednoliko: {velicina: "a", vrijednost: 0}; vaga pokazuje nulu: {velicina: "vaga", vrijednost: 0}. Ciljana veličina se ne vidi dok učenik ne provjeri. krivo?: [{vrijednost (vrijednost klizača), poruka}].`;
  var PRIMJER = {
    tip: "sile_tijelo",
    nacin: "nacrtaj",
    scena: "kutija",
    g: 10,
    masa: 35,
    sile: [
      {
        id: "F",
        oznaka: "F",
        izvor: "uže vuče",
        smjer: 30,
        iznos: 200,
        uze: true
      }
    ],
    tekst: "Uže vuče sanduk pod kutom 30° prema vodoravnoj. Dovuci na sanduk **sve sile** koje na njega djeluju i okreni ih u pravi smjer.",
    ponudjene: [
      { id: "G", izvor: "Zemlja privlači" },
      { id: "N", izvor: "pod gura" },
      { id: "F", izvor: "uže vuče" },
      {
        id: "X",
        oznaka: "F_s",
        izvor: "sila kojom sanduk stoji",
        lazna: true,
        poruka: "Tko bi tu silu davao? Sanduk ne može djelovati sam na sebe. Ako ne možeš imenovati izvor sile, ta sila ne postoji."
      }
    ]
  };
  var PRIMJERI = [
    {
      tip: "sile_tijelo",
      nacin: "istrazi",
      scena: "kolica",
      g: 10,
      masa: 4,
      tekst: "Ana gura kolica udesno, Boris ih vuče ulijevo. **Pomiči klizače** i gledaj rezultantnu silu i akceleraciju.",
      sile: [
        {
          id: "FA",
          oznaka: "F_A",
          izvor: "Ana gura",
          smjer: "desno",
          iznos: "FA"
        },
        {
          id: "FB",
          oznaka: "F_B",
          izvor: "Boris vuče",
          smjer: "lijevo",
          iznos: "FB"
        }
      ],
      klizaci: [
        {
          id: "FA",
          oznaka: "Anina sila",
          min: 0,
          max: 50,
          korak: 1,
          pocetak: 30,
          jedinica: "N"
        },
        {
          id: "FB",
          oznaka: "Borisova sila",
          min: 0,
          max: 50,
          korak: 1,
          pocetak: 18,
          jedinica: "N"
        }
      ]
    },
    {
      tip: "sile_tijelo",
      nacin: "postavi",
      scena: "kolica",
      g: 10,
      masa: 4,
      tekst: "Ana gura kolica silom 30 N. Postavi Borisovu silu tako da se kolica gibaju **jednoliko**.",
      sile: [
        { id: "FA", oznaka: "F_A", izvor: "Ana gura", smjer: "desno", iznos: 30 },
        {
          id: "FB",
          oznaka: "F_B",
          izvor: "Boris vuče",
          smjer: "lijevo",
          iznos: "FB"
        }
      ],
      klizaci: [
        {
          id: "FB",
          oznaka: "Borisova sila",
          min: 0,
          max: 50,
          korak: 1,
          pocetak: 18,
          jedinica: "N"
        }
      ],
      prikazi: ["F_rez", "a"],
      cilj: { velicina: "a", vrijednost: 0 }
    },
    {
      tip: "sile_tijelo",
      nacin: "istrazi",
      scena: "dizalo",
      g: 10,
      masa: 70,
      a: "a",
      tekst: "Osoba od 70 kg stoji na vagi u dizalu. **Mijenjaj akceleraciju dizala** i gledaj što pokazuje vaga.",
      klizaci: [
        {
          id: "a",
          oznaka: "akceleracija dizala (+ gore)",
          min: -10,
          max: 5,
          korak: 1,
          pocetak: 0,
          jedinica: "m/s²"
        }
      ]
    },
    {
      tip: "sile_tijelo",
      nacin: "postavi",
      scena: "dizalo",
      g: 10,
      masa: 70,
      a: "a",
      tekst: "Postavi akceleraciju dizala tako da vaga pokaže **nulu**.",
      klizaci: [
        {
          id: "a",
          oznaka: "akceleracija dizala (+ gore)",
          min: -10,
          max: 5,
          korak: 1,
          pocetak: 0,
          jedinica: "m/s²"
        }
      ],
      prikazi: ["G", "N", "vaga"],
      cilj: { velicina: "vaga", vrijednost: 0 },
      krivo: [
        {
          vrijednost: 0,
          poruka: "Dok dizalo miruje ili se giba jednoliko, vaga pokazuje punu težinu."
        }
      ]
    },
    {
      tip: "sile_tijelo",
      nacin: "nacrtaj",
      scena: "dizalo",
      g: 10,
      masa: 70,
      a: 2,
      tekst: "Dizalo ubrzava **prema gore** akceleracijom 2 m/s². Nacrtaj sile na osobu i pazi koja je strelica dulja.",
      ponudjene: [
        { id: "G", izvor: "Zemlja privlači" },
        { id: "N", izvor: "vaga gura" },
        {
          id: "X",
          oznaka: "F_d",
          izvor: "sila dizala prema gore",
          lazna: true,
          poruka: "Dizalo ne dodiruje osobu, nego vaga. Na osobu djeluju samo Zemlja i vaga."
        }
      ],
      odnosi: [
        {
          a: "N",
          op: ">",
          b: "G",
          poruka: "Rezultantna sila mora gledati prema gore, kamo je i akceleracija."
        }
      ]
    },
    {
      tip: "sile_tijelo",
      nacin: "postavi",
      scena: "kutija",
      g: 10,
      masa: 20,
      mi: 0.3,
      v0: 1,
      tekst: "Sanduk od 20 kg već klizi udesno, a faktor trenja je 0,3. Postavi silu guranja tako da sanduk klizi **jednoliko**.",
      sile: [
        { id: "F", oznaka: "F", izvor: "ruka gura", smjer: "desno", iznos: "F" }
      ],
      klizaci: [
        {
          id: "F",
          oznaka: "sila guranja",
          min: 0,
          max: 100,
          korak: 5,
          pocetak: 20,
          jedinica: "N"
        }
      ],
      prikazi: ["F_tr", "a"],
      cilj: { velicina: "a", vrijednost: 0 }
    },
    {
      tip: "sile_tijelo",
      nacin: "istrazi",
      scena: "kosina",
      g: 10,
      masa: "m",
      kut: "kut",
      rastav: true,
      tekst: "Tijelo kliže niz kosinu bez trenja. **Mijenjaj nagib i masu**: o čemu ovisi akceleracija?",
      klizaci: [
        {
          id: "kut",
          oznaka: "nagib kosine",
          min: 10,
          max: 60,
          korak: 5,
          pocetak: 30,
          jedinica: "°"
        },
        {
          id: "m",
          oznaka: "masa tijela",
          min: 1,
          max: 10,
          korak: 1,
          pocetak: 5,
          jedinica: "kg"
        }
      ],
      prikazi: ["F_par", "a"]
    },
    {
      tip: "sile_tijelo",
      nacin: "nacrtaj",
      scena: "kosina",
      g: 10,
      masa: 5,
      kut: 30,
      tekst: "Tijelo kliže niz kosinu bez trenja. Nacrtaj sile koje na njega djeluju.",
      ponudjene: [
        { id: "G", izvor: "Zemlja privlači" },
        { id: "N", izvor: "kosina gura" },
        {
          id: "X",
          oznaka: "F_k",
          izvor: "sila niz kosinu",
          lazna: true,
          poruka: "Niz kosinu tijelo vuče dio sile teže; to nije zasebna sila. Tko bi je davao?"
        }
      ]
    },
    {
      tip: "sile_tijelo",
      nacin: "istrazi",
      scena: "kolotura",
      g: 10,
      m1: "m1",
      m2: "m2",
      tekst: "Dva tijela povezana niti preko koloture. **Pomiči klizače** i gledaj akceleraciju i silu niti.",
      klizaci: [
        {
          id: "m1",
          oznaka: "masa na stolu",
          min: 1,
          max: 6,
          korak: 1,
          pocetak: 3,
          jedinica: "kg"
        },
        {
          id: "m2",
          oznaka: "obješena masa",
          min: 1,
          max: 6,
          korak: 1,
          pocetak: 2,
          jedinica: "kg"
        }
      ],
      prikazi: ["a", "T", "G2"]
    },
    {
      tip: "sile_tijelo",
      nacin: "nacrtaj",
      scena: "kolotura",
      g: 10,
      m1: 3,
      m2: 2,
      tekst: "Nacrtaj sile na **svako** tijelo: dovuci karticu na tijelo na koje ta sila djeluje.",
      ponudjene: [
        { id: "G1" },
        { id: "N" },
        { id: "T1" },
        { id: "G2" },
        { id: "T2" }
      ]
    }
  ];
  var sile_tijelo = {
    tip: "sile_tijelo",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: OPIS,
    primjer: PRIMJER,
    primjeri: PRIMJERI,
    fizika: fizikaSila,
    velicine: velicineScene,
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI3.includes(s.nacin),
        `sile_tijelo.nacin mora biti jedan od: ${NACINI3.join(", ")}`
      );
      need(isText(s.tekst), "sile_tijelo treba „tekst“");
      if (!need(
        SCENE.includes(s.scena),
        `sile_tijelo.scena mora biti jedna od: ${SCENE.join(", ")}`
      ))
        return list;
      if (s.g != null)
        need(
          isNum(s.g) && s.g >= 1 && s.g <= 30,
          "g mora biti broj od 1 do 30 (m/s²)"
        );
      if (s.nacin === "istrazi") validateKlizaci(s, need, { najvise: 3 });
      else if (s.nacin === "postavi")
        validateKlizaci(s, need, { najmanje: 1, najvise: 1 });
      else
        need(
          s.klizaci == null,
          "nacrtaj nema klizače: sve vrijednosti su brojevi"
        );
      const koristeni = [];
      const polje2 = (ime2, o) => {
        if (typeof s[ime2] === "string") koristeni.push(s[ime2]);
        return validatePolje(s, need, ime2, o);
      };
      const dozvoljena = {
        kutija: ["masa", "mi", "v0", "sile"],
        kolica: ["masa", "v0", "sile"],
        dizalo: ["masa", "a", "vaga"],
        kosina: ["masa", "kut", "mi", "rastav"],
        kolotura: ["m1", "m2", "mi"]
      }[s.scena];
      for (const f2 of [
        "masa",
        "mi",
        "v0",
        "sile",
        "a",
        "vaga",
        "kut",
        "rastav",
        "m1",
        "m2"
      ])
        need(
          !(f2 in s) || dozvoljena.includes(f2),
          `polje „${f2}“ ne postoji u sceni ${s.scena} (polja scene: ${dozvoljena.join(", ")})`
        );
      const sc = s.scena;
      if (sc === "kolotura") {
        polje2("m1", { min: 0.01, max: 1e3, opis: "masa tijela na stolu u kg" });
        polje2("m2", { min: 0.01, max: 1e3, opis: "masa obješenog tijela u kg" });
      } else polje2("masa", { min: 0.01, max: 1e5, opis: "masa u kg" });
      if (sc === "kutija" || sc === "kosina" || sc === "kolotura")
        polje2("mi", { min: 0, max: 2, obavezno: false });
      if (sc === "kutija" || sc === "kolica") {
        if (s.v0 != null)
          need(
            isNum(s.v0) && Math.abs(s.v0) <= 50,
            "v0 mora biti broj od −50 do 50 (m/s)"
          );
        if (sc === "kolica")
          need(s.sile != null, "kolica trebaju sile (npr. Ana gura, Boris vuče)");
        if (s.sile != null && need(
          Array.isArray(s.sile) && s.sile.length >= 1 && s.sile.length <= 3,
          "sile mora biti popis od 1 do 3 sile"
        )) {
          const rez = ["G", "N", "F_tr", "F_rez", "a", "m"];
          s.sile.forEach((f2, i) => {
            need(
              /^[A-Za-z][A-Za-z0-9_]*$/.test(f2?.id ?? "") && !rez.includes(f2.id),
              `sile[${i}].id mora biti ime (slova i brojke) i ne smije biti ${rez.join(", ")}`
            );
            need(
              isText(f2?.izvor),
              `sile[${i}] treba „izvor“ (tko djeluje, npr. "uže vuče")`
            );
            need(
              isNum(f2?.smjer) || f2?.smjer in STRANE,
              `sile[${i}].smjer mora biti kut u ° ili jedno od: ${Object.keys(STRANE).join(", ")}`
            );
            if (typeof f2?.iznos === "string") koristeni.push(f2.iznos);
            if (isNum(f2?.iznos))
              need(
                f2.iznos >= 0 && f2.iznos <= 1e6,
                `sile[${i}].iznos mora biti od 0 do 10⁶ N`
              );
            else
              need(
                klizaciOd(s).some((k) => k.id === f2?.iznos),
                `sile[${i}].iznos mora biti broj (N) ili id klizača`
              );
          });
          need(
            new Set(s.sile.map((f2) => f2?.id)).size === s.sile.length,
            "id-jevi sila moraju biti različiti"
          );
        }
      }
      if (sc === "dizalo") {
        polje2("a", {
          min: -30,
          max: 30,
          obavezno: false,
          opis: "akceleracija dizala u m/s²"
        });
        if (s.vaga != null)
          need(["N", "kg"].includes(s.vaga), 'vaga mora biti "N" ili "kg"');
      }
      if (sc === "kosina") {
        polje2("kut", { min: 0, max: 80, opis: "nagib u °" });
        if (s.rastav != null)
          need(typeof s.rastav === "boolean", "rastav mora biti true ili false");
      }
      if (list.length) return list;
      validateKoristeniKlizaci(s, need, koristeni);
      if (sc === "kutija" || sc === "kolica") {
        for (const p of krajnjeKombinacije(s)) {
          const m = iznos2(s, s.masa, p);
          const fy2 = (s.sile ?? []).reduce(
            (z, f2) => z + iznos2(s, f2.iznos, p) * Math.sin(rad(isNum(f2.smjer) ? f2.smjer : STRANE[f2.smjer])),
            0
          );
          if (!need(
            fy2 <= m * (isNum(s.g) ? s.g : G_ZEMLJA) + 1e-9,
            "vlastite sile podižu tijelo s poda (okomita komponenta veća od sile teže): smanji ih ili povećaj masu"
          ))
            break;
        }
      }
      const meta = velicineScene(s);
      if (s.prikazi != null && need(
        Array.isArray(s.prikazi) && s.prikazi.length >= 1 && s.prikazi.length <= 4,
        "prikazi mora biti popis od 1 do 4 veličine"
      )) {
        for (const v of s.prikazi)
          need(
            v in meta,
            `prikazi: „${v}“ nije veličina scene (${Object.keys(meta).join(", ")})`
          );
      }
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        need(
          s.ponudjene == null && s.odnosi == null,
          "istrazi nema ponudjene ni odnosi (to je za nacrtaj)"
        );
      }
      if (s.nacin === "postavi") {
        need(
          s.ponudjene == null && s.odnosi == null,
          "postavi nema ponudjene ni odnosi (to je za nacrtaj)"
        );
        const k = klizaciOd(s)[0];
        validateCilj(s, need, k, (p) => fizikaSila(s, p).vel, meta);
        if (k) validateKrivo(s.krivo, null, 0, need);
        if (k && Array.isArray(s.krivo) && s.cilj) {
          try {
            const a = analizaCilja(s, k, (p) => fizikaSila(s, p).vel);
            s.krivo.forEach(
              (x, i) => isNum(x?.vrijednost) && need(
                !a.ok(x.vrijednost),
                `krivo[${i}].vrijednost ${formatNumber(x.vrijednost)} ostvaruje cilj, a to je točan odgovor`
              )
            );
          } catch {
          }
        }
      }
      if (s.nacin === "nacrtaj") {
        need(s.cilj == null && s.prikazi == null, "nacrtaj nema cilj ni prikazi");
        validateNacrtaj(s, need);
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "nacrtaj") return checkNacrtaj(s, odgovor3);
      if (s.nacin === "postavi")
        return checkCilj(
          s,
          odgovor3,
          (p) => fizikaSila(s, p).vel,
          velicineScene(s)
        );
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "postavi") {
        const k = klizaciOd(s)[0];
        const a = analizaCilja(s, k, (p) => fizikaSila(s, p).vel);
        return `${k.oznaka} = ${withUnit(a.best, k.jedinica)}`;
      }
      if (s.nacin === "nacrtaj") {
        const karte = Object.fromEntries(karteNacrtaj(s).map((c) => [c.id, c]));
        const tijela = tijelaScene(s);
        const crtez = tocanCrtez(s);
        const dio = (t) => crtez.filter((c) => c.tijelo === t).map((c) => `${sto(karte[c.id])}: ${smjerRijecima(c.kut)}`).join("; ");
        const sile = tijela.length > 1 ? tijela.map((ime2, t) => `${ime2}: ${dio(t)}`).join(" · ") : dio(0);
        const odnosi = (s.odnosi ?? []).map(
          (r) => `$${karte[r.a].oznaka}$ ${OP_RIJEC[r.op]} $${karte[r.b].oznaka}$`
        );
        return [sile, ...odnosi].join("; ");
      }
      return "";
    }
  };
  function validateNacrtaj(s, need) {
    const sile = fizikaSila(s).sile;
    const ids = sile.map((f2) => f2.id);
    if (!need(
      Array.isArray(s.ponudjene) && s.ponudjene.length >= 2 && s.ponudjene.length <= 8,
      "nacrtaj treba ponudjene: 2–8 kartica sila"
    ))
      return;
    const pon = s.ponudjene;
    need(
      new Set(pon.map((c) => c?.id)).size === pon.length,
      "id-jevi kartica moraju biti različiti"
    );
    pon.forEach((c, i) => {
      if (!need(
        c && typeof c === "object" && isText(c.id),
        `ponudjene[${i}] treba id`
      ))
        return;
      if (c.lazna) {
        need(c.lazna === true, `ponudjene[${i}].lazna mora biti true`);
        need(
          !ids.includes(c.id),
          `ponudjene[${i}] je označena kao lažna, a „${c.id}“ je prava sila scene`
        );
        need(
          isText(c.izvor),
          `lažna kartica ponudjene[${i}] treba „izvor“ (npr. „sila kojom sanduk stoji“)`
        );
        if (c.poruka != null)
          need(isText(c.poruka), `ponudjene[${i}].poruka mora biti tekst`);
      } else {
        need(
          ids.includes(c.id),
          `ponudjene[${i}].id „${c.id}“ nije sila ove scene (sile scene: ${ids.join(", ")}); lažnu silu označi s lazna: true`
        );
        need(
          c.smjer == null && c.iznos == null,
          `ponudjene[${i}]: smjer i iznos prave sile računa aplikacija iz scene, ne pišu se`
        );
      }
      if (c.oznaka != null)
        need(isText(c.oznaka), `ponudjene[${i}].oznaka mora biti tekst`);
      if (c.izvor != null)
        need(isText(c.izvor), `ponudjene[${i}].izvor mora biti tekst`);
    });
    need(lazneKarte(s).length <= 3, "najviše 3 lažne kartice");
    need(praveKarte(s).length >= 1, "nacrtaj treba barem jednu pravu silu");
    for (const f2 of sile)
      need(
        pon.some((c) => c?.id === f2.id && !c.lazna),
        `sila scene ${f2.id} (${f2.izvor}) nije među ponuđenima, a djeluje na tijelo: dodaj je (crtež bez nje ne bi bio potpun)`
      );
    if (s.odnosi != null && need(
      Array.isArray(s.odnosi) && s.odnosi.length <= 3,
      "odnosi mora biti popis od najviše 3 odnosa"
    )) {
      const F = Object.fromEntries(sile.map((f2) => [f2.id, f2.iznos]));
      s.odnosi.forEach((r, i) => {
        if (!need(
          r && r.a in F && r.b in F && r.a !== r.b,
          `odnosi[${i}]: a i b moraju biti dvije različite prave sile scene (${ids.join(", ")})`
        ))
          return;
        if (!need(
          [">", "<", "="].includes(r.op),
          `odnosi[${i}].op mora biti ">", "<" ili "="`
        ))
          return;
        const d = (F[r.a] - F[r.b]) / Math.max(F[r.a], F[r.b]);
        const fiz = Math.abs(d) <= 0.01 ? "=" : d > 0 ? ">" : "<";
        need(
          fiz === r.op,
          `odnosi[${i}]: po fizici scene ${r.a} = ${formatNumber(Math.round(F[r.a] * 10) / 10)} N i ${r.b} = ${formatNumber(Math.round(F[r.b] * 10) / 10)} N, pa je ${r.a} ${fiz} ${r.b}, a ne ${r.op}`
        );
        if (r.poruka != null)
          need(isText(r.poruka), `odnosi[${i}].poruka mora biti tekst`);
      });
    }
  }

  // src/lib/lesson/katalog/fizika/vektori.js
  var NACINI_MREZA = ["istrazi", "zbroji", "rastavi", "razlika"];
  var NACINI_RAD = ["istrazi", "postavi"];
  var PRIKAZI = ["iznos", "komponente", "zbroj", "put"];
  var NAJVISE_POLJA = 25;
  var tocka = (t) => Array.isArray(t) && t.length === 2 && t.every(isNum);
  var okruglo = (x) => Math.round(x * 1e9) / 1e9;
  var duljina = ([x, y]) => Math.hypot(x, y);
  var plus = (a, b) => [okruglo(a[0] + b[0]), okruglo(a[1] + b[1])];
  var minus = (a, b) => [okruglo(a[0] - b[0]), okruglo(a[1] - b[1])];
  function krajevi(s) {
    const out = [];
    (s.vektori ?? []).forEach((v, i) => {
      const od = tocka(v.od) ? v.od : i > 0 ? out[i - 1].do : [0, 0];
      const d = tocka(v.do) ? minus(v.do, od) : [
        okruglo(v.iznos * Math.cos(rad(v.kut))),
        okruglo(v.iznos * Math.sin(rad(v.kut)))
      ];
      out.push({ ...v, od, do: plus(od, d), d });
    });
    return out;
  }
  var zbrojVektora = (s) => krajevi(s).reduce((z, v) => plus(z, v.d), [0, 0]);
  var pocetakZbroja = (s) => tocka(s.pocetak) ? s.pocetak : krajevi(s)[0]?.od ?? [0, 0];
  var vrhZbroja = (s) => plus(pocetakZbroja(s), zbrojVektora(s));
  var pocetakRazlike = (s) => tocka(s.pocetak) ? s.pocetak : krajevi(s)[0]?.od ?? [0, 0];
  var deltaV = (s) => {
    const [v1, v2] = krajevi(s);
    return minus(v2.d, v1.d);
  };
  var komponente = (s) => krajevi(s)[0].d;
  var korakMreze = (s) => s.mreza.korak;
  var naMrezi2 = (v, k) => Math.abs(v / k - Math.round(v / k)) < 1e-6;
  var unutra = (s, [x, y]) => x >= s.mreza.x[0] - 1e-9 && x <= s.mreza.x[1] + 1e-9 && y >= s.mreza.y[0] - 1e-9 && y <= s.mreza.y[1] + 1e-9;
  var jed = (s) => s.scena === "rad" ? "N" : s.mreza?.jedinica ?? "";
  var tolRastav = (s) => isNum(s.tolerancija) ? s.tolerancija : 0.04;
  var tolRazlika = (s) => isNum(s.tolerancija) ? s.tolerancija : 0.1;
  var radVelicine = (s, kut) => {
    const F = s.F;
    const W2 = F * s.s * Math.cos(rad(kut));
    return {
      kut,
      W: Math.abs(W2) < 1e-9 ? 0 : W2,
      F_s: Math.abs(W2) < 1e-9 ? 0 : F * Math.cos(rad(kut)),
      F_n: F * Math.sin(rad(kut))
    };
  };
  var RAD_META = {
    W: { naziv: "rad sile", oznaka: "W", jedinica: "J" },
    F_s: {
      naziv: "komponenta sile u smjeru pomaka",
      oznaka: "F_s",
      jedinica: "N"
    },
    F_n: {
      naziv: "komponenta sile okomito na pomak",
      oznaka: "F_⊥",
      jedinica: "N"
    },
    kut: { naziv: "kut između sile i pomaka", oznaka: "α", jedinica: "°" }
  };
  var korakKuta = (s) => isNum(s.korak_kuta) ? s.korak_kuta : 5;
  var radKlizac = (s) => ({
    id: "kut",
    oznaka: "kut sile",
    min: 0,
    max: 180,
    korak: korakKuta(s),
    pocetak: s.kut,
    jedinica: "°"
  });
  var radAnaliza = (s) => analizaCilja(
    { ...s, klizaci: [radKlizac(s)] },
    radKlizac(s),
    (p) => radVelicine(s, p.kut)
  );
  function checkZbroji(s, tip) {
    if (!tocka(tip))
      return { tocno: false, poruka: "Povuci vrh strelice.", prazno: true };
    const od = pocetakZbroja(s);
    if (near(tip[0], od[0], 1e-6) && near(tip[1], od[1], 1e-6))
      return {
        tocno: false,
        poruka: "Povuci vrh strelice iz početne točke.",
        prazno: true
      };
    const cilj = vrhZbroja(s);
    const t = korakMreze(s) * 0.25;
    const isti2 = (a, b) => near(a[0], b[0], t) && near(a[1], b[1], t);
    if (isti2(tip, cilj)) return { tocno: true };
    const v = krajevi(s);
    let z = od;
    for (let i = 0; i < v.length - 1; i++) {
      z = plus(z, v[i].d);
      if (isti2(tip, z))
        return {
          tocno: false,
          poruka: `Strelica završava na kraju ${i + 1}. strelice. Ukupni pomak (zbroj) ide od početka do vrha **zadnje** strelice.`
        };
    }
    if (isti2(tip, minus(od, zbrojVektora(s))))
      return {
        tocno: false,
        poruka: "Strelica je dobre duljine, ali pokazuje obrnuto: zbroj ide od početka prve strelice prema vrhu zadnje."
      };
    const put = v.reduce((x, w) => x + duljina(w.d), 0);
    if (v.length > 1 && near(duljina(minus(tip, od)), put, t))
      return {
        tocno: false,
        poruka: "Duljina tvoje strelice jednaka je zbroju duljina zadanih strelica. Vektori se ne zbrajaju kao brojevi: zbroj je ravna strelica od početka prve do vrha zadnje (nadovezane) strelice."
      };
    return {
      tocno: false,
      poruka: s.savjet || "Vrh nije na pravom mjestu. Nadoveži strelice jednu na drugu: zbroj ide od početka prve do vrha zadnje."
    };
  }
  function checkRastavi(s, odg) {
    if (!tocka(odg) || odg[0] === 0 && odg[1] === 0)
      return {
        tocno: false,
        poruka: "Povuci vrhove obiju komponenti.",
        prazno: true
      };
    const [fx2, fy2] = komponente(s);
    const F = duljina([fx2, fy2]);
    const t = tolRastav(s) * F;
    const [x, y] = odg;
    if (near(x, fx2, t) && near(y, fy2, t)) return { tocno: true };
    if (Math.abs(fx2 - fy2) > t && near(x, fy2, t) && near(y, fx2, t))
      return {
        tocno: false,
        poruka: "Komponente su zamijenjene. Vodoravna komponenta je projekcija sile na vodoravnu os: iz vrha sile spusti okomicu na tu os."
      };
    const os = (ime2, v, tocna) => {
      if (near(v, tocna, t)) return null;
      if (Math.abs(tocna) > t && near(v, -tocna, t))
        return `${ime2} komponenta pokazuje na krivu stranu.`;
      if (Math.abs(tocna) <= t)
        return `Sila nema ${ime2.toLowerCase()} komponentu (sila je ${ime2 === "Vodoravna" ? "okomita" : "vodoravna"}).`;
      return `${ime2} komponenta je ${Math.abs(v) > Math.abs(tocna) ? "preduga" : "prekratka"}.`;
    };
    const msg = [os("Vodoravna", x, fx2), os("Okomita", y, fy2)].filter(Boolean).join(" ");
    return {
      tocno: false,
      poruka: `${msg} Komponenta završava ondje gdje okomica iz vrha sile pada na os.`
    };
  }
  function checkRazlika(s, odg) {
    if (!tocka(odg) || odg[0] === 0 && odg[1] === 0)
      return { tocno: false, poruka: "Povuci vrh strelice Δv.", prazno: true };
    const dv = deltaV(s);
    const [v1, v2] = krajevi(s);
    const isto = (a, b) => duljina(b) > 0 && razlikaKuta(kutVektora(...a), kutVektora(...b)) <= 10 && Math.abs(duljina(a) - duljina(b)) <= tolRazlika(s) * duljina(b);
    if (isto(odg, dv)) return { tocno: true };
    if (isto(odg, minus(v1.d, v2.d)))
      return {
        tocno: false,
        poruka: "To je v₁ − v₂: strelica je obrnuta. Δv = v₂ − v₁ ide od vrha v₁ do vrha v₂ (kad obje kreću iz iste točke)."
      };
    if (isto(odg, plus(v1.d, v2.d)))
      return {
        tocno: false,
        poruka: "To je zbroj v₁ + v₂. Za razliku Δv = v₂ − v₁ strelica ide od vrha v₁ do vrha v₂."
      };
    if (isto(odg, v2.d))
      return {
        tocno: false,
        poruka: "To je sama v₂. Δv je promjena brzine: strelica od vrha v₁ do vrha v₂."
      };
    return {
      tocno: false,
      poruka: s.savjet || "Nije dobro. Obje brzine kreću iz iste točke; Δv je strelica koja spaja vrh v₁ s vrhom v₂."
    };
  }
  function checkRad(s, kut) {
    if (!isNum(kut))
      return { tocno: false, poruka: "Okreni silu.", prazno: true };
    const a = radAnaliza(s);
    if (a.ok(kut)) return { tocno: true };
    const W2 = radVelicine(s, kut).W;
    const cilj = s.cilj.vrijednost;
    const tol2 = a.tol;
    const sin = s.F * s.s * Math.sin(rad(kut));
    let savjet3 = "";
    if (Math.abs(cilj) > tol2 && near(W2, -cilj, tol2))
      savjet3 = "Iznos je dobar, ali predznak nije: sila koja ima komponentu unatrag (protiv pomaka) obavlja negativan rad.";
    else if (near(sin, cilj, tol2))
      savjet3 = "Izgleda da si računao sa sinusom. U rad ulazi samo komponenta sile u smjeru pomaka, $F \\cos\\alpha$.";
    else
      savjet3 = Math.abs(W2) > Math.abs(cilj) ? "Komponenta sile u smjeru pomaka je prevelika." : "Komponenta sile u smjeru pomaka je premala.";
    return {
      tocno: false,
      poruka: `Pod kutom ${formatNumber(kut)}° rad je ${opisVelicine(W2, RAD_META.W)}, a treba ${iznosJed(cilj, "J")}. ${savjet3}`
    };
  }
  var OPIS2 = `Vektori na mreži ili sila pod kutom (fizika, matura). tekst, nacin, scena? ("mreza" zadano | "rad").
Scena "mreza": mreza: {x: [od, do], y: [od, do], korak, jedinica?} (isto mjerilo na osima, najviše ${NAJVISE_POLJA} kvadratića po osi), strane_svijeta? (true: S, J, I, Z umjesto osi x, y),
vektori: [{od? ([x, y]; bez „od“ vektor se nadovezuje na vrh prethodnog), do ([x, y]) ili iznos + kut (° od +x, suprotno kazaljci), oznaka?, pomicno? (istrazi: vrh se vuče)}]. Načini:
- "istrazi": korak za gledanje: vuku se vrhovi pomičnih vektora ili točke putanja?: [[x, y], …] (2–6 točaka); prikazi?: ${PRIKAZI.join(", ")} (iznos, komponente, zbroj kao strelica, put naspram pomaka). Bez odgovora.
- "zbroji" (Tvoj red): učenik vuče vrh strelice zbroja iz pocetak? (zadano početak prvog vektora); točan vrh = početak + zbroj (računa aplikacija, mora biti na mreži); iznos se dok vuče ne vidi. Tipične greške (kraj prve strelice, obrnut smjer, zbroj iznosa) prepoznaju se same.
- "rastavi" (Tvoj red): jedan kosi vektor (iznos + kut); učenik vuče vrhove vodoravne i okomite komponente (zaključavanje na desetinu kvadratića); tolerancija? (udio iznosa, zadano 0,04).
- "razlika" (Tvoj red): dva vektora v₁, v₂ (npr. brzine u dvije točke kružnice); aplikacija ih prenese u zajednički pocetak? (zadano početak v₁), a učenik crta Δv = v₂ − v₁ iz vrha v₁; točno uz kut ±10° i iznos ±tolerancija (zadano 0,1).
Scena "rad": sanduk se pomakne za s (m) udesno, sila F (N) vuče ga pod kutom kut (°, 0–180, početni položaj); vidi se F·cos α i rad W = F·s·cos α (zelen pozitivan, crven negativan). korak_kuta? (zadano 5). Načini:
- "istrazi": učenik okreće silu i gleda komponentu i rad;
- "postavi" (Tvoj red): cilj: {velicina: "W", vrijednost (J), tolerancija?}; učenik okrene silu tako da rad bude cilj (npr. 0 J kod 90°). Kut koji to ostvaruje računa aplikacija.`;
  var PRIMJER2 = {
    tip: "vektori",
    nacin: "zbroji",
    strane_svijeta: true,
    tekst: "Biciklist vozi 30 km na istok, pa 40 km na jug. Povuci strelicu **pomaka**: od točke O do mjesta gdje je stao.",
    mreza: { x: [-10, 50], y: [-50, 10], korak: 10, jedinica: "km" },
    vektori: [
      { od: [0, 0], do: [30, 0], oznaka: "30 km" },
      { do: [30, -40], oznaka: "40 km" }
    ]
  };
  var PRIMJERI2 = [
    {
      tip: "vektori",
      nacin: "istrazi",
      strane_svijeta: true,
      tekst: "Povuci točke puta. Gledaj kako se mijenjaju **put** i **pomak**; probaj se vratiti u točku O.",
      mreza: { x: [-10, 50], y: [-50, 10], korak: 10, jedinica: "km" },
      putanja: [
        [0, 0],
        [30, 0],
        [30, -40]
      ],
      prikazi: ["put"]
    },
    {
      tip: "vektori",
      nacin: "rastavi",
      tekst: "Uže vuče sanduk silom 200 N pod kutom 30°. Povuci vrhove **vodoravne** i **okomite** komponente sile.",
      mreza: { x: [-25, 225], y: [-25, 125], korak: 25, jedinica: "N" },
      vektori: [{ od: [0, 0], iznos: 200, kut: 30, oznaka: "F = 200 N" }]
    },
    {
      tip: "vektori",
      nacin: "razlika",
      tekst: "Točka na kotaču ima brzinu $v_1$, a četvrt okreta kasnije brzinu $v_2$ (isti iznos). Nacrtaj promjenu brzine $\\Delta v = v_2 - v_1$.",
      mreza: { x: [-4, 4], y: [-4, 4], korak: 1, jedinica: "m/s" },
      vektori: [
        { od: [0, 0], do: [0, 3], oznaka: "v₁" },
        { od: [0, 0], do: [-3, 0], oznaka: "v₂" }
      ]
    },
    {
      tip: "vektori",
      nacin: "istrazi",
      tekst: "Povuci vrhove vektora $\\vec a$ i $\\vec b$ i gledaj njihov **zbroj** i komponente.",
      mreza: { x: [-2, 8], y: [-2, 6], korak: 1 },
      vektori: [
        { od: [0, 0], do: [3, 1], oznaka: "a", pomicno: true },
        { do: [5, 4], oznaka: "b", pomicno: true }
      ],
      prikazi: ["zbroj", "komponente"]
    },
    {
      tip: "vektori",
      scena: "rad",
      nacin: "istrazi",
      F: 80,
      s: 10,
      kut: 30,
      tekst: "Sanduk se pomakne 10 m udesno. **Okreći silu** i gledaj koliki dio sile vuče naprijed i koliki je rad."
    },
    {
      tip: "vektori",
      scena: "rad",
      nacin: "postavi",
      F: 80,
      s: 10,
      kut: 30,
      tekst: "Okreni silu tako da **ne obavlja rad**, iako se sanduk pomiče.",
      cilj: { velicina: "W", vrijednost: 0 }
    }
  ];
  var vektori = {
    tip: "vektori",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: OPIS2,
    primjer: PRIMJER2,
    primjeri: PRIMJERI2,
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "vektori treba „tekst“");
      if (s.scena === "rad") {
        validateRad(s, need);
        return list;
      }
      if (!need(
        s.scena == null || s.scena === "mreza",
        'vektori.scena mora biti "mreza" (zadano) ili "rad"'
      ))
        return list;
      if (!need(
        NACINI_MREZA.includes(s.nacin),
        `vektori.nacin mora biti jedan od: ${NACINI_MREZA.join(", ")}`
      ))
        return list;
      const m = s.mreza;
      if (!need(
        m && Array.isArray(m.x) && Array.isArray(m.y) && m.x.length === 2 && m.y.length === 2 && [...m.x, ...m.y].every(isNum) && m.x[1] > m.x[0] && m.y[1] > m.y[0],
        "vektori treba mreza: {x: [od, do], y: [od, do], korak} uz od < do"
      ))
        return list;
      if (!need(
        isNum(m.korak) && m.korak > 0,
        "mreza.korak mora biti pozitivan broj"
      ))
        return list;
      need(
        (m.x[1] - m.x[0]) / m.korak <= NAJVISE_POLJA && (m.y[1] - m.y[0]) / m.korak <= NAJVISE_POLJA,
        `mreza ima previše kvadratića (najviše ${NAJVISE_POLJA} po osi): povećaj korak ili smanji raspon`
      );
      need(
        (m.x[1] - m.x[0]) / m.korak >= 2 && (m.y[1] - m.y[0]) / m.korak >= 2,
        "mreza treba barem 2 kvadratića po osi"
      );
      need(
        [...m.x, ...m.y].every((v) => naMrezi2(v, m.korak)),
        "rubovi mreže (x i y) moraju biti višekratnici koraka"
      );
      if (m.jedinica != null)
        need(typeof m.jedinica === "string", "mreza.jedinica mora biti tekst");
      if (s.strane_svijeta != null)
        need(
          typeof s.strane_svijeta === "boolean",
          "strane_svijeta mora biti true ili false"
        );
      if (s.pocetak != null)
        need(
          tocka(s.pocetak) && unutra(s, s.pocetak),
          "pocetak mora biti točka [x, y] na mreži"
        );
      const vs = s.vektori;
      const trebaVektora = {
        istrazi: [0, 4],
        zbroji: [1, 4],
        rastavi: [1, 1],
        razlika: [2, 2]
      }[s.nacin];
      if (vs != null || trebaVektora[0] > 0) {
        if (!need(
          Array.isArray(vs) && vs.length >= trebaVektora[0] && vs.length <= trebaVektora[1],
          `vektori (${s.nacin}) treba ${trebaVektora[0] === trebaVektora[1] ? trebaVektora[0] : `${trebaVektora[0]}–${trebaVektora[1]}`} vektor(a)`
        ))
          return list;
        vs.forEach((v, i) => {
          if (!need(v && typeof v === "object", `vektori[${i}] mora biti objekt`))
            return;
          if (v.od != null)
            need(tocka(v.od), `vektori[${i}].od mora biti [x, y]`);
          need(i > 0 || tocka(v.od), "prvi vektor treba „od“ ([x, y])");
          need(
            tocka(v.do) !== (isNum(v.iznos) && isNum(v.kut)),
            `vektori[${i}] treba ili „do“ ([x, y]) ili „iznos“ i „kut“`
          );
          if (isNum(v.iznos))
            need(v.iznos > 0, `vektori[${i}].iznos mora biti pozitivan`);
          if (v.oznaka != null)
            need(isText(v.oznaka), `vektori[${i}].oznaka mora biti tekst`);
          if (v.pomicno != null)
            need(
              s.nacin === "istrazi" && typeof v.pomicno === "boolean",
              `vektori[${i}].pomicno postoji samo u načinu istrazi`
            );
        });
        if (list.length) return list;
        const k = krajevi(s);
        k.forEach((v, i) => {
          need(
            unutra(s, v.od) && unutra(s, v.do),
            `vektori[${i}] izlazi iz mreže`
          );
          need(duljina(v.d) > 0, `vektori[${i}] ima duljinu 0`);
          if (v.pomicno || s.nacin === "zbroji")
            need(
              [...v.od, ...v.do].every((x) => naMrezi2(x, m.korak)),
              `vektori[${i}]: krajevi moraju biti na čvorovima mreže (višekratnici koraka)`
            );
        });
      }
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        need(s.tolerancija == null, "istrazi nema toleranciju");
        if (s.putanja != null && need(
          Array.isArray(s.putanja) && s.putanja.length >= 2 && s.putanja.length <= 6 && s.putanja.every(tocka),
          "putanja mora biti popis od 2 do 6 točaka [x, y]"
        )) {
          need(
            s.putanja.every(
              (t) => unutra(s, t) && t.every((x) => naMrezi2(x, m.korak))
            ),
            "točke putanje moraju biti na čvorovima mreže"
          );
        }
        need(
          (vs?.length ?? 0) > 0 || s.putanja != null,
          "istrazi treba vektore ili putanju"
        );
        if (s.prikazi != null && need(
          Array.isArray(s.prikazi) && s.prikazi.every((p) => PRIKAZI.includes(p)),
          `prikazi smije sadržavati: ${PRIKAZI.join(", ")}`
        )) {
          need(
            !s.prikazi.includes("put") || s.putanja != null,
            "prikazi „put“ treba putanju"
          );
          need(
            !s.prikazi.includes("zbroj") || (vs?.length ?? 0) >= 2,
            "prikazi „zbroj“ treba barem 2 vektora"
          );
        }
        return list;
      }
      need(
        s.putanja == null && s.prikazi == null,
        `${s.nacin} nema putanju ni prikazi (to je za istrazi)`
      );
      if (s.nacin === "zbroji") {
        const vrh = vrhZbroja(s);
        need(unutra(s, vrh), "vrh zbroja izlazi iz mreže: proširi mrežu");
        need(
          vrh.every((x) => naMrezi2(x, m.korak)),
          "vrh zbroja mora biti na čvoru mreže"
        );
        need(
          duljina(zbrojVektora(s)) > 0,
          "zbroj vektora je nula: učenik ne bi imao što nacrtati"
        );
        if (s.tolerancija != null)
          need(
            false,
            "zbroji nema toleranciju (vrh se zaključava na čvor mreže)"
          );
      }
      if (s.nacin === "rastavi") {
        if (s.tolerancija != null)
          need(
            isNum(s.tolerancija) && s.tolerancija > 0 && s.tolerancija <= 0.2,
            "tolerancija je udio iznosa sile (npr. 0.04)"
          );
        const v = krajevi(s)[0];
        need(
          unutra(s, [v.od[0] + v.d[0], v.od[1]]) && unutra(s, [v.od[0], v.od[1] + v.d[1]]),
          "komponente izlaze iz mreže"
        );
        need(
          Math.abs(v.d[0]) > 1e-9 && Math.abs(v.d[1]) > 1e-9,
          "vektor za rastav mora biti kos (obje komponente različite od nule)"
        );
      }
      if (s.nacin === "razlika") {
        if (s.tolerancija != null)
          need(
            isNum(s.tolerancija) && s.tolerancija > 0 && s.tolerancija <= 0.3,
            "tolerancija je udio iznosa Δv (npr. 0.1)"
          );
        const p0 = pocetakRazlike(s);
        const [v1, v2] = krajevi(s);
        need(duljina(deltaV(s)) > 0, "v₁ i v₂ su jednaki: Δv je nula");
        need(
          unutra(s, plus(p0, v1.d)) && unutra(s, plus(p0, v2.d)),
          "preneseni v₁ i v₂ (iz zajedničkog početka) izlaze iz mreže: promijeni pocetak ili mrežu"
        );
        need(
          [...plus(p0, v1.d), ...plus(p0, v2.d)].every(
            (x) => naMrezi2(x, m.korak / 2)
          ),
          "vrhovi prenesenih v₁ i v₂ moraju biti na mreži (pola koraka)"
        );
      }
      return list;
    },
    check(s, odg) {
      if (s.scena === "rad")
        return s.nacin === "postavi" ? checkRad(s, odg) : { tocno: false, poruka: "Ovaj korak nema provjeru." };
      if (s.nacin === "zbroji") return checkZbroji(s, odg);
      if (s.nacin === "rastavi") return checkRastavi(s, odg);
      if (s.nacin === "razlika") return checkRazlika(s, odg);
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      const j = jed(s);
      if (s.scena === "rad")
        return s.nacin === "postavi" ? `α = ${formatNumber(radAnaliza(s).best)}°` : "";
      if (s.nacin === "zbroji") {
        const z = zbrojVektora(s);
        return `strelica od (${s.pocetak ? pocetakZbroja(s).map(formatNumber).join(", ") : "početka"}) do (${vrhZbroja(s).map(formatNumber).join(", ")}), iznos ${iznosJed(duljina(z), j)}`;
      }
      if (s.nacin === "rastavi") {
        const [x, y] = komponente(s);
        return `vodoravna ${iznosJed(x, j)}, okomita ${iznosJed(y, j)}`;
      }
      if (s.nacin === "razlika") {
        const d = deltaV(s);
        return `$\\Delta v$ od vrha $v_1$ do vrha $v_2$: (${d.map(formatNumber).join(", ")}), iznos ${iznosJed(duljina(d), j)}`;
      }
      return "";
    }
  };
  function validateRad(s, need) {
    need(
      NACINI_RAD.includes(s.nacin),
      `vektori (scena rad): nacin mora biti ${NACINI_RAD.join(" ili ")}`
    );
    need(
      isNum(s.F) && s.F > 0 && s.F <= 1e6,
      "rad treba F (iznos sile u N, pozitivan)"
    );
    need(
      isNum(s.s) && s.s > 0 && s.s <= 1e6,
      "rad treba s (pomak u m, pozitivan)"
    );
    need(
      isNum(s.kut) && s.kut >= 0 && s.kut <= 180,
      "rad treba kut (početni kut sile, 0–180°)"
    );
    if (s.korak_kuta != null)
      need(
        isNum(s.korak_kuta) && [1, 5, 10, 15].includes(s.korak_kuta),
        "korak_kuta mora biti 1, 5, 10 ili 15"
      );
    for (const f2 of ["mreza", "vektori", "putanja", "prikazi", "klizaci"])
      need(!(f2 in s), `scena rad nema polje „${f2}“`);
    if (!isNum(s.F) || !isNum(s.s) || !isNum(s.kut)) return;
    need(
      Math.abs(s.kut / korakKuta(s) - Math.round(s.kut / korakKuta(s))) < 1e-9,
      "kut mora biti višekratnik koraka kuta"
    );
    if (s.nacin === "istrazi") return noAnswerFields(s, need);
    const c = s.cilj;
    if (!need(
      c && c.velicina === "W" && isNum(c.vrijednost),
      'rad postavi treba cilj: {velicina: "W", vrijednost (J)}'
    ))
      return;
    if (c.tolerancija != null)
      need(
        isNum(c.tolerancija) && c.tolerancija >= 0,
        "cilj.tolerancija mora biti nenegativan broj (J)"
      );
    need(
      Math.abs(c.vrijednost) <= s.F * s.s + 1e-9,
      `rad ne može biti veći od F·s = ${broj3(s.F * s.s)} J`
    );
    const a = radAnaliza(s);
    need(
      a.dostizno,
      `cilj W = ${broj3(c.vrijednost)} J nije ostvariv uz korak kuta ${korakKuta(s)}° (najbliže ${formatNumber(a.best)}°)`
    );
    need(
      !a.ok(s.kut),
      "početni kut već daje ciljani rad, pa bi „Provjeri“ bez okretanja bio točan"
    );
  }

  // src/lib/lesson/katalog/fizika/kruzenje.js
  var SCENE2 = ["kotac", "zavoj", "orbita"];
  var NACINI4 = ["istrazi", "postavi", "smjer"];
  var PITANJA = ["brzina", "akceleracija", "sila", "izlijetanje"];
  var VRTNJA = ["suprotno_kazaljci", "kao_kazaljka"];
  var TOCKE = ["A", "B"];
  var tockeKotaca = (s) => Array.isArray(s.tocke) ? s.tocke : [1, 0.5];
  var predznakVrtnje = (s) => s.smjer_vrtnje === "kao_kazaljka" ? -1 : 1;
  function fizikaKruzenja(s, p = pocetneVrijednosti2(s)) {
    const g = isNum(s.g) ? s.g : G_ZEMLJA;
    if (s.scena === "kotac") {
      const r = iznos2(s, s.r, p);
      const f2 = iznos2(s, s.f, p);
      const omega = 2 * Math.PI * f2;
      const vel = { r, f: f2, n: f2 * 60, T: f2 > 0 ? 1 / f2 : Infinity, omega };
      tockeKotaca(s).forEach((k, i) => {
        vel[`v_${TOCKE[i]}`] = omega * r * k;
        vel[`a_${TOCKE[i]}`] = omega * omega * r * k;
      });
      return { vel, omega, r };
    }
    if (s.scena === "zavoj") {
      const r = iznos2(s, s.r, p);
      const m = iznos2(s, s.masa, p);
      const v = iznos2(s, s.v, p);
      const Fmax = s.F_max != null ? iznos2(s, s.F_max, p) : iznos2(s, s.mi, p) * m * g;
      const Fcp = m * v * v / r;
      const vel = {
        r,
        m,
        v,
        a_cp: v * v / r,
        F_cp: Fcp,
        F_max: Fmax,
        v_max: Math.sqrt(Fmax * r / m),
        omega: v / r,
        T: 2 * Math.PI * r / v
      };
      return { vel, omega: v / r, r, prolazi: Fcp <= Fmax * (1 + 1e-9) };
    }
    if (s.scena === "orbita") {
      const M = iznos2(s, s.M, p);
      const R2 = iznos2(s, s.R, p);
      const h = iznos2(s, s.h, p);
      const r = R2 + h;
      const v = Math.sqrt(GRAV_KONST * M / r);
      const vel = {
        h,
        r,
        v,
        T: 2 * Math.PI * r / v,
        a: GRAV_KONST * M / (r * r),
        g0: GRAV_KONST * M / (R2 * R2),
        omega: v / r
      };
      if (s.masa != null) vel.F = iznos2(s, s.masa, p) * vel.a;
      return { vel, omega: v / r, r, R: R2 };
    }
    return { vel: {} };
  }
  function velicineKruzenja(s) {
    if (s.scena === "kotac") {
      const out = {
        r: { naziv: "polumjer kotača", oznaka: "r", jedinica: "m" },
        f: { naziv: "frekvencija", oznaka: "f", jedinica: "Hz" },
        n: { naziv: "broj okretaja u minuti", oznaka: "n", jedinica: "okr/min" },
        T: { naziv: "period", oznaka: "T", jedinica: "s" },
        omega: { naziv: "kutna brzina", oznaka: "ω", jedinica: "rad/s" }
      };
      tockeKotaca(s).forEach((k, i) => {
        const t = TOCKE[i];
        const gdje = k === 1 ? "na rubu" : `na ${formatNumber(k)} polumjera`;
        out[`v_${t}`] = {
          naziv: `obodna brzina točke ${t} (${gdje})`,
          oznaka: `v_${t}`,
          jedinica: "m/s"
        };
        out[`a_${t}`] = {
          naziv: `centripetalna akceleracija točke ${t}`,
          oznaka: `a_${t}`,
          jedinica: "m/s²"
        };
      });
      return out;
    }
    if (s.scena === "zavoj")
      return {
        r: { naziv: "polumjer zavoja", oznaka: "r", jedinica: "m" },
        m: { naziv: "masa auta", oznaka: "m", jedinica: "kg" },
        v: { naziv: "brzina", oznaka: "v", jedinica: "m/s" },
        a_cp: {
          naziv: "centripetalna akceleracija",
          oznaka: "a_{cp}",
          jedinica: "m/s²"
        },
        F_cp: {
          naziv: "potrebna centripetalna sila",
          oznaka: "F_{cp}",
          jedinica: "N"
        },
        F_max: { naziv: "najveća sila trenja", oznaka: "F_{max}", jedinica: "N" },
        v_max: {
          naziv: "najveća brzina u zavoju",
          oznaka: "v_{max}",
          jedinica: "m/s"
        },
        omega: { naziv: "kutna brzina", oznaka: "ω", jedinica: "rad/s" },
        T: { naziv: "vrijeme jednog kruga", oznaka: "T", jedinica: "s" }
      };
    if (s.scena === "orbita") {
      const out = {
        h: { naziv: "visina iznad površine", oznaka: "h", jedinica: "m" },
        r: { naziv: "polumjer orbite", oznaka: "r", jedinica: "m" },
        v: { naziv: "brzina satelita", oznaka: "v", jedinica: "m/s" },
        T: { naziv: "period ophoda", oznaka: "T", jedinica: "s" },
        a: {
          naziv: "akceleracija (gravitacijsko ubrzanje na orbiti)",
          oznaka: "a",
          jedinica: "m/s²"
        },
        g0: {
          naziv: "gravitacijsko ubrzanje na površini",
          oznaka: "g_0",
          jedinica: "m/s²"
        },
        omega: { naziv: "kutna brzina", oznaka: "ω", jedinica: "rad/s" }
      };
      if (s.masa != null)
        out.F = {
          naziv: "gravitacijska sila na satelit",
          oznaka: "F_g",
          jedinica: "N"
        };
      return out;
    }
    return {};
  }
  var tangenta = (s) => normKut(s.tocka + 90 * predznakVrtnje(s));
  var tocanSmjer = (s) => s.pitaj === "brzina" || s.pitaj === "izlijetanje" ? tangenta(s) : normKut(s.tocka + 180);
  var STO = {
    brzina: "brzina",
    akceleracija: "akceleracija",
    sila: "sila",
    izlijetanje: "smjer izlijetanja"
  };
  var TIJELO = { kotac: "točka", zavoj: "auto", orbita: "satelit" };
  function checkSmjer(s, kut) {
    if (!isNum(kut))
      return {
        tocno: false,
        poruka: "Okreni strelicu: povuci njezin vrh.",
        prazno: true
      };
    const tocan = tocanSmjer(s);
    if (razlikaKuta(kut, tocan) < 1) return { tocno: true };
    const van = s.tocka;
    const unutra2 = normKut(s.tocka + 180);
    const naprijed = tangenta(s);
    const natrag = normKut(naprijed + 180);
    const isti2 = (a) => razlikaKuta(kut, a) < 1;
    const tko = TIJELO[s.scena];
    const vrti = predznakVrtnje(s) > 0 ? "suprotno od kazaljke na satu" : "kao kazaljka na satu";
    if (isti2(van)) {
      if (s.pitaj === "izlijetanje")
        return {
          tocno: false,
          poruka: `To je česta zabluda: ${tko} ne odleti radijalno prema van. Kad nestane sile prema središtu, ništa ga više ne skreće, pa nastavi **ravno**, u smjeru brzine koju je imao: po tangenti.`
        };
      if (s.pitaj === "brzina")
        return {
          tocno: false,
          poruka: "Brzina ne gleda prema van. Pokazuje kamo se tijelo upravo giba, a to je tangenta na kružnicu."
        };
      return {
        tocno: false,
        poruka: "To je „centrifugalna“ zabluda: prema van ne djeluje nikakva sila. Da bi skretalo u krug, na tijelo mora djelovati sila **prema središtu**; bez nje bi otišlo ravno, po tangenti."
      };
    }
    if (isti2(natrag))
      return {
        tocno: false,
        poruka: `Pravac je dobar (tangenta), ali smjer je obrnut: tijelo se vrti ${vrti}, pa ${STO[s.pitaj]} gleda naprijed, u smjeru vrtnje.`
      };
    if (isti2(unutra2))
      return {
        tocno: false,
        poruka: s.pitaj === "izlijetanje" ? "Prema središtu ga je vukla sila dok je kružio. Kad te sile nema, tijelo nastavi ravno, u smjeru brzine: po tangenti." : "Prema središtu pokazuju akceleracija i sila. Brzina je uvijek tangenta na kružnicu, u smjeru vrtnje."
      };
    if (isti2(naprijed))
      return {
        tocno: false,
        poruka: `To je smjer brzine. Iznos brzine se ne mijenja, mijenja se njezin smjer, pa ${s.pitaj === "sila" ? "sila" : "akceleracija"} pokazuje **prema središtu** kružnice.`
      };
    return {
      tocno: false,
      poruka: s.savjet || (s.pitaj === "brzina" || s.pitaj === "izlijetanje" ? "Nije taj smjer. Brzina je tangenta na kružnicu u točki gdje je tijelo, u smjeru vrtnje." : "Nije taj smjer. Kod jednolikog kruženja akceleracija i sila pokazuju prema središtu kružnice.")
    };
  }
  var OPIS3 = `Kružno gibanje (fizika, matura). tekst, scena, nacin, g? (zadano 9,81), smjer_vrtnje? ("suprotno_kazaljci" zadano | "kao_kazaljka").
Scene (brojčano polje smije biti id klizača):
- "kotac": r (m), f (Hz; okretaje u minuti zadaj klizačem s jedinica "okr/min" i faktor 1/60), tocke? (udjeli polumjera za točke A i B, zadano [1, 0.5]). Veličine r, f, n, T, omega, v_A, a_A, v_B, a_B. Gumb „Pusti točku“: točka odleti po tangenti.
- "zavoj": auto odozgo na kružnoj cesti: r (m), masa (kg), v (m/s), F_max (N, najveća sila trenja) ili mi. Veličine r, m, v, a_cp, F_cp, F_max, v_max, omega, T. Kad je F_cp > F_max, auto klizne po tangenti.
- "orbita": satelit oko planeta: M (kg), R (m), h (visina u m; klizač u km uz faktor 1000), masa? (satelita, za silu). Veličine h, r, v, T, a, g0, omega (i F uz masu). Gumb „Isključi gravitaciju“: satelit ode po tangenti.
Načini:
- ${NACIN_ISTRAZI2}. ${KLIZACI_OPIS} (najviše 3); prikazi?: veličine koje se vide uživo (najviše 4).
- "postavi" (Tvoj red): točno jedan klizač; ${CILJ_OPIS}. Npr. najveća brzina u zavoju: {velicina: "F_cp", vrijednost: "F_max"}. krivo?: [{vrijednost (klizača), poruka}].
- "smjer" (Tvoj red): gibanje stane, tijelo je na tocka (kut položaja u °, višekratnik 22,5; 0 = desno od središta, 90 = iznad); pitaj: "brzina" | "akceleracija" | "sila" | "izlijetanje"; učenik okrene strelicu (16 smjerova). Točan smjer računa aplikacija; poruke za „prema van“ (centrifugalna zabluda), obrnutu tangentu i zamjenu brzine s akceleracijom dodaju se same. Bez klizača.`;
  var PRIMJER3 = {
    tip: "kruzenje",
    nacin: "postavi",
    scena: "zavoj",
    r: 50,
    masa: 1200,
    F_max: 9600,
    v: "v",
    tekst: "Najveća sila trenja koju gume mogu dati je 9600 N. Postavi **najveću** brzinu kojom auto još prolazi zavoj.",
    klizaci: [
      {
        id: "v",
        oznaka: "brzina auta",
        min: 5,
        max: 30,
        korak: 0.5,
        pocetak: 15,
        jedinica: "m/s"
      }
    ],
    prikazi: ["v", "F_cp", "F_max"],
    cilj: { velicina: "F_cp", vrijednost: "F_max" }
  };
  var PRIMJERI3 = [
    {
      tip: "kruzenje",
      nacin: "istrazi",
      scena: "kotac",
      r: 0.4,
      f: "n",
      tekst: "Kotač polumjera 0,4 m. **Mijenjaj broj okretaja** i usporedi točku na rubu (A) i točku na pola polumjera (B).",
      klizaci: [
        {
          id: "n",
          oznaka: "okretaja u minuti",
          min: 30,
          max: 600,
          korak: 30,
          pocetak: 300,
          jedinica: "okr/min",
          faktor: 1 / 60
        }
      ],
      prikazi: ["T", "omega", "v_A", "v_B"]
    },
    {
      tip: "kruzenje",
      nacin: "smjer",
      scena: "kotac",
      r: 0.4,
      f: 5,
      tocka: 45,
      pitaj: "brzina",
      tekst: "Kotač se vrti suprotno od kazaljke na satu. Okreni strelicu tako da pokazuje **brzinu** označene točke."
    },
    {
      tip: "kruzenje",
      nacin: "istrazi",
      scena: "zavoj",
      r: 50,
      masa: 1200,
      F_max: 9600,
      v: "v",
      tekst: "Auto u zavoju polumjera 50 m. **Povećavaj brzinu** i usporedi potrebnu centripetalnu silu s najvećim trenjem.",
      klizaci: [
        {
          id: "v",
          oznaka: "brzina auta",
          min: 5,
          max: 30,
          korak: 0.5,
          pocetak: 15,
          jedinica: "m/s"
        }
      ]
    },
    {
      tip: "kruzenje",
      nacin: "smjer",
      scena: "zavoj",
      r: 50,
      masa: 1200,
      F_max: 9600,
      v: 25,
      tocka: 90,
      pitaj: "izlijetanje",
      tekst: "Auto ulazi u zavoj prebrzo i trenje ga više ne može zadržati. Okreni strelicu: **kamo** će auto kliznuti?"
    },
    {
      tip: "kruzenje",
      nacin: "istrazi",
      scena: "orbita",
      M: 6e24,
      R: 637e4,
      h: "h",
      tekst: "Satelit kruži oko Zemlje. **Mijenjaj visinu** i gledaj brzinu i period. Probaj i isključiti gravitaciju.",
      klizaci: [
        {
          id: "h",
          oznaka: "visina orbite",
          min: 200,
          max: 2e3,
          korak: 100,
          pocetak: 400,
          jedinica: "km",
          faktor: 1e3
        }
      ],
      prikazi: ["h", "v", "T"]
    },
    {
      tip: "kruzenje",
      nacin: "smjer",
      scena: "orbita",
      M: 6e24,
      R: 637e4,
      h: 4e5,
      tocka: 135,
      pitaj: "sila",
      tekst: "Okreni strelicu tako da pokazuje smjer **sile** koja drži satelit u orbiti."
    }
  ];
  var kruzenje = {
    tip: "kruzenje",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: OPIS3,
    primjer: PRIMJER3,
    primjeri: PRIMJERI3,
    fizika: fizikaKruzenja,
    velicine: velicineKruzenja,
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI4.includes(s.nacin),
        `kruzenje.nacin mora biti jedan od: ${NACINI4.join(", ")}`
      );
      need(isText(s.tekst), "kruzenje treba „tekst“");
      if (!need(
        SCENE2.includes(s.scena),
        `kruzenje.scena mora biti jedna od: ${SCENE2.join(", ")}`
      ))
        return list;
      if (s.g != null)
        need(
          isNum(s.g) && s.g >= 1 && s.g <= 30,
          "g mora biti broj od 1 do 30 (m/s²)"
        );
      if (s.smjer_vrtnje != null)
        need(
          VRTNJA.includes(s.smjer_vrtnje),
          `smjer_vrtnje mora biti ${VRTNJA.join(" ili ")}`
        );
      if (s.nacin === "istrazi") validateKlizaci(s, need, { najvise: 3 });
      else if (s.nacin === "postavi")
        validateKlizaci(s, need, { najmanje: 1, najvise: 1 });
      else
        need(s.klizaci == null, "smjer nema klizače: sve vrijednosti su brojevi");
      const koristeni = [];
      const polje2 = (ime2, o) => {
        if (typeof s[ime2] === "string") koristeni.push(s[ime2]);
        return validatePolje(s, need, ime2, o);
      };
      const dozvoljena = {
        kotac: ["r", "f", "tocke"],
        zavoj: ["r", "masa", "v", "F_max", "mi"],
        orbita: ["M", "R", "h", "masa"]
      }[s.scena];
      for (const f2 of [
        "r",
        "f",
        "tocke",
        "masa",
        "v",
        "F_max",
        "mi",
        "M",
        "R",
        "h"
      ])
        need(
          !(f2 in s) || dozvoljena.includes(f2),
          `polje „${f2}“ ne postoji u sceni ${s.scena} (polja scene: ${dozvoljena.join(", ")})`
        );
      if (s.scena === "kotac") {
        polje2("r", { min: 1e-3, max: 1e3, opis: "polumjer u m" });
        polje2("f", { min: 1e-3, max: 1e3, opis: "frekvencija u Hz" });
        if (s.tocke != null)
          need(
            Array.isArray(s.tocke) && s.tocke.length >= 1 && s.tocke.length <= 2 && s.tocke.every((k) => isNum(k) && k > 0 && k <= 1) && new Set(s.tocke).size === s.tocke.length,
            "tocke mora biti 1–2 različita udjela polumjera iz (0, 1], npr. [1, 0.5]"
          );
      }
      if (s.scena === "zavoj") {
        polje2("r", { min: 1, max: 1e4, opis: "polumjer zavoja u m" });
        polje2("masa", { min: 1, max: 1e5, opis: "masa auta u kg" });
        polje2("v", { min: 0.1, max: 200, opis: "brzina u m/s" });
        need(
          s.F_max != null !== (s.mi != null),
          "zavoj treba točno jedno od: F_max (najveća sila trenja u N) ili mi (faktor trenja)"
        );
        if (s.F_max != null) polje2("F_max", { min: 1, max: 1e7 });
        if (s.mi != null) polje2("mi", { min: 0.01, max: 2 });
      }
      if (s.scena === "orbita") {
        polje2("M", { min: 1e15, max: 1e32, opis: "masa planeta u kg" });
        polje2("R", { min: 1e3, max: 1e9, opis: "polumjer planeta u m" });
        polje2("h", { min: 0, max: 1e9, opis: "visina orbite u m" });
        if (s.masa != null) polje2("masa", { min: 1e-3, max: 1e7 });
        if (isNum(s.R) && isNum(iznos2(s, s.h)))
          need(
            klizaciOd(s).every(
              (k) => k.id !== s.h || k.max * (k.faktor ?? 1) <= 5 * s.R
            ) && (typeof s.h === "string" || s.h <= 5 * s.R),
            "orbita: visina do 5 polumjera planeta (inače se planet na crtežu ne vidi)"
          );
      }
      if (list.length) return list;
      validateKoristeniKlizaci(s, need, koristeni);
      const meta = velicineKruzenja(s);
      if (s.prikazi != null && need(
        Array.isArray(s.prikazi) && s.prikazi.length >= 1 && s.prikazi.length <= 4,
        "prikazi mora biti popis od 1 do 4 veličine"
      )) {
        for (const v of s.prikazi)
          need(
            v in meta,
            `prikazi: „${v}“ nije veličina scene (${Object.keys(meta).join(", ")})`
          );
      }
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        need(
          s.pitaj == null && s.tocka == null,
          "istrazi nema pitaj ni tocka (to je za smjer)"
        );
      }
      if (s.nacin === "postavi") {
        need(
          s.pitaj == null && s.tocka == null,
          "postavi nema pitaj ni tocka (to je za smjer)"
        );
        const k = klizaciOd(s)[0];
        validateCilj(s, need, k, (p) => fizikaKruzenja(s, p).vel, meta);
        if (k) validateKrivo(s.krivo, null, 0, need);
        if (k && Array.isArray(s.krivo) && s.cilj) {
          try {
            const a = analizaCilja(s, k, (p) => fizikaKruzenja(s, p).vel);
            s.krivo.forEach(
              (x, i) => isNum(x?.vrijednost) && need(
                !a.ok(x.vrijednost),
                `krivo[${i}].vrijednost ${formatNumber(x.vrijednost)} ostvaruje cilj, a to je točan odgovor`
              )
            );
          } catch {
          }
        }
      }
      if (s.nacin === "smjer") {
        need(
          PITANJA.includes(s.pitaj),
          `smjer treba pitaj: ${PITANJA.join(", ")}`
        );
        need(
          isNum(s.tocka) && s.tocka >= 0 && s.tocka < 360 && Math.abs(s.tocka / 22.5 - Math.round(s.tocka / 22.5)) < 1e-9,
          "tocka mora biti kut položaja 0–337,5° u koracima od 22,5°"
        );
        need(s.cilj == null && s.prikazi == null, "smjer nema cilj ni prikazi");
        if (s.pitaj === "izlijetanje" && s.scena === "zavoj")
          need(
            !fizikaKruzenja(s).prolazi,
            "izlijetanje u zavoju: brzina mora biti veća od najveće (F_cp > F_max), inače auto ne izlijeće"
          );
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "smjer") return checkSmjer(s, odgovor3);
      if (s.nacin === "postavi")
        return checkCilj(
          s,
          odgovor3,
          (p) => fizikaKruzenja(s, p).vel,
          velicineKruzenja(s)
        );
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "postavi") {
        const k = klizaciOd(s)[0];
        return `${k.oznaka} = ${withUnit(analizaCilja(s, k, (p) => fizikaKruzenja(s, p).vel).best, k.jedinica)}`;
      }
      if (s.nacin === "smjer")
        return s.pitaj === "brzina" || s.pitaj === "izlijetanje" ? "po tangenti na kružnicu, u smjeru vrtnje" : "prema središtu kružnice";
      return "";
    }
  };

  // src/lib/lesson/katalog/fizika/sudar.js
  var NACINI5 = ["istrazi", "prikaz", "predvidi"];
  var SUDAR_VRSTE = [
    "elasticni",
    "neelasticni",
    "savrseno_neelasticni",
    "razdvajanje"
  ];
  var SUDAR_SLIKE = [
    "vagon",
    "kolica",
    "kutija",
    "auto",
    "kugla",
    "lopta",
    "puska",
    "metak"
  ];
  var JEDINICE_MASE = { kg: 1, g: 1e-3, t: 1e3 };
  var PRIKAZI2 = ["p", "Ek", "F", "tablica"];
  var KLIZACI = ["m1", "m2", "v1", "v2", "vp", "e", "dt"];
  var SUDARI = ["elasticni", "neelasticni", "savrseno_neelasticni"];
  var SUDAR_VELICINE = {
    v_poslije: { jedinica: "m/s", oznaka: "v′", naziv: "brzina poslije" },
    p_prije: { jedinica: "kg·m/s", oznaka: "p", naziv: "količina gibanja prije" },
    p_poslije: {
      jedinica: "kg·m/s",
      oznaka: "p′",
      naziv: "količina gibanja poslije"
    },
    dp: { jedinica: "kg·m/s", oznaka: "Δp", naziv: "promjena količine gibanja" },
    F: { jedinica: "N", oznaka: "F", naziv: "prosječna sila" },
    Ek_prije: { jedinica: "J", oznaka: "Eₖ", naziv: "kinetička energija prije" },
    Ek_poslije: {
      jedinica: "J",
      oznaka: "Eₖ′",
      naziv: "kinetička energija poslije"
    },
    gubitak: {
      jedinica: "J",
      oznaka: "Q",
      naziv: "kinetička energija pretvorena u toplinu, zvuk i deformaciju"
    }
  };
  var clean = (x) => isNum(x) ? Number(x.toPrecision(12)) + 0 : x;
  var ime = (s, i) => s.tijela[i]?.oznaka ?? `tijelo ${i + 1}`;
  function sudarIshod(s) {
    const T = s.tijela.map((b) => ({ m: b.m, v: isNum(b.v) ? b.v : 0 }));
    const vrsta = s.vrsta;
    const zadan = s.tijela.findIndex((b) => isNum(b.v_poslije));
    let vp;
    if (s.zid) {
      const b = s.tijela[0];
      vp = [
        isNum(b.v_poslije) ? b.v_poslije : vrsta === "savrseno_neelasticni" ? 0 : -(vrsta === "elasticni" ? 1 : s.e) * T[0].v
      ];
    } else if (vrsta === "razdvajanje") {
      const v0 = isNum(s.v0) ? s.v0 : 0;
      T.forEach((t) => t.v = v0);
      const j = 1 - zadan;
      vp = [];
      vp[zadan] = s.tijela[zadan].v_poslije;
      vp[j] = ((T[0].m + T[1].m) * v0 - T[zadan].m * vp[zadan]) / T[j].m;
    } else {
      const [a, b] = T;
      const P = a.m * a.v + b.m * b.v;
      const M = a.m + b.m;
      if (vrsta === "savrseno_neelasticni") vp = [P / M, P / M];
      else if (vrsta === "neelasticni" && zadan >= 0) {
        const j = 1 - zadan;
        vp = [];
        vp[zadan] = s.tijela[zadan].v_poslije;
        vp[j] = (P - T[zadan].m * vp[zadan]) / T[j].m;
      } else {
        const e = vrsta === "elasticni" ? 1 : s.e;
        vp = [(P + b.m * e * (b.v - a.v)) / M, (P + a.m * e * (a.v - b.v)) / M];
      }
    }
    const dt = isNum(s.trajanje) && s.trajanje > 0 ? s.trajanje : null;
    const tijela = T.map((t, i) => {
      const v2 = clean(vp[i]);
      const p = clean(t.m * t.v);
      const pp = clean(t.m * v2);
      const dp = clean(pp - p);
      return {
        m: t.m,
        v: clean(t.v),
        vp: v2,
        p,
        pp,
        Ek: clean(t.m * t.v ** 2 / 2),
        Ekp: clean(t.m * v2 ** 2 / 2),
        dp,
        F: dt ? clean(dp / dt) : null
      };
    });
    const sum = (k) => clean(tijela.reduce((a, t) => a + t[k], 0));
    const Ek = sum("Ek");
    const Ekp = sum("Ekp");
    return {
      zid: !!s.zid,
      vrsta,
      spojeni: !s.zid && vrsta === "savrseno_neelasticni",
      razdvajanje: vrsta === "razdvajanje",
      tijela,
      P: sum("p"),
      Pp: sum("pp"),
      Ek,
      Ekp,
      gubitak: clean(Ek - Ekp),
      dt
    };
  }
  function sudarProblem(s) {
    const t = s.tijela;
    if (s.zid) {
      if (!(t[0].v > 0))
        return "tijelo mora ići prema zidu: zid je desno, pa mora biti v > 0";
      const vp = t[0].v_poslije;
      if (isNum(vp)) {
        if (vp > 0)
          return "nakon udara u zid tijelo ide natrag, pa v_poslije mora biti negativna (ili 0)";
        if (-vp > t[0].v * (1 + 1e-9))
          return "tijelo se ne može odbiti brže nego što je udarilo (energija bi nastala ni iz čega)";
      }
      return null;
    }
    const r = sudarIshod(s);
    if (s.vrsta === "razdvajanje") {
      if (!(r.tijela[1].vp > r.tijela[0].vp))
        return "nakon razdvajanja desno tijelo (tijela[1]) mora ići udesno brže od lijevog, inače se ne razdvoje; tijela[0] je lijevo (npr. puška), tijela[1] desno (metak)";
      return null;
    }
    if (!(t[0].v > (t[1].v ?? 0)))
      return "tijela se ne sudaraju: lijevo tijelo (tijela[0]) mora ići udesno brže od desnog (v₁ > v₂)";
    const rel = r.tijela[1].vp - r.tijela[0].vp;
    const pril = t[0].v - (t[1].v ?? 0);
    if (rel < -1e-9 * pril)
      return "zadana brzina poslije nije moguća: tijela bi prošla jedno kroz drugo";
    if (rel > pril * (1 + 1e-9))
      return "zadana brzina poslije nije moguća: kinetička energija bi porasla";
    return null;
  }
  function sudarTijeloPitanja(s) {
    const q = s.pitaj ?? {};
    if (Number.isInteger(q.tijelo)) return q.tijelo;
    if (s.zid) return 0;
    if (q.velicina === "v_poslije") {
      const zadan = s.tijela.findIndex((b) => isNum(b.v_poslije));
      if (zadan >= 0) return 1 - zadan;
      return 0;
    }
    if (q.velicina === "dp" || q.velicina === "F") return 0;
    return null;
  }
  function sudarOdgovor(s) {
    const R2 = sudarIshod(s);
    const i = sudarTijeloPitanja(s);
    const tj = i == null ? null : R2.tijela[i];
    switch (s.pitaj?.velicina) {
      case "v_poslije":
        return tj.vp;
      case "p_prije":
        return tj ? tj.p : R2.P;
      case "p_poslije":
        return tj ? tj.pp : R2.Pp;
      case "dp":
        return tj.dp;
      case "F":
        return tj.F;
      case "Ek_prije":
        return tj ? tj.Ek : R2.Ek;
      case "Ek_poslije":
        return tj ? tj.Ekp : R2.Ekp;
      case "gubitak":
        return R2.gubitak;
      default:
        return null;
    }
  }
  function bezPretvorbe(s) {
    if (!s.tijela.some((b) => b.jedinica && b.jedinica !== "kg")) return null;
    const t = {
      ...s,
      tijela: s.tijela.map((b) => ({
        ...b,
        m: b.m / (JEDINICE_MASE[b.jedinica] ?? 1)
      }))
    };
    return sudarOdgovor(t);
  }
  function sudarKrivo(s) {
    const R2 = sudarIshod(s);
    const q = s.pitaj;
    const i = sudarTijeloPitanja(s);
    const ans = sudarOdgovor(s);
    const K2 = [];
    const add2 = (vrijednost3, poruka2) => isNum(vrijednost3) && K2.push({ vrijednost: clean(vrijednost3), poruka: poruka2 });
    const T = R2.tijela;
    const smjer = "Iznos je dobar, ali predznak nije. Pozitivan smjer je udesno (→); brzina, količina gibanja i sila ulijevo imaju minus.";
    if (q.velicina === "v_poslije") {
      if (R2.zid) {
        add2(
          -ans,
          "Iznos je dobar, ali tijelo se nakon udara giba natrag, od zida: brzina mora imati minus."
        );
        add2(
          T[0].v,
          "Tijelo se ne nastavlja gibati prema zidu istom brzinom: odbije se natrag."
        );
      } else if (R2.razdvajanje) {
        const k = 1 - i;
        const v0 = isNum(s.v0) ? s.v0 : 0;
        if (v0 === 0)
          add2(
            -ans,
            `Iznos je dobar, ali smjer nije. Prije je ukupna količina gibanja bila 0, pa poslije ${ime(s, i)} i ${ime(s, k)} moraju ići na suprotne strane: brzina treba minus.`
          );
        else add2(-ans, smjer);
        add2(
          -(T[i].m * T[k].vp) / T[k].m,
          `Zamijenio si mase. Iz $m_1 v_1' + m_2 v_2' = 0$ brzina je $v' = -\\frac{m_{\\text{drugog}} \\cdot v_{\\text{drugog}}'}{m_{\\text{ovog}}}$: dijeli se masom tijela kojem tražiš brzinu.`
        );
        add2(
          -(T[k].m * T[k].vp) / (T[0].m + T[1].m),
          `Podijelio si ukupnom masom. Nakon razdvajanja tijela se gibaju odvojeno, pa se dijeli samo masom tijela ${ime(s, i)}.`
        );
        add2(
          -T[k].vp,
          "Tijela nemaju jednake brzine, nego jednake i suprotne količine gibanja: lakše tijelo je mnogo brže."
        );
        add2(
          -Math.sign(T[k].vp) * Math.sqrt(T[k].m * T[k].vp ** 2 / T[i].m),
          "Izjednačio si kinetičke energije. Pri razdvajanju se čuva količina gibanja, a energija dolazi izvana (barut, opruga), pa se ne čuva."
        );
      } else {
        const j = 1 - i;
        const M = T[0].m + T[1].m;
        add2(-ans, smjer);
        if (R2.spojeni) {
          add2(
            R2.P / T[1].m,
            `Podijelio si samo masom tijela ${ime(s, 1)}. Nakon sudara tijela se gibaju zajedno, kao jedno tijelo mase $m_1 + m_2$.`
          );
          add2(
            R2.P / T[0].m,
            `Podijelio si samo masom tijela ${ime(s, 0)}. Nakon sudara tijela se gibaju zajedno, kao jedno tijelo mase $m_1 + m_2$.`
          );
          add2(
            R2.P,
            "To je ukupna količina gibanja (kg·m/s), a ne brzina: podijeli je ukupnom masom."
          );
          add2(
            T[0].v,
            "Brzina ne može ostati ista: isti zalet sad gura veću masu, pa je zajednička brzina manja."
          );
          add2(
            Math.sign(R2.P) * Math.sqrt(2 * R2.Ek / M),
            "Izjednačio si kinetičke energije. Kad se tijela spoje, kinetička energija se ne čuva (dio ode u toplinu i deformaciju); čuva se količina gibanja."
          );
          add2(
            (T[0].v + T[1].v) / 2,
            "Ne računa se prosjek brzina: teže tijelo više utječe. Izjednači količinu gibanja prije i poslije."
          );
        } else {
          add2(
            R2.P / M,
            "To bi bila zajednička brzina da se tijela spoje. Ovdje se tijela odbiju i gibaju odvojeno."
          );
          add2(
            T[i].v,
            `Brzina tijela ${ime(s, i)} se u sudaru promijeni; ne ostaje ista kao prije.`
          );
          if (Math.abs(T[0].m - T[1].m) > 1e-9 * M)
            add2(T[j].v, "Brzine se zamijene samo kad su mase jednake.");
          if (isNum(s.tijela[j]?.v_poslije)) {
            add2(
              R2.P / T[i].m,
              `Zaboravio si oduzeti količinu gibanja tijela ${ime(s, j)} poslije sudara: $m_i v_i' = p_{\\text{ukupno}} - m_j v_j'$.`
            );
            add2(
              (R2.P + T[j].m * T[j].vp) / T[i].m,
              `Količinu gibanja tijela ${ime(s, j)} poslije sudara treba oduzeti od ukupne, a ne pribrojiti.`
            );
          }
        }
      }
    }
    if (q.velicina === "p_prije" || q.velicina === "p_poslije") {
      const poslije = q.velicina === "p_poslije";
      add2(-ans, smjer);
      if (i == null && !R2.zid) {
        const magn = T.reduce((a, t) => a + Math.abs(poslije ? t.pp : t.p), 0);
        add2(
          magn,
          "Količine gibanja zbrajaju se s predznakom: kad tijela idu na suprotne strane, oduzimaju se."
        );
      }
      const tj = i == null ? null : T[i];
      if (tj)
        add2(
          poslije ? tj.m * tj.vp ** 2 / 2 : tj.m * tj.v ** 2 / 2,
          "To je kinetička energija. Količina gibanja je $p = m \\cdot v$."
        );
    }
    if (q.velicina === "dp" || q.velicina === "F") {
      const tj = T[i];
      const dt = R2.dt ?? 1;
      const f2 = q.velicina === "F" ? 1 / dt : 1;
      const razlikaIznosa = tj.m * (Math.abs(tj.vp) - Math.abs(tj.v));
      const porukaIznosa = "Oduzeo si samo iznose brzina. Tijelo je stalo i krenulo natrag, pa se brzini promijeni i predznak: $\\Delta p = m v' - m v$, s minusom za gibanje ulijevo.";
      add2(razlikaIznosa * f2, porukaIznosa);
      add2(-razlikaIznosa * f2, porukaIznosa);
      add2(
        -ans,
        q.velicina === "dp" ? "Promjena je uvijek „poslije minus prije“: $\\Delta p = p' - p$, a ne $p - p'$." : "Sila ima smjer promjene količine gibanja: $F = \\frac{\\Delta p}{\\Delta t}$, s predznakom od $\\Delta p = p' - p$."
      );
      add2(
        (tj.vp - tj.v) * f2,
        q.velicina === "dp" ? "To je promjena brzine. Količina gibanja je $p = m v$, pa pomnoži masom." : "Zaboravio si masu: $F = \\frac{m(v' - v)}{\\Delta t}$."
      );
      if (q.velicina === "F") {
        add2(
          tj.dp * dt,
          "Sila je promjena količine gibanja podijeljena s vremenom, a ne pomnožena: $F = \\frac{\\Delta p}{\\Delta t}$."
        );
        add2(
          -tj.p / dt,
          "To je samo količina gibanja prije udara podijeljena s $\\Delta t$. Sila mora promijeniti cijelu količinu gibanja: $\\Delta p = p' - p$."
        );
        add2(
          tj.p / dt,
          "To je samo količina gibanja prije udara podijeljena s $\\Delta t$. Sila mora promijeniti cijelu količinu gibanja: $\\Delta p = p' - p$."
        );
      }
    }
    if (q.velicina === "Ek_prije" || q.velicina === "Ek_poslije") {
      const poslije = q.velicina === "Ek_poslije";
      const tj = i == null ? T : [T[i]];
      const v = (t) => poslije ? t.vp : t.v;
      add2(
        tj.reduce((a, t) => a + t.m * v(t) ** 2, 0),
        "Zaboravio si podijeliti s 2: $E_k = \\frac{m v^2}{2}$."
      );
      add2(
        tj.reduce((a, t) => a + t.m * Math.abs(v(t)) / 2, 0),
        "Brzinu treba kvadrirati: $E_k = \\frac{m v^2}{2}$."
      );
      if (poslije && i == null && !R2.zid && R2.vrsta !== "elasticni")
        add2(
          R2.Ek,
          "Kinetička energija se u ovom sudaru ne čuva: dio ode u toplinu, zvuk i deformaciju. Izračunaj je iz brzina poslije."
        );
      if (poslije && R2.spojeni && i == null)
        add2(
          T[0].m * T[0].vp ** 2 / 2,
          "Nakon sudara gibaju se oba tijela zajedno: uzmi ukupnu masu $m_1 + m_2$."
        );
    }
    if (q.velicina === "gubitak") {
      add2(
        R2.Ekp,
        "To je kinetička energija poslije sudara. Traži se koliko je nestalo: $E_k - E_k'$."
      );
      add2(
        R2.Ek,
        "To je kinetička energija prije sudara. Traži se koliko je nestalo: $E_k - E_k'$."
      );
      add2(
        0,
        "Kinetička energija se u ovom sudaru ne čuva: usporedi $E_k$ prije i poslije."
      );
      add2(
        2 * R2.gubitak,
        "Zaboravio si podijeliti s 2: $E_k = \\frac{m v^2}{2}$."
      );
    }
    const g = bezPretvorbe(s);
    if (isNum(g)) add2(g, "Masu u gramima pretvori u kilograme (10 g = 0,01 kg).");
    const tol2 = tolerance(ans, s.pitaj?.tolerancija);
    const out = [];
    for (const k of K2) {
      if (near(k.vrijednost, ans, Math.max(tol2, tolerance(k.vrijednost))))
        continue;
      if (out.some((o) => near(o.vrijednost, k.vrijednost, tolerance(k.vrijednost))))
        continue;
      out.push(k);
    }
    return out;
  }
  var savjet2 = (s) => {
    const R2 = sudarIshod(s);
    switch (s.pitaj.velicina) {
      case "v_poslije":
        if (R2.zid)
          return "Pri udaru u zid brzina mijenja smjer; pozitivan smjer je prema zidu.";
        if (R2.razdvajanje)
          return "Ukupna količina gibanja prije i poslije je ista: $(m_1 + m_2) v_0 = m_1 v_1' + m_2 v_2'$. Pazi na predznak.";
        if (R2.spojeni)
          return "Ukupna količina gibanja prije = poslije. Nakon sudara tijela se gibaju zajedno, kao jedno tijelo mase $m_1 + m_2$.";
        return "Ukupna količina gibanja prije i poslije sudara je ista: $m_1 v_1 + m_2 v_2 = m_1 v_1' + m_2 v_2'$. Pazi na predznake.";
      case "p_prije":
      case "p_poslije":
        return "Količina gibanja je $p = m v$, s predznakom smjera (udesno +, ulijevo −); ukupna je zbroj s predznacima.";
      case "dp":
        return "$\\Delta p = p' - p = m v' - m v$; brzina ulijevo ima minus.";
      case "F":
        return "Prosječna sila je $F = \\frac{\\Delta p}{\\Delta t}$, a $\\Delta p = m v' - m v$ (s predznacima).";
      case "gubitak":
        return "Izračunaj ukupnu kinetičku energiju prije i poslije ($E_k = \\frac{m v^2}{2}$ za svako tijelo) i oduzmi.";
      default:
        return "Kinetička energija je $E_k = \\frac{m v^2}{2}$ za svako tijelo; ukupna je zbroj.";
    }
  };
  var VELICINE_OPIS = Object.entries(SUDAR_VELICINE).map(([k, v]) => `${k} (${v.jedinica})`).join(", ");
  var sudar = {
    tip: "sudar",
    samoRucno: true,
    uloga: (s) => s.nacin === "predvidi" ? ODGOVOR : POGLEDAJ,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Sudar na pruzi (jedna dimenzija): dva tijela ili tijelo i zid. Animacija sudara, strelice brzina, količine gibanja prije/poslije
na brojevnom pravcu, stupci kinetičke energije (gubitak kao toplina) i po želji impuls sile F–t s klizačem trajanja. Sve vrijednosti
računa aplikacija. Pozitivan smjer je udesno; tijela[0] je lijevo, tijela[1] desno; zid je desno.
Polja: tekst, nacin, vrsta ("elasticni" | "neelasticni" | "savrseno_neelasticni" (spoje se) | "razdvajanje" (trzaj, eksplozija)),
tijela: [{oznaka (kratko, npr. "vagon A", "puška"), m (u kg!), v? (m/s, s predznakom; zadano 0), v_poslije?, jedinica? ("kg" | "g" | "t" – samo prikaz mase),
slika? (${SUDAR_SLIKE.join(" | ")})}] (2 tijela; 1 uz zid: true),
e? (koeficijent restitucije 0 < e < 1, samo za "neelasticni" bez zadane v_poslije), v0? (razdvajanje: zajednička brzina prije, zadano 0),
trajanje? (Δt sudara u s: crta impuls sile F = Δp/Δt s klizačem), prikazi? (podskup od ${PRIKAZI2.join(", ")}; zadano p, Ek i F ako ima trajanje).
- elasticni / savrseno_neelasticni: brzine poslije računa aplikacija (bez v_poslije);
- neelasticni: zadaj e ILI v_poslije jednog tijela (drugo se računa iz očuvanja količine gibanja);
- razdvajanje: tijela miruju (ili se gibaju s v0) zajedno; točno jedno tijelo ima v_poslije, drugo se računa (npr. [puška, metak]);
- zid: true, jedno tijelo s v > 0; vrsta elasticni (v' = −v), savrseno_neelasticni (stane) ili neelasticni s e ili v_poslije (npr. −6).
Načini:
- "istrazi": korak za gledanje, učenik pušta sudar i mijenja klizače; klizaci?: [{id: ${KLIZACI.join(" | ")}, min, max, korak}]
  (m u jedinici tijela, v u m/s, vp = zadana v_poslije, dt u s), vrste?: popis 2–3 vrste sudara među kojima učenik bira (prekidač);
- "prikaz": korak za gledanje, zadani sudar s gumbom za puštanje;
- "predvidi": učenik upiše traženu veličinu s predznakom, zatim animacija pokaže pravi ishod. pitaj: {velicina: ${VELICINE_OPIS},
  tijelo? (0 | 1; bez njega p i Ek su ukupni, a v_poslije kod spajanja zajednička), tolerancija?}. Tipične greške (zaboravljena ukupna masa,
  iznos bez predznaka, razlika iznosa umjesto Δp, očuvanje Ek u neelastičnom sudaru, grami umjesto kilograma…) dodaju se same; ${KRIVO_OPIS}.`,
    primjer: {
      tip: "sudar",
      nacin: "predvidi",
      vrsta: "savrseno_neelasticni",
      tekst: "Vagon mase 8000 kg naleti brzinom 2 m/s na vagon mase 12000 kg koji miruje i spoje se. Kolikom se brzinom gibaju zajedno?",
      tijela: [
        { oznaka: "vagon A", m: 8e3, v: 2, slika: "vagon" },
        { oznaka: "vagon B", m: 12e3, v: 0, slika: "vagon" }
      ],
      pitaj: { velicina: "v_poslije" }
    },
    primjeri: [
      {
        tip: "sudar",
        nacin: "istrazi",
        vrsta: "savrseno_neelasticni",
        vrste: ["elasticni", "neelasticni", "savrseno_neelasticni"],
        e: 0.5,
        tekst: "Pusti sudar, pa promijeni vrstu sudara i mase. Gledaj ukupnu količinu gibanja i ukupnu kinetičku energiju prije i poslije.",
        tijela: [
          { oznaka: "A", m: 2, v: 3, slika: "kolica" },
          { oznaka: "B", m: 3, v: 0, slika: "kolica" }
        ],
        klizaci: [
          { id: "m1", min: 1, max: 6, korak: 0.5 },
          { id: "m2", min: 1, max: 6, korak: 0.5 },
          { id: "v1", min: 1, max: 6, korak: 0.5 },
          { id: "v2", min: -4, max: 4, korak: 0.5 }
        ]
      },
      {
        tip: "sudar",
        nacin: "predvidi",
        vrsta: "razdvajanje",
        tekst: "Puška mase 4 kg ispali metak mase 10 g brzinom 600 m/s. Kolikom brzinom puška trzne? Pazi na predznak.",
        tijela: [
          { oznaka: "puška", m: 4, slika: "puska" },
          {
            oznaka: "metak",
            m: 0.01,
            jedinica: "g",
            v_poslije: 600,
            slika: "metak"
          }
        ],
        pitaj: { velicina: "v_poslije" }
      },
      {
        tip: "sudar",
        nacin: "prikaz",
        vrsta: "razdvajanje",
        tekst: "Pucanj: puška i metak dobiju jednake i suprotne količine gibanja. Usporedi njihove kinetičke energije.",
        tijela: [
          { oznaka: "puška", m: 4, slika: "puska" },
          {
            oznaka: "metak",
            m: 0.01,
            jedinica: "g",
            v_poslije: 600,
            slika: "metak"
          }
        ]
      },
      {
        tip: "sudar",
        nacin: "predvidi",
        vrsta: "neelasticni",
        zid: true,
        trajanje: 0.02,
        tekst: "Lopta mase 0,4 kg udari okomito u zid brzinom 8 m/s i odbije se natrag brzinom 6 m/s. Kolika je promjena količine gibanja lopte?",
        tijela: [
          { oznaka: "lopta", m: 0.4, v: 8, v_poslije: -6, slika: "lopta" }
        ],
        pitaj: { velicina: "dp" }
      },
      {
        tip: "sudar",
        nacin: "predvidi",
        vrsta: "neelasticni",
        zid: true,
        trajanje: 0.02,
        tekst: "Sudar lopte (0,4 kg, 8 m/s → 6 m/s natrag) sa zidom trajao je 0,02 s. Kolika je prosječna sila zida na loptu?",
        tijela: [
          { oznaka: "lopta", m: 0.4, v: 8, v_poslije: -6, slika: "lopta" }
        ],
        pitaj: { velicina: "F" }
      },
      {
        tip: "sudar",
        nacin: "istrazi",
        vrsta: "neelasticni",
        zid: true,
        trajanje: 0.02,
        tekst: "Mijenjaj trajanje udara klizačem $\\Delta t$. Promjena količine gibanja ostaje ista – što se događa sa silom?",
        tijela: [
          { oznaka: "lopta", m: 0.4, v: 8, v_poslije: -6, slika: "lopta" }
        ],
        klizaci: [{ id: "dt", min: 0.01, max: 0.4, korak: 0.01 }],
        prikazi: ["p", "F"]
      },
      {
        tip: "sudar",
        nacin: "predvidi",
        vrsta: "savrseno_neelasticni",
        tekst: "Vagoni (8000 kg, 2 m/s i 12000 kg u mirovanju) spoje se i gibaju zajedno brzinom 0,8 m/s. Koliko se kinetičke energije pretvorilo u toplinu, zvuk i deformaciju?",
        tijela: [
          { oznaka: "vagon A", m: 8e3, v: 2, slika: "vagon" },
          { oznaka: "vagon B", m: 12e3, v: 0, slika: "vagon" }
        ],
        pitaj: { velicina: "gubitak" }
      },
      {
        tip: "sudar",
        nacin: "predvidi",
        vrsta: "neelasticni",
        tekst: "Kugla A (2 kg, 3 m/s) udari kuglu B (1 kg) koja ide prema njoj brzinom 1 m/s. Nakon sudara A ide udesno brzinom 0,5 m/s. Kolika je brzina kugle B?",
        tijela: [
          { oznaka: "A", m: 2, v: 3, v_poslije: 0.5, slika: "kugla" },
          { oznaka: "B", m: 1, v: -1, slika: "kugla" }
        ],
        pitaj: { velicina: "v_poslije", tijelo: 1 }
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI5.includes(s.nacin),
        `sudar.nacin mora biti ${NACINI5.join(", ")}`
      );
      need(isText(s.tekst), "sudar treba „tekst“");
      if (!need(
        SUDAR_VRSTE.includes(s.vrsta),
        `sudar.vrsta mora biti ${SUDAR_VRSTE.join(", ")}`
      ))
        return list;
      if (s.zid != null) need(s.zid === true, "zid je true ili ga nema");
      const n = s.zid ? 1 : 2;
      if (!need(
        Array.isArray(s.tijela) && s.tijela.length === n,
        s.zid ? "uz zid: true treba točno jedno tijelo" : "sudar treba točno dva tijela (ili jedno uz zid: true)"
      ))
        return list;
      let ok = true;
      s.tijela.forEach((b, i) => {
        const at = `tijela[${i}]`;
        ok = need(
          isText(b?.oznaka) && b.oznaka.length <= 14,
          `${at}.oznaka mora biti kratak tekst (do 14 znakova)`
        ) && ok;
        ok = need(
          isNum(b?.m) && b.m > 0 && b.m <= 1e7,
          `${at}.m mora biti masa u kg (0 < m ≤ 10⁷); grame zapiši kao kg uz jedinica: "g"`
        ) && ok;
        if (b?.v != null)
          ok = need(
            isNum(b.v) && Math.abs(b.v) <= 5e3,
            `${at}.v mora biti broj (m/s, s predznakom)`
          ) && ok;
        if (b?.v_poslije != null)
          ok = need(
            isNum(b.v_poslije) && Math.abs(b.v_poslije) <= 5e3,
            `${at}.v_poslije mora biti broj (m/s, s predznakom)`
          ) && ok;
        if (b?.jedinica != null)
          need(
            b.jedinica in JEDINICE_MASE,
            `${at}.jedinica mora biti ${Object.keys(JEDINICE_MASE).join(", ")} (m je uvijek u kg)`
          );
        if (b?.slika != null)
          need(
            SUDAR_SLIKE.includes(b.slika),
            `${at}.slika mora biti jedna od: ${SUDAR_SLIKE.join(", ")}`
          );
      });
      if (!ok) return list;
      const zadanih = s.tijela.filter((b) => b.v_poslije != null).length;
      if (s.zid) {
        need(s.vrsta !== "razdvajanje", "zid ne može uz vrsta razdvajanje");
        need(isNum(s.tijela[0].v), "tijelo uz zid treba brzinu v");
        if (s.vrsta === "neelasticni")
          need(
            s.e != null !== (zadanih === 1),
            "zid s vrsta neelasticni: zadaj e ILI v_poslije (jedno od dvoga)"
          );
        else
          need(
            zadanih === 0,
            `uz vrsta ${s.vrsta} brzinu poslije računa aplikacija: makni v_poslije`
          );
      } else if (s.vrsta === "razdvajanje") {
        need(
          zadanih === 1,
          "razdvajanje treba točno jedno tijelo s v_poslije (drugom brzinu računa aplikacija)"
        );
        need(
          s.tijela.every((b) => b.v == null),
          "kod razdvajanja tijela prije imaju zajedničku brzinu v0 (zadano 0), pa makni tijela[].v"
        );
        if (s.v0 != null)
          need(isNum(s.v0) && Math.abs(s.v0) <= 5e3, "v0 mora biti broj (m/s)");
      } else {
        if (s.vrsta === "neelasticni")
          need(
            s.e != null !== (zadanih === 1),
            "neelasticni: zadaj e ILI v_poslije jednog tijela (jedno od dvoga)"
          );
        else
          need(
            zadanih === 0,
            `uz vrsta ${s.vrsta} brzine poslije računa aplikacija: makni v_poslije`
          );
      }
      if (s.v0 != null)
        need(s.vrsta === "razdvajanje", "v0 postoji samo kod razdvajanja");
      const trebaE = s.vrsta === "neelasticni" && zadanih === 0;
      const eDopusten = trebaE || s.nacin === "istrazi" && Array.isArray(s.vrste) && s.vrste.includes("neelasticni");
      if (s.e != null)
        need(
          eDopusten && isNum(s.e) && s.e > 0 && s.e < 1,
          "e (koeficijent restitucije) mora biti 0 < e < 1 i samo uz neelastičan sudar bez zadane v_poslije"
        );
      if (s.trajanje != null)
        need(
          isNum(s.trajanje) && s.trajanje > 0 && s.trajanje <= 5,
          "trajanje (Δt sudara) mora biti broj sekundi, 0 < Δt ≤ 5"
        );
      if (s.prikazi != null) {
        if (need(
          Array.isArray(s.prikazi) && s.prikazi.length >= 1 && s.prikazi.every((p) => PRIKAZI2.includes(p)),
          `prikazi mora biti popis od: ${PRIKAZI2.join(", ")}`
        )) {
          need(
            !s.prikazi.includes("F") || s.trajanje != null,
            "prikazi F treba trajanje (Δt)"
          );
        }
      }
      if (list.length) return list;
      const fiz = sudarProblem(s);
      need(!fiz, `sudar: ${fiz}`);
      if (s.nacin !== "istrazi") {
        need(s.klizaci == null, "klizaci postoje samo u načinu istrazi");
        need(
          s.vrste == null,
          "vrste (prekidač vrste sudara) postoje samo u načinu istrazi"
        );
      }
      if (s.nacin !== "predvidi") {
        noAnswerFields(s, need);
        need(
          s.pitaj == null,
          `korak „sudar“ u načinu ${s.nacin} je za gledanje, pa ne smije imati „pitaj“`
        );
      }
      if (s.nacin === "istrazi") {
        if (s.vrste != null) {
          const v = s.vrste;
          if (need(
            Array.isArray(v) && v.length >= 2 && v.length <= 3 && new Set(v).size === v.length && v.every((x) => SUDARI.includes(x)),
            "vrste mora biti 2–3 različite vrste od: elasticni, neelasticni, savrseno_neelasticni"
          )) {
            need(
              v.includes(s.vrsta),
              "vrste mora sadržavati početnu vrstu (vrsta)"
            );
            need(
              zadanih === 0,
              "uz prekidač vrsta brzine poslije računa aplikacija: makni v_poslije"
            );
            if (v.includes("neelasticni"))
              need(
                isNum(s.e),
                "prekidač s neelastičnim sudarom treba e (0 < e < 1)"
              );
          }
        }
        if (s.klizaci != null && need(
          Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 5,
          "klizaci mora biti popis 1–5 klizača"
        )) {
          const ids = [];
          s.klizaci.forEach((k, i) => {
            const at = `klizaci[${i}]`;
            if (!need(
              KLIZACI.includes(k?.id),
              `${at}.id mora biti jedan od: ${KLIZACI.join(", ")}`
            ))
              return;
            ids.push(k.id);
            if (!need(
              [k.min, k.max, k.korak].every(isNum) && k.max > k.min && k.korak > 0,
              `${at} treba brojeve min < max i korak > 0`
            ))
              return;
            need(
              (k.max - k.min) / k.korak <= 400,
              `${at} ima previše koraka (najviše 400)`
            );
            const cur = klizacPocetak(s, k.id);
            need(
              cur == null || cur >= k.min - 1e-9 && cur <= k.max + 1e-9,
              `${at}: početna vrijednost iz speca (${formatNumber(cur)}) mora biti između min i max`
            );
            if (k.id === "m1" || k.id === "m2")
              need(k.min > 0, `${at}: masa mora biti > 0`);
            if (k.id === "m2" || k.id === "v2")
              need(!s.zid, `${at}: uz zid postoji samo jedno tijelo`);
            if (k.id === "v1" || k.id === "v2")
              need(
                s.vrsta !== "razdvajanje",
                `${at}: kod razdvajanja nema brzina prije (klizač vp mijenja zadanu brzinu poslije)`
              );
            if (k.id === "vp")
              need(
                zadanih === 1,
                `${at}: klizač vp treba tijelo sa zadanom v_poslije`
              );
            if (k.id === "e") need(isNum(s.e), `${at}: klizač e treba zadani e`);
            if (k.id === "dt")
              need(
                s.trajanje != null && k.min > 0 && (!s.prikazi || s.prikazi.includes("F")),
                `${at}: klizač dt treba trajanje, min > 0 i prikaz F`
              );
          });
          need(
            new Set(ids).size === ids.length,
            "id-jevi klizača moraju biti različiti"
          );
        }
      }
      if (s.nacin === "predvidi") {
        const q = s.pitaj;
        if (!need(
          q && SUDAR_VELICINE[q.velicina],
          `predvidi treba pitaj: {velicina: ${Object.keys(SUDAR_VELICINE).join(" | ")}}`
        ))
          return list;
        if (q.tijelo != null)
          need(
            Number.isInteger(q.tijelo) && q.tijelo >= 0 && q.tijelo < n,
            `pitaj.tijelo mora biti indeks tijela (0${n > 1 ? " ili 1" : ""})`
          );
        if (q.tolerancija != null)
          need(
            isNum(q.tolerancija) && q.tolerancija >= 0,
            "pitaj.tolerancija mora biti broj ≥ 0"
          );
        const i = sudarTijeloPitanja(s);
        if (q.velicina === "v_poslije") {
          if (!s.zid && SUDARI.includes(s.vrsta) && s.vrsta !== "savrseno_neelasticni" && zadanih === 0)
            need(
              q.tijelo != null,
              "pitaj v_poslije kod sudara koji nije savršeno neelastičan treba tijelo (0 ili 1)"
            );
          if (i != null)
            need(
              !isNum(s.tijela[i]?.v_poslije),
              "pitaj v_poslije za tijelo kojem je v_poslije već zadana: pitaj za drugo tijelo"
            );
        }
        if (q.velicina === "F")
          need(s.trajanje != null, "pitaj F treba trajanje (Δt)");
        if (q.velicina === "gubitak") {
          need(
            s.vrsta !== "razdvajanje",
            "kod razdvajanja kinetička energija raste; pitaj Ek_poslije umjesto gubitka"
          );
          need(
            s.vrsta !== "elasticni",
            "u elastičnom sudaru nema gubitka kinetičke energije"
          );
        }
        if (list.length) return list;
        const a = sudarOdgovor(s);
        need(isNum(a), "traženu veličinu nije moguće izračunati");
        validateKrivo(s.krivo, a, tolerance(a, q.tolerancija), need);
      }
      return list;
    },
    check(s, input) {
      const q = s.pitaj;
      const unit = SUDAR_VELICINE[q.velicina].jedinica;
      const answer = sudarOdgovor(s);
      const krivo = [...s.krivo ?? [], ...sudarKrivo(s)];
      const tol2 = isNum(q.tolerancija) ? q.tolerancija : answer === 0 ? 1e-6 * skala(s) : void 0;
      return checkNumber({
        input,
        answer,
        tol: tol2,
        unit,
        krivo,
        savjet: s.savjet || savjet2(s)
      });
    },
    tocanOdgovor(s) {
      const v = SUDAR_VELICINE[s.pitaj.velicina];
      const i = sudarTijeloPitanja(s);
      const kome = i == null ? s.zid || s.pitaj.velicina === "gubitak" ? "" : " (ukupno)" : s.zid || s.pitaj.velicina === "v_poslije" && s.vrsta === "savrseno_neelasticni" ? "" : ` (${ime(s, i)})`;
      return `${v.naziv}${kome}: ${withUnit(sudarOdgovor(s), v.jedinica)}`;
    },
    brojcaniOdgovor: (s) => s.nacin === "predvidi" && s.pitaj ? sudarOdgovor(s) : null
  };
  function klizacPocetak(s, id) {
    const t = s.tijela ?? [];
    const f2 = (i) => JEDINICE_MASE[t[i]?.jedinica] ?? 1;
    switch (id) {
      case "m1":
        return isNum(t[0]?.m) ? clean(t[0].m / f2(0)) : null;
      case "m2":
        return isNum(t[1]?.m) ? clean(t[1].m / f2(1)) : null;
      case "v1":
        return isNum(t[0]?.v) ? t[0].v : 0;
      case "v2":
        return isNum(t[1]?.v) ? t[1].v : 0;
      case "vp":
        return t.find((b) => isNum(b?.v_poslije))?.v_poslije ?? null;
      case "e":
        return isNum(s.e) ? s.e : null;
      case "dt":
        return isNum(s.trajanje) ? s.trajanje : null;
      default:
        return null;
    }
  }
  function skala(s) {
    const R2 = sudarIshod(s);
    return Math.max(
      1,
      ...R2.tijela.flatMap((t) => [
        Math.abs(t.p),
        Math.abs(t.pp),
        Math.abs(t.v),
        Math.abs(t.vp),
        t.Ek
      ])
    );
  }

  // src/lib/lesson/katalog/fizika/energija_stupci.js
  var NACINI6 = ["prikaz", "dovrsi", "istrazi"];
  var ENERGIJA_BOJE = [
    "plava",
    "crvena",
    "narancasta",
    "zelena",
    "ljubicasta",
    "siva"
  ];
  var SCENE3 = ["pad", "odskok", "tobogan"];
  var ID5 = /^[A-Za-z_][A-Za-z_0-9]*$/;
  var EPS3 = 1e-9;
  var clean2 = (x) => Number(x.toPrecision(12)) + 0;
  var aligned2 = (v, step) => Math.abs(v / step - Math.round(v / step)) < 1e-6;
  function lijepiKorak(raspon2) {
    if (!(raspon2 > 0)) return 1;
    const raw = raspon2 / 10;
    const p = Math.pow(10, Math.floor(Math.log10(raw)));
    for (const k of [1, 2, 5, 10]) if (k * p >= raw - EPS3) return clean2(k * p);
    return clean2(10 * p);
  }
  var vrijednosti = (st) => st?.vrijednosti ?? {};
  var zbroj = (st) => clean2(
    Object.values(vrijednosti(st)).reduce((a, v) => a + (isNum(v) ? v : 0), 0)
  );
  var vrhovi = (s) => (s.stanja ?? []).flatMap(
    (st) => s.slozeno ? [zbroj(st)] : Object.values(vrijednosti(st)).filter(isNum)
  );
  var energijaKorak = (s) => isNum(s.korak) ? s.korak : lijepiKorak(Math.max(...vrhovi(s).map(Math.abs), EPS3));
  var pitanoStanje = (s) => (s.stanja ?? []).findIndex((st) => st?.pitanje === true);
  function vuceSe(s) {
    const st = s.stanja?.[pitanoStanje(s)];
    if (!st) return [];
    const zad = st.zadano ?? [];
    return Object.keys(vrijednosti(st)).filter((id) => !zad.includes(id));
  }
  function energijaUkupno(s) {
    if (!s.zatvoren) return null;
    const i = pitanoStanje(s);
    const st = (s.stanja ?? []).find((_, k) => k !== i);
    return st ? zbroj(st) : null;
  }
  function energijaKorisnost(s, ulaz = null) {
    const k = s.korisnost;
    if (!k) return null;
    const i = pitanoStanje(s);
    const nadi = (id) => {
      for (let n = 0; n < (s.stanja ?? []).length; n++) {
        const v = n === i && ulaz && id in ulaz ? ulaz[id] : vrijednosti(s.stanja[n])[id];
        if (isNum(v) && Math.abs(v) > EPS3) return v;
      }
      return 0;
    };
    const u = nadi(k.ulozeno);
    return u > 0 ? nadi(k.korisno) / u : null;
  }
  var oznakaTex = (s, id) => {
    const v = (s.vrste ?? []).find((x) => x.id === id);
    if (!v) return id;
    return /[_\\]/.test(v.oznaka) ? `$${v.oznaka}$` : v.oznaka;
  };
  var naziv = (s, id) => {
    const v = (s.vrste ?? []).find((x) => x.id === id);
    return v?.naziv ? `${oznakaTex(s, id)} (${v.naziv})` : oznakaTex(s, id);
  };
  var energija_stupci = {
    tip: "energija_stupci",
    samoRucno: true,
    uloga: (s) => s.nacin === "dovrsi" ? ODGOVOR : POGLEDAJ,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Stupčasti dijagram energije: za svako stanje stupci vrsta energije na zajedničkoj mreži (Ep → Ek, Ek → Ek + toplina, uloženo → korisno
+ gubici, Q = W + ΔU, toplinski stroj Q_t = W + Q_h). Polja: tekst, nacin, jedinica? (zadano "J"), korak? (korak mreže i hvatanja; zadano okrugla
desetina najvećeg stupca), vrste: [{id (npr. "Ep"), oznaka (kratko, LaTeX bez $: "E_{gp}", "E_k", "Q", "\\\\Delta U", "Q_t"), naziv? (npr. "toplina i zvuk"),
boja? (${ENERGIJA_BOJE.join(" | ")})}] (1–5), stanja: [{oznaka (npr. "gore", "tik prije udara"), vrijednosti: {id: broj} (samo vrste koje u tom
stanju postoje; 0 se crta kao prazan stupac), pitanje?: true, zadano?: [id]}] (1–4), zatvoren?: true (ukupna energija je ista u svim stanjima;
provjerava se), slozeno?: true (svako stanje je jedan složeni stupac; samo nenegativne vrijednosti), korisnost?: {korisno: id, ulozeno: id}
(prikazuje η = korisno/uloženo; svaka od tih vrsta ima vrijednost u samo jednom stanju), poruke?: {id: poruka kad je baš taj stupac kriv}.
Negativne vrijednosti (npr. W < 0 pri kompresiji) crtaju se ispod nule. Načini:
- "prikaz": korak za gledanje; stanja se otkrivaju redom (gumb „Dalje“), stupci prelaze iz prethodnog stanja;
- "dovrsi": točno jedno stanje ima pitanje: true; učenik vuče stupce tog stanja (osim onih u zadano) na mrežu. Točno je kad je svaki stupac
  unutar pola koraka od točne vrijednosti (zato vrijednosti pitanog stanja moraju biti višekratnici koraka). Poruke za zbroj koji se ne čuva
  (zatvoren), zamijenjene stupce, stupac koji mora biti 0 i previsok/prenizak stupac daju se same;
- "istrazi": korak za gledanje sa scenom i stupcima E_gp, E_k i toplina uživo; učenik pušta tijelo i pomiče ga po putanji.
  scena: {vrsta: "pad" | "odskok" | "tobogan", m (kg), h (m), odskok_do? (m, za odskok), g? (zadano 9,81)}, klizaci?: [{id: "h" | "m", min, max, korak}].
  Tobogan ima prekidač strmi/blagi (ista brzina na dnu). Bez vrste i stanja.`,
    primjer: {
      tip: "energija_stupci",
      nacin: "dovrsi",
      jedinica: "J",
      korak: 0.8,
      zatvoren: true,
      tekst: "Kuglica mase 0,4 kg pada s 3,2 m i odbije se samo do 2 m ($g = 10$ m/s²). Povuci stupce za trenutak kad je na vrhu odskoka.",
      vrste: [
        {
          id: "Ep",
          oznaka: "E_{gp}",
          naziv: "gravitacijska potencijalna",
          boja: "plava"
        },
        { id: "Ek", oznaka: "E_k", naziv: "kinetička", boja: "crvena" },
        { id: "Q", oznaka: "Q", naziv: "toplina i zvuk", boja: "narancasta" }
      ],
      stanja: [
        { oznaka: "puštena", vrijednosti: { Ep: 12.8, Ek: 0, Q: 0 } },
        { oznaka: "tik prije udara", vrijednosti: { Ep: 0, Ek: 12.8, Q: 0 } },
        {
          oznaka: "vrh odskoka",
          vrijednosti: { Ep: 8, Ek: 0, Q: 4.8 },
          pitanje: true
        }
      ],
      poruke: {
        Ek: "Na vrhu odskoka kuglica na trenutak miruje, pa je $E_k = 0$."
      }
    },
    primjeri: [
      {
        tip: "energija_stupci",
        nacin: "prikaz",
        zatvoren: true,
        tekst: "Kuglica (0,2 kg) pada s 1,8 m. Klikni „Dalje“ i gledaj kako potencijalna energija prelazi u kinetičku.",
        vrste: [
          {
            id: "Ep",
            oznaka: "E_{gp}",
            naziv: "gravitacijska potencijalna",
            boja: "plava"
          },
          { id: "Ek", oznaka: "E_k", naziv: "kinetička", boja: "crvena" }
        ],
        stanja: [
          { oznaka: "gore", vrijednosti: { Ep: 3.6, Ek: 0 } },
          { oznaka: "na pola puta", vrijednosti: { Ep: 1.8, Ek: 1.8 } },
          { oznaka: "tik prije tla", vrijednosti: { Ep: 0, Ek: 3.6 } }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "dovrsi",
        korak: 600,
        zatvoren: true,
        tekst: "Motor dizala iz mreže uzme 9000 J, a ormar dobije 7200 J potencijalne energije. Povuci stupac gubitaka.",
        vrste: [
          {
            id: "E",
            oznaka: "E_{el}",
            naziv: "iz mreže (uloženo)",
            boja: "ljubicasta"
          },
          { id: "Ep", oznaka: "E_{gp}", naziv: "ormar (korisno)", boja: "plava" },
          {
            id: "Q",
            oznaka: "Q",
            naziv: "gubici (toplina, trenje)",
            boja: "narancasta"
          }
        ],
        stanja: [
          { oznaka: "uloženo", vrijednosti: { E: 9e3 } },
          {
            oznaka: "kamo ode",
            vrijednosti: { Ep: 7200, Q: 1800 },
            pitanje: true,
            zadano: ["Ep"]
          }
        ],
        korisnost: { korisno: "Ep", ulozeno: "E" }
      },
      {
        tip: "energija_stupci",
        nacin: "prikaz",
        zatvoren: true,
        tekst: "Vagoni se spoje: kinetička energija prije i poslije sudara.",
        vrste: [
          { id: "Ek", oznaka: "E_k", naziv: "kinetička", boja: "crvena" },
          {
            id: "Q",
            oznaka: "Q",
            naziv: "toplina, zvuk, deformacija",
            boja: "narancasta"
          }
        ],
        stanja: [
          { oznaka: "prije sudara", vrijednosti: { Ek: 16e3, Q: 0 } },
          { oznaka: "poslije sudara", vrijednosti: { Ek: 6400, Q: 9600 } }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "prikaz",
        tekst: "Puška i metak imaju jednake količine gibanja, ali ne i kinetičke energije.",
        vrste: [
          { id: "metak", oznaka: "E_{k}", naziv: "metak", boja: "crvena" },
          { id: "puska", oznaka: "E_{k}", naziv: "puška", boja: "plava" }
        ],
        stanja: [
          { oznaka: "nakon pucnja", vrijednosti: { metak: 1800, puska: 4.5 } }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "dovrsi",
        korak: 50,
        zatvoren: true,
        slozeno: true,
        tekst: "Plinu je dovedeno 800 J topline, a plin je obavio rad 300 J. Povuci gornji dio stupca: koliko je porasla unutarnja energija?",
        vrste: [
          { id: "Q", oznaka: "Q", naziv: "dovedena toplina", boja: "narancasta" },
          { id: "W", oznaka: "W", naziv: "rad plina", boja: "zelena" },
          {
            id: "dU",
            oznaka: "\\Delta U",
            naziv: "promjena unutarnje energije",
            boja: "crvena"
          }
        ],
        stanja: [
          { oznaka: "dovedeno", vrijednosti: { Q: 800 } },
          {
            oznaka: "kamo ode",
            vrijednosti: { W: 300, dU: 500 },
            pitanje: true,
            zadano: ["W"]
          }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "dovrsi",
        korak: 100,
        zatvoren: true,
        slozeno: true,
        tekst: "Stroj u ciklusu obavi rad 800 J i preda hladnom spremniku 1200 J. Povuci stupac topline iz toplog spremnika.",
        vrste: [
          {
            id: "Qt",
            oznaka: "Q_t",
            naziv: "iz toplog spremnika",
            boja: "crvena"
          },
          { id: "W", oznaka: "W", naziv: "rad", boja: "zelena" },
          { id: "Qh", oznaka: "Q_h", naziv: "u hladni spremnik", boja: "plava" }
        ],
        stanja: [
          { oznaka: "primljeno", vrijednosti: { Qt: 2e3 }, pitanje: true },
          { oznaka: "predano", vrijednosti: { W: 800, Qh: 1200 } }
        ],
        korisnost: { korisno: "W", ulozeno: "Qt" }
      },
      {
        tip: "energija_stupci",
        nacin: "prikaz",
        zatvoren: true,
        slozeno: true,
        tekst: "Grijač preda 68 400 J: većinu dobije voda, a dio aluminijska posuda.",
        vrste: [
          { id: "E", oznaka: "E_{el}", naziv: "grijač", boja: "ljubicasta" },
          { id: "Qv", oznaka: "Q_v", naziv: "voda", boja: "plava" },
          { id: "Qp", oznaka: "Q_p", naziv: "posuda", boja: "siva" }
        ],
        stanja: [
          { oznaka: "grijač daje", vrijednosti: { E: 68400 } },
          { oznaka: "primaju", vrijednosti: { Qv: 63e3, Qp: 5400 } }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "istrazi",
        tekst: "Pusti kuglicu i povlači klizač položaja. Promijeni visinu i masu: što se događa s brzinom pri tlu?",
        scena: { vrsta: "pad", m: 0.2, h: 1.8, g: 10 },
        klizaci: [
          { id: "h", min: 0.5, max: 5, korak: 0.1 },
          { id: "m", min: 0.1, max: 1, korak: 0.1 }
        ]
      },
      {
        tip: "energija_stupci",
        nacin: "istrazi",
        tekst: "Pusti kuglicu da padne i odbije se. Pomiči klizač položaja i gledaj kamo ode dio energije pri udaru.",
        scena: { vrsta: "odskok", m: 0.2, h: 1.8, odskok_do: 1.3, g: 10 }
      },
      {
        tip: "energija_stupci",
        nacin: "istrazi",
        tekst: "Pusti dijete niz strmi pa niz blagi tobogan iste visine. Promijeni i masu: ovisi li brzina u dnu o obliku ili masi?",
        scena: { vrsta: "tobogan", m: 30, h: 3.2, g: 10 },
        klizaci: [{ id: "m", min: 20, max: 80, korak: 5 }]
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI6.includes(s.nacin),
        `energija_stupci.nacin mora biti ${NACINI6.join(", ")}`
      );
      need(isText(s.tekst), "energija_stupci treba „tekst“");
      if (s.jedinica != null)
        need(
          isText(s.jedinica) && s.jedinica.length <= 6,
          "jedinica mora biti kratak tekst (npr. J, kJ)"
        );
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        for (const f2 of [
          "vrste",
          "stanja",
          "pitanje",
          "korisnost",
          "poruke",
          "slozeno",
          "zatvoren"
        ])
          need(
            !(f2 in s),
            `istrazi crta svoje stupce (E_gp, E_k, toplina): makni „${f2}“`
          );
        const sc = s.scena;
        if (!need(
          sc && SCENE3.includes(sc.vrsta),
          `istrazi treba scena: {vrsta: ${SCENE3.join(" | ")}, m, h}`
        ))
          return list;
        need(
          isNum(sc.m) && sc.m > 0 && sc.m <= 1e5,
          "scena.m mora biti masa u kg (> 0)"
        );
        need(
          isNum(sc.h) && sc.h > 0 && sc.h <= 1e3,
          "scena.h mora biti visina u m (0 < h ≤ 1000)"
        );
        if (sc.g != null)
          need(
            isNum(sc.g) && sc.g > 0 && sc.g <= 30,
            "scena.g mora biti broj (npr. 9,81 ili 10)"
          );
        if (sc.vrsta === "odskok")
          need(
            isNum(sc.odskok_do) && sc.odskok_do > 0 && sc.odskok_do < sc.h,
            "odskok treba odskok_do između 0 i h"
          );
        else need(sc.odskok_do == null, "odskok_do postoji samo uz vrsta odskok");
        if (s.klizaci != null && need(
          Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 2,
          "klizaci mora biti popis 1–2 klizača (h, m)"
        )) {
          const ids2 = [];
          s.klizaci.forEach((k, i) => {
            const at = `klizaci[${i}]`;
            if (!need(
              k?.id === "h" || k?.id === "m",
              `${at}.id mora biti "h" ili "m"`
            ))
              return;
            ids2.push(k.id);
            if (!need(
              [k.min, k.max, k.korak].every(isNum) && k.min > 0 && k.max > k.min && k.korak > 0,
              `${at} treba brojeve 0 < min < max i korak > 0`
            ))
              return;
            need(
              (k.max - k.min) / k.korak <= 400,
              `${at} ima previše koraka (najviše 400)`
            );
            const cur = sc[k.id];
            need(
              isNum(cur) && cur >= k.min - EPS3 && cur <= k.max + EPS3,
              `${at}: scena.${k.id} mora biti između min i max`
            );
            if (k.id === "h" && sc.vrsta === "odskok")
              need(
                isNum(sc.odskok_do) && sc.odskok_do < k.min,
                `${at}: najmanja visina mora biti veća od odskok_do`
              );
          });
          need(
            new Set(ids2).size === ids2.length,
            "id-jevi klizača moraju biti različiti"
          );
        }
        return list;
      }
      need(
        s.scena == null && s.klizaci == null,
        "scena i klizaci postoje samo u načinu istrazi"
      );
      const ids = [];
      if (!need(
        Array.isArray(s.vrste) && s.vrste.length >= 1 && s.vrste.length <= 5,
        "energija_stupci treba vrste: 1–5 vrsta energije"
      ))
        return list;
      s.vrste.forEach((v, i) => {
        const at = `vrste[${i}]`;
        need(
          ID5.test(v?.id ?? ""),
          `${at}.id mora biti ime bez razmaka (npr. "Ep")`
        );
        ids.push(v?.id);
        need(
          isText(v?.oznaka) && v.oznaka.length <= 16 && !v.oznaka.includes("$"),
          `${at}.oznaka mora biti kratka oznaka bez $ (npr. "E_k", "\\\\Delta U")`
        );
        if (v?.naziv != null)
          need(
            isText(v.naziv) && v.naziv.length <= 40,
            `${at}.naziv mora biti kratak tekst`
          );
        if (v?.boja != null)
          need(
            ENERGIJA_BOJE.includes(v.boja),
            `${at}.boja mora biti jedna od: ${ENERGIJA_BOJE.join(", ")}`
          );
      });
      need(
        new Set(ids).size === ids.length,
        "id-jevi vrsta moraju biti različiti"
      );
      if (!need(
        Array.isArray(s.stanja) && s.stanja.length >= 1 && s.stanja.length <= 4,
        "energija_stupci treba stanja: 1–4 stanja"
      ))
        return list;
      let ok = true;
      s.stanja.forEach((st, i) => {
        const at = `stanja[${i}]`;
        need(
          isText(st?.oznaka) && st.oznaka.length <= 28,
          `${at}.oznaka mora biti kratak tekst (do 28 znakova)`
        );
        const v = st?.vrijednosti;
        if (!(ok = need(
          v && typeof v === "object" && !Array.isArray(v) && Object.keys(v).length >= 1,
          `${at}.vrijednosti mora biti {id: broj} s barem jednom vrstom`
        ) && ok))
          return;
        for (const [id, x] of Object.entries(v)) {
          ok = need(
            ids.includes(id),
            `${at}.vrijednosti: nepoznata vrsta „${id}“`
          ) && ok;
          ok = need(isNum(x), `${at}.vrijednosti.${id} mora biti broj`) && ok;
          if (s.slozeno && isNum(x))
            ok = need(
              x >= 0,
              `${at}.vrijednosti.${id}: složeni stupac ne može imati negativan dio`
            ) && ok;
        }
        if (st.pitanje != null)
          need(st.pitanje === true, `${at}.pitanje je true ili ga nema`);
        if (st.zadano != null) {
          need(st.pitanje === true, `${at}.zadano postoji samo u pitanom stanju`);
          need(
            Array.isArray(st.zadano) && st.zadano.every((id) => id in v),
            `${at}.zadano mora biti popis vrsta iz tog stanja`
          );
        }
      });
      if (!ok) return list;
      if (s.slozeno != null)
        need(typeof s.slozeno === "boolean", "slozeno je true ili false");
      if (s.zatvoren != null)
        need(typeof s.zatvoren === "boolean", "zatvoren je true ili false");
      const korak = energijaKorak(s);
      if (s.korak != null)
        need(isNum(s.korak) && s.korak > 0, "korak mora biti pozitivan broj");
      const vrh = Math.max(...vrhovi(s).map(Math.abs), EPS3);
      if (korak > 0)
        need(
          vrh / korak <= 40,
          `korak ${formatNumber(korak)} je premalen za stupce do ${formatNumber(vrh)} (najviše 40 koraka mreže)`
        );
      if (s.zatvoren && s.stanja.length >= 2) {
        const z = s.stanja.map(zbroj);
        need(
          z.every(
            (x) => Math.abs(x - z[0]) <= Math.max(EPS3, Math.abs(z[0]) * 5e-3)
          ),
          `zatvoren: ukupna energija mora biti ista u svim stanjima, a zbrojevi su ${z.map((x) => formatNumber(x)).join(", ")}`
        );
      }
      if (s.zatvoren)
        need(s.stanja.length >= 2, "zatvoren ima smisla tek uz barem dva stanja");
      if (s.korisnost != null) {
        const k = s.korisnost;
        if (need(
          k && ids.includes(k.korisno) && ids.includes(k.ulozeno) && k.korisno !== k.ulozeno,
          "korisnost: {korisno: id, ulozeno: id} – dvije različite vrste"
        )) {
          for (const id of [k.korisno, k.ulozeno]) {
            const n = s.stanja.filter(
              (st) => isNum(st.vrijednosti[id]) && Math.abs(st.vrijednosti[id]) > EPS3
            ).length;
            need(
              n === 1,
              `korisnost: vrsta „${id}“ mora imati vrijednost (≠ 0) u točno jednom stanju`
            );
          }
          const eta = energijaKorisnost(s);
          need(
            eta != null && eta >= 0 && eta <= 1 + EPS3,
            "korisnost mora biti između 0 i 100 % (uloženo > 0, korisno ≤ uloženo)"
          );
        }
      }
      if (s.poruke != null) {
        if (need(
          typeof s.poruke === "object" && !Array.isArray(s.poruke),
          "poruke mora biti {id: poruka}"
        ))
          for (const [id, p] of Object.entries(s.poruke)) {
            need(ids.includes(id), `poruke: nepoznata vrsta „${id}“`);
            need(isText(p), `poruke.${id} mora biti tekst`);
          }
      }
      const pitana = s.stanja.filter((st) => st.pitanje === true).length;
      if (s.nacin === "prikaz") {
        noAnswerFields(s, need);
        need(
          pitana === 0,
          "prikaz je za gledanje: nijedno stanje ne smije imati pitanje"
        );
        need(s.poruke == null, "poruke postoje samo u načinu dovrsi");
      }
      if (s.nacin === "dovrsi") {
        if (!need(pitana === 1, "dovrsi treba točno jedno stanje s pitanje: true"))
          return list;
        const st = s.stanja[pitanoStanje(s)];
        const vuku = vuceSe(s);
        need(
          vuku.length >= 1,
          "u pitanom stanju mora ostati barem jedan stupac za vući (nije sve u zadano)"
        );
        need(
          vuku.some((id) => Math.abs(st.vrijednosti[id]) > EPS3),
          "barem jedan stupac za vući mora biti različit od 0 (stupci kreću od 0, pa bi bez vučenja bilo točno)"
        );
        for (const id of vuku)
          need(
            aligned2(st.vrijednosti[id], korak),
            `vrijednost ${id} = ${formatNumber(st.vrijednosti[id])} nije višekratnik koraka ${formatNumber(korak)}: učenik je ne može točno namjestiti`
          );
      }
      return list;
    },
    // odgovor: {id: vrijednost} za stupce koje učenik vuče
    check(s, odgovor3) {
      const i = pitanoStanje(s);
      const st = s.stanja[i];
      const vuku = vuceSe(s);
      const korak = energijaKorak(s);
      const tol2 = korak / 2 + EPS3 * Math.max(1, korak);
      const o = odgovor3 && typeof odgovor3 === "object" ? odgovor3 : {};
      const x = (id2) => isNum(o[id2]) ? o[id2] : 0;
      const t = (id2) => st.vrijednosti[id2];
      if (vuku.every((id2) => x(id2) === 0))
        return {
          tocno: false,
          poruka: "Povuci stupce stanja „" + st.oznaka + "“ na pravu visinu.",
          prazno: true
        };
      const krivi2 = vuku.filter((id2) => Math.abs(x(id2) - t(id2)) > tol2);
      if (!krivi2.length) return { tocno: true };
      const J = s.jedinica ?? "J";
      const ukupno = energijaUkupno(s);
      if (ukupno != null) {
        const S = clean2(
          Object.keys(st.vrijednosti).reduce(
            (a, id2) => a + (vuku.includes(id2) ? x(id2) : t(id2)),
            0
          )
        );
        if (Math.abs(S - ukupno) > tol2)
          return {
            tocno: false,
            poruka: `Ukupna energija se ne mijenja: zbroj stupaca u stanju „${st.oznaka}“ mora biti ${withUnit(ukupno, J)}, a sad je ${withUnit(S, J)}.`
          };
      }
      for (const a of krivi2)
        for (const b of krivi2) {
          if (a < b && Math.abs(x(a) - t(b)) <= tol2 && Math.abs(x(b) - t(a)) <= tol2)
            return {
              tocno: false,
              poruka: `Zamijenio si ${oznakaTex(s, a)} i ${oznakaTex(s, b)}.`
            };
        }
      const id = krivi2[0];
      if (s.poruke?.[id]) return { tocno: false, poruka: s.poruke[id] };
      if (Math.abs(t(id)) <= EPS3)
        return {
          tocno: false,
          poruka: `Stupac ${naziv(s, id)} u stanju „${st.oznaka}“ mora biti 0.`
        };
      if (Math.abs(x(id)) <= EPS3)
        return {
          tocno: false,
          poruka: `Stupac ${naziv(s, id)} u stanju „${st.oznaka}“ nije 0: povuci i njega.`
        };
      return {
        tocno: false,
        poruka: `Stupac ${naziv(s, id)} je ${x(id) > t(id) ? "previsok" : "prenizak"}.`
      };
    },
    tocanOdgovor(s) {
      const st = s.stanja?.[pitanoStanje(s)];
      if (!st) return "";
      const J = s.jedinica ?? "J";
      return vuceSe(s).map((id) => `${oznakaTex(s, id)} = ${withUnit(st.vrijednosti[id], J)}`).join(", ");
    },
    brojcaniOdgovor(s) {
      if (s.nacin !== "dovrsi") return null;
      const v = vuceSe(s);
      return v.length === 1 ? s.stanja[pitanoStanje(s)].vrijednosti[v[0]] : null;
    }
  };

  // src/lib/lesson/katalog/fizika/_fluidi.js
  var G2 = 9.81;
  var gOf = (s) => isNum(s?.g) ? s.g : G2;
  var JEDINICE = {
    tlak: { Pa: 1, hPa: 100, kPa: 1e3, MPa: 1e6 },
    sila: { N: 1, kN: 1e3 },
    brzina: { "m/s": 1 },
    protok: { "m³/s": 1, "L/s": 1e-3, "L/min": 1e-3 / 60 },
    vrijeme: { s: 1, min: 60 },
    energija: { J: 1, kJ: 1e3, MJ: 1e6 },
    temperatura: { "°C": 1 },
    duljina: { m: 1, cm: 0.01 }
  };
  var jedinicaOk = (velicina3, jed2) => typeof jed2 === "string" && jed2 in (JEDINICE[velicina3] ?? {});
  var uJedinici = (si, velicina3, jed2) => si / JEDINICE[velicina3][jed2];
  function krivoJedinica(si, velicina3, jed2) {
    return Object.entries(JEDINICE[velicina3] ?? {}).filter(([u]) => u !== jed2).map(([u, f2]) => ({
      vrijednost: si / f2,
      poruka: `To je iznos u ${u}, a odgovor treba upisati u ${jed2}.`
    }));
  }
  function krivoU(lista, velicina3, jed2, tocno) {
    const out = [];
    for (const k of lista) {
      if (!isNum(k.si)) continue;
      const v = uJedinici(k.si, velicina3, jed2);
      if (Math.abs(v - tocno) <= Math.abs(tocno) * 6e-3 + 1e-12) continue;
      if (out.some((o) => Math.abs(o.vrijednost - v) <= Math.abs(v) * 1e-6 + 1e-12))
        continue;
      out.push({ vrijednost: v, poruka: k.poruka });
    }
    return out;
  }
  function nacinOk(s, need, nacini, tip) {
    return need(
      nacini.includes(s.nacin),
      `${tip}.nacin mora biti jedan od: ${nacini.join(", ")}`
    );
  }
  var pozitivan = (v) => isNum(v) && v > 0;
  function fmtBroj(n, sig = 4) {
    if (!isNum(n)) return "—";
    if (n === 0) return "0";
    const r = Number(n.toPrecision(sig));
    const t = formatNumber(
      r,
      Math.max(0, sig - 1 - Math.floor(Math.log10(Math.abs(r))))
    );
    const [cijeli, dec] = t.split(",");
    const znak2 = cijeli.startsWith("−") ? "−" : "";
    const z = cijeli.replace("−", "");
    const grupirano = z.length > 4 ? z.replace(/\B(?=(\d{3})+(?!\d))/g, " ") : z;
    return `${znak2}${grupirano}${dec ? `,${dec}` : ""}`;
  }
  var fmtJ = (n, jed2, sig = 4) => `${fmtBroj(n, sig)}${jed2 ? ` ${jed2}` : ""}`;

  // src/lib/lesson/katalog/fizika/tlak_dubina.js
  var TIP = "tlak_dubina";
  var NACINI7 = ["istrazi", "izmjeri", "postavi", "usporedi"];
  var OBLICI = [
    "valjak",
    "siroka",
    "uska",
    "lijevak",
    "obrnuti_lijevak",
    "kosa",
    "boca"
  ];
  var VELICINE = { p: "tlak", F_u: "sila", F_gore: "sila", F_dolje: "sila" };
  function posude(s) {
    return (s.posude ?? []).map((p) => {
      const t = p?.tekucina ?? s.tekucina ?? {};
      return {
        oblik: p?.oblik,
        h: isNum(p?.h) ? p.h : s.h,
        rho: t.gustoca,
        tekucina: t.naziv
      };
    });
  }
  var p0Of = (s) => isNum(s.p0) ? s.p0 : 0;
  var tlak = (s, i, d) => p0Of(s) + posude(s)[i].rho * gOf(s) * d;
  var tlakDno = (s, i) => tlak(s, i, posude(s)[i].h);
  function tijelo(s) {
    const t = s.tijelo;
    if (!t) return null;
    const V = isNum(t.volumen) ? t.volumen / 1e3 : t.stranica ** 3;
    const a = isNum(t.stranica) ? t.stranica : Math.cbrt(V);
    return { a, V, S: a * a, d: isNum(t.dubina_vrha) ? t.dubina_vrha : 0 };
  }
  function sileTijela(s, d) {
    const k = tijelo(s);
    const rho = posude(s)[0].rho;
    const g = gOf(s);
    const p1 = rho * g * d;
    const p2 = rho * g * (d + k.a);
    return {
      p1,
      p2,
      F1: p1 * k.S,
      F2: p2 * k.S,
      Fu: (p2 - p1) * k.S,
      Fu_V: rho * g * k.V
    };
  }
  var velicina = (s) => s.pitaj?.velicina ?? "p";
  var jedinica = (s) => s.jedinica ?? (VELICINE[velicina(s)] === "tlak" ? "Pa" : "N");
  var posudaPitanja = (s) => Number.isInteger(s.pitaj?.posuda) ? s.pitaj.posuda : 0;
  function izmjeriSI(s) {
    const v = velicina(s);
    if (v === "p") return tlak(s, posudaPitanja(s), s.pitaj.dubina);
    const f2 = sileTijela(s, tijelo(s).d);
    return v === "F_u" ? f2.Fu_V : v === "F_gore" ? f2.F1 : f2.F2;
  }
  var dubinaCilja = (s) => (s.cilj - p0Of(s)) / (posude(s)[posudaPitanja(s)].rho * gOf(s));
  function korakSonde(s) {
    if (isNum(s.korak)) return s.korak;
    const H2 = Math.max(...posude(s).map((p) => p.h));
    return [5e-3, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5].find((k) => H2 / k <= 60) ?? 1;
  }
  function najveciTlak(s) {
    const p = posude(s).map((_, i) => tlakDno(s, i));
    const max = Math.max(...p);
    if (p.every((x) => near(x, max, max * 1e-9))) return "jednako";
    return p.indexOf(max);
  }
  var tolOf = (s, answer) => isNum(s.tolerancija) ? s.tolerancija : Math.abs(answer) * 5e-3;
  function validate(s) {
    const { list, need } = problems();
    nacinOk(s, need, NACINI7, TIP);
    need(isText(s.tekst), `${TIP} treba „tekst“`);
    need(
      pozitivan(s.h) && s.h <= 100,
      `${TIP}.h (visina tekućine u m) mora biti pozitivan broj do 100`
    );
    const tekOk = (t, gdje) => need(
      isText(t?.naziv) && pozitivan(t?.gustoca) && t.gustoca >= 500 && t.gustoca <= 22e3,
      `${gdje} treba {naziv, gustoca} uz gustoću u kg/m³ (500–22 000; voda je 1000)`
    );
    if (s.tekucina != null || !(s.posude ?? []).every((p) => p?.tekucina))
      tekOk(s.tekucina, `${TIP}.tekucina`);
    if (s.g != null)
      need(
        isNum(s.g) && s.g >= 9.7 && s.g <= 10,
        "g mora biti između 9,7 i 10 m/s² (9,81 ili 10)"
      );
    if (s.p0 != null)
      need(
        isNum(s.p0) && s.p0 >= 0 && s.p0 <= 2e5,
        "p0 (tlak iznad tekućine, Pa) mora biti između 0 i 200 000 (atmosferski ≈ 101 325)"
      );
    if (s.korak != null)
      need(
        pozitivan(s.korak) && s.korak <= (s.h ?? 1) / 4,
        "korak (sonde, m) mora biti pozitivan i barem 4 puta manji od h"
      );
    if (!need(
      Array.isArray(s.posude) && s.posude.length >= 1 && s.posude.length <= 3,
      `${TIP} treba 1–3 posude`
    ))
      return list;
    s.posude.forEach((p, i) => {
      need(
        OBLICI.includes(p?.oblik),
        `posude[${i}].oblik mora biti jedan od: ${OBLICI.join(", ")}`
      );
      if (p?.h != null)
        need(
          pozitivan(p.h) && p.h <= 100,
          `posude[${i}].h mora biti pozitivan broj (m)`
        );
      if (p?.tekucina != null) tekOk(p.tekucina, `posude[${i}].tekucina`);
    });
    if (list.length) return list;
    const ps = posude(s);
    const H2 = Math.max(...ps.map((p) => p.h));
    need(
      ps.every((p) => p.h >= H2 / 4),
      "visine tekućine u posudama smiju se razlikovati najviše 4 puta, inače se crtež ne vidi"
    );
    if (s.spojene != null) {
      need(typeof s.spojene === "boolean", "spojene mora biti true ili false");
      if (s.spojene) {
        need(ps.length >= 2, "spojene posude trebaju barem 2 posude");
        need(
          ps.every((p) => p.h === ps[0].h && p.rho === ps[0].rho),
          "u spojenim posudama s istom tekućinom razina je svugdje ista: izostavi posude[].h i posude[].tekucina"
        );
      }
    }
    if (s.tijelo != null) {
      const t = s.tijelo;
      need(
        ps.length === 1,
        "tijelo (uronjena kocka) može biti samo uz jednu posudu"
      );
      if (need(
        pozitivan(t.stranica) !== pozitivan(t.volumen),
        "tijelo treba točno jedno od: stranica (m) ili volumen (dm³)"
      )) {
        const k = tijelo(s);
        need(
          k.a <= s.h / 2,
          `kocka stranice ${formatNumber(k.a)} m prevelika je za tekućinu dubine ${formatNumber(s.h)} m (najviše pola dubine)`
        );
        if (t.dubina_vrha != null)
          need(
            isNum(t.dubina_vrha) && t.dubina_vrha >= 0 && t.dubina_vrha + k.a <= s.h + 1e-9,
            "tijelo.dubina_vrha mora biti ≥ 0, a kocka cijela u tekućini (dubina_vrha + stranica ≤ h)"
          );
      }
      if (t.gustoca != null)
        need(
          pozitivan(t.gustoca),
          "tijelo.gustoca (kg/m³) mora biti pozitivan broj"
        );
    }
    const pitajPosudu = () => {
      if (s.pitaj?.posuda != null)
        return need(
          Number.isInteger(s.pitaj.posuda) && s.pitaj.posuda >= 0 && s.pitaj.posuda < ps.length,
          "pitaj.posuda mora biti indeks posude (od 0)"
        );
      return true;
    };
    if (s.nacin === "istrazi") {
      noAnswerFields(s, need);
      need(s.pitaj == null, "istrazi nema polje pitaj");
    }
    if (s.nacin === "izmjeri") {
      const v = velicina(s);
      if (!need(
        v in VELICINE,
        `pitaj.velicina mora biti jedna od: ${Object.keys(VELICINE).join(", ")}`
      ))
        return list;
      need(
        jedinicaOk(VELICINE[v], jedinica(s)),
        `jedinica za ${v} mora biti jedna od: ${Object.keys(JEDINICE[VELICINE[v]]).join(", ")}`
      );
      if (v === "p") {
        if (pitajPosudu() && need(
          isNum(s.pitaj?.dubina),
          "izmjeri treba pitaj.dubina (m ispod površine)"
        )) {
          const h = ps[posudaPitanja(s)].h;
          need(
            s.pitaj.dubina > 0 && s.pitaj.dubina <= h + 1e-9,
            `pitaj.dubina mora biti u tekućini (0 < dubina ≤ ${formatNumber(h)} m)`
          );
        }
        need(s.tijelo == null, "izmjeri tlaka ne treba tijelo");
      } else {
        need(s.tijelo != null, `${v} se računa za uronjenu kocku: dodaj tijelo`);
        if (v !== "F_u")
          need(
            isNum(s.tijelo?.dubina_vrha),
            `${v} ovisi o dubini: zadaj tijelo.dubina_vrha`
          );
      }
      if (!list.length)
        validateKrivo(
          s.krivo,
          izmjeriOdgovor(s),
          tolOf(s, izmjeriOdgovor(s)),
          need
        );
    }
    if (s.nacin === "postavi") {
      need(s.tijelo == null, "postavi ne koristi tijelo");
      if (pitajPosudu() && need(
        pozitivan(s.cilj),
        "postavi treba cilj: tlak u Pa na koji učenik spušta sondu"
      )) {
        const d = dubinaCilja(s);
        const h = ps[posudaPitanja(s)].h;
        const k = korakSonde(s);
        need(
          d > 0 && d <= h + 1e-9,
          `tlak ${formatNumber(s.cilj)} Pa nije u tekućini: odgovara dubini ${formatNumber(d)} m, a tekućina je duboka ${formatNumber(h)} m`
        );
        need(
          Math.abs(d / k - Math.round(d / k)) < 1e-6,
          `dubina cilja ${formatNumber(d)} m nije na koraku sonde (${formatNumber(k)} m); odaberi cilj kojemu je dubina višekratnik koraka ili zadaj korak`
        );
      }
    }
    if (s.nacin === "usporedi") {
      need(ps.length >= 2, "usporedi treba barem 2 posude");
      need(s.tijelo == null, "usporedi ne koristi tijelo");
      const p = ps.map((_, i) => tlakDno(s, i));
      const max = Math.max(...p);
      const najvecih = p.filter((x) => near(x, max, max * 1e-9)).length;
      need(
        najvecih === 1 || najvecih === p.length,
        "najveći tlak na dnu mora biti u jednoj posudi ili jednak u svima"
      );
    }
    return list;
  }
  var izmjeriOdgovor = (s) => uJedinici(izmjeriSI(s), VELICINE[velicina(s)], jedinica(s));
  function izmjeriKrivo(s) {
    const v = velicina(s);
    const g = gOf(s);
    const druga = g === 10 ? 9.81 : 10;
    const gTekst = (x) => formatNumber(x);
    const out = [];
    if (v === "p") {
      const i = posudaPitanja(s);
      const { rho, h } = posude(s)[i];
      const d = s.pitaj.dubina;
      const p0 = p0Of(s);
      out.push({
        si: p0 + rho * d,
        poruka: "Zaboravio si g: hidrostatički tlak je p = ρ·g·h."
      });
      out.push({
        si: p0 + rho * g * d * 100,
        poruka: "Dubinu uvrsti u metrima, a ne u centimetrima."
      });
      out.push({
        si: p0 + rho * druga * d,
        poruka: `U ovom zadatku uzmi g = ${gTekst(g)} m/s².`
      });
      if (p0)
        out.push({
          si: rho * g * d,
          poruka: "To je samo tlak stupca tekućine. Traži se ukupni tlak: dodaj tlak iznad površine p₀."
        });
      if (d < h)
        out.push({
          si: p0 + rho * g * (h - d),
          poruka: "Dubina se mjeri od površine tekućine prema dolje, a ne od dna."
        });
      if (d < h)
        out.push({
          si: p0 + rho * g * h,
          poruka: "To je tlak na dnu. Traži se tlak na označenoj dubini."
        });
      posude(s).forEach((p, j) => {
        if (j !== i && p.rho !== rho)
          out.push({
            si: p0 + p.rho * g * d,
            poruka: `To je gustoća tekućine u drugoj posudi (${p.tekucina}). Uzmi gustoću tekućine u kojoj je točka.`
          });
      });
    } else {
      const k = tijelo(s);
      const rho = posude(s)[0].rho;
      const f2 = sileTijela(s, k.d);
      if (v === "F_u") {
        out.push({ si: rho * k.V, poruka: "Zaboravio si g: F_u = ρ·g·V." });
        out.push({
          si: rho * g * k.V * 1e3,
          poruka: "Volumen pretvori u m³ (1 dm³ = 0,001 m³)."
        });
        out.push({
          si: rho * druga * k.V,
          poruka: `U ovom zadatku uzmi g = ${gTekst(g)} m/s².`
        });
        if (pozitivan(s.tijelo.gustoca))
          out.push({
            si: s.tijelo.gustoca * g * k.V,
            poruka: "To je težina tijela. Uzgon ovisi o gustoći tekućine, a ne tijela."
          });
        if (k.d > 0)
          out.push({
            si: f2.F2,
            poruka: "To je samo sila na donju plohu. Uzgon je razlika: sila odozdo minus sila odozgo."
          });
      } else {
        const [moja, tuda] = v === "F_gore" ? [f2.p1, f2.p2] : [f2.p2, f2.p1];
        out.push({
          si: tuda * k.S,
          poruka: v === "F_gore" ? "To je sila na donju plohu (dubina gornje plohe + stranica)." : "To je sila na gornju plohu. Donja ploha je dublje za cijelu stranicu kocke."
        });
        out.push({
          si: moja,
          poruka: "To je tlak. Sila je tlak puta površina plohe: F = p·S."
        });
        out.push({
          si: moja / g * k.S,
          poruka: "Zaboravio si g u tlaku p = ρ·g·h."
        });
      }
    }
    return out;
  }
  function check(s, odgovor3) {
    if (s.nacin === "usporedi") {
      if (odgovor3 !== "jednako" && !Number.isInteger(odgovor3))
        return {
          tocno: false,
          poruka: "Dodirni posudu ili „Jednako u svima“.",
          prazno: true
        };
      const tocno = najveciTlak(s);
      if (odgovor3 === tocno) return { tocno: true };
      if (tocno === "jednako")
        return {
          tocno: false,
          poruka: "Tlak na dnu ovisi samo o dubini i gustoći tekućine, a ne o obliku posude ni o količini tekućine. Ovdje je u svim posudama jednak."
        };
      if (odgovor3 === "jednako")
        return {
          tocno: false,
          poruka: "Posude se razlikuju po dubini ili po tekućini. Usporedi ρ·h za svaku posudu."
        };
      return {
        tocno: false,
        poruka: "Usporedi ρ·g·h za svaku posudu: oblik i količina tekućine ne utječu na tlak, samo dubina i gustoća."
      };
    }
    if (s.nacin === "postavi") {
      if (!isNum(odgovor3))
        return {
          tocno: false,
          poruka: "Povuci sondu na traženu dubinu.",
          prazno: true
        };
      const d = dubinaCilja(s);
      const k = korakSonde(s);
      if (Math.abs(odgovor3 - d) <= k * 0.25) return { tocno: true };
      const h = posude(s)[posudaPitanja(s)].h;
      if (Math.abs(odgovor3 - (h - d)) <= k * 0.25)
        return {
          tocno: false,
          poruka: "Dubina se mjeri od površine tekućine prema dolje, a ne od dna."
        };
      const g = gOf(s);
      if (Math.abs(
        odgovor3 - (s.cilj - p0Of(s)) / posude(s)[posudaPitanja(s)].rho
      ) <= k * 0.25)
        return { tocno: false, poruka: "Zaboravio si g: h = p / (ρ·g)." };
      return {
        tocno: false,
        poruka: `Na dubini ${formatNumber(odgovor3)} m tlak je ${odgovor3 < d ? "manji" : "veći"} od ${fmtJ(s.cilj, "Pa")}: sonda mora ${odgovor3 < d ? "dublje" : "pliće"}. Izračunaj h = ${p0Of(s) ? "(p − p₀)" : "p"} / (ρ·g), uz g = ${formatNumber(g)} m/s².`
      };
    }
    const v = velicina(s);
    const jed2 = jedinica(s);
    const answer = izmjeriOdgovor(s);
    const krivo = [
      ...s.krivo ?? [],
      ...krivoU(izmjeriKrivo(s), VELICINE[v], jed2, answer),
      ...krivoJedinica(izmjeriSI(s), VELICINE[v], jed2)
    ];
    const savjet3 = v === "p" ? "Hidrostatički tlak: p = ρ·g·h, dubina u metrima, mjerena od površine." : "Uzgon je razlika sila tlaka na donju i gornju plohu: F_u = ρ·g·V.";
    return checkNumber({
      input: odgovor3,
      answer,
      tol: tolOf(s, answer),
      unit: jed2,
      krivo,
      savjet: s.savjet || savjet3
    });
  }
  function tocanOdgovor(s) {
    if (s.nacin === "usporedi") {
      const t = najveciTlak(s);
      return t === "jednako" ? `jednak u svim posudama (${fmtJ(tlakDno(s, 0), "Pa")})` : `posuda ${t + 1} (${fmtJ(tlakDno(s, t), "Pa")})`;
    }
    if (s.nacin === "postavi") return `dubina ${formatNumber(dubinaCilja(s))} m`;
    if (s.nacin === "izmjeri")
      return `${formatNumber(izmjeriOdgovor(s))} ${jedinica(s)}`;
    return "";
  }
  var VODA = { naziv: "voda", gustoca: 1e3 };
  var tlak_dubina = {
    tip: TIP,
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Hidrostatički tlak p = p₀ + ρgh: 1–3 posude s tekućinom, sonda s manometrom, uronjena kocka (uzgon iz razlike tlakova).
Polja: tekst, h (visina tekućine, m), tekucina: {naziv, gustoca (kg/m³)}, posude: [{oblik, h?, tekucina?}] (1–3; oblik: ${OBLICI.join(" | ")};
h i tekucina posude zamjenjuju zajedničke), spojene? (true: posude spojene cijevi pri dnu, ista razina), g? (9,81; u zadacima često 10),
p0? (tlak iznad tekućine u Pa, npr. 101325; bez njega se prikazuje samo hidrostatički tlak), tijelo?: {stranica (m) ili volumen (dm³),
dubina_vrha? (m), gustoca? (kg/m³)} (kocka u jedinoj posudi; strelice sila tlaka na plohe, F_u = F₂ − F₁ = ρgV), korak? (korak sonde u m), nacin:
- "istrazi": učenik vuče sondu kroz tekućinu (ili kocku gore-dolje) i gleda tlak uživo; isprekidana crta pokazuje istu dubinu u svim posudama;
- "izmjeri": pitaj: {velicina: "p" (zadano) | "F_u" | "F_gore" | "F_dolje", dubina (m, za p), posuda? (indeks)}; točka je označena,
  učenik upiše iznos u jedinica ("Pa" | "hPa" | "kPa" | "MPa" za tlak, "N" | "kN" za silu). Točan odgovor i tipične greške (bez g,
  dubina u cm, dubina od dna, tlak na dnu, dm³ umjesto m³, krivi g, kriva jedinica) računa aplikacija; ${KRIVO_OPIS};
- "postavi": cilj (tlak u Pa): učenik spusti sondu na dubinu na kojoj je tlak jednak cilju (vidi dubinu, ne tlak); dubina cilja mora biti
  višekratnik koraka sonde;
- "usporedi": učenik dodirne posudu s najvećim tlakom na dnu ili „Jednako u svima“ (računa se iz ρ i h svake posude).`,
    primjer: {
      tip: TIP,
      nacin: "usporedi",
      tekst: "Tri posude različitog oblika napunjene su vodom do visine 1,2 m. U kojoj je posudi tlak na dnu najveći?",
      h: 1.2,
      g: 10,
      tekucina: VODA,
      posude: [
        { oblik: "valjak" },
        { oblik: "lijevak" },
        { oblik: "obrnuti_lijevak" }
      ]
    },
    primjeri: [
      {
        tip: TIP,
        nacin: "istrazi",
        tekst: "Povuci sondu kroz vodu u bilo kojoj posudi. Gledaj manometar: o čemu ovisi tlak?",
        h: 1.2,
        g: 10,
        tekucina: VODA,
        posude: [
          { oblik: "valjak" },
          { oblik: "lijevak" },
          { oblik: "obrnuti_lijevak" }
        ]
      },
      {
        tip: TIP,
        nacin: "izmjeri",
        tekst: "Koliki je tlak vode na dnu posude?",
        h: 1.2,
        g: 10,
        tekucina: VODA,
        posude: [{ oblik: "lijevak" }],
        pitaj: { dubina: 1.2 }
      },
      {
        tip: TIP,
        nacin: "izmjeri",
        tekst: "Koliki je tlak morske vode na dubini 1,5 m? Odgovor upiši u kPa.",
        h: 2.5,
        g: 10,
        tekucina: { naziv: "morska voda", gustoca: 1030 },
        posude: [{ oblik: "siroka" }],
        pitaj: { dubina: 1.5 },
        jedinica: "kPa"
      },
      {
        tip: TIP,
        nacin: "postavi",
        tekst: "Spusti sondu na dubinu na kojoj je tlak ulja 4500 Pa.",
        h: 0.8,
        g: 10,
        tekucina: { naziv: "ulje", gustoca: 900 },
        posude: [{ oblik: "siroka" }],
        cilj: 4500
      },
      {
        tip: TIP,
        nacin: "istrazi",
        tekst: "Spojene posude: povuci sondu i usporedi tlak na istoj dubini u svakoj posudi.",
        h: 0.5,
        g: 10,
        tekucina: VODA,
        spojene: true,
        posude: [{ oblik: "uska" }, { oblik: "boca" }, { oblik: "kosa" }]
      },
      {
        tip: TIP,
        nacin: "usporedi",
        tekst: "Posude su napunjene do iste visine različitim tekućinama. Gdje je tlak na dnu najveći?",
        h: 0.8,
        g: 10,
        posude: [
          { oblik: "valjak", tekucina: { naziv: "ulje", gustoca: 900 } },
          { oblik: "valjak", tekucina: VODA },
          { oblik: "valjak", tekucina: { naziv: "glicerin", gustoca: 1260 } }
        ]
      },
      {
        tip: TIP,
        nacin: "istrazi",
        tekst: "Povuci kocku gore-dolje kroz vodu. Strelice su sile tlaka na plohe kocke. Mijenja li se njihova razlika?",
        h: 0.4,
        g: 10,
        tekucina: VODA,
        posude: [{ oblik: "siroka" }],
        tijelo: { volumen: 2, dubina_vrha: 0.1 }
      },
      {
        tip: TIP,
        nacin: "izmjeri",
        tekst: "Kocka volumena 2 dm³ potpuno je uronjena u vodu. Kolika je sila uzgona?",
        h: 0.4,
        g: 10,
        tekucina: VODA,
        posude: [{ oblik: "siroka" }],
        tijelo: { volumen: 2, dubina_vrha: 0.1 },
        pitaj: { velicina: "F_u" }
      }
    ],
    validate,
    check,
    tocanOdgovor,
    brojcaniOdgovor: (s) => s.nacin === "izmjeri" ? izmjeriOdgovor(s) : s.nacin === "postavi" ? dubinaCilja(s) : null
  };

  // src/lib/lesson/katalog/fizika/cijev.js
  var TIP2 = "cijev";
  var NACINI8 = ["istrazi", "predvidi"];
  var VELICINE2 = {
    v2: "brzina",
    p2: "tlak",
    Q: "protok",
    t_kanta: "vrijeme",
    p_d1: "tlak",
    p_d2: "tlak"
  };
  var ZADANA_JEDINICA = {
    brzina: "m/s",
    tlak: "Pa",
    protok: "L/s",
    vrijeme: "s"
  };
  var KLIZACI2 = ["d2", "S2", "v1"];
  var povrsina = (p) => isNum(p?.S) ? p.S * 1e-4 : Math.PI * (p.d / 100) ** 2 / 4;
  var rhoOf = (s) => isNum(s.rho) ? s.rho : 1e3;
  function stanje(s, promjena3 = {}) {
    const [a, b0] = s.presjeci;
    const b = isNum(promjena3.d2) ? { ...b0, d: promjena3.d2 } : isNum(promjena3.S2) ? { ...b0, S: promjena3.S2 } : b0;
    const S1 = povrsina(a);
    const S2 = povrsina(b);
    const v1 = isNum(promjena3.v1) ? promjena3.v1 : s.v1;
    const rho = rhoOf(s);
    const Q = S1 * v1;
    const v2 = Q / S2;
    const pd1 = rho * v1 * v1 / 2;
    const pd2 = rho * v2 * v2 / 2;
    const p1 = isNum(s.p1) ? s.p1 : null;
    const p2 = p1 == null ? null : p1 + pd1 - pd2;
    const t_kanta = pozitivan(s.kanta?.V) ? s.kanta.V / 1e3 / Q : null;
    return {
      S1,
      S2,
      d1: a.d ?? null,
      d2: b.d ?? null,
      v1,
      v2,
      Q,
      rho,
      p1,
      p2,
      pd1,
      pd2,
      t_kanta
    };
  }
  var velicina2 = (s) => s.pitaj?.velicina;
  var jedinica2 = (s) => s.jedinica ?? ZADANA_JEDINICA[VELICINE2[velicina2(s)]];
  var odgovorSI = (s) => {
    const st = stanje(s);
    const v = velicina2(s);
    return v === "p_d1" ? st.pd1 : v === "p_d2" ? st.pd2 : st[v];
  };
  var odgovor = (s) => uJedinici(odgovorSI(s), VELICINE2[velicina2(s)], jedinica2(s));
  var tolOf2 = (s, answer) => isNum(s.tolerancija) ? s.tolerancija : Math.abs(answer) * 5e-3;
  function validate2(s) {
    const { list, need } = problems();
    nacinOk(s, need, NACINI8, TIP2);
    need(isText(s.tekst), `${TIP2} treba „tekst“`);
    if (need(
      Array.isArray(s.presjeci) && s.presjeci.length === 2,
      `${TIP2} treba točno 2 presjeka: [šire, uže]`
    )) {
      s.presjeci.forEach((p, i) => {
        need(
          pozitivan(p?.d) !== pozitivan(p?.S),
          `presjeci[${i}] treba točno jedno od: d (promjer u cm) ili S (površina u cm²)`
        );
        if (p?.d != null)
          need(
            pozitivan(p.d) && p.d <= 200,
            `presjeci[${i}].d mora biti promjer u cm (0–200)`
          );
        if (p?.S != null)
          need(
            pozitivan(p.S) && p.S <= 3e4,
            `presjeci[${i}].S mora biti površina u cm² (0–30 000)`
          );
        if (p?.oznaka != null)
          need(
            isText(p.oznaka) && p.oznaka.length <= 12,
            `presjeci[${i}].oznaka je kratak tekst (npr. „šire“)`
          );
      });
      const [a, b] = s.presjeci;
      need(
        pozitivan(a?.d) === pozitivan(b?.d),
        "oba presjeka zadaj na isti način: oba promjerom d ili oba površinom S"
      );
      if (!list.length) {
        const r = povrsina(a) / povrsina(b);
        need(
          r > 1.0001,
          "prvi presjek mora biti širi od drugog (cijev se sužava)"
        );
        need(r <= 64, "omjer površina najviše 64 (inače se uži dio ne vidi)");
      }
    }
    need(
      pozitivan(s.v1) && s.v1 <= 50,
      "v1 (brzina u širem dijelu) mora biti između 0 i 50 m/s"
    );
    if (s.rho != null)
      need(
        pozitivan(s.rho) && s.rho >= 500 && s.rho <= 22e3,
        "rho (gustoća tekućine) u kg/m³, 500–22 000"
      );
    if (s.p1 != null)
      need(
        pozitivan(s.p1) && s.p1 <= 1e7,
        "p1 (tlak u širem dijelu) u Pa, pozitivan"
      );
    if (s.kanta != null)
      need(
        pozitivan(s.kanta?.V) && s.kanta.V <= 1e4,
        "kanta treba V (volumen u litrama)"
      );
    if (list.length) return list;
    const st = stanje(s);
    if (s.p1 != null)
      need(
        st.p2 > 0,
        `uz ove brojeve tlak u užem dijelu bio bi ${formatNumber(st.p2)} Pa (negativan): povećaj p1 ili smanji brzinu`
      );
    if (s.klizaci != null) {
      if (need(
        Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 2,
        "klizaci je popis 1–2 klizača"
      )) {
        s.klizaci.forEach((k, i) => {
          if (!need(
            KLIZACI2.includes(k?.id),
            `klizaci[${i}].id mora biti jedan od: ${KLIZACI2.join(", ")}`
          ))
            return;
          need(
            k.id === "d2" === pozitivan(s.presjeci[1].d) || k.id === "v1",
            `klizač ${k.id} ne odgovara načinu zadavanja presjeka (${pozitivan(s.presjeci[1].d) ? "d2" : "S2"})`
          );
          if (need(
            [k.min, k.max].every(pozitivan) && k.max > k.min,
            `klizaci[${i}] treba min < max (pozitivni)`
          )) {
            const zad = k.id === "v1" ? s.v1 : k.id === "d2" ? s.presjeci[1].d : s.presjeci[1].S;
            need(
              zad >= k.min && zad <= k.max,
              `klizaci[${i}]: zadana vrijednost ${formatNumber(zad)} mora biti između min i max`
            );
            if (k.id !== "v1") {
              const prvi = k.id === "d2" ? s.presjeci[0].d : s.presjeci[0].S;
              need(
                k.max <= prvi * 1.5,
                `klizaci[${i}].max neka ne bude puno veći od prvog presjeka (${formatNumber(prvi)})`
              );
              need(
                prvi / k.min <= (k.id === "d2" ? 8 : 64),
                `klizaci[${i}].min je premalen (uži dio se ne bi vidio)`
              );
            }
          }
          if (k.korak != null)
            need(
              pozitivan(k.korak) && k.korak < k.max - k.min,
              `klizaci[${i}].korak mora biti pozitivan i manji od raspona`
            );
        });
        need(
          new Set(s.klizaci.map((k) => k?.id)).size === s.klizaci.length,
          "klizači se ne smiju ponavljati"
        );
      }
    }
    if (s.nacin === "istrazi") {
      noAnswerFields(s, need);
      need(s.pitaj == null, "istrazi nema polje pitaj");
    }
    if (s.nacin === "predvidi") {
      need(
        s.klizaci == null,
        "predvidi nema klizače (učenik računa za zadane brojeve)"
      );
      const v = velicina2(s);
      if (!need(
        v in VELICINE2,
        `predvidi treba pitaj.velicina, jedna od: ${Object.keys(VELICINE2).join(", ")}`
      ))
        return list;
      need(
        jedinicaOk(VELICINE2[v], jedinica2(s)),
        `jedinica za ${v} mora biti jedna od: ${Object.keys(JEDINICE[VELICINE2[v]]).join(", ")}`
      );
      if (v === "p2")
        need(s.p1 != null, "za p2 treba zadati p1 (tlak u širem dijelu, Pa)");
      if (v === "t_kanta") need(s.kanta != null, "za t_kanta treba kanta: {V}");
      if (!list.length)
        validateKrivo(s.krivo, odgovor(s), tolOf2(s, odgovor(s)), need);
    }
    return list;
  }
  function krivoSI(s) {
    const st = stanje(s);
    const [a, b] = s.presjeci;
    const v = velicina2(s);
    const sPromjerom = pozitivan(a.d);
    const omjerD = sPromjerom ? a.d / b.d : null;
    const out = [];
    if (v === "v2") {
      if (sPromjerom)
        out.push({
          si: st.v1 * omjerD,
          poruka: "Brzina se mijenja s omjerom površina, a površina ide s kvadratom promjera: S₁/S₂ = (d₁/d₂)²."
        });
      out.push({
        si: st.v1 * st.S2 / st.S1,
        poruka: "Gdje je cijev uža, voda teče brže, a ne sporije: v₂ = v₁·S₁/S₂."
      });
      out.push({
        si: st.v1,
        poruka: "Brzina nije ista u oba dijela. Isti je protok Q = S·v, pa u užem dijelu voda mora biti brža."
      });
    }
    if (v === "p2") {
      out.push({
        si: st.p1,
        poruka: "Tlak nije isti: u užem dijelu voda je brža, pa je statički tlak manji."
      });
      out.push({
        si: st.p1 - st.pd1 + st.pd2,
        poruka: "Gdje je voda brža, tlak je manji, a ne veći: p₂ = p₁ + ρ(v₁² − v₂²)/2."
      });
      out.push({
        si: st.p1 + 2 * (st.pd1 - st.pd2),
        poruka: "Dinamički tlak je ρv²/2: zaboravio si podijeliti s 2."
      });
      out.push({
        si: st.p1 + (st.pd1 - st.pd2) / st.rho,
        poruka: "Pomnoži s gustoćom vode ρ: dinamički tlak je ρv²/2."
      });
      out.push({
        si: st.p1 + st.rho / 2 * (st.v1 - st.v2),
        poruka: "Brzine u Bernoullijevoj jednadžbi su na kvadrat: ρv²/2."
      });
      out.push({
        si: st.p1 / 1e3 + st.pd1 - st.pd2,
        poruka: "Tlak p₁ najprije pretvori u paskale, pa ga tek onda zbroji s ρv²/2."
      });
      if (sPromjerom) {
        const vKrivo = st.v1 * omjerD;
        out.push({
          si: st.p1 + st.rho / 2 * (st.v1 ** 2 - vKrivo ** 2),
          poruka: "Najprije točno izračunaj v₂: omjer površina, a ne promjera."
        });
      }
    }
    if (v === "Q") {
      out.push({
        si: st.S1 * 1e4 * st.v1,
        poruka: "Površinu pretvori u m² (1 cm² = 0,0001 m²) prije množenja brzinom."
      });
      if (sPromjerom)
        out.push({
          si: st.Q * 4,
          poruka: "Polumjer je pola promjera: S = r²π = d²π/4."
        });
      out.push({
        si: st.S2 * st.v1,
        poruka: "Brzina v₁ pripada širem presjeku: Q = S₁·v₁ (ili S₂·v₂)."
      });
    }
    if (v === "t_kanta") {
      const V = s.kanta.V / 1e3;
      out.push({
        si: V / (st.Q * 1e3),
        poruka: "Volumen i protok uzmi u istim jedinicama (L i L/s, ili m³ i m³/s)."
      });
      out.push({ si: st.Q / V * 1, poruka: "Vrijeme je t = V/Q, a ne Q/V." });
      if (sPromjerom)
        out.push({
          si: V / (st.Q * 4),
          poruka: "Polumjer je pola promjera: S = r²π = d²π/4."
        });
      out.push({
        si: V / (st.S1 * 1e4 * st.v1),
        poruka: "Površinu pretvori u m² (1 cm² = 0,0001 m²)."
      });
    }
    if (v === "p_d1" || v === "p_d2") {
      const [moj, tudi] = v === "p_d1" ? [st.v1, st.v2] : [st.v2, st.v1];
      out.push({
        si: st.rho * moj * moj,
        poruka: "Dinamički tlak je ρv²/2: zaboravio si podijeliti s 2."
      });
      out.push({
        si: st.rho * moj / 2,
        poruka: "Brzina je u dinamičkom tlaku na kvadrat: ρv²/2."
      });
      out.push({
        si: st.rho * tudi * tudi / 2,
        poruka: `To je dinamički tlak u ${v === "p_d1" ? "užem" : "širem"} dijelu. Uzmi brzinu iz traženog dijela.`
      });
      if (st.p1 != null)
        out.push({
          si: v === "p_d1" ? st.p1 : st.p2,
          poruka: "To je statički tlak (onaj koji pokazuje manometar). Dinamički je ρv²/2."
        });
    }
    return out;
  }
  function check2(s, input) {
    const v = velicina2(s);
    const jed2 = jedinica2(s);
    const answer = odgovor(s);
    const krivo = [
      ...s.krivo ?? [],
      ...krivoU(krivoSI(s), VELICINE2[v], jed2, answer),
      ...krivoJedinica(odgovorSI(s), VELICINE2[v], jed2)
    ];
    const SAVJET2 = {
      v2: "Protok je isti u oba dijela: S₁v₁ = S₂v₂, pa v₂ = v₁·S₁/S₂.",
      p2: "Bernoulli za vodoravnu cijev: p₁ + ρv₁²/2 = p₂ + ρv₂²/2. Najprije izračunaj v₂.",
      Q: "Protok je Q = S·v, s površinom u m².",
      t_kanta: "Vrijeme punjenja je t = V/Q, s istim jedinicama volumena.",
      p_d1: "Dinamički tlak je p_d = ρv²/2.",
      p_d2: "Dinamički tlak je p_d = ρv²/2, s brzinom u užem dijelu."
    };
    return checkNumber({
      input,
      answer,
      tol: tolOf2(s, answer),
      unit: jed2,
      krivo,
      savjet: s.savjet || SAVJET2[v]
    });
  }
  var VODA_694 = {
    presjeci: [
      { oznaka: "šire", d: 4 },
      { oznaka: "uže", d: 2 }
    ],
    v1: 1.5
  };
  var BERNOULLI_699 = {
    presjeci: [
      { oznaka: "šire", S: 20 },
      { oznaka: "uže", S: 5 }
    ],
    v1: 1,
    p1: 15e4
  };
  var cijev = {
    tip: TIP2,
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Voda teče kroz vodoravnu cijev koja se sužava: čestice se gibaju brzinom v = Q/S (u užem dijelu brže), uz svaki presjek d ili S i v,
isti protok Q; uz p1 manometri pokazuju statički tlak, a stupci statički + dinamički tlak (zbroj isti). Kanta se puni protokom Q.
Polja: tekst, presjeci: [{oznaka?, d (cm) ili S (cm²)}, {…}] (točno 2, prvi širi, oba istim načinom), v1 (m/s, u širem dijelu),
p1? (statički tlak u širem dijelu, Pa), rho? (1000 kg/m³), kanta?: {V (L)}, nacin:
- "istrazi": učenik mijenja klizače i gleda; klizaci?: [{id: "d2" | "S2" | "v1", min, max, korak?}] (1–2; bez njih klizač za uži presjek i v1);
- "predvidi": pitaj: {velicina: "v2" | "p2" | "Q" | "t_kanta" | "p_d1" | "p_d2"}, jedinica? (v2: "m/s"; p: "Pa" | "kPa" …; Q: "L/s" | "m³/s" |
  "L/min"; t: "s" | "min"); animacija i traženi broj skriveni su dok učenik ne odgovori. Točan odgovor i tipične greške (omjer promjera
  umjesto površina, veći tlak gdje je brže, zaboravljeno /2, cm² u m², kPa i Pa…) računa aplikacija; tolerancija? (zadano 0,5 %); ${KRIVO_OPIS}.`,
    primjer: {
      tip: TIP2,
      nacin: "predvidi",
      tekst: "Cijev se sužava s promjera 4 cm na promjer 2 cm. U širem dijelu voda teče brzinom 1,5 m/s. Kolika je brzina vode u užem dijelu?",
      ...VODA_694,
      kanta: { V: 10 },
      pitaj: { velicina: "v2" }
    },
    primjeri: [
      {
        tip: TIP2,
        nacin: "istrazi",
        tekst: "Mijenjaj promjer užeg dijela i brzinu. Gledaj kako se mijenjaju brzina u užem dijelu i protok.",
        ...VODA_694,
        kanta: { V: 10 }
      },
      {
        tip: TIP2,
        nacin: "predvidi",
        tekst: "Koliki je protok vode kroz cijev?",
        ...VODA_694,
        kanta: { V: 10 },
        pitaj: { velicina: "Q" }
      },
      {
        tip: TIP2,
        nacin: "predvidi",
        tekst: "Za koliko se sekundi napuni kanta od 10 litara?",
        ...VODA_694,
        kanta: { V: 10 },
        pitaj: { velicina: "t_kanta" }
      },
      {
        tip: TIP2,
        nacin: "istrazi",
        tekst: "Mijenjaj površinu užeg dijela. Prati manometre: gdje je tlak manji? Zbroj statičkog i dinamičkog tlaka ostaje isti.",
        ...BERNOULLI_699,
        klizaci: [{ id: "S2", min: 4, max: 20, korak: 1 }]
      },
      {
        tip: TIP2,
        nacin: "predvidi",
        tekst: "U vodoravnoj cijevi presjeka 20 cm² voda teče brzinom 1 m/s pri tlaku 150 kPa. Uži dio ima presjek 5 cm². Koliki je tlak u užem dijelu?",
        ...BERNOULLI_699,
        pitaj: { velicina: "p2" }
      },
      {
        tip: TIP2,
        nacin: "predvidi",
        tekst: "Koliki je dinamički tlak u užem dijelu cijevi?",
        ...BERNOULLI_699,
        pitaj: { velicina: "p_d2" }
      }
    ],
    validate: validate2,
    check: check2,
    tocanOdgovor: (s) => s.nacin === "predvidi" ? `${formatNumber(odgovor(s))} ${jedinica2(s)}` : "",
    brojcaniOdgovor: (s) => s.nacin === "predvidi" ? odgovor(s) : null
  };

  // src/lib/lesson/katalog/fizika/krivulja_zagrijavanja.js
  var TIP3 = "krivulja_zagrijavanja";
  var VRSTE2 = {
    krivulja: ["istrazi", "odaberi", "ocitaj"],
    mijesanje: ["istrazi", "predvidi"],
    grijac: ["istrazi", "predvidi"]
  };
  var STANJA2 = ["kruto", "tekuce", "plin"];
  var NAZIV_DIJELA = {
    grije_kruto: "zagrijavanje krute tvari",
    taljenje: "taljenje",
    grije_tekuce: "zagrijavanje tekućine",
    isparavanje: "isparavanje",
    grije_plin: "zagrijavanje plina (pare)"
  };
  var ODABERI = {
    promjena_stanja: (d) => d.ravno,
    stalna_temperatura: (d) => d.ravno,
    temperatura_raste: (d) => !d.ravno,
    taljenje: (d) => d.vrsta === "taljenje",
    isparavanje: (d) => d.vrsta === "isparavanje",
    grije_kruto: (d) => d.vrsta === "grije_kruto",
    grije_tekuce: (d) => d.vrsta === "grije_tekuce",
    grije_plin: (d) => d.vrsta === "grije_plin"
  };
  var RAVNE = [
    "promjena_stanja",
    "stalna_temperatura",
    "taljenje",
    "isparavanje"
  ];
  var OCITAJ = {
    Q_dio: "energija",
    Q_ukupno: "energija",
    t_dio: "vrijeme",
    t_ukupno: "vrijeme"
  };
  var PITAJ_KAL = {
    mijesanje: { t_konacna: "temperatura" },
    grijac: { Q: "energija", t_grijanja: "vrijeme" }
  };
  var ZADANA = { energija: "J", vrijeme: "s", temperatura: "°C" };
  var vrstaOf = (s) => s.vrsta ?? "krivulja";
  function stanjeOd(s) {
    if (s.stanje_od) return s.stanje_od;
    const { taliste, vreliste } = s.tvar;
    return s.od <= taliste ? "kruto" : s.od <= vreliste ? "tekuce" : "plin";
  }
  function stanjeDo(s) {
    if (s.stanje_do) return s.stanje_do;
    const { taliste, vreliste } = s.tvar;
    return s.do < taliste ? "kruto" : s.do < vreliste ? "tekuce" : "plin";
  }
  function dijelovi(s) {
    const t = s.tvar;
    const m = s.m;
    const out = [];
    const grij = (vrsta, c, T0, T1, stanje3) => {
      if (T1 > T0 + 1e-9)
        out.push({
          vrsta,
          ravno: false,
          T0,
          T1,
          c,
          Q: isNum(m) && isNum(c) ? m * c * (T1 - T0) : null,
          stanje: stanje3
        });
    };
    const prijelaz = (vrsta, T2, L, simbol) => out.push({
      vrsta,
      ravno: true,
      T0: T2,
      T1: T2,
      L,
      simbol,
      Q: isNum(m) && isNum(L) ? m * L : null
    });
    let st = stanjeOd(s);
    const kraj3 = stanjeDo(s);
    let T = s.od;
    if (st === "kruto") {
      const Tk = kraj3 === "kruto" ? s.do : t.taliste;
      grij("grije_kruto", t.c_kruto, T, Tk, "kruto");
      T = Tk;
      if (kraj3 !== "kruto") {
        prijelaz("taljenje", t.taliste, t.lambda, "λ");
        st = "tekuce";
      }
    }
    if (st === "tekuce") {
      const Tk = kraj3 === "tekuce" ? s.do : t.vreliste;
      grij("grije_tekuce", t.c_tekuce, T, Tk, "tekuce");
      T = Tk;
      if (kraj3 === "plin") {
        prijelaz("isparavanje", t.vreliste, t.r, "r");
        st = "plin";
      }
    }
    if (st === "plin") grij("grije_plin", t.c_plin, T, s.do, "plin");
    let Q0 = 0;
    return out.map((d, i) => {
      const r = { ...d, br: i + 1, Q0 };
      Q0 = isNum(Q0) && isNum(d.Q) ? Q0 + d.Q : null;
      return r;
    });
  }
  var ocitajVelicina = (s) => s.ocitaj?.velicina;
  function rasponDijelova(s) {
    const q = s.ocitaj;
    const v = ocitajVelicina(s);
    if (v === "Q_dio" || v === "t_dio") return [q.dio, q.dio];
    const n = dijelovi(s).length;
    return [q.od_dijela ?? 1, q.do_dijela ?? n];
  }
  var zbrojQ = (ds, a, b, f2 = (d) => d.Q) => ds.filter((d) => d.br >= a && d.br <= b).reduce((sum, d) => sum + f2(d), 0);
  var mc = (t) => t.m * t.c;
  var tKonacna = (tijela) => tijela.reduce((a, t) => a + mc(t) * t.t0, 0) / tijela.reduce((a, t) => a + mc(t), 0);
  function grijanje(s) {
    const dt = s.t1 - s.t0;
    const Qi = s.tijela.map((t) => mc(t) * dt);
    const Q = Qi.reduce((a, b) => a + b, 0);
    return { Qi, Q, t: isNum(s.snaga) ? Q / s.snaga : null, dt };
  }
  function velicinaOdgovora(s) {
    const v = vrstaOf(s);
    if (v === "krivulja") return OCITAJ[ocitajVelicina(s)];
    return PITAJ_KAL[v]?.[s.pitaj];
  }
  var jedinica3 = (s) => s.jedinica ?? ZADANA[velicinaOdgovora(s)];
  function odgovorSI2(s) {
    const v = vrstaOf(s);
    if (v === "mijesanje") return tKonacna(s.tijela);
    if (v === "grijac") return s.pitaj === "Q" ? grijanje(s).Q : grijanje(s).t;
    const q = ocitajVelicina(s);
    const [a, b] = rasponDijelova(s);
    const Q = zbrojQ(dijelovi(s), a, b);
    return q.startsWith("t_") ? Q / s.snaga : Q;
  }
  var odgovor2 = (s) => uJedinici(odgovorSI2(s), velicinaOdgovora(s), jedinica3(s));
  var tocniDijelovi = (s) => dijelovi(s).filter(ODABERI[s.odaberi] ?? (() => false)).map((d) => d.br);
  function tolOf3(s, answer) {
    if (isNum(s.tolerancija)) return s.tolerancija;
    return velicinaOdgovora(s) === "temperatura" ? 0.5 : Math.abs(answer) * 5e-3;
  }
  function validateTvar(s, need) {
    const t = s.tvar;
    if (!need(
      t && typeof t === "object",
      "krivulja treba tvar: {naziv, taliste, vreliste, c_kruto, c_tekuce, c_plin?, lambda, r}"
    ))
      return false;
    need(isText(t.naziv), "tvar.naziv mora biti tekst (npr. „voda“)");
    const ok = need(
      isNum(t.taliste) && isNum(t.vreliste) && t.taliste < t.vreliste && t.taliste > -273.15,
      "tvar treba taliste < vreliste (°C, iznad −273,15)"
    );
    for (const k of ["c_kruto", "c_tekuce", "c_plin"])
      if (t[k] != null)
        need(
          pozitivan(t[k]) && t[k] <= 5e4,
          `tvar.${k} je specifični toplinski kapacitet u J/(kg·K), pozitivan`
        );
    for (const k of ["lambda", "r"])
      if (t[k] != null)
        need(
          pozitivan(t[k]) && t[k] <= 1e8,
          `tvar.${k} je latentna toplina u J/kg, pozitivna`
        );
    return ok;
  }
  function validateKrivulja(s, need, list) {
    if (!validateTvar(s, need)) return;
    need(
      isNum(s.od) && isNum(s.do) && s.od <= s.do && s.od > -273.15,
      "krivulja treba od ≤ do (°C, iznad −273,15)"
    );
    if (s.stanje_od != null)
      need(
        STANJA2.includes(s.stanje_od),
        "stanje_od mora biti kruto, tekuce ili plin"
      );
    if (s.stanje_do != null)
      need(
        STANJA2.includes(s.stanje_do),
        "stanje_do mora biti kruto, tekuce ili plin"
      );
    if (s.shematski != null)
      need(
        typeof s.shematski === "boolean",
        "shematski mora biti true ili false"
      );
    if (s.snaga != null)
      need(pozitivan(s.snaga) && s.snaga <= 1e7, "snaga grijača u W, pozitivna");
    if (list.length) return;
    const { taliste: Tt, vreliste: Tv } = s.tvar;
    const moze = {
      kruto: (T) => T <= Tt,
      tekuce: (T) => T >= Tt && T <= Tv,
      plin: (T) => T >= Tv
    };
    need(
      moze[stanjeOd(s)](s.od),
      `na ${formatNumber(s.od)} °C tvar ne može biti u stanju „${stanjeOd(s)}“ (talište ${formatNumber(Tt)} °C, vrelište ${formatNumber(Tv)} °C)`
    );
    need(
      moze[stanjeDo(s)](s.do),
      `na ${formatNumber(s.do)} °C tvar ne može biti u stanju „${stanjeDo(s)}“`
    );
    need(
      STANJA2.indexOf(stanjeOd(s)) <= STANJA2.indexOf(stanjeDo(s)),
      "krivulja zagrijavanja ide od krutog prema plinu, ne obrnuto"
    );
    if (list.length) return;
    const ds = dijelovi(s);
    if (!need(ds.length >= 1 && ds.length <= 5, "krivulja mora imati 1–5 dijelova"))
      return;
    if (!s.shematski) {
      need(
        pozitivan(s.m) && s.m <= 1e3,
        "m (masa u kg) mora biti pozitivna; za graf bez brojeva na osi Q stavi shematski: true"
      );
      for (const d of ds)
        need(
          isNum(d.Q),
          `dio ${d.br} (${NAZIV_DIJELA[d.vrsta]}) treba ${d.ravno ? d.vrsta === "taljenje" ? "tvar.lambda" : "tvar.r" : `tvar.c_${d.stanje}`} (ili shematski: true)`
        );
    } else if (s.m != null)
      need(pozitivan(s.m), "m (masa u kg) mora biti pozitivna");
    if (s.nacin === "istrazi") noAnswerFields(s, need);
    if (s.nacin === "odaberi") {
      if (need(
        s.odaberi in ODABERI,
        `odaberi mora biti jedno od: ${Object.keys(ODABERI).join(", ")}`
      ))
        need(
          tocniDijelovi(s).length >= 1,
          `na ovoj krivulji nema nijednog dijela „${s.odaberi}“`
        );
    }
    if (s.nacin === "ocitaj") {
      const q = s.ocitaj;
      const v = q?.velicina;
      if (!need(
        v in OCITAJ,
        `ocitaj.velicina mora biti jedna od: ${Object.keys(OCITAJ).join(", ")}`
      ))
        return;
      need(
        jedinicaOk(OCITAJ[v], jedinica3(s)),
        `jedinica za ${v} mora biti jedna od: ${Object.keys(JEDINICE[OCITAJ[v]]).join(", ")}`
      );
      {
        need(!s.shematski, "toplina se ne može očitati sa shematskog grafa");
        if (v.startsWith("t_")) need(pozitivan(s.snaga), `${v} treba snaga (W)`);
        if (v.endsWith("_dio"))
          need(
            Number.isInteger(q.dio) && q.dio >= 1 && q.dio <= ds.length,
            `ocitaj.dio mora biti broj dijela 1–${ds.length}`
          );
        else {
          const a = q.od_dijela ?? 1;
          const b = q.do_dijela ?? ds.length;
          need(
            Number.isInteger(a) && Number.isInteger(b) && a >= 1 && b <= ds.length && a <= b,
            `ocitaj.od_dijela i do_dijela moraju biti brojevi dijelova 1–${ds.length}, od ≤ do`
          );
        }
      }
      if (!list.length)
        validateKrivo(s.krivo, odgovor2(s), tolOf3(s, odgovor2(s)), need);
    }
  }
  function validateKalorimetar(s, need, list) {
    const v = vrstaOf(s);
    const n = v === "mijesanje" ? [2, 3] : [1, 3];
    if (!need(
      Array.isArray(s.tijela) && s.tijela.length >= n[0] && s.tijela.length <= n[1],
      `${v} treba ${n[0]}–${n[1]} tijela: [{naziv, m (kg), c${v === "mijesanje" ? ", t0" : ""}}]`
    ))
      return;
    s.tijela.forEach((t, i) => {
      need(
        isText(t?.naziv) && t.naziv.length <= 24,
        `tijela[${i}].naziv je kratak tekst`
      );
      need(
        pozitivan(t?.m) && t.m <= 50,
        `tijela[${i}].m je masa u kg, najviše 50 (npr. 250 g = 0.25)`
      );
      need(
        pozitivan(t?.c) && t.c <= 5e4,
        `tijela[${i}].c je specifični toplinski kapacitet u J/(kg·K)`
      );
      if (v === "mijesanje")
        need(
          isNum(t?.t0) && t.t0 > -273.15 && t.t0 < 5e3,
          `tijela[${i}].t0 je početna temperatura u °C`
        );
    });
    if (v === "mijesanje" && !list.length) {
      const T = s.tijela.map((t) => t.t0);
      need(
        Math.max(...T) - Math.min(...T) >= 1,
        "tijela trebaju različite početne temperature"
      );
    }
    if (v === "grijac") {
      need(
        isNum(s.t0) && isNum(s.t1) && s.t1 > s.t0,
        "grijac treba t0 < t1 (°C)"
      );
      need(pozitivan(s.snaga) && s.snaga <= 1e6, "grijac treba snaga (W)");
    }
    if (s.nacin === "istrazi") {
      noAnswerFields(s, need);
      need(s.pitaj == null, "istrazi nema polje pitaj");
    }
    if (s.nacin === "predvidi" && !list.length) {
      const opcije = PITAJ_KAL[v];
      if (!need(
        s.pitaj in opcije,
        `pitaj mora biti jedno od: ${Object.keys(opcije).join(", ")}`
      ))
        return;
      need(
        jedinicaOk(opcije[s.pitaj], jedinica3(s)),
        `jedinica mora biti jedna od: ${Object.keys(JEDINICE[opcije[s.pitaj]]).join(", ")}`
      );
      if (!list.length)
        validateKrivo(s.krivo, odgovor2(s), tolOf3(s, odgovor2(s)), need);
    }
  }
  function validate3(s) {
    const { list, need } = problems();
    const v = vrstaOf(s);
    if (!need(
      v in VRSTE2,
      `vrsta mora biti jedna od: ${Object.keys(VRSTE2).join(", ")}`
    ))
      return list;
    need(
      VRSTE2[v].includes(s.nacin),
      `za vrstu ${v} nacin mora biti jedan od: ${VRSTE2[v].join(", ")}`
    );
    need(isText(s.tekst), `${TIP3} treba „tekst“`);
    if (list.length) return list;
    if (v === "krivulja") validateKrivulja(s, need, list);
    else validateKalorimetar(s, need, list);
    return list;
  }
  function krivoKrivulja(s) {
    const q = ocitajVelicina(s);
    const ds = dijelovi(s);
    const [a, b] = rasponDijelova(s);
    const k = q.startsWith("t_") ? 1 / s.snaga : 1;
    const t = s.tvar;
    const out = [];
    const dio = ds.filter((d) => d.br >= a && d.br <= b);
    const imaRavno = dio.some((d) => d.ravno);
    const imaKoso = dio.some((d) => !d.ravno);
    if (imaRavno && imaKoso) {
      out.push({
        si: zbrojQ(ds, a, b, (d) => d.ravno ? 0 : d.Q) * k,
        poruka: "Zaboravio si toplinu za promjenu stanja (Q = mλ ili Q = mr): temperatura tada stoji, ali grijač i dalje troši toplinu."
      });
      out.push({
        si: zbrojQ(ds, a, b, (d) => d.ravno ? d.Q : 0) * k,
        poruka: "Zaboravio si zagrijavanje do tališta ili vrelišta: na kosim dijelovima Q = m·c·Δt."
      });
    }
    const zamjenaC = { kruto: t.c_tekuce, tekuce: t.c_kruto, plin: t.c_tekuce };
    if (imaKoso) {
      out.push({
        si: zbrojQ(
          ds,
          a,
          b,
          (d) => d.ravno || !isNum(zamjenaC[d.stanje]) ? d.Q : s.m * zamjenaC[d.stanje] * (d.T1 - d.T0)
        ) * k,
        poruka: dio.some((d) => d.stanje === "kruto") ? `Za krutu tvar (${t.naziv} u krutom stanju) uzmi c krute tvari, a ne c tekućine.` : "Za tekućinu uzmi c tekućine, a ne c krute tvari."
      });
    }
    if (imaRavno && isNum(t.lambda) && isNum(t.r)) {
      out.push({
        si: zbrojQ(
          ds,
          a,
          b,
          (d) => !d.ravno ? d.Q : s.m * (d.vrsta === "taljenje" ? t.r : t.lambda)
        ) * k,
        poruka: "Zamijenio si latentne topline: za taljenje vrijedi λ (taljenje), za isparavanje r (isparavanje)."
      });
    }
    if (b > a)
      for (const d of dio)
        out.push({
          si: d.Q * k,
          poruka: `To je samo dio ${d.br} (${NAZIV_DIJELA[d.vrsta]}). Zbroji topline svih dijelova od ${a} do ${b}.`
        });
    if (q.startsWith("t_"))
      out.push({
        si: zbrojQ(ds, a, b),
        poruka: "To je toplina u J. Vrijeme je t = Q / P."
      });
    return out;
  }
  function krivoKalorimetar(s) {
    const v = vrstaOf(s);
    const out = [];
    if (v === "mijesanje") {
      const T = s.tijela;
      const sredina = T.reduce((a, t) => a + t.t0, 0) / T.length;
      const isteMase = T.every((t) => Math.abs(t.m - T[0].m) < 1e-12);
      const istiC = T.every((t) => t.c === T[0].c);
      out.push({
        si: sredina,
        poruka: isteMase ? "Aritmetička sredina vrijedi samo za istu tvar i iste mase. Ovdje se tvari razlikuju po c: uzmi u obzir m·c svakog tijela." : "Aritmetička sredina vrijedi samo za jednake mase. Smjesa je bliže temperaturi tijela kojeg ima više (veći m·c)."
      });
      if (!istiC)
        out.push({
          si: T.reduce((a, t) => a + t.m * t.t0, 0) / T.reduce((a, t) => a + t.m, 0),
          poruka: "Tvari su različite: uz masu u račun ide i specifični toplinski kapacitet c."
        });
      if (!isteMase)
        out.push({
          si: T.reduce((a, t) => a + t.c * t.t0, 0) / T.reduce((a, t) => a + t.c, 0),
          poruka: "Uz c u račun ide i masa svakog tijela."
        });
      return out;
    }
    const g = grijanje(s);
    const k = s.pitaj === "Q" ? 1 : 1 / s.snaga;
    if (s.tijela.length > 1)
      s.tijela.forEach(
        (t, i) => out.push({
          si: (g.Q - g.Qi[i]) * k,
          poruka: `Zaboravio si toplinu koju primi ${t.naziv}: i to se grije od ${formatNumber(s.t0)} °C do ${formatNumber(s.t1)} °C.`
        })
      );
    out.push({
      si: g.Q * 1e3 * k,
      poruka: "Masu pretvori u kilograme (250 g = 0,25 kg)."
    });
    if (s.t0 !== 0)
      out.push({
        si: s.tijela.reduce((a, t) => a + mc(t) * s.t1, 0) * k,
        poruka: "Uvrsti promjenu temperature Δt = t₂ − t₁, a ne konačnu temperaturu."
      });
    if (s.pitaj === "t_grijanja")
      out.push({ si: g.Q, poruka: "To je toplina u J. Vrijeme je t = Q / P." });
    return out;
  }
  function check3(s, odg) {
    if (vrstaOf(s) === "krivulja" && s.nacin === "odaberi") {
      if (!Array.isArray(odg) || !odg.length)
        return {
          tocno: false,
          poruka: "Dodirni dijelove grafa koje tražiš.",
          prazno: true
        };
      const tocni = tocniDijelovi(s);
      const ds = dijelovi(s);
      const visak = odg.filter((b) => !tocni.includes(b));
      const fali = tocni.filter((b) => !odg.includes(b));
      if (!visak.length && !fali.length) return { tocno: true };
      if (visak.length) {
        const d = ds.find((x) => x.br === visak[0]);
        if (!d)
          return {
            tocno: false,
            poruka: "Dodirni dijelove grafa koje tražiš.",
            prazno: true
          };
        const traziRavno = RAVNE.includes(s.odaberi);
        if (!d.ravno && traziRavno)
          return {
            tocno: false,
            poruka: `Na dijelu ${d.br} temperatura raste: tvar se samo grije (${NAZIV_DIJELA[d.vrsta]}), stanje se ne mijenja.`
          };
        if (d.ravno && traziRavno)
          return {
            tocno: false,
            poruka: `Dio ${d.br} je vodoravan na ${formatNumber(d.T0)} °C: tu je ${NAZIV_DIJELA[d.vrsta]}${d.vrsta === "taljenje" ? " (talište)" : " (vrelište)"}.`
          };
        if (d.ravno)
          return {
            tocno: false,
            poruka: `Na dijelu ${d.br} temperatura stoji: tu se mijenja stanje (${NAZIV_DIJELA[d.vrsta]}), a tvar se ne grije.`
          };
        return {
          tocno: false,
          poruka: `Dio ${d.br} je ${NAZIV_DIJELA[d.vrsta]}, a to nije ono što se traži. Pogledaj na kojoj je temperaturi taj dio.`
        };
      }
      return {
        tocno: false,
        poruka: `Nisi označio sve: nedostaje ${fali.length === 1 ? "još jedan dio" : `još ${fali.length} dijela`}.`
      };
    }
    const vel = velicinaOdgovora(s);
    const jed2 = jedinica3(s);
    const answer = odgovor2(s);
    const lista = vrstaOf(s) === "krivulja" ? krivoKrivulja(s) : krivoKalorimetar(s);
    const krivo = [
      ...s.krivo ?? [],
      ...krivoU(lista, vel, jed2, answer).map(
        (k) => vel === "temperatura" ? { ...k, tolerancija: 0.3 } : k
      ),
      ...vel === "temperatura" ? [] : krivoJedinica(odgovorSI2(s), vel, jed2)
    ];
    const SAVJET2 = {
      krivulja: "Na kosim dijelovima Q = m·c·Δt (c za stanje tvari na tom dijelu), na vodoravnima Q = m·λ (taljenje) ili Q = m·r (isparavanje).",
      mijesanje: "Toplina koju toplije tijelo preda jednaka je toplini koju hladnije primi: Σ m·c·(t − t₀) = 0.",
      grijac: "Grije se sve u posudi: Q = Σ m·c·Δt, a vrijeme na grijaču t = Q / P."
    };
    return checkNumber({
      input: odg,
      answer,
      tol: tolOf3(s, answer),
      unit: jed2,
      krivo,
      savjet: s.savjet || SAVJET2[vrstaOf(s)]
    });
  }
  function tocanOdgovor2(s) {
    if (s.nacin === "istrazi") return "";
    if (vrstaOf(s) === "krivulja" && s.nacin === "odaberi") {
      const ds = dijelovi(s);
      const t = tocniDijelovi(s);
      const popis = t.length === 1 ? String(t[0]) : `${t.slice(0, -1).join(", ")} i ${t.at(-1)}`;
      return `${t.length === 1 ? "dio" : "dijelovi"} ${popis} (${t.map((b) => NAZIV_DIJELA[ds[b - 1].vrsta]).join(", ")})`;
    }
    return fmtJ(odgovor2(s), jedinica3(s)).replace(/\u202f/g, "");
  }
  var VODA2 = {
    naziv: "voda",
    taliste: 0,
    vreliste: 100,
    c_kruto: 2100,
    c_tekuce: 4200,
    c_plin: 2e3,
    lambda: 333e3,
    r: 226e4
  };
  var krivulja_zagrijavanja = {
    tip: TIP3,
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Toplina i promjene stanja. vrsta "krivulja" (zadano): graf T–Q zagrijavanja tvari, dijelovi u omjeru stvarnih toplina; klizač po
krivulji pokazuje temperaturu, stanje (čestice, udio rastaljenog) i dovedenu toplinu po dijelovima. Polja: tekst, tvar: {naziv, taliste,
vreliste (°C), c_kruto, c_tekuce, c_plin (J/(kg·K)), lambda, r (J/kg)}, m (kg), od, do (°C), stanje_od?/stanje_do? ("kruto" | "tekuce" | "plin";
zadano: na talištu je tvar na početku kruta, a na kraju rastaljena, isto na vrelištu), shematski? (true: jednako široki dijelovi, bez
brojeva topline; tada c, λ, r i m nisu potrebni), snaga? (W: os x je vrijeme t = Q/P), nacin:
- "istrazi": učenik vuče po krivulji ili pusti grijač;
- "odaberi": odaberi: "promjena_stanja" | "stalna_temperatura" | "temperatura_raste" | "taljenje" | "isparavanje" | "grije_kruto" |
  "grije_tekuce" | "grije_plin"; učenik dodirom označi sve takve dijelove (točan skup računa aplikacija);
- "ocitaj": ocitaj: {velicina: "Q_dio", dio} | {velicina: "Q_ukupno", od_dijela?, do_dijela?} | {velicina: "t_dio" | "t_ukupno", …} (uz snaga)
  ; jedinica? ("J" | "kJ" | "MJ"; "s" | "min"). Brojevi na osi topline (vremena) su tada skriveni.
  Tipične greške (bez latentne topline, c tekućine za led, λ i r zamijenjeni, samo jedan dio, kJ/J) dodaju se same; ${KRIVO_OPIS}.
vrsta "mijesanje": tijela: [{naziv, m (kg), c, t0 (°C)}] (2–3) se miješaju, temperature se izjednačuju (graf T–vrijeme, toplina predana =
primljena). nacin "istrazi" (učenik mijenja mase i miješa) ili "predvidi" s pitaj: "t_konacna" (odgovor u °C, tolerancija 0,5 °C; krive vrijednosti
aritmetička sredina, bez c ili bez m dodaju se same).
vrsta "grijac": tijela: [{naziv, m (kg), c}] (1–3, npr. voda i posuda) grije grijač snage snaga (W) od t0 do t1 (°C); toplina po tijelima raste
uživo. nacin "istrazi" ili "predvidi" s pitaj: "Q" | "t_grijanja" (jedinica "J" | "kJ" ili "s" | "min"; krive vrijednosti bez posude,
grami, konačna temperatura umjesto Δt dodaju se same).`,
    primjer: {
      tip: TIP3,
      nacin: "ocitaj",
      tekst: "Koliko topline treba da se 0,5 kg leda temperature −20 °C zagrije do tališta i potpuno otopi? (dijelovi 1 i 2)",
      tvar: VODA2,
      m: 0.5,
      od: -20,
      do: 120,
      ocitaj: { velicina: "Q_ukupno", od_dijela: 1, do_dijela: 2 },
      jedinica: "J"
    },
    primjeri: [
      {
        tip: TIP3,
        nacin: "istrazi",
        tekst: "Povuci točku po krivulji (ili uključi grijač) i gledaj temperaturu, čestice i toplinu. Zašto je isparavanje toliko dugo?",
        tvar: VODA2,
        m: 0.5,
        od: -20,
        do: 120
      },
      {
        tip: TIP3,
        nacin: "odaberi",
        tekst: "Uzorak vode ravnomjerno zagrijavamo od −20 °C do 120 °C. Dodirni dijelove grafa u kojima tvar mijenja agregacijsko stanje.",
        tvar: { naziv: "voda", taliste: 0, vreliste: 100 },
        od: -20,
        do: 120,
        shematski: true,
        odaberi: "promjena_stanja"
      },
      {
        tip: TIP3,
        nacin: "odaberi",
        tekst: "Uzorak žive zagrijavamo od −60 °C do 400 °C. Na kojem se dijelu živa tali?",
        tvar: { naziv: "živa", taliste: -39, vreliste: 357 },
        od: -60,
        do: 400,
        shematski: true,
        odaberi: "taljenje"
      },
      {
        tip: TIP3,
        nacin: "ocitaj",
        tekst: "Koliko topline treba da se ta voda od 0 °C zagrije do 100 °C i sva ispari? Odgovor upiši u kJ.",
        tvar: VODA2,
        m: 0.5,
        od: 0,
        do: 100,
        stanje_od: "tekuce",
        ocitaj: { velicina: "Q_ukupno" },
        jedinica: "kJ"
      },
      {
        tip: TIP3,
        nacin: "ocitaj",
        tekst: "Grijač snage 2000 W grije 0,5 kg leda od −20 °C. Koliko dugo traje taljenje (dio 2)?",
        tvar: VODA2,
        m: 0.5,
        od: -20,
        do: 30,
        snaga: 2e3,
        ocitaj: { velicina: "t_dio", dio: 2 }
      },
      {
        tip: TIP3,
        vrsta: "mijesanje",
        nacin: "istrazi",
        tekst: "Mijenjaj mase i pomiješaj vodu. Kamo se pomiče konačna temperatura?",
        tijela: [
          { naziv: "topla voda", m: 0.25, c: 4200, t0: 80 },
          { naziv: "hladna voda", m: 0.5, c: 4200, t0: 20 }
        ]
      },
      {
        tip: TIP3,
        vrsta: "mijesanje",
        nacin: "predvidi",
        tekst: "250 g vode od 80 °C pomiješamo s 500 g vode od 20 °C. Kolika je konačna temperatura smjese?",
        tijela: [
          { naziv: "topla voda", m: 0.25, c: 4200, t0: 80 },
          { naziv: "hladna voda", m: 0.5, c: 4200, t0: 20 }
        ],
        pitaj: "t_konacna"
      },
      {
        tip: TIP3,
        vrsta: "grijac",
        nacin: "istrazi",
        tekst: "Uključi grijač i gledaj kako toplina odlazi u vodu i u posudu.",
        tijela: [
          { naziv: "voda", m: 0.25, c: 4200 },
          { naziv: "posuda (aluminij)", m: 0.1, c: 900 }
        ],
        t0: 20,
        t1: 80,
        snaga: 1200
      },
      {
        tip: TIP3,
        vrsta: "grijac",
        nacin: "predvidi",
        tekst: "U aluminijskoj posudi mase 100 g grije se 250 g vode od 20 °C do 80 °C na grijaču snage 1200 W. Koliko traje grijanje?",
        tijela: [
          { naziv: "voda", m: 0.25, c: 4200 },
          { naziv: "posuda (aluminij)", m: 0.1, c: 900 }
        ],
        t0: 20,
        t1: 80,
        snaga: 1200,
        pitaj: "t_grijanja"
      }
    ],
    validate: validate3,
    check: check3,
    tocanOdgovor: tocanOdgovor2,
    brojcaniOdgovor: (s) => s.nacin === "istrazi" || s.nacin === "odaberi" ? null : odgovor2(s)
  };

  // src/lib/lesson/katalog/fizika/_plin.js
  var R = 8.314;
  var KB = 1381e-26;
  var NA = 6022e20;
  var GAMA = 5 / 3;
  var NULA_C = 273;
  var P_JEDINICE = { Pa: 1, kPa: 1e3, bar: 1e5, MPa: 1e6 };
  var V_JEDINICE = {
    "m³": 1,
    L: 1e-3,
    "dm³": 1e-3,
    mL: 1e-6,
    "cm³": 1e-6
  };
  var T_JEDINICE = ["K", "°C"];
  var IME = {
    p: "tlak",
    V: "volumen",
    T: "temperatura",
    n: "količina plina",
    N: "broj čestica",
    Ek: "srednja kinetička energija",
    vef: "efektivna brzina",
    U: "unutarnja energija",
    W: "rad",
    Q: "toplina",
    pV: "umnožak pV"
  };
  var iznos3 = (v, jedinica4 = "") => isNum(v) ? `${formatSci(v)}${jedinica4 ? ` ${jedinica4}` : ""}` : "—";
  var blizu = (a, b, rel = 1e-6) => Math.abs(a - b) <= rel * Math.max(1, Math.abs(a), Math.abs(b));
  var raspon = (r) => Array.isArray(r) && r.length === 2 && r.every(isNum) && r[1] > r[0];

  // src/lib/lesson/katalog/fizika/plinski_dijagram.js
  var NACINI9 = ["istrazi", "postavi", "put", "dionice"];
  var OSI = {
    pV: { x: "V", y: "p" },
    VT: { x: "T", y: "V" },
    pT: { x: "T", y: "p" }
  };
  var VRSTE3 = [
    "izobarno",
    "izohorno",
    "izotermno",
    "adijabatno",
    "ravno"
  ];
  var STALNO = { izobarno: "p", izohorno: "V", izotermno: "T" };
  var OPIS_VRSTE = {
    izobarno: "izobarni (stalan tlak)",
    izohorno: "izohorni (stalan volumen)",
    izotermno: "izotermni (stalna temperatura)",
    adijabatno: "adijabatski (bez izmjene topline)",
    ravno: "ravna crta"
  };
  var GENITIV = {
    izobarno: "izobarnog procesa (stalan tlak)",
    izohorno: "izohornog procesa (stalan volumen)",
    izotermno: "izotermnog procesa (stalna temperatura)",
    adijabatno: "adijabatskog procesa"
  };
  var ZENSKI = {
    izobarno: "izobarna",
    izohorno: "izohorna",
    izotermno: "izotermna",
    adijabatno: "adijabatska",
    ravno: "ravna crta"
  };
  var KRATKO_VRSTE = {
    izobarno: "izobarno",
    izohorno: "izohorno",
    izotermno: "izotermno",
    adijabatno: "adijabatski",
    ravno: "ravno"
  };
  var PRIKAZI3 = ["p", "V", "T", "pV", "U", "W", "Q"];
  var ENERGIJE = ["pV", "U", "W", "Q"];
  var VELICINE3 = ["W", "dU", "Q"];
  var ZNAKOVI2 = ["+", "0", "-"];
  var MAX_STANJA = 8;
  var osiOd = (s) => OSI[s.osi ?? "pV"];
  function jediniceOd(s) {
    const j = s.jedinice ?? {};
    return { p: j.p ?? "kPa", V: j.V ?? "L", T: j.T ?? "K" };
  }
  var celzij = (s) => jediniceOd(s).T === "°C";
  var uK = (s, t) => celzij(s) ? t + NULA_C : t;
  var izK = (s, T) => celzij(s) ? T - NULA_C : T;
  var energijaFaktor = (s) => P_JEDINICE[jediniceOd(s).p] * V_JEDINICE[jediniceOd(s).V];
  function konstanta(s) {
    if (isNum(s.n) && s.n > 0)
      return { k: s.n * R / energijaFaktor(s), poznato: true };
    const treca = trecaVelicina(s);
    const st = (s.stanja ?? []).find(
      (t) => isNum(t?.[treca]) && isNum(t?.p ?? t?.V) && ["p", "V", "T"].every((q) => isNum(t?.[q]))
    );
    if (st && uK(s, st.T) > 0)
      return { k: st.p * st.V / uK(s, st.T), poznato: true };
    return { k: 1, poznato: false };
  }
  var trecaVelicina = (s) => ["p", "V", "T"].find((q) => q !== osiOd(s).x && q !== osiOd(s).y);
  function izKoordinata(s, k, t) {
    const osi2 = osiOd(s);
    if (osi2.x === "V") return { p: t.p, V: t.V, T: t.p * t.V / k };
    const T = uK(s, t.T);
    return osi2.y === "V" ? { p: k * T / t.V, V: t.V, T } : { p: t.p, V: k * T / t.p, T };
  }
  function punaStanja(s) {
    const { k } = konstanta(s);
    return (s.stanja ?? []).map((t) => ({
      oznaka: t.oznaka,
      ...izKoordinata(s, k, t)
    }));
  }
  var stanjeOd2 = (s, oznaka) => punaStanja(s).find((t) => t.oznaka === oznaka);
  function koordinate(s, st) {
    const osi2 = osiOd(s);
    const v = (q) => q === "T" ? izK(s, st.T) : st[q];
    return { x: v(osi2.x), y: v(osi2.y) };
  }
  function izTocke(s, x, y) {
    const osi2 = osiOd(s);
    return izKoordinata(s, konstanta(s).k, { [osi2.x]: x, [osi2.y]: y });
  }
  function naProcesu(a, vrsta, q, v, k) {
    if (!(v > 0)) return null;
    const st = (p, V) => p > 0 && V > 0 && Number.isFinite(p) && Number.isFinite(V) ? { p, V, T: p * V / k } : null;
    if (STALNO[vrsta] === q)
      return blizu(a[q], v, 1e-9) ? { p: a.p, V: a.V, T: a.T } : null;
    if (vrsta === "izobarno")
      return q === "V" ? st(a.p, v) : st(a.p, k * v / a.p);
    if (vrsta === "izohorno")
      return q === "p" ? st(v, a.V) : st(k * v / a.V, a.V);
    if (vrsta === "izotermno")
      return q === "p" ? st(v, a.p * a.V / v) : st(a.p * a.V / v, v);
    if (vrsta === "adijabatno") {
      const C = a.p * a.V ** GAMA;
      if (q === "V") return st(C / v ** GAMA, v);
      if (q === "p") return st(v, (C / v) ** (1 / GAMA));
      const V = a.V * (a.T / v) ** (1 / (GAMA - 1));
      return st(k * v / V, V);
    }
    return null;
  }
  function naIstomProcesu(a, b, vrsta) {
    if (vrsta === "ravno") return true;
    if (vrsta === "izobarno") return blizu(a.p, b.p, 1e-3);
    if (vrsta === "izohorno") return blizu(a.V, b.V, 1e-3);
    if (vrsta === "izotermno") return blizu(a.p * a.V, b.p * b.V, 5e-3);
    if (vrsta === "adijabatno")
      return blizu(a.p * a.V ** GAMA, b.p * b.V ** GAMA, 0.01);
    return false;
  }
  function uzorci(s, a, b, vrsta, n = 60) {
    const { k } = konstanta(s);
    const out = [];
    if (vrsta === "ravno") {
      const [ka, kb] = [koordinate(s, a), koordinate(s, b)];
      for (let i = 0; i <= n; i++) {
        const t = i / n;
        out.push(izTocke(s, ka.x + (kb.x - ka.x) * t, ka.y + (kb.y - ka.y) * t));
      }
      return out;
    }
    const q = vrsta === "izohorno" ? "p" : "V";
    for (let i = 0; i <= n; i++)
      out.push(naProcesu(a, vrsta, q, a[q] + (b[q] - a[q]) * i / n, k) ?? a);
    return out;
  }
  function dionica2(s, a, b, vrsta) {
    const E2 = energijaFaktor(s);
    let W2;
    if (vrsta === "izobarno") W2 = a.p * (b.V - a.V);
    else if (vrsta === "izohorno") W2 = 0;
    else if (vrsta === "izotermno") W2 = a.p * a.V * Math.log(b.V / a.V);
    else if (vrsta === "adijabatno") W2 = (a.p * a.V - b.p * b.V) / (GAMA - 1);
    else {
      const u = uzorci(s, a, b, "ravno", 200);
      W2 = 0;
      for (let i = 1; i < u.length; i++)
        W2 += (u[i].p + u[i - 1].p) / 2 * (u[i].V - u[i - 1].V);
    }
    const dU = 1.5 * (b.p * b.V - a.p * a.V);
    const Q = vrsta === "adijabatno" ? 0 : W2 + dU;
    const skala2 = Math.max(a.p * a.V, b.p * b.V) * E2;
    return { W: W2 * E2, dU: dU * E2, Q: Q * E2, skala: skala2 };
  }
  var znak = (x, skala2) => Math.abs(x) <= 1e-6 * skala2 ? "0" : x > 0 ? "+" : "-";
  function znakoviDionica(s) {
    return (s.procesi ?? []).map((pr) => {
      const [a, b] = [stanjeOd2(s, pr.od), stanjeOd2(s, pr.do)];
      const d = dionica2(s, a, b, pr.vrsta);
      return {
        W: znak(d.W, d.skala),
        dU: znak(d.dU, d.skala),
        Q: znak(d.Q, d.skala),
        iznosi: d,
        a,
        b,
        vrsta: pr.vrsta
      };
    });
  }
  var ciljVelicina = (s) => Object.keys(s.cilj ?? {})[0];
  function stanjeNakon(s, { vrsta = s.proces, kaoKelvin = false } = {}) {
    const a = stanjeOd2(s, s.pocetak);
    if (!a) return null;
    const q = ciljVelicina(s);
    const v = s.cilj[q];
    const { k } = konstanta(s);
    if (kaoKelvin) {
      const t1 = izK(s, a.T);
      if (!(t1 > 0)) return null;
      const lazni = { p: a.p, V: a.V, T: t1 };
      const k2 = a.p * a.V / t1;
      const r = naProcesu(lazni, vrsta, q, v, k2);
      return r ? { ...r, T: uK(s, r.T) } : null;
    }
    return naProcesu(a, vrsta, q, q === "T" ? uK(s, v) : v, k);
  }
  function kriveTocke(s) {
    const a = stanjeOd2(s, s.pocetak);
    const tocno = stanjeNakon(s);
    const q = ciljVelicina(s);
    const out = [];
    for (const vrsta of ["izobarno", "izohorno", "izotermno", "adijabatno"]) {
      if (vrsta === s.proces) continue;
      const b = stanjeNakon(s, { vrsta });
      if (b)
        out.push({
          st: b,
          poruka: `Ta bi točka bila kraj ${GENITIV[vrsta]}. Ovdje je proces ${OPIS_VRSTE[s.proces]}${s.proces === "izotermno" ? ": vrijedi $pV$ = konst., pa točka ide po hiperboli" : s.proces === "adijabatno" ? ": plin ne izmjenjuje toplinu, pa se mijenjaju sve tri veličine" : ""}.`
        });
    }
    const osi2 = osiOd(s);
    const druga = [osi2.x, osi2.y].find((x) => x !== q && x !== STALNO[s.proces]);
    if (druga && tocno && a[druga] !== tocno[druga]) {
      const obrnuto = { ...tocno, [druga]: a[druga] * a[druga] / tocno[druga] };
      const gore = tocno[druga] > a[druga];
      out.push({
        st: obrnuto,
        poruka: `Obrnuo si omjer: ${IME[druga]} se ovdje mora ${gore ? "povećati" : "smanjiti"}, a ne ${gore ? "smanjiti" : "povećati"}. ${s.proces === "izotermno" ? "Pri stalnoj temperaturi veći tlak znači manji volumen ($pV$ = konst.)." : "Uvrsti vrijednosti u isti omjer s obje strane jednadžbe."}`
      });
    }
    if (celzij(s) && (q === "T" || osi2.x === "T")) {
      const b = stanjeNakon(s, { kaoKelvin: true });
      if (b)
        out.push({
          st: b,
          poruka: "Temperature si uvrstio u °C. U plinskim zakonima temperatura mora biti u kelvinima: $T = t + 273$."
        });
    }
    return out;
  }
  function putSpeca(s) {
    const st = s.stanja ?? [];
    return (s.procesi ?? []).map((pr, i) => ({
      ...pr,
      cilj: st[(i + 1) % st.length]
    }));
  }
  function tolerancije(s) {
    const osi2 = osiOd(s);
    const t = (q) => (isNum(s.tolerancija?.[q]) ? s.tolerancija[q] : (s.korak?.[q] ?? 0) / 2) * (1 + 1e-6) + 1e-9;
    return { x: t(osi2.x), y: t(osi2.y) };
  }
  function istaTocka(s, st, tocka3) {
    const k = koordinate(s, st);
    const tol2 = tolerancije(s);
    return Math.abs(k.x - tocka3.x) <= tol2.x && Math.abs(k.y - tocka3.y) <= tol2.y;
  }
  function tockaOdgovora(s, o) {
    const osi2 = osiOd(s);
    const x = Number(o?.[osi2.x]);
    const y = Number(o?.[osi2.y]);
    return Number.isFinite(x) && Number.isFinite(y) ? { x, y } : null;
  }
  function vrijednostTxt(s, q, v) {
    const j = jediniceOd(s);
    if (q === "T") return iznos3(v, j.T);
    return iznos3(v, j[q]);
  }
  var tockaTxt = (s, st) => {
    const osi2 = osiOd(s);
    const k = koordinate(s, st);
    return `${osi2.x} = ${vrijednostTxt(s, osi2.x, k.x)}, ${osi2.y} = ${vrijednostTxt(s, osi2.y, k.y)}`;
  };
  var ZNAK_TXT = { "+": "+", 0: "0", "-": "−" };
  var VELICINA_TXT = { W: "W", dU: "ΔU", Q: "Q" };
  var velicineOd = (s) => [].concat(s.velicina ?? "W");
  function validateZajednicko(s, need) {
    need(
      NACINI9.includes(s.nacin),
      `plinski_dijagram.nacin mora biti jedan od: ${NACINI9.join(", ")}`
    );
    need(isText(s.tekst), "plinski_dijagram treba „tekst“ (što učenik radi)");
    if (!need(s.osi == null || s.osi in OSI, 'osi mora biti "pV", "VT" ili "pT"'))
      return false;
    const j = s.jedinice ?? {};
    need(
      j.p == null || j.p in P_JEDINICE,
      `jedinice.p mora biti jedna od: ${Object.keys(P_JEDINICE).join(", ")}`
    );
    need(
      j.V == null || j.V in V_JEDINICE,
      `jedinice.V mora biti jedna od: ${Object.keys(V_JEDINICE).join(", ")}`
    );
    need(
      j.T == null || T_JEDINICE.includes(j.T),
      'jedinice.T mora biti "K" ili "°C"'
    );
    if (s.jedinice != null)
      need(
        typeof s.jedinice === "object" && Object.keys(s.jedinice).every((q) => ["p", "V", "T"].includes(q)),
        "jedinice smije imati samo p, V i T"
      );
    if (s.n != null)
      need(
        isNum(s.n) && s.n > 0,
        "n (količina plina u mol) mora biti pozitivan broj"
      );
    const osi2 = osiOd(s);
    let ok = true;
    for (const q of [osi2.x, osi2.y]) {
      const r = s[q];
      if (!need(raspon(r), `${q} mora biti raspon osi [od, do] uz od < do`)) {
        ok = false;
        continue;
      }
      if (q === "T")
        need(
          uK(s, r[0]) >= -30,
          "raspon T smije početi najniže na −300 °C (apsolutna nula je 0 K = −273 °C)"
        );
      else need(r[0] >= 0, `raspon ${q} ne smije biti negativan`);
      const k2 = s.korak?.[q];
      if (need(
        isNum(k2) && k2 > 0,
        `korak.${q} (razmak crta mreže na osi ${q}) mora biti pozitivan broj`
      )) {
        const n = (r[1] - r[0]) / k2;
        need(
          n >= 2 && n <= 40,
          `os ${q}: (do − od)/korak mora biti između 2 i 40 (sada ${formatNumber(n)})`
        );
      } else ok = false;
    }
    if (s.tolerancija != null)
      need(
        typeof s.tolerancija === "object" && Object.entries(s.tolerancija).every(
          ([q, v]) => [osi2.x, osi2.y].includes(q) && isNum(v) && v > 0
        ),
        `tolerancija mora biti {${osi2.x}?, ${osi2.y}?} s pozitivnim brojevima`
      );
    for (const f2 of ["izoterme", "povrsina", "slobodno", "vodilica", "zatvoren"])
      need(
        s[f2] == null || typeof s[f2] === "boolean",
        `${f2} mora biti true ili false`
      );
    need(
      !s.povrsina || osi2.x === "V",
      "povrsina (rad kao površina ispod krivulje) postoji samo na p–V dijagramu"
    );
    need(
      !s.izoterme || osi2.x === "V",
      "izoterme u pozadini postoje samo na p–V dijagramu (na V–T i p–T su izoterme okomite crte)"
    );
    if (s.savjet != null) need(isText(s.savjet), "savjet mora biti tekst");
    const oznake = [];
    if (need(
      Array.isArray(s.stanja) && s.stanja.length >= 1 && s.stanja.length <= MAX_STANJA,
      `plinski_dijagram treba stanja: 1–${MAX_STANJA} točaka [{oznaka, ${osi2.x}, ${osi2.y}}]`
    )) {
      s.stanja.forEach((t, i) => {
        need(
          isText(t?.oznaka) && String(t.oznaka).length <= 3,
          `stanja[${i}].oznaka mora biti kratak tekst (npr. "1" ili "A")`
        );
        oznake.push(t?.oznaka);
        for (const q of [osi2.x, osi2.y]) {
          if (need(isNum(t?.[q]), `stanja[${i}] treba broj ${q}`) && raspon(s[q]))
            need(
              t[q] >= s[q][0] && t[q] <= s[q][1],
              `stanja[${i}].${q} = ${formatNumber(t[q])} je izvan osi [${formatNumber(s[q][0])}, ${formatNumber(s[q][1])}]`
            );
        }
        const treca = trecaVelicina(s);
        if (t?.[treca] != null)
          need(
            isNum(t[treca]) && (treca === "T" ? uK(s, t[treca]) > 0 : t[treca] > 0),
            `stanja[${i}].${treca} mora biti pozitivan broj`
          );
        need(
          Object.keys(t ?? {}).every(
            (f2) => ["oznaka", "p", "V", "T"].includes(f2)
          ),
          `stanja[${i}] smije imati samo oznaka, p, V i T`
        );
      });
      need(
        new Set(oznake).size === oznake.length,
        "oznake stanja moraju biti različite"
      );
    } else ok = false;
    if (!ok) return false;
    const { k, poznato } = konstanta(s);
    const puna = punaStanja(s);
    s.stanja.forEach((t, i) => {
      need(
        puna[i].p > 0 && puna[i].V > 0 && puna[i].T > 0,
        `stanja[${i}]: tlak, volumen i temperatura moraju biti pozitivni (u K)`
      );
      const treca = trecaVelicina(s);
      if (isNum(t[treca])) {
        const zadano = treca === "T" ? uK(s, t[treca]) : t[treca];
        need(
          blizu(zadano, puna[i][treca], 0.01),
          `stanja[${i}].${treca} se ne slaže s pV = nRT (iz ostalih podataka izlazi ${formatNumber(treca === "T" ? izK(s, puna[i].T) : puna[i][treca])})`
        );
      }
    });
    if (s.prikazi != null) {
      if (need(
        Array.isArray(s.prikazi) && s.prikazi.every((q) => PRIKAZI3.includes(q)),
        `prikazi je popis od: ${PRIKAZI3.join(", ")}`
      )) {
        need(
          !s.prikazi.includes("T") || osi2.x === "T" || poznato,
          "prikazi T na p–V dijagramu traži n ili T u nekom stanju"
        );
        need(
          osi2.x === "V" || poznato || !s.prikazi.some((q) => ENERGIJE.includes(q)),
          "energije (pV, U, W, Q) na V–T i p–T dijagramu traže n"
        );
        need(
          osi2.x === "V" || poznato || !s.prikazi.includes(trecaVelicina(s)),
          `${trecaVelicina(s)} na ovom dijagramu nije na osi, pa prikaz traži n`
        );
      }
    }
    if (s.procesi != null) {
      if (need(
        Array.isArray(s.procesi) && s.procesi.length <= 8,
        "procesi mora biti popis (najviše 8) [{od, do, vrsta}]"
      )) {
        s.procesi.forEach((pr, i) => {
          const a = puna.find((t) => t.oznaka === pr?.od);
          const b = puna.find((t) => t.oznaka === pr?.do);
          need(
            VRSTE3.includes(pr?.vrsta),
            `procesi[${i}].vrsta mora biti jedna od: ${VRSTE3.join(", ")}`
          );
          if (need(a && b, `procesi[${i}]: od i do moraju biti oznake stanja`) && VRSTE3.includes(pr.vrsta)) {
            need(
              pr.od !== pr.do,
              `procesi[${i}]: početno i završno stanje moraju biti različiti`
            );
            need(
              naIstomProcesu(a, b, pr.vrsta),
              `procesi[${i}] (${pr.od} → ${pr.do}) nije ${pr.vrsta}: stanja ne leže na takvom procesu`
            );
            need(
              pr.vrsta !== "adijabatno" || osi2.x === "V" || poznato,
              `procesi[${i}]: adijabata na V–T / p–T dijagramu traži n`
            );
          }
        });
      }
    }
    return { k, poznato, puna };
  }
  var lanac2 = (procesi) => procesi.every((pr, i) => i === 0 || pr.od === procesi[i - 1].do);
  var plinski_dijagram = {
    tip: "plinski_dijagram",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Dijagram stanja idealnog (jednoatomnog) plina: osi? ("pV" zadano: V vodoravno, p okomito | "VT": T vodoravno, V okomito | "pT": T vodoravno, p okomito),
rasponi osi kao [od, do] za veličine na osima (npr. V: [0, 8], p: [0, 350]), korak: {V, p} (ili {T, …}) razmak mreže (2–40 crta po osi),
jedinice?: {p: "kPa" (Pa | kPa | bar | MPa), V: "L" (L | dm³ | mL | cm³ | m³), T: "K" (K | °C)} – svi brojevi u specu su u tim jedinicama (1 kPa · 1 L = 1 J),
stanja: [{oznaka, i dvije veličine s osi, npr. V, p}] (1–8; treća veličina smije se dodati i mora se slagati s pV = nRT),
n? (mol; bez njega T na p–V dijagramu nije poznata, osim ako neko stanje ima T), procesi?: [{od, do, vrsta: "izobarno" | "izohorno" | "izotermno" | "adijabatno" | "ravno"}]
(stanja moraju ležati na takvom procesu), izoterme? (blijede izoterme kroz stanja, p–V), povrsina? (rad kao osjenčana površina ispod puta, p–V),
prikazi?: ["p", "V", "T", "pV", "U", "W", "Q"] očitanja uživo (U = 3/2 pV), tekst, nacin:
- "istrazi" (nije zadatak): učenik vuče stanje po putu iz procesi (lanac 1 → 2 → 3…) i gleda očitanja; slobodno: true → učenik sam bira vrstu procesa
  (gumbi) i vuče stanje od stanja[0]; vrste? = ponuđene vrste;
- "postavi": pocetak (oznaka stanja), proces (vrsta), cilj: {p | V | T: vrijednost}; učenik povuče točku na stanje nakon procesa. Točku računa aplikacija;
  prihvaća se pola koraka mreže (tolerancija?: {V, p} prepisuje). vodilica: true → točka klizi po krivulji procesa (lakše). Poruke za kraj drugog procesa,
  obrnuti omjer i °C umjesto K dodaju se same; krivo?: [{V, p (osi), poruka}];
- "put": učenik slaže put od stanja[0]: za svaku dionicu odabere vrstu procesa i povuče kraj. Točan put = stanja redom povezana s procesi
  (procesi[i] ide od stanja[i] do stanja[i+1]); zatvoren: true → zadnji proces vraća u stanja[0] (obrnut smjer ciklusa dobiva posebnu poruku); vrste?;
- "dionice": za svaki proces iz procesi učenik označi znak (+, 0, −) veličina iz velicina ("W" | "dU" | "Q" ili popis); znakove računa aplikacija
  (W = ∫p dV, ΔU = 3/2 Δ(pV), Q = W + ΔU). Nakon točnog vide se iznosi i ukupni rad ciklusa.`,
    primjer: {
      tip: "plinski_dijagram",
      nacin: "postavi",
      tekst: "Zrak se izotermno stisne dok tlak ne naraste na 125 kPa. Povuci stanje na kraj procesa.",
      V: [0, 0.8],
      p: [0, 150],
      korak: { V: 0.05, p: 25 },
      jedinice: { p: "kPa", V: "L" },
      stanja: [{ oznaka: "1", V: 0.5, p: 100 }],
      pocetak: "1",
      proces: "izotermno",
      cilj: { p: 125 }
    },
    primjeri: [
      {
        tip: "plinski_dijagram",
        nacin: "istrazi",
        tekst: "Povuci stanje plina po putu 1 → 2 → 3 → 4. Gledaj umnožak $pV$ i unutarnju energiju: na izotermi se $U$ ne mijenja.",
        V: [0, 14],
        p: [0, 350],
        korak: { V: 1, p: 50 },
        stanja: [
          { oznaka: "1", V: 2, p: 100 },
          { oznaka: "2", V: 6, p: 100 },
          { oznaka: "3", V: 6, p: 300 },
          { oznaka: "4", V: 12, p: 150 }
        ],
        procesi: [
          { od: "1", do: "2", vrsta: "izobarno" },
          { od: "2", do: "3", vrsta: "izohorno" },
          { od: "3", do: "4", vrsta: "izotermno" }
        ],
        izoterme: true,
        povrsina: true,
        prikazi: ["pV", "U", "W"]
      },
      {
        tip: "plinski_dijagram",
        nacin: "istrazi",
        slobodno: true,
        tekst: "Odaberi proces i vuci stanje. Rad plina je površina ispod krivulje: zelena kad se plin širi, crvena kad se steže.",
        V: [0, 10],
        p: [0, 400],
        korak: { V: 1, p: 50 },
        n: 0.2,
        stanja: [{ oznaka: "1", V: 4, p: 200 }],
        vrste: ["izobarno", "izohorno", "izotermno", "adijabatno"],
        izoterme: true,
        povrsina: true,
        prikazi: ["T", "U", "W", "Q"]
      },
      {
        tip: "plinski_dijagram",
        nacin: "postavi",
        osi: "pT",
        tekst: "Čvrsta boca s plinom pri tlaku 200 kPa zagrije se s 27 °C na 177 °C. Povuci stanje na kraj zagrijavanja.",
        T: [-300, 300],
        p: [0, 400],
        korak: { T: 50, p: 25 },
        jedinice: { p: "kPa", T: "°C" },
        stanja: [{ oznaka: "1", T: 27, p: 200 }],
        pocetak: "1",
        proces: "izohorno",
        cilj: { T: 177 },
        vodilica: true
      },
      {
        tip: "plinski_dijagram",
        nacin: "postavi",
        osi: "VT",
        tekst: "Plin se pri stalnom tlaku zagrijava s 300 K dok mu volumen ne poraste s 10 L na 15 L. Povuci stanje na kraj procesa i očitaj temperaturu.",
        T: [0, 500],
        V: [0, 20],
        korak: { T: 50, V: 2.5 },
        stanja: [{ oznaka: "1", T: 300, V: 10 }],
        pocetak: "1",
        proces: "izobarno",
        cilj: { V: 15 }
      },
      {
        tip: "plinski_dijagram",
        nacin: "put",
        zatvoren: true,
        tekst: "Nacrtaj kružni proces: iz stanja 1 (2 L, 300 kPa) plin se izobarno raširi na 6 L, zatim se izohorno ohladi na 100 kPa, izobarno stisne na 2 L i izohorno vrati u stanje 1.",
        V: [0, 8],
        p: [0, 400],
        korak: { V: 1, p: 50 },
        stanja: [
          { oznaka: "1", V: 2, p: 300 },
          { oznaka: "2", V: 6, p: 300 },
          { oznaka: "3", V: 6, p: 100 },
          { oznaka: "4", V: 2, p: 100 }
        ],
        procesi: [
          { od: "1", do: "2", vrsta: "izobarno" },
          { od: "2", do: "3", vrsta: "izohorno" },
          { od: "3", do: "4", vrsta: "izobarno" },
          { od: "4", do: "1", vrsta: "izohorno" }
        ]
      },
      {
        tip: "plinski_dijagram",
        nacin: "dionice",
        velicina: ["W", "dU", "Q"],
        tekst: "Za svaku dionicu ciklusa označi je li rad plina $W$, promjena unutarnje energije $\\Delta U$ i dovedena toplina $Q$ pozitivna, nula ili negativna.",
        V: [0, 8],
        p: [0, 400],
        korak: { V: 1, p: 100 },
        stanja: [
          { oznaka: "1", V: 2, p: 300 },
          { oznaka: "2", V: 6, p: 300 },
          { oznaka: "3", V: 6, p: 100 },
          { oznaka: "4", V: 2, p: 100 }
        ],
        procesi: [
          { od: "1", do: "2", vrsta: "izobarno" },
          { od: "2", do: "3", vrsta: "izohorno" },
          { od: "3", do: "4", vrsta: "izobarno" },
          { od: "4", do: "1", vrsta: "izohorno" }
        ],
        povrsina: true
      }
    ],
    validate(s) {
      const { list, need } = problems();
      const z = validateZajednicko(s, need);
      if (s.nacin === "istrazi") noAnswerFields(s, need);
      if (!z) return list;
      const osi2 = osiOd(s);
      const procesi = s.procesi ?? [];
      const vrste = s.vrste;
      if (vrste != null)
        need(
          Array.isArray(vrste) && vrste.length >= 1 && vrste.every((v) => VRSTE3.includes(v)) && new Set(vrste).size === vrste.length,
          `vrste mora biti popis različitih od: ${VRSTE3.join(", ")}`
        );
      need(
        !(vrste ?? []).includes("adijabatno") || osi2.x === "V" || z.poznato,
        "adijabata na V–T / p–T dijagramu traži n"
      );
      const zabranjena = (polja) => polja.forEach(
        (f2) => need(!(f2 in s), `polje „${f2}“ ne postoji u načinu ${s.nacin}`)
      );
      if (s.nacin === "istrazi") {
        zabranjena([
          "pocetak",
          "proces",
          "velicina",
          "zatvoren",
          "vodilica",
          "tolerancija"
        ]);
        if (s.slobodno)
          need(
            !procesi.length,
            "istrazi sa slobodno: true ne treba procese (učenik ih bira sam)"
          );
        else {
          need(
            procesi.length >= 1,
            "istrazi treba procese (put po kojem se vuče stanje) ili slobodno: true"
          );
          need(
            lanac2(procesi),
            "procesi u istrazi moraju biti lanac: svaki počinje gdje je prethodni završio"
          );
          need(s.vrste == null, "vrste postoje samo uz slobodno: true");
        }
      }
      if (s.nacin === "postavi") {
        zabranjena(["velicina", "zatvoren", "slobodno", "vrste"]);
        const a = z.puna.find((t) => t.oznaka === s.pocetak);
        need(a, "postavi treba pocetak: oznaka jednog od stanja");
        const vrstaOk = need(
          ["izobarno", "izohorno", "izotermno", "adijabatno"].includes(s.proces),
          "postavi treba proces: izobarno, izohorno, izotermno ili adijabatno"
        );
        const kljucevi = Object.keys(s.cilj ?? {});
        const ciljOk = need(
          s.cilj && typeof s.cilj === "object" && kljucevi.length === 1 && ["p", "V", "T"].includes(kljucevi[0]) && isNum(s.cilj[kljucevi[0]]),
          "postavi treba cilj: točno jedna od {p}, {V} ili {T} s brojem (krajnja vrijednost)"
        );
        if (a && vrstaOk && ciljOk) {
          const q = kljucevi[0];
          need(
            STALNO[s.proces] !== q,
            `cilj ${q} ne može se zadati za ${s.proces} proces (${q} je tada stalan)`
          );
          need(
            q !== "T" || osi2.x === "T" || z.poznato,
            "cilj T na p–V dijagramu traži n ili T početnog stanja"
          );
          need(
            q === "T" ? uK(s, s.cilj.T) > 0 : s.cilj[q] > 0,
            "cilj mora biti pozitivan (T iznad apsolutne nule)"
          );
          const b = STALNO[s.proces] !== q ? stanjeNakon(s) : null;
          if (need(b, "stanje nakon procesa ne postoji (provjeri cilj)")) {
            const kb = koordinate(s, b);
            const ka = koordinate(s, a);
            for (const [os, v] of [
              [osi2.x, kb.x],
              [osi2.y, kb.y]
            ])
              need(
                v >= s[os][0] - 1e-9 && v <= s[os][1] + 1e-9,
                `stanje nakon procesa (${os} = ${formatNumber(v)}) je izvan osi ${os}; proširi raspon`
              );
            need(
              Math.abs(kb.x - ka.x) >= s.korak[osi2.x] - 1e-9 || Math.abs(kb.y - ka.y) >= s.korak[osi2.y] - 1e-9,
              "krajnje stanje mora biti barem jedan korak mreže od početnog (inače je odgovor već na početku)"
            );
            if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
              s.krivo.forEach((kr, i) => {
                need(
                  isNum(kr?.[osi2.x]) && isNum(kr?.[osi2.y]),
                  `krivo[${i}] treba ${osi2.x} i ${osi2.y} (točku na dijagramu)`
                );
                need(isText(kr?.poruka), `krivo[${i}].poruka mora biti tekst`);
                if (isNum(kr?.[osi2.x]) && isNum(kr?.[osi2.y]))
                  need(
                    !istaTocka(s, b, { x: kr[osi2.x], y: kr[osi2.y] }),
                    `krivo[${i}] je točan odgovor`
                  );
              });
            }
          }
        }
      }
      if (s.nacin === "put") {
        zabranjena([
          "pocetak",
          "proces",
          "cilj",
          "velicina",
          "slobodno",
          "vodilica",
          "krivo"
        ]);
        const st = s.stanja;
        const trebaProcesa = st.length - 1 + (s.zatvoren ? 1 : 0);
        if (need(
          st.length >= 2 && procesi.length === trebaProcesa && procesi.length <= 6,
          `put treba 2+ stanja i ${trebaProcesa} procesa (procesi[i] od stanja[i] do stanja[i+1]${s.zatvoren ? ", zadnji natrag u stanja[0]" : ""}), najviše 6 dionica`
        )) {
          procesi.forEach(
            (pr, i) => need(
              pr.od === st[i].oznaka && pr.do === st[(i + 1) % st.length].oznaka,
              `procesi[${i}] mora ići od ${st[i].oznaka} do ${st[(i + 1) % st.length].oznaka} (redom kako su stanja zadana)`
            )
          );
          const ponudjene = vrste ?? zadaneVrste(s);
          need(
            procesi.every((pr) => ponudjene.includes(pr.vrsta)),
            "vrste moraju sadržavati sve vrste procesa iz puta"
          );
          procesi.forEach((pr, i) => {
            if (pr.vrsta !== "ravno") return;
            const k = koordinate(s, z.puna[(i + 1) % st.length]);
            const naMrezi3 = (v, q) => Math.abs(
              (v - s[q][0]) / s.korak[q] - Math.round((v - s[q][0]) / s.korak[q])
            ) < 1e-6;
            need(
              naMrezi3(k.x, osi2.x) && naMrezi3(k.y, osi2.y),
              `procesi[${i}] je ravna crta, pa njegov kraj mora biti na čvoru mreže`
            );
          });
        }
      }
      if (s.nacin === "dionice") {
        zabranjena([
          "pocetak",
          "proces",
          "cilj",
          "slobodno",
          "vodilica",
          "krivo",
          "vrste"
        ]);
        const vel = velicineOd(s);
        need(
          vel.length >= 1 && vel.every((v) => VELICINE3.includes(v)) && new Set(vel).size === vel.length,
          'velicina mora biti "W", "dU", "Q" ili popis od njih'
        );
        need(
          procesi.length >= 1 && procesi.length <= 6,
          "dionice treba 1–6 procesa"
        );
        need(
          lanac2(procesi),
          "procesi moraju biti lanac (svaki počinje gdje je prethodni završio)"
        );
        need(
          osi2.x === "V" || z.poznato || !vel.includes("W") || procesi.every((pr) => pr.vrsta !== "ravno"),
          "ravna crta na V–T / p–T dijagramu traži n"
        );
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "postavi") return checkPostavi2(s, odgovor3);
      if (s.nacin === "put") return checkPut(s, odgovor3);
      if (s.nacin === "dionice") return checkDionice(s, odgovor3);
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "postavi") {
        const b = stanjeNakon(s);
        const q = ciljVelicina(s);
        const osi2 = osiOd(s);
        const dodatno = q === "T" && osi2.x === "V" ? `, T = ${vrijednostTxt(s, "T", izK(s, b.T))}` : "";
        return `${tockaTxt(s, b)}${dodatno}`;
      }
      if (s.nacin === "put") {
        const puna = punaStanja(s);
        return putSpeca(s).map(
          (pr) => `${pr.od} → ${pr.do}: ${KRATKO_VRSTE[pr.vrsta]} do (${tockaTxt(
            s,
            puna.find((t) => t.oznaka === pr.do)
          )})`
        ).join("; ");
      }
      if (s.nacin === "dionice") {
        const vel = velicineOd(s);
        return znakoviDionica(s).map(
          (d, i) => `${s.procesi[i].od} → ${s.procesi[i].do}: ${vel.map((v) => `${VELICINA_TXT[v]} ${ZNAK_TXT[d[v]]}`).join(", ")}`
        ).join("; ");
      }
      return "";
    }
  };
  var zadaneVrste = (s) => {
    const iz = (s.procesi ?? []).map((pr) => pr.vrsta);
    return [
      "izobarno",
      "izohorno",
      "izotermno",
      ...iz.includes("adijabatno") ? ["adijabatno"] : [],
      ...iz.includes("ravno") ? ["ravno"] : []
    ];
  };
  function checkPostavi2(s, odgovor3) {
    const t = tockaOdgovora(s, odgovor3);
    if (!t)
      return {
        tocno: false,
        poruka: "Povuci točku na stanje nakon procesa.",
        prazno: true
      };
    const b = stanjeNakon(s);
    if (istaTocka(s, b, t)) return { tocno: true };
    const a = stanjeOd2(s, s.pocetak);
    if (istaTocka(s, a, t))
      return {
        tocno: false,
        poruka: "Točka je još u početnom stanju. Povuci je tamo gdje plin završi nakon procesa.",
        prazno: true
      };
    const osi2 = osiOd(s);
    const vlastite = (s.krivo ?? []).map((k) => ({
      st: izTocke(s, k[osi2.x], k[osi2.y]),
      poruka: k.poruka
    }));
    const pogodak = [...vlastite, ...kriveTocke(s)].find(
      (k) => istaTocka(s, k.st, t)
    );
    if (pogodak) return { tocno: false, poruka: pogodak.poruka };
    const q = ciljVelicina(s);
    const tol2 = tolerancije(s);
    const stu = izTocke(s, t.x, t.y);
    if (q === osi2.x || q === osi2.y) {
      const os = q === osi2.x ? "x" : "y";
      const zadano = s.cilj[q];
      if (Math.abs(t[os] - zadano) > tol2[os])
        return {
          tocno: false,
          poruka: `Na kraju procesa mora biti ${q} = ${vrijednostTxt(s, q, zadano)}, a tvoja točka je na ${q} = ${vrijednostTxt(s, q, t[os])}.`
        };
    } else if (q === "T" && konstanta(s).poznato && Math.abs(stu.T - uK(s, s.cilj.T)) > 0.03 * uK(s, s.cilj.T)) {
      return {
        tocno: false,
        poruka: `Na kraju procesa temperatura mora biti ${vrijednostTxt(s, "T", s.cilj.T)}, a u tvojoj točki je ${vrijednostTxt(s, "T", izK(s, stu.T))}. Pomakni točku po krivulji procesa.`
      };
    }
    const zakon2 = {
      izobarno: "Pri stalnom tlaku vrijedi $V/T$ = konst.: volumen raste u istom omjeru kao temperatura (u K).",
      izohorno: "Pri stalnom volumenu vrijedi $p/T$ = konst.: tlak raste u istom omjeru kao temperatura (u K).",
      izotermno: "Pri stalnoj temperaturi vrijedi $pV$ = konst.: koliko puta tlak naraste, toliko se puta volumen smanji.",
      adijabatno: "Adijabatski vrijedi $pV^{5/3}$ = konst.: krivulja je strmija od izoterme."
    }[s.proces];
    return {
      tocno: false,
      poruka: s.savjet || `Zadana vrijednost je dobra, ali proces nije. ${zakon2}`
    };
  }
  function checkPut(s, odgovor3) {
    const tocke = (odgovor3?.tocke ?? []).map((o) => tockaOdgovora(s, o));
    const vrste = odgovor3?.vrste ?? [];
    if (tocke.length < 2 || tocke.some((t) => !t) || vrste.length !== tocke.length - 1)
      return {
        tocno: false,
        poruka: "Dodaj barem jednu dionicu: odaberi proces i povuci kraj.",
        prazno: true
      };
    const puna = punaStanja(s);
    const put = putSpeca(s);
    const sredi2 = (koraci) => {
      for (let i = 0; i < Math.min(koraci.length, vrste.length); i++) {
        const pr = koraci[i];
        const kraj3 = puna.find((t) => t.oznaka === pr.do);
        if (vrste[i] !== pr.vrsta) return { i, vrsta: true, pr };
        if (!istaTocka(s, kraj3, tocke[i + 1]))
          return { i, kraj: true, pr, cilj: kraj3 };
      }
      return null;
    };
    const greska = sredi2(put);
    if (!greska && vrste.length === put.length) return { tocno: true };
    if (s.zatvoren && greska) {
      const unatrag = [...put].reverse().map((pr) => ({ od: pr.do, do: pr.od, vrsta: pr.vrsta }));
      if (!sredi2(unatrag) && vrste.length >= unatrag.length)
        return {
          tocno: false,
          poruka: "Obišao si ciklus u suprotnom smjeru (suprotno od kazaljke na satu). Takav ciklus je hladnjak: ukupni rad plina bio bi negativan. Kreni od 1 redom kako piše u zadatku."
        };
    }
    if (greska) {
      const br = greska.i + 1;
      const oznaka = `Dionica ${br} (${greska.pr.od} → ${greska.pr.do})`;
      if (greska.vrsta)
        return {
          tocno: false,
          poruka: `${oznaka} nije ${ZENSKI[vrste[greska.i]] ?? "dobro odabrana"}: ${greska.pr.vrsta === "ravno" ? "to je ravna crta između dva stanja" : `pročitaj opet što se u njoj ne mijenja. To je ${ZENSKI[greska.pr.vrsta]} dionica ${OPIS_VRSTE[greska.pr.vrsta].replace(/^\S+ /, "")}`}.`
        };
      const osi2 = osiOd(s);
      const kc = koordinate(s, greska.cilj);
      const t = tocke[greska.i + 1];
      const tol2 = tolerancije(s);
      const krivaOs = Math.abs(t.x - kc.x) > tol2.x ? ["x", osi2.x] : ["y", osi2.y];
      const vise = t[krivaOs[0]] > kc[krivaOs[0]];
      const zenski = krivaOs[1] === "T";
      return {
        tocno: false,
        poruka: `${oznaka} je dobra vrsta procesa, ali završava na krivom mjestu: ${IME[krivaOs[1]]} na kraju je ${vrijednostTxt(s, krivaOs[1], t[krivaOs[0]])}, a prema zadatku treba biti ${vise ? zenski ? "manja" : "manji" : zenski ? "veća" : "veći"}.`
      };
    }
    if (vrste.length < put.length)
      return {
        tocno: false,
        poruka: `Put još nije gotov: nacrtano je ${vrste.length} od ${put.length} dionica.${s.zatvoren ? " Ciklus se mora vratiti u stanje 1." : ""}`
      };
    return {
      tocno: false,
      poruka: `Put ima previše dionica: treba ih ${put.length}. Obriši višak gumbom „Poništi“.`
    };
  }
  var OBJASNI = {
    W: (d) => d.vrsta === "izohorno" ? "volumen se ne mijenja, pa plin ne obavlja rad ($W$ = 0)" : d.W === "+" ? "plin se širi (volumen raste), pa obavlja pozitivan rad" : d.W === "-" ? "plin se steže (volumen pada): rad plina je negativan, okolina radi na plinu" : "volumen je na kraju isti",
    dU: (d) => d.vrsta === "izotermno" ? "temperatura je stalna, a $U$ ovisi samo o temperaturi, pa je $\\Delta U$ = 0" : `$U = \\frac{3}{2}pV$, a umnožak $pV$ ${d.dU === "+" ? "raste (temperatura raste)" : d.dU === "-" ? "pada (temperatura pada)" : "ostaje isti"}`,
    Q: (d) => d.vrsta === "adijabatno" ? "adijabatski proces po definiciji nema izmjene topline ($Q$ = 0)" : d.vrsta === "izotermno" ? "$\\Delta U$ = 0, pa je $Q = W$" : d.vrsta === "izohorno" ? "nema rada, pa je $Q = \\Delta U$" : `$Q = W + \\Delta U$, a ${d.W === d.dU ? `i $W$ i $\\Delta U$ su ${d.W === "+" ? "pozitivni" : "negativni"}` : "pogledaj koji je veći po iznosu"}`
  };
  function checkDionice(s, odgovor3) {
    const vel = velicineOd(s);
    const zn2 = znakoviDionica(s);
    if (!Array.isArray(odgovor3) || odgovor3.length !== zn2.length || odgovor3.some((o) => vel.some((v) => !ZNAKOVI2.includes(o?.[v]))))
      return {
        tocno: false,
        poruka: "Označi znak za svaku dionicu.",
        prazno: true
      };
    for (let i = 0; i < zn2.length; i++) {
      for (const v of vel) {
        if (odgovor3[i][v] !== zn2[i][v])
          return {
            tocno: false,
            poruka: s.savjet || `${s.procesi[i].od} → ${s.procesi[i].do}, ${VELICINA_TXT[v]}: ${OBJASNI[v](zn2[i])}.`
          };
      }
    }
    return { tocno: true };
  }

  // src/lib/lesson/katalog/fizika/plin_posuda.js
  var NACINI10 = ["istrazi", "predvidi", "odaberi"];
  var KLIPOVI = ["zakljucan", "pomican", "rucni"];
  var TOPLINE = ["termostat", "izolirano"];
  var KONTROLE = ["T", "V", "n"];
  var PRIKAZI4 = ["p", "V", "T", "n", "N", "Ek", "vef", "W"];
  var PITAJ = ["p", "V", "T", "n", "Ek", "vef"];
  var ODABERI2 = ["vef", "p", "Ek", "T", "n", "V"];
  var SMJEROVI = ["gore", "dolje", "isto"];
  var TOL_FAKTOR = 0.01;
  function jediniceOd2(s) {
    return { p: s.jedinice?.p ?? "kPa", V: s.jedinice?.V ?? "L" };
  }
  function stanje2(s, { n, V, T, M }) {
    const j = jediniceOd2(s);
    const pPa = n * R * T / (V * V_JEDINICE[j.V]);
    return {
      n,
      V,
      T,
      M,
      p: pPa / P_JEDINICE[j.p],
      N: n * NA,
      Ek: 1.5 * KB * T,
      vef: Math.sqrt(3 * R * T / (M / 1e3))
    };
  }
  function nakonPromjene(s, posuda, promjena3 = s.promjena ?? {}) {
    const fn = promjena3.n ?? 1;
    const fT = promjena3.T ?? 1;
    const fV = promjena3.V ?? 1;
    if (posuda.klip === "zakljucan")
      return stanje2(s, { ...posuda, n: posuda.n * fn, T: posuda.T * fT });
    if (posuda.klip === "pomican")
      return stanje2(s, {
        ...posuda,
        n: posuda.n * fn,
        T: posuda.T * fT,
        V: posuda.V * fn * fT
      });
    if (posuda.toplina === "izolirano")
      return stanje2(s, {
        ...posuda,
        V: posuda.V * fV,
        T: posuda.T * fV ** (1 - GAMA)
      });
    if (posuda.toplina === "termostat")
      return stanje2(s, { ...posuda, n: posuda.n * fn, V: posuda.V * fV });
    return stanje2(s, {
      ...posuda,
      n: posuda.n * fn,
      T: posuda.T * fT,
      V: posuda.V * fV
    });
  }
  function faktori(s, promjena3 = s.promjena) {
    const p = s.posude[0];
    const a = stanje2(s, p);
    const b = nakonPromjene(s, p, promjena3);
    return Object.fromEntries(PITAJ.map((q) => [q, b[q] / a[q]]));
  }
  var smjerOd = (f2) => f2 > 1 + 1e-9 ? "gore" : f2 < 1 - 1e-9 ? "dolje" : "isto";
  var faktorPolja = (s) => [].concat(s.faktor ?? []);
  var SMJER_TXT = {
    gore: "raste",
    dolje: "pada",
    isto: "ne mijenja se"
  };
  var STRELICA = { gore: "↑", dolje: "↓", isto: "=" };
  var OPIS_PROMJENE = {
    n: (f2) => f2 < 1 ? `izlazak ${formatNumber(Math.round((1 - f2) * 1e3) / 10)} % čestica (n × ${formatNumber(f2)})` : `dodavanje plina (n × ${formatNumber(f2)})`,
    T: (f2) => `${f2 > 1 ? "grijanje" : "hlađenje"} (T × ${formatNumber(f2)})`,
    V: (f2) => `${f2 < 1 ? "stiskanje" : "širenje"} (V × ${formatNumber(f2)})`
  };
  function zakon(s, q) {
    const p = s.posude[0];
    if (q === "Ek")
      return "Srednja kinetička energija čestice ovisi samo o termodinamičkoj temperaturi: $\\bar{E}_k = \\frac{3}{2}kT$.";
    if (q === "vef")
      return "Efektivna brzina razmjerna je korijenu temperature: $v_{ef} = \\sqrt{3RT/M}$.";
    if (q === "n")
      return "Količina plina mijenja se samo kad plin izlazi kroz ventil ili se dodaje.";
    if (q === "V")
      return p.klip === "zakljucan" ? "Boca je čvrsta, pa joj se volumen ne mijenja." : p.klip === "pomican" ? "Klip je slobodan, pa je tlak stalan i volumen je razmjeran umnošku $nT$ ($V = nRT/p$)." : "Volumen određuje položaj klipa.";
    if (q === "T")
      return p.toplina === "termostat" ? "Termostat drži temperaturu stalnom." : p.toplina === "izolirano" ? "Izolirana posuda ne izmjenjuje toplinu: rad pri stiskanju povećava unutarnju energiju, pa temperatura raste (pri širenju pada)." : "Temperatura se mijenja samo grijanjem ili hlađenjem.";
    if (p.klip === "zakljucan")
      return "Volumen je stalan, pa je tlak razmjeran umnošku $nT$ ($p = nRT/V$).";
    if (p.klip === "pomican")
      return "Tlak plina uvijek je jednak tlaku koji stvara slobodni klip s utegom, pa se ne mijenja (mijenja se volumen).";
    if (p.toplina === "termostat")
      return "Temperatura je stalna, pa je tlak razmjeran $n/V$ ($pV$ = konst. za isti plin).";
    if (p.toplina === "izolirano")
      return "U izoliranoj posudi pri stiskanju raste i temperatura, pa tlak raste više nego obrnuto razmjerno volumenu ($pV^{5/3}$ = konst.).";
    return "Vrijedi $p = nRT/V$: pomnoži faktore promjene $n$ i $T$ i podijeli faktorom promjene $V$.";
  }
  function kriviFaktori(s, q) {
    const prom = s.promjena ?? {};
    const f2 = faktori(s)[q];
    const out = [];
    const kljucevi = Object.keys(prom);
    if (kljucevi.length > 1) {
      for (const k of kljucevi) {
        const bez3 = { ...prom, [k]: 1 };
        out.push({
          faktor: faktori(s, bez3)[q],
          poruka: `Nisi uračunao ${OPIS_PROMJENE[k](prom[k])}. Na kraju obje promjene djeluju zajedno.`
        });
      }
      const zbroj2 = 1 + kljucevi.reduce((a, k) => a + (faktori(s, { [k]: prom[k] })[q] - 1), 0);
      out.push({
        faktor: zbroj2,
        poruka: "Faktori promjena se množe, a ne zbrajaju."
      });
    }
    if (isNum(prom.n) && prom.n < 1)
      out.push({
        faktor: faktori(s, { ...prom, n: 1 - prom.n })[q],
        poruka: `Izašlo je ${formatNumber(Math.round((1 - prom.n) * 1e3) / 10)} % čestica, pa ih je ostalo ${formatNumber(Math.round(prom.n * 1e3) / 10)} %: n × ${formatNumber(prom.n)}.`
      });
    if (s.posude[0].toplina === "izolirano" && isNum(prom.V) && (q === "p" || q === "T")) {
      const izotermno = q === "p" ? 1 / prom.V : 1;
      out.push({
        faktor: izotermno,
        poruka: "To bi vrijedilo pri stalnoj temperaturi. Posuda je izolirana, pa se pri stiskanju plin i zagrije."
      });
    }
    if (Math.abs(f2 - 1) > 1e-9)
      out.push({
        faktor: 1 / f2,
        poruka: "Obrnuo si omjer: podijelio si umjesto pomnožio (ili obrnuto)."
      });
    return out.filter((k) => Math.abs(k.faktor - f2) > TOL_FAKTOR * Math.abs(f2));
  }
  var usporedba = (s) => s.posude.map((p) => stanje2(s, p)[s.pitanje]);
  function tocnaPosuda(s) {
    const [a, b] = usporedba(s);
    if (Math.abs(a - b) <= 1e-6 * Math.max(Math.abs(a), Math.abs(b)))
      return "jednako";
    const veca = a > b ? 0 : 1;
    return s.manja ? 1 - veca : veca;
  }
  function validatePosuda(p, i, need) {
    const w = `posude[${i}]`;
    need(isText(p?.oznaka), `${w}.oznaka mora biti tekst (npr. "helij")`);
    need(
      isNum(p?.M) && p.M > 0 && p.M <= 400,
      `${w}.M (molarna masa u g/mol) mora biti broj između 0 i 400`
    );
    need(isNum(p?.n) && p.n > 0, `${w}.n (mol) mora biti pozitivan broj`);
    need(isNum(p?.V) && p.V > 0, `${w}.V mora biti pozitivan broj`);
    need(
      isNum(p?.T) && p.T > 0 && p.T <= 5e3,
      `${w}.T (u K) mora biti između 0 i 5000`
    );
    need(
      KLIPOVI.includes(p?.klip),
      `${w}.klip mora biti "zakljucan", "pomican" ili "rucni"`
    );
    if (p?.toplina != null)
      need(
        TOPLINE.includes(p.toplina) && p.klip === "rucni",
        `${w}.toplina mora biti "termostat" ili "izolirano" i postoji samo uz klip "rucni"`
      );
    need(
      Object.keys(p ?? {}).every(
        (f2) => ["oznaka", "M", "n", "V", "T", "klip", "toplina"].includes(f2)
      ),
      `${w} smije imati samo oznaka, M, n, V, T, klip i toplina`
    );
  }
  var plin_posuda = {
    tip: "plin_posuda",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Čestice plina u cilindru s klipom (animacija; brzina čestica razmjerna je √(T/M), broj čestica količini n). Tlak se računa iz pV = nRT.
posude: [{oznaka, M (g/mol), n (mol), V, T (K), klip: "zakljucan" (čvrsta boca, V stalan) | "pomican" (klip s utegom, p stalan) | "rucni" (V mijenja učenik),
toplina?: "termostat" (T stalna) | "izolirano" (adijabatski, TV^(2/3) = konst.) – samo uz "rucni"}] (1–2), jedinice?: {p: "kPa" (Pa | kPa | bar | MPa), V: "L" (L | dm³ | m³ | mL | cm³)},
prikazi?: popis od ${PRIKAZI4.join(", ")} (očitanja; zadano p, V, T; Ek = srednja kinetička energija čestice, vef = efektivna brzina, W = rad plina od početka), tekst, nacin:
- "istrazi" (nije zadatak): kontrole: popis od "T" (grij/hladi), "V" (pomiči klip, samo uz "rucni"), "n" (ventil); raspon?: {T: [od, do], V: […], n: […]} za klizače.
  Uz dvije posude kontrole mijenjaju obje (početne vrijednosti kontroliranih veličina moraju biti jednake);
- "predvidi": jedna posuda, promjena: {n?, T?, V?} faktori (n 0,75 = izađe 25 % čestica; V samo uz "rucni"; uz termostat bez T, uz izolirano samo V),
  pitaj: popis od ${PITAJ.join(", ")} – za svaku učenik bira raste / pada / ostaje isti; faktor?: veličina ili popis veličina za koje upisuje i faktor (npr. p × 1,5).
  Točne faktore računa aplikacija; krivi faktori (zanemarena jedna promjena, zbroj umjesto umnoška, obrnut omjer, izotermno umjesto adijabatski) dodaju se sami;
  krivo?: [{velicina, faktor, poruka}]. Nakon točnog odgovora animacija izvede promjenu;
- "odaberi": dvije posude, pitanje: ${ODABERI2.join(" | ")}, manja?: true (traži se manja vrijednost); učenik dodirne posudu ili „jednako“.`,
    primjer: {
      tip: "plin_posuda",
      nacin: "predvidi",
      tekst: "Kroz rupicu iz čvrste boce izađe 25 % čestica, a termodinamička temperatura preostalog plina poraste 2 puta. Što se dogodi s tlakom u boci?",
      posude: [
        { oznaka: "kisik", M: 32, n: 2, V: 10, T: 300, klip: "zakljucan" }
      ],
      promjena: { n: 0.75, T: 2 },
      pitaj: ["p"],
      faktor: "p"
    },
    primjeri: [
      {
        tip: "plin_posuda",
        nacin: "istrazi",
        tekst: "U boci je 64 g kisika (2 mol). Mijenjaj temperaturu i količinu plina i gledaj tlak.",
        posude: [
          { oznaka: "kisik", M: 32, n: 2, V: 10, T: 300, klip: "zakljucan" }
        ],
        kontrole: ["T", "n"],
        prikazi: ["p", "V", "T", "n", "N"]
      },
      {
        tip: "plin_posuda",
        nacin: "istrazi",
        tekst: "Plin je ispod slobodnog klipa s utegom. Grij ga i gledaj kako se klip diže pri stalnom tlaku i koliki rad plin obavi.",
        posude: [
          { oznaka: "zrak", M: 29, n: 0.2, V: 4, T: 300, klip: "pomican" }
        ],
        kontrole: ["T"],
        prikazi: ["p", "V", "T", "W"]
      },
      {
        tip: "plin_posuda",
        nacin: "istrazi",
        tekst: "Obje posude su na istoj temperaturi. Mijenjaj je i usporedi molekule helija i argona.",
        posude: [
          { oznaka: "helij", M: 4, n: 1, V: 10, T: 300, klip: "zakljucan" },
          { oznaka: "argon", M: 40, n: 1, V: 10, T: 300, klip: "zakljucan" }
        ],
        kontrole: ["T"],
        prikazi: ["T", "Ek", "vef"]
      },
      {
        tip: "plin_posuda",
        nacin: "istrazi",
        tekst: "Posuda je toplinski izolirana. Stisni plin klipom i gledaj temperaturu.",
        posude: [
          {
            oznaka: "helij",
            M: 4,
            n: 0.5,
            V: 12,
            T: 300,
            klip: "rucni",
            toplina: "izolirano"
          }
        ],
        kontrole: ["V"],
        raspon: { V: [3, 12] },
        prikazi: ["p", "V", "T", "W"]
      },
      {
        tip: "plin_posuda",
        nacin: "predvidi",
        tekst: "Plin u izoliranoj posudi adijabatski se stisne na polovicu volumena. Što se dogodi s temperaturom, tlakom i brzinom molekula?",
        posude: [
          {
            oznaka: "helij",
            M: 4,
            n: 0.5,
            V: 12,
            T: 300,
            klip: "rucni",
            toplina: "izolirano"
          }
        ],
        promjena: { V: 0.5 },
        pitaj: ["T", "p", "vef"]
      },
      {
        tip: "plin_posuda",
        nacin: "odaberi",
        tekst: "Helij i argon su na istoj temperaturi. U kojoj se posudi molekule gibaju brže?",
        posude: [
          { oznaka: "helij", M: 4, n: 1, V: 10, T: 300, klip: "zakljucan" },
          { oznaka: "argon", M: 40, n: 1, V: 10, T: 300, klip: "zakljucan" }
        ],
        pitanje: "vef"
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI10.includes(s.nacin),
        `plin_posuda.nacin mora biti jedan od: ${NACINI10.join(", ")}`
      );
      need(isText(s.tekst), "plin_posuda treba „tekst“");
      const j = s.jedinice ?? {};
      need(
        j.p == null || j.p in P_JEDINICE,
        `jedinice.p mora biti jedna od: ${Object.keys(P_JEDINICE).join(", ")}`
      );
      need(
        j.V == null || j.V in V_JEDINICE,
        `jedinice.V mora biti jedna od: ${Object.keys(V_JEDINICE).join(", ")}`
      );
      if (s.jedinice != null)
        need(
          typeof s.jedinice === "object" && Object.keys(s.jedinice).every((q) => ["p", "V"].includes(q)),
          "jedinice smije imati samo p i V"
        );
      if (s.prikazi != null)
        need(
          Array.isArray(s.prikazi) && s.prikazi.length >= 1 && s.prikazi.every((q) => PRIKAZI4.includes(q)) && new Set(s.prikazi).size === s.prikazi.length,
          `prikazi je popis različitih od: ${PRIKAZI4.join(", ")}`
        );
      if (s.savjet != null) need(isText(s.savjet), "savjet mora biti tekst");
      if (!need(
        Array.isArray(s.posude) && s.posude.length >= 1 && s.posude.length <= 2,
        "plin_posuda treba posude: 1–2 posude"
      ))
        return list;
      s.posude.forEach((p, i) => validatePosuda(p, i, need));
      if (list.length) return list;
      const zabranjena = (polja) => polja.forEach(
        (f2) => need(!(f2 in s), `polje „${f2}“ ne postoji u načinu ${s.nacin}`)
      );
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        zabranjena(["promjena", "pitaj", "faktor", "pitanje", "manja"]);
        const k = s.kontrole ?? [];
        if (need(
          Array.isArray(k) && k.length >= 1 && k.every((q) => KONTROLE.includes(q)) && new Set(k).size === k.length,
          "istrazi treba kontrole: popis od T, V, n"
        )) {
          for (const p of s.posude) {
            need(
              !k.includes("V") || p.klip === "rucni",
              `kontrola V (pomicanje klipa) traži klip "rucni" (${p.oznaka})`
            );
            need(
              !k.includes("T") || p.toplina !== "izolirano",
              `izoliranu posudu (${p.oznaka}) ne može se grijati: makni kontrolu T`
            );
            need(
              !k.includes("n") || p.toplina !== "izolirano",
              `izoliranoj posudi (${p.oznaka}) ne ispušta se plin: makni kontrolu n`
            );
          }
          if (s.posude.length === 2)
            for (const q of k)
              need(
                s.posude[0][q] === s.posude[1][q],
                `uz dvije posude kontrola ${q} mijenja obje, pa početni ${q} mora biti jednak`
              );
        }
        if (s.raspon != null) {
          if (need(
            typeof s.raspon === "object" && Object.keys(s.raspon).every((q) => k.includes(q)),
            "raspon smije imati samo veličine iz kontrole"
          )) {
            for (const [q, r] of Object.entries(s.raspon)) {
              if (need(
                raspon(r) && r[0] > 0,
                `raspon.${q} mora biti [od, do] uz 0 < od < do`
              ))
                need(
                  s.posude.every((p) => p[q] >= r[0] && p[q] <= r[1]),
                  `početni ${q} mora biti unutar raspon.${q}`
                );
            }
          }
        }
      }
      if (s.nacin === "predvidi") {
        zabranjena(["kontrole", "raspon", "pitanje", "manja"]);
        need(s.posude.length === 1, "predvidi ima jednu posudu");
        const p = s.posude[0];
        const prom = s.promjena;
        if (need(
          prom && typeof prom === "object" && Object.keys(prom).length >= 1 && Object.entries(prom).every(
            ([q, f2]) => KONTROLE.includes(q) && isNum(f2) && f2 > 0 && Math.abs(f2 - 1) > 1e-9
          ),
          "predvidi treba promjena: {n?, T?, V?} s faktorima > 0 i različitim od 1"
        )) {
          need(
            !("V" in prom) || p.klip === "rucni",
            'promjena V traži klip "rucni" (zaključan klip drži volumen, slobodni ga mijenja sam)'
          );
          need(
            !("T" in prom) || p.toplina == null,
            "uz termostat ili izolaciju temperatura se ne zadaje: mijenja se sama (ili ostaje stalna)"
          );
          need(
            p.toplina !== "izolirano" || Object.keys(prom).join() === "V",
            "u izoliranoj posudi zadaje se samo promjena V"
          );
          need(
            !isNum(prom.n) || prom.n <= 1 || p.toplina !== "izolirano",
            "u izoliranu posudu ne dodaje se plin"
          );
        }
        const pitaj = s.pitaj;
        if (need(
          Array.isArray(pitaj) && pitaj.length >= 1 && pitaj.length <= 4 && pitaj.every((q) => PITAJ.includes(q)) && new Set(pitaj).size === pitaj.length,
          `pitaj je popis 1–4 različite od: ${PITAJ.join(", ")}`
        )) {
          const fp = faktorPolja(s);
          need(
            s.faktor == null || fp.length >= 1 && fp.every((q) => pitaj.includes(q)),
            "faktor mora biti veličina (ili popis veličina) iz pitaj"
          );
          if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
            const f2 = list.length ? null : faktori(s);
            s.krivo.forEach((k, i) => {
              need(
                fp.includes(k?.velicina),
                `krivo[${i}].velicina mora biti veličina iz faktor`
              );
              need(
                isNum(k?.faktor) && isText(k?.poruka),
                `krivo[${i}] treba faktor (broj) i poruku`
              );
              if (f2 && isNum(k?.faktor) && fp.includes(k?.velicina))
                need(
                  Math.abs(k.faktor - f2[k.velicina]) > TOL_FAKTOR * f2[k.velicina],
                  `krivo[${i}].faktor je točan odgovor`
                );
            });
          }
        }
      }
      if (s.nacin === "odaberi") {
        zabranjena([
          "kontrole",
          "raspon",
          "promjena",
          "pitaj",
          "faktor",
          "krivo"
        ]);
        need(s.posude.length === 2, "odaberi treba dvije posude");
        need(
          ODABERI2.includes(s.pitanje),
          `pitanje mora biti jedno od: ${ODABERI2.join(", ")}`
        );
        if (s.manja != null)
          need(typeof s.manja === "boolean", "manja mora biti true ili false");
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "odaberi") return checkOdaberi(s, odgovor3);
      if (s.nacin === "predvidi") return checkPredvidi(s, odgovor3);
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "odaberi") {
        const t = tocnaPosuda(s);
        return t === "jednako" ? "jednako u obje posude" : s.posude[t].oznaka;
      }
      if (s.nacin === "predvidi") {
        const f2 = faktori(s);
        const fp = faktorPolja(s);
        return s.pitaj.map(
          (q) => `${q} ${STRELICA[smjerOd(f2[q])]} ${SMJER_TXT[smjerOd(f2[q])]}${fp.includes(q) ? ` (× ${formatNumber(Number(f2[q].toPrecision(3)))})` : ""}`
        ).join("; ");
      }
      return "";
    }
  };
  function checkPredvidi(s, odgovor3) {
    const f2 = faktori(s);
    const fp = faktorPolja(s);
    for (const q of s.pitaj)
      if (!SMJEROVI.includes(odgovor3?.[q]?.smjer))
        return {
          tocno: false,
          poruka: "Za svaku veličinu odaberi raste, pada ili ostaje isti.",
          prazno: true
        };
    for (const q of s.pitaj) {
      const tocan = smjerOd(f2[q]);
      if (odgovor3[q].smjer !== tocan)
        return {
          tocno: false,
          poruka: s.savjet || `${cap(IME[q])} ${odgovor3[q].smjer === "isto" ? "se mijenja" : `ne ${SMJER_TXT[odgovor3[q].smjer]}`}. ${zakon(s, q)}`
        };
    }
    for (const q of s.pitaj) {
      if (!fp.includes(q) || smjerOd(f2[q]) === "isto") continue;
      const unos = parseNumber(
        String(odgovor3[q].faktor ?? "").replace(/^[×x*]\s*/, "")
      );
      if (unos == null)
        return {
          tocno: false,
          poruka: `Upiši koliko puta se promijeni ${IME[q]} (npr. 1,5).`,
          prazno: true
        };
      if (Math.abs(unos - f2[q]) <= TOL_FAKTOR * f2[q]) continue;
      const hit = [
        ...(s.krivo ?? []).filter((k) => k.velicina === q),
        ...kriviFaktori(s, q)
      ].find((k) => Math.abs(unos - k.faktor) <= TOL_FAKTOR * Math.abs(k.faktor));
      if (hit) return { tocno: false, poruka: hit.poruka };
      return {
        tocno: false,
        poruka: s.savjet || `Smjer je dobar, ali faktor nije. ${zakon(s, q)}`
      };
    }
    return { tocno: true };
  }
  function checkOdaberi(s, odgovor3) {
    if (odgovor3 !== 0 && odgovor3 !== 1 && odgovor3 !== "jednako")
      return {
        tocno: false,
        poruka: "Dodirni posudu (ili „jednako“).",
        prazno: true
      };
    const t = tocnaPosuda(s);
    if (odgovor3 === t) return { tocno: true };
    const [a, b] = s.posude;
    const poruka2 = {
      vef: t === "jednako" ? "Molekule imaju jednaku masu i temperaturu, pa su jednako brze." : `$v_{ef} = \\sqrt{3RT/M}$: ${a.T === b.T ? "na istoj temperaturi lakše molekule (manja molarna masa) gibaju se brže" : "brzina raste s temperaturom, a pada s molarnom masom"}.`,
      Ek: t === "jednako" ? "Srednja kinetička energija ovisi samo o temperaturi ($\\bar{E}_k = \\frac{3}{2}kT$), a ne o vrsti plina. Na istoj temperaturi jednaka je." : "Srednja kinetička energija ovisi samo o temperaturi: veća je gdje je temperatura viša.",
      T: "Pogledaj termometre: temperatura je zadana za svaku posudu.",
      p: `Tlak je $p = nRT/V$ i ne ovisi o vrsti plina${t === "jednako" ? ": uz iste n, T i V tlak je jednak" : ""}.`,
      n: "Količina plina je broj molova: usporedi n.",
      V: "Usporedi volumene posuda."
    }[s.pitanje];
    return { tocno: false, poruka: s.savjet || poruka2 };
  }
  var cap = (t) => t.charAt(0).toUpperCase() + t.slice(1);

  // src/lib/lesson/katalog/fizika/_elektrostatika.js
  var K = 9e9;
  var E_NABOJ = 16e-20;
  var M_E = 911e-33;
  var M_P = 167e-29;
  var EPS0 = 885e-14;
  var NC = 1e-9;
  var CM = 0.01;
  var MM = 1e-3;
  var deg = (r) => r * 180 / Math.PI;
  var kutVektora2 = (x, y) => (deg(Math.atan2(y, x)) + 360) % 360;
  function razlikaKuta2(a, b) {
    const d = Math.abs(((a - b) % 360 + 540) % 360 - 180);
    return d;
  }
  function poljeNaboja(n, x, y, er = 1) {
    const dx = (x - n.x) * CM;
    const dy = (y - n.y) * CM;
    const r2 = dx * dx + dy * dy;
    if (r2 < 1e-12) return null;
    const r = Math.sqrt(r2);
    const E2 = K * n.q * NC / (er * r2);
    return { Ex: E2 * dx / r, Ey: E2 * dy / r, E: Math.abs(E2) };
  }
  function poljeKugle(k, x, y, er = 1) {
    const r = Math.hypot(x - k.x, y - k.y);
    if (r < k.R - 1e-9) return { Ex: 0, Ey: 0, E: 0, unutra: true };
    return { ...poljeNaboja(k, x, y, er), unutra: false };
  }
  function polje(s, x, y, er = sredstvoEr(s)) {
    const sastavnice = (s.naboji ?? []).map((n) => ({
      oznaka: n.oznaka,
      ...poljeNaboja(n, x, y, er) ?? { Ex: NaN, Ey: NaN, E: Infinity }
    }));
    if (s.kugla)
      sastavnice.push({
        oznaka: s.kugla.oznaka ?? "Q",
        ...poljeKugle(s.kugla, x, y, er)
      });
    const Ex = sastavnice.reduce((a, c) => a + c.Ex, 0);
    const Ey = sastavnice.reduce((a, c) => a + c.Ey, 0);
    return { Ex, Ey, E: Math.hypot(Ex, Ey), sastavnice };
  }
  var sredstvoEr = (s) => typeof s?.sredstvo?.er === "number" ? s.sredstvo.er : 1;
  function silaNa(s, i, er = sredstvoEr(s)) {
    const n = s.naboji[i];
    const sastavnice = s.naboji.map((o, j) => {
      if (j === i) return null;
      const p = poljeNaboja(o, n.x, n.y, er);
      if (!p) return null;
      return {
        oznaka: o.oznaka,
        Fx: p.Ex * n.q * NC,
        Fy: p.Ey * n.q * NC,
        F: Math.abs(p.E * n.q * NC)
      };
    }).filter(Boolean);
    if (s.kugla) {
      const p = poljeKugle(s.kugla, n.x, n.y, er);
      sastavnice.push({
        oznaka: s.kugla.oznaka ?? "Q",
        Fx: p.Ex * n.q * NC,
        Fy: p.Ey * n.q * NC,
        F: Math.abs(p.E * n.q * NC)
      });
    }
    const Fx = sastavnice.reduce((a, c) => a + c.Fx, 0);
    const Fy = sastavnice.reduce((a, c) => a + c.Fy, 0);
    return { Fx, Fy, F: Math.hypot(Fx, Fy), sastavnice };
  }
  function ciljniVektor(s) {
    const c = s.cilj;
    if (c.vektor === "F" && c.naboj != null) {
      const i = s.naboji.findIndex((n) => n.oznaka === c.naboj);
      const f2 = silaNa(s, i);
      return {
        x: f2.Fx,
        y: f2.Fy,
        iznos: f2.F,
        jedinica: "N",
        sastavnice: f2.sastavnice.map((k) => ({
          oznaka: k.oznaka,
          x: k.Fx,
          y: k.Fy,
          iznos: k.F
        })),
        tocka: s.naboji[i]
      };
    }
    const t = s.tocke.find((p2) => p2.oznaka === c.tocka);
    const p = polje(s, t.x, t.y);
    if (c.vektor === "F") {
      const q = c.q * NC;
      return {
        x: p.Ex * q,
        y: p.Ey * q,
        iznos: p.E * Math.abs(q),
        jedinica: "N",
        sastavnice: p.sastavnice.map((k) => ({
          oznaka: k.oznaka,
          x: k.Ex * q,
          y: k.Ey * q,
          iznos: k.E * Math.abs(q)
        })),
        tocka: t
      };
    }
    return {
      x: p.Ex,
      y: p.Ey,
      iznos: p.E,
      jedinica: "N/C",
      sastavnice: p.sastavnice.map((k) => ({
        oznaka: k.oznaka,
        x: k.Ex,
        y: k.Ey,
        iznos: k.E
      })),
      tocka: t
    };
  }
  function tockaNule(n1, n2) {
    const d = Math.hypot(n2.x - n1.x, n2.y - n1.y);
    const a = Math.sqrt(Math.abs(n1.q));
    const b = Math.sqrt(Math.abs(n2.q));
    if (!(d > 0) || n1.q === 0 || n2.q === 0) return null;
    let s;
    if (Math.sign(n1.q) === Math.sign(n2.q)) s = d * a / (a + b);
    else if (Math.abs(a - b) < 1e-9) return null;
    else if (a < b) s = a / b * d / (a / b - 1);
    else s = d / (1 - b / a);
    const ux = (n2.x - n1.x) / d;
    const uy = (n2.y - n1.y) / d;
    return { x: n1.x + s * ux, y: n1.y + s * uy, s, d };
  }
  function nakonDodira(kugle) {
    const Q = kugle.reduce((a, k) => a + k.q, 0);
    const R2 = kugle.map((k) => typeof k.R === "number" ? k.R : 1);
    const sumR = R2.reduce((a, b) => a + b, 0);
    return R2.map((r) => Q * r / sumR);
  }
  var CESTICE = {
    elektron: { naziv: "elektron", q: -E_NABOJ, m: M_E },
    proton: { naziv: "proton", q: E_NABOJ, m: M_P }
  };
  var jakostPolja = (s) => typeof s.E === "number" ? s.E : s.U / (s.d * MM);
  var napon = (s) => typeof s.U === "number" ? s.U : s.E * s.d * MM;
  function putanja(s, pr = {}) {
    const c = CESTICE[s.cestica.vrsta];
    const m = pr.m ?? c.m;
    const E2 = jakostPolja(s);
    const a = Math.abs(c.q) * E2 / m;
    const v0 = s.cestica.v0;
    const L = s.L * CM;
    const pola = s.d * MM / 2;
    const tIzlaz = L / v0;
    const yIzlaz = 0.5 * a * tIzlaz * tIzlaz;
    const smjer = c.q < 0 ? 1 : -1;
    if (yIzlaz <= pola)
      return { a, smjer, t: tIzlaz, izlaz: true, y: yIzlaz / MM, x: s.L, v0 };
    const t = Math.sqrt(2 * pola / a);
    return { a, smjer, t, izlaz: false, y: pola / MM, x: v0 * t / CM, v0 };
  }
  function kondenzator(s) {
    const er = typeof s.er === "number" ? s.er : 1;
    const d = (typeof s.d === "number" ? s.d : 1) * MM;
    const C = typeof s.C === "number" ? s.C * 1e-12 : EPS0 * er * s.A * CM * CM / d;
    const U = napon({ ...s, d: d / MM });
    return { C, U, Q: C * U, E: U / d, W: 0.5 * C * U * U, d, er };
  }
  function nakonAkcije(s) {
    const p = kondenzator(s);
    const a = s.akcija;
    let fC = 1;
    let fd = 1;
    if (a.vrsta === "dielektrik") fC = a.er / p.er;
    if (a.vrsta === "razmakni") {
      fC = 1 / a.faktor;
      fd = a.faktor;
    }
    if (a.vrsta === "povecaj_A") fC = a.faktor;
    const C = p.C * fC;
    const d = p.d * fd;
    const spojen = s.izvor === "spojen";
    const Q = spojen ? C * p.U : p.Q;
    const U = spojen ? p.U : Q / C;
    return {
      prije: p,
      poslije: {
        C,
        U,
        Q,
        E: U / d,
        W: 0.5 * C * U * U,
        d,
        er: a.vrsta === "dielektrik" ? a.er : p.er
      },
      fC,
      fd
    };
  }
  var VELICINE_KOND = ["Q", "C", "U", "E", "W"];
  function smjeroviPromjene(s) {
    const { prije, poslije } = nakonAkcije(s);
    return Object.fromEntries(
      VELICINE_KOND.map((v) => [v, promjena(prije[v], poslije[v])])
    );
  }
  var promjena = (a, b) => Math.abs(b - a) <= Math.abs(a) * 1e-9 ? "isto" : b > a ? "gore" : "dolje";

  // src/lib/lesson/katalog/fizika/naboji_polje.js
  var NACINI11 = ["istrazi", "smjer", "nula", "dodir"];
  var PRIKAZI5 = ["sile", "polje", "sastavnice", "silnice", "Er_graf"];
  var TOL_KUT = 12;
  var MAX_KOORD = 100;
  var SMJEROVI2 = [
    "udesno",
    "gore-desno",
    "gore",
    "gore-lijevo",
    "ulijevo",
    "dolje-lijevo",
    "dolje",
    "dolje-desno"
  ];
  var opisSmjera = (kut) => SMJEROVI2[Math.round((kut % 360 + 360) / 45) % 8];
  var fmtNaboj = (q) => `${q > 0 ? "+" : ""}${formatNumber(q)} nC`;
  var fmtIznos = (v, jed2) => `${formatSci(v)} ${jed2}`;
  var oznakaMd = (o) => /^[a-zA-Z]_\w+$/.test(o) ? `$${o.replace(/_(\w+)/, "_{$1}")}$` : o;
  var izvori = (s) => [
    ...(s.naboji ?? []).map((n) => n.oznaka),
    ...s.kugla ? [s.kugla.oznaka ?? "Q"] : []
  ];
  function validateScena(s, need) {
    const naboji = s.naboji ?? [];
    if (s.naboji != null) {
      if (need(
        Array.isArray(s.naboji) && s.naboji.length <= 4,
        "naboji: popis od najviše 4 naboja"
      )) {
        s.naboji.forEach((n, i) => {
          need(
            isText(n?.oznaka),
            `naboji[${i}].oznaka mora biti tekst (npr. "q_1")`
          );
          need(
            isNum(n?.q) && n.q !== 0 && Math.abs(n.q) <= 1e3,
            `naboji[${i}].q mora biti broj različit od 0, u nC (najviše 1000 po iznosu)`
          );
          need(
            isNum(n?.x) && isNum(n?.y) && Math.abs(n.x) <= MAX_KOORD && Math.abs(n.y) <= MAX_KOORD,
            `naboji[${i}] treba koordinate x, y u cm (do ${MAX_KOORD} po iznosu)`
          );
          if (n?.pomican != null)
            need(
              typeof n.pomican === "boolean",
              `naboji[${i}].pomican mora biti true ili false`
            );
        });
        const ozn = s.naboji.map((n) => n?.oznaka);
        need(
          new Set(ozn).size === ozn.length,
          "oznake naboja moraju biti različite"
        );
        for (let i = 0; i < naboji.length; i++)
          for (let j = i + 1; j < naboji.length; j++)
            if ([naboji[i], naboji[j]].every((n) => isNum(n?.x) && isNum(n?.y)))
              need(
                Math.hypot(
                  naboji[i].x - naboji[j].x,
                  naboji[i].y - naboji[j].y
                ) >= 1,
                `naboji ${naboji[i].oznaka} i ${naboji[j].oznaka} su preblizu (najmanje 1 cm)`
              );
      }
    }
    if (s.kugla != null) {
      const k = s.kugla;
      need(
        isNum(k?.x) && isNum(k?.y) && Math.abs(k.x) <= MAX_KOORD && Math.abs(k.y) <= MAX_KOORD,
        "kugla treba središte x, y u cm"
      );
      need(
        isNum(k?.R) && k.R > 0 && k.R <= 50,
        "kugla.R (vanjski polumjer, cm) mora biti između 0 i 50"
      );
      need(
        isNum(k?.q) && k.q !== 0 && Math.abs(k.q) <= 1e3,
        "kugla.q mora biti broj različit od 0, u nC"
      );
      if (k?.R_unutra != null)
        need(
          isNum(k.R_unutra) && k.R_unutra > 0 && k.R_unutra < k.R,
          "kugla.R_unutra (polumjer šupljine) mora biti između 0 i R"
        );
      if (k?.oznaka != null)
        need(isText(k.oznaka), "kugla.oznaka mora biti tekst");
      if (isNum(k?.R))
        naboji.forEach(
          (n, i) => need(
            !(isNum(n?.x) && isNum(n?.y)) || Math.hypot(n.x - k.x, n.y - k.y) > k.R + 0.5,
            `naboji[${i}] je unutar kugle ili na njoj`
          )
        );
    }
    need(
      naboji.length + (s.kugla ? 1 : 0) >= 1,
      "naboji_polje treba barem jedan naboj (naboji) ili kuglu"
    );
    if (s.sredstvo != null)
      need(
        isNum(s.sredstvo?.er) && s.sredstvo.er >= 1 && s.sredstvo.er <= 100 && isText(s.sredstvo?.naziv),
        "sredstvo treba {er (1–100), naziv}"
      );
    if (s.tocke != null) {
      if (need(
        Array.isArray(s.tocke) && s.tocke.length <= 5,
        "tocke: popis od najviše 5 točaka"
      )) {
        s.tocke.forEach((t, i) => {
          need(isText(t?.oznaka), `tocke[${i}].oznaka mora biti tekst`);
          need(
            isNum(t?.x) && isNum(t?.y) && Math.abs(t.x) <= MAX_KOORD && Math.abs(t.y) <= MAX_KOORD,
            `tocke[${i}] treba koordinate x, y u cm`
          );
          naboji.forEach(
            (n) => need(
              !(isNum(n?.x) && isNum(t?.x)) || Math.hypot(n.x - t.x, n.y - t.y) >= 0.5,
              `točka ${t?.oznaka} je na naboju ${n.oznaka}`
            )
          );
        });
        const ozn = s.tocke.map((t) => t?.oznaka);
        need(
          new Set(ozn).size === ozn.length,
          "oznake točaka moraju biti različite"
        );
      }
    }
    if (s.prikazi != null) {
      if (need(
        Array.isArray(s.prikazi) && s.prikazi.every((p) => PRIKAZI5.includes(p)),
        `prikazi smije sadržavati samo: ${PRIKAZI5.join(", ")}`
      )) {
        if (s.prikazi.includes("Er_graf"))
          need(!!s.kugla, "prikaz Er_graf (graf E(r)) postoji samo uz kuglu");
        if (s.prikazi.includes("sile"))
          need(
            naboji.length >= 2 || naboji.length >= 1 && s.kugla,
            "prikaz sile treba barem dva tijela s nabojem"
          );
      }
    }
  }
  function validateKugle(s, need) {
    if (!need(
      Array.isArray(s.kugle) && s.kugle.length === 2,
      "dodir treba kugle: točno 2 kugle [{oznaka, q (nC), R? (cm)}]"
    ))
      return false;
    s.kugle.forEach((k, i) => {
      need(isText(k?.oznaka), `kugle[${i}].oznaka mora biti tekst (npr. "A")`);
      need(
        isNum(k?.q) && Math.abs(k.q) <= 100,
        `kugle[${i}].q mora biti broj u nC (najviše 100 po iznosu)`
      );
      if (k?.R != null)
        need(
          isNum(k.R) && k.R >= 1 && k.R <= 20,
          `kugle[${i}].R mora biti između 1 i 20 cm`
        );
    });
    need(
      s.kugle.some((k) => isNum(k?.q) && k.q !== 0),
      "barem jedna kugla mora imati naboj"
    );
    return true;
  }
  var naboji_polje = {
    tip: "naboji_polje",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Točkasti naboji u ravnini (Coulomb, polje, superpozicija), vodljiva kugla i dodir dviju kugli. k = 9·10⁹ N m²/C² kao u knjižici.
Scena: naboji: [{oznaka (npr. "q_1"), q (nC, s predznakom), x, y (cm, y prema gore), pomican? (istrazi, zadano true)}] (0–4),
kugla?: {x, y, R (cm), q (nC), R_unutra? (šupljina, cm), oznaka?} (vodljiva: polje unutar površine 0, izvana kao točkasti naboj u središtu),
sredstvo?: {er, naziv} (zadano zrak), tocke?: [{oznaka, x, y}] (istaknute točke), prikazi?: ["sile", "polje", "sastavnice", "silnice", "Er_graf"], tekst, nacin:
- "istrazi": korak za gledanje. Učenik vuče naboje i sondu (sonda kreće iz prve točke) i gleda polje u sondi (E₁, E₂ … i zbroj),
  sile na nabojima (3. Newtonov zakon), silnice; klizaci?: oznake naboja/kugle i "er" (promjena q od −50 do 50 nC, izbor sredstva iz
  sredstva?: [{er, naziv}] ili zrak/ulje/staklo/voda); Er_graf crta E(r) kugle s položajem sonde. Ili kugle (kao u "dodir") za pokus dodira,
  uz uzemljenje?: true (gumb „dotakni rukom“: naboj kugle oteče u zemlju).
- "smjer": učenik povuče strelicu iz točke u smjeru vektora; cilj: {vektor: "E", tocka} (ukupno polje u točki), {vektor: "F", naboj} (ukupna
  sila na naboj) ili {vektor: "F", tocka, q (nC)} (sila na probni naboj u točki). Točno unutar ±tolerancija (zadano ${TOL_KUT}°); iznos se ne ocjenjuje.
  Poruke za suprotan smjer, smjer samo jedne sastavnice i smjer polja umjesto sile na negativni naboj dodaju se same; krivo?: [{kut (°), poruka}].
  Nakon odgovora vide se sastavnice i zbroj s iznosima.
- "nula": točno 2 naboja; učenik postavi točku P gdje je ukupno polje (i sila na probni naboj) nula. Točku računa aplikacija
  (između istoimenih, izvan raznoimenih uz manji); tolerancija? u cm (zadano 3 % razmaka). Poruke za krivo područje i za r umjesto r² su automatske.
- "dodir": kugle: [{oznaka, q (nC), R? (cm, zadano jednake)}] (2): učenik upiše naboj svake kugle nakon dodira, pa animacija pokaže prelazak
  elektrona. Točno: ukupni naboj se dijeli u omjeru polumjera (jednake: napola). Poruke za neočuvan zbroj, zbroj bez predznaka, zamjenu su automatske;
  krivo?: [{vrijednosti: [q₁, q₂], poruka}].`,
    primjer: {
      tip: "naboji_polje",
      nacin: "smjer",
      tekst: "Kamo pokazuje ukupno električno polje u točki T? Povuci strelicu iz točke T.",
      sredstvo: { er: 80, naziv: "voda" },
      naboji: [
        { oznaka: "q_1", q: 10, x: 0, y: 0 },
        { oznaka: "q_2", q: -15, x: 15, y: 0 }
      ],
      tocke: [{ oznaka: "T", x: 5, y: 0 }],
      cilj: { vektor: "E", tocka: "T" },
      prikazi: ["sastavnice"]
    },
    primjeri: [
      {
        tip: "naboji_polje",
        nacin: "istrazi",
        tekst: "Povuci sondu T i naboje. Mijenjaj predznak naboja $q_2$ i gledaj kako se polja u T zbrajaju ili oduzimaju.",
        sredstvo: { er: 80, naziv: "voda" },
        naboji: [
          { oznaka: "q_1", q: 10, x: 0, y: 0 },
          { oznaka: "q_2", q: -15, x: 15, y: 0 }
        ],
        tocke: [{ oznaka: "T", x: 5, y: 0 }],
        prikazi: ["polje", "sastavnice", "silnice"],
        klizaci: ["q_2"]
      },
      {
        tip: "naboji_polje",
        nacin: "istrazi",
        tekst: "Mijenjaj naboje i sredstvo, pomiči naboje i gledaj sile kojima djeluju jedan na drugoga.",
        sredstvo: { er: 80, naziv: "voda" },
        naboji: [
          { oznaka: "q_1", q: 10, x: 0, y: 0 },
          { oznaka: "q_2", q: -15, x: 15, y: 0 }
        ],
        prikazi: ["sile"],
        klizaci: ["q_1", "q_2", "er"]
      },
      {
        tip: "naboji_polje",
        nacin: "smjer",
        tekst: "Kamo djeluje sila na naboj $q_2$? Povuci strelicu iz naboja $q_2$.",
        naboji: [
          { oznaka: "q_1", q: 8, x: 0, y: 0 },
          { oznaka: "q_2", q: 25, x: 10, y: 0 }
        ],
        sredstvo: { er: 5, naziv: "staklo" },
        cilj: { vektor: "F", naboj: "q_2" }
      },
      {
        tip: "naboji_polje",
        nacin: "smjer",
        tekst: "U točku T stavimo naboj $-2$ nC. Kamo djeluje sila na njega? Povuci strelicu iz točke T.",
        naboji: [
          { oznaka: "q_1", q: 6, x: 0, y: 0 },
          { oznaka: "q_2", q: 6, x: 12, y: 0 }
        ],
        tocke: [{ oznaka: "T", x: 6, y: 6 }],
        cilj: { vektor: "F", tocka: "T", q: -2 }
      },
      {
        tip: "naboji_polje",
        nacin: "nula",
        tekst: "Naboji $q_1 = 4$ nC i $q_2 = 9$ nC razmaknuti su 15 cm. Postavi točku P u kojoj je ukupno električno polje nula.",
        naboji: [
          { oznaka: "q_1", q: 4, x: 0, y: 0 },
          { oznaka: "q_2", q: 9, x: 15, y: 0 }
        ]
      },
      {
        tip: "naboji_polje",
        nacin: "nula",
        tekst: "Naboji $q_1 = 2$ nC i $q_2 = -8$ nC razmaknuti su 10 cm. Gdje je ukupno polje nula? Postavi točku P.",
        naboji: [
          { oznaka: "q_1", q: 2, x: 0, y: 0 },
          { oznaka: "q_2", q: -8, x: 10, y: 0 }
        ]
      },
      {
        tip: "naboji_polje",
        nacin: "istrazi",
        tekst: "Povuci sondu kroz šupljinu (točka A), kroz metal (B), uz površinu (točka C) i dalje od kugle (D) i gledaj jakost polja.",
        kugla: { x: 0, y: 0, R: 20, R_unutra: 15, q: 40 },
        tocke: [
          { oznaka: "A", x: 0, y: 7 },
          { oznaka: "B", x: -17.5, y: 0 },
          { oznaka: "C", x: 20, y: 0 },
          { oznaka: "D", x: 40, y: 0 }
        ],
        prikazi: ["polje", "silnice", "Er_graf"]
      },
      {
        tip: "naboji_polje",
        nacin: "dodir",
        tekst: "Dvije jednake metalne kugle imaju naboje $+6$ nC i $-2$ nC. Kugle se kratko dodirnu i razdvoje. Koliki je naboj svake kugle nakon toga?",
        kugle: [
          { oznaka: "A", q: 6 },
          { oznaka: "B", q: -2 }
        ]
      },
      {
        tip: "naboji_polje",
        nacin: "istrazi",
        tekst: "Namjesti naboje kugli i dodirni ih. Pokušaj i „dotakni rukom“: kamo nestane naboj?",
        kugle: [
          { oznaka: "A", q: -9 },
          { oznaka: "B", q: 3 }
        ],
        klizaci: ["A", "B"],
        uzemljenje: true
      }
    ],
    validate(s) {
      const { list, need } = problems();
      if (!need(
        NACINI11.includes(s.nacin),
        `naboji_polje.nacin mora biti jedan od: ${NACINI11.join(", ")}`
      ))
        return list;
      need(isText(s.tekst), "naboji_polje treba „tekst“");
      const dodirScena = s.nacin === "dodir" || s.nacin === "istrazi" && s.kugle != null;
      if (dodirScena) {
        need(
          s.naboji == null && s.kugla == null && s.tocke == null,
          "pokus dodira (kugle) ne smije imati i naboje, kuglu ili točke"
        );
        validateKugle(s, need);
        if (s.uzemljenje != null)
          need(
            s.nacin === "istrazi" && typeof s.uzemljenje === "boolean",
            "uzemljenje (true/false) postoji samo u načinu istrazi"
          );
      } else {
        need(
          s.kugle == null,
          "kugle postoje samo u pokusu dodira (nacin dodir ili istrazi bez naboja)"
        );
        validateScena(s, need);
      }
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        const imena = dodirScena ? (s.kugle ?? []).map((k) => k?.oznaka) : izvori(s);
        if (s.klizaci != null && need(
          Array.isArray(s.klizaci) && s.klizaci.length <= 4,
          "klizaci: popis od najviše 4 oznake"
        )) {
          s.klizaci.forEach(
            (k) => need(
              imena.includes(k) || !dodirScena && k === "er",
              `klizac „${k}“ nije oznaka naboja${dodirScena ? " (kugle)" : ', kugle ni "er"'}`
            )
          );
        }
        if (s.sredstva != null) {
          need(
            !dodirScena && Array.isArray(s.klizaci) && s.klizaci.includes("er"),
            'sredstva (izbor sredstva) imaju smisla samo uz klizač "er"'
          );
          need(
            Array.isArray(s.sredstva) && s.sredstva.length >= 2 && s.sredstva.length <= 5 && s.sredstva.every(
              (m) => isNum(m?.er) && m.er >= 1 && m.er <= 100 && isText(m?.naziv)
            ),
            "sredstva: 2–5 sredstava {er (1–100), naziv}"
          );
        }
        return list;
      }
      need(
        s.klizaci == null && s.sredstva == null,
        "klizaci i sredstva postoje samo u načinu istrazi"
      );
      if (s.nacin === "smjer") validateSmjer(s, need);
      if (s.nacin === "nula") validateNula(s, need);
      if (s.nacin === "dodir" && Array.isArray(s.kugle) && s.krivo != null) {
        const tocno = nakonDodira(s.kugle);
        if (need(Array.isArray(s.krivo), "krivo mora biti popis")) {
          s.krivo.forEach((k, i) => {
            need(
              Array.isArray(k?.vrijednosti) && k.vrijednosti.length === 2 && k.vrijednosti.every(isNum),
              `krivo[${i}].vrijednosti mora biti [q₁, q₂] u nC`
            );
            need(isText(k?.poruka), `krivo[${i}].poruka mora biti tekst`);
            if (Array.isArray(k?.vrijednosti))
              need(
                !k.vrijednosti.every((v, j) => Math.abs(v - tocno[j]) < 1e-9),
                `krivo[${i}] je točan odgovor`
              );
          });
        }
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "smjer") return checkSmjer2(s, odgovor3);
      if (s.nacin === "nula") return checkNula(s, odgovor3);
      if (s.nacin === "dodir") return checkDodir(s, odgovor3);
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "smjer") {
        const v = ciljniVektor(s);
        const kut = kutVektora2(v.x, v.y);
        return `${opisSmjera(kut)} (${formatNumber(Math.round(kut))}°), iznos ${v.jedinica === "N" ? "F" : "E"} ≈ ${fmtIznos(v.iznos, v.jedinica)}`;
      }
      if (s.nacin === "nula") {
        const z = nulaInfo(s);
        if (!z) return "";
        const [a, b] = s.naboji;
        const r12 = Math.abs(z.s);
        const r2 = Math.abs(z.d - z.s);
        return `na pravcu kroz naboje, ${formatNumber(r12, 2)} cm od ${oznakaMd(a.oznaka)} i ${formatNumber(r2, 2)} cm od ${oznakaMd(b.oznaka)}${z.s > 0 && z.s < z.d ? " (između njih)" : ` (s vanjske strane naboja ${oznakaMd(r12 < r2 ? a.oznaka : b.oznaka)})`}`;
      }
      if (s.nacin === "dodir") {
        const t = nakonDodira(s.kugle);
        return s.kugle.map((k, i) => `${k.oznaka}: ${fmtNaboj(t[i])}`).join(", ");
      }
      return "";
    },
    brojcaniOdgovor: (s) => s.nacin === "dodir" ? nakonDodira(s.kugle)[0] : null,
    // za komponentu: točka nultog polja (nula) i tolerancija u cm
    nula: (s) => nulaInfo(s)
  };
  function validateSmjer(s, need) {
    const c = s.cilj;
    if (!need(
      c && ["E", "F"].includes(c.vektor),
      'smjer treba cilj: {vektor: "E" | "F", tocka | naboj}'
    ))
      return;
    if (c.naboj != null) {
      need(
        c.vektor === "F",
        'cilj.naboj ide samo uz vektor "F" (sila na taj naboj); polje u točki traži cilj.tocka'
      );
      need(c.tocka == null, "cilj ima ili tocka ili naboj, ne oboje");
      if (!need(
        (s.naboji ?? []).some((n) => n.oznaka === c.naboj),
        `cilj.naboj „${c.naboj}“ nije oznaka nijednog naboja`
      ))
        return;
      need(
        (s.naboji ?? []).length + (s.kugla ? 1 : 0) >= 2,
        "sila na naboj treba barem još jedan naboj ili kuglu"
      );
    } else {
      if (!need(
        (s.tocke ?? []).some((t) => t.oznaka === c.tocka),
        "cilj.tocka mora biti oznaka jedne od točaka (tocke)"
      ))
        return;
      if (c.vektor === "F")
        need(
          isNum(c.q) && c.q !== 0,
          "sila u točki treba cilj.q (probni naboj u nC, različit od 0)"
        );
      else need(c.q == null, 'cilj.q ide samo uz vektor "F"');
    }
    if (s.tolerancija != null)
      need(
        isNum(s.tolerancija) && s.tolerancija >= 3 && s.tolerancija <= 30,
        "tolerancija (stupnjevi) mora biti između 3 i 30"
      );
    let v;
    try {
      v = ciljniVektor(s);
    } catch {
      return;
    }
    if (!need(
      isNum(v.iznos) && Number.isFinite(v.iznos),
      "vektor se ne može izračunati (točka je na naboju?)"
    ))
      return;
    const najveca = Math.max(...v.sastavnice.map((k) => k.iznos), 0);
    need(
      v.iznos > 0.02 * najveca && v.iznos > 0,
      'ukupni vektor je (gotovo) nula, pa nema smjera; za takvo pitanje koristi nacin "nula"'
    );
    if (s.krivo != null && need(Array.isArray(s.krivo), "krivo mora biti popis")) {
      const tocan = kutVektora2(v.x, v.y);
      s.krivo.forEach((k, i) => {
        need(
          isNum(k?.kut),
          `krivo[${i}].kut mora biti broj (stupnjevi, 0° = udesno, 90° = gore)`
        );
        need(isText(k?.poruka), `krivo[${i}].poruka mora biti tekst`);
        if (isNum(k?.kut))
          need(
            razlikaKuta2(k.kut, tocan) > tolKut(s),
            `krivo[${i}].kut je unutar tolerancije točnog smjera`
          );
      });
    }
  }
  var tolKut = (s) => isNum(s.tolerancija) ? s.tolerancija : TOL_KUT;
  function checkSmjer2(s, odgovor3) {
    if (!isNum(odgovor3))
      return {
        tocno: false,
        poruka: "Povuci strelicu iz točke u smjeru koji misliš da je točan.",
        prazno: true
      };
    const v = ciljniVektor(s);
    const tocan = kutVektora2(v.x, v.y);
    const tol2 = tolKut(s);
    const diff = razlikaKuta2(odgovor3, tocan);
    if (diff <= tol2) return { tocno: true };
    const hit = (s.krivo ?? []).find((k) => razlikaKuta2(odgovor3, k.kut) <= tol2);
    if (hit) return { tocno: false, poruka: hit.poruka };
    const c = s.cilj;
    const sila = c.vektor === "F";
    const negProba = sila && c.tocka != null && c.q < 0;
    if (negProba && diff >= 180 - tol2)
      return {
        tocno: false,
        poruka: "To je smjer polja u točki. Naboj koji tamo stavljamo je negativan, a na negativan naboj sila djeluje suprotno od smjera polja."
      };
    const vise = v.sastavnice.length >= 2;
    if (vise) {
      for (const k of v.sastavnice) {
        if (k.iznos === 0) continue;
        const kk = kutVektora2(k.x, k.y);
        if (razlikaKuta2(odgovor3, kk) <= tol2)
          return {
            tocno: false,
            poruka: sila ? `To je smjer sile samo od ${oznakaMd(k.oznaka)}. Nacrtaj i sile ostalih naboja i zbroji ih kao vektore.` : `To je smjer polja samo od ${oznakaMd(k.oznaka)}. Nacrtaj i polje od ostalih naboja, pa ih zbroji kao vektore.`
          };
      }
    }
    if (diff >= 180 - tol2) {
      if (sila && c.naboj != null)
        return {
          tocno: false,
          poruka: "Strelica pokazuje točno suprotno. Istoimeni naboji se odbijaju, a raznoimeni privlače: provjeri predznake."
        };
      return {
        tocno: false,
        poruka: "Strelica pokazuje točno suprotno. Polje pozitivnog naboja pokazuje od njega, a polje negativnog naboja prema njemu."
      };
    }
    if (diff <= 35)
      return {
        tocno: false,
        poruka: `Blizu si, ali smjer odstupa za oko ${Math.round(diff)}°. ${vise ? "Pazi koji je doprinos jači: bliži naboj i veći naboj daju jače polje." : "Strelica mora ležati na pravcu kroz naboj i točku."}`
      };
    return {
      tocno: false,
      poruka: s.savjet || (sila && c.naboj != null ? "Nije taj smjer. Za svaki drugi naboj odredi silu (odbijanje ili privlačenje, uzduž pravca kroz oba naboja), pa ih zbroji." : "Nije taj smjer. Za svaki naboj nacrtaj njegovo polje u točki (od pozitivnog, prema negativnom), pa ih zbroji kao vektore.")
    };
  }
  function nulaInfo(s) {
    if (!Array.isArray(s.naboji) || s.naboji.length !== 2) return null;
    const z = tockaNule(s.naboji[0], s.naboji[1]);
    if (!z) return null;
    return { ...z, tol: isNum(s.tolerancija) ? s.tolerancija : 0.03 * z.d };
  }
  function validateNula(s, need) {
    need(
      s.kugla == null,
      "nacin nula radi samo s točkastim nabojima (bez kugle)"
    );
    if (!need(
      Array.isArray(s.naboji) && s.naboji.length === 2,
      "nacin nula treba točno 2 naboja"
    ))
      return;
    if (!s.naboji.every((n) => isNum(n?.q) && isNum(n?.x) && isNum(n?.y))) return;
    const z = tockaNule(s.naboji[0], s.naboji[1]);
    if (!need(
      z,
      "jednaki naboji suprotnih predznaka nemaju točku nultog polja (osim beskonačno daleko); promijeni iznos jednog naboja"
    ))
      return;
    need(
      Math.abs(z.s) <= 3 * z.d && Math.abs(z.d - z.s) <= 3 * z.d,
      `točka nultog polja je predaleko (${formatNumber(Math.abs(z.s), 1)} cm od prvog naboja); izaberi naboje većeg omjera`
    );
    if (s.tolerancija != null)
      need(
        isNum(s.tolerancija) && s.tolerancija > 0 && s.tolerancija <= 0.2 * z.d,
        "tolerancija (cm) mora biti pozitivna i najviše 20 % razmaka naboja"
      );
    need(s.cilj == null, "nacin nula ne treba cilj (točku računa aplikacija)");
  }
  function checkNula(s, odgovor3) {
    if (!isNum(odgovor3?.x) || !isNum(odgovor3?.y))
      return {
        tocno: false,
        poruka: "Povuci točku P na mjesto gdje je polje nula.",
        prazno: true
      };
    const z = nulaInfo(s);
    const [a, b] = s.naboji;
    if (Math.hypot(odgovor3.x - z.x, odgovor3.y - z.y) <= z.tol)
      return { tocno: true };
    const ux = (b.x - a.x) / z.d;
    const uy = (b.y - a.y) / z.d;
    const p = (odgovor3.x - a.x) * ux + (odgovor3.y - a.y) * uy;
    const okomito = Math.abs(-(odgovor3.x - a.x) * uy + (odgovor3.y - a.y) * ux);
    if (okomito > Math.max(z.tol, 0.08 * z.d))
      return {
        tocno: false,
        poruka: "Polje može biti nula samo na pravcu kroz oba naboja: izvan njega polja dvaju naboja nisu suprotnog smjera, pa se ne mogu poništiti."
      };
    const isti2 = Math.sign(a.q) === Math.sign(b.q);
    const izmedu = p > 0 && p < z.d;
    const A = oznakaMd(a.oznaka);
    const B = oznakaMd(b.oznaka);
    if (isti2 && !izmedu)
      return {
        tocno: false,
        poruka: "Izvan para istoimenih naboja oba polja pokazuju na istu stranu, pa se zbrajaju. Polje se poništava između njih."
      };
    if (!isti2 && izmedu)
      return {
        tocno: false,
        poruka: "Između raznoimenih naboja oba polja pokazuju prema negativnom naboju, pa se zbrajaju. Polje se poništava izvan njih."
      };
    const manji = Math.abs(a.q) < Math.abs(b.q) ? a : b;
    if (!isti2 && p < 0 !== z.s < 0)
      return {
        tocno: false,
        poruka: `S ove strane je bliži veći naboj, pa njegovo polje uvijek prevlada. Točka je s druge strane, uz manji naboj (${oznakaMd(manji.oznaka)}).`
      };
    const ra = Math.abs(p);
    const rb = Math.abs(z.d - p);
    const linearno = isti2 ? z.d * Math.abs(a.q) / (Math.abs(a.q) + Math.abs(b.q)) : z.s < 0 ? -(Math.abs(a.q) * z.d) / (Math.abs(b.q) - Math.abs(a.q)) : Math.abs(a.q) * z.d / (Math.abs(a.q) - Math.abs(b.q));
    if (Math.abs(p - linearno) <= z.tol)
      return {
        tocno: false,
        poruka: "Izgleda da si udaljenosti postavio u omjeru naboja. Polje pada s kvadratom udaljenosti, pa vrijedi $\\frac{|q_1|}{r_1^2} = \\frac{|q_2|}{r_2^2}$."
      };
    const Ea = Math.abs(a.q) / (ra * ra);
    const Eb = Math.abs(b.q) / (rb * rb);
    const jaci = Ea > Eb ? A : B;
    return {
      tocno: false,
      poruka: s.savjet || `Ovdje je polje od ${jaci} jače, pa se ne poništavaju. Pomakni točku dalje od ${jaci}. Uvjet je $\\frac{|q_1|}{r_1^2} = \\frac{|q_2|}{r_2^2}$.`
    };
  }
  function checkDodir(s, odgovor3) {
    const unos = Array.isArray(odgovor3) ? odgovor3.map(
      (v) => parseNumber(
        typeof v === "number" ? v : String(v ?? "").replace(/^\+/, ""),
        "nC"
      )
    ) : [];
    if (unos.length !== 2 || unos.some((v) => v == null))
      return {
        tocno: false,
        poruka: "Upiši naboj obje kugle u nC (npr. 2 ili −3,5).",
        prazno: true
      };
    const [q1, q2] = s.kugle.map((k) => k.q);
    const t = nakonDodira(s.kugle);
    const Q = q1 + q2;
    const tol2 = Math.max(0.01, 5e-3 * Math.max(Math.abs(q1), Math.abs(q2)));
    const blizu2 = (x, y) => Math.abs(x - y) <= tol2;
    const par = (a, b) => blizu2(unos[0], a) && blizu2(unos[1], b);
    if (par(t[0], t[1])) return { tocno: true };
    const hit = (s.krivo ?? []).find(
      (k) => par(k.vrijednosti[0], k.vrijednosti[1])
    );
    if (hit) return { tocno: false, poruka: hit.poruka };
    const jednake = Math.abs(t[0] - t[1]) < 1e-9 || Q === 0;
    if (par(q1, q2))
      return {
        tocno: false,
        poruka: "Kugle su metalne, dakle vodiči: dok se dodiruju, elektroni prelaze s jedne na drugu, pa se naboji promijene."
      };
    if (par(q2, q1) && q1 !== q2)
      return {
        tocno: false,
        poruka: "Kugle nisu samo zamijenile naboje. Dok se dodiruju, ponašaju se kao jedno tijelo i ukupni naboj se razmjesti po obje."
      };
    const zbroj2 = unos[0] + unos[1];
    if (!blizu2(zbroj2, Q)) {
      const abs = (Math.abs(q1) + Math.abs(q2)) / 2;
      if (jednake && Math.abs(q1) !== Math.abs(q2) && par(abs, abs) && Math.sign(q1) !== Math.sign(q2))
        return {
          tocno: false,
          poruka: `Zbrojio si iznose bez predznaka. Naboje zbrajaj s predznacima: (${fmtNaboj(q1)}) + (${fmtNaboj(q2)}) = ${fmtNaboj(Q)}.`
        };
      if (par(Q, Q))
        return {
          tocno: false,
          poruka: `Ukupni naboj ${fmtNaboj(Q)} dijele obje kugle zajedno: svaka dobije dio, a ne cijeli iznos.`
        };
      if (par(-t[0], -t[1]))
        return {
          tocno: false,
          poruka: "Iznos je dobar, ali predznak nije. Zbroji naboje s predznacima: prevladava naboj većeg iznosa."
        };
      return {
        tocno: false,
        poruka: `Naboj se ne stvara ni ne gubi: zbroj nakon dodira (${fmtNaboj(zbroj2)}) mora biti jednak zbroju prije (${fmtNaboj(Q)}).`
      };
    }
    if (jednake)
      return {
        tocno: false,
        poruka: "Zbroj je dobar, ali kugle su jednake, pa nema razloga da jedna dobije više: ukupni naboj dijele napola."
      };
    return {
      tocno: false,
      poruka: "Zbroj je dobar, ali veća kugla dobije veći dio: ukupni naboj dijeli se u omjeru polumjera."
    };
  }

  // src/lib/lesson/katalog/fizika/ploce_polje.js
  var NACINI12 = ["istrazi", "pogodi", "predvidi"];
  var KLIZACI3 = ["U", "d", "A", "er", "v0"];
  var PRIKAZI_PLOCE = [
    "E",
    "F",
    "a",
    "v",
    "W",
    "t",
    "y",
    "C",
    "Q",
    "U",
    "Wel"
  ];
  var AKCIJE = ["dielektrik", "razmakni", "povecaj_A"];
  var SMJER_OPIS = { gore: "raste", dolje: "pada", isto: "ostaje isti" };
  var tri = (v) => formatNumber(Number(v.toPrecision(3)));
  var fmtMm = (v) => `${tri(v)} mm`;
  var fmtCm = (v) => `${tri(v)} cm`;
  var imaCesticu = (s) => s.cestica != null;
  function validateCestica(s, need) {
    const c = s.cestica;
    if (!need(c && typeof c === "object", "cestica treba {vrsta, ulaz, v0?}"))
      return;
    need(
      ["proton", "elektron"].includes(c.vrsta),
      "cestica.vrsta mora biti proton ili elektron"
    );
    need(
      ["mirovanje", "vodoravno"].includes(c.ulaz),
      "cestica.ulaz mora biti mirovanje (kreće uz ploču) ili vodoravno (ulijeće okomito na silnice)"
    );
    if (c.ulaz === "vodoravno") {
      need(
        isNum(c.v0) && c.v0 >= 1e3 && c.v0 <= 1e8,
        "cestica.v0 (m/s) mora biti između 1000 i 10⁸ (manje od brzine svjetlosti)"
      );
      need(
        isNum(s.L) && s.L >= 1 && s.L <= 50,
        "vodoravni ulet treba L (duljina ploča, cm, 1–50)"
      );
    } else need(c.v0 == null, "čestica iz mirovanja nema v0");
  }
  var ploce_polje = {
    tip: "ploce_polje",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Homogeno polje između dviju vodoravnih ploča (gornja +, donja −). Konstante iz knjižice (e = 1,6·10⁻¹⁹ C, mₑ = 9,11·10⁻³¹ kg,
mₚ = 1,67·10⁻²⁷ kg, ε₀ = 8,85·10⁻¹² F/m). Polja: tekst, nacin, U? (V) ili E? (V/m), d (razmak, mm), L? (duljina ploča, cm),
A? (površina ploča, cm²) ili C? (pF), er? (zadano 1), izvor? ("spojen" | "odspojen"),
cestica?: {vrsta: "proton" | "elektron", ulaz: "mirovanje" (kreće uz ploču istog predznaka) | "vodoravno" (ulijeće sredinom, okomito na silnice), v0? (m/s)}.
- "istrazi": korak za gledanje; klizaci?: ["U", "d", "A", "er", "v0"]; prikazi?: ["E", "F", "a", "v", "W", "t", "y", "C", "Q", "U", "Wel"].
  S česticom: gumb „Pusti“ animira gibanje (ubrzanje ili parabola) i pokazuje očitanja. Bez čestice: kondenzator (C, Q, U, E, W uživo,
  gustoća naboja na pločama i silnica); uz izvor učenik spaja/odspaja izvor i vidi što ostaje stalno (Q ili U).
- "pogodi": cestica s ulaz "vodoravno", L i d obavezni. Učenik povuče oznaku na mjesto gdje čestica izlazi iz ploča (desni rub, otklon y u mm)
  ili udari u ploču (x u cm od ulaza), pa provjeri; zatim čestica proleti. Točno: |y − y*| ≤ max(tolerancija, 8 % y*), y* = ½·(qE/m)·(L/v₀)²
  (isto za x). Poruke za krivu ploču, zaboravljenu ½, krivu masu i izlaz/udar su automatske; krivo?: [{vrijednost (mm), poruka}].
- "predvidi": kondenzator (U i C ili A, d) s izvor "spojen" | "odspojen" i akcija: {vrsta: "dielektrik" (er) | "razmakni" (faktor, d × faktor) |
  "povecaj_A" (faktor), naziv? (npr. "guma")}; pitaj: popis iz ["Q", "C", "U", "E", "W"]. Učenik za svaku bira raste / pada / isto;
  točno računa aplikacija (odspojen: Q stalan; spojen: U stalan), a nakon odgovora animacija izvede promjenu i pokaže faktore.`,
    primjer: {
      tip: "ploce_polje",
      nacin: "pogodi",
      tekst: "Elektron ulijeće brzinom $2 \\cdot 10^7$ m/s okomito na silnice polja jakosti 2000 V/m. Ploče su duge 8 cm. Izračunaj otklon i povuci oznaku na mjesto gdje će elektron izaći.",
      E: 2e3,
      d: 10,
      L: 8,
      cestica: { vrsta: "elektron", v0: 2e7, ulaz: "vodoravno" }
    },
    primjeri: [
      {
        tip: "ploce_polje",
        nacin: "istrazi",
        tekst: "Proton kreće iz mirovanja uz pozitivnu ploču. Mijenjaj napon i razmak, pusti proton i gledaj polje, akceleraciju i brzinu udara.",
        U: 200,
        d: 5,
        cestica: { vrsta: "proton", ulaz: "mirovanje" },
        klizaci: ["U", "d"],
        prikazi: ["E", "a", "W", "v"]
      },
      {
        tip: "ploce_polje",
        nacin: "istrazi",
        tekst: "Elektron ulijeće među ploče. Mijenjaj brzinu i napon i pusti ga: koliko skrene?",
        U: 20,
        d: 10,
        L: 8,
        cestica: { vrsta: "elektron", v0: 2e7, ulaz: "vodoravno" },
        klizaci: ["v0", "U"],
        prikazi: ["E", "a", "t", "y"]
      },
      {
        tip: "ploce_polje",
        nacin: "istrazi",
        tekst: "Mijenjaj površinu ploča, razmak i dielektrik i gledaj kapacitet. Odspoji izvor pa ponovo mijenjaj: što ostaje isto?",
        U: 500,
        A: 200,
        d: 1,
        izvor: "spojen",
        klizaci: ["A", "d", "er"],
        prikazi: ["C", "Q", "U", "E", "Wel"]
      },
      {
        tip: "ploce_polje",
        nacin: "predvidi",
        tekst: "Kondenzator je odspojen od izvora. Između ploča umetnemo gumu ($\\varepsilon_r = 2{,}5$). Što se dogodi s nabojem, kapacitetom, naponom i energijom?",
        C: 40,
        U: 500,
        izvor: "odspojen",
        akcija: { vrsta: "dielektrik", er: 2.5, naziv: "guma" },
        pitaj: ["Q", "C", "U", "W"]
      },
      {
        tip: "ploce_polje",
        nacin: "predvidi",
        tekst: "Kondenzator ostaje spojen na izvor, a razmak ploča udvostručimo. Što se dogodi?",
        C: 40,
        U: 500,
        d: 1,
        izvor: "spojen",
        akcija: { vrsta: "razmakni", faktor: 2 },
        pitaj: ["C", "Q", "U", "E"]
      },
      {
        tip: "ploce_polje",
        nacin: "pogodi",
        tekst: "Proton ulijeće brzinom $2 \\cdot 10^5$ m/s među ploče razmaka 10 mm i napona 100 V. Gdje udari? Povuci oznaku.",
        U: 100,
        d: 10,
        L: 10,
        cestica: { vrsta: "proton", v0: 2e5, ulaz: "vodoravno" }
      }
    ],
    validate(s) {
      const { list, need } = problems();
      if (!need(
        NACINI12.includes(s.nacin),
        `ploce_polje.nacin mora biti jedan od: ${NACINI12.join(", ")}`
      ))
        return list;
      need(isText(s.tekst), "ploce_polje treba „tekst“");
      need(
        !(s.U != null && s.E != null),
        "zadaj ili U (napon) ili E (jakost polja), ne oboje"
      );
      if (s.U != null)
        need(
          isNum(s.U) && s.U > 0 && s.U <= 1e6,
          "U (V) mora biti pozitivan broj do 10⁶"
        );
      if (s.E != null)
        need(
          isNum(s.E) && s.E > 0 && s.E <= 1e8,
          "E (V/m) mora biti pozitivan broj do 10⁸"
        );
      need(
        s.U != null || s.E != null,
        "ploce_polje treba napon U (V) ili jakost polja E (V/m)"
      );
      if (s.d != null)
        need(
          isNum(s.d) && s.d >= 0.1 && s.d <= 100,
          "d (razmak ploča) mora biti između 0,1 i 100 mm"
        );
      if (s.L != null)
        need(
          isNum(s.L) && s.L >= 1 && s.L <= 50,
          "L (duljina ploča) mora biti između 1 i 50 cm"
        );
      need(
        !(s.A != null && s.C != null),
        "zadaj ili A (površina ploča) ili C (kapacitet), ne oboje"
      );
      if (s.A != null)
        need(
          isNum(s.A) && s.A >= 1 && s.A <= 1e4,
          "A (površina ploča) mora biti između 1 i 10 000 cm²"
        );
      if (s.C != null)
        need(
          isNum(s.C) && s.C >= 0.1 && s.C <= 1e6,
          "C (kapacitet) mora biti između 0,1 i 10⁶ pF"
        );
      if (s.er != null)
        need(
          isNum(s.er) && s.er >= 1 && s.er <= 100,
          "er (relativna permitivnost) mora biti između 1 i 100"
        );
      if (s.izvor != null)
        need(
          ["spojen", "odspojen"].includes(s.izvor),
          "izvor mora biti spojen ili odspojen"
        );
      if (s.cestica != null) validateCestica(s, need);
      if (s.prikazi != null)
        need(
          Array.isArray(s.prikazi) && s.prikazi.length >= 1 && s.prikazi.every((p) => PRIKAZI_PLOCE.includes(p)),
          `prikazi smije sadržavati samo: ${PRIKAZI_PLOCE.join(", ")}`
        );
      if (imaCesticu(s)) {
        need(isNum(s.d), "čestica među pločama treba d (razmak, mm)");
        need(
          s.A == null && s.C == null && s.izvor == null,
          "čestica među pločama ne koristi A, C ni izvor (to je za kondenzator)"
        );
      }
      if (s.nacin === "istrazi") {
        noAnswerFields(s, need);
        need(
          s.akcija == null && s.pitaj == null && s.pogodi == null,
          "istrazi nema akcija, pitaj ni pogodi"
        );
        if (!imaCesticu(s)) {
          need(s.U != null, "kondenzator treba napon U (V)");
          need(
            s.C != null || s.A != null && s.d != null,
            "kondenzator treba C (pF) ili A (cm²) i d (mm)"
          );
        }
        if (s.klizaci != null && need(
          Array.isArray(s.klizaci) && s.klizaci.every((k) => KLIZACI3.includes(k)) && new Set(s.klizaci).size === s.klizaci.length,
          `klizaci smije sadržavati samo: ${KLIZACI3.join(", ")} (bez ponavljanja)`
        )) {
          if (s.klizaci.includes("v0"))
            need(
              s.cestica?.ulaz === "vodoravno",
              "klizač v0 postoji samo uz vodoravni ulet čestice"
            );
          if (s.klizaci.includes("A"))
            need(s.A != null, "klizač A treba zadanu površinu A");
          if (s.klizaci.includes("er"))
            need(
              !imaCesticu(s),
              "klizač er (dielektrik) je za kondenzator bez čestice"
            );
          if (s.klizaci.includes("d"))
            need(isNum(s.d), "klizač d treba zadan razmak d");
        }
        return list;
      }
      need(s.klizaci == null, "klizaci postoje samo u načinu istrazi");
      if (s.nacin === "pogodi") {
        if (!need(
          s.cestica?.ulaz === "vodoravno",
          'pogodi treba cesticu s ulaz "vodoravno" (i v0, L, d)'
        ))
          return list;
        if (s.pogodi != null)
          need(
            s.pogodi?.velicina == null || s.pogodi.velicina === "y",
            'pogodi.velicina može biti samo "y"'
          );
        if (!list.length) {
          const p = putanja(s);
          if (p.izlaz) {
            need(
              p.y >= 0.08 * (s.d / 2),
              `otklon na izlazu (${fmtMm(p.y)}) je premalen prema razmaku ploča ${fmtMm(s.d)}: smanji d da se oznaka može postaviti`
            );
            validateKrivo(s.krivo, p.y, tolPogodi(s, p), need);
          } else {
            need(
              p.x >= 0.08 * s.L,
              "čestica udari u ploču odmah na ulazu; povećaj d ili v₀"
            );
            need(
              s.krivo == null,
              "krivo (mm) ide samo kad čestica izlazi iz ploča"
            );
          }
          if (s.tolerancija != null)
            need(
              isNum(s.tolerancija) && s.tolerancija > 0,
              "tolerancija mora biti pozitivan broj (mm za otklon, cm kad udari u ploču)"
            );
        }
        need(s.akcija == null && s.pitaj == null, "pogodi nema akcija ni pitaj");
        return list;
      }
      need(!imaCesticu(s), "predvidi je za kondenzator, bez čestice");
      need(s.U != null, "predvidi treba napon U (V)");
      need(
        s.C != null || s.A != null && s.d != null,
        "predvidi treba C (pF) ili A (cm²) i d (mm)"
      );
      need(
        s.izvor != null,
        'predvidi treba izvor: "spojen" (napon stalan) ili "odspojen" (naboj stalan)'
      );
      const a = s.akcija;
      if (need(
        a && AKCIJE.includes(a.vrsta),
        `predvidi treba akcija: {vrsta: ${AKCIJE.join(" | ")}}`
      )) {
        if (a.vrsta === "dielektrik")
          need(
            isNum(a.er) && a.er > 1 && a.er <= 100 && a.er !== (s.er ?? 1),
            "akcija dielektrik treba er (> 1 i različit od početnog)"
          );
        else
          need(
            isNum(a.faktor) && a.faktor > 0 && a.faktor !== 1 && a.faktor <= 10 && a.faktor >= 0.1,
            "akcija treba faktor (0,1–10, različit od 1)"
          );
        if (a.naziv != null)
          need(isText(a.naziv), "akcija.naziv mora biti tekst");
      }
      need(
        Array.isArray(s.pitaj) && s.pitaj.length >= 1 && s.pitaj.every((v) => VELICINE_KOND.includes(v)) && new Set(s.pitaj).size === s.pitaj.length,
        `pitaj: 1–5 veličina iz ${VELICINE_KOND.join(", ")} bez ponavljanja`
      );
      need(s.pogodi == null && s.krivo == null, "predvidi nema pogodi ni krivo");
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "pogodi") return checkPogodi(s, odgovor3);
      if (s.nacin === "predvidi") return checkPredvidi2(s, odgovor3);
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "pogodi") {
        const p = putanja(s);
        const ploca = p.smjer > 0 ? "gornju (+)" : "donju (−)";
        return p.izlaz ? `izađe ${fmtMm(p.y)} ${p.smjer > 0 ? "iznad" : "ispod"} pravca ulaska (y = ${tri(p.y)}·10⁻³ m)` : `udari u ${ploca} ploču ${fmtCm(p.x)} od ulaza`;
      }
      if (s.nacin === "predvidi") {
        const t = smjeroviPromjene(s);
        return s.pitaj.map((v) => `${v} ${SMJER_OPIS[t[v]]}`).join(", ");
      }
      return "";
    },
    brojcaniOdgovor(s) {
      if (s.nacin !== "pogodi") return null;
      const p = putanja(s);
      return p.izlaz ? p.y : p.x;
    }
  };
  var tolPogodi = (s, p) => Math.max(
    isNum(s.tolerancija) ? s.tolerancija : 0,
    0.08 * (p.izlaz ? p.y : p.x)
  );
  function checkPogodi(s, odgovor3) {
    const o = odgovor3;
    if (!o || !["izlaz", "gore", "dolje"].includes(o.gdje) || !isNum(o.x) || !isNum(o.y))
      return {
        tocno: false,
        poruka: "Povuci oznaku na mjesto gdje čestica izlazi iz ploča ili udari u ploču.",
        prazno: true
      };
    const p = putanja(s);
    const tol2 = tolPogodi(s, p);
    const elektron = s.cestica.vrsta === "elektron";
    const naziv4 = elektron ? "Elektron" : "Proton";
    const strana2 = o.gdje === "izlaz" ? Math.abs(o.y) <= tol2 ? 0 : Math.sign(o.y) : o.gdje === "gore" ? 1 : -1;
    if (strana2 === -p.smjer)
      return {
        tocno: false,
        poruka: elektron ? "Elektron je negativan, pa ga privlači pozitivna (gornja) ploča: skreće prema njoj." : "Proton je pozitivan, pa ga privlači negativna (donja) ploča: skreće prema njoj."
      };
    if (p.izlaz) {
      if (o.gdje !== "izlaz")
        return {
          tocno: false,
          poruka: `${naziv4} ne stigne do ploče: izađe kroz desni rub prije nego što skrene za pola razmaka (${fmtMm(s.d / 2)}). Ako ti je račun dao više, provjeri jesi li L pretvorio u metre i kvadrirao vrijeme.`
        };
      const y = Math.abs(o.y);
      if (Math.abs(y - p.y) <= tol2) return { tocno: true };
      const hit = (s.krivo ?? []).find(
        (k) => Math.abs(y - k.vrijednost) <= Math.max(tol2, 0.08 * Math.abs(k.vrijednost))
      );
      if (hit) return { tocno: false, poruka: hit.poruka };
      if (Math.abs(y - 2 * p.y) <= Math.max(tol2, 0.08 * 2 * p.y))
        return {
          tocno: false,
          poruka: "Oznaka je dvaput dalje nego treba. Jesi li zaboravio ½ u $y = \\frac{1}{2} a t^2$?"
        };
      const drugaMasa = elektron ? p.y * M_E / M_P : p.y * M_P / M_E;
      if (drugaMasa < s.d / 2 && Math.abs(y - drugaMasa) <= Math.max(tol2, 0.08 * drugaMasa))
        return {
          tocno: false,
          poruka: elektron ? "Tako bi skrenuo proton. Za elektron uzmi njegovu masu $m_e = 9{,}11 \\cdot 10^{-31}$ kg." : "Tako bi skrenuo elektron. Za proton uzmi njegovu masu $m_p = 1{,}67 \\cdot 10^{-27}$ kg."
        };
      return {
        tocno: false,
        poruka: s.savjet || `${naziv4} izađe ${y > p.y ? "bliže pravcu ulaska" : "dalje od pravca ulaska"}. Izračunaj $a = \\frac{qE}{m}$, vrijeme prolaska $t = \\frac{L}{v_0}$ i otklon $y = \\frac{1}{2} a t^2$.`
      };
    }
    if (o.gdje === "izlaz")
      return {
        tocno: false,
        poruka: `Otklon na kraju ploča bio bi veći od pola razmaka (${fmtMm(s.d / 2)}), pa ${naziv4.toLowerCase()} udari u ploču prije kraja. Iz $\\frac{d}{2} = \\frac{1}{2} a t^2$ nađi vrijeme, pa $x = v_0 t$.`
      };
    if (Math.abs(o.x - p.x) <= tol2) return { tocno: true };
    return {
      tocno: false,
      poruka: s.savjet || `${naziv4} udari ${o.x > p.x ? "bliže ulazu" : "dalje od ulaza"}. Iz $\\frac{d}{2} = \\frac{1}{2} a t^2$ nađi vrijeme do ploče, pa $x = v_0 t$.`
    };
  }
  var SMJEROVI3 = ["gore", "dolje", "isto"];
  function checkPredvidi2(s, odgovor3) {
    if (!odgovor3 || s.pitaj.some((v) => !SMJEROVI3.includes(odgovor3[v])))
      return {
        tocno: false,
        poruka: "Za svaku veličinu odaberi: raste, pada ili ostaje ista.",
        prazno: true
      };
    const t = smjeroviPromjene(s);
    const kriva = s.pitaj.find((v) => odgovor3[v] !== t[v]);
    if (!kriva) return { tocno: true };
    return { tocno: false, poruka: objasni(s, kriva, t), velicina: kriva };
  }
  function objasni(s, v, t) {
    const spojen = s.izvor === "spojen";
    const { fC, fd } = nakonAkcije(s);
    const a = s.akcija;
    const Cgore = fC > 1;
    const zasto = a.vrsta === "dielektrik" ? `dielektrik povećava $\\varepsilon_r$` : a.vrsta === "razmakni" ? `${fd > 1 ? "veći" : "manji"} razmak $d$` : `${a.faktor > 1 ? "veća" : "manja"} površina $S$`;
    const C = Cgore ? "raste" : "pada";
    switch (v) {
      case "C":
        return `Kapacitet ovisi samo o građi kondenzatora: $C = \\varepsilon_0 \\varepsilon_r \\frac{S}{d}$, a ${zasto}, pa C ${C}. Izvor tu ne igra ulogu.`;
      case "Q":
        return spojen ? `Izvor drži napon stalnim, pa kad C ${C}, izvor ${Cgore ? "dovede još" : "odvede dio"} naboja: $Q = CU$ ${t.Q === "gore" ? "raste" : "pada"}.` : "Odspojen kondenzator nema kamo poslati naboj ni odakle ga dobiti: Q ostaje isti.";
      case "U":
        return spojen ? "Kondenzator je spojen na izvor, pa je napon na njemu uvijek napon izvora: U ostaje isti." : `Naboj je zaključan, a C ${C}, pa $U = \\frac{Q}{C}$ ${t.U === "gore" ? "raste" : "pada"}.`;
      case "E":
        if (t.E === "isto")
          return spojen ? "Napon je stalan i razmak se ne mijenja, pa $E = \\frac{U}{d}$ ostaje isti." : "Napon se mijenja jednako kao razmak, pa $E = \\frac{U}{d}$ ostaje isti (gustoća naboja na pločama je ista).";
        return spojen ? `Napon je stalan, a razmak ${fd > 1 ? "veći" : "manji"}, pa $E = \\frac{U}{d}$ ${t.E === "gore" ? "raste" : "pada"}.` : `Polje je $E = \\frac{U}{d}$: napon ${t.U === "gore" ? "raste" : "pada"}${fd !== 1 ? ", ali pazi i na razmak" : ", a razmak je isti"}, pa E ${t.E === "gore" ? "raste" : "pada"}.`;
      case "W":
        return spojen ? `Napon je stalan, a C ${C}, pa $W = \\frac{CU^2}{2}$ ${t.W === "gore" ? "raste" : "pada"}.` : `Naboj je stalan, a C ${C}, pa $W = \\frac{Q^2}{2C}$ ${t.W === "gore" ? "raste" : "pada"}.`;
    }
    return "Nije točno.";
  }

  // src/lib/lesson/katalog/fizika/_krug.js
  var POSEBNI = ["izvor", "r", "ukupno"];
  var JEDINICA = { I: "A", U: "V", P: "W", R: "Ω" };
  var fmt = (v) => formatSci(v, 3);
  var SUB = {
    0: "₀",
    1: "₁",
    2: "₂",
    3: "₃",
    4: "₄",
    5: "₅",
    6: "₆",
    7: "₇",
    8: "₈",
    9: "₉"
  };
  var naziv2 = (c) => c.oznaka || String(c.id ?? "").replace(
    /_?(\d+)$/,
    (m, d) => [...d].map((x) => SUB[x]).join("")
  );
  function vrstaCvora(n) {
    if (!n || typeof n !== "object" || Array.isArray(n)) return null;
    const k = [
      ["serijski", "ser"],
      ["paralelno", "par"],
      ["zarulja", "Z"],
      ["sklopka", "S"],
      ["R", "R"]
    ].filter(([f2]) => f2 in n);
    return k.length === 1 ? k[0][1] : null;
  }
  function stablo(spoj) {
    const listovi = [];
    const grupe = [];
    const po = {};
    const walk = (n, key, dubina, roditelj) => {
      const vrsta = vrstaCvora(n);
      const c = {
        key,
        id: n.id ?? null,
        oznaka: n.oznaka ?? null,
        vrsta,
        dubina,
        roditelj
      };
      if (vrsta === "ser" || vrsta === "par") {
        c.djeca = (vrsta === "ser" ? n.serijski : n.paralelno).map(
          (d, i) => walk(d, `${key}.${i}`, dubina + 1, c)
        );
        grupe.push(c);
      } else {
        if (vrsta === "R") c.R = n.R;
        if (vrsta === "Z") {
          c.Unaz = n.zarulja.U;
          c.Pnaz = n.zarulja.P;
          c.R = n.zarulja.U * n.zarulja.U / n.zarulja.P;
        }
        if (vrsta === "S") c.zatvorena = n.sklopka === "zatvorena";
        listovi.push(c);
      }
      if (c.id) po[c.id] = c;
      return c;
    };
    return { korijen: walk(spoj, "k", 0, null), listovi, grupe, po };
  }
  function pocetnoStanje(spec) {
    return {
      emf: spec.izvor?.emf ?? 0,
      r: spec.izvor?.r ?? 0,
      R: {},
      sklopke: {},
      spoj: {}
    };
  }
  var zatvorena = (c, st) => st.sklopke?.[c.id] ?? c.zatvorena;
  var vrstaSkupine = (c, st) => st.spoj?.[c.key] ?? c.vrsta;
  function rijesi(spec, st, T = stablo(spec.spoj)) {
    const po = {};
    const Rc = {};
    const otpor = (c) => {
      let R2;
      if (c.djeca) {
        const rs = c.djeca.map(otpor);
        if (vrstaSkupine(c, st) === "ser") R2 = rs.reduce((s, x) => s + x, 0);
        else if (rs.some((x) => x === 0)) R2 = 0;
        else {
          const g = rs.reduce((s, x) => s + (x === Infinity ? 0 : 1 / x), 0);
          R2 = g === 0 ? Infinity : 1 / g;
        }
      } else if (c.vrsta === "S") R2 = zatvorena(c, st) ? 0 : Infinity;
      else if (c.vrsta === "R") R2 = st.R?.[c.id] ?? c.R;
      else R2 = c.R;
      Rc[c.key] = R2;
      return R2;
    };
    const raspodijeli = (c, I2, U) => {
      const R2 = Rc[c.key];
      po[c.key] = { I: I2, U, P: U * I2, R: R2 };
      if (!c.djeca) return;
      if (vrstaSkupine(c, st) === "ser") {
        if (R2 === Infinity) {
          const n = c.djeca.filter((d) => Rc[d.key] === Infinity).length;
          c.djeca.forEach(
            (d) => raspodijeli(d, 0, Rc[d.key] === Infinity ? U / n : 0)
          );
        } else c.djeca.forEach((d) => raspodijeli(d, I2, I2 * Rc[d.key]));
      } else if (R2 === 0) {
        const n = c.djeca.filter((d) => Rc[d.key] === 0).length;
        c.djeca.forEach((d) => raspodijeli(d, Rc[d.key] === 0 ? I2 / n : 0, 0));
      } else
        c.djeca.forEach(
          (d) => raspodijeli(d, Rc[d.key] === Infinity ? 0 : U / Rc[d.key], U)
        );
    };
    const Rv = otpor(T.korijen);
    const emf = st.emf;
    const r = st.r;
    const I = Rv === Infinity || Rv + r === 0 ? 0 : emf / (Rv + r);
    const Ut = Rv === Infinity ? emf : I * Rv;
    raspodijeli(T.korijen, I, Ut);
    return { po, I, Ut, Rv, emf, r, T };
  }
  function velicineOd2(rez, element) {
    if (element === "ukupno")
      return { I: rez.I, U: rez.Ut, P: rez.Ut * rez.I, R: rez.Rv };
    if (element === "izvor")
      return { I: rez.I, U: rez.Ut, P: rez.emf * rez.I, R: rez.Rv + rez.r };
    if (element === "r")
      return { I: rez.I, U: rez.I * rez.r, P: rez.I * rez.I * rez.r, R: rez.r };
    const c = rez.T.po[element];
    return c ? rez.po[c.key] : null;
  }
  var vrijednost2 = (rez, velicina3, element) => velicineOd2(rez, element)?.[velicina3];
  var LEAD = 10;
  var GAP = 10;
  var RAIL = 12;
  var AM = 40;
  var MR = 11;
  var RH = 14;
  var RW = 46;
  var BRG = 14;
  var GAPV = 14;
  var GAPC = 18;
  var LINE = 14;
  var TIJELO2 = { R: [40, 8, 8], Z: [28, 14, 14], S: [36, 18, 4] };
  var sirinaTeksta = (s) => String(s).length * 6.9 + 2;
  var preslikaj = (o, x0, y0) => o === "h" ? (u, v) => [x0 + u, y0 + v] : (u, v) => [x0 + v, y0 + u];
  var strana = (o) => o === "h" ? { ls: -1, bs: 1 } : { ls: 1, bs: -1 };
  var ocitanjeAmp = (o) => o === "h" ? MR + 4 + RH : MR + 6 + RW;
  var ocitanjeMosta = (o) => o === "h" ? MR + 4 + RH : MR + 6 + RW;
  function rasporedi(spec, st, T, { linije, oznakaIzvora: oznakaIzvora2 }) {
    const mj = spec.mjerila ?? [];
    const imaMj = (vrsta, el) => mj.some((m) => m.vrsta === vrsta && m.element === el);
    const refMj = (vrsta, c) => c.id && imaMj(vrsta, c.id) ? c.id : null;
    const pripremi2 = (c) => {
      const A = refMj("A", c);
      const V = refMj("V", c);
      if (!c.djeca) return { key: c.key, c, vrsta: c.vrsta, A, V };
      const v = vrstaSkupine(c, st);
      let djeca = c.djeca.map(pripremi2);
      if (djeca.length === 1) {
        const k = djeca[0];
        k.A ??= A;
        k.V ??= V;
        return k;
      }
      djeca = djeca.flatMap(
        (d) => d.djeca && d.vrsta === v && !d.A && !d.V ? d.djeca : [d]
      );
      return { key: c.key, c, vrsta: v, djeca, A, V };
    };
    let korijen = pripremi2(T.korijen);
    let dolje = mj.find(
      (m) => m.vrsta === "A" && (m.element === "izvor" || m.element === "ukupno")
    )?.element ?? null;
    let lijevo = mj.find(
      (m) => m.vrsta === "V" && (m.element === "izvor" || m.element === "ukupno")
    )?.element ?? null;
    const naR = imaMj("V", "r") ? "r" : null;
    let niz = [];
    let stupci = null;
    if (!korijen.djeca) niz = [korijen];
    else if (korijen.vrsta === "ser") {
      dolje ??= korijen.A;
      lijevo ??= korijen.V;
      korijen.A = korijen.V = null;
      const zadnji = korijen.djeca.at(-1);
      if (zadnji.djeca && zadnji.vrsta === "par") {
        niz = korijen.djeca.slice(0, -1);
        stupci = zadnji;
      } else niz = korijen.djeca;
    } else stupci = korijen;
    if (stupci) {
      dolje ??= stupci.A;
      stupci.A = null;
    }
    const mjeri = (L, o) => {
      let m;
      if (!L.djeca) {
        const [tl, th, to] = TIJELO2[L.vrsta];
        const red = linije(L.c);
        const lw = Math.max(...red.map(sirinaTeksta));
        const tijelo2 = Math.max(tl + 2 * LEAD, o === "h" ? lw + 10 : 0);
        const am = L.A ? AM : 0;
        const ls = Math.max(
          o === "h" ? th + 6 + red.length * LINE : th + 8 + lw,
          L.A ? ocitanjeAmp(o) : 0
        );
        let bs = Math.max(to, L.A ? MR : 0);
        const dV = bs + BRG;
        if (L.V) bs = dV + ocitanjeMosta(o);
        m = { len: am + tijelo2, ls, bs, am, tijelo: tijelo2, dV, red };
      } else if (L.vrsta === "ser") {
        const ms = L.djeca.map((d) => mjeri(d, o));
        const am = L.A ? AM : 0;
        const ls = Math.max(...ms.map((x2) => x2.ls), L.A ? ocitanjeAmp(o) : 0);
        let bs = Math.max(...ms.map((x2) => x2.bs), L.A ? MR : 0);
        const dV = bs + BRG;
        if (L.V) bs = dV + ocitanjeMosta(o);
        m = {
          len: am + ms.reduce((s, x2) => s + x2.len, 0) + GAP * (ms.length - 1),
          ls,
          bs,
          am,
          dV
        };
      } else {
        const grane = L.djeca.map((d) => ({ L: d, m: mjeri(d, o) }));
        if (L.V)
          grane.push({
            L: null,
            ref: L.V,
            m: { len: 2 * MR + 2 * LEAD, ls: ocitanjeAmp(o), bs: MR }
          });
        const neg = (x2) => o === "h" ? x2.ls : x2.bs;
        const pos = (x2) => o === "h" ? x2.bs : x2.ls;
        let v = 0;
        grane.forEach((g, i) => {
          if (i) v += pos(grane[i - 1].m) + GAPV + neg(g.m);
          g.v = v;
        });
        const am = L.A ? AM : 0;
        const unutra2 = Math.max(...grane.map((g) => g.m.len));
        const n = neg(grane[0].m);
        const p = v + pos(grane.at(-1).m);
        let ls = o === "h" ? n : p;
        let bs = o === "h" ? p : n;
        if (L.A) {
          ls = Math.max(ls, ocitanjeAmp(o));
          bs = Math.max(bs, MR);
        }
        m = { len: am + unutra2 + 2 * RAIL, ls, bs, am, grane, unutra: unutra2 };
      }
      L.m = m;
      return m;
    };
    const zice = [];
    const elementi = [];
    const mjerila = [];
    const cvorovi = [];
    const zica = (pts, kljucevi) => zice.push({ pts, kljucevi });
    const mjerilo2 = (vrsta, element, [x2, y], ocitanje) => mjerila.push({ vrsta, element, x: x2, y, ocitanje });
    const ocitanjeUz = (o, P, u2, v, sgn) => o === "h" ? {
      ...xy(P(u2, v + sgn * (MR + 4))),
      anchor: "middle",
      baseline: sgn < 0 ? "auto" : "hanging"
    } : {
      ...xy(P(u2, v + sgn * (MR + 6))),
      anchor: sgn > 0 ? "start" : "end",
      baseline: "central"
    };
    const crtaj = (L, o, P, u0, v0) => {
      const m = L.m;
      const { ls, bs } = strana(o);
      let u2 = u0;
      const kroz = [L.key];
      if (m.am) {
        zica([P(u2, v0), P(u2 + AM, v0)], kroz);
        mjerilo2(
          "A",
          L.A,
          P(u2 + AM / 2, v0),
          ocitanjeUz(o, P, u2 + AM / 2, v0, ls)
        );
        u2 += AM;
      }
      const most = (a, b, uc, d, ref) => {
        zica([P(a, v0), P(a, v0 + bs * d), P(b, v0 + bs * d), P(b, v0)], null);
        cvorovi.push(P(a, v0), P(b, v0));
        mjerilo2(
          "V",
          ref,
          P(uc, v0 + bs * d),
          ocitanjeUz(o, P, uc, v0 + bs * d, bs)
        );
      };
      if (!L.djeca) {
        const [tl, th] = TIJELO2[L.vrsta];
        const uc = u2 + m.tijelo / 2;
        if (L.vrsta === "S") {
          zica([P(u2, v0), P(uc - tl / 2, v0)], kroz);
          zica([P(uc + tl / 2, v0), P(u2 + m.tijelo, v0)], kroz);
        } else zica([P(u2, v0), P(u2 + m.tijelo, v0)], kroz);
        const oz = o === "h" ? { ...xy(P(uc, v0 - (th + 6))), anchor: "middle", poravnaj: "dno" } : { ...xy(P(uc, v0 + th + 8)), anchor: "start", poravnaj: "sredina" };
        elementi.push({ c: L.c, ...xy(P(uc, v0)), o, oznaka: oz, ls });
        if (L.V) most(uc - tl / 2 - 5, uc + tl / 2 + 5, uc, m.dV, L.V);
        return;
      }
      if (L.vrsta === "ser") {
        const start = u2;
        L.djeca.forEach((d, i) => {
          if (i) {
            zica([P(u2, v0), P(u2 + GAP, v0)], kroz);
            u2 += GAP;
          }
          crtaj(d, o, P, u2, v0);
          u2 += d.m.len;
        });
        if (L.V) most(start, u2, (start + u2) / 2, m.dV, L.V);
        return;
      }
      const uA = u2 + RAIL;
      const uB = u0 + m.len - RAIL;
      zica([P(u2, v0), P(uA, v0)], kroz);
      zica([P(uB, v0), P(u0 + m.len, v0)], kroz);
      const g = m.grane;
      g.forEach((gr, i) => {
        const v = v0 + gr.v;
        const us = uA + (uB - uA - gr.m.len) / 2;
        const kljucevi = gr.L ? [gr.L.key] : null;
        zica([P(uA, v), P(us, v)], kljucevi);
        zica([P(us + gr.m.len, v), P(uB, v)], kljucevi);
        if (gr.L) crtaj(gr.L, o, P, us, v);
        else {
          zica([P(us, v), P(us + gr.m.len, v)], null);
          mjerilo2(
            "V",
            gr.ref,
            P(us + gr.m.len / 2, v),
            ocitanjeUz(o, P, us + gr.m.len / 2, v, ls)
          );
        }
        if (i < g.length - 1) {
          const iza = g.slice(i + 1).filter((x2) => x2.L).map((x2) => x2.L.key);
          const vn = v0 + g[i + 1].v;
          zica([P(uA, v), P(uA, vn)], iza.length ? iza : null);
          zica([P(uB, vn), P(uB, v)], iza.length ? iza : null);
          cvorovi.push(P(uA, v), P(uB, v));
        }
      });
    };
    const imaIzvor = !!spec.izvor;
    const rUnutra2 = imaIzvor && (spec.izvor.r > 0 || (spec.klizaci ?? []).some((k) => k.element === "r"));
    const izvRed = imaIzvor ? oznakaIzvora2() : [];
    const izvW = Math.max(0, ...izvRed.map(sirinaTeksta));
    const izvPola = rUnutra2 ? 22 : 17;
    const mostL = imaIzvor && (lijevo || naR);
    const xs = 8 + (mostL ? izvPola + 14 + MR + 6 + RW : 26);
    const xNiz = xs + Math.max(48, izvPola + 10 + izvW + 14);
    niz.forEach((L) => mjeri(L, "h"));
    const nizLen = niz.reduce((s, L) => s + L.m.len, 0) + GAP * Math.max(0, niz.length - 1);
    const nizLs = Math.max(0, ...niz.map((L) => L.m.ls));
    const nizBs = Math.max(0, ...niz.map((L) => L.m.bs));
    let kolone = [];
    if (stupci) {
      kolone = stupci.djeca.map((d) => ({ L: d, m: mjeri(d, "v") }));
      if (stupci.V)
        kolone.push({
          L: null,
          ref: stupci.V,
          m: { len: 2 * MR + 2 * LEAD, ls: ocitanjeAmp("v"), bs: MR }
        });
    }
    let x = xNiz + nizLen;
    kolone.forEach((k, i) => {
      x += i ? kolone[i - 1].m.ls + GAPC + k.m.bs : (niz.length ? GAP + 6 : 0) + k.m.bs;
      k.x = x;
    });
    const xR = kolone.length ? kolone.at(-1).x : x + 16;
    const sirina = (kolone.length ? xR + kolone.at(-1).m.ls : xR) + 10;
    const presjek2 = spec.presjek && imaIzvor;
    const yTop = 10 + Math.max(nizLs, presjek2 ? 28 : 12);
    const visinaIzvora = imaIzvor ? rUnutra2 ? 128 : 96 : 70;
    const H2 = Math.max(
      visinaIzvora,
      nizBs + 22 + (dolje ? MR : 0),
      ...kolone.map((k) => k.m.len + 16),
      mostL ? 96 : 0
    );
    const yBot = yTop + H2;
    const visina = yBot + (dolje ? MR + 4 + RH + 8 : 12);
    const ym = (yTop + yBot) / 2;
    const xPrvi = kolone.length ? kolone[0].x : xR;
    zica(
      [
        [xs, yTop],
        [xNiz, yTop]
      ],
      ["@"]
    );
    let u = xNiz;
    niz.forEach((L, i) => {
      if (i) {
        zica(
          [
            [u, yTop],
            [u + GAP, yTop]
          ],
          ["@"]
        );
        u += GAP;
      }
      crtaj(L, "h", preslikaj("h", 0, yTop), u, 0);
      u += L.m.len;
    });
    zica(
      [
        [u, yTop],
        [xPrvi, yTop]
      ],
      ["@"]
    );
    if (kolone.length) {
      kolone.forEach((k, i) => {
        const y0 = yTop + (H2 - k.m.len) / 2;
        const kljucevi = k.L ? [k.L.key] : null;
        zica(
          [
            [k.x, yTop],
            [k.x, y0]
          ],
          kljucevi
        );
        zica(
          [
            [k.x, y0 + k.m.len],
            [k.x, yBot]
          ],
          kljucevi
        );
        if (k.L) crtaj(k.L, "v", preslikaj("v", k.x, y0), 0, 0);
        else {
          zica(
            [
              [k.x, y0],
              [k.x, y0 + k.m.len]
            ],
            null
          );
          mjerilo2("V", k.ref, [k.x, ym], {
            x: k.x + MR + 6,
            y: ym,
            anchor: "start",
            baseline: "central"
          });
        }
        if (i < kolone.length - 1) {
          const iza = kolone.slice(i + 1).filter((z) => z.L).map((z) => z.L.key);
          zica(
            [
              [k.x, yTop],
              [kolone[i + 1].x, yTop]
            ],
            iza.length ? iza : null
          );
          zica(
            [
              [kolone[i + 1].x, yBot],
              [k.x, yBot]
            ],
            iza.length ? iza : null
          );
          cvorovi.push([k.x, yTop], [k.x, yBot]);
        }
      });
    } else
      zica(
        [
          [xR, yTop],
          [xR, yBot]
        ],
        ["@"]
      );
    zica(
      [
        [xPrvi, yBot],
        [xs, yBot]
      ],
      ["@"]
    );
    if (dolje) {
      const xa = (xs + xPrvi) / 2;
      mjerilo2("A", dolje, [xa, yBot], {
        x: xa,
        y: yBot + MR + 4,
        anchor: "middle",
        baseline: "hanging"
      });
    }
    let izvor2 = null;
    if (imaIzvor) {
      const yb = rUnutra2 ? ym - 24 : ym;
      const yr = rUnutra2 ? ym + 26 : null;
      zica(
        [
          [xs, yBot],
          [xs, yb + 5]
        ],
        ["@"]
      );
      zica(
        [
          [xs, yb - 5],
          [xs, yTop]
        ],
        ["@"]
      );
      izvor2 = {
        x: xs,
        yb,
        yr,
        kutija: rUnutra2 ? {
          x: xs - izvPola,
          y: yb - 22,
          w: 2 * izvPola,
          h: yr + 30 - (yb - 22)
        } : null,
        oznaka: { x: xs + izvPola + 10, red: izvRed }
      };
      if (mostL) {
        const xb = xs - izvPola - 14;
        if (lijevo) {
          zica(
            [
              [xs, yTop],
              [xb, yTop],
              [xb, yBot],
              [xs, yBot]
            ],
            null
          );
          cvorovi.push([xs, yTop], [xs, yBot]);
          mjerilo2("V", lijevo, [xb, ym], {
            x: xb - MR - 6,
            y: ym,
            anchor: "end",
            baseline: "central"
          });
        } else {
          const a = yr - 22;
          const b = yr + 22;
          zica(
            [
              [xs, a],
              [xb, a],
              [xb, b],
              [xs, b]
            ],
            null
          );
          cvorovi.push([xs, a], [xs, b]);
          mjerilo2("V", naR, [xb, yr], {
            x: xb - MR - 6,
            y: yr,
            anchor: "end",
            baseline: "central"
          });
        }
      }
    }
    return {
      w: Math.round(sirina),
      h: Math.round(visina),
      zice: zice.map((z) => ({ ...z, pts: z.pts.map(([px, py]) => [r1(px), r1(py)]) })).filter((z) => z.pts.length > 1 && duljina2(z.pts) > 0.5),
      elementi,
      mjerila,
      cvorovi: cvorovi.map(([px, py]) => [r1(px), r1(py)]),
      izvor: izvor2,
      stezaljke: imaIzvor ? null : { x: xs, yTop, yBot },
      presjek: presjek2 ? { x: r1((xs + xNiz) / 2), y: yTop } : null,
      yTop,
      yBot
    };
  }
  var r1 = (n) => Math.round(n * 10) / 10;
  var xy = ([x, y]) => ({ x: r1(x), y: r1(y) });
  var duljina2 = (pts) => pts.slice(1).reduce((s, p, i) => s + Math.hypot(p[0] - pts[i][0], p[1] - pts[i][1]), 0);

  // src/lib/lesson/katalog/fizika/strujni_krug.js
  var NACINI13 = ["istrazi", "izracunaj", "postavi", "odaberi"];
  var VELICINE4 = ["I", "U", "P", "R"];
  var ID6 = /^[A-Za-zČĆŽŠĐčćžšđ][A-Za-z0-9_ČĆŽŠĐčćžšđ]*$/;
  var MAX_W = 470;
  var MAX_H = 520;
  var KRITERIJI = {
    najsjajnija: ["P", 1, true],
    najtamnija: ["P", -1, true],
    najveca_struja: ["I", 1],
    najmanja_struja: ["I", -1],
    najveci_napon: ["U", 1],
    najmanji_napon: ["U", -1],
    najveca_snaga: ["P", 1],
    najmanja_snaga: ["P", -1]
  };
  var PROMJENE = {
    ugase_se: "se ugase",
    upale_se: "se upale",
    jace: "svijetle jače",
    slabije: "svijetle slabije (ali i dalje svijetle)",
    ne_mijenja: "svijetle jednako kao prije"
  };
  var aligned3 = (v, step) => Math.abs(v / step - Math.round(v / step)) < 1e-6;
  var clean3 = (x) => Math.round(x * 1e9) / 1e9;
  var oznakaIzvora = (s) => s.izvor?.oznaka ?? (s.izvor?.r != null ? "ε" : "U");
  var rUnutra = (s) => !!s.izvor && (s.izvor.r > 0 || (s.klizaci ?? []).some((k) => k.element === "r"));
  function fraza(T, v, el) {
    const c = T.po[el];
    const g = c?.djeca ? c.oznaka ?? c.id : null;
    const ime2 = c ? naziv2(c) : el;
    if (v === "I")
      return el === "izvor" || el === "ukupno" || el === "r" ? "struja iz izvora" : g ? `struja kroz skupinu ${g}` : `struja kroz ${ime2}`;
    if (v === "U")
      return el === "izvor" || el === "ukupno" ? "napon stezaljki izvora" : el === "r" ? "napon na unutarnjem otporu" : g ? `napon na skupini ${g}` : `napon na ${ime2}`;
    if (v === "P")
      return el === "izvor" ? "ukupna snaga izvora" : el === "ukupno" ? "snaga vanjskog dijela kruga" : el === "r" ? "snaga na unutarnjem otporu" : g ? `snaga na skupini ${g}` : `snaga na ${ime2}`;
    return el === "izvor" ? "otpor cijelog kruga (s unutarnjim)" : el === "ukupno" ? "ukupni otpor" : el === "r" ? "unutarnji otpor" : g ? `otpor skupine ${g}` : `otpor ${ime2}`;
  }
  var veliko2 = (t) => t.charAt(0).toUpperCase() + t.slice(1);
  var oznakaPitanja = (s, T, q) => q.oznaka ?? `${veliko2(fraza(T, q.velicina, q.element))} =`;
  function pitanoNaShemi(s, c) {
    if (s.nacin !== "izracunaj" || !c.id) return [];
    const mj = s.mjerila ?? [];
    return (s.pitaj ?? []).filter(
      (q) => q.element === c.id && !mj.some(
        (m) => m.element === c.id && (m.vrsta === "A" && q.velicina === "I" || m.vrsta === "V" && q.velicina === "U")
      )
    ).map((q) => q.velicina);
  }
  function linijeElementa(s, c, st) {
    const red = [naziv2(c)];
    if (c.vrsta === "R") red.push(`${fmt(st.R?.[c.id] ?? c.R)} Ω`);
    if (c.vrsta === "Z") red.push(`${fmt(c.Unaz)} V, ${fmt(c.Pnaz)} W`);
    const q = pitanoNaShemi(s, c);
    if (q.length) red.push(q.map((v) => `${v} = ?`).join(", "));
    return red;
  }
  function linijeIzvora(s, st) {
    const red = [`${oznakaIzvora(s)} = ${fmt(st.emf)} V`];
    if (rUnutra(s)) red.push(`r = ${fmt(st.r)} Ω`);
    return red;
  }
  var kraj2 = (s, el, k) => k === 0 ? s.klizaci.find((x) => x.element === el)?.min : s.klizaci.find((x) => x.element === el)?.max;
  var sire = (a, b) => Math.max(...a.map(sirinaTeksta)) >= Math.max(...b.map(sirinaTeksta)) ? a : b;
  function oznakeZaRaspored(s) {
    const st = pocetnoStanje(s);
    const ima2 = (el) => (s.klizaci ?? []).some((k) => k.element === el);
    return {
      linije: (c) => {
        const a = linijeElementa(s, c, st);
        if (c.vrsta !== "R" || !ima2(c.id)) return a;
        return [0, 1].map(
          (k) => linijeElementa(s, c, { ...st, R: { [c.id]: kraj2(s, c.id, k) } })
        ).reduce(sire, a);
      },
      oznakaIzvora: () => {
        let a = linijeIzvora(s, st);
        for (const k of [0, 1])
          a = sire(
            a,
            linijeIzvora(s, {
              ...st,
              emf: ima2("izvor") ? kraj2(s, "izvor", k) : st.emf,
              r: ima2("r") ? kraj2(s, "r", k) : st.r
            })
          );
        return a;
      }
    };
  }
  function grupaIzbora(s, T) {
    if (s.spoj_izbor === true) return T.korijen.djeca ? T.korijen : null;
    const c = typeof s.spoj_izbor === "string" ? T.po[s.spoj_izbor] : null;
    return c?.djeca ? c : null;
  }
  function postaviVrijednost(st, el, v) {
    if (el === "izvor") st.emf = v;
    else if (el === "r") st.r = v;
    else st.R = { ...st.R, [el]: v };
  }
  function pocetnaVrijednost(s, T, el) {
    if (el === "izvor") return s.izvor?.emf;
    if (el === "r") return s.izvor?.r ?? 0;
    return T.po[el]?.R;
  }
  function stanjeIz(s, T, odg) {
    const st = pocetnoStanje(s);
    for (const k of s.klizaci ?? []) {
      const v = odg?.klizaci?.[k.element];
      if (isNum(v)) postaviVrijednost(st, k.element, v);
    }
    const g = grupaIzbora(s, T);
    if (g && (odg?.spoj === "ser" || odg?.spoj === "par"))
      st.spoj = { [g.key]: odg.spoj };
    return st;
  }
  var tolCilja = (s) => isNum(s.cilj.tolerancija) ? s.cilj.tolerancija : Math.max(1e-9, Math.abs(s.cilj.vrijednost) * 0.01);
  var ciljU = (s, T, odg) => vrijednost2(
    rijesi(s, stanjeIz(s, T, odg), T),
    s.cilj.velicina,
    s.cilj.element
  );
  var mreza2 = (k) => Array.from(
    { length: Math.round((k.max - k.min) / k.korak) + 1 },
    (_, i) => clean3(k.min + i * k.korak)
  );
  function pretrazi(s, T, samoSpoj) {
    const ks = s.klizaci ?? [];
    const g = grupaIzbora(s, T);
    const spojevi = samoSpoj ? [samoSpoj] : g ? [g.vrsta, g.vrsta === "ser" ? "par" : "ser"] : [null];
    let kombinacije = [{}];
    for (const k of ks)
      kombinacije = kombinacije.flatMap(
        (o) => mreza2(k).map((v) => ({ ...o, [k.element]: v }))
      );
    const rjesenja = [];
    let najblize = null;
    let imaKrivih = false;
    for (const spoj of spojevi) {
      for (const klizaci of kombinacije) {
        const odg = { klizaci, spoj };
        const v = ciljU(s, T, odg);
        if (!isNum(v)) {
          imaKrivih = true;
          continue;
        }
        const d = Math.abs(v - s.cilj.vrijednost);
        if (d <= tolCilja(s) + 1e-12) rjesenja.push({ odg, v, d });
        else imaKrivih = true;
        if (!najblize || d < najblize.d) najblize = { odg, v, d };
      }
    }
    rjesenja.sort((a, b) => a.d - b.d);
    return { rjesenja, najblize, imaKrivih };
  }
  function nazivKlizaca(s, T, k) {
    if (k.oznaka) return k.oznaka;
    if (k.element === "izvor") return oznakaIzvora(s);
    if (k.element === "r") return "r";
    return naziv2(T.po[k.element] ?? { id: k.element });
  }
  var jedinicaKlizaca = (k) => k.element === "izvor" ? "V" : "Ω";
  function kandidati(s, T) {
    if (Array.isArray(s.odaberi?.medju)) return s.odaberi.medju;
    const zarulje = T.listovi.filter((c) => c.vrsta === "Z").map((c) => c.id);
    const k = s.odaberi?.kriterij;
    if (zarulje.length && (KRITERIJI[k]?.[2] || k in PROMJENE)) return zarulje;
    return T.listovi.filter((c) => c.vrsta === "R" || c.vrsta === "Z").map((c) => c.id);
  }
  function stanjePoslije(s, T) {
    const st = pocetnoStanje(s);
    const c = T.po[s.odaberi?.sklopka];
    if (c?.vrsta === "S") st.sklopke = { [c.id]: !c.zatvorena };
    return st;
  }
  var snage = (s, T, st, ids) => {
    const rez = rijesi(s, st, T);
    return ids.map((id) => vrijednost2(rez, "P", id));
  };
  function promjena2(pb, pa, skala2) {
    const off = (p) => p <= 1e-9 * skala2;
    if (off(pb) && off(pa)) return "ne_svijetli";
    if (!off(pb) && off(pa)) return "ugase_se";
    if (off(pb)) return "upale_se";
    if (pa > pb * 1.01) return "jace";
    if (pa < pb * 0.99) return "slabije";
    return "ne_mijenja";
  }
  function rjesenjeOdaberi(s, T) {
    const ids = kandidati(s, T);
    const k = s.odaberi.kriterij;
    if (k in PROMJENE) {
      const pb = snage(s, T, pocetnoStanje(s), ids);
      const pa = snage(s, T, stanjePoslije(s, T), ids);
      const skala3 = Math.max(1e-12, ...pb, ...pa);
      const opis = ids.map((id, i) => promjena2(pb[i], pa[i], skala3));
      return { ids, tocni: ids.filter((id, i) => opis[i] === k), opis, pb, pa };
    }
    const [vel, smjer] = KRITERIJI[k];
    const rez = rijesi(s, pocetnoStanje(s), T);
    const vr = ids.map((id) => vrijednost2(rez, vel, id));
    const naj = smjer > 0 ? Math.max(...vr) : Math.min(...vr);
    const skala2 = Math.max(1e-12, ...vr.map(Math.abs));
    const tocni = ids.filter((id, i) => Math.abs(vr[i] - naj) <= 0.01 * skala2);
    return { ids, tocni, vr, vel };
  }
  var OPIS_PROMJENE2 = {
    ugase_se: "se ugasi",
    upale_se: "se upali",
    jace: "svijetli jače",
    slabije: "svijetli slabije",
    ne_mijenja: "svijetli jednako",
    ne_svijetli: "ne svijetli ni prije ni poslije"
  };
  var PAR_KAO_SER = "Paralelno spojene otpore zbrojio si kao da su u nizu. U paralelnom spoju zbrajaju se recipročne vrijednosti: $\\frac{1}{R} = \\frac{1}{R_1} + \\frac{1}{R_2}$.";
  var SER_KAO_PAR = "Serijski spojene otpore računao si kao paralelne. U serijskom spoju otpori se jednostavno zbrajaju: $R = R_1 + R_2$.";
  function uParalelnoj(c) {
    for (let p = c?.roditelj; p; p = p.roditelj)
      if (p.vrsta === "par" && p.djeca.length > 1) return true;
    return false;
  }
  function autoKrivo(s, T, q) {
    const st0 = pocetnoStanje(s);
    const rez = rijesi(s, st0, T);
    const { velicina: v, element: el } = q;
    const tocno = vrijednost2(rez, v, el);
    const out = [];
    const tol2 = Math.max(1e-9, Math.abs(tocno) * 0.01);
    const add2 = (x, poruka2) => {
      if (!isNum(x) || near(x, tocno, tol2) || out.some((k) => near(k.vrijednost, x, Math.abs(x) * 5e-3 + 1e-12)))
        return;
      out.push({ vrijednost: x, poruka: poruka2 });
    };
    const c = T.po[el];
    const list = c && !c.djeca ? c : null;
    const ime2 = c ? naziv2(c) : "";
    const oz = oznakaIzvora(s);
    const emf = st0.emf;
    const ostatak = () => {
      const unutra2 = st0.r > 0;
      const drugi2 = rez.po[c.key].U < rez.Ut * (1 - 1e-9);
      return unutra2 && drugi2 ? "unutarnjem otporu i ostalim otpornicima" : unutra2 ? "unutarnjem otporu izvora" : "ostalim otpornicima";
    };
    const Rel = c ? rez.po[c.key].R : null;
    if (list?.vrsta === "Z") {
      const st = rez.po[c.key];
      if (v === "P") {
        add2(
          c.Pnaz,
          `To je nazivna snaga, ona koja piše na žarulji. Žarulja je daje samo kad je na njoj ${withUnit(c.Unaz, "V")}, a ovdje je na njoj ${withUnit(st.U, "V")}.`
        );
        add2(
          c.Pnaz * (st.U / c.Unaz),
          "Snaga ne pada razmjerno naponu: kad se smanji napon, smanji se i struja. Zato $P = \\frac{U^2}{R}$, s otporom žarulje $R = \\frac{U_{naz}^2}{P_{naz}}$."
        );
      }
      if (v === "I")
        add2(
          c.Pnaz / c.Unaz,
          `To je nazivna struja (kad je na žarulji ${withUnit(c.Unaz, "V")}). Ovdje je na žarulji ${withUnit(st.U, "V")}, pa je i struja drukčija.`
        );
      if (v === "U")
        add2(
          c.Unaz,
          "To je nazivni napon, onaj koji piše na žarulji. Stvarni napon na žarulji ovisi o spoju."
        );
    }
    if (s.izvor && list && isNum(Rel) && Rel > 0 && Rel < Infinity) {
      if (v === "I")
        add2(
          emf / Rel,
          `Podijelio si cijeli napon izvora s otporom ${ime2}. Na ${ime2} nije cijeli napon izvora: dio padne na ${ostatak()}. Najprije nađi napon na ${ime2}.`
        );
      if (v === "U")
        add2(
          emf,
          `To je napon cijelog izvora. Na ${ime2} padne samo dio, a ostatak na ${ostatak()}.`
        );
      if (v === "P")
        add2(
          emf * emf / Rel,
          `Računao si snagu s punim naponom izvora. Na ${ime2} je samo dio napona, pa je i snaga manja.`
        );
    }
    if (s.izvor && (el === "izvor" || el === "ukupno") && v === "U" && st0.r > 0)
      add2(
        emf,
        `To je elektromotorni napon ${oz}. Napon stezaljki manji je za pad napona na unutarnjem otporu: $U = ${oz === "ε" ? "\\varepsilon" : oz} - I r$.`
      );
    if (v === "I" && c && uParalelnoj(c))
      add2(
        rez.I,
        `To je struja iz izvora. U čvoru se ona grana, pa kroz ${ime2} teče samo dio. Nađi napon na ${ime2}, pa $I = \\frac{U}{R}$.`
      );
    if (v === "P" && list && uParalelnoj(c))
      add2(
        rez.po[c.key].U * rez.I,
        `Pomnožio si napon sa strujom iz izvora. Za snagu na ${ime2} treba struja baš kroz ${ime2}.`
      );
    if (v === "I" && (el === "izvor" || el === "ukupno")) {
      for (const l of T.listovi)
        if (l.vrsta !== "S")
          add2(
            rez.po[l.key].I,
            `To je struja samo kroz ${naziv2(l)}. Iz izvora izlazi zbroj struja svih grana.`
          );
    }
    if (v === "R") {
      const g = el === "ukupno" ? T.korijen : c;
      if (g?.djeca && g.vrsta === "par")
        add2(
          g.djeca.reduce((x, d) => x + 1 / rez.po[d.key].R, 0),
          "To je $\\frac{1}{R}$, a ne $R$. Na kraju razlomak treba prevrnuti."
        );
    }
    if (s.izvor && st0.r > 0 && el !== "r")
      add2(
        vrijednost2(rijesi(s, { ...st0, r: 0 }, T), v, el),
        `Zaboravio si unutarnji otpor izvora. Struja u krugu je $I = \\frac{${oz === "ε" ? "\\varepsilon" : oz}}{R + r}$: i $r$ je u nazivniku.`
      );
    for (const g of T.grupe) {
      if (g.djeca.length < 2) continue;
      add2(
        vrijednost2(
          rijesi(
            s,
            { ...st0, spoj: { [g.key]: g.vrsta === "ser" ? "par" : "ser" } },
            T
          ),
          v,
          el
        ),
        g.vrsta === "par" ? PAR_KAO_SER : SER_KAO_PAR
      );
    }
    const drugi = [
      ...T.listovi.filter((l) => l.vrsta !== "S" || v === "I" || v === "U").map((l) => l.id),
      ...T.grupe.filter((g) => g.id).map((g) => g.id),
      ...s.izvor ? ["ukupno"] : [],
      ...st0.r > 0 ? ["r"] : []
    ];
    for (const d of drugi)
      if (d !== el)
        add2(
          vrijednost2(rez, v, d),
          `To je ${fraza(T, v, d)}, a traži se ${fraza(T, v, el)}.`
        );
    return out;
  }
  var SAVJET = {
    I: "Nije točno. Kreni od ukupnog otpora kruga, izračunaj struju iz izvora, pa pogledaj kako se dijeli.",
    U: "Nije točno. Napon na dijelu kruga je $U = I R$, sa strujom i otporom baš tog dijela.",
    P: "Nije točno. Snaga je $P = U I$, s naponom i strujom baš tog elementa.",
    R: "Nije točno. Serijski se otpori zbrajaju, a u paralelnom spoju zbrajaju se recipročne vrijednosti."
  };
  var tolPitanja = (q, a) => isNum(q.tolerancija) ? q.tolerancija : Math.max(1e-9, Math.abs(a) * 0.01);
  function provjeriStablo(spoj, need) {
    const ids = /* @__PURE__ */ new Set();
    let listova = 0;
    let ok = true;
    const ne = (uvjet, poruka2) => {
      if (!need(uvjet, poruka2)) ok = false;
      return uvjet;
    };
    const walk = (n, put, dubina) => {
      const v = vrstaCvora(n);
      if (!ne(
        v,
        `${put}: element mora imati točno jedno od polja R, zarulja, sklopka, serijski ili paralelno`
      ))
        return;
      if (n.id != null && ne(
        typeof n.id === "string" && ID6.test(n.id),
        `${put}.id mora biti kratko ime od slova i brojki (npr. "R1")`
      )) {
        ne(
          !POSEBNI.includes(n.id),
          `${put}.id „${n.id}“ je rezerviran (izvor, r, ukupno)`
        );
        ne(!ids.has(n.id), `id „${n.id}“ se ponavlja`);
        ids.add(n.id);
      }
      if (n.oznaka != null)
        ne(
          isText(n.oznaka) && n.oznaka.length <= 10,
          `${put}.oznaka mora biti kratak tekst (najviše 10 znakova, npr. "Ž₁")`
        );
      if (v === "ser" || v === "par") {
        const polje2 = v === "ser" ? "serijski" : "paralelno";
        const arr = n[polje2];
        if (!ne(
          Array.isArray(arr) && arr.length >= 1,
          `${put}.${polje2} mora biti neprazan popis`
        ))
          return;
        ne(
          dubina < 3,
          `${put}: spoj je preduboko ugniježđen (najviše 3 razine skupina)`
        );
        if (dubina > 0)
          ne(arr.length >= 2, `${put}: skupina unutar spoja treba barem 2 člana`);
        if (v === "par") ne(arr.length <= 4, `${put}: najviše 4 paralelne grane`);
        else ne(arr.length <= 5, `${put}: najviše 5 elemenata u nizu`);
        arr.forEach((d, i) => walk(d, `${put}.${polje2}[${i}]`, dubina + 1));
        return;
      }
      listova++;
      ne(n.id != null, `${put}: svaki element treba id (npr. "R1", "Z1", "S1")`);
      if (v === "R")
        ne(
          isNum(n.R) && n.R > 0 && n.R <= 1e6,
          `${put}.R mora biti otpor u Ω (0 < R ≤ 10⁶)`
        );
      if (v === "Z")
        ne(
          isNum(n.zarulja?.U) && n.zarulja.U > 0 && n.zarulja.U <= 1e4 && isNum(n.zarulja?.P) && n.zarulja.P > 0 && n.zarulja.P <= 1e5,
          `${put}.zarulja treba nazivni napon U (V) i nazivnu snagu P (W), oba pozitivna`
        );
      if (v === "S")
        ne(
          ["otvorena", "zatvorena"].includes(n.sklopka),
          `${put}.sklopka mora biti "otvorena" ili "zatvorena"`
        );
    };
    walk(spoj, "spoj", 0);
    if (ok)
      ne(
        listova <= 8,
        `spoj ima ${listova} elemenata; najviše 8 stane na mobitel`
      );
    return ok;
  }
  function refOk(s, T, el) {
    if (typeof el !== "string") return false;
    if (el === "ukupno") return true;
    if (el === "izvor") return !!s.izvor;
    if (el === "r") return !!s.izvor && s.izvor.r != null;
    return !!T.po[el];
  }
  var REF_OPIS = `id elementa ili skupine, "izvor", "ukupno" ili "r"`;
  function provjeriVelicinu(s, T, q, at, need) {
    const ok1 = need(
      VELICINE4.includes(q?.velicina),
      `${at}.velicina mora biti I, U, P ili R`
    );
    const ok2 = need(
      refOk(s, T, q?.element),
      `${at}.element mora biti ${REF_OPIS}`
    );
    if (!ok1 || !ok2) return false;
    if (!s.izvor)
      return need(
        q.velicina === "R",
        `${at}: bez izvora se može pitati samo otpor (R)`
      );
    const c = T.po[q.element];
    if (c?.vrsta === "S")
      return need(
        q.velicina === "I" || q.velicina === "U",
        `${at}: za sklopku ima smisla samo struja ili napon`
      );
    return true;
  }
  function svaStanja(s, T) {
    const st0 = pocetnoStanje(s);
    const rk = (s.klizaci ?? []).find((k) => k.element === "r");
    if (rk) st0.r = rk.min;
    let stanja = [st0];
    if (s.nacin === "istrazi")
      for (const c of T.listovi.filter((l) => l.vrsta === "S"))
        stanja = stanja.flatMap(
          (st) => [true, false].map((z) => ({
            ...st,
            sklopke: { ...st.sklopke, [c.id]: z }
          }))
        );
    if (s.nacin === "odaberi" && T.po[s.odaberi?.sklopka]?.vrsta === "S")
      stanja.push({ ...st0, sklopke: stanjePoslije(s, T).sklopke });
    const g = grupaIzbora(s, T);
    if (g)
      stanja = stanja.flatMap(
        (st) => ["ser", "par"].map((v) => ({ ...st, spoj: { [g.key]: v } }))
      );
    return stanja;
  }
  var strujni_krug = {
    tip: "strujni_krug",
    samoRucno: true,
    uloga: (s) => s.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s.nacin === "istrazi",
    opis: `Istosmjerni strujni krug: shema se crta sama iz stabla spoja, a sve struje, napone i snage računa aplikacija. Struja teče kao točkice (brže kad je veća), žarulje svijetle razmjerno snazi, mjerila pokazuju žive vrijednosti.
Polja: tekst, nacin, izvor?: {emf (V), r? (unutarnji otpor u Ω; tada se izvor crta u isprekidanoj kutiji), oznaka? ("ε" | "U")} (bez izvora shema ima samo stezaljke i pita se samo otpor),
spoj: stablo od elemenata {id, R} (otpornik), {id, zarulja: {U, P}} (nazivni napon i snaga; R = U²/P), {id, sklopka: "otvorena" | "zatvorena"} i skupina {id?, serijski: [...]} | {id?, paralelno: [...]}
(najviše 8 elemenata, 4 grane, 3 razine; svaki element može imati oznaka, npr. "Ž₁"; inače se "R1" piše R₁),
mjerila?: [{vrsta: "A" | "V", element}] (najviše 5; element = id elementa ili skupine, "izvor" (struja izvora / napon stezaljki), "ukupno" (isto, vanjski dio) ili "r" (samo V: pad napona na unutarnjem otporu)).
Veličine (velicina + element): I, U, P, R za element, skupinu, "ukupno" (vanjski dio kruga; R = ukupni otpor), "izvor" (P = εI, R = R + r) ili "r".
nacin:
- "istrazi": korak za gledanje: dodir na sklopku je otvara/zatvara; klizaci?: [{element (id otpornika, "izvor" za ε ili "r"), min, max, korak, oznaka?}] (1–2; početak je vrijednost iz speca);
  spoj_izbor?: true (cijeli spoj) ili id skupine – gumb serijski ⇄ paralelno; presjek?: true – crtkani presjek žice s brojačem naboja q = It i broja elektrona; podaci?: [{velicina, element, oznaka?}] (najviše 3 živa iznosa ispod sheme);
- "izracunaj": pitaj: [{velicina, element, oznaka? (npr. "$I_1 =$"), tolerancija?, krivo?}] (1–3 polja). Mjerila tražene veličine pokazuju „?“. Točno računa aplikacija (1 %), a krive vrijednosti
  (zaboravljen r, zamijenjen serijski/paralelni spoj, ε umjesto napona na dijelu, struja izvora umjesto grane, 1/R, nazivna snaga žarulje, vrijednost drugog elementa) dodaju se same;
- "postavi": učenik klizačem (i po želji spoj_izbor) dovede veličinu na cilj: cilj: {velicina, element, vrijednost, tolerancija? (zadano 1 %), skriveno? (true: živi iznos se ne vidi do provjere)}, klizaci, podaci?;
- "odaberi": učenik dodirne element: odaberi: {kriterij, medju? (id-jevi; zadano žarulje ili svi otpornici)}. kriterij: ${Object.keys(KRITERIJI).join(", ")} (mora biti jedinstven; sjaj = stvarna snaga)
  ili promjena kad se sklopka prebaci: odaberi: {kriterij: ${Object.keys(PROMJENE).join(" | ")}, sklopka: id} (učenik označi sve takve žarulje; nakon točnog odgovora sklopka se prebaci).`,
    primjer: {
      tip: "strujni_krug",
      nacin: "izracunaj",
      tekst: "Kolika struja izlazi iz izvora i kolika teče kroz svaki otpornik?",
      izvor: { emf: 96, r: 4 },
      spoj: {
        paralelno: [
          { id: "R1", R: 20 },
          { id: "R2", R: 30 }
        ]
      },
      mjerila: [
        { vrsta: "A", element: "izvor" },
        { vrsta: "A", element: "R1" },
        { vrsta: "A", element: "R2" }
      ],
      pitaj: [
        { velicina: "I", element: "izvor", oznaka: "$I =$" },
        { velicina: "I", element: "R1", oznaka: "$I_1 =$" },
        { velicina: "I", element: "R2", oznaka: "$I_2 =$" }
      ]
    },
    primjeri: [
      {
        tip: "strujni_krug",
        nacin: "istrazi",
        tekst: "Pomiči klizač otpora $R_2$ i gledaj mjerila: što se događa sa strujom izvora i s naponom na paru kad se $R_2$ smanji?",
        izvor: { emf: 96, r: 4 },
        spoj: {
          id: "P",
          paralelno: [
            { id: "R1", R: 20 },
            { id: "R2", R: 30 }
          ]
        },
        mjerila: [
          { vrsta: "A", element: "izvor" },
          { vrsta: "A", element: "R2" },
          { vrsta: "V", element: "r" },
          { vrsta: "V", element: "P" }
        ],
        klizaci: [{ element: "R2", min: 5, max: 60, korak: 5 }]
      },
      {
        tip: "strujni_krug",
        nacin: "istrazi",
        tekst: "Mijenjaj napon izvora i gledaj koliko naboja prođe kroz presjek žice. Dodirni sklopku da prekineš krug.",
        izvor: { emf: 12 },
        spoj: {
          serijski: [
            { id: "S", sklopka: "zatvorena" },
            { id: "R1", R: 4 }
          ]
        },
        mjerila: [{ vrsta: "A", element: "izvor" }],
        klizaci: [{ element: "izvor", min: 2, max: 24, korak: 1 }],
        presjek: true
      },
      {
        tip: "strujni_krug",
        nacin: "istrazi",
        tekst: "Prebaci žarulje iz serijskog u paralelni spoj i usporedi koliko svijetle.",
        izvor: { emf: 12 },
        spoj: {
          serijski: [
            { id: "Z1", oznaka: "Ž₁", zarulja: { U: 12, P: 9 } },
            { id: "Z2", oznaka: "Ž₂", zarulja: { U: 12, P: 9 } }
          ]
        },
        spoj_izbor: true,
        mjerila: [{ vrsta: "V", element: "Z1" }],
        podaci: [
          { velicina: "P", element: "Z1", oznaka: "snaga $P_1$" },
          { velicina: "R", element: "ukupno", oznaka: "ukupni otpor $R_u$" }
        ]
      },
      {
        tip: "strujni_krug",
        nacin: "postavi",
        tekst: "Otpornik od 60 Ω spojen je s otpornikom nepoznatog otpora $R$ tako da je ukupni otpor 20 Ω. Odaberi spoj i namjesti $R$.",
        spoj: {
          serijski: [
            { id: "R1", R: 60 },
            { id: "R", R: 10 }
          ]
        },
        spoj_izbor: true,
        klizaci: [{ element: "R", min: 5, max: 100, korak: 5 }],
        cilj: {
          velicina: "R",
          element: "ukupno",
          vrijednost: 20,
          oznaka: "$R_u$"
        }
      },
      {
        tip: "strujni_krug",
        nacin: "postavi",
        tekst: "Namjesti napon izvora tako da kroz otpornik teče struja 3 A.",
        izvor: { emf: 6 },
        spoj: { id: "R1", R: 4 },
        mjerila: [
          { vrsta: "A", element: "R1" },
          { vrsta: "V", element: "izvor" }
        ],
        klizaci: [{ element: "izvor", min: 1, max: 24, korak: 1 }],
        cilj: { velicina: "I", element: "R1", vrijednost: 3 }
      },
      {
        tip: "strujni_krug",
        nacin: "izracunaj",
        tekst: "Dvije žarulje s oznakom 12 V, 9 W spojene su serijski na 12 V. Kolika je snaga svake?",
        izvor: { emf: 12 },
        spoj: {
          serijski: [
            { id: "Z1", oznaka: "Ž₁", zarulja: { U: 12, P: 9 } },
            { id: "Z2", oznaka: "Ž₂", zarulja: { U: 12, P: 9 } }
          ]
        },
        pitaj: [{ velicina: "P", element: "Z1", oznaka: "$P =$" }]
      },
      {
        tip: "strujni_krug",
        nacin: "odaberi",
        tekst: "Žarulje 12 V, 9 W i 12 V, 18 W spojene su serijski na 12 V. Dodirni žarulju koja svijetli jače.",
        izvor: { emf: 12 },
        spoj: {
          serijski: [
            { id: "Z1", oznaka: "Ž₁", zarulja: { U: 12, P: 9 } },
            { id: "Z2", oznaka: "Ž₂", zarulja: { U: 12, P: 18 } }
          ]
        },
        odaberi: { kriterij: "najsjajnija" }
      },
      {
        tip: "strujni_krug",
        nacin: "odaberi",
        tekst: "Sve tri žarulje su jednake. Koje žarulje svijetle jače kad se sklopka S otvori? Dodirni sve takve.",
        izvor: { emf: 6 },
        spoj: {
          serijski: [
            { id: "Z1", oznaka: "Ž₁", zarulja: { U: 6, P: 3 } },
            {
              paralelno: [
                { id: "Z2", oznaka: "Ž₂", zarulja: { U: 6, P: 3 } },
                {
                  serijski: [
                    { id: "S", sklopka: "zatvorena" },
                    { id: "Z3", oznaka: "Ž₃", zarulja: { U: 6, P: 3 } }
                  ]
                }
              ]
            }
          ]
        },
        odaberi: { kriterij: "jace", sklopka: "S" }
      },
      {
        tip: "strujni_krug",
        nacin: "istrazi",
        tekst: "Dodirni sklopku S i gledaj kako se mijenja sjaj svake žarulje.",
        izvor: { emf: 6, r: 1 },
        spoj: {
          serijski: [
            { id: "Z1", oznaka: "Ž₁", zarulja: { U: 6, P: 3 } },
            {
              paralelno: [
                { id: "Z2", oznaka: "Ž₂", zarulja: { U: 6, P: 3 } },
                {
                  serijski: [
                    { id: "S", sklopka: "zatvorena" },
                    { id: "Z3", oznaka: "Ž₃", zarulja: { U: 6, P: 3 } }
                  ]
                }
              ]
            }
          ]
        },
        mjerila: [{ vrsta: "A", element: "izvor" }]
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(
        NACINI13.includes(s.nacin),
        `strujni_krug.nacin mora biti jedan od: ${NACINI13.join(", ")}`
      );
      need(isText(s.tekst), "strujni_krug treba „tekst“");
      if (s.izvor != null) {
        if (need(typeof s.izvor === "object", "izvor mora biti {emf, r?}")) {
          need(
            isNum(s.izvor.emf) && s.izvor.emf > 0 && s.izvor.emf <= 1e5,
            "izvor.emf mora biti pozitivan napon u V"
          );
          if (s.izvor.r != null)
            need(
              isNum(s.izvor.r) && s.izvor.r >= 0 && s.izvor.r <= 1e4,
              "izvor.r mora biti unutarnji otpor u Ω (≥ 0)"
            );
          if (s.izvor.oznaka != null)
            need(
              ["ε", "U"].includes(s.izvor.oznaka),
              `izvor.oznaka mora biti "ε" ili "U"`
            );
        }
      }
      if (!need(
        s.spoj && typeof s.spoj === "object",
        "strujni_krug treba „spoj“ (stablo elemenata)"
      ))
        return list;
      if (!provjeriStablo(s.spoj, need) || list.length) return list;
      const T = stablo(s.spoj);
      const nacin = s.nacin;
      if (s.mjerila != null && need(
        Array.isArray(s.mjerila) && s.mjerila.length <= 5,
        "mjerila: popis od najviše 5 mjerila"
      )) {
        need(
          !!s.izvor || !s.mjerila.length,
          "mjerila imaju smisla samo uz izvor"
        );
        const kljucevi = /* @__PURE__ */ new Set();
        s.mjerila.forEach((m, i) => {
          need(
            ["A", "V"].includes(m?.vrsta),
            `mjerila[${i}].vrsta mora biti "A" (ampermetar) ili "V" (voltmetar)`
          );
          need(
            refOk(s, T, m?.element),
            `mjerila[${i}].element mora biti ${REF_OPIS}`
          );
          if (m?.vrsta === "A")
            need(
              m.element !== "r",
              `mjerila[${i}]: struju izvora mjeri ampermetar na "izvor"`
            );
          if (m?.element === "r")
            need(
              rUnutra(s),
              `mjerila[${i}]: voltmetar na "r" treba izvor s unutarnjim otporom r > 0`
            );
          const k = `${m?.vrsta}:${m?.element === "ukupno" ? "izvor" : m?.element}`;
          need(!kljucevi.has(k), `mjerila[${i}] se ponavlja`);
          kljucevi.add(k);
        });
        need(
          !(kljucevi.has("V:r") && kljucevi.has("V:izvor")),
          `najviše jedan voltmetar uz izvor: na "r" ili na "izvor"`
        );
      }
      if (s.klizaci != null) {
        need(
          nacin === "istrazi" || nacin === "postavi",
          "klizaci postoje samo u načinima istrazi i postavi"
        );
        if (need(
          Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 2,
          "klizaci: 1–2 klizača"
        )) {
          s.klizaci.forEach((k, i) => {
            const el = k?.element;
            const c = T.po[el];
            need(
              el === "izvor" || el === "r" ? !!s.izvor : c?.vrsta === "R",
              `klizaci[${i}].element mora biti id otpornika, "izvor" (ε) ili "r"`
            );
            if (k?.oznaka != null)
              need(
                isText(k.oznaka) && k.oznaka.length <= 10,
                `klizaci[${i}].oznaka mora biti kratak tekst`
              );
            if (!need(
              [k?.min, k?.max, k?.korak].every(isNum),
              `klizaci[${i}] treba brojeve min, max i korak`
            ))
              return;
            need(
              k.max > k.min && k.korak > 0,
              `klizaci[${i}]: mora biti min < max i korak > 0`
            );
            need(
              (k.max - k.min) / k.korak <= 1e3,
              `klizaci[${i}] ima previše koraka (najviše 1000)`
            );
            need(
              el === "r" ? k.min >= 0 : k.min > 0,
              `klizaci[${i}].min mora biti pozitivan`
            );
            const p = pocetnaVrijednost(s, T, el);
            if (isNum(p)) {
              need(
                p >= k.min - 1e-9 && p <= k.max + 1e-9,
                `klizaci[${i}]: početna vrijednost iz speca (${p}) mora biti između min i max`
              );
              need(
                aligned3(p - k.min, k.korak),
                `klizaci[${i}]: početna vrijednost iz speca (${p}) mora biti na koraku klizača`
              );
            }
          });
          need(
            new Set(s.klizaci.map((k) => k?.element)).size === s.klizaci.length,
            "dva klizača za isti element"
          );
        }
      }
      if (s.spoj_izbor != null) {
        need(
          nacin === "istrazi" || nacin === "postavi",
          "spoj_izbor postoji samo u načinima istrazi i postavi"
        );
        need(
          grupaIzbora(s, T)?.djeca.length >= 2,
          "spoj_izbor mora biti true (cijeli spoj je skupina) ili id skupine s barem 2 člana"
        );
      }
      if (s.presjek != null)
        need(
          s.presjek === true && nacin === "istrazi" && !!s.izvor,
          "presjek: true postoji samo u načinu istrazi, uz izvor"
        );
      if (s.podaci != null) {
        need(
          nacin === "istrazi" || nacin === "postavi",
          "podaci (živi iznosi) postoje samo u načinima istrazi i postavi"
        );
        if (need(
          Array.isArray(s.podaci) && s.podaci.length >= 1 && s.podaci.length <= 3,
          "podaci: 1–3 iznosa"
        ))
          s.podaci.forEach(
            (q, i) => provjeriVelicinu(s, T, q, `podaci[${i}]`, need)
          );
      }
      if (list.length) return list;
      if (s.izvor) {
        for (const st of svaStanja(s, T))
          if (!need(
            rijesi(s, st, T).Rv + st.r > 1e-9,
            "kratki spoj: u nekom stanju sklopki krug nema nikakav otpor (dodaj unutarnji otpor r ili otpornik u niz)"
          ))
            break;
      }
      if (nacin === "istrazi") {
        noAnswerFields(s, need);
        for (const f2 of ["pitaj", "odaberi"])
          need(
            !(f2 in s),
            `korak „istrazi“ je za gledanje, pa ne smije imati polje „${f2}“`
          );
      }
      if (nacin === "izracunaj") {
        for (const f2 of [
          "klizaci",
          "spoj_izbor",
          "presjek",
          "podaci",
          "cilj",
          "odaberi"
        ])
          need(!(f2 in s), `izracunaj je nepomičan krug, bez polja „${f2}“`);
        if (need(
          Array.isArray(s.pitaj) && s.pitaj.length >= 1 && s.pitaj.length <= 3,
          "izracunaj treba pitaj: 1–3 tražene veličine"
        )) {
          const rez = rijesi(s, pocetnoStanje(s), T);
          const vidjeno = /* @__PURE__ */ new Set();
          s.pitaj.forEach((q, i) => {
            if (!provjeriVelicinu(s, T, q, `pitaj[${i}]`, need)) return;
            const c = T.po[q.element];
            if (c?.vrsta === "R")
              need(
                q.velicina !== "R",
                `pitaj[${i}]: otpor ${naziv2(c)} piše na shemi; pitaj ukupni otpor ili otpor žarulje`
              );
            const a = vrijednost2(rez, q.velicina, q.element);
            need(
              isNum(a),
              `pitaj[${i}]: tražena vrijednost nije konačan broj (npr. otpor prekinutog kruga)`
            );
            if (q.oznaka != null)
              need(isText(q.oznaka), `pitaj[${i}].oznaka mora biti tekst`);
            if (q.tolerancija != null)
              need(
                isNum(q.tolerancija) && q.tolerancija >= 0,
                `pitaj[${i}].tolerancija mora biti broj ≥ 0`
              );
            if (isNum(a))
              validateKrivo(
                q.krivo,
                a,
                tolPitanja(q, a),
                need,
                `pitaj[${i}].krivo`
              );
            const k = `${q.velicina}:${q.element}`;
            need(!vidjeno.has(k), `pitaj[${i}] se ponavlja`);
            vidjeno.add(k);
          });
        }
      }
      if (nacin === "postavi") {
        for (const f2 of ["pitaj", "odaberi", "presjek"])
          need(!(f2 in s), `postavi ne smije imati polje „${f2}“`);
        need(
          (s.klizaci?.length ?? 0) > 0 || s.spoj_izbor != null,
          "postavi treba barem jedan klizač ili spoj_izbor"
        );
        const c = s.cilj;
        if (need(
          c && typeof c === "object",
          "postavi treba cilj: {velicina, element, vrijednost}"
        ) && provjeriVelicinu(s, T, c, "cilj", need)) {
          need(isNum(c.vrijednost), "cilj.vrijednost mora biti broj");
          if (c.tolerancija != null)
            need(
              isNum(c.tolerancija) && c.tolerancija >= 0,
              "cilj.tolerancija mora biti broj ≥ 0"
            );
          if (c.skriveno != null)
            need(
              typeof c.skriveno === "boolean",
              "cilj.skriveno mora biti true ili false"
            );
          if (c.oznaka != null)
            need(isText(c.oznaka), "cilj.oznaka mora biti tekst");
          const n = (s.klizaci ?? []).reduce(
            (p, k) => p * (Math.round((k.max - k.min) / k.korak) + 1),
            1
          );
          if (need(
            n <= 4e4,
            "klizači zajedno imaju previše položaja (najviše 40 000)"
          ) && isNum(c.vrijednost) && !list.length) {
            const { rjesenja, najblize, imaKrivih } = pretrazi(s, T);
            need(
              rjesenja.length > 0,
              `cilj se ne može postići ni na jednom položaju klizača${najblize ? ` (najbliže je ${withUnit(najblize.v, JEDINICA[c.velicina])})` : ""}`
            );
            need(
              imaKrivih,
              "cilj je ispunjen na svakom položaju: nema što namjestiti"
            );
          }
        }
      }
      if (nacin === "odaberi") {
        for (const f2 of [
          "pitaj",
          "cilj",
          "klizaci",
          "spoj_izbor",
          "presjek",
          "podaci"
        ])
          need(!(f2 in s), `odaberi ne smije imati polje „${f2}“`);
        const o = s.odaberi;
        if (need(
          o && typeof o === "object",
          "odaberi treba odaberi: {kriterij, medju?, sklopka?}"
        ) && need(
          o.kriterij in KRITERIJI || o.kriterij in PROMJENE,
          `odaberi.kriterij mora biti jedan od: ${[...Object.keys(KRITERIJI), ...Object.keys(PROMJENE)].join(", ")}`
        )) {
          need(!!s.izvor, "odaberi treba izvor");
          const promjena3 = o.kriterij in PROMJENE;
          if (promjena3)
            need(
              T.po[o.sklopka]?.vrsta === "S",
              "odaberi.sklopka mora biti id sklopke koja se prebacuje"
            );
          else
            need(
              o.sklopka == null,
              "odaberi.sklopka ide samo uz kriterije promjene (ugase_se, upale_se, jace, slabije, ne_mijenja)"
            );
          if (o.medju != null)
            need(
              Array.isArray(o.medju) && o.medju.every((id) => ["R", "Z"].includes(T.po[id]?.vrsta)) && new Set(o.medju).size === o.medju.length,
              "odaberi.medju mora biti popis različitih id-jeva otpornika i žarulja"
            );
          const ids = kandidati(s, T);
          need(
            ids.length >= 2,
            "odaberi treba barem 2 elementa između kojih učenik bira"
          );
          if ((KRITERIJI[o.kriterij]?.[2] || promjena3) && !o.medju)
            need(
              ids.every((id) => T.po[id]?.vrsta === "Z"),
              "za sjaj i promjenu sjaja kandidati moraju biti žarulje"
            );
          if (!list.length) {
            const { tocni } = rjesenjeOdaberi(s, T);
            if (promjena3)
              need(
                tocni.length > 0,
                `nijedna žarulja ne ${PROMJENE[o.kriterij].replace(/^se /, "")} kad se sklopka prebaci; promijeni kriterij`
              );
            else
              need(
                tocni.length === 1,
                `kriterij „${o.kriterij}“ nema jedinstven odgovor (${tocni.length} elementa su jednaka)`
              );
          }
        }
      }
      if (list.length) return list;
      const oz = oznakeZaRaspored(s);
      for (const st of svaStanja(s, T).slice(0, 4)) {
        const L = rasporedi(s, st, T, oz);
        if (!need(
          L.w <= MAX_W && L.h <= MAX_H,
          `shema je prevelika za mobitel (${L.w}×${L.h}, najviše ${MAX_W}×${MAX_H}); smanji broj elemenata ili mjerila`
        ))
          break;
      }
      return list;
    },
    check(s, odgovor3) {
      const T = stablo(s.spoj);
      if (s.nacin === "izracunaj") {
        const unosi = Array.isArray(odgovor3) ? odgovor3 : [odgovor3];
        const rez = rijesi(s, pocetnoStanje(s), T);
        const vise = s.pitaj.length > 1;
        const polja = [];
        let prva = null;
        for (const [i, q] of s.pitaj.entries()) {
          const a = vrijednost2(rez, q.velicina, q.element);
          const r = checkNumber({
            input: unosi[i] ?? "",
            answer: a,
            tol: tolPitanja(q, a),
            unit: JEDINICA[q.velicina],
            krivo: [...q.krivo ?? [], ...autoKrivo(s, T, q)],
            savjet: s.savjet || SAVJET[q.velicina]
          });
          if (r.prazno)
            return {
              tocno: false,
              poruka: vise ? `${oznakaPitanja(s, T, q)} upiši broj (decimalni zarez je u redu).` : r.poruka,
              prazno: true
            };
          polja.push(!!r.tocno);
          if (!r.tocno && !prva) prva = { ...r, q };
        }
        if (!prva) return { tocno: true, polja };
        return {
          tocno: false,
          poruka: vise ? `${oznakaPitanja(s, T, prva.q)} ${prva.poruka}` : prva.poruka,
          polja
        };
      }
      if (s.nacin === "postavi") {
        if (!odgovor3 || typeof odgovor3 !== "object")
          return { tocno: false, poruka: "Pomakni klizač.", prazno: true };
        const c = s.cilj;
        const v = ciljU(s, T, odgovor3);
        const j = JEDINICA[c.velicina];
        if (isNum(v) && near(v, c.vrijednost, tolCilja(s)))
          return { tocno: true };
        const opis = c.oznaka ?? fraza(T, c.velicina, c.element);
        let poruka2 = `Sad je ${opis} = ${isNum(v) ? withUnit(v, j) : "?"}, a treba ${withUnit(c.vrijednost, j)}.`;
        const g = grupaIzbora(s, T);
        const spoj = odgovor3.spoj ?? g?.vrsta;
        if (g && !pretrazi(s, T, spoj).rjesenja.length) {
          poruka2 += c.velicina === "R" ? " S ovim spojem to ne ide: serijski spoj uvijek daje više od najvećeg otpornika, a paralelni manje od najmanjeg." : " S ovim spojem cilj se ne može postići. Probaj drugi spoj.";
        } else if (isNum(v))
          poruka2 += v > c.vrijednost ? " Treba manje." : " Treba više.";
        return { tocno: false, poruka: poruka2 };
      }
      if (s.nacin === "odaberi") {
        const odabrano = (Array.isArray(odgovor3) ? odgovor3 : odgovor3 ? [odgovor3] : []).filter((x) => typeof x === "string");
        const promjena3 = s.odaberi.kriterij in PROMJENE;
        if (!odabrano.length)
          return {
            tocno: false,
            poruka: promjena3 ? "Dodirni žarulje za koje to vrijedi." : "Dodirni jedan element.",
            prazno: true
          };
        const r = rjesenjeOdaberi(s, T);
        const ime2 = (id2) => naziv2(T.po[id2]);
        const visak = odabrano.filter((id2) => !r.tocni.includes(id2));
        const fali = r.tocni.filter((id2) => !odabrano.includes(id2));
        if (!visak.length && !fali.length) return { tocno: true };
        if (promjena3) {
          if (visak.length) {
            const i2 = r.ids.indexOf(visak[0]);
            const sk = T.po[s.odaberi.sklopka];
            return {
              tocno: false,
              poruka: `${ime2(visak[0])} ${OPIS_PROMJENE2[r.opis[i2]] ?? "se mijenja drukčije"} kad se sklopka ${naziv2(sk)} ${sk.zatvorena ? "otvori" : "zatvori"}. Prati put struje: kroz koje žarulje struja ide i kroz sklopku?`
            };
          }
          return {
            tocno: false,
            poruka: `Nisi označio sve: ${fali.length === 1 ? "još jedna žarulja" : `još ${fali.length} žarulje`} ${PROMJENE[s.odaberi.kriterij]}. Pomisli kako se promijeni ukupni otpor, a time i struja iz izvora.`
          };
        }
        const id = odabrano[0];
        const i = r.ids.indexOf(id);
        const vel = r.vel;
        const k = s.odaberi.kriterij;
        const c = T.po[id];
        const t = r.tocni[0] ? T.po[r.tocni[0]] : null;
        const iznos4 = isNum(r.vr[i]) ? withUnit(r.vr[i], JEDINICA[vel]) : "";
        if (KRITERIJI[k][2]) {
          let poruka2 = `${ime2(id)} troši ${iznos4}, a jedna druga žarulja ${KRITERIJI[k][1] > 0 ? "više" : "manje"}. Sjaj ovisi o snazi koju žarulja stvarno troši ($P = U I$), a ne o tome što piše na njoj.`;
          if (c?.vrsta === "Z" && t?.vrsta === "Z" && (c.Pnaz - t.Pnaz) * KRITERIJI[k][1] > 0)
            poruka2 += " U serijskom spoju kroz obje teče ista struja, pa jače svijetli ona s većim otporom ($P = I^2 R$), a to je žarulja manje nazivne snage.";
          return { tocno: false, poruka: poruka2 };
        }
        const rijec = { I: "Kroz", U: "Na", P: "Na" }[vel];
        return {
          tocno: false,
          poruka: `${rijec} ${ime2(id)} je ${{ I: "struja", U: "napon", P: "snaga" }[vel]} ${iznos4}, a na jednom drugom elementu ${KRITERIJI[k][1] > 0 ? "veća" : "manja"}.`
        };
      }
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      const T = stablo(s.spoj);
      const rez = rijesi(s, pocetnoStanje(s), T);
      if (s.nacin === "izracunaj")
        return s.pitaj.map(
          (q) => `${oznakaPitanja(s, T, q)} ${withUnit(vrijednost2(rez, q.velicina, q.element), JEDINICA[q.velicina])}`
        ).join(", ");
      if (s.nacin === "postavi") {
        const best = pretrazi(s, T).rjesenja[0];
        if (!best) return "";
        const dijelovi2 = [];
        if (best.odg.spoj)
          dijelovi2.push(
            best.odg.spoj === "par" ? "paralelni spoj" : "serijski spoj"
          );
        for (const k of s.klizaci ?? [])
          dijelovi2.push(
            `${nazivKlizaca(s, T, k)} = ${withUnit(best.odg.klizaci[k.element], jedinicaKlizaca(k))}`
          );
        return `${dijelovi2.join(", ")} (tada je ${s.cilj.oznaka ?? fraza(T, s.cilj.velicina, s.cilj.element)} = ${withUnit(best.v, JEDINICA[s.cilj.velicina])})`;
      }
      if (s.nacin === "odaberi")
        return rjesenjeOdaberi(s, T).tocni.map((id) => naziv2(T.po[id])).join(", ");
      return "";
    },
    brojcaniOdgovor(s) {
      const T = stablo(s.spoj);
      if (s.nacin === "izracunaj" && s.pitaj.length === 1)
        return vrijednost2(
          rijesi(s, pocetnoStanje(s), T),
          s.pitaj[0].velicina,
          s.pitaj[0].element
        );
      if (s.nacin === "postavi") {
        const best = pretrazi(s, T).rjesenja[0];
        return (s.klizaci ?? []).length === 1 && best ? best.odg.klizaci[s.klizaci[0].element] : s.cilj.vrijednost;
      }
      return null;
    }
  };

  // src/lib/lesson/katalog/fizika/_magnetizam.js
  var MU0 = 4 * Math.PI * 1e-7;
  var E = 16e-20;
  var ME = 911e-33;
  var MP = 167e-29;
  var SMJEROVI4 = {
    gore: [0, 1, 0],
    dolje: [0, -1, 0],
    lijevo: [-1, 0, 0],
    desno: [1, 0, 0],
    iz_papira: [0, 0, 1],
    u_papir: [0, 0, -1]
  };
  var SMJER_IMENA = Object.keys(SMJEROVI4);
  var U_RAVNINI = ["gore", "dolje", "lijevo", "desno"];
  var OKOMITO = ["iz_papira", "u_papir"];
  var NULA = "nula";
  var KRUZENJE = ["kazaljka", "suprotno"];
  var NEMA = "nema";
  var ZNAK = {
    gore: "↑",
    dolje: "↓",
    lijevo: "←",
    desno: "→",
    iz_papira: "⊙",
    u_papir: "⊗",
    nula: "0",
    kazaljka: "↻",
    suprotno: "↺",
    nema: "–"
  };
  var RIJEC = {
    gore: "gore",
    dolje: "dolje",
    lijevo: "lijevo",
    desno: "desno",
    iz_papira: "iz papira",
    u_papir: "u papir",
    nula: "nula",
    kazaljka: "u smjeru kazaljke na satu",
    suprotno: "suprotno od kazaljke na satu",
    nema: "ne teče"
  };
  var naziv3 = (smjer) => `${RIJEC[smjer] ?? smjer} ${ZNAK[smjer] ?? ""}`.trim();
  var isSmjer = (s) => SMJER_IMENA.includes(s);
  var suprotan = (s) => ({
    gore: "dolje",
    dolje: "gore",
    lijevo: "desno",
    desno: "lijevo",
    iz_papira: "u_papir",
    u_papir: "iz_papira",
    kazaljka: "suprotno",
    suprotno: "kazaljka"
  })[s] ?? s;
  var add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
  var sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
  var mul = (a, k) => [a[0] * k, a[1] * k, a[2] * k];
  var dot2 = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  var cross = (a, b) => [
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0]
  ];
  var len = (a) => Math.hypot(a[0], a[1], a[2]);
  var vec = (smjer) => SMJEROVI4[smjer];
  var v3 = (p) => [p[0], p[1], p[2] ?? 0];
  function smjerIz(v, mjera = 1, tolKut2 = 1e-6) {
    const n = len(v);
    if (n <= Math.max(1e-12, mjera * 1e-9)) return NULA;
    for (const ime2 of SMJER_IMENA)
      if (dot2(v, SMJEROVI4[ime2]) / n > Math.cos(tolKut2)) return ime2;
    return null;
  }
  var lorentz = (naboj, v, B) => mul(cross(vec(v), vec(B)), naboj === "-" ? -1 : 1);
  var silaVodic = (I, B) => cross(vec(I), vec(B));
  function rijesiFaktor(mjesto, poznat, F, k = 1) {
    return SMJER_IMENA.filter(
      (x) => smjerIz(
        mul(
          mjesto === 0 ? cross(vec(x), vec(poznat)) : cross(vec(poznat), vec(x)),
          k
        )
      ) === F
    );
  }
  function poljeVodica(vodic, tocka3, m = 0.01) {
    const I = vec(vodic.I);
    const r = sub(v3(tocka3), v3(vodic.polozaj));
    const rp = sub(r, mul(I, dot2(r, I)));
    const d = len(rp);
    if (d < 1e-9) return null;
    const B = MU0 * (vodic.jakost ?? 1) / (2 * Math.PI * d * m);
    return mul(cross(I, mul(rp, 1 / d)), B);
  }
  function poljeVodica_zbroj(vodici, tocka3, m = 0.01) {
    let B = [0, 0, 0];
    for (const w of vodici) {
      const b = poljeVodica(w, tocka3, m);
      if (!b) return null;
      B = add(B, b);
    }
    return B;
  }
  var poljePetlje = (struja) => struja === "kazaljka" ? "u_papir" : "iz_papira";
  var strujaZaPolje = (B) => B === "u_papir" ? "kazaljka" : "suprotno";
  var poljeZavojnice = (struja) => struja === "gore" ? "lijevo" : "desno";
  var strujaZavojnice = (B) => B === "lijevo" ? "gore" : "dolje";
  var DEBLJINA = 0.32;
  function polovi(N = "desno") {
    const os = vec(N);
    return { n: [os[0], os[1], 0], s: [-os[0], -os[1], 0], os };
  }
  var unutarMagneta = (p, N = "desno") => {
    const { os } = polovi(N);
    const uz2 = Math.abs(p[0] * os[0] + p[1] * os[1]);
    const preko = Math.abs(p[0] * os[1] - p[1] * os[0]);
    return uz2 < 1 && preko < DEBLJINA;
  };
  function poljeMagneta(p, N = "desno") {
    const { n, s, os } = polovi(N);
    if (unutarMagneta(p, N)) return [os[0] * 3, os[1] * 3, 0];
    const t = v3(p);
    const a = sub(t, n);
    const b = sub(t, s);
    const la = len(a);
    const lb = len(b);
    if (la < 1e-9 || lb < 1e-9) return null;
    return sub(mul(a, 1 / la ** 3), mul(b, 1 / lb ** 3));
  }
  function lenzMagnet({ pol, polozaj, gibanje: gibanje2 }) {
    const premaDolje = pol === "N" === (polozaj === "iznad");
    const Bkroz = premaDolje ? "dolje" : "gore";
    if (gibanje2 === "miruje")
      return { Bkroz, tok: "stalan", inducirano: null, struja: NEMA };
    const tok = gibanje2 === "priblizava" ? "raste" : "pada";
    const inducirano = tok === "raste" ? suprotan(Bkroz) : Bkroz;
    return {
      Bkroz,
      tok,
      inducirano,
      struja: inducirano === "gore" ? "suprotno" : "kazaljka"
    };
  }
  var FAZE_PETLJE = ["izvan", "ulazi", "unutra", "izlazi"];
  function lenzPetlja({ B, faza }) {
    if (faza === "unutra" || faza === "izvan")
      return {
        tok: faza === "unutra" ? "stalan" : "nula",
        inducirano: null,
        struja: NEMA
      };
    const tok = faza === "ulazi" ? "raste" : "pada";
    const inducirano = tok === "raste" ? suprotan(B) : B;
    return { tok, inducirano, struja: strujaZaPolje(inducirano) };
  }
  function checkFiz({ input, answer, tol: tol2, unit, krivo = [], savjet: savjet3 }) {
    const n = parseNumber(input, unit);
    if (n == null)
      return checkNumber({ input, answer, tol: tol2, unit, krivo, savjet: savjet3 });
    const ref = Math.abs(answer) || Math.max(1e-300, ...krivo.map((k2) => Math.abs(k2.vrijednost)));
    const k = 1 / ref;
    return checkNumber({
      input: n * k,
      answer: answer * k,
      tol: tol2 == null ? void 0 : tol2 * k,
      krivo: krivo.map((x) => ({
        ...x,
        vrijednost: x.vrijednost * k,
        tolerancija: x.tolerancija == null ? void 0 : x.tolerancija * k
      })),
      savjet: savjet3
    });
  }

  // src/lib/lesson/katalog/fizika/smjer_u_polju.js
  var SCENE4 = {
    naboj: { trazi: ["F", "v", "B"], polja: ["naboj", "ime", "v", "B", "F"] },
    vodic_u_polju: { trazi: ["F", "B", "I"], polja: ["I", "B", "F"] },
    polje_vodica: {
      trazi: ["B"],
      polja: [
        "vodici",
        "tocka",
        "sonda",
        "jedinica_duljine",
        "prikazi_doprinose"
      ]
    },
    dva_vodica: { trazi: ["F2", "F1", "B2", "B1"], polja: ["I1", "I2"] },
    petlja_struja: { trazi: ["B", "struja"], polja: ["struja", "B"] },
    zavojnica: {
      trazi: ["B", "struja"],
      polja: ["struja", "B", "N", "jakost", "duljina", "polumjer"]
    },
    magnet: { trazi: ["B"], polja: ["N", "tocka", "sonda"] },
    lenz_magnet: { trazi: ["struja"], polja: ["pol", "polozaj", "gibanje"] },
    lenz_petlja: { trazi: ["struja"], polja: ["B", "faza", "v"] }
  };
  var OPCE = ["scena", "oznaka", "poruke", "trazi", "mijenja"];
  var SPEC_POLJA = ["tip", "nacin", "tekst", "pitanja"];
  var pitanjaOf = (s) => Array.isArray(s?.pitanja) ? s.pitanja : s?.scena ? [s] : [];
  var traziOf = (q) => q.trazi ?? SCENE4[q.scena]?.trazi[0];
  var nudiNulu = (q) => ["naboj", "vodic_u_polju", "polje_vodica"].includes(q.scena) && ["F", "B"].includes(traziOf(q));
  var DVA = [-1, 1];
  var vodiciDva = (q) => [
    { polozaj: [DVA[0], 0], I: q.I1 },
    { polozaj: [DVA[1], 0], I: q.I2 }
  ];
  var metara = (q) => ({ cm: 0.01, mm: 1e-3, m: 1 })[q.jedinica_duljine ?? "cm"];
  function rjesenje(q) {
    const t = traziOf(q);
    switch (q.scena) {
      case "naboj": {
        if (t === "F") return smjerIz(lorentz(q.naboj, q.v, q.B));
        const sol = rijesiFaktor(
          t === "v" ? 0 : 1,
          t === "v" ? q.B : q.v,
          q.F,
          q.naboj === "-" ? -1 : 1
        );
        return sol.length === 1 ? sol[0] : null;
      }
      case "vodic_u_polju": {
        if (t === "F") return smjerIz(silaVodic(q.I, q.B));
        const sol = rijesiFaktor(t === "I" ? 0 : 1, t === "I" ? q.B : q.I, q.F);
        return sol.length === 1 ? sol[0] : null;
      }
      case "polje_vodica": {
        if (!tocka2(q.tocka) || !Array.isArray(q.vodici)) return null;
        const B = poljeVodica_zbroj(q.vodici, q.tocka, metara(q));
        if (!B) return null;
        const jedan = Math.max(
          ...q.vodici.map((w) => len(poljeVodica(w, q.tocka, metara(q))))
        );
        return smjerIz(B, jedan);
      }
      case "dva_vodica": {
        const w = vodiciDva(q);
        const [na, od] = t.endsWith("2") ? [1, 0] : [0, 1];
        const B = poljeVodica(w[od], w[na].polozaj, 1);
        if (!B) return null;
        return t.startsWith("B") ? smjerIz(B, len(B)) : smjerIz(cross(vec(w[na].I), B), len(B));
      }
      case "petlja_struja":
        return t === "B" ? poljePetlje(q.struja) : strujaZaPolje(q.B);
      case "zavojnica":
        return t === "B" ? poljeZavojnice(q.struja) : strujaZavojnice(q.B);
      case "magnet": {
        if (!tocka2(q.tocka)) return null;
        const B = poljeMagneta(q.tocka, q.N);
        return B ? smjerIz(B, len(B), 1.5 * Math.PI / 180) : null;
      }
      case "lenz_magnet":
        return lenzMagnet(q).struja;
      case "lenz_petlja":
        return lenzPetlja(q).struja;
    }
    return null;
  }
  var tocka2 = (p) => Array.isArray(p) && p.length === 2 && p.every(isNum);
  function validatePitanje(q, at, need, istrazi, list) {
    const pred = list.length;
    if (!need(q && typeof q === "object", `${at} mora biti objekt`)) return;
    const sc = SCENE4[q.scena];
    if (!need(
      sc,
      `${at}.scena mora biti jedna od: ${Object.keys(SCENE4).join(", ")}`
    ))
      return;
    const dopusteno = [...OPCE, ...sc.polja];
    for (const k of Object.keys(q))
      if (!SPEC_POLJA.includes(k))
        need(
          dopusteno.includes(k),
          `${at}: scena „${q.scena}“ nema polje „${k}“`
        );
    if (q.oznaka != null) need(isText(q.oznaka), `${at}.oznaka mora biti tekst`);
    const t = traziOf(q);
    if (!istrazi)
      need(
        sc.trazi.includes(t),
        `${at}.trazi mora biti jedno od: ${sc.trazi.join(", ")}`
      );
    else
      need(
        q.trazi == null || sc.trazi.includes(q.trazi),
        `${at}.trazi mora biti jedno od: ${sc.trazi.join(", ")}`
      );
    const smjer = (k, popis = SMJER_IMENA) => need(
      popis.includes(q[k]),
      `${at}.${k} mora biti jedan od smjerova: ${popis.join(", ")}`
    );
    const nezadano = (k) => need(q[k] == null, `${at}: ${k} se traži, pa ne smije biti zadan`);
    const trojka = (a) => {
      if (istrazi || t === "F") {
        smjer(a);
        smjer("B");
        need(q.F == null, `${at}: sila se računa sama, pa F ne smije biti zadan`);
        return;
      }
      nezadano(t);
      for (const k of [a, "B", "F"].filter((k2) => k2 !== t)) smjer(k);
    };
    switch (q.scena) {
      case "naboj":
        need(["+", "-"].includes(q.naboj), `${at}.naboj mora biti "+" ili "-"`);
        if (q.ime != null)
          need(
            isText(q.ime) && q.ime.length <= 14,
            `${at}.ime je kratki naziv čestice (npr. "proton")`
          );
        trojka("v");
        break;
      case "vodic_u_polju":
        trojka("I");
        break;
      case "polje_vodica": {
        const ok = need(
          Array.isArray(q.vodici) && q.vodici.length >= 1 && q.vodici.length <= 3,
          `${at}.vodici: 1–3 vodiča [{polozaj: [x, y], I, jakost?}]`
        );
        if (ok)
          q.vodici.forEach((w, i) => {
            need(
              tocka2(w?.polozaj),
              `${at}.vodici[${i}].polozaj mora biti [x, y]`
            );
            need(
              isSmjer(w?.I),
              `${at}.vodici[${i}].I mora biti smjer struje (${SMJER_IMENA.join(", ")})`
            );
            if (w?.jakost != null)
              need(
                isNum(w.jakost) && w.jakost > 0 && w.jakost <= 1e3,
                `${at}.vodici[${i}].jakost je struja u A (pozitivan broj; smjer je u I)`
              );
            for (const k of Object.keys(w ?? {}))
              need(
                ["polozaj", "I", "jakost", "oznaka"].includes(k),
                `${at}.vodici[${i}] nema polje „${k}“`
              );
          });
        if (q.jedinica_duljine != null)
          need(
            ["mm", "cm", "m"].includes(q.jedinica_duljine),
            `${at}.jedinica_duljine mora biti mm, cm ili m`
          );
        if (q.prikazi_doprinose != null)
          need(
            typeof q.prikazi_doprinose === "boolean",
            `${at}.prikazi_doprinose je true ili false`
          );
        if (istrazi) {
          need(
            q.tocka == null,
            `${at}: u istraživanju nema točke s odgovorom; početni položaj sonde je „sonda“`
          );
          if (q.sonda != null)
            need(tocka2(q.sonda), `${at}.sonda mora biti [x, y]`);
          if (ok)
            need(
              q.vodici.every((w) => OKOMITO.includes(w?.I)),
              `${at}: u istraživanju vodiči moraju biti okomiti na papir (I: iz_papira ili u_papir), da se vide silnice`
            );
        } else {
          need(q.sonda == null, `${at}.sonda postoji samo u istraživanju`);
          if (need(
            tocka2(q.tocka),
            `${at}.tocka mora biti [x, y] (točka T u kojoj se traži polje)`
          ) && ok && q.vodici.every((w) => tocka2(w?.polozaj) && isSmjer(w?.I))) {
            const B = poljeVodica_zbroj(q.vodici, q.tocka, 1);
            if (need(B, `${at}: točka T leži na vodiču`))
              need(
                rjesenje(q) != null,
                `${at}: polje u točki T nije ni uz jednu os (ni ↑ ↓ ← → ⊙ ⊗ ni nula); premjesti točku ili vodiče`
              );
          }
        }
        break;
      }
      case "dva_vodica":
        smjer("I1");
        smjer("I2");
        if (isSmjer(q.I1) && isSmjer(q.I2)) {
          need(
            Math.abs(dot2(vec(q.I1), vec(q.I2))) === 1,
            `${at}: vodiči su paralelni, pa I1 i I2 moraju biti isti ili suprotni smjer`
          );
          need(
            ["gore", "dolje", ...OKOMITO].includes(q.I1),
            `${at}: vodiči stoje okomito (I gore/dolje) ili su okomiti na papir (iz_papira/u_papir)`
          );
        }
        break;
      case "petlja_struja":
        if (t === "struja" && !istrazi) {
          smjer("B", OKOMITO);
          nezadano("struja");
        } else {
          need(
            KRUZENJE.includes(q.struja),
            `${at}.struja mora biti "kazaljka" ili "suprotno"`
          );
          if (!istrazi) nezadano("B");
        }
        break;
      case "zavojnica":
        if (t === "struja" && !istrazi) {
          smjer("B", ["lijevo", "desno"]);
          nezadano("struja");
        } else {
          need(
            ["gore", "dolje"].includes(q.struja),
            `${at}.struja (smjer struje u prednjim zavojima) mora biti "gore" ili "dolje"`
          );
          if (!istrazi) nezadano("B");
        }
        for (const [k, lo, hi, jed2] of [
          ["N", 1, 5e3, ""],
          ["jakost", 0.01, 100, " A"],
          ["duljina", 1, 200, " cm"],
          ["polumjer", 0.2, 20, " cm"]
        ])
          if (q[k] != null)
            need(
              isNum(q[k]) && q[k] >= lo && q[k] <= hi,
              `${at}.${k} mora biti broj od ${lo} do ${hi}${jed2}`
            );
        if (!istrazi)
          need(
            ["N", "jakost", "duljina", "polumjer"].every((k) => q[k] == null),
            `${at}: N, jakost, duljina i polumjer (klizači) postoje samo u istraživanju`
          );
        break;
      case "magnet":
        smjer("N", U_RAVNINI);
        if (istrazi) {
          need(
            q.tocka == null,
            `${at}: u istraživanju nema točke s odgovorom; početni položaj kompasa je „sonda“`
          );
          if (q.sonda != null)
            need(
              tocka2(q.sonda) && Math.abs(q.sonda[0]) <= 3 && Math.abs(q.sonda[1]) <= 2,
              `${at}.sonda mora biti [x, y] unutar [−3, 3] × [−2, 2]`
            );
        } else if (need(
          tocka2(q.tocka),
          `${at}.tocka mora biti [x, y] (magnet je od −1 do 1, kompas negdje oko njega)`
        )) {
          need(
            Math.abs(q.tocka[0]) <= 3 && Math.abs(q.tocka[1]) <= 2,
            `${at}.tocka mora biti unutar [−3, 3] × [−2, 2]`
          );
          if (U_RAVNINI.includes(q.N)) {
            need(
              !unutarMagneta(q.tocka, q.N),
              `${at}.tocka je unutar magneta; kompas stavi izvan njega`
            );
            need(
              rjesenje(q) != null,
              `${at}: polje u točki nije ni uz jednu os; stavi kompas na os magneta ili na simetralu`
            );
          }
        }
        break;
      case "lenz_magnet":
        need(
          ["N", "S"].includes(q.pol),
          `${at}.pol (pol okrenut prema petlji) mora biti "N" ili "S"`
        );
        need(
          ["iznad", "ispod"].includes(q.polozaj),
          `${at}.polozaj mora biti "iznad" ili "ispod"`
        );
        need(
          ["priblizava", "udaljava", "miruje"].includes(q.gibanje),
          `${at}.gibanje mora biti "priblizava", "udaljava" ili "miruje"`
        );
        break;
      case "lenz_petlja":
        smjer("B", OKOMITO);
        need(
          FAZE_PETLJE.includes(q.faza),
          `${at}.faza mora biti jedna od: ${FAZE_PETLJE.join(", ")}`
        );
        if (q.v != null)
          need(
            ["lijevo", "desno"].includes(q.v),
            `${at}.v (smjer gibanja petlje) mora biti "lijevo" ili "desno"`
          );
        break;
    }
    if (q.poruke != null) {
      if (need(
        typeof q.poruke === "object" && !Array.isArray(q.poruke),
        `${at}.poruke: {odgovor: poruka}, npr. {"dolje": "…"}`
      )) {
        const moguci = [...SMJER_IMENA, NULA, ...KRUZENJE, NEMA];
        for (const [k, v] of Object.entries(q.poruke)) {
          need(
            moguci.includes(k),
            `${at}.poruke: „${k}“ nije mogući odgovor (${moguci.join(", ")})`
          );
          need(isText(v), `${at}.poruke.${k} mora biti tekst`);
          if (!istrazi && k === rjesenje(q))
            need(false, `${at}.poruke.${k}: to je točan odgovor`);
        }
      }
    }
    if (q.mijenja != null) {
      need(istrazi, `${at}.mijenja postoji samo u istraživanju`);
      need(
        Array.isArray(q.mijenja) && q.mijenja.every((k) => MIJENJA[q.scena]?.includes(k)),
        `${at}.mijenja: popis od ${MIJENJA[q.scena]?.join(", ") || "(ova scena nema što mijenjati)"}`
      );
    }
    if (!istrazi && list.length === pred) {
      const r = rjesenje(q);
      need(
        r != null,
        `${at}: odgovor nije jednoznačan (nijedan ili više smjerova daju zadano)`
      );
      if (r === NULA)
        need(nudiNulu(q), `${at}: točan odgovor je nula, a ona se ovdje ne nudi`);
    }
  }
  var MIJENJA = {
    naboj: ["naboj", "v", "B"],
    vodic_u_polju: ["I", "B"],
    polje_vodica: ["I"],
    dva_vodica: ["I1", "I2"],
    petlja_struja: ["struja"],
    zavojnica: ["struja"],
    magnet: ["N"],
    lenz_magnet: ["pol", "polozaj", "gibanje"],
    lenz_petlja: ["B", "faza"]
  };
  var RUKA = "palac u smjeru struje (gibanja pozitivnog naboja), ispruženi prsti uz silnice, a sila izlazi iz dlana";
  var uz = (a, b) => isSmjer(a) && isSmjer(b) && Math.abs(dot2(vec(a), vec(b))) === 1;
  function poruka(q, o, r) {
    if (q.poruke?.[o]) return q.poruke[o];
    const t = traziOf(q);
    switch (q.scena) {
      case "naboj":
      case "vodic_u_polju": {
        const nab = q.scena === "naboj";
        const brzina = nab ? "v" : "I";
        const tko = nab ? q.ime ? q.ime : "naboj" : "vodič";
        if (t === "F") {
          if (r === NULA)
            return nab ? "Brzina je paralelna s poljem: naboj se giba uzduž silnica, pa sile nema (sin 0° = 0)." : "Vodič leži uzduž silnica (struja je paralelna s poljem), pa sile nema (sin 0° = 0).";
          if (o === NULA)
            return nab ? "Sila nestaje samo kad se naboj giba uzduž silnica (v ∥ B). Ovdje v i B nisu paralelni." : "Sila nestaje samo kad vodič leži uzduž silnica. Ovdje struja i polje nisu paralelni.";
          if (uz(o, q[brzina]))
            return `Sila je uvijek okomita i na ${nab ? "brzinu" : "struju"} i na polje. Ovaj smjer leži uz ${nab ? "brzinu v" : "struju I"}.`;
          if (uz(o, q.B))
            return `Sila je uvijek okomita i na ${nab ? "brzinu" : "struju"} i na polje. Ovaj smjer leži uz polje B.`;
          if (o === suprotan(r)) {
            if (nab && q.naboj === "-")
              return `${tko[0].toUpperCase()}${tko.slice(1)} je negativan: sila je suprotna od smjera koji daje pravilo desne ruke.`;
            return `Obrnuto. Pravilo desne ruke: ${RUKA}. Lijeva ruka ili zamijenjeni ${brzina} i B daju upravo suprotan smjer.`;
          }
          return `Pravilo desne ruke: ${RUKA}.`;
        }
        if (o === NULA || !isSmjer(o)) return "Odaberi smjer.";
        const k = nab && q.naboj === "-" ? -1 : 1;
        const dobio = smjerIz(
          mul(
            t === "B" ? cross(vec(q[brzina]), vec(o)) : cross(vec(o), vec(q.B)),
            k
          )
        );
        const sto2 = t === "B" ? "polje" : nab ? "brzina" : "struja";
        if (dobio === NULA)
          return `S tim smjerom ${sto2 === "polje" ? "polje bi bilo paralelno s " + (nab ? "brzinom" : "strujom") : sto2 + " bi bila paralelna s poljem"}, pa sile uopće ne bi bilo.`;
        return `S tim smjerom sila bi gledala ${RIJEC[dobio]}, a zadana sila gleda ${RIJEC[q.F]}.${nab && q.naboj === "-" ? " Pazi: naboj je negativan." : ""}`;
      }
      case "polje_vodica": {
        if (r === NULA)
          return "Polja vodiča u točki T jednaka su po iznosu i suprotnog smjera, pa se poništavaju: ukupno polje je nula.";
        if (o === NULA)
          return q.vodici.length > 1 ? "Polja se u točki T ne poništavaju: odredi smjer polja svakog vodiča pravilom desne ruke i zbroji ih." : "Oko vodiča kojim teče struja uvijek postoji polje. Odredi mu smjer pravilom desne ruke.";
        if (o === suprotan(r))
          return "Obrnut smjer. Palac desne ruke u smjeru struje, savijeni prsti pokazuju kamo kruže silnice oko vodiča.";
        if (q.vodici.some((w) => uz(o, w.I)))
          return "Polje ne ide u smjeru struje, nego kruži oko vodiča (silnice su kružnice oko njega).";
        const radijalno = q.vodici.some((w) => {
          const d = [q.tocka[0] - w.polozaj[0], q.tocka[1] - w.polozaj[1], 0];
          return isSmjer(o) && Math.abs(dot2(vec(o), d)) / Math.max(1e-9, len(d)) > 0.999;
        });
        if (radijalno)
          return "Polje vodiča ne gleda prema vodiču ni od njega: silnice su kružnice oko vodiča, a polje je tangenta na kružnicu.";
        if (q.vodici.length > 1 && q.vodici.some((w) => smjerIz(poljeVodica(w, q.tocka, 1)) === o))
          return "To je smjer polja samo jednog vodiča. Odredi polje svakog vodiča i zbroji ih.";
        return "Silnice su kružnice oko vodiča: palac desne ruke u smjeru struje, savijeni prsti pokazuju smjer polja.";
      }
      case "dva_vodica": {
        const iste = q.I1 === q.I2;
        if (o === NULA)
          return t.startsWith("B") ? "Vodič oko sebe stvara polje i na mjestu drugog vodiča." : "Svaki vodič je u polju onog drugog, pa na njega djeluje sila.";
        if (t.startsWith("F")) {
          if (o === suprotan(r))
            return iste ? "Struje teku u istom smjeru, pa se vodiči privlače: sila gleda prema drugom vodiču." : "Struje teku u suprotnim smjerovima, pa se vodiči odbijaju: sila gleda od drugog vodiča.";
          return "Sila između paralelnih vodiča leži na spojnici vodiča: gleda prema drugom vodiču (privlačenje) ili od njega (odbijanje).";
        }
        if (o === suprotan(r))
          return "Obrnuto. Palac desne ruke u smjeru struje prvog vodiča, savijeni prsti pokazuju smjer polja kod drugog vodiča.";
        return "Polje vodiča kruži oko njega: kod drugog vodiča je okomito i na struju i na spojnicu vodiča.";
      }
      case "petlja_struja":
        if (t === "B") {
          if (o === suprotan(r))
            return "Obrnuto. Savij prste desne ruke u smjeru struje kroz petlju, palac pokazuje smjer polja u sredini.";
          return "U središtu petlje polje je okomito na ravninu petlje: iz papira ⊙ ili u papir ⊗.";
        }
        return "Obrnuto. Palac desne ruke u smjeru polja u sredini petlje, savijeni prsti pokazuju smjer struje.";
      case "zavojnica":
        if (t === "B") {
          if (o === suprotan(r))
            return "Obrnuto. Prsti desne ruke prate struju kroz zavoje, palac pokazuje smjer polja unutar zavojnice.";
          return "Unutar zavojnice polje ide uzduž njezine osi (lijevo ili desno).";
        }
        return "Obrnuto. Palac desne ruke u smjeru polja unutar zavojnice, savijeni prsti pokazuju smjer struje kroz zavoje.";
      case "magnet":
        if (o === suprotan(r))
          return "Obrnuto. Izvan magneta silnice izlaze iz sjevernog pola (N) i ulaze u južni (S), a sjeverni kraj igle kompasa pokazuje smjer polja.";
        if (OKOMITO.includes(o) || o === NULA)
          return "Polje magneta ovdje leži u ravnini papira: igla kompasa pokazuje gore, dolje, lijevo ili desno.";
        return "Prati silnicu kroz točku: izlazi iz N, savija se oko magneta i ulazi u S.";
      case "lenz_magnet": {
        const L = lenzMagnet(q);
        if (q.gibanje === "miruje")
          return "Magnet miruje: tok kroz petlju se ne mijenja, pa nema inducirane struje.";
        if (o === NEMA)
          return "Magnet se giba, pa se tok kroz petlju mijenja: inducirana struja teče.";
        const B = `Polje magneta kroz petlju gleda ${RIJEC[L.Bkroz]} (silnice izlaze iz N, ulaze u S)`;
        return L.tok === "raste" ? `${B}, a tok raste jer se magnet približava. Inducirano polje se opire porastu, pa unutar petlje gleda ${RIJEC[L.inducirano]}.` : `${B}, a tok pada jer se magnet udaljava. Inducirano polje pokušava zadržati tok, pa unutar petlje gleda isto: ${RIJEC[L.inducirano]}.`;
      }
      case "lenz_petlja": {
        const L = lenzPetlja(q);
        if (q.faza === "unutra")
          return "Petlja je cijela u polju: tok se ne mijenja, pa nema napona ni struje. Indukcija reagira na promjenu toka, a ne na samo polje.";
        if (q.faza === "izvan")
          return "Petlja je izvan polja: kroz nju nema toka i ništa se ne mijenja, pa nema struje.";
        if (o === NEMA)
          return `Dio petlje u polju ${q.faza === "ulazi" ? "raste" : "se smanjuje"}, pa se tok mijenja: inducirana struja teče.`;
        return L.tok === "raste" ? `Tok ${RIJEC[q.B]} raste, pa se inducirano polje opire: unutar petlje gleda ${RIJEC[L.inducirano]}.` : `Tok ${RIJEC[q.B]} pada, pa inducirano polje pokušava zadržati tok: unutar petlje gleda isto, ${RIJEC[L.inducirano]}.`;
      }
    }
    return "Nije točno.";
  }
  var istraziOf = (s) => s?.nacin === "istrazi";
  function natpis(q, o) {
    if (o === NULA)
      return ["naboj", "vodic_u_polju"].includes(q.scena) || traziOf(q).startsWith("F") ? "0 (nema sile)" : "0 (nema polja)";
    if (o === NEMA) return "struja ne teče";
    return naziv3(o);
  }
  var smjer_u_polju = {
    tip: "smjer_u_polju",
    samoRucno: true,
    uloga: (s) => istraziOf(s) ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => istraziOf(s),
    opis: `Smjer u magnetizmu (pravilo desne ruke, Lenz): učenik bira smjer na ploči ↑ ↓ ← → ⊙ (iz papira) ⊗ (u papir) i „0“ (nema sile/polja),
ili kruženje struje ↻ (kazaljka) / ↺ (suprotno) / „nema struje“. Točan smjer računa aplikacija iz situacije; poruke za tipične greške
(negativan naboj, v ∥ B, lijeva ruka, radijalno polje, Lenz: polje magneta umjesto opiranja…) dodaju se same.
Polja: tekst, nacin ("odaberi" = Tvoj red, "istrazi" = korak za gledanje: učenik mijenja zadane veličine i gleda rezultat; nema odgovora),
pitanja: 1–3 scene (istrazi: točno 1; može i jedna scena izravno u specu). Smjerovi: gore, dolje, lijevo, desno, iz_papira, u_papir.
Scene (trazi = nepoznata veličina, prva je zadana; nepoznata se ne zadaje):
- naboj: {naboj: "+"|"-", ime?, v, B, F?, trazi: "F"|"v"|"B"}  F = q·v×B
- vodic_u_polju: {I, B, F?, trazi: "F"|"B"|"I"}  F = I·L×B
- polje_vodica: {vodici: [{polozaj: [x, y], I, jakost? (A)}] (1–3), tocka: [x, y], trazi: "B"}  vodič ⊙/⊗ ili u papiru (I gore…); istrazi: sonda?, jedinica_duljine? ("cm"), prikazi_doprinose?
- dva_vodica: {I1, I2 (gore/dolje ili ⊙/⊗; vodič 1 lijevo, 2 desno), trazi: "F2"|"F1"|"B2"|"B1"}  B2 = polje vodiča 1 kod vodiča 2
- petlja_struja: {struja: "kazaljka"|"suprotno", trazi: "B"} ili {B, trazi: "struja"}  polje u središtu petlje
- zavojnica: {struja: "gore"|"dolje" (u prednjim zavojima), trazi: "B"} ili {B: lijevo|desno, trazi: "struja"}; istrazi: N?, jakost? (A), duljina? (cm), polumjer? (cm) → klizači
- magnet: {N: smjer sjevernog pola (gore/dolje/lijevo/desno), tocka: [x, y] (magnet je od −1 do 1), trazi: "B"} igla kompasa; istrazi: sonda?
- lenz_magnet: {pol: "N"|"S" (prema petlji), polozaj: "iznad"|"ispod", gibanje: "priblizava"|"udaljava"|"miruje"}  struja gledano odozgo
- lenz_petlja: {B: iz_papira|u_papir, faza: "izvan"|"ulazi"|"unutra"|"izlazi", v?: "desno"|"lijevo"}
Svaka scena: oznaka? (natpis iznad scene, npr. "1. magnet se približava"), poruke? ({odgovor: poruka} nadjačava automatske),
mijenja? (samo istrazi: koje zadane veličine učenik smije mijenjati).`,
    primjer: {
      tip: "smjer_u_polju",
      nacin: "odaberi",
      tekst: "Proton ulijeće u polje koje ulazi u papir. Kamo djeluje sila na njega?",
      pitanja: [
        {
          scena: "naboj",
          naboj: "+",
          ime: "proton",
          v: "desno",
          B: "u_papir",
          trazi: "F"
        }
      ]
    },
    primjeri: [
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Kojim smjerom teče inducirana struja, gledano odozgo?",
        pitanja: [
          {
            scena: "lenz_magnet",
            pol: "N",
            polozaj: "iznad",
            gibanje: "priblizava",
            oznaka: "1. magnet se približava"
          },
          {
            scena: "lenz_magnet",
            pol: "N",
            polozaj: "iznad",
            gibanje: "udaljava",
            oznaka: "2. magnet se udaljava"
          }
        ]
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Elektron se giba udesno u polju koje ulazi u papir. Kamo djeluje sila na njega?",
        scena: "naboj",
        naboj: "-",
        ime: "elektron",
        v: "desno",
        B: "u_papir"
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Vodičem teče struja prema gore, a polje ulazi u papir. Kamo djeluje sila na vodič?",
        scena: "vodic_u_polju",
        I: "gore",
        B: "u_papir"
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Kamo je usmjereno polje u točki T, na polovici između vodiča?",
        scena: "polje_vodica",
        vodici: [
          { polozaj: [-10, 0], I: "iz_papira" },
          { polozaj: [10, 0], I: "u_papir" }
        ],
        tocka: [0, 0]
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Struje u oba vodiča teku prema gore. Kamo gleda polje prvog vodiča kod drugog vodiča, a kamo sila na drugi vodič?",
        pitanja: [
          {
            scena: "dva_vodica",
            I1: "gore",
            I2: "gore",
            trazi: "B2",
            oznaka: "1. polje vodiča 1 kod vodiča 2"
          },
          {
            scena: "dva_vodica",
            I1: "gore",
            I2: "gore",
            trazi: "F2",
            oznaka: "2. sila na vodič 2"
          }
        ]
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Petlja ulazi u polje, cijela je u polju, pa izlazi. Kojim smjerom teče inducirana struja?",
        pitanja: [
          {
            scena: "lenz_petlja",
            B: "u_papir",
            faza: "ulazi",
            oznaka: "1. ulazi"
          },
          {
            scena: "lenz_petlja",
            B: "u_papir",
            faza: "unutra",
            oznaka: "2. cijela unutra"
          },
          {
            scena: "lenz_petlja",
            B: "u_papir",
            faza: "izlazi",
            oznaka: "3. izlazi"
          }
        ]
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Kamo pokazuje sjeverni kraj igle kompasa u točki T?",
        scena: "magnet",
        N: "desno",
        tocka: [0, 1.2]
      },
      {
        tip: "smjer_u_polju",
        nacin: "odaberi",
        tekst: "Kamo je usmjereno polje unutar zavojnice?",
        scena: "zavojnica",
        struja: "gore"
      },
      {
        tip: "smjer_u_polju",
        nacin: "istrazi",
        tekst: "Mijenjaj smjer brzine, polja i predznak naboja i gledaj kamo djeluje sila.",
        scena: "naboj",
        naboj: "+",
        v: "desno",
        B: "u_papir"
      },
      {
        tip: "smjer_u_polju",
        nacin: "istrazi",
        tekst: "Povuci kompas između vodiča. Zatim promijeni smjer struje u drugom vodiču.",
        scena: "polje_vodica",
        vodici: [
          { polozaj: [-10, 0], I: "iz_papira", jakost: 5 },
          { polozaj: [10, 0], I: "u_papir", jakost: 5 }
        ],
        sonda: [0, 0],
        jedinica_duljine: "cm",
        prikazi_doprinose: true
      },
      {
        tip: "smjer_u_polju",
        nacin: "istrazi",
        tekst: "Mijenjaj broj zavoja, struju, duljinu i polumjer zavojnice i gledaj polje u njoj. Povuci kompas oko zavojnice.",
        scena: "zavojnica",
        struja: "gore",
        N: 500,
        jakost: 2,
        duljina: 20,
        polumjer: 3
      },
      {
        tip: "smjer_u_polju",
        nacin: "istrazi",
        tekst: "Povuci kompas oko magneta i gledaj kamo pokazuje igla. Okreni magnet.",
        scena: "magnet",
        N: "desno",
        sonda: [1.8, 0.6]
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(
        ["odaberi", "istrazi"].includes(s.nacin),
        "smjer_u_polju.nacin mora biti odaberi ili istrazi"
      );
      need(isText(s.tekst), "smjer_u_polju treba „tekst“");
      const ist = istraziOf(s);
      if (ist) noAnswerFields(s, need);
      const pit = pitanjaOf(s);
      if (Array.isArray(s.pitanja)) {
        need(
          s.scena == null,
          "scena ide ili izravno u spec ili u „pitanja“, ne oboje"
        );
        for (const k of Object.keys(s))
          need(
            SPEC_POLJA.includes(k),
            `smjer_u_polju: polje „${k}“ ide u pojedinu scenu u „pitanja“`
          );
      }
      if (!need(
        pit.length >= 1 && pit.length <= (ist ? 1 : 3),
        ist ? "istrazi ima točno jednu scenu" : "smjer_u_polju treba 1–3 pitanja (scene)"
      ))
        return list;
      pit.forEach(
        (q, i) => validatePitanje(
          q,
          pit === s.pitanja ? `pitanja[${i}]` : "scena",
          need,
          ist,
          list
        )
      );
      return list;
    },
    rjesenje: (s) => pitanjaOf(s).map(rjesenje),
    check(s, odgovor3) {
      const pit = pitanjaOf(s);
      const o = Array.isArray(odgovor3) ? odgovor3 : [odgovor3];
      if (o.length !== pit.length || o.some((x) => typeof x !== "string" || !x))
        return {
          tocno: false,
          poruka: pit.length > 1 ? "Odaberi odgovor za svaku sliku." : "Odaberi smjer.",
          prazno: true
        };
      for (let i = 0; i < pit.length; i++) {
        const r = rjesenje(pit[i]);
        if (o[i] !== r) {
          const p = poruka(pit[i], o[i], r);
          return {
            tocno: false,
            poruka: pit.length > 1 ? `**${pit[i].oznaka ?? `${i + 1}. slika`}:** ${p}` : p,
            kriv: i
          };
        }
      }
      return { tocno: true };
    },
    tocanOdgovor(s) {
      const pit = pitanjaOf(s);
      if (pit.length === 1) return natpis(pit[0], rjesenje(pit[0]));
      return pit.map((q, i) => `${q.oznaka ?? `${i + 1}.`}: ${natpis(q, rjesenje(q))}`).join("; ");
    }
  };

  // src/lib/lesson/katalog/fizika/indukcija.js
  var SCENE5 = ["zakretanje", "magnet_petlja", "petlja_u_polje", "graf_toka"];
  var NACINI14 = {
    zakretanje: ["istrazi", "izracunaj"],
    magnet_petlja: ["istrazi", "smjer"],
    petlja_u_polje: ["istrazi", "smjer", "izracunaj"],
    graf_toka: ["istrazi", "oznaci", "izracunaj"]
  };
  var POLJA = {
    zakretanje: ["B", "S", "jedinica_S", "N", "kut", "pitaj"],
    magnet_petlja: ["pol", "polozaj", "faze"],
    petlja_u_polje: [
      "B",
      "smjer_B",
      "l",
      "jedinica_l",
      "v",
      "R",
      "faze",
      "pitaj"
    ],
    graf_toka: ["tocke", "jedinica_toka", "N", "trazi", "dionica", "pitaj"]
  };
  var OPCE2 = [
    "tip",
    "scena",
    "nacin",
    "tekst",
    "krivo",
    "savjet",
    "tolerancija"
  ];
  var TOK_JED = { Wb: 1, mWb: 1e-3, µWb: 1e-6, μWb: 1e-6 };
  var S_JED = { "m²": 1, "dm²": 0.01, "cm²": 1e-4, "mm²": 1e-6 };
  var L_JED = { m: 1, cm: 0.01, mm: 1e-3 };
  var rad2 = (deg2) => deg2 * Math.PI / 180;
  var povrsina2 = (s) => s.S * S_JED[s.jedinica_S ?? "m²"];
  var duljina3 = (s) => s.l * L_JED[s.jedinica_l ?? "m"];
  var naponPetlje = (s) => s.B * duljina3(s) * s.v;
  function dionice(s) {
    const k = TOK_JED[s.jedinica_toka ?? "mWb"];
    const N = s.N ?? 1;
    return s.tocke.slice(1).map((p, i) => {
      const [t0, f0] = s.tocke[i];
      const [t1, f1] = p;
      const dPhi = (f1 - f0) * k;
      const dt = t1 - t0;
      return { od: t0, do: t1, f0, f1, dPhi, dt, U: -N * dPhi / dt };
    });
  }
  var NULA_TOL = 1e-12;
  function tocneDionice(s) {
    const d = dionice(s);
    if (s.trazi === "nula")
      return d.map((x, i) => Math.abs(x.U) < NULA_TOL ? i : -1).filter((i) => i >= 0);
    const max = Math.max(...d.map((x) => Math.abs(x.U)));
    return d.map((x, i) => Math.abs(Math.abs(x.U) - max) <= max * 1e-9 ? i : -1).filter((i) => i >= 0);
  }
  function fazaKaoScena(s, f2) {
    if (s.scena === "magnet_petlja")
      return {
        scena: "lenz_magnet",
        pol: s.pol,
        polozaj: s.polozaj,
        gibanje: f2.gibanje,
        oznaka: f2.oznaka
      };
    return {
      scena: "lenz_petlja",
      B: s.smjer_B,
      faza: typeof f2 === "string" ? f2 : f2.faza,
      oznaka: typeof f2 === "string" ? void 0 : f2.oznaka
    };
  }
  var fazeOf = (s) => s.faze ?? [];
  var strujaFaze = (s, f2) => s.scena === "magnet_petlja" ? lenzMagnet(fazaKaoScena(s, f2)).struja : lenzPetlja(fazaKaoScena(s, f2)).struja;
  var FAZA_TXT = {
    izvan: "izvan polja",
    ulazi: "ulazi u polje",
    unutra: "cijela u polju",
    izlazi: "izlazi iz polja",
    priblizava: "magnet se približava",
    udaljava: "magnet se udaljava",
    miruje: "magnet miruje"
  };
  var oznakaFaze = (s, f2, i) => typeof f2 === "object" && f2.oznaka || `${i + 1}. ${FAZA_TXT[typeof f2 === "string" ? f2 : f2.gibanje ?? f2.faza]}`;
  function izracun(s) {
    if (s.scena === "zakretanje") {
      const jed2 = s.pitaj?.jedinica ?? "Wb";
      const f2 = 1 / TOK_JED[jed2];
      const a = rad2(s.kut ?? 0);
      const B = s.B;
      const S = povrsina2(s);
      const k2 = [];
      if (Math.abs(Math.sin(a) - Math.cos(a)) > 1e-6)
        k2.push({
          vrijednost: B * S * Math.sin(a) * f2,
          poruka: "Uzeo si sinus. U Φ = BS cos α kut α je između silnica i okomice na plohu petlje, zato kosinus."
        });
      if (Math.abs(Math.cos(a) - 1) > 1e-6)
        k2.push({
          vrijednost: B * S * f2,
          poruka: "To je tok kad su silnice okomite na petlju. Petlja je zakrenuta, pa pomnoži s cos α."
        });
      if ((s.jedinica_S ?? "m²") !== "m²")
        k2.push({
          vrijednost: B * s.S * Math.cos(a) * f2,
          poruka: `Površinu treba pretvoriti u m²: 1 ${s.jedinica_S} = ${formatSci(S_JED[s.jedinica_S])} m².`
        });
      if ((s.N ?? 1) > 1)
        k2.push({
          vrijednost: s.N * B * S * Math.cos(a) * f2,
          poruka: "Tok se računa kroz jedan zavoj, bez broja zavoja N (N ulazi tek u inducirani napon)."
        });
      return {
        vrijednost: B * S * Math.cos(a) * f2,
        jedinica: jed2,
        oznaka: "Φ",
        krivo: k2
      };
    }
    if (s.scena === "petlja_u_polje") {
      const U2 = naponPetlje(s);
      const uCm = s.B * s.l * s.v;
      if ((s.pitaj?.velicina ?? "U") === "U") {
        const k3 = [];
        if ((s.jedinica_l ?? "m") !== "m")
          k3.push({
            vrijednost: uCm,
            poruka: `Duljinu stranice treba pretvoriti u metre: ${withUnit(s.l, s.jedinica_l)} = ${withUnit(duljina3(s), "m")}.`
          });
        if (isNum(s.R))
          k3.push({
            vrijednost: U2 / s.R,
            poruka: "To je struja I = U/R, a pita se napon."
          });
        return { vrijednost: U2, jedinica: "V", oznaka: "U", krivo: k3 };
      }
      const k2 = [
        {
          vrijednost: U2,
          poruka: "To je napon U = Blv. Struju daje Ohmov zakon: I = U/R."
        },
        {
          vrijednost: U2 * s.R,
          poruka: "Pomnožio si s otporom. Ohmov zakon: I = U/R."
        },
        { vrijednost: s.R / U2, poruka: "Obrnuo si omjer. Ohmov zakon: I = U/R." }
      ];
      if ((s.jedinica_l ?? "m") !== "m")
        k2.push({
          vrijednost: uCm / s.R,
          poruka: `Duljinu stranice treba pretvoriti u metre: ${withUnit(s.l, s.jedinica_l)} = ${withUnit(duljina3(s), "m")}.`
        });
      return { vrijednost: U2 / s.R, jedinica: "A", oznaka: "I", krivo: k2 };
    }
    const d = dionice(s)[s.dionica - 1];
    const N = s.N ?? 1;
    const kj = TOK_JED[s.jedinica_toka ?? "mWb"];
    const U = Math.abs(d.U);
    const k = [];
    if (N > 1)
      k.push({
        vrijednost: Math.abs(d.dPhi / d.dt),
        poruka: `Zaboravio si broj zavoja: U = N·ΔΦ/Δt, a N = ${N}.`
      });
    if (kj !== 1)
      k.push({
        vrijednost: N * Math.abs(d.f1 - d.f0) / d.dt,
        poruka: `Tok je u ${s.jedinica_toka ?? "mWb"}: pretvori ga u Wb (1 ${s.jedinica_toka ?? "mWb"} = ${formatSci(kj)} Wb).`
      });
    if (d.dt !== 1)
      k.push({
        vrijednost: N * Math.abs(d.dPhi),
        poruka: "Zaboravio si podijeliti s vremenom: napon je promjena toka u jedinici vremena, ΔΦ/Δt."
      });
    if (d.do !== d.dt)
      k.push({
        vrijednost: N * Math.abs(d.dPhi) / d.do,
        poruka: `Dijeli s trajanjem dionice Δt = ${withUnit(d.dt, "s")}, a ne s trenutkom kraja.`
      });
    if (Math.abs(d.f1) !== Math.abs(d.f1 - d.f0))
      k.push({
        vrijednost: N * Math.abs(d.f1) * kj / d.dt,
        poruka: "Uzeo si visinu grafa (tok), a treba promjena toka ΔΦ (razlika na kraju i početku dionice)."
      });
    if (Math.abs(d.dPhi) > 0)
      k.push({
        vrijednost: N * d.dt / Math.abs(d.dPhi),
        poruka: "Obrnuo si omjer: U = N·ΔΦ/Δt."
      });
    return { vrijednost: U, jedinica: "V", oznaka: "U", krivo: k };
  }
  var bez = (krivo, a) => krivo.filter(
    (k) => Number.isFinite(k.vrijednost) && Math.abs(k.vrijednost - a) > Math.max(Math.abs(a) * 0.011, Math.abs(k.vrijednost) * 1e-6)
  );
  var tol = (s, a) => isNum(s.tolerancija) ? s.tolerancija : Math.max(1e-15, Math.abs(a) * 0.01);
  var indukcija = {
    tip: "indukcija",
    samoRucno: true,
    uloga: (s) => s?.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s?.nacin === "istrazi",
    opis: `Elektromagnetska indukcija kao simulacija. Polja: tekst, scena, nacin ("istrazi" = korak za gledanje, ostalo = Tvoj red).
Scene i načini:
- "zakretanje": petlja u homogenom polju, klizač kuta i gumb „Vrti“ (grafovi Φ(t) i U(t)). B (T), S, jedinica_S? ("m²" | "cm²", zadano m²), N? (zavoji), kut? (°, kut između silnica i okomice na petlju).
  nacin "istrazi" | "izracunaj" (Φ = BS cos α pri kutu; pitaj?: {jedinica: "Wb" | "mWb"}).
- "magnet_petlja": štapićasti magnet iznad ili ispod vodoravne petlje: pol ("N" | "S", pol prema petlji), polozaj ("iznad" | "ispod").
  nacin "istrazi" (učenik vuče magnet, galvanometar i struja uživo) | "smjer" (faze: [{gibanje: "priblizava" | "udaljava" | "miruje", oznaka?}] 1–3; učenik za svaku bira ↻ / ↺ / nema, gledano odozgo).
- "petlja_u_polje": pravokutna petlja ulazi u područje polja, prolazi ga i izlazi: B (T), smjer_B ("u_papir" | "iz_papira"), l (prednja stranica), jedinica_l? ("m" | "cm"), v (m/s), R? (Ω).
  nacin "istrazi" (pokreni/povuci petlju, grafovi Φ i U) | "smjer" (faze: 1–4 od "izvan", "ulazi", "unutra", "izlazi") | "izracunaj" (pitaj: {velicina: "U" | "I"} dok petlja ulazi; za I treba R).
- "graf_toka": graf Φ(t) po dionicama: tocke [[t (s), Φ], …] (3–6 točaka, t raste), jedinica_toka? ("mWb" zadano | "Wb" | "µWb"), N? (zavoji, zadano 1); ispod je graf napona U = −N·ΔΦ/Δt.
  nacin "istrazi" | "oznaci" (trazi: "najveci" | "nula": učenik dodirne dionicu s najvećim / nultim naponom) | "izracunaj" (dionica: redni broj od 1, učenik upiše iznos napona u V).
Za "izracunaj" točan iznos i tipične krive vrijednosti (sinus umjesto kosinusa, cm² ili mWb nepretvoreni, bez N, visina umjesto nagiba, I = U·R…) računa aplikacija; ${KRIVO_OPIS}; tolerancija? (zadano 1 %).`,
    primjer: {
      tip: "indukcija",
      scena: "graf_toka",
      nacin: "oznaci",
      tekst: "Dodirni dionicu u kojoj je inducirani napon najveći.",
      tocke: [
        [0, 0],
        [4, 4],
        [6, 4],
        [7, 0]
      ],
      jedinica_toka: "mWb",
      N: 200,
      trazi: "najveci"
    },
    primjeri: [
      {
        tip: "indukcija",
        scena: "graf_toka",
        nacin: "oznaci",
        tekst: "Dodirni dionicu u kojoj je inducirani napon nula.",
        tocke: [
          [0, 0],
          [4, 4],
          [6, 4],
          [7, 0]
        ],
        jedinica_toka: "mWb",
        N: 200,
        trazi: "nula"
      },
      {
        tip: "indukcija",
        scena: "graf_toka",
        nacin: "izracunaj",
        tekst: "Koliki je inducirani napon u prvoj dionici? Zavojnica ima 200 zavoja.",
        tocke: [
          [0, 0],
          [4, 4],
          [6, 4],
          [7, 0]
        ],
        jedinica_toka: "mWb",
        N: 200,
        dionica: 1
      },
      {
        tip: "indukcija",
        scena: "graf_toka",
        nacin: "istrazi",
        tekst: "Povuci crtu po grafu toka i gledaj napon ispod: ovisi o nagibu, a ne o visini.",
        tocke: [
          [0, 0],
          [4, 4],
          [6, 4],
          [7, 0]
        ],
        jedinica_toka: "mWb",
        N: 200
      },
      {
        tip: "indukcija",
        scena: "zakretanje",
        nacin: "istrazi",
        tekst: "Zakreći petlju klizačem ili je pusti da se vrti i gledaj tok i napon.",
        B: 0.4,
        S: 12,
        jedinica_S: "cm²"
      },
      {
        tip: "indukcija",
        scena: "zakretanje",
        nacin: "izracunaj",
        tekst: "Zavoj površine 12 cm² zakrenut je za 60°. Koliki je tok kroz njega u polju 0,4 T?",
        B: 0.4,
        S: 12,
        jedinica_S: "cm²",
        N: 200,
        kut: 60
      },
      {
        tip: "indukcija",
        scena: "magnet_petlja",
        nacin: "istrazi",
        tekst: "Povuci magnet prema petlji i od nje i gledaj galvanometar i smjer struje.",
        pol: "N",
        polozaj: "iznad"
      },
      {
        tip: "indukcija",
        scena: "magnet_petlja",
        nacin: "smjer",
        tekst: "Kojim smjerom teče inducirana struja, gledano odozgo?",
        pol: "N",
        polozaj: "iznad",
        faze: [{ gibanje: "priblizava" }, { gibanje: "udaljava" }]
      },
      {
        tip: "indukcija",
        scena: "petlja_u_polje",
        nacin: "istrazi",
        tekst: "Pokreni petlju (ili je povuci) i prati graf napona.",
        B: 0.25,
        smjer_B: "u_papir",
        l: 40,
        jedinica_l: "cm",
        v: 5,
        R: 2.5
      },
      {
        tip: "indukcija",
        scena: "petlja_u_polje",
        nacin: "smjer",
        tekst: "Kojim smjerom teče struja dok petlja ulazi, dok je cijela u polju i dok izlazi?",
        B: 0.25,
        smjer_B: "u_papir",
        l: 40,
        jedinica_l: "cm",
        v: 5,
        R: 2.5,
        faze: ["ulazi", "unutra", "izlazi"]
      },
      {
        tip: "indukcija",
        scena: "petlja_u_polje",
        nacin: "izracunaj",
        tekst: "Koliku struju polje inducira dok petlja ulazi?",
        B: 0.25,
        smjer_B: "u_papir",
        l: 40,
        jedinica_l: "cm",
        v: 5,
        R: 2.5,
        pitaj: { velicina: "I" }
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "indukcija treba „tekst“");
      if (!need(
        SCENE5.includes(s.scena),
        `indukcija.scena mora biti jedna od: ${SCENE5.join(", ")}`
      ))
        return list;
      if (!need(
        NACINI14[s.scena].includes(s.nacin),
        `za scenu „${s.scena}“ nacin mora biti: ${NACINI14[s.scena].join(", ")}`
      ))
        return list;
      for (const k of Object.keys(s))
        need(
          [...OPCE2, ...POLJA[s.scena]].includes(k),
          `indukcija (${s.scena}): nepoznato polje „${k}“`
        );
      const ist = s.nacin === "istrazi";
      if (ist) noAnswerFields(s, need);
      const broj4 = (k, lo, hi, opis) => need(
        isNum(s[k]) && s[k] > lo && s[k] <= hi,
        `${k} mora biti broj (${opis})`
      );
      const nepotrebno = (keys, kada) => keys.forEach(
        (k) => need(s[k] == null, `polje „${k}“ ima smisla samo ${kada}`)
      );
      if (s.nacin !== "izracunaj")
        nepotrebno(
          ["pitaj", "savjet", "tolerancija", ...ist ? [] : ["krivo"]],
          "u načinu izracunaj"
        );
      if (s.scena === "zakretanje") {
        broj4("B", 0, 100, "T, veći od 0");
        broj4("S", 0, 1e6, "površina, veća od 0");
        if (s.jedinica_S != null)
          need(
            s.jedinica_S in S_JED,
            `jedinica_S mora biti jedna od: ${Object.keys(S_JED).join(", ")}`
          );
        if (s.N != null)
          need(
            Number.isInteger(s.N) && s.N >= 1 && s.N <= 1e4,
            "N je cijeli broj zavoja (1–10000)"
          );
        if (s.kut != null)
          need(
            isNum(s.kut) && s.kut >= 0 && s.kut <= 180,
            "kut je u stupnjevima, 0–180"
          );
        if (s.nacin === "izracunaj") {
          need(isNum(s.kut), "izracunaj treba kut (°) pri kojem se računa tok");
          if (s.pitaj != null)
            need(
              typeof s.pitaj === "object" && ["Wb", "mWb", "µWb"].includes(s.pitaj.jedinica ?? "Wb") && Object.keys(s.pitaj).every(
                (k) => ["jedinica", "velicina"].includes(k)
              ),
              "pitaj: {jedinica: Wb | mWb | µWb}"
            );
        }
      }
      if (s.scena === "magnet_petlja") {
        need(
          ["N", "S"].includes(s.pol),
          `pol (pol okrenut prema petlji) mora biti "N" ili "S"`
        );
        need(
          ["iznad", "ispod"].includes(s.polozaj),
          `polozaj mora biti "iznad" ili "ispod"`
        );
        if (s.nacin === "smjer") {
          if (need(
            Array.isArray(s.faze) && s.faze.length >= 1 && s.faze.length <= 3,
            "faze: 1–3 faze [{gibanje, oznaka?}]"
          ))
            s.faze.forEach((f2, i) => {
              need(
                ["priblizava", "udaljava", "miruje"].includes(f2?.gibanje),
                `faze[${i}].gibanje mora biti priblizava, udaljava ili miruje`
              );
              if (f2?.oznaka != null)
                need(isText(f2.oznaka), `faze[${i}].oznaka mora biti tekst`);
              for (const k of Object.keys(f2 ?? {}))
                need(
                  ["gibanje", "oznaka"].includes(k),
                  `faze[${i}] nema polje „${k}“`
                );
            });
        } else nepotrebno(["faze"], "u načinu smjer");
      }
      if (s.scena === "petlja_u_polje") {
        broj4("B", 0, 100, "T, veći od 0");
        need(
          OKOMITO.includes(s.smjer_B),
          `smjer_B mora biti "u_papir" ili "iz_papira"`
        );
        broj4("l", 0, 1e3, "duljina prednje stranice, veća od 0");
        if (s.jedinica_l != null)
          need(s.jedinica_l in L_JED, "jedinica_l mora biti m, cm ili mm");
        broj4("v", 0, 1e4, "brzina u m/s, veća od 0");
        if (s.R != null) broj4("R", 0, 1e6, "otpor petlje u Ω");
        if (s.nacin === "smjer") {
          if (need(
            Array.isArray(s.faze) && s.faze.length >= 1 && s.faze.length <= 4,
            "faze: 1–4 od izvan, ulazi, unutra, izlazi"
          ))
            s.faze.forEach(
              (f2, i) => need(
                FAZE_PETLJE.includes(f2) || FAZE_PETLJE.includes(f2?.faza) && (f2.oznaka == null || isText(f2.oznaka)),
                `faze[${i}] mora biti jedna od: ${FAZE_PETLJE.join(", ")} (ili {faza, oznaka})`
              )
            );
        } else nepotrebno(["faze"], "u načinu smjer");
        if (s.nacin === "izracunaj") {
          if (need(
            s.pitaj && ["U", "I"].includes(s.pitaj.velicina),
            `pitaj: {velicina: "U" | "I"}`
          )) {
            need(
              Object.keys(s.pitaj).every((k) => ["velicina"].includes(k)),
              "pitaj ima samo polje velicina"
            );
            if (s.pitaj.velicina === "I")
              need(isNum(s.R), "za struju I treba otpor R");
          }
        }
      }
      if (s.scena === "graf_toka") {
        const okT = need(
          Array.isArray(s.tocke) && s.tocke.length >= 3 && s.tocke.length <= 6 && s.tocke.every(
            (p) => Array.isArray(p) && p.length === 2 && p.every(isNum)
          ),
          "tocke: 3–6 točaka [t, Φ]"
        );
        if (okT) {
          need(
            s.tocke.every((p, i) => i === 0 || p[0] > s.tocke[i - 1][0]),
            "vrijeme t u tockama mora strogo rasti"
          );
          need(s.tocke[0][0] >= 0, "graf počinje u t ≥ 0");
          need(
            s.tocke.every((p) => p[1] >= 0),
            "tok u grafu neka bude ≥ 0 (crta se od nule)"
          );
          need(
            s.tocke.some((p) => p[1] > 0),
            "tok ne smije biti stalno nula"
          );
        }
        if (s.jedinica_toka != null)
          need(
            s.jedinica_toka in TOK_JED,
            `jedinica_toka mora biti jedna od: Wb, mWb, µWb`
          );
        if (s.N != null)
          need(
            Number.isInteger(s.N) && s.N >= 1 && s.N <= 1e4,
            "N je cijeli broj zavoja (1–10000)"
          );
        if (s.nacin === "oznaci") {
          if (need(
            ["najveci", "nula"].includes(s.trazi),
            `oznaci treba trazi: "najveci" ili "nula"`
          ) && okT && !list.length) {
            const t = tocneDionice(s);
            if (s.trazi === "najveci")
              need(
                t.length === 1,
                "najveći napon mora biti u točno jednoj dionici (dvije su jednako strme)"
              );
            else
              need(
                t.length >= 1 && t.length < dionice(s).length,
                "za trazi nula graf treba barem jednu vodoravnu dionicu i barem jednu koja nije"
              );
          }
        } else nepotrebno(["trazi"], "u načinu oznaci");
        if (s.nacin === "izracunaj")
          need(
            Number.isInteger(s.dionica) && okT && s.dionica >= 1 && s.dionica < s.tocke.length,
            `dionica mora biti redni broj dionice (1–${okT ? s.tocke.length - 1 : "?"})`
          );
        else nepotrebno(["dionica"], "u načinu izracunaj");
      }
      if (s.nacin === "izracunaj" && !list.length) {
        const r = izracun(s);
        need(Number.isFinite(r.vrijednost), "rezultat se ne može izračunati");
        validateKrivo(s.krivo, r.vrijednost, tol(s, r.vrijednost), need);
      }
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "smjer") {
        const f2 = fazeOf(s);
        const o = Array.isArray(odgovor3) ? odgovor3 : [];
        if (o.length !== f2.length || o.some((x) => ![...KRUZENJE, NEMA].includes(x)))
          return {
            tocno: false,
            poruka: "Odaberi smjer struje za svaku fazu.",
            prazno: true
          };
        for (let i = 0; i < f2.length; i++) {
          const r = strujaFaze(s, f2[i]);
          if (o[i] !== r)
            return {
              tocno: false,
              kriv: i,
              poruka: `**${oznakaFaze(s, f2[i], i)}:** ${poruka(fazaKaoScena(s, f2[i]), o[i], r)}`
            };
        }
        return { tocno: true };
      }
      if (s.nacin === "oznaci") {
        if (!Number.isInteger(odgovor3))
          return {
            tocno: false,
            poruka: "Dodirni jednu dionicu grafa.",
            prazno: true
          };
        const d = dionice(s);
        if (tocneDionice(s).includes(odgovor3)) return { tocno: true };
        const x = d[odgovor3];
        if (!x)
          return {
            tocno: false,
            poruka: "Dodirni jednu dionicu grafa.",
            prazno: true
          };
        const najvisa = Math.max(...s.tocke.map((p) => p[1]));
        if (s.trazi === "najveci") {
          if (Math.abs(x.U) < NULA_TOL && Math.max(x.f0, x.f1) === najvisa)
            return {
              tocno: false,
              poruka: "Tu je tok najveći, ali se ne mijenja: graf je vodoravan, pa je napon nula. Napon ovisi o nagibu grafa, a ne o visini."
            };
          if (Math.abs(x.U) < NULA_TOL)
            return {
              tocno: false,
              poruka: "Tu je graf vodoravan: tok se ne mijenja, pa je napon nula. Traži najstrmiju dionicu."
            };
          return {
            tocno: false,
            poruka: `U ${odgovor3 + 1}. dionici tok se mijenja, ali ima strmija dionica. Napon je razmjeran nagibu: ΔΦ/Δt.`
          };
        }
        return {
          tocno: false,
          poruka: "Tu se tok mijenja (graf nije vodoravan), pa napon nije nula. Napon je nula gdje je graf vodoravan, makar tok bio velik."
        };
      }
      if (s.nacin === "izracunaj") {
        const r = izracun(s);
        return checkFiz({
          input: odgovor3,
          answer: r.vrijednost,
          tol: tol(s, r.vrijednost),
          unit: r.jedinica,
          krivo: [...s.krivo ?? [], ...bez(r.krivo, r.vrijednost)],
          savjet: s.savjet
        });
      }
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "smjer")
        return fazeOf(s).map(
          (f2, i) => `${oznakaFaze(s, f2, i)}: ${natpis(fazaKaoScena(s, f2), strujaFaze(s, f2))}`
        ).join("; ");
      if (s.nacin === "oznaci")
        return tocneDionice(s).map((i) => `${i + 1}. dionica`).join(" ili ");
      if (s.nacin === "izracunaj") {
        const r = izracun(s);
        return `${r.oznaka} = ${formatSci(r.vrijednost)} ${r.jedinica}`;
      }
      return "";
    },
    brojcaniOdgovor: (s) => s?.nacin === "izracunaj" ? izracun(s).vrijednost : null
  };

  // src/lib/lesson/katalog/fizika/naboj_u_polju.js
  var CESTICE2 = {
    proton: { ime: "proton", oznaka: "p⁺", m: MP, q: E },
    elektron: { ime: "elektron", oznaka: "e⁻", m: ME, q: -E },
    alfa: { ime: "alfa-čestica", oznaka: "α", m: 664e-29, q: 2 * E }
  };
  function cesticaOf(s) {
    if (typeof s.cestica === "string") return CESTICE2[s.cestica];
    const c = s.cestica;
    return {
      ime: c.ime ?? "čestica",
      oznaka: c.oznaka ?? (c.q > 0 ? "+" : "−"),
      m: c.m,
      q: c.q
    };
  }
  var JEDINICE2 = {
    B: { T: 1, mT: 1e-3, µT: 1e-6 },
    v: { "m/s": 1, "km/s": 1e3 },
    kut: { "°": 1 },
    I: { A: 1 }
  };
  var ZADANA_JED = { B: "T", v: "m/s", kut: "°", I: "A" };
  var jedKlizaca = (k) => k.jedinica ?? ZADANA_JED[k.id];
  var faktor = (k) => JEDINICE2[k.id]?.[jedKlizaca(k)] ?? 1;
  function vrijednostiKlizaca3(k) {
    if (Array.isArray(k.vrijednosti)) return k.vrijednosti;
    const n = Math.round((k.max - k.min) / k.korak);
    return Array.from(
      { length: n + 1 },
      (_, i) => Number((k.min + i * k.korak).toPrecision(12))
    );
  }
  var klizacOf = (s, id) => (s.klizaci ?? []).find((k) => k.id === id);
  function pocetakKlizaca(s, k) {
    const vals = vrijednostiKlizaca3(k);
    if (isNum(k.pocetak)) return k.pocetak;
    const si = s[k.id];
    if (isNum(si)) {
      const x = si / faktor(k);
      return vals.reduce(
        (a, b) => Math.abs(b - x) < Math.abs(a - x) ? b : a,
        vals[0]
      );
    }
    return vals[0];
  }
  var krug = (s) => s.objekt === "cestica" && OKOMITO.includes(s.smjer_B);
  var rad3 = (d) => d * Math.PI / 180;
  function velicine2(s, o = {}) {
    const v = o.v ?? s.v;
    const B = o.B ?? s.B;
    const kut = o.kut ?? s.kut ?? 90;
    if (s.objekt === "vodic") {
      const I = o.I ?? s.I;
      const l = s.l * { m: 1, cm: 0.01, mm: 1e-3 }[s.jedinica_l ?? "m"];
      return { F: B * I * l * Math.sin(rad3(kut)), B, I, l, kut };
    }
    const c = cesticaOf(s);
    const aq = Math.abs(c.q);
    return {
      F: aq * v * B * Math.sin(rad3(kut)),
      r: c.m * v / (aq * B),
      T: 2 * Math.PI * c.m / (aq * B),
      v,
      B,
      kut,
      m: c.m,
      q: c.q
    };
  }
  var doskok = (s, o) => 2 * velicine2(s, o).r;
  var pogodiTol = (s) => (isNum(s.tolerancija) ? s.tolerancija : 0.03) * s.detektor;
  function pogodiRjesenja(s) {
    const k = klizacOf(s, s.mijenja);
    if (!k) return [];
    return vrijednostiKlizaca3(k).filter(
      (x) => Math.abs(doskok(s, { [s.mijenja]: x * faktor(k) }) - s.detektor) <= pogodiTol(s)
    );
  }
  var JED_ODG = {
    r: { m: 1, cm: 0.01, mm: 1e-3 },
    F: { N: 1, mN: 1e-3 },
    T: { s: 1, ms: 1e-3, µs: 1e-6, ns: 1e-9 }
  };
  function izracun2(s) {
    const vel = s.pitaj.velicina;
    const jed2 = s.pitaj.jedinica ?? { r: "m", F: "N", T: "s" }[vel];
    const f2 = 1 / JED_ODG[vel][jed2];
    const x = velicine2(s);
    const k = [];
    const add2 = (vrijednost3, poruka2) => k.push({ vrijednost: vrijednost3 * f2, poruka: poruka2 });
    const jB = s.jedinica_B && s.jedinica_B !== "T" ? JEDINICE2.B[s.jedinica_B] : null;
    if (s.objekt === "vodic") {
      if ((s.jedinica_l ?? "m") !== "m")
        add2(
          x.B * x.I * s.l * Math.sin(rad3(x.kut)),
          `Duljinu vodiča pretvori u metre: ${withUnit(s.l, s.jedinica_l)} = ${withUnit(x.l, "m")}.`
        );
      if (Math.abs(Math.sin(rad3(x.kut)) - Math.cos(rad3(x.kut))) > 1e-9)
        add2(
          x.B * x.I * x.l * Math.cos(rad3(x.kut)),
          "Uzeo si kosinus. U F = BIl sin α kut α je između vodiča i silnica."
        );
      if (Math.abs(Math.sin(rad3(x.kut)) - 1) > 1e-9)
        add2(
          x.B * x.I * x.l,
          x.kut === 0 ? "Vodič leži uzduž silnica: α = 0°, a sin 0° = 0, pa sile nema." : "Izostavio si sin α: vodič nije okomit na silnice."
        );
      if (jB)
        add2(
          x.B / jB * x.I * x.l * Math.sin(rad3(x.kut)),
          `Polje pretvori u tesle: 1 ${s.jedinica_B} = ${formatSci(jB)} T.`
        );
      return { vrijednost: x.F * f2, jedinica: jed2, oznaka: "F", krivo: k };
    }
    const c = cesticaOf(s);
    const aq = Math.abs(c.q);
    const druga = s.cestica === "elektron" ? MP : s.cestica === "proton" ? ME : null;
    if (vel === "F") {
      if (Math.abs(Math.sin(rad3(x.kut)) - 1) > 1e-9)
        add2(
          aq * x.v * x.B,
          x.kut === 0 ? "Čestica se giba uzduž silnica: α = 0°, a sin 0° = 0, pa sile nema." : "Izostavio si sin α: brzina nije okomita na silnice."
        );
      add2(
        x.v * x.B * Math.sin(rad3(x.kut)),
        "Zaboravio si naboj: F = q·v·B·sin α, a q = 1,6·10⁻¹⁹ C."
      );
      if (jB)
        add2(
          aq * x.v * (x.B / jB) * Math.sin(rad3(x.kut)),
          `Polje pretvori u tesle: 1 ${s.jedinica_B} = ${formatSci(jB)} T.`
        );
      return { vrijednost: x.F * f2, jedinica: jed2, oznaka: "F", krivo: k };
    }
    if (vel === "r") {
      if (druga)
        add2(
          druga * x.v / (aq * x.B),
          `To je polumjer za ${s.cestica === "elektron" ? "proton" : "elektron"}: uzmi masu ${s.cestica === "elektron" ? "elektrona, 9,11·10⁻³¹ kg" : "protona, 1,67·10⁻²⁷ kg"}.`
        );
      add2(
        c.m * x.v * x.v / (aq * x.B),
        "Pokrati jedan v: iz qvB = mv²/r slijedi r = mv/(qB)."
      );
      add2(aq * x.B / (c.m * x.v), "Obrnuo si razlomak: r = mv/(qB).");
      if (jed2 === "m")
        k.push({
          vrijednost: x.r * 100,
          poruka: "To je polumjer u centimetrima, a odgovor treba upisati u metrima."
        });
      if (jB)
        add2(
          c.m * x.v / (aq * (x.B / jB)),
          `Polje pretvori u tesle: 1 ${s.jedinica_B} = ${formatSci(jB)} T.`
        );
      return { vrijednost: x.r * f2, jedinica: jed2, oznaka: "r", krivo: k };
    }
    if (druga)
      add2(
        2 * Math.PI * druga / (aq * x.B),
        `To je period za ${s.cestica === "elektron" ? "proton" : "elektron"}: uzmi njegovu masu.`
      );
    add2(c.m / (aq * x.B), "Zaboravio si 2π: T = 2πr/v = 2πm/(qB).");
    if (jB)
      add2(
        2 * Math.PI * c.m / (aq * (x.B / jB)),
        `Polje pretvori u tesle: 1 ${s.jedinica_B} = ${formatSci(jB)} T.`
      );
    return { vrijednost: x.T * f2, jedinica: jed2, oznaka: "T", krivo: k };
  }
  var tolIzr = (s, a) => isNum(s.tolerancija) ? s.tolerancija : Math.max(1e-30, Math.abs(a) * 0.01);
  var bez2 = (krivo, a) => krivo.filter(
    (k) => Number.isFinite(k.vrijednost) && Math.abs(k.vrijednost - a) > Math.max(Math.abs(a) * 0.011, Math.abs(k.vrijednost) * 1e-6)
  );
  var NACINI15 = {
    cestica: ["istrazi", "pogodi", "izracunaj"],
    vodic: ["istrazi", "izracunaj"]
  };
  var POLJA2 = {
    cestica: [
      "cestica",
      "v",
      "B",
      "smjer_B",
      "kut",
      "jedinica_B",
      "klizaci",
      "detektor",
      "mijenja",
      "pitaj"
    ],
    vodic: ["B", "I", "l", "jedinica_l", "kut", "jedinica_B", "klizaci", "pitaj"]
  };
  var OPCE3 = [
    "tip",
    "objekt",
    "nacin",
    "tekst",
    "krivo",
    "savjet",
    "tolerancija"
  ];
  var naboj_u_polju = {
    tip: "naboj_u_polju",
    samoRucno: true,
    uloga: (s) => s?.nacin === "istrazi" ? POGLEDAJ : ODGOVOR,
    istrazi: (s) => s?.nacin === "istrazi",
    opis: `Naboj ili vodič u homogenom magnetskom polju. Polja: tekst, objekt ("cestica" | "vodic"), nacin.
- objekt "cestica": cestica ("proton" | "elektron" | "alfa" | {ime?, oznaka?, m (kg), q (C, s predznakom)}), v (m/s), B (T), jedinica_B? (u kojoj je B zadan u tekstu, npr. "mT": dodaje grešku „nepretvoreno“),
  smjer_B: "u_papir" | "iz_papira" (polje okomito na papir: čestica kruži, crta se kružnica r = mv/(|q|B) u mjerilu, sila prema središtu, period)
  ili "desno" (polje u ravnini papira: v je pod kutom kut (°) prema B, sila ⊙/⊗ iznosa |q|vB sin α).
- objekt "vodic": B (T), I (A), l, jedinica_l? ("m" | "cm"), kut (° između vodiča i silnica): F = BIl sin α; vodič se može zakretati.
- klizaci?: [{id: "v" | "B" | "kut" | "I", min, max, korak ili vrijednosti: [..], jedinica? (B: T | mT | µT, v: m/s | km/s), pocetak?}] (najviše 3; vrijednosti su u jedinici klizača).
nacin:
- "istrazi": korak za gledanje: učenik pomiče klizače (i zakreće vodič), sve se računa i crta uživo;
- "pogodi" (samo cestica s poljem okomitim na papir): detektor (m, udaljenost od ulaza), mijenja ("B" | "v", id klizača koji učenik namješta);
  čestica nakon polukruga mora pasti na detektor (|2r − d| ≤ tolerancija·d, zadano 3 %); r se ne prikazuje, učenik ga mora procijeniti ili izračunati;
- "izracunaj": pitaj: {velicina: "r" | "F" | "T" (vodič: "F"), jedinica?}; točan iznos i tipične greške (masa protona umjesto elektrona, v², bez sin α,
  nepretvoreni mT ili cm, kosinus) računa aplikacija; ${KRIVO_OPIS}; tolerancija? (zadano 1 %).`,
    primjer: {
      tip: "naboj_u_polju",
      objekt: "cestica",
      nacin: "izracunaj",
      tekst: "Elektron brzine $2 \\cdot 10^{6}$ m/s ulijeće okomito u polje $1 \\cdot 10^{-3}$ T. Koliki je polumjer kružnice?",
      cestica: "elektron",
      v: 2e6,
      B: 1e-3,
      smjer_B: "u_papir",
      pitaj: { velicina: "r", jedinica: "m" }
    },
    primjeri: [
      {
        tip: "naboj_u_polju",
        objekt: "cestica",
        nacin: "istrazi",
        tekst: "Mijenjaj brzinu i polje i gledaj kako se mijenjaju polumjer i period kruženja elektrona.",
        cestica: "elektron",
        v: 2e6,
        B: 1e-3,
        smjer_B: "u_papir",
        klizaci: [
          { id: "v", vrijednosti: [1e6, 2e6, 3e6, 5e6], jedinica: "m/s" },
          { id: "B", min: 0.5, max: 3, korak: 0.5, jedinica: "mT" }
        ]
      },
      {
        tip: "naboj_u_polju",
        objekt: "cestica",
        nacin: "pogodi",
        tekst: "Elektron brzine $2 \\cdot 10^{6}$ m/s ulijeće u polje. Namjesti polje $B$ tako da elektron nakon polukruga pogodi detektor udaljen 4,56 cm od ulaza.",
        cestica: "elektron",
        v: 2e6,
        B: 1e-3,
        smjer_B: "u_papir",
        detektor: 0.0456,
        mijenja: "B",
        klizaci: [
          { id: "B", min: 0.1, max: 2, korak: 0.1, jedinica: "mT", pocetak: 1 }
        ]
      },
      {
        tip: "naboj_u_polju",
        objekt: "cestica",
        nacin: "istrazi",
        tekst: "Mijenjaj kut između brzine protona i silnica. Kolika je sila kad se proton giba uzduž silnica?",
        cestica: "proton",
        v: 2e6,
        B: 0.5,
        smjer_B: "desno",
        kut: 90,
        klizaci: [{ id: "kut", min: 0, max: 180, korak: 15, jedinica: "°" }]
      },
      {
        tip: "naboj_u_polju",
        objekt: "cestica",
        nacin: "izracunaj",
        tekst: "Kolika sila djeluje na proton koji se brzinom $2 \\cdot 10^{6}$ m/s giba okomito na silnice polja 0,5 T?",
        cestica: "proton",
        v: 2e6,
        B: 0.5,
        smjer_B: "desno",
        kut: 90,
        pitaj: { velicina: "F" }
      },
      {
        tip: "naboj_u_polju",
        objekt: "vodic",
        nacin: "istrazi",
        tekst: "Zakreći vodič (povuci mu kraj ili pomakni klizač) i gledaj silu. Kad sila nestaje?",
        B: 0.4,
        I: 5,
        l: 20,
        jedinica_l: "cm",
        kut: 90,
        klizaci: [{ id: "kut", min: 0, max: 180, korak: 5, jedinica: "°" }]
      },
      {
        tip: "naboj_u_polju",
        objekt: "vodic",
        nacin: "izracunaj",
        tekst: "Vodič duljine 20 cm sa strujom 5 A okomit je na silnice polja 0,4 T. Kolika sila djeluje na njega?",
        B: 0.4,
        I: 5,
        l: 20,
        jedinica_l: "cm",
        kut: 90,
        pitaj: { velicina: "F" }
      }
    ],
    validate(s) {
      const { list, need } = problems();
      need(isText(s.tekst), "naboj_u_polju treba „tekst“");
      if (!need(
        ["cestica", "vodic"].includes(s.objekt),
        `objekt mora biti "cestica" ili "vodic"`
      ))
        return list;
      if (!need(
        NACINI15[s.objekt].includes(s.nacin),
        `za objekt „${s.objekt}“ nacin mora biti: ${NACINI15[s.objekt].join(", ")}`
      ))
        return list;
      for (const k of Object.keys(s))
        need(
          [...OPCE3, ...POLJA2[s.objekt]].includes(k),
          `naboj_u_polju (${s.objekt}): nepoznato polje „${k}“`
        );
      const ist = s.nacin === "istrazi";
      if (ist) noAnswerFields(s, need);
      const broj4 = (k, lo, hi, opis) => need(
        isNum(s[k]) && s[k] > lo && s[k] <= hi,
        `${k} mora biti broj (${opis})`
      );
      if (s.objekt === "cestica") {
        if (typeof s.cestica === "string")
          need(
            s.cestica in CESTICE2,
            `cestica mora biti jedna od: ${Object.keys(CESTICE2).join(", ")} ili {m, q}`
          );
        else
          need(
            s.cestica && isNum(s.cestica.m) && s.cestica.m > 0 && isNum(s.cestica.q) && s.cestica.q !== 0,
            "cestica: {ime?, oznaka?, m (kg) > 0, q (C) ≠ 0}"
          );
        broj4("v", 0, 3e8, "m/s, 0 < v < c");
        broj4("B", 0, 100, "T, veći od 0");
        need(
          [...OKOMITO, "desno"].includes(s.smjer_B),
          `smjer_B mora biti "u_papir", "iz_papira" ili "desno"`
        );
        if (OKOMITO.includes(s.smjer_B))
          need(
            s.kut == null || s.kut === 90,
            "kad je polje okomito na papir, brzina je okomita na polje (kut 90° ili bez kuta)"
          );
        else
          need(
            isNum(s.kut) && s.kut >= 0 && s.kut <= 180,
            "uz polje u ravnini papira treba kut (0–180°) između brzine i silnica"
          );
      } else {
        broj4("B", 0, 100, "T, veći od 0");
        broj4("I", 0, 1e4, "A, veća od 0");
        broj4("l", 0, 1e4, "duljina vodiča u polju");
        if (s.jedinica_l != null)
          need(
            ["m", "cm", "mm"].includes(s.jedinica_l),
            "jedinica_l mora biti m, cm ili mm"
          );
        need(
          isNum(s.kut) && s.kut >= 0 && s.kut <= 180,
          "kut (0–180°) između vodiča i silnica"
        );
      }
      if (s.jedinica_B != null)
        need(s.jedinica_B in JEDINICE2.B, "jedinica_B mora biti T, mT ili µT");
      const dop = s.objekt === "vodic" ? ["kut", "I", "B"] : krug(s) ? ["v", "B"] : ["kut", "v", "B"];
      if (s.klizaci != null && need(
        Array.isArray(s.klizaci) && s.klizaci.length >= 1 && s.klizaci.length <= 3,
        "klizaci: 1–3 klizača"
      )) {
        const ids = s.klizaci.map((k) => k?.id);
        need(
          new Set(ids).size === ids.length,
          "id-jevi klizača moraju biti različiti"
        );
        s.klizaci.forEach((k, i) => {
          if (!need(
            dop.includes(k?.id),
            `klizaci[${i}].id mora biti jedan od: ${dop.join(", ")}`
          ))
            return;
          need(
            k.jedinica == null || k.jedinica in JEDINICE2[k.id],
            `klizaci[${i}].jedinica za ${k.id} mora biti: ${Object.keys(JEDINICE2[k.id]).join(", ")}`
          );
          if (Array.isArray(k.vrijednosti))
            need(
              k.vrijednosti.length >= 2 && k.vrijednosti.length <= 30 && k.vrijednosti.every((x) => isNum(x) && x >= 0) && k.vrijednosti.every(
                (x, j) => j === 0 || x > k.vrijednosti[j - 1]
              ),
              `klizaci[${i}].vrijednosti: 2–30 rastućih brojeva`
            );
          else if (need(
            [k.min, k.max, k.korak].every(isNum) && k.max > k.min && k.korak > 0 && (k.max - k.min) / k.korak <= 400,
            `klizaci[${i}] treba min < max i korak > 0 (najviše 400 koraka) ili vrijednosti`
          ))
            need(
              Math.abs(
                (k.max - k.min) / k.korak - Math.round((k.max - k.min) / k.korak)
              ) < 1e-6,
              `klizaci[${i}]: (max − min) mora biti višekratnik koraka`
            );
          if (k.id === "kut")
            need(
              vrijednostiKlizaca3(k).every((x) => x >= 0 && x <= 180),
              `klizaci[${i}]: kut je od 0° do 180°`
            );
          if (k.id !== "kut" && list.length === 0)
            need(
              vrijednostiKlizaca3(k).every((x) => x > 0),
              `klizaci[${i}]: ${k.id} mora biti veći od 0`
            );
          if (k.pocetak != null)
            need(
              vrijednostiKlizaca3(k).some(
                (x) => Math.abs(x - k.pocetak) < 1e-9 * Math.max(1, Math.abs(x))
              ),
              `klizaci[${i}].pocetak mora biti jedna od vrijednosti klizača`
            );
          for (const key of Object.keys(k))
            need(
              [
                "id",
                "min",
                "max",
                "korak",
                "vrijednosti",
                "jedinica",
                "pocetak",
                "oznaka"
              ].includes(key),
              `klizaci[${i}] nema polje „${key}“`
            );
        });
      }
      if (s.nacin === "pogodi") {
        need(
          krug(s),
          "pogodi radi samo kad je polje okomito na papir (smjer_B u_papir ili iz_papira)"
        );
        broj4("detektor", 0, 100, "udaljenost detektora od ulaza u m");
        if (need(
          ["B", "v"].includes(s.mijenja) && klizacOf(s, s.mijenja),
          `mijenja mora biti "B" ili "v", a klizač s tim id-jem mora postojati`
        ) && !list.length) {
          const k = klizacOf(s, s.mijenja);
          const rj = pogodiRjesenja(s);
          need(
            rj.length >= 1,
            "nijedan položaj klizača ne pogađa detektor; promijeni detektor ili raspon klizača"
          );
          need(
            rj.length <= 2,
            "detektor se pogađa s previše položaja klizača; smanji toleranciju ili korak"
          );
          const p = pocetakKlizaca(s, k);
          need(
            !rj.some((x) => Math.abs(x - p) < 1e-12 * Math.max(1, x)),
            "početni položaj klizača već pogađa detektor"
          );
        }
        if (s.tolerancija != null)
          need(
            isNum(s.tolerancija) && s.tolerancija > 0 && s.tolerancija <= 0.2,
            "tolerancija je udio udaljenosti detektora (npr. 0.03)"
          );
      } else {
        need(
          s.detektor == null && s.mijenja == null,
          "detektor i mijenja postoje samo u načinu pogodi"
        );
      }
      if (s.nacin === "izracunaj") {
        const vel = s.objekt === "vodic" ? ["F"] : krug(s) ? ["r", "F", "T"] : ["F"];
        if (need(
          s.pitaj && vel.includes(s.pitaj.velicina),
          `pitaj: {velicina: ${vel.join(" | ")}, jedinica?}`
        )) {
          need(
            s.pitaj.jedinica == null || s.pitaj.jedinica in JED_ODG[s.pitaj.velicina],
            `pitaj.jedinica za ${s.pitaj.velicina} mora biti: ${Object.keys(JED_ODG[s.pitaj.velicina]).join(", ")}`
          );
          need(
            Object.keys(s.pitaj).every(
              (k) => ["velicina", "jedinica"].includes(k)
            ),
            "pitaj ima samo velicina i jedinica"
          );
          if (!list.length) {
            const r = izracun2(s);
            need(Number.isFinite(r.vrijednost), "rezultat se ne može izračunati");
            validateKrivo(s.krivo, r.vrijednost, tolIzr(s, r.vrijednost), need);
          }
        }
        need(
          s.klizaci == null,
          "u načinu izracunaj nema klizača (vrijednosti su zadane)"
        );
      } else need(s.pitaj == null, "pitaj postoji samo u načinu izracunaj");
      if (s.nacin !== "izracunaj")
        for (const k of ["savjet", ...ist ? [] : ["krivo"]])
          need(s[k] == null, `polje „${k}“ ima smisla samo u načinu izracunaj`);
      return list;
    },
    check(s, odgovor3) {
      if (s.nacin === "pogodi") {
        if (!isNum(odgovor3) || odgovor3 <= 0)
          return { tocno: false, poruka: "Namjesti klizač.", prazno: true };
        const d = doskok(s, { [s.mijenja]: odgovor3 });
        if (Math.abs(d - s.detektor) <= pogodiTol(s)) return { tocno: true };
        const predaleko = d > s.detektor;
        const savjet3 = s.mijenja === "B" ? predaleko ? "Jače polje jače savija putanju: veće B → manji krug." : "Slabije polje manje savija putanju: manje B → veći krug." : predaleko ? "Sporija čestica kruži po manjoj kružnici: manja v → manji krug." : "Brža čestica kruži po većoj kružnici: veća v → veći krug.";
        return {
          tocno: false,
          poruka: `Čestica je pala ${predaleko ? "iza" : "ispred"} detektora: na ${withUnit(Number((d * 100).toPrecision(3)), "cm")} od ulaza, a detektor je na ${withUnit(Number((s.detektor * 100).toPrecision(3)), "cm")}. ${savjet3} Pomoć: pada na udaljenosti 2r, a r = mv/(|q|B).`
        };
      }
      if (s.nacin === "izracunaj") {
        const r = izracun2(s);
        return checkFiz({
          input: odgovor3,
          answer: r.vrijednost,
          tol: tolIzr(s, r.vrijednost),
          unit: r.jedinica,
          krivo: [...s.krivo ?? [], ...bez2(r.krivo, r.vrijednost)],
          savjet: s.savjet
        });
      }
      return { tocno: false, poruka: "Ovaj korak nema provjeru." };
    },
    tocanOdgovor(s) {
      if (s.nacin === "pogodi") {
        const k = klizacOf(s, s.mijenja);
        return pogodiRjesenja(s).map((x) => `${s.mijenja} = ${formatSci(x)} ${jedKlizaca(k)}`).join(" ili ");
      }
      if (s.nacin === "izracunaj") {
        const r = izracun2(s);
        return `${r.oznaka} = ${formatSci(r.vrijednost)} ${r.jedinica}`;
      }
      return "";
    },
    brojcaniOdgovor: (s) => s?.nacin === "izracunaj" ? izracun2(s).vrijednost : null,
    // za komponentu: pogodi šalje vrijednost klizača u SI
    uSI: (k, x) => x * faktor(k)
  };

  // src/lib/lesson/katalog/fizika/index.js
  var FIZIKA = [gibanje, crtaj_graf, sile_tijelo, vektori, kruzenje, sudar, energija_stupci, tlak_dubina, cijev, krivulja_zagrijavanja, plinski_dijagram, plin_posuda, naboji_polje, ploce_polje, strujni_krug, smjer_u_polju, indukcija, naboj_u_polju];

  // src/lib/lesson/katalog/index.js
  var KATALOG = Object.fromEntries(
    [pojam, postupak, skica, klizac, broj, izbor, vise_tocnih, tocno_netocno, praznina, parovi, redoslijed, razvrstaj, mjerilo, mreza, ocitaj_graf, pretvorba_jedinica, cestice, uronjeno, izraz, interval, graf_funkcije, ...FIZIKA].map((w) => [w.tip, w])
  );
  var ZA_PROMPT = Object.fromEntries(Object.entries(KATALOG).filter(([, w]) => !w.samoRucno));
  var widget = (spec) => KATALOG[spec?.tip];
  var uloga = (spec) => widget(spec)?.uloga(spec);
  var istrazuje = (spec) => uloga(spec) === POGLEDAJ && !!widget(spec)?.istrazi?.(spec);
  var tocanOdgovor3 = (spec) => widget(spec)?.tocanOdgovor?.(spec) ?? "";

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
    const jed2 = [...new Set((v.jedinice ?? []).map(normUnit))].filter((u) => unitIndex(v.velicina, u) >= 0).sort((a, b) => unitIndex(v.velicina, a) - unitIndex(v.velicina, b));
    if (jed2.length < 2) return [];
    const n = Math.min(MAX_ZADATAKA, Math.max(MIN_ZADATAKA, v.zadataka ?? 6));
    const vrste = ["jedna", "jedna", ...v.lanac && jed2.length >= 3 ? ["lanac"] : [], ...v.mjesovito ? ["mjesovito"] : []];
    const out = [];
    const vidjeno = /* @__PURE__ */ new Set();
    for (let pokusaj = 0; out.length < n && pokusaj < 400; pokusaj++) {
      const vrsta = out.length < 2 ? "jedna" : vrste[Math.floor(rand() * vrste.length)];
      const z = sastavi(v.velicina, jed2, vrsta, rand);
      if (!z) continue;
      const kljuc = JSON.stringify([z.vrijednost, z.od, z.do]);
      if (vidjeno.has(kljuc)) continue;
      vidjeno.add(kljuc);
      out.push({ tip: "pretvorba_jedinica", velicina: v.velicina, ...z, tekst: "" });
    }
    return out;
  }
  function sastavi(velicina3, jed2, vrsta, rand) {
    const pick = (arr) => arr[Math.floor(rand() * arr.length)];
    if (vrsta === "lanac") {
      const duljina4 = jed2.length >= 4 && rand() < 0.5 ? 4 : 3;
      const start = Math.floor(rand() * (jed2.length - duljina4 + 1));
      const niz = jed2.slice(start, start + duljina4);
      if (rand() < 0.5) niz.reverse();
      const vrijednost4 = lijepBroj(rand);
      const base = toBase(velicina3, vrijednost4, niz[0]);
      const rez2 = niz.slice(1).map((u) => fromBase(velicina3, base, u));
      return rez2.every(lijepRezultat) ? { vrijednost: vrijednost4, od: niz[0], do: niz.slice(1) } : null;
    }
    if (vrsta === "mjesovito") {
      const i2 = Math.floor(rand() * (jed2.length - 1));
      const [veca, manja] = [jed2[i2], jed2[i2 + 1]];
      const omjer = Math.round(toBase(velicina3, 1, veca) / toBase(velicina3, 1, manja));
      if (!(omjer >= 10 && omjer <= 1e3)) return null;
      const a = 1 + Math.floor(rand() * 9);
      const b = 1 + Math.floor(rand() * (omjer - 1));
      const cilj2 = rand() < 0.6 || !jed2[i2 + 2] ? manja : jed2[i2 + 2];
      const rez2 = fromBase(velicina3, toBase(velicina3, [a, b], [veca, manja]), cilj2);
      return lijepRezultat(rez2) ? { vrijednost: [a, b], od: [veca, manja], do: cilj2 } : null;
    }
    const i = Math.floor(rand() * jed2.length);
    const cilj = pick(jed2.filter((u, j) => j !== i && Math.abs(j - i) <= 2));
    const od = jed2[i];
    const vrijednost3 = lijepBroj(rand);
    const rez = fromBase(velicina3, toBase(velicina3, vrijednost3, od), cilj);
    return lijepRezultat(rez) ? { vrijednost: vrijednost3, od, do: cilj } : null;
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
    if (!err(
      lekcija && typeof lekcija === "object",
      "lekcija mora biti JSON objekt"
    ))
      return { greske, upozorenja };
    err(lekcija.verzija === VERZIJA, `verzija mora biti ${VERZIJA}`);
    const text = allText(lekcija);
    const z = lekcija.zadatak;
    if (err(z && typeof z === "object", "nedostaje „zadatak“")) {
      err(isText(z.tekst), "zadatak.tekst mora opisati situaciju i podatke");
      if (z.skica != null)
        sketchProblems(
          z.skica,
          { text },
          (ok, msg) => err(ok, `zadatak.skica: ${msg}`)
        );
      if (z.graf != null) {
        if (err(
          z.graf?.tip === "graf_funkcije" && z.graf.nacin === "prikaz",
          'zadatak.graf mora biti graf_funkcije s nacin "prikaz"'
        )) {
          for (const p of KATALOG.graf_funkcije.validate(z.graf))
            greske.push(`zadatak.graf: ${p}`);
        }
      }
    }
    const pz = lekcija.podzadaci;
    if (err(
      Array.isArray(pz) && pz.length >= 1 && pz.length <= 3,
      "lekcija treba 1–3 podzadatka"
    )) {
      const ids = pz.map((p) => p?.id);
      err(
        new Set(ids).size === ids.length,
        "id-jevi podzadataka moraju biti različiti"
      );
      pz.forEach((p, i) => {
        const at = `podzadaci[${i}]${p?.id ? ` (${p.id})` : ""}`;
        err(
          ["a", "b", "c"].includes(p?.id),
          `${at}.id mora biti "a", "b" ili "c"`
        );
        err(isText(p?.naslov), `${at} treba kratak naslov`);
        err(
          isText(p?.zasto),
          `${at} treba „zasto“: 1–2 rečenice za učenika koji je odmah točno odgovorio`
        );
        err(
          isText(p?.objasnjenje),
          `${at} treba „objasnjenje“ (cijelo objašnjenje, skriveno dok ga učenik ne otvori)`
        );
        if (p?.graf != null && err(
          p.graf?.tip === "graf_funkcije" && p.graf.nacin === "prikaz",
          `${at}.graf mora biti graf_funkcije s nacin "prikaz"`
        )) {
          for (const g of KATALOG.graf_funkcije.validate(p.graf))
            greske.push(`${at}.graf: ${g}`);
        }
        step(p?.pitanje, `${at}.pitanje`, ODGOVOR);
        err(
          !p?.pitanje?.stube,
          `${at}.pitanje: stube jedinica otkrivaju postupak; smiju samo u koraku pomoći`
        );
        if (p?.blizanac != null) {
          blizanac(p.blizanac, p.pitanje, at);
        } else {
          warn(
            false,
            `${at} nema blizanca: nakon pomoći učenik ponovno rješava isto pitanje`
          );
        }
        if (err(
          Array.isArray(p?.pomoc) && p.pomoc.length >= 1 && p.pomoc.length <= MAX_POMOC,
          `${at}.pomoc treba 1–${MAX_POMOC} koraka`
        )) {
          p.pomoc.forEach((k, j) => step(k, `${at}.pomoc[${j}]`));
          warn(
            p.pomoc.some((k) => uloga(k) === ODGOVOR),
            `${at}.pomoc nema nijedan korak „Tvoj red“: pomoć bi trebala prepustiti učeniku barem jedan mali korak`
          );
          warn(
            uloga(p.pomoc[0]) === POGLEDAJ,
            `${at}.pomoc ne počinje objašnjenjem: učenik koji je pogriješio najprije treba vidjeti kako se to radi`
          );
        }
        const r = rezultati?.[p?.id];
        if (r != null && p?.pitanje) {
          const a = widget(p.pitanje)?.brojcaniOdgovor?.(p.pitanje);
          if (isNum(a))
            warn(
              numbersIn(String(r)).some(
                (n) => Math.abs(n - a) <= (a === 0 ? 1e-9 : Math.abs(a) * 5e-3)
              ),
              `${at}: točan odgovor ${a} ne pojavljuje se u rezultatima izvornog zadatka („${r}“); provjeri je li pitanje isto`
            );
        }
      });
    }
    const c = lekcija.cijeli;
    if (c != null && err(typeof c === "object", "cijeli mora biti objekt { pitanje, blizanac, blizanacRjesenje }")) {
      step(c.pitanje, "cijeli.pitanje", ODGOVOR);
      err(!c.pitanje?.stube, "cijeli.pitanje: stube jedinica otkrivaju postupak; cijeli zadatak učenik rješava sam");
      if (err(c.blizanac != null, "cijeli treba „blizanac“: sličan cijeli zadatak koji učenik na kraju rješava sam, bez koraka")) {
        blizanac(c.blizanac, c.pitanje, "cijeli");
        err(isText(c.blizanacRjesenje), "cijeli treba „blizanacRjesenje“: kratko rješenje sličnog zadatka, za učenika koji ga ne riješi");
      }
      err(Array.isArray(pz) && pz.length >= 2, "lekcija s „cijeli“ treba barem 2 podzadatka: oni su koraci za učenika koji ne zna riješiti cijeli zadatak");
      for (const u of prepisivo(c.pitanje, z?.tekst)) warn(false, `cijeli.pitanje: ${u}`);
      for (const u of prepisivo(c.blizanac, c.blizanac?.tekst)) warn(false, `cijeli.blizanac: ${u}`);
      const rez = rezultati && typeof rezultati === "object" ? Object.values(rezultati).flatMap((r) => typeof r === "string" || typeof r === "number" ? numbersIn(String(r)) : []) : [];
      const odg = answerNumbers(c.pitanje);
      if (rez.length && odg.length) {
        warn(odg.some((a) => rez.some((n) => Math.abs(n - a) <= (a === 0 ? 1e-9 : Math.abs(a) * 5e-3))), `cijeli.pitanje: točan odgovor (${odg.map(formatNumber2).join("; ")}) ne pojavljuje se u rezultatima izvornog zadatka; provjeri je li to konačni odgovor`);
      }
    }
    for (const oblik of genderForms(text)) err(false, `„${oblik}“: učeniku se obraćamo u muškom rodu („zbrojio si“), bez ženskih i dvojnih oblika`);
    if (Array.isArray(pz)) {
      sourceSketch(lekcija, parseMaybe(skica2), err);
      sourceResults(pz, rezultati, err, warn);
    }
    if (lekcija.vjezba != null)
      for (const p of validateVjezba(lekcija.vjezba)) greske.push(p);
    if (err(
      Array.isArray(lekcija.zapamti) && lekcija.zapamti.length >= 1 && lekcija.zapamti.length <= 4,
      "zapamti treba 1–4 kratke natuknice"
    )) {
      lekcija.zapamti.forEach(
        (t, i) => err(isText(t), `zapamti[${i}] mora biti tekst`)
      );
    }
    function blizanac(b, pitanje, at) {
      step(b, `${at}.blizanac`, ODGOVOR);
      err(JSON.stringify(b) !== JSON.stringify(pitanje), `${at}.blizanac je isti kao pitanje; treba nove brojeve ili novu situaciju`);
      warn(b?.tip === pitanje?.tip, `${at}.blizanac je drugog tipa nego pitanje; blizanac provjerava istu vještinu`);
      const kljuc = widget(b)?.kljucOdgovora;
      if (kljuc && b.tip === pitanje?.tip) {
        err(kljuc(b) == null || kljuc(b) !== kljuc(pitanje), `${at}.blizanac ima isti odgovor kao pitanje, pa bi ga učenik samo prepisao; odaberi brojeve s kojima i rezultat bude drukčiji`);
      }
      const isti2 = quantityAnswers(b);
      err(
        !isti2.length || !isti2.every((x) => quantityAnswers(pitanje).some((a) => Math.abs(a - x) <= (a === 0 ? 1e-9 : Math.abs(a) * 5e-3))),
        `${at}.blizanac ima isti rezultat kao pitanje (${isti2.map(formatNumber2).join("; ")}), pa bi ga učenik samo prepisao; odaberi brojeve s kojima i rezultat bude drukčiji`
      );
    }
    function step(spec, at, mora) {
      if (!err(
        spec && typeof spec === "object" && KATALOG[spec.tip],
        `${at}: nepoznat ili nedostaje tip widgeta („${spec?.tip}“); dopušteni: ${Object.keys(ZA_PROMPT).join(", ")}`
      ))
        return;
      const role = uloga(spec);
      if (mora === ODGOVOR)
        err(
          role === ODGOVOR,
          `${at} mora biti korak s odgovorom („Tvoj red“), a ${spec.tip}${spec.nacin ? ` (${spec.nacin})` : ""} je korak za gledanje`
        );
      for (const p of KATALOG[spec.tip].validate(spec, { text }))
        greske.push(`${at} (${spec.tip}): ${p}`);
      if (istrazuje(spec))
        warn(
          /istraž|pomi|dodir|povuci|mijenjaj|igraj/i.test(spec.tekst ?? ""),
          `${at}: korak za istraživanje neka u tekstu kaže što učenik može dirati`
        );
    }
    return { greske, upozorenja };
  }
  function sourceSketch(lekcija, izvor2, err) {
    if (!izvor2?.tip || lekcija.zadatak?.skica) return;
    const pitanja = lekcija.podzadaci.map((p) => p?.pitanje).filter(Boolean);
    if (izvor2.tip === "ljestvica") {
      err(
        pitanja.some(
          (q) => q.tip === "mjerilo" && q.nacin !== "istrazi" && (!izvor2.instrument || q.instrument === izvor2.instrument)
        ),
        `izvorni zadatak očitava se s instrumenta (${izvor2.instrument ?? "ljestvica"}), a lekcija ga ne prikazuje: neka očitanje bude pitanje (mjerilo "procitaj") ili stavi ljestvicu u zadatak.skica`
      );
    } else if (!(izvor2.tip === "graf" && pitanja.some((q) => q.tip === "ocitaj_graf"))) {
      err(
        false,
        `izvorni zadatak ima skicu (${izvor2.tip}), a lekcija je nema: učenik ne vidi lik. Stavi istu skicu u zadatak.skica (izostavi samo oznaku koju učenik treba izračunati)`
      );
    }
  }
  function sourceResults(pz, rezultati, err, warn) {
    if (!rezultati || typeof rezultati !== "object") return;
    const odgovori = pz.flatMap((p) => answerNumbers(p?.pitanje));
    const odgovoreno = (n) => odgovori.some(
      (a) => Math.abs(a - n) <= (n === 0 ? 1e-9 : Math.abs(n) * 5e-3)
    );
    for (const [id, r] of Object.entries(rezultati)) {
      if (typeof r !== "string" && typeof r !== "number") continue;
      for (const dio of answerParts(String(r))) {
        const brojevi = numbersIn(dio);
        const nedostaje = brojevi.filter((n) => !odgovoreno(n));
        if (!nedostaje.length) continue;
        const poruka2 = `rezultat ${id}) izvornog zadatka („${dio.trim()}“): ${nedostaje.map(formatNumber2).join(", ")} nije točan odgovor nijednog pitanja`;
        if (nedostaje.length === brojevi.length)
          err(
            false,
            `${poruka2}. Učenik taj rezultat mora sam dobiti u pitanju podzadatka, ne samo u pomoći ili u tekstu. Ako podzadatak ima više rezultata, pitanje neka bude praznina s njima`
          );
        else warn(false, `${poruka2}; učenik ga vidi samo u objašnjenju`);
      }
    }
  }
  function prepisivo(spec, tekst) {
    if (spec?.tip !== "izraz" || !Array.isArray(spec.polja) || typeof tekst !== "string") return [];
    const vars = spec.varijable ?? [];
    const formule = [...tekst.matchAll(/\$\$([\s\S]+?)\$\$|\$([^$]+)\$/g)].map((m) => (m[1] ?? m[2]).trim());
    const out = [];
    spec.polja.forEach((p, i) => {
      const tocan = ucitaj(String(p?.odgovor ?? ""), vars);
      if (tocan.greska) return;
      for (const f2 of formule) {
        if (/[=<>]|\\[lg]eq?\b/.test(f2)) continue;
        if (/^-?\d+(?:[.,{}]\d+)*$/.test(f2.replace(/\s+/g, ""))) continue;
        const u = ucitajLatex(f2, vars);
        if (u.greska || !jednako(u, tocan, vars)) continue;
        out.push(`polja[${i}]: izraz $${f2}$ iz teksta ima istu vrijednost kao točan odgovor, pa bi ga učenik mogao samo prepisati; traži rezultat po dijelovima (npr. brojnik i nazivnik) ili u obliku koji se mora izračunati`);
        break;
      }
    });
    return out;
  }
  function answerNumbers(spec) {
    if (!spec || !widget(spec)) return [];
    if (spec.tip === "pretvorba_jedinica")
      return pretvorbaRezultati(spec).filter(isNum);
    if (spec.tip === "praznina")
      return (spec.praznine ?? []).flatMap(
        (p) => isNum(p?.odgovor) ? [p.odgovor] : numbersIn(String(p?.odgovor ?? ""))
      );
    const out = [];
    try {
      const a = widget(spec).brojcaniOdgovor?.(spec);
      if (isNum(a)) out.push(a);
      out.push(...numbersIn(String(tocanOdgovor3(spec))));
    } catch {
    }
    return out;
  }
  function quantityAnswers(spec) {
    if (spec?.tip === "pretvorba_jedinica") return answerNumbers(spec);
    if (spec?.tip === "praznina")
      return (spec.praznine ?? []).filter((p) => p?.jedinica && isNum(p.odgovor)).map((p) => p.odgovor);
    const a = spec?.jedinica && widget(spec) ? widget(spec).brojcaniOdgovor?.(spec) : null;
    return isNum(a) ? [a] : [];
  }
  function answerParts(r) {
    const bezObrazlozenja = (t) => t.split(/\bjer\b|\bzato\b|\(/i)[0];
    const [prvi, ...ostali] = r.split(";");
    return [
      prvi,
      ...ostali.filter((t) => /^\s*(?:[^:\d]{1,30}:\s*)?-?\d/.test(t))
    ].map(bezObrazlozenja);
  }
  function genderForms(tekst) {
    const text = tekst.replace(/https?:\/\/[^\s"')\]]+/g, " ");
    const oblici = [
      ...text.matchAll(/(?<![\p{L}\d])\p{Ll}{3,}\/\p{Ll}{2,}(?![\p{L}\d³²])/gu),
      ...text.matchAll(
        /(?<!\p{L})si \p{L}+la(?!\p{L})|(?<!\p{L})\p{L}+la si(?!\p{L})/gu
      )
    ].map((m) => m[0]).filter((o) => !/\/(?:min|mol|sat|dan|god)$/.test(o));
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
