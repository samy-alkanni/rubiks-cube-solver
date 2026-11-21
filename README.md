# Rubik's Cube Solver

An interactive 3D Rubik's cube simulator and solver built in Java. It can scan a real cube from photos using computer vision, then solve it step by step with the CFOP method.

*Final integrative project — Science, Computer Science and Mathematics (SIM) program, Cégep Gérald-Godin (2026).*
*Authors: Samy Al-Kanni and Victor Noel.*

## Features

- **Interactive 3D cube** — rotate the view with the mouse; face turns are animated.
- **Scramble**
  - *Automatic*: random scramble.
  - *Interactive*: scramble it yourself with the keyboard (up to 20 moves).
- **Scan a real cube** — select a folder containing 6 photos (one per face):
  - stickers are located with OpenCV (adaptive thresholding + contour detection),
  - each sticker's color is classified in HSV color space,
  - each photo is matched to its face automatically by its center color (no file naming required).
- **Three solving modes**
  - *Automatic*: replays the inverse of the scramble.
  - *Interactive*: step through the solution move by move.
  - *Human method (CFOP)*: Cross → F2L → 2-look OLL → 2-look PLL, shown stage by stage.
- **Session stats** — session time, number of scrambles and solves, move counts, last durations.
- **Built-in guide** — official move notation and keyboard shortcuts displayed in the app.
- **Unit tests** — 7 tests covering move logic (inverses, quarter turns, double moves) and verifying that CFOP solves a scramble.

## Tech stack

Java 21+ · JavaFX 25 (3D) · OpenCV 4.9 · Maven

## Requirements

- JDK 21 or newer (required by JavaFX 25)
- Windows (the launcher script is a `.cmd` file)

## Run

Double-click `run.cmd`, or from a terminal:

```
mvnw.cmd javafx:run
```

The Maven Wrapper downloads Maven and all dependencies (JavaFX, OpenCV) on the first run.

## Keyboard shortcuts

**Interactive scramble**

| Face | Clockwise | Counterclockwise | Double |
|---|---|---|---|
| F | F | G | S |
| B | B | V | C |
| L | L | K | J |
| R | R | E | W |
| U | U | Q | A |
| D | D | O | I |

**Interactive solve**: `N` next move · `B` previous move

## Scanning tips

Take one photo per face, straight on, with even lighting. Colors are detected from the center of each sticker; if a color is misread, better lighting usually fixes it.
