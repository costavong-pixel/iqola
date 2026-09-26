# IQOla — premium Water Sort prototype

This repository is distributed under the GNU General Public License v3.0
(GPL-3.0). See LICENSE.

IQOla is a native Android brain-training app. Version 0.3.0 is the visual-standard prototype for the catalog: a playable Water Sort puzzle in a shared premium game frame.

It includes:

- A solvable, generated Water Sort level with six glass vessels
- Tap-to-select play, valid colour pours, undo, restart, and completion flow
- Liquid gradients, vessel highlights, a pour animation, and a full-screen crystal chamber
- A deliberate shared visual direction for future IQOla games rather than separate flat game screens
- Local best-level progress

The Water Sort rule implementation and game presentation are original IQOla code. The chamber artwork is an original project asset. The earlier four-game core pack remains preserved in Git history while this release establishes the new product direction.

## Advertising and ad-free purchase

This build includes a clearly labelled **ad-space reservation** at the bottom of the game screen. It deliberately does not request a real advert yet.

When the AdMob account is verified, replace the `drawAdSlot` implementation in `MainActivity.java` with the Google Mobile Ads banner view and use Google test ad IDs while developing. The “Remove ads” action is intentionally a preview message until Google Play Billing is connected for a real one-time non-consumable purchase.

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
