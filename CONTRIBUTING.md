# Contributing

Thank you for helping people get their time back. Reel Block stays small on purpose: anyone must be
able to read all of it in five minutes. Please keep changes small too.

## Fix an ID or add an app (most useful)

Apps rename their views after updates. A fix is usually one line.

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
5. Add the ID to `VIEWERS` in [`ReelBlockService.java`](src/dev/local/reelblock/ReelBlockService.java)
   as `"<package>:id/<id>"`, with a comment that names the app and version. For a new app, also add
   the package to `android:packageNames` in [`res/xml/accessibility.xml`](res/xml/accessibility.xml).
6. Build, turn Reel Block on, and check:
   ```sh
   ./build.sh
   adb logcat -s ReelBlock:I
   ```
   Open the viewer: you see one `pressing Back` line and the app goes back. Open the normal screens
   and normal videos: you see no line.

In the pull request, write the app version, your phone and Android version, and what you tested.

## Other good first contributions

- A switch in the app to choose which apps to block.
- Translations of the text in [`MainActivity.java`](src/dev/local/reelblock/MainActivity.java).
- Test reports on more phones (open an issue with phone, Android version, and what worked).

## Rules

- No internet permission, no analytics, no ads. Ever.
- No new dependencies and no Gradle. The build must stay `./build.sh`.
- Do not read text, messages or images on the screen. View IDs only.
