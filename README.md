# Sailing Simulator

[Run in your browser](https://pubino.github.io/sail-sim/).

The browser uses the Java app’s in-place value iteration, wind transition
matrix, tacking rule, and initial NE wind. The default lake and costs come
from the desktop input panel. The initial seed of 524 makes the first
voyage reproducible. Calculate finds the policy and runs one voyage, and
Simulate 100 extends the current histogram with 100 more voyages.

The transition editor accepts three probabilities per wind direction,
matching the editable cells in Java. Each row must add up to one.

The Python model runs in a Pyodide worker. The test suite compares it with
the original Java app and exercises the browser controls.

### Browser development

```sh
docker-compose up --build preview
# Open http://localhost:8000
docker-compose run --build --rm js-check
docker-compose run --build --rm test
```

For a local test with JDK 25, uv and Node installed:

```sh
uv sync --locked --python 3.13.12
uv run python scripts/build_site.py
uv run playwright install --only-shell chromium
uv run python -m unittest discover -s tests -v
npm ci
npm run check
```

Part of [Prof. Robert Vanderbei's Java Applications](https://pubino.github.io/vanderbei-apps#sail-sim).

A Java application for simulating optimal sailing strategies using dynamic programming. Originally based on Professor Robert Vanderbei, Princeton University.

## Overview

This simulator computes optimal sailing strategies given:
- **Wind transition probabilities**: Markov chain model for wind direction changes
- **Sailing times**: Time costs for different points of sail (into wind, upwind, crosswind, downwind, away from wind)
- **Tacking delay**: Time penalty for changing tack

The application uses dynamic programming to find the expected minimum time to sail from point A to point B, then simulates random wind conditions to demonstrate the optimal strategy.

## Requirements

- Java 14+ (for running the JAR)
- Java 14+ with `jpackage` (for building macOS app bundle)

## Building

```bash
# Build the JAR file
make jar

# Build macOS .app bundle (unsigned)
make app
```

## Running

```bash
# Run the JAR
make run

# Or run directly
java -jar dist/SailingSim.jar
```

## Usage

1. Click "Push here to go sailing" to open the simulator
2. Adjust wind transition probabilities in the matrix (rows = current wind, columns = next wind)
3. Set sailing times for each point of sail
4. Configure the lake size and start/end positions
5. Click "Calculate" to compute the optimal strategy
6. Click "Simulate" to run a simulation with random wind changes
7. Click "Simulate 100" to run 100 simulations
8. Click "View Stats" to see a histogram of travel times

## Project Structure

```
sail-sim/
├── sail/
│   ├── Sail.java                  # Main application and simulation logic
│   ├── beach.gif                  # Beach background image
│   ├── princeton_shield_small.gif # Princeton logo
│   └── sailboat_right.gif         # Sailboat sprite
├── myutil/
│   ├── AppletFrame.java           # Applet-to-Frame wrapper
│   ├── Format.java                # Number formatting utilities
│   ├── GL.java                    # Graphics library helpers
│   └── SL.java                    # String parsing utilities
├── Makefile                       # Build configuration
└── .github/workflows/             # CI/CD configuration
```

## Algorithm

The simulator uses value iteration to solve a stochastic shortest path problem:
- State space: position (x, y), current tack, wind direction
- Action space: 8 possible headings (N, NE, E, SE, S, SW, W, NW)
- Transition: deterministic position update, stochastic wind change
- Cost: sailing time + tacking delay
