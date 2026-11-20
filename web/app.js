import { drawHistogram, drawRoute, find, time } from "./view.js";

const form = find("settings", HTMLFormElement);
const inputs = find("inputs", HTMLFieldSetElement);
const advanced = find("advanced", HTMLFieldSetElement);
const stats = find("stats", HTMLElement);
const simulate = find("simulate", HTMLButtonElement);
const batch = find("batch", HTMLButtonElement);
const showStats = find("show-stats", HTMLButtonElement);
const status = find("status", HTMLElement);
const error = find("error", HTMLElement);
const retry = find("retry", HTMLButtonElement);
const worker = new Worker(new URL("./worker.js", import.meta.url));
/** @type {import("./protocol").State | null} */
let state = null;
let busy = true;
let edited = false;

const compass = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"];
const wind = [
  [0.3, 0.4, 0.3], [0.4, 0.3, 0.3], [0.4, 0.3, 0.3], [0.4, 0.3, 0.3],
  [0.4, 0.2, 0.4], [0.3, 0.3, 0.4], [0.3, 0.3, 0.4], [0.3, 0.3, 0.4],
].map((row, direction) => {
  const tableRow = find("wind", HTMLTableSectionElement).insertRow();
  const label = document.createElement("th");
  label.scope = "row";
  label.textContent = compass[direction];
  tableRow.append(label);
  return row.map((value, index) => {
    const input = document.createElement("input");
    input.type = "number";
    input.min = "0";
    input.max = "1";
    input.step = "any";
    input.value = String(value);
    input.required = true;
    input.setAttribute("aria-label", `${compass[direction]} ${["left", "same", "right"][index]}`);
    tableRow.insertCell().append(input);
    return input;
  });
});

/** @param {string} name */
function read(name) {
  return find(name, HTMLInputElement).valueAsNumber;
}

function controls() {
  inputs.disabled = advanced.disabled = busy;
  simulate.disabled = batch.disabled = !state || edited;
  showStats.disabled = !state;
}

/** @param {import("./protocol").Request} request @param {string} message */
function send(request, message) {
  if (busy) return;
  busy = true;
  error.hidden = true;
  status.textContent = message;
  controls();
  worker.postMessage(request);
}

function showStatistics() {
  const result = state?.statistics;
  if (!result) return;
  for (const key of /** @type {const} */ (["minimum", "mean", "median", "maximum"])) {
    find(key, HTMLElement).textContent = time(result[key]);
  }
  drawHistogram(result);
}

function fail() {
  busy = true;
  controls();
  worker.terminate();
  status.textContent = "";
  error.textContent = "Python could not load. Reload to try again.";
  error.hidden = retry.hidden = false;
}

worker.onerror = fail;
worker.onmessage = (/** @type {MessageEvent<import("./protocol").Reply>} */ { data }) => {
  if (data.type === "fatal") { fail(); return; }
  busy = false;
  if (data.type === "failed") {
    status.textContent = "";
    error.textContent = data.message;
    error.hidden = false;
  } else {
    edited = false;
    state = data.state;
    status.textContent = `Policy converged in ${state.sweeps} sweeps.`;
    for (const key of /** @type {const} */ (["expected", "last", "count"])) {
      find(key, HTMLElement).textContent = time(state[key]);
    }
    drawRoute(state);
    if (!stats.hidden) showStatistics();
  }
  controls();
};

form.addEventListener("input", () => {
  edited = true;
  status.textContent = "Calculate to apply the changed settings.";
  controls();
});
form.addEventListener("submit", (event) => {
  event.preventDefault();
  send({ action: "calculate", parameters: {
    size: read("size"), seed: read("seed"), start: [read("start-x"), read("start-y")],
    end: [read("end-x"), read("end-y")], times: ["into", "up", "cross", "down", "away"].map(read),
    delay: read("delay"), wind: wind.map((row) => row.map((item) => item.valueAsNumber)),
  } }, "Calculating the sailing policy…");
});
simulate.addEventListener("click", () => send({ action: "simulate", count: 1 }, "Simulating a voyage…"));
batch.addEventListener("click", () => send({ action: "simulate", count: 100 }, "Simulating 100 voyages…"));
showStats.addEventListener("click", () => {
  stats.hidden = false;
  showStatistics();
  stats.scrollIntoView({ block: "nearest" });
});
retry.addEventListener("click", () => window.location.reload());
