/**
 * @template {Element} T
 * @param {string} id
 * @param {{new(): T}} constructor
 */
export function find(id, constructor) {
  const element = document.getElementById(id);
  if (!(element instanceof constructor)) throw new Error(`Missing element ${id}`);
  return element;
}

/** @param {number | null} value */
export function time(value) {
  return value === null ? "—" : value.toLocaleString(undefined, { maximumFractionDigits: 2 });
}

const compass = [[0, 1], [1, 1], [1, 0], [1, -1], [0, -1], [-1, -1], [-1, 0], [-1, 1]];

/** @param {import("./protocol").State} state */
export function drawRoute(state) {
  const canvas = find("route", HTMLCanvasElement);
  const p = canvas.getContext("2d");
  if (!p) throw new Error("Canvas unavailable.");
  const pad = 36;
  const width = canvas.width;
  const scale = (width - 2 * pad) / state.size;
  /** @param {number} x @param {number} y @returns {[number, number]} */
  const position = (x, y) => [pad + x * scale, width - pad - y * scale];
  p.fillStyle = "#eef6fa";
  p.fillRect(0, 0, width, width);
  p.fillStyle = "#dceef6";
  p.fillRect(pad, pad, width - 2 * pad, width - 2 * pad);
  p.strokeStyle = "#b6cedb";
  p.lineWidth = 1;
  p.beginPath();
  for (let n = 0; n <= state.size; n++) {
    p.moveTo(...position(n, 0));
    p.lineTo(...position(n, state.size));
    p.moveTo(...position(0, n));
    p.lineTo(...position(state.size, n));
  }
  p.stroke();

  p.strokeStyle = "#28637f";
  p.lineWidth = 3;
  p.lineJoin = "round";
  p.beginPath();
  p.moveTo(...position(...state.start));
  for (const [x, y] of state.route) p.lineTo(...position(x, y));
  p.lineTo(...position(...state.end));
  p.stroke();

  p.strokeStyle = "#557b99";
  p.lineWidth = 1.4;
  const spacing = Math.min(17, scale * 0.23);
  for (const [x, y, , wind] of state.route) {
    const [xx, yy] = position(x, y);
    const [wx, wy] = compass[wind];
    p.beginPath();
    p.moveTo(xx, yy);
    p.lineTo(xx + spacing * wx, yy - spacing * wy);
    p.stroke();
    p.beginPath();
    p.arc(xx + spacing * wx, yy - spacing * wy, 2.2, 0, 2 * Math.PI);
    p.stroke();
  }

  p.font = "bold 15px system-ui";
  p.textAlign = "center";
  p.textBaseline = "middle";
  const markers = /** @type {[import("./protocol").Point, string, string][]} */ (
    [[state.start, "A", "#174f77"], [state.end, "B", "#227766"]]
  );
  for (const [point, label, color] of markers) {
    const [x, y] = position(...point);
    p.beginPath();
    p.fillStyle = color;
    p.arc(x, y, 12, 0, 2 * Math.PI);
    p.fill();
    p.fillStyle = "#ffffff";
    p.fillText(label, x, y);
  }
}

/** @param {import("./protocol").Statistics} stats */
export function drawHistogram(stats) {
  const canvas = find("histogram", HTMLCanvasElement);
  const p = canvas.getContext("2d");
  if (!p) throw new Error("Canvas unavailable.");
  const { width, height } = canvas;
  const left = 46, right = width - 28, base = height - 48, top = 28;
  const maxCount = Math.max(...stats.histogram.map(([, count]) => count));
  const span = stats.maximum - stats.minimum;
  /** @param {number} value */
  const px = (value) => span === 0 ? (left + right) / 2 : left + (value - stats.minimum) * (right - left) / span;
  p.fillStyle = "#f5f8fb";
  p.fillRect(0, 0, width, height);
  p.strokeStyle = "#7395a8";
  p.beginPath();
  p.moveTo(left, base);
  p.lineTo(right, base);
  p.stroke();
  p.strokeStyle = "#174f77";
  p.lineWidth = 3;
  p.beginPath();
  for (const [travel, count] of stats.histogram) {
    const x = px(travel);
    p.moveTo(x, base);
    p.lineTo(x, base - (base - top) * count / maxCount);
  }
  p.stroke();
  p.font = "14px system-ui";
  p.fillStyle = "#334f68";
  p.textAlign = "center";
  for (const value of new Set([stats.minimum, stats.maximum])) p.fillText(time(value), px(value), base + 19);
  p.fillText("Travel time", width / 2, height - 7);
}
