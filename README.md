<p align="center">
  <img src="docs/icon.svg" width="96" alt="Reel Block icon: a play button with a red slash">
</p>

<h1 align="center">Reel Block</h1>

<p align="center"><b>Keep Instagram. Lose the Reels.</b></p>

<p align="center">
  Reel Block is a free, open-source Android app that closes Instagram Reels
  the moment they open, and keeps everything else in Instagram.
</p>

<p align="center">
  <a href="../../releases/latest"><b>⬇ Download the APK</b></a> ·
  <a href="#install-in-2-minutes">Install</a> ·
  <a href="#faq">FAQ</a>
</p>

https://github.com/user-attachments/assets/9e368622-f6dc-417e-a583-c22226c2a839

---

## Why

You open Instagram to answer one message. Forty minutes later you are still swiping Reels.
That is not a lack of willpower. The feed is built to never end.

Most blockers lock the whole app. Then you lose your DMs, and your friends' Stories, so you
turn the blocker off. Reel Block removes only the endless Reels feed. Everything else stays.

| | Reel Block |
|---|---|
| Instagram Reels tab | **Closed** |
| Reels opened from DMs, Explore or a profile | **Closed** (same viewer, not tested yet) |
| Instagram feed, Stories, DMs, profiles, posting | Works |
| Other apps | **Never touched**. Reel Block listens only to Instagram |
| Root, modified APKs, accounts, ads, tracking, logs | **None** |
| Size | About 20 KB |

## How it feels

You tap the Reels tab. You are back on your feed before the first Reel plays.
After a few days you stop reaching for it, because nothing is there.

Open the app to see how many Reels it closed for you, and share the number.

## Install in 2 minutes

1. **Download** `ReelBlock.apk` from the [latest release](../../releases/latest) on your phone and open it.
   If Android asks, allow your browser or file manager to install apps.
2. **Open Reel Block** from your app list and tap **Turn on Reel Block**.
3. In the Accessibility list, tap **Reel Block** (it can be under *Downloaded apps* or
   *Installed services*) and turn it on.

That is all. Open Instagram and tap the Reels tab to test it.

**The switch is grey or says "Restricted setting"?** Android 13 and later lock this setting for
apps that are not from an app store. In Reel Block, tap **Open app info**, then **⋮ → Allow
restricted settings**, then go back to step 3.

**Xiaomi, Redmi, POCO (HyperOS, MIUI), Samsung, OnePlus, Oppo, Vivo:** if Reels come back after
some hours, the phone stopped the service to save battery. In **Settings → Apps → Reel Block**,
turn on **Autostart** (if you see it) and set battery use to **No restrictions / Unrestricted**.
On Xiaomi phones, swiping Reel Block away in Recents also stops it ("Not working" in Accessibility
settings). Since v1.2.1 the app leaves no Recents card when you leave it. With **Autostart** on, the
phone starts it again in about 1 second, even after a swipe. On Xiaomi phones, the app shows an
**Open Autostart** button for this.

**To pause:** open Reel Block and turn off the "Block Reels" switch. Choose **15 minutes**, **1 hour** or **Until I turn it on**. A timed pause ends by itself, so you cannot forget to turn blocking on again.

**To stop:** turn off the service in Accessibility settings, or uninstall the app.

## FAQ

**Is it safe? Accessibility apps can read my screen.**
Yes, Android gives accessibility apps a lot of access. That is why Reel Block is open source and
very small: [the service is about 40 lines](src/dev/local/reelblock/ReelBlockService.java) and the
[app screen is about 130](src/dev/local/reelblock/MainActivity.java). Read them. The app:
- listens only to Instagram (see [`accessibility.xml`](res/xml/accessibility.xml)), and the code
  checks the package again before it looks at any window,
- looks for one view ID and reads no text, messages or images,
- has **no internet permission** (see [`AndroidManifest.xml`](AndroidManifest.xml)), so it cannot send
  anything anywhere,
- writes no logs,
- stores one number on your phone: how many times it closed a viewer. Backup and phone-to-phone
  transfer are off, so the number stays on this phone.

**Why is it not on the Play Store?**
Google allows accessibility apps on the Play Store only for some uses. You can build the APK
yourself from this source (see below) and compare.

**Does it work on iPhone?**
No. iOS does not let apps act on other apps. On iPhone, the closest option is a Screen Time limit
on the whole app.

**What about YouTube Shorts, Facebook Reels, TikTok?**
Reel Block is Instagram only, on purpose: the fewer apps an accessibility service can see, the
less it can leak. Use a separate app for the others.

**Short videos still play inside my Instagram feed.**
Yes. Reel Block closes the full-screen viewer, which is the endless part. A video in the feed
stops when you scroll past it.

**Reels came back after an Instagram update.**
Instagram maybe changed its view ID. [Open an issue](../../issues/new?template=id-changed.md)
with your app version. Fixes are one line, see [CONTRIBUTING.md](CONTRIBUTING.md).

## How it works

Reel Block is an Android accessibility service. When Instagram changes its screen, the
service looks for the visible full-screen Reels viewer by its view ID. If it finds one, it
presses Back.

| App | Viewer ID | How we found it |
|---|---|---|
| Instagram 453.0.0.57.79 | `clips_viewer_view_pager` | `aapt2 dump resources` on the APK, then `dumpsys activity top` with Reels open. Inside Instagram, Reels are called *clips*; `reel_*` IDs are Stories. |

After each Back press the service waits 1.5 seconds, so one Back does not become two while the app
closes. It writes no log lines. To check it, watch the counter in the app go up.

## Build from source

You need a JDK (11 or later) and the Android SDK with one `build-tools` and one `platforms`
package. No Gradle and no Android Studio. The build takes a few seconds.

```sh
./build.sh                # build ReelBlock.apk and install it with adb
./build.sh <serial>       # pick a device when more than one is connected
NO_INSTALL=1 ./build.sh   # build only
```

The script uses `$ANDROID_HOME` (default `~/Library/Android/sdk`) and picks the newest build-tools
and platform. Set `BUILD_TOOLS=/path/to/build-tools/<version>` or `ANDROID_JAR=/path/to/android.jar`
to choose others. On the first build it creates a local `debug.keystore` to sign the APK. Git ignores
this file. GitHub Actions builds every push, see [`.github/workflows/build.yml`](.github/workflows/build.yml).

Merging does not make a release. To release, bump `versionCode` and `versionName` in
`AndroidManifest.xml`, merge to `main`, then run `./release.sh notes.md` from `main`. It tags the version,
checks the APK has the release key and no permissions, and uploads it to GitHub Releases.

## Spread it

If Reel Block gave you some time back, send it to one friend who needs it.
The **Share** button in the app writes the message for you. A ⭐ on GitHub also helps other people
find it.

## Contributing

Fixed IDs, translations and test reports on more phones are all welcome.
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Disclaimer

This project is not affiliated with Instagram or Meta. Instagram is a trademark of
Meta Platforms, Inc.

## License

[MIT](LICENSE)
