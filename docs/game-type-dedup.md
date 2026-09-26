# IQOla game-type dedup registry

## Scope

`sidhant947/Puzzle` is the base source pool. It has **304 actual game folders**
with a screen, metadata, and registry entry for each folder. Its README's six
category totals add up to 303, so the source registry is the authoritative
count.

This registry filters the **user-facing IQOla catalogue**. It does not delete
upstream GPL source. Keeping the source intact preserves a reversible audit
trail and makes the required public-source notices straightforward.

## What counts as a duplicate

Two games are treated as one family when their main player action and win rule
are the same, even if the labels, colours, or assets differ. Games that merely
train the same skill but use a different loop remain separate.

The retained representative is normally the deeper or more visually engaging
version.

## First-pass result

| Source screens | Repeat screens hidden from IQOla menu | Retained Puzzle mechanics |
| ---: | ---: | ---: |
| 304 | 59 | 245 |

## Confirmed mechanic families

| Family | Keep in IQOla | Hide as repeat |
| --- | --- | --- |
| Stroop interference | `reverse_stroop` | `stroop_test`, `stroop_number_size` |
| Attentional blink | `attentional_blink_probe` | `attentional_blink` |
| Conjunction visual search | `visual_search_conjunction` | `conjunction_search`, `visual_search` |
| Simple reaction | `choice_reaction_time` | `reflex_tap` |
| Response inhibition | `stop_signal` | `go_no_go` |
| Flanker conflict | `symbolic_flanker` | `flanker_test` |
| Rule switching | `wisconsin_card_sorting` | `ab_reversal_oddball`, `rule_switcher`, `switch_task`, `color_word_match_up` |
| Moving-object tracking | `multiple_object_tracking` | `spotlight_track` |
| Cup shuffle tracking | `object_shuffle` | `shell_game` |
| N-back | `double_n_back` | `n_back` |
| Spatial sequence recall | `corsi_backward_span` | `corsi_blocks`, `pattern_sequence_draw`, `path_recall`, `simon_sequence`, `staircase_memory` |
| Working-memory span | `operation_span` | `digit_span_reverse`, `probe_digit_span`, `running_memory_span`, `counting_span`, `reading_span` |
| Associative pairs | `face_name_association` | `associative_pairs`, `continuous_paired_associate`, `word_pair_associate_memory`, `face_trait_association`, `word_association_recall` |
| Old/new recognition | `delayed_match_sample` | `continuous_recognition`, `dnms`, `sternberg_task` |
| Anagram solving | `anagram_definition` | `conundrum_anagram`, `word_scramble` |
| Three-word link | `semantic_link` | `semantic_association` |
| Word ladder | `word_ladder` | `one_letter_shift`, `word_ladder_step` |
| Vowel completion | `vowel_reconstruct` | `missing_vowels` |
| Equation sprint | `calculation_sprint` | `quick_math` |
| Target-number arithmetic | `target_number` | `target_10`, `countdown_math` |
| Equivalent-fraction match | `fraction_matcher` | `fraction_match` |
| Inequality sprint | `inequality_dash` | `algebraic_inequality_solver` |
| Clock modulo | `modulo_clock` | `modular_clock_arithmetic` |
| Matrix multiplication | `matrix_multiplier_match` | `matrix_multiplier` |
| Venn diagram | `set_theory_venn` | `venn_numbers` |
| Swipe-merge tiles | `game_2048` | `fibonacci_merge` |
| Sudoku | `sudoku` | `alphabet_sudoku` |
| Sliding-block puzzle | `klotski` | `block_escape`, `slide_puzzle` |
| 3D net folding | `complex_folding_nets` | `cube_net_fold` |
| 3D rotation | `wireframe_3d_rotation` | `mental_rotation`, `odd_rotation` |
| Gear direction | `gear_train_direction` | `gear_rotation` |
| Perspective rotation | `perspective_shift_view` | `perspective_taking` |
| Silhouette matching | `silhouette_match_ortho` | `silhouette_match` |
| Maze/path finding | `rotating_maze` | `classic_maze`, `path_finder` |

## Cross-source pass

The remaining verified repositories were compared against the retained Puzzle
catalogue by the same player-loop rule. A repository name, graphics, or
cognitive-domain label does not make a new game type by itself.

| Verified source | Audited entries | Folded into an existing family or excluded | Retained mechanics |
| --- | ---: | ---: | ---: |
| Puzzle | 304 | 59 | 245 |
| Braincup | 46 | 35 | 11 |
| MindForge | 4 | 3 | 1 |
| BrainiumX | 12 | 11 | 1 |
| Brain Speed Exercises | 9 | 6 | 3 |
| **IQOla catalogue target** | — | — | **261** |

