# Original Orbit icon — implemented

Selected concept: original 04-orbit-preview.png, not a round-two variant.

## Resources

- Manifest icon and roundIcon both point to @mipmap/ic_launcher.
- Adaptive icon: cobalt #345DD2 background and clean vector foreground.
- Monochrome: same vector silhouette, recolored by compatible launchers on Android 13+.
- Minimum Android version remains API 26; no pre-26 raster fallback is needed.
- Foreground mark is about 42dp across in a 108dp adaptive-icon viewport, inside the 66dp safe circle. Circle mask verified in Pixel Launcher; other OEM masks were not runtime-tested.
- App code, application version and user data were not changed by this icon work.

## Verification

- Fresh :app:clean :app:assembleDebug :app:lintDebug: BUILD SUCCESSFUL.
- Lint: 0 errors, 13 warnings (see lint.txt).
- git diff --check: passed.
- aapt2 confirms packaged application icon and background/foreground/monochrome XML layers.
- Installed final APK over existing app on the task-owned API 35 emulator.
- Actual Pixel Launcher screenshots: normal, Violet preset light, Violet preset dark.
- Launcher recolors Takt along with system apps. The outer ring around the icon in the dock is Pixel Launcher's predicted-app indicator, not part of the Takt artwork.
- Tapping the icon launches com.kpyruy.takt/.app.MainActivity; process running; crash log empty.
- An intermediate build following resource-folder relocation failed to link the icon due to stale incremental resource state. A clean app build resolved it; all runtime captures here use the final successful build.

APK: /home/kpyr/Projects/Takt/app/build/outputs/apk/debug/app-debug.apk
SHA-256: ef07c3bbfac36bc71cc4b59da97a0806eec9bc25f07f4198bb4c1b1826d6a0da

## Screenshots

- [Normal](launcher-normal.png)
- [Violet light](launcher-purple.png)
- [Violet dark](launcher-purple-dark.png)
- [Comparison of actual screenshot crops](launcher-comparison.png)
