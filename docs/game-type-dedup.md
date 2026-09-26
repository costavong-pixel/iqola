# IQOla game-type dedup registry

## Scope

`sidhant947/Puzzle` is the source pool. It has **304 actual game folders**
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

## Next dedupe pass

The same registry will map the other verified sources onto these families.
Only a mechanic with no Puzzle representative will be added to IQOla. The
known Brain Speed candidates remain in review as separate candidates:

- Directional Processing
- Sound Sweep
- Card Rat

They are not counted until their mechanics are compared against the retained
Puzzle catalogue.
