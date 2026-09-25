# IQOla selected game sources

This list records the duplicate-filtering decision for the public IQOla
repository. One representative is retained from each mechanic family.

## Puzzle representatives

These representatives are taken from the sidhant947/Puzzle project:

| Mechanic family | IQOla representative | Puzzle source ID |
| --- | --- | --- |
| Stroop / colour interference | Reverse Stroop | reverse_stroop |
| Spatial working memory | Memory Matrix | memory_matrix |
| Moving-object tracking | Multiple Object Tracking | multiple_object_tracking |
| Inhibitory control | Stop Signal | stop_signal |
| Anagram solving | Anagram Definition | anagram_definition |
| Colour sorting | Water Sort | water_sort |
| Choice speed | Choice Reaction Time | choice_reaction_time |
| Maze navigation | Classic Maze | classic_maze |

Puzzle is licensed under GPL-3.0. Any incorporated Puzzle source must retain
its copyright and license notices, and IQOla's corresponding source must
remain available under the applicable GPL terms.

## Other retained unique games

The Brain Speed Exercises audit found three mechanics that do not duplicate
the other reviewed repositories:

- Directional Processing
- Sound Sweep
- Card Rat

## Scope note

This document records the selection only. The current Android MVP still uses
its original Java implementations; porting the selected games is a separate
implementation step.
