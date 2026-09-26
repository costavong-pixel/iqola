# IQOla — Android core pack

This repository is distributed under the GNU General Public License v3.0
(GPL-3.0). See LICENSE.

IQOla is a native Android brain-training app. The first polished core pack has four games:

- Reverse Stroop
- Memory Matrix
- Stop Signal
- Object Track

Each game has eight short rounds, four levels, shared score and best-score handling, a common result screen, and the same fixed banner-ad area. The interface is drawn in code so every game uses the same original visual frame without copied game artwork or third-party image assets. Best scores are saved locally on the device.

## Advertising and ad-free purchase

This build includes a clearly labelled **test-ad space** at the bottom of game screens. It deliberately does not request a real advert yet.

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

The core-pack games were implemented in IQOla's shared Java canvas frame.
Puzzle remains the documented source for the selected mechanic families; this
build uses original IQOla presentation and implementation rather than copying
upstream screens or artwork.

## Working identity

`IQOla` is a working project title only. Confirm the final app-store name and trademark availability before release.
