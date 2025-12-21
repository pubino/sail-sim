# Sailing Simulator

Part of [Vanderbei's Java Simulations](https://github.com/pubino/vanderbei-apps) collection.

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
