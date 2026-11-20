const scope = /** @type {import("./protocol").WorkerScope} */ (/** @type {unknown} */ (self));

async function initialize() {
  const vendor = new URL("./vendor/", scope.location.href).href;
  scope.importScripts(new URL("pyodide.js", vendor).href);
  const [python, response] = await Promise.all([
    scope.loadPyodide({ indexURL: vendor }), fetch(new URL("simulation.py", scope.location.href)),
  ]);
  if (!response.ok) throw new Error("Sailing model unavailable.");
  python.FS.writeFile("/home/pyodide/simulation.py", await response.text());
  const apply = /** @type {(request: string) => string} */ (
    python.runPython("from simulation import create\ncreate().apply")
  );
  scope.postMessage({ type: "state", state: JSON.parse(apply(JSON.stringify({ action: "state" }))) });
  scope.onmessage = ({ data }) => {
    try {
      scope.postMessage({ type: "state", state: JSON.parse(apply(JSON.stringify(data))) });
    } catch (error) {
      const message = String(error).match(/ValueError: ([^\r\n]+)/)?.[1];
      scope.postMessage({ type: "failed", message: message ?? "The sailing model could not finish." });
    }
  };
}

initialize().catch(() => scope.postMessage({ type: "fatal" }));
