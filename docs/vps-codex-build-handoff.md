# VPS Codex build handoff

## Model routing

- Luna is the coordinator and reviewer.
- Spark is the coding agent.
- Terra is reserved for a difficult blocker or final audit.

## Repository

Clone the public repository:

    https://github.com/costavong-pixel/iqola.git

Work on a feature branch. Do not rewrite main history.

## Product rule

IQOla must look like one app. Read and follow:

    docs/visual-design-system.md

Upstream repositories provide mechanics and logic only. Do not copy their
screens, navigation, colors, typography, menus, or visual layouts.

## Phase 1 pilot

Implement only the Puzzle Reverse Stroop mechanic as the first pilot.

Source reference:

    puzzle source ID: reverse_stroop
    upstream project: sidhant947/Puzzle
    license: GPL-3.0

The pilot must:

1. Keep the existing IQOla Android app buildable.
2. Use the existing IQOla front-facing 2D game frame.
3. Preserve the shared header, instruction area, board surface, controls,
   result state, and bottom ad reservation.
4. Port the mechanic into the current Android implementation unless Luna
   approves a documented architecture change.
5. Leave the existing 12 MVP games playable.
6. Add level/round progression and a best score using the existing progress
   pattern.
7. Keep gameplay free of interactive banner ads during a timed round.
8. Preserve GPL-3.0 notices for incorporated Puzzle-derived logic.

## Build and verification

Verify the VPS has JDK, Android SDK, and Gradle. If the repository lacks a
Gradle wrapper, add one so builds are reproducible.

Run the available unit/build checks and produce a debug APK. Do not commit
build outputs. Record the exact build command and result in the handoff.

## Stop conditions

Stop and report to Luna before proceeding if:

- the current Android architecture must be replaced;
- the Puzzle mechanic cannot be ported without copying its UI;
- GPL attribution or corresponding-source handling is unclear;
- the app's shared visual frame would need to change;
- the build cannot be reproduced on the VPS.

Do not implement the other selected games until the Reverse Stroop pilot
passes visual and build review.
