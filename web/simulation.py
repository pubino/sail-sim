"""The original Sailing Simulator's value iteration and wind simulation."""

import json
from collections import Counter
from dataclasses import dataclass
from math import sqrt
from typing import Any

INFINITY = 1_000_000.0
TOLERANCE = 1e-3
DX = (0, 1, 1, 1, 0, -1, -1, -1)
DY = (1, 1, 0, -1, -1, -1, 0, 1)
TURN = (0, 1, 2, 3, 4, 3, 2, 1)
TACK = (2, 1, 1, 1, 2, 0, 0, 0)

# Each row gives the previous, same and next compass direction.
DEFAULT_WIND = (
    (0.3, 0.4, 0.3),
    (0.4, 0.3, 0.3),
    (0.4, 0.3, 0.3),
    (0.4, 0.3, 0.3),
    (0.4, 0.2, 0.4),
    (0.3, 0.3, 0.4),
    (0.3, 0.3, 0.4),
    (0.3, 0.3, 0.4),
)
DEFAULTS: dict[str, Any] = {
    "size": 5,
    "start": [1, 1],
    "end": [4, 4],
    "seed": 524,
    "times": [100000, 4, 3, 2, 1],
    "delay": 3,
    "wind": DEFAULT_WIND,
}


class JavaRandom:
    def __init__(self, seed: int) -> None:
        self.state = (seed ^ 0x5DEECE66D) & ((1 << 48) - 1)

    def bits(self, count: int) -> int:
        self.state = (self.state * 0x5DEECE66D + 11) & ((1 << 48) - 1)
        return self.state >> (48 - count)

    def random(self) -> float:
        return ((self.bits(26) << 27) + self.bits(27)) / (1 << 53)


def integer(value: Any, lo: int, hi: int, label: str) -> int:
    if type(value) is not int or not lo <= value <= hi:
        raise ValueError(f"{label} must be a whole number from {lo} to {hi}.")
    return value


def real(value: Any, lo: float, hi: float, label: str) -> float:
    if type(value) not in (int, float) or not lo <= value <= hi:
        raise ValueError(f"{label} must be a number from {lo:g} to {hi:g}.")
    return float(value)


@dataclass(frozen=True)
class Problem:
    size: int
    start: tuple[int, int]
    end: tuple[int, int]
    seed: int
    times: list[float]
    delay: float
    wind: list[list[float]]

    @classmethod
    def read(cls, config: object) -> "Problem":
        if not isinstance(config, dict):
            raise ValueError("Choose valid problem settings.")
        size = integer(config.get("size"), 2, 24, "Lake size")

        def cell(label: str) -> tuple[int, int]:
            xy = config.get(label)
            if not isinstance(xy, (list, tuple)) or len(xy) != 2:
                raise ValueError(f"Choose both {label} coordinates.")
            return (
                integer(xy[0], 1, size - 1, f"{label.title()} x"),
                integer(xy[1], 1, size - 1, f"{label.title()} y"),
            )

        times = config.get("times")
        if not isinstance(times, (list, tuple)) or len(times) != 5:
            raise ValueError("Set all five sailing times.")
        wind = config.get("wind")
        if not isinstance(wind, (list, tuple)) or len(wind) != 8:
            raise ValueError("Set all eight rows of wind probabilities.")
        matrix: list[list[float]] = []
        for i, row in enumerate(wind):
            if not isinstance(row, (list, tuple)) or len(row) != 3:
                raise ValueError("Set three probabilities per wind direction.")
            next_row = [0.0] * 8
            for change, value in zip((-1, 0, 1), row, strict=True):
                next_row[(i + change) % 8] = real(value, 0, 1, "Wind probability")
            if abs(sum(next_row) - 1) > 1e-10:
                raise ValueError("Each wind probability row must add up to 1.")
            matrix.append(next_row)
        return cls(
            size,
            cell("start"),
            cell("end"),
            integer(config.get("seed"), -(2**31), 2**31 - 1, "Seed"),
            [real(value, 0.000001, INFINITY, "Sailing time") for value in times],
            real(config.get("delay"), 0, INFINITY, "Tacking delay"),
            matrix,
        )

    def cost(self, prev_tack: int, wind: int, heading: int) -> float:
        bearing = (wind - heading) % 8
        tack = TACK[bearing]
        delay = 0.0 if tack == prev_tack or tack == 2 or prev_tack == 2 else self.delay
        factor = sqrt(2) if heading % 2 else 1.0
        return delay + self.times[TURN[bearing]] * factor


