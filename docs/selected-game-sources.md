# IQOla selected game sources

This list records the duplicate-filtering decision for the public IQOla
repository. One representative is retained from each mechanic family.

## Puzzle representatives

These representatives are taken from the sidhant947/Puzzle project:

| Mechanic family | IQOla representative | Puzzle source ID |
| --- | --- | --- |
| Stroop / colour interference | Reverse Stroop | reverse_stroop |
| Spatial working memory | Memory Matrix | memory_matrix |
| Moving-object tracking | Object Track | multiple_object_tracking |
| Inhibitory control | Stop Signal | stop_signal |
| Anagram solving | Anagram Definition | anagram_definition |
| Colour sorting | Water Sort | water_sort |
| Choice speed | Choice Reaction Time | choice_reaction_time |
| Maze navigation | Rotating Maze | rotating_maze |

Puzzle is licensed under GPL-3.0. Any incorporated Puzzle source must retain
its copyright and license notices, and IQOla's corresponding source must
remain available under the applicable GPL terms.

## Retained additions from other verified sources

These are the mechanics with no retained Puzzle equivalent after the
cross-source duplicate pass. Their presentation will be redrawn in IQOla's
shared visual system.

| Source | Retained mechanic | Why it stays |
| --- | --- | --- |
| Braincup | Mini Chess | Short checkmate scenarios; one representative for the chess family |
| Braincup | Tower of Hanoi | Constrained stack-transfer planning |
| Braincup | Colored Shapes | Deduce shape/colour point values and total them |
| Braincup | Bubble Sum | Add briefly shown floating values from memory |
| Braincup | Fraction Calculation | Solve a fraction multiplication expression |
| Braincup | Prism Clear | No-refill match-three board-clearing puzzle |
| Braincup | Mental Flex | Infer the relevant visual attribute, then match it |
| Braincup | Trio | Set-style visual-trait selection |
| Braincup | Matchstick Riddles | Move sticks to repair an equation |
| Braincup | Peg Solitaire | Jump pegs until one remains |
| Braincup | Reversi | Turn-based capture strategy |
| MindForge | False Light | Sweep tiles outside one changing light source |
| BrainiumX | Color Dominance | Find the most frequent colour in a dense grid |
| Brain Speed Exercises | Directional Processing | Judge the direction of a moving Gabor pattern |
| Brain Speed Exercises | Sound Sweep | Identify the order of two rising/falling audio sweeps |
| Brain Speed Exercises | Card Rat | Conditional reaction game for pairs, sandwiches, and jokers |

The full per-game mapping, including all games deliberately folded away, is
in `docs/game-type-dedup.md`.

## Source-license notes

| Source | License | Use in IQOla |
| --- | --- | --- |
| Puzzle | GPL-3.0 | Corresponding source and notices must remain available under GPL terms. |
| Braincup | Apache-2.0 | Preserve notices when source is incorporated. |
| MindForge | Apache-2.0 | Preserve notices when source is incorporated. |
| BrainiumX | MIT | Preserve the MIT notice when source is incorporated. |
| Brain Speed Exercises | MIT | Preserve the MIT notice when source is incorporated. |

## Scope note

This document records the selection only. The detailed mechanic-level filter
is in `docs/game-type-dedup.md`. The current Android MVP still uses its
original Java implementations; porting the selected games is a separate
implementation step.
