import json
import subprocess
import tempfile
import unittest
from collections import Counter
from pathlib import Path
from typing import Any

from web.simulation import DEFAULTS, DX, DY, Problem, Sailing, create

ROOT = Path(__file__).resolve().parents[1]


def legs(route: list[list[int]]) -> list[list[int]]:
    return [
        [x, y, tack, wind, DX[heading], DY[heading]]
        for x, y, tack, wind, heading in route
    ]


class SailingTest(unittest.TestCase):
    def test_policy_route_and_101_trials_match_java(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(
                [
                    "javac",
                    "--release",
                    "25",
                    "-d",
                    directory,
                    ROOT / "sail/Sail.java",
                    *sorted((ROOT / "myutil").glob("*.java")),
                    ROOT / "tests/SailingReference.java",
                ],
                check=True,
                timeout=60,
            )
            for spec in (
                (524, 5, 1, 1, 4, 4, 3, False),
                (-420, 8, 6, 4, 1, 1, 7.5, False),
                (0, 6, 4, 4, 1, 2, 3, True),
            ):
                with self.subTest(spec=spec):
                    seed, size, x, y, xx, yy, delay, calm = spec
                    config = DEFAULTS | {
                        "seed": seed,
                        "size": size,
                        "start": [x, y],
                        "end": [xx, yy],
                        "delay": delay,
                    }
                    if calm:
                        config["wind"] = [[0, 1, 0] for _ in range(8)]
                    model = Sailing(Problem.read(config))
                    output = subprocess.check_output(
                        [
                            "java",
                            "-Djava.awt.headless=true",
                            "-cp",
                            directory,
                            "sail.SailingReference",
                            *(str(item).lower() for item in spec),
                        ],
                        text=True,
                        timeout=20,
                    )
                    ref = json.loads(output.splitlines()[-1])

                    def shape(values: Any) -> list[list[float]]:
                        return [
                            [v for row in cell for v in row]
                            for column in values
                            for cell in column
                        ]

                    self.assertEqual(model.values, shape(ref["values"]))
                    self.assertEqual(model.headings, shape(ref["headings"]))
                    self.assertEqual(model.sweeps, ref["sweeps"])
                    self.assertEqual(model.residual, ref["residual"])
                    model.simulate(1)
                    self.assertEqual(legs(model.route), ref["first"])
                    model.simulate(100)
                    self.assertEqual(model.trials, ref["times"])
                    self.assertEqual(legs(model.route), ref["last"])
                    self.assertEqual(
                        model.state()["statistics"]["histogram"],
                        sorted(Counter(ref["times"]).items()),
                    )
                    self.assertEqual(
                        model.state()["statistics"]["median"], sorted(ref["times"])[50]
                    )

    def test_invalid_settings_and_zero_length_voyage(self) -> None:
        app = create()
        before = app.sailing.state()
        bad = DEFAULTS | {"wind": [[0.3, 0.3, 0.3] for _ in range(8)]}
        with self.assertRaisesRegex(ValueError, "must add up"):
            app.apply(json.dumps({"action": "calculate", "parameters": bad}))
        self.assertEqual(app.sailing.state(), before)
        near = Sailing(Problem.read(DEFAULTS | {"end": [1, 1]}))
        near.simulate(1)
        self.assertEqual(near.trials, [0])
        self.assertEqual(near.route, [])
