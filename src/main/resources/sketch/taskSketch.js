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
  checks.push({ ok: Math.abs(n - Math.round(n)) < 1e-6 && n <= 200, text: `od ${num(od)} do ${num(to)} ima ${num(n)} razmaka (cijeli broj, najviše 200)` });
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

const RENDERERS = {
  trokut: triangle, kruznica: circle, graf: graph, koordinatni: coords, pravokutnik: rectangle,
  kvadar: cuboid, ljestvica: scale, sile: forces, strujni_krug: circuit,
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
