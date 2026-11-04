# Contributing

Thank you for helping people get their time back. Reel Block stays small on purpose: anyone must be
able to read all of it in five minutes. Please keep changes small too.

## Fix the Instagram ID (most useful)

Reel Block is Instagram only, on purpose. Pull requests that add other apps will be closed.

Instagram renames its views after updates. A fix is usually one line.

1. Connect your phone with USB debugging on, and **turn Reel Block off**.
2. Open the full-screen short-video viewer in the app.
3. Run:
   ```sh
   adb shell dumpsys activity activities | grep topResumedActivity   # the package name
   adb shell dumpsys activity top | grep -E 'app:id/' | grep -iE 'reel|short|clip|pager|recycler'
   ```
   Find a view that fills the screen (bounds like `0,0-1080,2174`) and has `V` (visible) in its flags.
4. Go to the app's normal home screen and run the same command. The ID must **not** be visible
   there, or Reel Block would close the whole app.
5. Change `VIEWER_NAME` in [`ReelBlockService.java`](src/dev/local/reelblock/ReelBlockService.java)
   and update the comment with the Instagram version.
6. Build, turn Reel Block on, and check:
   ```sh
   ./build.sh
   ```
   Open Reels: Instagram goes back and the counter in Reel Block goes up by one. Open the feed,
   Stories and DMs: the counter does not change.

In the pull request, write the app version, your phone and Android version, and what you tested.

## Other good first contributions

- Translations of [`res/values/strings.xml`](res/values/strings.xml) (add `res/values-<lang>/strings.xml`).
- Test reports on more phones (open an issue with phone, Android version, and what worked).
  [`tools/device-check.sh`](tools/device-check.sh) runs the on-phone checks with `adb`. It has not been run
  on a phone yet (it was written without one), so a first report may find mistakes in the script as well as in
  the app. It cannot make the "Android stopped Reel Block" or "Instagram has changed" screens, so test those by
  hand if you can. `FIRST_RUN=1` clears the app's data.

## To update screenshots

The pictures in [`docs/screens/`](docs/screens) are rendered, not taken on a phone. The tool is
[`tools/screens/`](tools/screens). It is a dev tool: it is not part of the APK or `build.sh`. It needs Maven
and a JDK 17 or later. Maven fetches Robolectric.

```sh
NO_INSTALL=1 ./build.sh         # the tool uses the classes and resources from this build
tools/screens/run.sh ShotsR     # Android 11, fixed brand colours -> out/screens/ShotsR/
tools/screens/run.sh Shots      # Android 14, wallpaper colours   -> out/screens/Shots/
```

Each run writes `<scene>_<variant>.png` and `audit.txt` (every clickable view needs a label and at least 48dp).
The script stops with an error if the audit has a FAIL. Scenes are lines in `tools/screens/scenes.txt`. A
second argument picks the variants, for example `tools/screens/run.sh ShotsR light,dark,font2,rtl,small`.
Copy the files you need into `docs/screens/` (the names there differ, for example `home_on_light.png` is
`after-home-on-light.png`) and update `audit-android11.txt` and `audit-android14.txt`. Say in the pull request
that they are renders.

## Rules

- No internet permission, no analytics, no ads. Ever.
- No new dependencies and no Gradle. The build must stay `./build.sh`.
- Do not read text, messages or images on the screen. View IDs only.
- Instagram only. No other packages in `accessibility.xml`. CI fails if the file has more or fewer than one
  `packageNames` attribute, or if it is not exactly `com.instagram.android`.
- Do not add `android:settingsActivity` to `accessibility.xml`. Android Settings would then start Reel Block
  inside Settings' own task, and the HyperOS fix (the app closes its task when you leave it) would close
  Settings too.
- No `android.util.Log` or any other logging.
- Colours, type and spacing come from `res/values/colors.xml` and `styles.xml`. Every colour pair
  must pass `python3 tools/check_contrast.py`.
- Layouts and drawables use tokens only: `python3 tools/check_tokens.py`.
