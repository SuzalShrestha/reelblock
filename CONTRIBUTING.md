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
5. Change `VIEWER` in [`ReelBlockService.java`](src/dev/local/reelblock/ReelBlockService.java)
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

## Rules

- No internet permission, no analytics, no ads. Ever.
- No new dependencies and no Gradle. The build must stay `./build.sh`.
- Do not read text, messages or images on the screen. View IDs only.
- Instagram only. No other packages in `accessibility.xml`.
- No `android.util.Log` or any other logging.
- Colours, type and spacing come from `res/values/colors.xml` and `styles.xml`. Every text colour
  must reach 4.5:1 contrast in light and dark.
