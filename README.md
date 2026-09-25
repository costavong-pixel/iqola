# IQOla — Android MVP

This repository is distributed under the GNU General Public License v3.0
(GPL-3.0). See LICENSE.

IQOla is a native Android brain-training app with twelve short, original mini-games:

- Pattern Memory
- Quick Math
- Digit Recall
- Arrow Path
- Connect Lines
- Word Match
- Color Focus
- Reaction Tap
- Number Order
- Odd One Out
- Sequence Logic
- Shape Rotate

The home screen is swipeable so the larger catalog stays easy to browse. The interface is drawn in code so the MVP has an original, consistent visual style without copied game artwork or third-party image assets. Best scores are saved locally on the device.

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

The current Android MVP commit still contains the original 12-game Java
implementation. The selected Puzzle games will be ported into the Android
app in a later change; this keeps the first public repository state
buildable and easy to review.

## Working identity

`IQOla` is a working project title only. Confirm the final app-store name and trademark availability before release.
