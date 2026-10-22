// Skice u zadacima (ticket „SVG skice u zadacima“): model uz zadatak napiše samo kratak JSON opis skice,
// a ovaj kod provjeri opis i iz njega izračuna i nacrta SVG. Model nikad ne crta i ne računa koordinate,
// pa su proporcije točne po konstrukciji; provjere hvataju krive brojeve u opisu.
//
// ISTI KOD izvršava backend (GraalJS, matmat-backend: src/main/resources/sketch/taskSketch.js) da odbije
// neispravan opis prije spremanja. Kad se ovdje nešto mijenja, kopija na backendu mora se zamijeniti.
//
// renderSketch(opis, tekst) → { svg, checks }: svg je null ako ijedna provjera ne prođe. tekst = zadatak,
// objašnjenje i rezultati: brojevi s oznaka moraju se pojaviti u njima (inače ih je model izmislio).
// Boje nisu upisane: elementi imaju klase, a boje im daje tema aplikacije (Sketch.svelte).

const W = 380;
const H = 260;
const PAD = 34;

const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const f = (n) => Math.round(n * 10) / 10;
const num = (n) => String(Math.round(n * 1000) / 1000).replace(".", ",");

const line = (a, b, cls = "ln") => `<line class="${cls}" x1="${f(a[0])}" y1="${f(a[1])}" x2="${f(b[0])}" y2="${f(b[1])}"/>`;
const label = (p, t, cls = "lbl", anchor = "middle") =>
  `<text class="${cls}" x="${f(p[0])}" y="${f(p[1])}" text-anchor="${anchor}" dominant-baseline="middle">${esc(t)}</text>`;
const dot = (p, cls = "pt") => `<circle class="${cls}" cx="${f(p[0])}" cy="${f(p[1])}" r="3.5"/>`;
const poly = (pts, cls) => `<polygon class="${cls}" points="${pts.map((p) => p.map(f).join(",")).join(" ")}"/>`;
const arrow = (a, b, cls) =>
  `<line class="arr ${cls}" x1="${f(a[0])}" y1="${f(a[1])}" x2="${f(b[0])}" y2="${f(b[1])}" marker-end="url(#mm-head-${cls})"/>`;

const defs = `<defs>${["acc", "c1", "c2", "c3", "c4"]
  .map((c) => `<marker id="mm-head-${c}" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path class="head ${c}" d="M0,0 L10,5 L0,10 z"/></marker>`)
  .join("")}</defs>`;

const svg = (body, h = H) => `<svg viewBox="0 0 ${W} ${h}" xmlns="http://www.w3.org/2000/svg" role="img">${defs}${body}</svg>`;

// Model koordinate → SVG, s jednakim mjerilom po osima (proporcije ostaju točne).
function fit(points, w = W, h = H, pad = PAD) {
  const xs = points.map((p) => p[0]);
  const ys = points.map((p) => p[1]);
  const [x0, x1, y0, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...ys), Math.max(...ys)];
  const s = Math.min((w - 2 * pad) / (x1 - x0 || 1), (h - 2 * pad) / (y1 - y0 || 1));
  const ox = (w - s * (x1 - x0)) / 2;
  const oy = (h - s * (y1 - y0)) / 2;
  return (p) => [ox + (p[0] - x0) * s, h - (oy + (p[1] - y0) * s)];
}

// Osi s različitim mjerilom po x i y (grafovi veličina: V u cm³, m u g).
function axesFit([x0, x1], [y0, y1]) {
  const l = 48, r = 18, t = 18, b = 34;
  return (p) => [l + ((p[0] - x0) / (x1 - x0)) * (W - l - r), H - b - ((p[1] - y0) / (y1 - y0)) * (H - t - b)];
}

const away = (p, c, d = 16) => {
  const dx = p[0] - c[0];
  const dy = p[1] - c[1];
  const l = Math.hypot(dx, dy) || 1;
  return [p[0] + (dx / l) * d, p[1] + (dy / l) * d];
};
const mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];

// Oznaka stranice: od polovišta okomito prema van, poravnata tako da ne prelazi preko stranice.
function sideLabel(p, q, g, t, cls) {
  const m = mid(p, q);
  let n = [-(q[1] - p[1]), q[0] - p[0]];
  const l = Math.hypot(...n) || 1;
  n = [n[0] / l, n[1] / l];
  if (n[0] * (m[0] - g[0]) + n[1] * (m[1] - g[1]) < 0) n = [-n[0], -n[1]];
  const anchor = n[0] > 0.35 ? "start" : n[0] < -0.35 ? "end" : "middle";
  return label([m[0] + n[0] * 16, m[1] + n[1] * 18], t, cls, anchor);
}

// Lijep korak ljestvice: 1, 2 ili 5 × 10ⁿ, tako da na osi bude 4–10 oznaka.
function niceStep(range) {
  const raw = range / 6;
  const p = Math.pow(10, Math.floor(Math.log10(raw)));
  for (const k of [1, 2, 5, 10]) if (k * p >= raw) return k * p;
  return 10 * p;
}
const ticks = (a, b, step) => {
  const out = [];
  for (let v = Math.ceil(a / step - 1e-9) * step; v <= b + 1e-9; v += step) out.push(Math.round(v / step) * step);
  return out;
};