The total is a **catalogue target**, not a request to implement 261 games at
once. It is the set of distinct mechanics available after the filter.

### Braincup mapping

Braincup has 40 menu games and six separately routed extras. Its duplicate
families resolve as follows:

| Braincup game(s) | IQOla result |
| --- | --- |
| Mini Sudoku, full Sudoku | `sudoku` |
| Lights Out | `lights_out` |
| Sliding Puzzle | `klotski` |
| Shikaku, Nurikabe | Same-named Puzzle games |
| Cat Queens | `crown` |
| Knot | `pipes` |
| Path Finder | `rotating_maze` |
| Anomaly Puzzle | `odd_one_out` |
| Ghost Grid, Simon Says | `corsi_backward_span` |
| Visual Memory | `memory_matrix` |
| Pattern Sequence | `matrix_reasoning` |
| Sherlock Calculation | `target_number` |
| Mental Calculation, Chain Calculation | `arithmetic_chain` |
| Quick Sum | Braincup's retained **Bubble Sum** |
| Value Comparison | `inequality_dash` |
| Color Confusion | `reverse_stroop` |
| Orbit Tracker | `multiple_object_tracking` |
| Flash Crowd | `subitizing_rush` |
| Schulte Table | `schulte_table` |
| Digit Memory | `operation_span` |
| Spot the New | `delayed_match_sample` |
| N-Back | `double_n_back` |
| Wordle | `find_word` |
| Missing Operators | `missing_operator` |
| Bulls & Cows | `word_mastermind` |
| Rule Shift | `wisconsin_card_sorting` |
| Mental Rotations | `wireframe_3d_rotation` |
| IQ Test | `matrix_reasoning`; do not present the result as a clinical IQ score |

The retained Braincup additions are: **Mini Chess**, **Tower of Hanoi**,
**Colored Shapes**, **Bubble Sum**, **Fraction Calculation**, **Prism Clear**,
**Mental Flex**, **Trio**, **Matchstick Riddles**, **Peg Solitaire**, and
**Reversi**. Mini Chess is the one chess-family representative; Solo Chess
and full Chess are intentionally omitted. Flags is trivia rather than an IQ
mini-game.

### MindForge mapping

| MindForge game | IQOla result |
| --- | --- |
| Stroop Rush | `reverse_stroop` |
| Schulte Grid | `schulte_table` |
| Digit Bridge | `symbol_digit_assoc` |
| False Light | **Retain:** a distinct light-coverage sweep mechanic |

### BrainiumX mapping

| BrainiumX game(s) | IQOla result |
| --- | --- |
| Speed Tap | `choice_reaction_time` |
| Go/No-Go | `stop_signal` |
| Focus Shift | `wisconsin_card_sorting` |
| Memory Grid | `memory_matrix` |
| Pattern Sequence, Color Match | `corsi_backward_span` — Color Match is a Simon-style sequence, not a colour-matching game |
| Arithmetic Sprint | `calculation_sprint` |
| Trail Connect | `trail_making` |
| Spatial Rotation | `wireframe_3d_rotation` |
| Stroop Match | `reverse_stroop` |
| Word Chain | `category_fluency` |
| Color Dominance | **Retain:** identify the most frequent colour in a populated grid |

### Brain Speed Exercises mapping

| Brain Speed game(s) | IQOla result |
| --- | --- |
| Fast Piggie | `odd_one_out` |
| Field of View | `divided_attention` |
| High Speed Memory | `memory_matrix` |
| Object Track | `multiple_object_tracking` |
| Orbit Sprite Memory | `corsi_backward_span` |
| Otter Stop | `stop_signal` |
| Directional Processing | **Retain:** brief moving-Gabor direction judgement |
| Sound Sweep | **Retain:** identify a two-sweep auditory direction sequence |
| Card Rat | **Retain:** Egyptian-Rat-Screw-style conditional card slap |

### Sources not eligible for direct inclusion

- **Brain Development Games:** its README says MIT, but its repository root
  contains no license file. Treat its 21 games as design reference only until
  the author supplies an unambiguous license.
- **Cognitive Training App:** the checked archive lacks the advertised game
  source, so it is not a verified source pool.

## Implementation boundary

This is a catalogue decision only. No game code, graphics, or screens were
copied into IQOla during the audit. A later build should use one IQOla visual
system for every retained game rather than import any upstream interface.
