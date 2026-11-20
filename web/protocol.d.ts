export type Point = [number, number];
export type Leg = [x: number, y: number, tack: number, wind: number, heading: number];
export interface Statistics {
  minimum: number;
  maximum: number;
  median: number;
  mean: number;
  histogram: [number, number][];
}
export interface State {
  size: number; start: Point; end: Point; expected: number; residual: number; sweeps: number;
  count: number; route: Leg[]; last: number | null; statistics: Statistics | null;
}
export interface Parameters {
  size: number; start: Point; end: Point; seed: number;
  times: number[]; delay: number; wind: number[][];
}
export type Request =
  | { action: "calculate"; parameters: Parameters }
  | { action: "simulate"; count: 1 | 100 };
export type Reply =
  | { type: "state"; state: State }
  | { type: "failed"; message: string }
  | { type: "fatal" };
export interface WorkerScope {
  importScripts(url: string): void;
  location: Location;
  loadPyodide(options: { indexURL: string }): Promise<{
    FS: { writeFile(path: string, contents: string): void };
    runPython(code: string): unknown;
  }>;
  onmessage: (event: MessageEvent<Request>) => void;
  postMessage(message: Reply): void;
}
