# IQOla — premium Water Sort prototype

This repository is distributed under the GNU General Public License v3.0
(GPL-3.0). See LICENSE.

IQOla is a native Android brain-training app. Version 0.6.0 is the visual-standard prototype for the catalog: a playable Water Sort puzzle in a shared premium game frame.

It includes:

- A solvable, generated Water Sort level with six glass vessels
- A bold, step-by-step guided opening where the player completes two real moves, with Hint and Skip Demo controls
- A large two-minute countdown that stays paused until the guide is completed or skipped
- Tap-to-select play, valid symbol-matched pours, undo, restart, timeout, and completion flow
- A fixed triangle, diamond, circle, or plus mark on every liquid layer, so play does not depend on colour alone
- Liquid gradients, vessel highlights, a pour animation, and three slowly animated crystal-chamber moods
- A deliberate shared visual direction for future IQOla games rather than separate flat game screens
- Local best-level progress

The Water Sort rule implementation and game presentation are original IQOla code. The chamber artwork is an original project asset. The earlier four-game core pack remains preserved in Git history while this release establishes the new product direction.

## Advertising and IQOla Plus plan

This build includes a clearly labelled **ad-space reservation** at the bottom of the game screen. It deliberately does not request a real advert or billing yet.

The planned launch model is free with ads plus IQOla Plus: an annual ad-free plan that can include ongoing games, levels, and progress features. Billing is intentionally not part of version 0.6.0.

## Open in Android Studio

1. Open this folder in Android Studio.
2. Install Android SDK 36 if Android Studio requests it.
3. Use JDK 17.
4. Sync Gradle and run on an Android 7.0+ device or emulator.

The debug APK, once built, is at:

app/build/outputs/apk/debug/app-debug.apk

## Selected open-source game sources

The planned expanded catalog uses selected game implementations from the
GPL-3.0 Puzzle project. The duplicate-filtering decision is recorded in
docs/selected-game-sources.md. IQOla will retain the upstream copyright and
license notices for any Puzzle code that is incorporated.

Puzzle remains the documented source for the selected mechanic families. This
build uses original IQOla presentation and implementation rather than copying
upstream screens or artwork.

## Working identity

`IQOla` is a working project title only. Confirm the final app-store name and trademark availability before release.