class Sailing:
    def __init__(self, problem: Problem) -> None:
        self.problem = problem
        self.state_width = problem.size + 1
        self.dest = self.index(*problem.end)
        self.values = [[INFINITY] * 24 for _ in range(self.state_width**2)]
        self.headings = [[0] * 24 for _ in self.values]
        self.values[self.dest] = [0.0] * 24
        self.rng = JavaRandom(problem.seed)
        self.trials: list[float] = []
        self.route: list[list[int]] = []
        self.residual, self.sweeps = self.solve()
        self.expected = self.values[self.index(*problem.start)][9]
        if self.expected >= INFINITY:
            raise ValueError(
                "The start has no computed route below the Java cost ceiling."
            )

    def index(self, x: int, y: int) -> int:
        return x * self.state_width + y

    def solve(self) -> tuple[float, int]:
        problem = self.problem
        offsets = [(x * self.state_width + y) for x, y in zip(DX, DY, strict=True)]
        interiors = [
            self.index(x, y)
            for x in range(1, problem.size)
            for y in range(1, problem.size)
            if self.index(x, y) != self.dest
        ]
        transitions = [
            [(w, weight) for w, weight in enumerate(row) if weight]
            for row in problem.wind
        ]
        actions = [
            [
                (
                    (TACK[(wind - h) % 8] * 8),
                    problem.cost(tack, wind, h),
                )
                for h in range(8)
            ]
            for tack in range(3)
            for wind in range(8)
        ]
        for sweep in range(1, 3001):
            error = 0.0
            for index in interiors:
                adjacent = [self.values[index + offset] for offset in offsets]
                values = self.values[index]
                best = self.headings[index]
                for state, candidates in enumerate(actions):
                    new = INFINITY
                    winner = 0
                    row = transitions[state % 8]
                    for h, (tack_offset, cost) in enumerate(candidates):
                        neighbor = adjacent[h]
                        expected = 0.0
                        for to_wind, chance in row:
                            expected += chance * neighbor[tack_offset + to_wind]
                        expected += cost
                        if expected < new:
                            new, winner = expected, h
                    error = max(error, abs(new - values[state]))
                    values[state], best[state] = new, winner
            if error <= TOLERANCE:
                return error, sweep
        raise ValueError("The policy did not converge within 3,000 sweeps.")

    def next_wind(self, wind: int) -> int:
        number = self.rng.random()
        cumulative = 0.0
        for index, chance in enumerate(self.problem.wind[wind]):
            cumulative += chance
            if number < cumulative:
                return index
        return 0  # Java's rounding fallback.

    def voyage(self) -> tuple[float, list[list[int]]]:
        problem = self.problem
        x, y = problem.start
        tack, wind = 1, 1
        total = 0.0
        route: list[list[int]] = []
        while (x, y) != problem.end:
            if (
                not (1 <= x < problem.size and 1 <= y < problem.size)
                or len(route) == 5000
            ):
                raise ValueError("The computed policy did not reach the destination.")
            heading = self.headings[self.index(x, y)][tack * 8 + wind]
            total += problem.cost(tack, wind, heading)
            tack = TACK[(wind - heading) % 8]
            route.append([x, y, tack, wind, heading])
            x, y = x + DX[heading], y + DY[heading]
            wind = self.next_wind(wind)
        return total, route

    def simulate(self, count: int) -> None:
        seed = self.rng.state
        try:
            voyages = [self.voyage() for _ in range(count)]
        except ValueError:
            self.rng.state = seed
            raise
        self.trials.extend(time for time, _ in voyages)
        self.route = voyages[-1][1]

    def state(self) -> dict[str, Any]:
        frequencies = Counter(self.trials)
        times = sorted(self.trials)
        stats = None
        if times:
            stats = {
                "minimum": times[0],
                "maximum": times[-1],
                "median": times[len(times) // 2],
                "mean": sum(t * frequencies[t] for t in sorted(frequencies))
                / len(times),
                "histogram": sorted(frequencies.items()),
            }
        return {
            "size": self.problem.size,
            "start": self.problem.start,
            "end": self.problem.end,
            "expected": self.expected,
            "residual": self.residual,
            "sweeps": self.sweeps,
            "count": len(times),
            "route": self.route,
            "last": self.trials[-1] if times else None,
            "statistics": stats,
        }


class App:
    def __init__(self) -> None:
        self.sailing = Sailing(Problem.read(DEFAULTS))
        self.sailing.simulate(1)

    def apply(self, raw: str) -> str:
        data = json.loads(raw)
        if not isinstance(data, dict):
            raise ValueError("Choose a valid sailing action.")
        action = data.get("action")
        if action == "calculate":
            configured = Sailing(Problem.read(data.get("parameters")))
            configured.simulate(1)
            self.sailing = configured
        elif action == "simulate":
            count = data.get("count")
            if count not in (1, 100) or type(count) is not int:
                raise ValueError("Choose 1 or 100 voyages.")
            if len(self.sailing.trials) + count > 10_000:
                raise ValueError(
                    "Calculate a new policy to start a new set of voyages."
                )
            self.sailing.simulate(count)
        elif action != "state":
            raise ValueError("Choose a valid sailing action.")
        return json.dumps(self.sailing.state(), allow_nan=False)


def create() -> App:
    return App()