// Brojevi iz oznaka moraju postojati u tekstu zadatka ili rješenja (inače ih je model izmislio).
function normalize(text) {
  return String(text ?? "")
    .replace(/\\[,;: ]/g, "")
    .replace(/\{,\}/g, ",")
    .replace(/(\d)\.(\d)/g, "$1,$2")
    .replace(/(\d)\s(?=\d{3}\b)/g, "$1");
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
const fail = (text) => ({ ok: false, text });
const positive = (...vs) => vs.every((v) => typeof v === "number" && isFinite(v) && v > 0);

// --- geometrija ---

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
    const u = (p) => { const d = [p[0] - pC[0], p[1] - pC[1]]; const l = Math.hypot(...d); return [d[0] / l * 14, d[1] / l * 14]; };
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
  const side = (p, q, key) => (s.oznake?.[key] ? sideLabel(p, q, g, `${key} = ${s.oznake[key]}`, "lbl dim-t") : "");
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

// Izraz funkcije → JS funkcija; dopuštene su samo znamenke, x, + − · / ^, zagrade i sqrt/sin/cos/abs.
function compile(expr) {
  const js = String(expr).replace(/\^/g, "**").replace(/(\d)\s*([a-z(])/g, "$1*$2").replace(/\)\s*\(/g, ")*(");
  if (!/^[0-9x+\-*/().,\s]*$/.test(js.replace(/\b(sqrt|sin|cos|abs)\b/g, ""))) throw new Error("izraz funkcije nije dopušten");
  // eslint-disable-next-line no-new-func
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
      const x = s.x[0] + ((s.x[1] - s.x[0]) * k) / 240;
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

// Pravokutnik, po želji podijeljen na jednake kvadratiće (pločice, milimetarski papir).
function rectangle(s, text) {
  const { a, b, mreza } = s;
  if (!positive(a, b)) return { checks: [fail("stranice pravokutnika moraju biti pozitivni brojevi")] };
  const checks = [...numbersInText(Object.values(s.oznake ?? {}), text)];
  if (mreza) {
    const n = (a / mreza) * (b / mreza);
    checks.push({ ok: positive(mreza) && n <= 900, text: `mreža ${num(mreza)}: ${Math.round(n)} polja (najviše 900)` });
  }
  if (checks.some((x) => !x.ok)) return { checks };
  const T = fit([[0, 0], [a, b]], W, H, 52);
  let body = "";
  if (mreza) {
    for (let x = mreza; x < a - 1e-9; x += mreza) body += line(T([x, 0]), T([x, b]), "grid strong");
    for (let y = mreza; y < b - 1e-9; y += mreza) body += line(T([0, y]), T([a, y]), "grid strong");
  }
  body = poly([T([0, 0]), T([a, 0]), T([a, b]), T([0, b])], "shape") + body + poly([T([0, 0]), T([a, 0]), T([a, b]), T([0, b])], "outline");
  if (s.oznake?.a) body += label([T([a / 2, 0])[0], T([a / 2, 0])[1] + 16], s.oznake.a, "lbl dim-t");
  if (s.oznake?.b) body += label([T([a, b / 2])[0] + 8, T([a, b / 2])[1]], s.oznake.b, "lbl dim-t", "start");
  return { svg: svg(body), checks };
}

// Kvadar u kosoj projekciji (dubina pod 45° i u pola mjerila), skriveni bridovi crtkano;
// po želji razina tekućine u njemu.
function cuboid(s, text) {
  const { a, b, c, razina } = s;
  if (!positive(a, b, c)) return { checks: [fail("bridovi kvadra moraju biti pozitivni brojevi")] };
  const checks = [...numbersInText(Object.values(s.oznake ?? {}), text)];
  if (razina != null) checks.push({ ok: razina > 0 && razina <= c, text: `razina ${num(razina)} ${razina <= c ? "≤" : ">"} visina ${num(c)}` });
  if (checks.some((x) => !x.ok)) return { checks };
  const k = Math.SQRT1_2 * 0.5;
  const P = (x, y, z) => [x + y * k, z + y * k];
  const corners = [];
  for (const x of [0, a]) for (const y of [0, b]) for (const z of [0, c]) corners.push(P(x, y, z));
  const T = fit(corners, W, H, 40);
  const p = (x, y, z) => T(P(x, y, z));
  let body = "";
  if (razina != null) {
    const h = razina;
    body += poly([p(0, 0, 0), p(a, 0, 0), p(a, b, 0), p(a, b, h), p(0, b, h), p(0, 0, h)], "liquid");
    body += poly([p(0, 0, h), p(a, 0, h), p(a, b, h), p(0, b, h)], "liquid top");
  }
  // skriveni bridovi (iz stražnjeg donjeg lijevog vrha)
  body += line(p(0, b, 0), p(a, b, 0), "ln dash thin") + line(p(0, b, 0), p(0, 0, 0), "ln dash thin") + line(p(0, b, 0), p(0, b, c), "ln dash thin");
  // vidljivi bridovi
  const vis = [
    [p(0, 0, 0), p(a, 0, 0)], [p(a, 0, 0), p(a, 0, c)], [p(a, 0, c), p(0, 0, c)], [p(0, 0, c), p(0, 0, 0)],
    [p(a, 0, 0), p(a, b, 0)], [p(a, b, 0), p(a, b, c)], [p(a, b, c), p(a, 0, c)], [p(0, 0, c), p(0, b, c)], [p(0, b, c), p(a, b, c)],
  ];
  for (const [u, v] of vis) body += line(u, v);
  if (s.oznake?.a) body += label([mid(p(0, 0, 0), p(a, 0, 0))[0], mid(p(0, 0, 0), p(a, 0, 0))[1] + 16], s.oznake.a, "lbl dim-t");
  if (s.oznake?.b) body += label([mid(p(a, 0, 0), p(a, b, 0))[0] + 10, mid(p(a, 0, 0), p(a, b, 0))[1] + 6], s.oznake.b, "lbl dim-t", "start");
  if (s.oznake?.c) body += label([p(0, 0, c / 2)[0] - 8, p(0, 0, c / 2)[1]], s.oznake.c, "lbl dim-t", "end");
  if (razina != null && s.oznake?.razina) body += label([p(a, 0, razina / 2)[0] - 8, p(a, 0, razina / 2)[1]], s.oznake.razina, "lbl acc-t", "end");
  return { svg: svg(body), checks };
}

// Ljestvica mjernog instrumenta: menzura (okomito, razina tekućine), vaga (kazaljka na luku)
// ili ravnalo (vodoravno, predmet od jedne do druge oznake). Vrijednosti su ono što se očitava.
function scale(s, text) {
  const { od, do: to, razmak } = s;
  const values = (s.vrijednosti ?? []).filter((v) => typeof v === "number");
  const checks = [];
  if (!(typeof od === "number" && typeof to === "number" && to > od && positive(razmak))) {
    return { checks: [fail("ljestvica treba od < do i pozitivan razmak")] };
  }
  const n = (to - od) / razmak;
  // najviše podjeljaka da crtice ostanu čitljive i na mobitelu (≈ 5 px razmaka)
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
      const Y = (u) => y1 - ((u - od) / (to - od)) * (y1 - y0);
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
    let body = values.length > 1 ? draw(120, values[0], "prije") + draw(270, values[1], "poslije") : draw(190, values[0]);
    if (unit) body += label([W - 16, 30], s.jedinica, "tick", "end");
    return { svg: svg(body, 270), checks };
  }

  if (s.instrument === "vaga") {
    const c = [W / 2, 200], R = 150;
    const ang = (u) => Math.PI * (1 - (u - od) / (to - od));
    const at = (u, r) => [c[0] + r * Math.cos(ang(u)), c[1] - r * Math.sin(ang(u))];
    let body = `<path class="ln" d="M${f(c[0] - R)},${c[1]} A${R},${R} 0 0 1 ${f(c[0] + R)},${c[1]}"/>`;
    for (let i = 0; i <= Math.round(n); i++) {
      const u = od + i * razmak;
      const long = isBig(u);
      body += line(at(u, R), at(u, R - (long ? 16 : 9)), "ln thin");
      if (long) body += label(at(u, R - 30), num(u), "tick");
    }
    body += line(c, at(values[0], R - 6), "ln needle") + `<circle class="pt" cx="${c[0]}" cy="${c[1]}" r="5"/>`;
    if (unit) body += label([c[0], c[1] + 22], s.jedinica, "tick");
    return { svg: svg(body, 240), checks };
  }

  // ravnalo
  const x0 = 30, x1 = W - 30, y = 150;
  const X = (u) => x0 + ((u - od) / (to - od)) * (x1 - x0);
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

// --- fizika ---

function forces(s, text) {
  const th = ((s.podloga?.kut ?? 0) * Math.PI) / 180;
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
    body += line([20, base[1]], [W - 20, base[1]], "ln") ;
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
    at[1] + along[1] * (u - bw / 2) + normal[1] * v,
  ]);
  body += poly(corners, "box");
  const c = [at[0] + normal[0] * (bh / 2), at[1] + normal[1] * (bh / 2)];
  const max = Math.max(...s.sile.map((x) => x.iznos));
  const len = (v) => 30 + (v / max) * 70;
  const dir = {
    dolje: [0, 1], gore: [0, -1], okomito_od_podloge: normal,
    uz_kosinu: along, niz_kosinu: [-along[0], -along[1]], desno: [1, 0], lijevo: [-1, 0],
  };
  s.sile.forEach((x, i) => {
    const d = dir[x.smjer] ?? [0, 1];
    const end = [c[0] + d[0] * len(x.iznos), c[1] + d[1] * len(x.iznos)];
    const cls = `c${(i % 4) + 1}`;
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

const resistorH = (x, y, lbl) =>
  `<rect class="box" x="${x - 22}" y="${y - 9}" width="44" height="18"/>` + label([x, y - 20], lbl, "lbl small");
const resistorV = (x, y, lbl, left = false) =>
  `<rect class="box" x="${x - 9}" y="${y - 22}" width="18" height="44"/>` +
  label([x + (left ? -14 : 14), y], lbl, "lbl small", left ? "end" : "start");

function circuit(s, text) {
  const parts = s.spoj?.serijski ?? [];
  const series = parts.filter((e) => e.R);
  const par = parts.find((e) => e.paralelno)?.paralelno ?? [];
  const labels = [s.izvor?.oznaka, ...series.map((e) => e.R), ...par.map((e) => e.R)].filter(Boolean);
  const checks = numbersInText(labels, text);
  checks.push({ ok: par.length <= 3, text: `paralelnih grana: ${par.length} (najviše 3)` });
  if (!series.length && !par.length) checks.push(fail("strujni krug nema nijedan otpornik"));
  if (checks.some((x) => !x.ok)) return { checks };
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

// --- kemija ---

const SUB = "₀₁₂₃₄₅₆₇₈₉";
const SUP = { "⁰": "0", "¹": "1", "²": "2", "³": "3", "⁴": "4", "⁵": "5", "⁶": "6", "⁷": "7", "⁸": "8", "⁹": "9", "⁺": "+", "⁻": "−" };

// Naboj na kraju oznake: "2−" → −2, "Ca²⁺" → 2, "Cl⁻" → −1, "O" → 0.
function chargeOf(t) {
  const s = [...String(t ?? "")].map((c) => SUP[c] ?? c).join("").replace(/-/g, "−").trim();
  const m = s.match(/(\d*)([+−])$/);
  return m ? (m[1] ? Number(m[1]) : 1) * (m[2] === "+" ? 1 : -1) : 0;
}

// Atomi u formuli: "Cu₂S" → { Cu: 2, S: 1 }, "(CH₃COO)₂Mg" i "Ca²⁺(aq)" također; null ako se ne da pročitati.
function atomsOf(formula) {
  let s = [...String(formula ?? "")].map((c) => (SUB.includes(c) ? String(SUB.indexOf(c)) : c)).join("");
  s = s.replace(/\((?:s|l|g|aq|ℓ)\)/g, "").replace(/[⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+$/, "").replace(/\s/g, "");
  const stack = [{}];
  const re = /([A-Z][a-z]?)(\d*)|(\()|(\))(\d*)/g;
  let m;
  let pos = 0;
  while ((m = re.exec(s))) {
    if (m.index !== pos) return null;
    pos = re.lastIndex;
    const top = stack[stack.length - 1];
    if (m[1]) top[m[1]] = (top[m[1]] ?? 0) + (m[2] ? Number(m[2]) : 1);
    else if (m[3]) stack.push({});
    else if (m[4]) {
      if (stack.length < 2) return null;
      const g = stack.pop();
      const into = stack[stack.length - 1];
      for (const [e, n] of Object.entries(g)) into[e] = (into[e] ?? 0) + n * (m[5] ? Number(m[5]) : 1);
    }
  }
  if (pos !== s.length || stack.length !== 1 || !Object.keys(stack[0]).length) return null;
  return stack[0];
}

// Prvi broj s predznakom u oznaci („ΔH = −891 kJ/mol“ → 891, predznak −1); null ako broja nema.
function signedNumber(t) {
  const m = normalize(t).match(/([+−-])?\s*(\d+(?:,\d+)?)/);
  return m ? { v: Number(m[2].replace(",", ".")), sign: m[1] ? (m[1] === "+" ? 1 : -1) : 0 } : null;
}

// Oznaka u više redaka ("\n"), redci prema gore od zadnjeg na y.
const lines = (t) => String(t ?? "").split("\n").filter((x) => x !== "");
const multiLabel = (x, yLast, t, cls, anchor = "middle", step = 14) => {
  const ls = lines(t);
  return ls.map((l, i) => label([x, yLast - (ls.length - 1 - i) * step], l, cls, anchor)).join("");
};
const headUp = (x, y) => poly([[x, y], [x - 4.5, y + 9], [x + 4.5, y + 9]], "wedge");

// Energijski (entalpijski) dijagram: razine kao vodoravne crte na visini svoje energije, prijelazi kao okomite
// strelice; vrhovi crtaju energijski profil s prijelaznim stanjem između dviju razina (energija aktivacije).
function energyDiagram(s, text) {
  const levels = s.razine ?? [];
  const peaks = s.vrhovi ?? [];
  const steps = s.prijelazi ?? [];
  if (levels.length < 1 || levels.length > 5) return { checks: [fail("energijski dijagram treba 1 do 5 razina")] };
  if (!levels.every((l) => typeof l.vrijednost === "number" && isFinite(l.vrijednost))) {
    return { checks: [fail("svaka razina treba brojčanu vrijednost")] };
  }
  const isLevel = (i) => Number.isInteger(i) && i >= 0 && i < levels.length;
  const checks = [];
  for (const p of peaks) {
    if (!isLevel(p.od) || !isLevel(p.do) || p.od === p.do) {
      checks.push(fail("vrh mora spajati dvije različite razine (od, do)"));
      continue;
    }
    const ok = typeof p.vrijednost === "number" && p.vrijednost > Math.max(levels[p.od].vrijednost, levels[p.do].vrijednost);
    checks.push({ ok, text: `vrh ${num(p.vrijednost)} ${ok ? "je" : "NIJE"} iznad obiju razina koje spaja` });
  }
  for (const t of steps) {
    const target = t.vrh != null ? peaks[t.vrh] : isLevel(t.do) ? levels[t.do] : null;
    if (!isLevel(t.od) || !target || (t.vrh == null && t.od === t.do)) {
      checks.push(fail("prijelaz mora ići od razine do druge razine (do) ili do vrha (vrh)"));
      continue;
    }
    const d = target.vrijednost - levels[t.od].vrijednost;
    const n = signedNumber(t.oznaka);
    if (n) {
      const ok = Math.abs(Math.abs(d) - n.v) <= Math.max(0.6, 0.01 * n.v) && (n.sign === 0 || n.sign === Math.sign(d));
      checks.push({ ok, text: `oznaka „${t.oznaka}“ ${ok ? "odgovara" : "NE odgovara"} razlici razina ${num(d)}` });
    }
  }
  const allLabels = [...levels.map((l) => l.oznaka), ...steps.map((t) => t.oznaka), ...peaks.map((p) => p.oznaka)];
  checks.push(...numbersInText(allLabels.filter(Boolean).flatMap(lines), text));
  if (checks.some((x) => !x.ok)) return { checks };

  const far = steps.filter((t) => t.vrh == null && Math.abs(t.do - t.od) > 1);
  const left = 46;
  const right = far.length ? 124 : 16;
  const top = 46;
  const bottom = 238;
  const vals = [...levels.map((l) => l.vrijednost), ...peaks.map((p) => p.vrijednost)];
  let lo = Math.min(...vals);
  let hi = Math.max(...vals);
  if (hi - lo < 1e-9) [lo, hi] = [lo - 1, hi + 1];
  const Y = (v) => bottom - ((v - lo) / (hi - lo)) * (bottom - top);
  const colW = (W - left - right) / levels.length;
  const half = (colW * (peaks.length ? 0.36 : 0.62)) / 2;
  const cx = (i) => left + colW * (i + 0.5);
  const ends = (i) => [cx(i) - half, cx(i) + half];
  const fitLevel = (t) =>
    lines(t).flatMap((l) => ([...l].length * 6.8 > colW - 4 && l.includes(" + ") ? [l.slice(0, l.indexOf(" + ") + 2), l.slice(l.indexOf(" + ") + 3)] : [l])).join("\n");
  const peakCurve = (p) => {
    const [a, b] = p.od < p.do ? [ends(p.od)[1], ends(p.do)[0]] : [ends(p.od)[0], ends(p.do)[1]];
    const [ya, yb, yp] = [Y(levels[p.od].vrijednost), Y(levels[p.do].vrijednost), Y(p.vrijednost)];
    const xp = (a + b) / 2;
    const k = (xp - a) * 0.55;
    return { a, b, xp, yp, segs: [[[a, ya], [a + k, ya], [xp - k, yp], [xp, yp]], [[xp, yp], [xp + k, yp], [b - k, yb], [b, yb]]] };
  };
  // y krivulje vrha na zadanom x (uzorkovanje Bézierovih krivulja)
  const curveY = (c, x) => {
    let best = [Infinity, c.yp];
    for (const [p0, p1, p2, p3] of c.segs) {
      for (let i = 0; i <= 80; i++) {
        const t = i / 80;
        const u = 1 - t;
        const pt = [0, 1].map((j) => u ** 3 * p0[j] + 3 * u * u * t * p1[j] + 3 * u * t * t * p2[j] + t ** 3 * p3[j]);
        if (Math.abs(pt[0] - x) < best[0]) best = [Math.abs(pt[0] - x), pt[1]];
      }
    }
    return best[1];
  };

  let body = line([left - 20, bottom + 14], [left - 20, top - 22], "ln thin") + headUp(left - 20, top - 30);
  body += label([left - 20, top - 38], s.os ?? "H", "lbl small");
  const curves = peaks.map(peakCurve);
  peaks.forEach((p, i) => {
    const c = curves[i];
    const [s0, s1] = c.segs;
    const pt = (q) => q.map(f).join(",");
    body += `<path class="curve c${(i % 4) + 1}${p.isprekidano ? " dash" : ""}" d="M${pt(s0[0])} C${pt(s0[1])} ${pt(s0[2])} ${pt(s0[3])} C${pt(s1[1])} ${pt(s1[2])} ${pt(s1[3])}"/>`;
    if (!p.oznaka) return;
    if (peaks.length === 1) body += multiLabel(c.xp, c.yp - 10, p.oznaka, "lbl small c1-t");
    else {
      // više vrhova: legenda gore desno
      const y = 12 + 17 * i;
      body += `<line class="curve c${(i % 4) + 1}${p.isprekidano ? " dash" : ""}" x1="${W - 22}" y1="${y}" x2="${W - 4}" y2="${y}"/>`;
      body += label([W - 27, y], p.oznaka, `lbl small c${(i % 4) + 1}-t`, "end");
    }
  });
  levels.forEach((l, i) => {
    const [a, b] = ends(i);
    body += line([a, Y(l.vrijednost)], [b, Y(l.vrijednost)], "ln thick");
    if (l.oznaka) body += multiLabel(cx(i), Y(l.vrijednost) - 11, fitLevel(l.oznaka), "lbl small");
  });
  const eaSteps = steps.filter((t) => t.vrh != null);
  const extended = new Set();
  steps.forEach((t) => {
    const y0 = Y(levels[t.od].vrijednost);
    if (t.vrh != null) {
      // energija aktivacije: strelica od razine do vrha krivulje, kod samog vrha (dvije strelice malo razmaknute)
      const c = curves[t.vrh];
      const k = eaSteps.indexOf(t);
      const x = eaSteps.length === 1 ? c.xp : c.xp + (k === 0 ? -13 : 13 * k);
      const end = ends(t.od)[c.a > cx(t.od) ? 1 : 0];
      if (!extended.has(t.od)) body += line([end, y0], [c.xp + 13 * Math.max(1, eaSteps.length - 1) + 8, y0], "ln thin dash");
      extended.add(t.od);
      const cls = `c${(t.vrh % 4) + 1}`;
      body += arrow([x, y0], [x, curveY(c, x) + 3], cls);
      // oznaka ispod podnožja strelice: tamo krivulje ne prolaze
      const anchor = eaSteps.length === 1 ? "middle" : k === 0 ? "end" : "start";
      body += multiLabel(x + (anchor === "end" ? 4 : anchor === "start" ? -4 : 0), y0 + 15, t.oznaka, `lbl small ${cls}-t`, anchor);
      return;
    }
    const y1 = Y(levels[t.do].vrijednost);
    if (Math.abs(t.do - t.od) === 1) {
      const bx = left + colW * Math.max(t.od, t.do);
      body += line([ends(t.od)[t.od < t.do ? 1 : 0], y0], [bx, y0], "ln thin dash");
      body += line([bx, y1], [ends(t.do)[t.od < t.do ? 0 : 1], y1], "ln thin dash");
      body += Math.abs(y1 - y0) < 18 ? line([bx, y0], [bx, y1], "ln acc") : arrow([bx, y0], [bx, y1 + (y1 > y0 ? -2 : 2)], "acc");
      const lowerRight = (t.do > t.od) === (y1 > y0);
      // kratka strelica: oznaka ispod niže razine, inače uz sredinu strelice
      const ly = Math.abs(y1 - y0) < 18 ? Math.max(y0, y1) + 15 : (y0 + y1) / 2 + 4;
      body += multiLabel(bx + (lowerRight ? 6 : -6), ly, t.oznaka, "lbl small acc-t", lowerRight ? "start" : "end");
    } else {
      const x = W - right + 22 + 30 * far.indexOf(t);
      body += line([ends(t.od)[1], y0], [x, y0], "ln thin dash") + line([ends(t.do)[1], y1], [x, y1], "ln thin dash");
      // kratka razlika: crta bez vrha strelice (vrh bi bio dulji od nje)
      body += Math.abs(y1 - y0) < 18 ? line([x, y0], [x, y1], "ln acc") : arrow([x, y0], [x, y1 + (y1 > y0 ? -2 : 2)], "c2");
      body += multiLabel(x + 6, (y0 + y1) / 2 + 4, t.oznaka, "lbl small c2-t", "start");
    }
  });
  return { svg: svg(body, 262), checks };
}

// Strukturna (i Lewisova) formula: atomi s koordinatama u duljinama veze (y prema gore), veze s redom.
// Prsten: atomi bez koordinata složeni u pravilan mnogokut; "uz": atom izvan prstena, prema van od njega.
const VALENCE = { H: 1, B: 3, C: 4, N: 3, O: 2, F: 1, Cl: 1, Br: 1, I: 1, S: 2, P: 3 };
const VALENCE_E = { H: 1, B: 3, C: 4, N: 5, O: 6, F: 7, Cl: 7, Br: 7, I: 7, S: 6, P: 5 };
const EXPANDED = new Set(["S", "P", "Cl", "Br", "I"]);
const BOND_ORDER = { 1: 1, 2: 2, 3: 3, klin: 1, crtkano: 1, vodikova: 0 };
const ELEMENT = /^[A-Z][a-z]?$/;

function allowedValence(el, q) {
  if (!q) return VALENCE[el];
  if (["N", "P", "O", "S"].includes(el)) return VALENCE[el] + q;
  if (el === "C") return 3;
  if (el === "B") return VALENCE[el] - q;
  return 0;
}

function structure(s, text) {
  const atoms = (s.atomi ?? []).map((a) => ({ ...a, id: String(a.id) }));
  if (!atoms.length || atoms.length > 40) return { checks: [fail("formula treba 1 do 40 atoma")] };
  const byId = new Map();
  for (const a of atoms) {
    if (a.id === "undefined" || byId.has(a.id)) return { checks: [fail(`atom bez oznake ili s ponovljenom oznakom „${a.id}“`)] };
    byId.set(a.id, a);
  }
  const rings = [].concat(s.prsten ?? []);
  for (const r of rings) {
    const ids = (r.atomi ?? []).map(String);
    if (ids.length < 3 || ids.length > 8 || !ids.every((id) => byId.has(id))) {
      return { checks: [fail("prsten treba 3 do 8 postojećih atoma")] };
    }
    const [c0, c1] = r.sredina ?? [0, 0];
    const R = 1 / (2 * Math.sin(Math.PI / ids.length));
    const start = ((r.kut ?? 90) * Math.PI) / 180;
    ids.forEach((id, k) => {
      const a = byId.get(id);
      const t = start - (2 * Math.PI * k) / ids.length;
      if (typeof a.x !== "number") [a.x, a.y] = [c0 + R * Math.cos(t), c1 + R * Math.sin(t)];
      a.ring = [c0, c1];
    });
  }
  for (const a of atoms) {
    if (typeof a.x === "number" || a.uz == null) continue;
    const b = byId.get(String(a.uz));
    if (!b?.ring) return { checks: [fail(`„uz“ atoma ${a.id} mora biti atom prstena`)] };
    const d = [b.x - b.ring[0], b.y - b.ring[1]];
    const l = Math.hypot(...d) || 1;
    [a.x, a.y] = [b.x + d[0] / l, b.y + d[1] / l];
  }
  if (!atoms.every((a) => typeof a.x === "number" && typeof a.y === "number" && isFinite(a.x) && isFinite(a.y))) {
    return { checks: [fail("svaki atom treba koordinate x, y (ili je u prstenu)")] };
  }
  const bonds = [];
  for (const b of s.veze ?? []) {
    const [a, c, red] = Array.isArray(b) ? b : [b.od, b.do, b.red];
    const order = red ?? 1;
    if (!byId.has(String(a)) || !byId.has(String(c)) || String(a) === String(c)) {
      return { checks: [fail(`veza ${a}–${c} spaja atom koji ne postoji`)] };
    }
    if (!(order in BOND_ORDER)) return { checks: [fail(`nepoznata vrsta veze „${order}“ (1, 2, 3, klin, crtkano, vodikova)`)] };
    if (bonds.some((x) => (x.a === String(a) && x.b === String(c)) || (x.a === String(c) && x.b === String(a)))) {
      return { checks: [fail(`veza ${a}–${c} navedena je dvaput`)] };
    }
    bonds.push({ a: String(a), b: String(c), red: order });
  }
  const checks = [];
  for (const b of bonds) {
    const [p, q] = [byId.get(b.a), byId.get(b.b)];
    const d = Math.hypot(p.x - q.x, p.y - q.y);
    const ok = b.red === "vodikova" ? d <= 3 : d >= 0.6 && d <= 1.8;
    if (!ok) checks.push(fail(`veza ${b.a}–${b.b} duga je ${num(d)} (treba oko 1)`));
  }
  for (let i = 0; i < atoms.length; i++) {
    for (let j = i + 1; j < atoms.length; j++) {
      if (Math.hypot(atoms[i].x - atoms[j].x, atoms[i].y - atoms[j].y) < 0.5) checks.push(fail(`atomi ${atoms[i].id} i ${atoms[j].id} preklapaju se`));
    }
  }
  const bondSum = (id) => bonds.reduce((t, b) => t + (b.a === id || b.b === id ? BOND_ORDER[b.red] : 0), 0);
  const lewis = atoms.some((a) => typeof a.nevezni === "number");
  const perAtomCharge = atoms.some((a) => a.naboj);
  let formal = 0;
  for (const a of atoms) {
    const el = a.simbol === "" ? "C" : a.simbol;
    if (!ELEMENT.test(el) || !(el in VALENCE)) continue;
    const bs = bondSum(a.id);
    const q = chargeOf(a.naboj);
    if (lewis && a.simbol !== "") {
      const lp = a.nevezni ?? 0;
      const e = 2 * bs + 2 * lp;
      const ok = el === "H" ? e === 2 : el === "B" ? e === 6 || e === 8 : EXPANDED.has(el) ? [8, 10, 12].includes(e) : e === 8;
      checks.push({ ok, text: `${a.id}: ${e} elektrona oko atoma ${el}${ok ? "" : " (NIJE popunjena ljuska: provjeri veze i nevezne parove)"}` });
      const fc = VALENCE_E[el] - 2 * lp - bs;
      if (perAtomCharge) {
        checks.push({ ok: fc === q, text: `${a.id}: formalni naboj ${fc} ${fc === q ? "=" : "≠"} upisani naboj ${q}` });
      } else formal += fc;
    } else {
      const ok = bs <= allowedValence(el, q);
      if (!ok) checks.push(fail(`${a.id}: atom ${el} ima ${bs} veza, a smije najviše ${allowedValence(el, q)}`));
    }
  }
  if (lewis && !perAtomCharge) {
    const q = chargeOf(s.naboj);
    checks.push({ ok: formal === q, text: `zbroj formalnih naboja ${formal} ${formal === q ? "=" : "≠"} naboj čestice ${q}` });
  }
  const angleOf = (k) => {
    const [p, c, q] = (k.atomi ?? []).map((id) => byId.get(String(id)));
    if (!p || !c || !q) return null;
    const a1 = Math.atan2(p.y - c.y, p.x - c.x);
    const a2 = Math.atan2(q.y - c.y, q.x - c.x);
    let d = Math.abs(a1 - a2);
    if (d > Math.PI) d = 2 * Math.PI - d;
    return { p, c, q, deg: (d * 180) / Math.PI };
  };
  for (const k of s.kutovi ?? []) {
    const g = angleOf(k);
    if (!g) {
      checks.push(fail("kut treba tri postojeća atoma"));
      continue;
    }
    const n = signedNumber(k.oznaka);
    if (n) {
      const ok = Math.abs(g.deg - n.v) <= 6;
      checks.push({ ok, text: `kut ${(k.atomi ?? []).join("-")} nacrtan je ${num(Math.round(g.deg))}°, oznaka kaže ${k.oznaka}` });
    }
  }
  checks.push(...numbersInText([...(s.kutovi ?? []).map((k) => k.oznaka), ...(s.natpisi ?? []).map((n) => n.tekst)].filter(Boolean), text));
  if (checks.some((x) => !x.ok)) return { checks };

  // zrcalna slika: ista molekula preslikana preko okomite crte desno od nje
  let all = atoms;
  let allBonds = bonds;
  let mirrorX = null;
  if (s.zrcalo) {
    mirrorX = Math.max(...atoms.map((a) => a.x)) + 0.9;
    const m = atoms.map((a) => ({ ...a, id: `${a.id}′`, x: 2 * mirrorX - a.x }));
    all = [...atoms, ...m];
    allBonds = [...bonds, ...bonds.map((b) => ({ ...b, a: `${b.a}′`, b: `${b.b}′` }))];
  }
  const pts = [...all.map((a) => [a.x, a.y]), ...(s.natpisi ?? []).map((n) => [n.x, n.y])];
  const xs = pts.map((p) => p[0]);
  const ys = pts.map((p) => p[1]);
  const pad = 40;
  const sc = Math.min((W - 2 * pad) / (Math.max(...xs) - Math.min(...xs) || 1), (H - 2 * pad) / (Math.max(...ys) - Math.min(...ys) || 1), 50);
  const h = Math.max(130, Math.min(H, (Math.max(...ys) - Math.min(...ys)) * sc + 2 * pad));
  const ox = (W - sc * (Math.max(...xs) - Math.min(...xs))) / 2;
  const oy = (h - sc * (Math.max(...ys) - Math.min(...ys))) / 2;
  const T = (x, y) => [ox + (x - Math.min(...xs)) * sc, h - (oy + (y - Math.min(...ys)) * sc)];
  const P = new Map(all.map((a) => [a.id, T(a.x, a.y)]));
  const A = new Map(all.map((a) => [a.id, a]));
  const shown = (a) => `${a.simbol ?? ""}${a.zvjezdica ? "*" : ""}`;
  const halfW = (a) => {
    const t = [...shown(a)];
    if (!t.length) return 0;
    return 5 + t.reduce((w, c) => w + (SUB.includes(c) || SUP[c] ? 2.6 : 4.4), 0);
  };
  const trim = (id, u) => {
    const a = A.get(id);
    const hw = halfW(a);
    if (!hw) return 0;
    return Math.min(Math.abs(u[0]) > 1e-6 ? hw / Math.abs(u[0]) : Infinity, Math.abs(u[1]) > 1e-6 ? 10 / Math.abs(u[1]) : Infinity);
  };
  const dirs = new Map(all.map((a) => [a.id, []]));
  let body = "";
  if (mirrorX != null) {
    const [mx] = T(mirrorX, 0);
    body += line([mx, 14], [mx, h - 26], "ln thin dash") + label([mx, h - 12], "zrcalo", "tick");
  }
  for (const b of allBonds) {
    const [p, q] = [P.get(b.a), P.get(b.b)];
    const L = Math.hypot(q[0] - p[0], q[1] - p[1]);
    const u = [(q[0] - p[0]) / L, (q[1] - p[1]) / L];
    dirs.get(b.a).push(Math.atan2(u[1], u[0]));
    dirs.get(b.b).push(Math.atan2(-u[1], -u[0]));
    const t1 = trim(b.a, u) + (b.red === "vodikova" ? 3 : 1);
    const t2 = trim(b.b, u) + (b.red === "vodikova" ? 3 : 1);
    const a = [p[0] + u[0] * t1, p[1] + u[1] * t1];
    const c = [q[0] - u[0] * t2, q[1] - u[1] * t2];
    const n = [-u[1], u[0]];
    const off = (pt, d) => [pt[0] + n[0] * d, pt[1] + n[1] * d];
    if (b.red === 2) body += line(off(a, 3), off(c, 3)) + line(off(a, -3), off(c, -3));
    else if (b.red === 3) body += line(a, c) + line(off(a, 4.5), off(c, 4.5)) + line(off(a, -4.5), off(c, -4.5));
    else if (b.red === "klin") body += poly([a, off(c, 4.5), off(c, -4.5)], "wedge");
    else if (b.red === "crtkano") body += line(a, c, "ln dash");
    else if (b.red === "vodikova") body += line(a, c, "ln dot acc");
    else body += line(a, c);
  }
  // slobodan smjer oko atoma: najdalje od veza i već zauzetih smjerova (prvo gore, dolje, lijevo, desno)
  const CAND = [-90, 90, 180, 0, -45, -135, 45, 135].map((d) => (d * Math.PI) / 180);
  const angDist = (x, y) => {
    const d = Math.abs(x - y) % (2 * Math.PI);
    return d > Math.PI ? 2 * Math.PI - d : d;
  };
  const free = (id) => {
    const taken = dirs.get(id);
    let best = CAND[0];
    let score = -1;
    for (const c of CAND) {
      const sc2 = taken.length ? Math.min(...taken.map((t) => angDist(c, t))) : Math.PI;
      if (sc2 > score + 1e-6) [best, score] = [c, sc2];
    }
    taken.push(best);
    return best;
  };
  for (const a of all) {
    const p = P.get(a.id);
    const r = Math.max(13, halfW(a) + 5);
    for (let k = 0; k < (a.nevezni ?? 0); k++) {
      const t = free(a.id);
      const u = [Math.cos(t), Math.sin(t)];
      const rr = Math.abs(u[0]) > 0.5 ? r : 13;
      const c = [p[0] + u[0] * rr, p[1] + u[1] * rr];
      body += `<circle class="pt" cx="${f(c[0] - u[1] * 3.4)}" cy="${f(c[1] + u[0] * 3.4)}" r="1.9"/>`;
      body += `<circle class="pt" cx="${f(c[0] + u[1] * 3.4)}" cy="${f(c[1] - u[0] * 3.4)}" r="1.9"/>`;
    }
    if (shown(a)) body += label([p[0], p[1] + 1], shown(a), a.istaknut ? "lbl acc-t" : "lbl");
    if (a.naboj) body += label([p[0] + halfW(a) + 3, p[1] - 10], a.naboj, "lbl small", "start");
    if (a.broj != null) {
      const t = free(a.id);
      body += label([p[0] + Math.cos(t) * 21, p[1] + Math.sin(t) * 21], String(a.broj), "tick");
    }
  }
  for (const k of s.kutovi ?? []) {
    const g = angleOf(k);
    const [c, p, q] = [P.get(g.c.id), P.get(g.p.id), P.get(g.q.id)];
    const a1 = Math.atan2(p[1] - c[1], p[0] - c[0]);
    let a2 = Math.atan2(q[1] - c[1], q[0] - c[0]);
    if (a2 - a1 > Math.PI) a2 -= 2 * Math.PI;
    if (a1 - a2 > Math.PI) a2 += 2 * Math.PI;
    const r = 24;
    const sweep = a2 > a1 ? 1 : 0;
    body += `<path class="ln thin acc" d="M${f(c[0] + r * Math.cos(a1))},${f(c[1] + r * Math.sin(a1))} A${r},${r} 0 0 ${sweep} ${f(c[0] + r * Math.cos(a2))},${f(c[1] + r * Math.sin(a2))}"/>`;
    const m = (a1 + a2) / 2;
    body += label([c[0] + 38 * Math.cos(m), c[1] + 38 * Math.sin(m)], k.oznaka, "lbl small acc-t");
  }
  if (s.zagrade) {
    const bx = atoms.map((a) => P.get(a.id));
    const [x0, x1] = [Math.min(...bx.map((p) => p[0])) - 24, Math.max(...bx.map((p) => p[0])) + 24];
    const [y0, y1] = [Math.min(...bx.map((p) => p[1])) - 24, Math.max(...bx.map((p) => p[1])) + 24];
    body += `<path class="ln thin" d="M${f(x0 + 7)},${f(y0)} H${f(x0)} V${f(y1)} H${f(x0 + 7)} M${f(x1 - 7)},${f(y0)} H${f(x1)} V${f(y1)} H${f(x1 - 7)}"/>`;
    if (s.naboj) body += label([x1 + 4, y0 + 2], s.naboj, "lbl small", "start");
  }
  for (const r of rings) {
    if (!r.krug) continue;
    const ids = (r.atomi ?? []).map(String);
    const c = T(...(r.sredina ?? [0, 0]));
    const R = (1 / (2 * Math.sin(Math.PI / ids.length))) * Math.cos(Math.PI / ids.length) * sc;
    body += `<circle class="ln" cx="${f(c[0])}" cy="${f(c[1])}" r="${f(R * 0.62)}"/>`;
  }
  for (const n of s.natpisi ?? []) body += label(T(n.x, n.y), n.tekst, "lbl small dim-t");
  return { svg: svg(body, f(h)), checks };
}

// Galvanski članak (dvije čaše i elektrolitni most) ili elektroliza (U-cijev ili jedna čaša s izvorom).
const TINT = { crvena: "t-danger", "ružičasta": "t-danger", "žuta": "t-warn", "narančasta": "t-warn", zelena: "t-success", plava: "t-primary", bezbojna: "t-none" };

function cell(s, text) {
  const L = s.lijevo ?? {};
  const R = s.desno ?? {};
  const galv = s.vrsta === "galvanski";
  const vessel = s.posuda ?? (galv ? "dvije_case" : "u_cijev");
  const checks = [];
  if (!["galvanski", "elektroliza"].includes(s.vrsta)) checks.push(fail("vrsta mora biti galvanski ili elektroliza"));
  if (!["dvije_case", "u_cijev", "casa"].includes(vessel)) checks.push(fail("posuda mora biti dvije_case, u_cijev ili casa"));
  const roles = [L.uloga, R.uloga].sort().join(",");
  if (roles !== "anoda,katoda") checks.push(fail("jedna elektroda mora biti anoda, a druga katoda"));
  for (const e of [L, R]) {
    if (!e.pol) continue;
    const want = (e.uloga === "anoda") === galv ? "−" : "+";
    const ok = e.pol.replace("-", "−") === want;
    checks.push({ ok, text: `${e.uloga} u ${galv ? "galvanskom članku" : "elektrolizi"} je pol ${want}${ok ? "" : `, a ne ${e.pol}`}` });
  }
  const anodeLeft = L.uloga === "anoda";
  if (s.elektroni) {
    const ok = s.elektroni === (anodeLeft ? "desno" : "lijevo");
    checks.push({ ok, text: `elektroni kroz žicu idu od anode prema katodi (${anodeLeft ? "desno" : "lijevo"})${ok ? "" : `, a ne ${s.elektroni}`}` });
  }
  if (galv && vessel === "dvije_case" && !s.most) checks.push(fail("galvanski članak u dvije čaše treba elektrolitni most"));
  checks.push(...numbersInText([s.uredaj?.oznaka].filter(Boolean), text));
  if (checks.some((x) => !x.ok)) return { checks };

  const tint = (e) => (e.boja && TINT[e.boja] ? ` ${TINT[e.boja]}` : "");
  const electrode = (x, y0, y1) => `<rect class="electrode" x="${x - 6}" y="${y0}" width="12" height="${y1 - y0}" rx="2"/>`;
  const bubbles = (x, y) =>
    [[-12, 0], [11, -9], [-10, -20], [12, -30], [-11, -42]]
      .map(([dx, dy], i) => `<circle class="bubble" cx="${x + dx}" cy="${y + dy}" r="${2.6 + (i % 2)}"/>`)
      .join("");
  const side = (e, x, anchor, y, d = anchor === "end" ? -12 : 12) => {
    let b = "";
    if (e.elektroda) b += label([x + d, y], e.elektroda, "lbl small", anchor);
    if (e.uloga) b += label([x + d, y + 16], `${e.uloga}${e.pol ? ` (${e.pol})` : ""}`, "lbl small dim-t", anchor);
    return b;
  };
  const xs = vessel === "dvije_case" ? [95, 285] : vessel === "u_cijev" ? [125, 255] : [140, 240];
  let body = "";
  const wireY = 34;
  // izvor ili mjerni uređaj na sredini žice
  const dev = s.uredaj?.vrsta ?? (galv ? "V" : "izvor");
  body += line([xs[0], wireY + 30], [xs[0], wireY]) + line([xs[1], wireY + 30], [xs[1], wireY]);
  if (dev === "izvor") {
    const plusLeft = anodeLeft !== galv;
    const [xl, xr] = [184, 196];
    body += line([xs[0], wireY], [xl, wireY]) + line([xr, wireY], [xs[1], wireY]);
    body += line([xl, wireY - (plusLeft ? 14 : 8)], [xl, wireY + (plusLeft ? 14 : 8)], plusLeft ? "ln" : "ln thick");
    body += line([xr, wireY - (plusLeft ? 8 : 14)], [xr, wireY + (plusLeft ? 8 : 14)], plusLeft ? "ln thick" : "ln");
    body += label([xl - 8, wireY - 14], plusLeft ? "+" : "−", "lbl small", "end") + label([xr + 8, wireY - 14], plusLeft ? "−" : "+", "lbl small", "start");
  } else {
    body += line([xs[0], wireY], [176, wireY]) + line([204, wireY], [xs[1], wireY]);
    body += `<circle class="meter" cx="190" cy="${wireY}" r="14"/>` + label([190, wireY + 1], dev === "žarulja" ? "×" : dev, "lbl small");
  }
  if (s.uredaj?.oznaka) body += label([190, wireY + 30], s.uredaj.oznaka, "lbl small");
  if (s.elektroni) {
    const dir = s.elektroni === "desno" ? 1 : -1;
    for (const [a, b] of [[xs[0] + 14, 166], [214, xs[1] - 14]]) {
      const [p, q] = dir > 0 ? [a, b] : [b, a];
      body += arrow([p, wireY - 12], [q, wireY - 12], "acc");
    }
    body += label([(xs[0] + 176) / 2, wireY - 24], "e⁻", "lbl small acc-t") + label([(xs[1] + 204) / 2, wireY - 24], "e⁻", "lbl small acc-t");
  }

  if (vessel === "dvije_case") {
    for (const [x, e] of [[xs[0], L], [xs[1], R]]) {
      body += `<rect class="liquid${tint(e)}" x="${x - 58}" y="150" width="116" height="88"/>`;
      body += `<path class="ln" d="M${x - 60},118 V240 H${x + 60} V118"/>`;
      body += electrode(x, wireY + 30, 222);
      if (e.otopina) body += label([x, 256], e.otopina, "lbl small");
    }
    body += side(L, xs[0], "end", 92) + side(R, xs[1], "start", 92);
    body += `<path class="bridge" d="M${xs[0] + 34},200 V108 Q${xs[0] + 34},98 ${xs[0] + 46},98 H${xs[1] - 46} Q${xs[1] - 34},98 ${xs[1] - 34},108 V200"/>`;
    body += label([190, 124], s.most, "lbl small");
    return { svg: svg(body, 266), checks };
  }
  if (vessel === "u_cijev") {
    body += `<path class="liquid" d="M100,110 V170 A90,90 0 0 0 280,170 V110 H230 V170 A40,40 0 0 1 150,170 V110 Z"/>`;
    body += `<rect class="liquid${tint(L)}" x="100" y="110" width="50" height="60"/><rect class="liquid${tint(R)}" x="230" y="110" width="50" height="60"/>`;
    body += `<path class="ln" d="M100,82 V170 A90,90 0 0 0 280,170 V82 M150,82 V170 A40,40 0 0 0 230,170 V82"/>`;
    for (const [x, e] of [[xs[0], L], [xs[1], R]]) {
      body += electrode(x, wireY + 30, 172);
      if (e.plin) body += bubbles(x, 160);
    }
    if (L.plin) body += label([94, 140], L.plin, "lbl small", "end");
    if (R.plin) body += label([286, 140], R.plin, "lbl small", "start");
    body += side(L, 94, "end", 84, 0) + side(R, 286, "start", 84, 0);
    if (s.otopina) body += label([190, 236], s.otopina, "lbl small");
    return { svg: svg(body, 266), checks };
  }
  body += `<rect class="liquid${tint(s)}" x="82" y="130" width="216" height="108"/>`;
  body += `<path class="ln" d="M80,100 V240 H300 V100"/>`;
  for (const [x, e] of [[xs[0], L], [xs[1], R]]) {
    body += electrode(x, wireY + 30, 222);
    if (e.plin) body += bubbles(x, 210);
  }
  body += side(L, xs[0], "end", 92) + side(R, xs[1], "start", 92);
  if (s.otopina) body += label([190, 256], s.otopina, "lbl small");
  return { svg: svg(body, 266), checks };
}

// Model ljuski (Bohr): jezgra s brojem protona i elektroni po ljuskama, do tri čestice jedna uz drugu.
function shells(s, text) {
  const ps = s.cestice ?? [];
  if (ps.length < 1 || ps.length > 3) return { checks: [fail("ljuske: 1 do 3 čestice")] };
  const checks = [];
  for (const p of ps) {
    const e = p.elektroni ?? [];
    if (!positive(p.protoni) || !Number.isInteger(p.protoni) || !e.length || e.length > 7 || !e.every((n) => Number.isInteger(n) && n > 0)) {
      checks.push(fail(`${p.simbol}: protoni i elektroni po ljuskama moraju biti pozitivni cijeli brojevi`));
      continue;
    }
    const total = e.reduce((a, b) => a + b, 0);
    const want = p.protoni - chargeOf(p.simbol);
    checks.push({ ok: total === want, text: `${p.simbol}: ${total} elektrona ${total === want ? "=" : "≠"} ${p.protoni} protona − naboj (${want})` });
    e.forEach((n, i) => {
      const max = Math.min(2 * (i + 1) ** 2, i === e.length - 1 ? 8 : Infinity, i === 0 ? 2 : Infinity);
      if (n > max) checks.push(fail(`${p.simbol}: u ${i + 1}. ljusci ${n} elektrona (najviše ${max})`));
    });
  }
  checks.push(...numbersInText([...ps.map((p) => String(p.protoni)), ...ps.map((p) => p.polumjer).filter((x) => x != null).map(String)], text));
  if (checks.some((x) => !x.ok)) return { checks };
  const colW = W / ps.length;
  const rmax = Math.min(colW / 2 - 10, 96);
  const maxR = Math.max(...ps.map((p) => p.polumjer ?? 0));
  const cy = 122;
  let body = "";
  ps.forEach((p, i) => {
    const cx = colW * (i + 0.5);
    const outer = maxR && p.polumjer ? Math.max(30, (rmax * p.polumjer) / maxR) : rmax;
    const n = p.elektroni.length;
    const r0 = 18;
    p.elektroni.forEach((count, k) => {
      const r = r0 + ((outer - r0) * (k + 1)) / n;
      body += `<circle class="ln thin" cx="${f(cx)}" cy="${cy}" r="${f(r)}"/>`;
      for (let j = 0; j < count; j++) {
        const t = -Math.PI / 2 + (k * Math.PI) / 9 + (2 * Math.PI * j) / count;
        body += `<circle class="pt acc-f" cx="${f(cx + r * Math.cos(t))}" cy="${f(cy + r * Math.sin(t))}" r="3.2"/>`;
      }
    });
    body += `<circle class="nucleus" cx="${f(cx)}" cy="${cy}" r="16"/>` + label([cx, cy + 1], `${p.protoni}p⁺`, "lbl small");
    body += label([cx, cy + rmax + 20], p.simbol, "lbl");
    if (s.raspored) body += label([cx, cy + rmax + 38], p.elektroni.join(", "), "lbl small dim-t");
  });
  return { svg: svg(body, s.raspored ? 270 : 252), checks };
}

// Čestice u posudi (model čestica): atomi istog elementa iste boje, molekula kao skupina dodirnih kuglica.
// Raspored je pseudoslučajan ali uvijek isti za isti opis.
function particles(s, text) {
  const boxes = s.posude ?? [];
  if (boxes.length < 1 || boxes.length > 3) return { checks: [fail("čestice: 1 do 3 posude")] };
  const checks = [];
  const species = new Map();
  for (const b of boxes) {
    for (const c of b.cestice ?? []) {
      if (!Number.isInteger(c.broj) || c.broj < 1) checks.push(fail(`broj čestica ${c.vrsta} mora biti pozitivan cijeli broj`));
      if (!species.has(c.vrsta)) species.set(c.vrsta, atomsOf(c.vrsta));
    }
    const n = (b.cestice ?? []).reduce((t, c) => t + (c.broj ?? 0), 0);
    if (n > 30) checks.push(fail(`u posudi „${b.oznaka ?? ""}“ je ${n} čestica (najviše 30)`));
  }
  const byAtoms = s.prikaz ? s.prikaz === "atomi" : [...species.values()].every((a) => a && Object.values(a).reduce((x, y) => x + y, 0) <= 4);
  if (byAtoms && [...species.values()].some((a) => !a)) checks.push(fail("formula čestice ne može se pročitati (npr. Cu₂S, H₂O, CO₂)"));
  if (s.ocuvanje && byAtoms && !checks.length) {
    const totals = boxes.map((b) => {
      const t = {};
      for (const c of b.cestice ?? []) for (const [e, k] of Object.entries(species.get(c.vrsta))) t[e] = (t[e] ?? 0) + k * c.broj;
      return t;
    });
    const key = (t) => Object.keys(t).sort().map((e) => `${e}${t[e]}`).join(" ");
    const ok = totals.every((t) => key(t) === key(totals[0]));
    checks.push({ ok, text: `atomi u posudama: ${totals.map(key).join(" | ")} ${ok ? "(jednako, atomi se čuvaju)" : "(NIJE jednako)"}` });
  }
  checks.push(...numbersInText(boxes.map((b) => b.oznaka).filter(Boolean), text));
  if (checks.some((x) => !x.ok)) return { checks };

  const kinds = [];
  const kindOf = (k) => {
    if (!kinds.includes(k)) kinds.push(k);
    return `k${(kinds.indexOf(k) % 5) + 1}`;
  };
  let seed = 7;
  for (const ch of JSON.stringify(boxes)) seed = (seed * 31 + ch.charCodeAt(0)) >>> 0;
  const rnd = () => {
    seed = (seed + 0x6d2b79f5) >>> 0;
    let t = seed;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  const gap = s.strelica ? 34 : 14;
  const bw = (W - 16 - gap * (boxes.length - 1)) / boxes.length;
  const [top, bh] = [12, 168];
  const r = byAtoms ? 7 : 8;
  let body = "";
  boxes.forEach((b, i) => {
    const x0 = 8 + i * (bw + gap);
    body += `<rect class="outline" x="${f(x0)}" y="${top}" width="${f(bw)}" height="${bh}" rx="6"/>`;
    if (b.oznaka) body += label([x0 + bw / 2, top + bh + 16], b.oznaka, "lbl small");
    if (s.strelica && i) body += arrow([x0 - gap + 6, top + bh / 2], [x0 - 6, top + bh / 2], "acc");
    const placed = [];
    for (const c of b.cestice ?? []) {
      const atomsList = byAtoms ? Object.entries(species.get(c.vrsta)).flatMap(([e, k]) => Array(k).fill(e)) : [c.vrsta];
      // rijetki element u sredini (Cu–S–Cu ne, nego Cu, S, Cu → S u sredini kad ga ima manje)
      if (atomsList.length === 3) {
        const counts = atomsList.reduce((m, e) => ((m[e] = (m[e] ?? 0) + 1), m), {});
        const lone = atomsList.find((e) => counts[e] === 1);
        if (lone && Object.keys(counts).length === 2) atomsList.splice(0, 3, atomsList.find((e) => e !== lone), lone, atomsList.find((e) => e !== lone));
      }
      const m = atomsList.length;
      const offs = m === 1 ? [[0, 0]] : m === 4 ? [[-0.8, -0.8], [0.8, -0.8], [-0.8, 0.8], [0.8, 0.8]] : atomsList.map((_, k) => [(k - (m - 1) / 2) * 1.55, 0]);
      const R = r * (m === 1 ? 1 : m === 4 ? 2.2 : 0.78 * m + 0.4);
      for (let n = 0; n < c.broj; n++) {
        let pos = null;
        for (let tries = 0; tries < 600 && !pos; tries++) {
          const p = [x0 + R + 6 + rnd() * (bw - 2 * R - 12), top + R + 6 + rnd() * (bh - 2 * R - 12)];
          if (placed.every(([q, Rq]) => Math.hypot(p[0] - q[0], p[1] - q[1]) > R + Rq + 3)) pos = p;
        }
        if (!pos) return checks.push(fail(`čestice u posudi „${b.oznaka ?? ""}“ ne stanu; smanji broj čestica`));
        placed.push([pos, R]);
        const turn = rnd() * Math.PI;
        atomsList.forEach((e, k) => {
          const [dx, dy] = offs[k];
          const q = [pos[0] + r * (dx * Math.cos(turn) - dy * Math.sin(turn)), pos[1] + r * (dx * Math.sin(turn) + dy * Math.cos(turn))];
          body += `<circle class="atom ${kindOf(e)}" cx="${f(q[0])}" cy="${f(q[1])}" r="${r}"/>`;
        });
      }
    }
  });
  if (checks.some((x) => !x.ok)) return { checks };
  const legend = kinds.map((k) => ({ k, w: 22 + [...k].length * 7.5 }));
  let lx = (W - legend.reduce((t, x) => t + x.w, 0)) / 2;
  for (const { k, w } of legend) {
    body += `<circle class="atom ${kindOf(k)}" cx="${f(lx + 7)}" cy="226" r="6"/>` + label([lx + 17, 227], k, "lbl small", "start");
    lx += w;
  }
  return { svg: svg(body, 244), checks };
}

// Kromatogram: traka (kreda ili papir) u čaši s otapalom, startna linija, fronta otapala i mrlje na
// udaljenostima od startne linije; kote prikazuju te udaljenosti.
const SPOT = { "žuta": "k2", "narančasta": "k2", zelena: "k3", plava: "k1", crvena: "k4", "ljubičasta": "k1", siva: "k5" };

function chromatogram(s, text) {
  const spots = s.mrlje ?? [];
  const checks = [];
  if (!positive(s.fronta)) return { checks: [fail("fronta otapala mora biti pozitivan broj")] };
  if (spots.length < 1 || spots.length > 5) checks.push(fail("kromatogram treba 1 do 5 mrlja"));
  for (const m of spots) {
    const ok = typeof m.udaljenost === "number" && m.udaljenost > 0 && m.udaljenost < s.fronta;
    checks.push({ ok, text: `mrlja ${m.oznaka ?? ""} na ${num(m.udaljenost)} ${ok ? "je" : "NIJE"} između startne linije i fronte (${num(s.fronta)})` });
  }
  if (s.kote !== false) checks.push(...numbersInText([s.fronta, ...spots.map((m) => m.udaljenost)].map(String), text));
  if (checks.some((x) => !x.ok)) return { checks };
  const unit = s.jedinica ? ` ${s.jedinica}` : "";
  const dec = Math.max(...[s.fronta, ...spots.map((m) => m.udaljenost)].map((v) => (String(v).split(".")[1] ?? "").length));
  const fmt = (v) => v.toFixed(dec).replace(".", ",");
  const [ys, yf] = [202, 62];
  const Y = (d) => ys - (d / s.fronta) * (ys - yf);
  const chalk = s.podloga !== "papir";
  const [sx0, sx1] = chalk ? [146, 174] : [142, 178];
  let body = `<rect class="liquid" x="92" y="218" width="136" height="24"/>`;
  body += `<path class="ln" d="M90,40 V244 H230 V40"/>`;
  body += `<rect class="strip" x="${sx0}" y="20" width="${sx1 - sx0}" height="220" rx="${chalk ? 10 : 1}"/>`;
  body += line([sx0 - 6, ys], [sx1 + 6, ys], "ln thin") + label([84, ys], "startna linija", "lbl small dim-t", "end");
  body += line([sx0 - 6, yf], [sx1 + 6, yf], "ln dash acc") + label([84, yf], "fronta otapala", "lbl small acc-t", "end");
  spots.forEach((m) => {
    const y = Y(m.udaljenost);
    body += `<ellipse class="spot ${SPOT[m.boja] ?? "k5"}" cx="160" cy="${f(y)}" rx="${chalk ? 11 : 13}" ry="6"/>`;
    if (m.oznaka) body += label([136, y], m.oznaka, "lbl small", "end");
  });
  if (s.kote !== false) {
    const marks = [...spots.map((m) => ({ d: m.udaljenost, t: `d${SUB[1]}` })), { d: s.fronta, t: `d${SUB[2]}` }];
    marks.forEach((m, i) => {
      const x = 244 + 15 * i;
      body += line([sx1 + 4, Y(m.d)], [x + 4, Y(m.d)], "ln thin dash");
      body += arrow([x, ys], [x, Y(m.d) + 2], i === marks.length - 1 ? "acc" : "c2");
    });
    body += line([sx1 + 4, ys], [244 + 15 * marks.length, ys], "ln thin dash");
    const lx = 244 + 15 * marks.length;
    marks.forEach((m, i) => {
      body += label([lx, Y(m.d)], `${m.t} = ${fmt(m.d)}${unit}`, i === marks.length - 1 ? "lbl small acc-t" : "lbl small c2-t", "start");
    });
  }
  return { svg: svg(body, 252), checks };
}

// Titracijska krivulja: pH otopine kiseline dok joj se dodaje lužina, izračunan iz koncentracija (jaka kiselina,
// ili slaba s Ka). Točka ekvivalencije i područje prijelaza indikatora označeni su.
const INDICATORS = { fenolftalein: [8.2, 10], "metiloranž": [3.1, 4.4], bromtimolplavo: [6, 7.6] };

function titrationPH(acid, cb, vb) {
  const vt = acid.V + vb;
  const ca = (acid.c * acid.V) / vt;
  const na = (cb * vb) / vt;
  const strong = !(acid.Ka > 0);
  const p = acid.protoni ?? 1;
  const g = (lh) => {
    const h = 10 ** lh;
    return h + na - 1e-14 / h - (strong ? p * ca : (ca * acid.Ka) / (acid.Ka + h));
  };
  let [lo, hi] = [-15, 1.5];
  for (let i = 0; i < 64; i++) {
    const m = (lo + hi) / 2;
    if (g(m) > 0) hi = m;
    else lo = m;
  }
  return -(lo + hi) / 2;
}

function titration(s, text) {
  const acids = [].concat(s.kiselina ?? []);
  const cb = s.luzina?.c;
  const checks = [];
  if (!acids.length || acids.length > 2) return { checks: [fail("titracija: 1 ili 2 kiseline")] };
  if (!positive(cb)) checks.push(fail("koncentracija lužine mora biti pozitivan broj"));
  for (const a of acids) {
    if (!positive(a.c, a.V)) checks.push(fail(`${a.formula ?? "kiselina"}: c i V moraju biti pozitivni`));
    if (a.protoni != null && ![1, 2, 3].includes(a.protoni)) checks.push(fail("protoni: 1, 2 ili 3"));
    if (a.Ka != null && !positive(a.Ka)) checks.push(fail("Ka mora biti pozitivan broj ili null (jaka kiselina)"));
  }
  if (checks.length) return { checks };
  const veq = acids.map((a) => ((a.protoni ?? 1) * a.c * a.V) / cb);
  const to = s.do ?? Math.ceil((Math.max(...veq) * 1.6) / 5) * 5;
  const okTo = to >= Math.max(...veq) * 1.15 && to <= Math.max(...veq) * 4;
  checks.push({ ok: okTo, text: `os volumena do ${num(to)} mL ${okTo ? "obuhvaća" : "NE obuhvaća lijepo"} točku ekvivalencije (${veq.map(num).join(", ")} mL)` });
  if (s.indikator && !INDICATORS[s.indikator]) checks.push(fail(`nepoznat indikator „${s.indikator}“ (fenolftalein, metiloranž, bromtimolplavo)`));
  checks.push(...numbersInText(veq.map(num), text));
  if (checks.some((x) => !x.ok)) return { checks };
  const T = axesFit([0, to], [0, 14]);
  let body = "";
  if (s.indikator) {
    const [a, b] = INDICATORS[s.indikator];
    const [p, q] = [T([0, b]), T([to, a])];
    body += `<rect class="band" x="${f(p[0])}" y="${f(p[1])}" width="${f(q[0] - p[0])}" height="${f(q[1] - p[1])}"/>`;
  }
  body += axes(T, [0, to], [0, 14], s.os_x ?? `V(${s.luzina?.formula ?? "NaOH"}) / mL`, "pH");
  if (s.indikator) body += label([T([0, 0])[0] + 6, T([0, INDICATORS[s.indikator][1]])[1] - 8], s.indikator, "lbl small dim-t", "start");
  acids.forEach((a, i) => {
    const pts = [];
    for (let k = 0; k <= 400; k++) {
      const v = (to * k) / 400;
      pts.push(T([v, Math.max(0, Math.min(14, titrationPH(a, cb, v)))]));
    }
    body += `<polyline class="curve c${i + 1}" points="${pts.map((p) => p.map(f).join(",")).join(" ")}"/>`;
    const e = T([veq[i], titrationPH(a, cb, veq[i])]);
    body += line([e[0], e[1]], [e[0], T([0, 0])[1]], "ln thin dash") + dot(e, "pt acc-f");
    const tag = `${acids.length > 1 ? `${a.formula ?? ""}: ` : ""}${num(veq[i])} mL`;
    body += label([e[0] + 7, e[1] + (i ? 14 : -10)], tag, `lbl small c${i + 1}-t`, "start");
  });
  return { svg: svg(body), checks };
}

const RENDERERS = {
  trokut: triangle, kruznica: circle, graf: graph, koordinatni: coords, pravokutnik: rectangle,
  kvadar: cuboid, ljestvica: scale, sile: forces, strujni_krug: circuit,
  energijski_dijagram: energyDiagram, strukturna_formula: structure, elektrokemijski_clanak: cell, ljuske: shells,
  cestice: particles, kromatogram: chromatogram, titracija: titration,
};
export const SKETCH_TYPES = Object.keys(RENDERERS);

// { svg, checks }: svg je null ako opis ne prođe provjere (tada se zadatak prikazuje bez skice).
export function renderSketch(spec, text) {
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
