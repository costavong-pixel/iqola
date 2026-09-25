# IQOla visual design system

## Product rule

Every game must look like it belongs to IQOla. Upstream repositories provide
game mechanics and logic only; their screens, colors, typography, menus, and
art direction are not copied into the app.

The visual direction is a calm, premium brain-training laboratory: clean,
front-facing 2D boards, soft surfaces, clear instructions, and satisfying
feedback. It should feel intelligent and approachable rather than like a
collection of unrelated arcade games.

## Shared perspective

- Portrait-first, front-facing 2D presentation.
- No game-specific camera angles, tilted boards, or unrelated 3D styles.
- Every board sits inside the same rounded IQOla play surface.
- Special boards such as mazes, tubes, cards, and moving targets keep their
  own geometry but use the same front-facing coordinate system.

## Shared screen frame

Every game uses the same order:

1. IQOla top bar with Back, title, level, and score.
2. One short instruction line.
3. Main game board in the shared rounded surface.
4. Consistent action controls and touch feedback.
5. Result state with score, best score, replay, and home actions.
6. Reserved bottom banner area for the ad; it never overlaps active gameplay.

Timed games do not display an interactive ad over the round. The banner
position remains stable so the paid remove-ads option has a predictable,
app-wide effect.

## Shared design tokens

- Pale neutral background with deep navy text.
- IQOla blue as the primary action color.
- A small supporting palette for success, warning, error, and emphasis.
- One typography family, consistent weights, and readable large labels.
- Consistent rounded corners, spacing, borders, shadows, and minimum touch
  targets.
- One feedback language: correct, incorrect, level complete, and retry states
  use the same colors, motion, and haptic pattern.

## Graphic policy

Graphics are generated or selected only when the mechanic truly needs them.
The first catalog should rely mainly on shapes, cards, symbols, and simple
sprites. This keeps visual quality consistent, reduces asset cost, and makes
new games faster to add.

## Acceptance test

Before a game is accepted, it must be possible to place its screenshot beside
another IQOla game and recognize the same app immediately from the frame,
perspective, typography, controls, feedback, and ad placement.
