# Reel Block

Block Instagram Reels on Android. Keep the rest of Instagram.

Reel Block is a small accessibility service (about 30 lines of Java). When Instagram shows the
full-screen Reels viewer, Reel Block presses Back. Instagram then goes back to the previous screen.

- Home feed, Stories, DMs and profiles work as before.
- No root. No modified Instagram APK. Instagram still updates from the Play Store.
- No network access, no data collection. The app has no permissions except the accessibility binding.

## Install

1. Download `ReelBlock.apk` from [Releases](../../releases) and install it.
   Or build it from source (see below).
2. Open **Settings → Accessibility → Downloaded apps (or Installed services) → Reel Block**.
3. Turn it on.

If the switch is greyed out, open **Settings → Apps → Reel Block → ⋮ → Allow restricted settings**,
then try again. Android shows this lock for apps that are not from an app store.

**Xiaomi / Redmi / POCO (HyperOS, MIUI):**
- To install over USB, turn on **Developer options → Install via USB**.
- If Reels comes back after some time, the system stopped the service. Turn on **Autostart** for
  Reel Block and set its battery saver to **No restrictions**.

To stop blocking Reels, turn off the service or uninstall the app.

## How it works

The view IDs come from the Instagram APK (version 453.0.0.57.79):

- Inside Instagram, Reels are called **clips**. Names that start with `reel_` are **Stories**.
- `aapt2 dump resources` on the APK lists `id/clips_tab` (the Reels button in the tab bar) and
  `id/clips_viewer_view_pager` (the full-screen Reels feed).
- With the Reels tab open, `adb shell dumpsys activity top` shows
  `ViewPager2 ... app:id/clips_viewer_view_pager` as visible. This confirms the ID on the live app.

The service listens only to `com.instagram.android`. On each window event it looks for a visible
`clips_viewer_view_pager`. If it finds one, it calls `performGlobalAction(GLOBAL_ACTION_BACK)` and logs
`Reels viewer visible, pressing Back` with the tag `ReelBlock`. This blocks Reels from the Reels tab (tested).
Reels that open from DMs, Explore or a profile use the same viewer, so they should close too
(not tested yet).

Short video posts that play inside the home feed are not blocked.

## If Instagram changes the ID

Open Reels in Instagram (with Reel Block turned off) and run:

```sh
adb shell dumpsys activity top | grep clips_viewer
```

Put the new ID in `REELS_VIEWER` in
[`ReelBlockService.java`](src/dev/local/reelblock/ReelBlockService.java), then build again.

## Build from source

You need a JDK (11 or later) and the Android SDK with one `build-tools` and one `platforms`
package. No Gradle and no Android Studio.

```sh
./build.sh            # builds ReelBlock.apk and installs it with adb
./build.sh <serial>   # pick a device when more than one is connected
```

The script uses `$ANDROID_HOME` (default `~/Library/Android/sdk`) and picks the newest build-tools
and platform. Set `BUILD_TOOLS=/path/to/build-tools/<version>` or
`ANDROID_JAR=/path/to/android.jar` to choose others. On the first build it creates a local
`debug.keystore` to sign the APK. Git ignores this file.

To check that it works:

```sh
adb logcat -s ReelBlock:I
```

Then open the Reels tab. You see one log line, and Instagram goes back to the previous screen.

## Disclaimer

This project is not affiliated with Instagram or Meta. Instagram is a trademark of Meta Platforms, Inc.
Reel Block reads only the view IDs on the screen. It does not read your messages or send data anywhere.

## License

[MIT](LICENSE)
