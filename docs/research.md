# Reel Block: Phase 1 research

Goal of the redesign: a person who finds Reel Block on GitHub trusts it enough to sideload the APK and turn on an accessibility service.

The core idea does not change:

- It closes the Instagram Reels viewer only.
- It works with Instagram only.
- It keeps no logs and has no INTERNET permission.
- It is tiny, free and open source, with no accounts, ads or tracking.

Any feature that breaks one of these is marked **rejected** below.

## 1. How this was researched

Date: 2026-11-04.

**What was read**

- This repo as it was before v2.0: the README, launch posts, the two Java files of v1.4, `accessibility.xml`, the manifest and `strings.xml`. Sections 3, 5 and 6 were then updated to match the code that was built (8 Java files, 952 lines after round 3).
- GitHub READMEs and issue trackers of about a dozen similar apps (AwayDoomscrollin, Odisej, Nudge, Curbox, Blockfy and others). These were fetched directly and are first-hand.
- `developer.android.com` pages on accessibility services, tiles, widgets, dynamic colour and developer verification.
- Material 3 token source files on GitHub (section 4).

**Repo state on the research date**

- The repo's GitHub issue tracker has **0 issues** (checked 2026-11-04). There is no user feedback to learn from yet. Every "user problem" below comes from other projects.
- `docs/launch-posts.md` was **stale**. It pitched "Instagram Reels and YouTube Shorts" and "about 120 lines", and the app has been Instagram only since v1.2. It has since been rewritten for Instagram only. Its line counts match the code (about 950 lines, the service 56).

**Limits**

- The sandbox blocked `m3.material.io`, `support.google.com`, `play.google.com`, `reddit.com` and the competitor websites (one sec, ScreenZen, Opal).
- The Google Play policy text and the Android Help restricted-settings article could not be opened. What this file says about them comes from **search summaries** of the official pages, not the pages. Those claims are marked "(search summary)". The exact wording must be checked before it is quoted anywhere.
- Play reviews, Play Data safety sections and Reddit threads were not read. Anything about one sec, ScreenZen, Opal and Google Digital Wellbeing is "(search snippet, unverified at source)". No Reddit thread is cited.
- Material 3 token values were not taken from `m3.material.io`. They come from the androidx Compose token sources and the material-components-android docs on GitHub. These mirror the M3 token export.

## 2. What builds and breaks trust

### Builders

| # | What | Evidence |
|---|---|---|
| 1 | **No INTERNET permission, enforced.** Odisej fails its build if a library adds one. This is the strongest claim Reel Block has. | [Odisej](https://github.com/paco208/odisej), [Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker), [Nudge](https://github.com/astraedus/nudge). AwayDoomscrollin has to explain its INTERNET use ([README](https://github.com/ResolveCommunity/AwayDoomscrollin)). |
| 2 | **Open source, permissive licence.** Curbox: "Anyone can verify how the app works." About 1.5k stars. | [Curbox](https://github.com/curbox-app/curbox-android) |
| 3 | **A plain explanation of the accessibility permission before Android's settings open.** | [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin); the spec in [block-instagram-reels #27](https://github.com/Moritz-Staat/block-instagram-reels/issues/27) |
| 4 | **Narrow scope, stated concretely.** FeedLock uses it "solely to detect which screen is active". | [FeedLock](https://github.com/caasion/FeedLock), [Odisej](https://github.com/paco208/odisej) |
| 5 | **Honest limits.** Nudge removed its Home/Explore claims after finding they did not work. Shorts-Blocker warns that app updates may break detection. | [Nudge #18](https://github.com/astraedus/nudge/issues/18), [Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker) |
| 6 | **Guided setup in the right order**: restricted settings, then accessibility, then battery. A reviewer said of ScreenZen: "the app walks you through each step clearly" (search snippet). | [Odisej](https://github.com/paco208/odisej), [WhistleOut](https://www.whistleout.com/CellPhones/Apps/screenzen-app-review) |
| 7 | **A clear list of what is stored, kept local.** Odisej keeps 7 days of totals. Nudge says "no analytics, no telemetry". | [Odisej](https://github.com/paco208/odisej), [Nudge](https://github.com/astraedus/nudge) |

### Breakers

| # | What | Evidence |
|---|---|---|
| 1 | **Silent failure.** The service stops and nothing tells the user. One Nudge issue: "silently stops all blocking with nothing shown to the user." On Xiaomi: "Settings still show Accessibility as "On," but the service is frozen." | [Nudge #57](https://github.com/astraedus/nudge/issues/57), [Nudge #116](https://github.com/astraedus/nudge/issues/116) (Samsung One UI 7), [RewardBox help](https://intercom.help/rewardbox-help-center/en/articles/16001568-why-does-the-app-say-accessibility-is-disabled-even-though-my-phone-shows-it-is-on) |
| 2 | **Instagram updates break detection.** Every open-source blocker warns of this. A dev write-up about YouTube Shorts claims "Instagram changes their View IDs every week" (found through search, not checked). | [Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker), [DEV Community](https://dev.to/vishal_pathak_209/i-reverse-engineered-youtube-to-delete-just-the-shorts-here-is-the-code-3b84) |
| 3 | **Paywalls.** one sec is free for one app, then paid. Opal is $99.99 a year or $399 for life (search summaries). | [AppBrain](https://www.appbrain.com/app/one-sec-%7C-app-blocker-focus/wtf.riedel.onesec), [autonomous.ai](https://www.autonomous.ai/ourblog/opal-app-review) |
| 4 | **Wrong triggers**: blocking mid-use or on the wrong screen. A false positive on a Reel shared in a DM is this class. | [Play, ScreenZen (snippet)](https://play.google.com/store/apps/details?id=com.screenzen&hl=en_US) |
| 5 | **Data-sharing signals.** ScreenZen's Data safety entries and Opal's VPN and dead privacy-link complaints (both unverified). Cloud sync weakens "offline" claims. | [Play, ScreenZen (search summary)](https://play.google.com/store/apps/details?id=com.screenzen&hl=en_US&gl=US), [Reels Blocker](https://github.com/sahedalomsumit/reels-blocker) |
| 6 | **Too easy to bypass.** "just pressing a button is too easy." Digital Wellbeing's one-tap "Ignore limit for today" gets the same criticism from a competitor blog (biased). | [Curbox #434](https://github.com/curbox-app/curbox-android/issues/434), [cursedscreen](https://cursedscreen-website.pages.dev/blog/digital-wellbeing-alternative/) |
| 7 | **Sideload friction that looks like malware.** "Android 13+ doesn't allow access to notifications and Accessibility APIs for sideloaded apps." | [F-Droid #2680](https://gitlab.com/fdroid/fdroidclient/-/issues/2680), [block-instagram-reels #37](https://github.com/Moritz-Staat/block-instagram-reels/issues/37) |
| 8 | **Closed code or "all rights reserved" with no support route.** | [dev.to (search summary)](https://dev.to/astraedus/block-youtube-shorts-and-reels-on-android-keep-the-app-1b9h) |

**What this meant for Reel Block.** The two strongest builders (1 and 2) were already true. The biggest gaps were builder 3 (no disclosure screen before settings) and breakers 1 and 2 (the app could not tell the user when it stopped working or when Instagram changed). v2.0 answers them with the disclosure screen and the self-check. One more point: "Users can always disable the service in Accessibility settings." ([Odisej README](https://github.com/paco208/odisej)). That is true for every such app, and we should say it plainly.

### Other apps people compare it with

These are broader tools: they mostly limit whole apps or add friction. The apps that target Reels are in the two tables above. These show what a user may expect before choosing a sideloaded APK.

| App | What it asks for | What it does | Notes |
|---|---|---|---|
| **Google Digital Wellbeing** (system app) | Nothing to install, no accessibility permission, no price | Focus Mode pauses chosen apps. It has 5, 15 and 30-minute breaks and schedules ([Android Central](https://androidcentral.com/digital-wellbeings-focus-mode-now-widely-rolling-out-all-users), [TechCrunch](https://techcrunch.com/2019/12/04/androids-focus-mode-exits-beta-adds-new-scheduling-features)). The reminder can be dismissed. | One tap on "Ignore limit for today" lifts a limit. A competitor blog calls this a major weakness ([cursedscreen](https://cursedscreen-website.pages.dev/blog/digital-wellbeing-alternative/), biased). It has no per-surface Reels blocking (unverified, from background knowledge). It is the trust baseline Reel Block has to beat: nothing to sideload and nothing to grant. (Search summaries.) |
| **one sec** | Accessibility, plus an overlay permission | A breathing pause before an app opens, then "continue". Free for one app, then paid. Since v2.3.7 it can also remove Reels, Shorts and Stories sections ([help](https://feedback.one-sec.app/help/articles/0145042-remove-in-app-distractions-reels-shorts-android)). | [Setup guide](https://tutorials.one-sec.app/android-accessibility-permission). Closed source. Prices and Data safety: search snippets, unverified. |
| **ScreenZen** | Four permissions: usage access, display over other apps, accessibility, ignore battery optimisation | A pause screen on open, unlock counts, schedules. Free. | [WhistleOut review](https://www.whistleout.com/CellPhones/Apps/screenzen-app-review) calls the guided setup clear. Its Data safety entries are a second-hand claim, unverified. |
| **Opal** | Accessibility on Android (third-party claim) | Sessions and "Deep Focus" that cannot be ended early (Pro). $99.99 a year or $399 for life. | [Opal help](https://help.opalapp.com/v3/introducing-opal-for-android) says little about permissions. The help pages are iOS-oriented. Search summaries. |

Reel Block is free and offline, and it removes one thing. That is a smaller promise than these apps make, and it is easier to check.

## 3. Play policy as a trust checklist

Reel Block is not on Play. Play's rules are still the best public description of what a careful accessibility app should show the user. Sources: [Play accessibility policy 10964491](https://support.google.com/googleplay/android-developer/answer/10964491) and [User Data policy 10144311](https://support.google.com/googleplay/android-developer/answer/10144311). **Both are (search summary): the pages were not readable.** Android's own pages were read directly.

In v1.4 the **Turn on Reel Block** button opened Accessibility settings straight away, and the privacy text sat lower on the same screen. In v2.0 the table's last column is what the code does.

| # | Checklist item | What Reel Block does |
|---|---|---|
| 1 | Show the disclosure in the app, before the settings handoff. It cannot live only in a README or privacy policy. (search summary) | A disclosure screen (page 2 of the first run), before any button leads to Settings. Page 3, "Turn on Reel Block", opens Settings. |
| 2 | Keep it a separate notice, not bundled with other terms. (search summary) | The page holds only this disclosure, two buttons (**Agree and continue**, **Not now**) and a **Read the code** link. |
| 3 | Say why the permission is needed, what is read, how it is used and whether it is shared. (search summary; exact wording of 10144311 unverified) | Three cards. **It can**: see when Instagram is open, see the name of the open Instagram screen, press Back when that screen is Reels, check which Instagram version is installed (to notice an update that breaks it). **It keeps, only on this phone**: the per-day counts for 30 days and the total; your pause setting, and that you agreed to this page. **It never**: reads messages, posts, photos or passwords, looks at any other app, or uses the internet (it has no internet permission). That describes the one fixed action, because the policy prohibits autonomous action ("strictly prohibited", search summary). |
| 4 | Consent is a tap. Dismissing or pressing Back is not consent. (search summary) | Two buttons: **Agree and continue** and **Not now**. Only a tap on **Agree and continue** counts. Back is not consent: it goes to the previous page. |
| 5 | Show the flow for consent declined as well as granted. (search summary) | **Not now** goes to the home screen in a calm "off" state. It is remembered, and the first run does not come back by itself. **Turn on Reel Block** on the home screen goes through the disclosure again if the user never agreed. |
| 6 | Do not open settings before consent, and do not change settings silently. [Android guide](https://developer.android.com/guide/topics/ui/accessibility/service) | Settings opens only after **Agree and continue**. The user flips the switch. |
| 7 | Do not set `isAccessibilityTool=true`. The default is false, and Android then shows a privacy notification after a while. [AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo) | `accessibility.xml` sets `android:isAccessibilityTool="false"` on purpose. The disclosure mentions the Android notice. |
| 8 | The `description` string must match the disclosure. It is not a substitute for the screen. (search summary) | One plain `service_description` string. It covers what it does and what it never does like the three cards. For what it keeps, it names the count of closed Reels and the pause setting, like the "It keeps" card. |
| 9 | Explain how to turn it off, and offer an in-app pause. Never nag. [Deceptive Behavior policy 9888379](https://support.google.com/googleplay/android-developer/answer/9888379) (URL and text unverified) | The README says how to stop it (Settings > Accessibility, or uninstall). The pause switch and the Quick Settings tile are in the app. The app never tries to stop the user from turning the service off. |
| 10 | Help with Android 13+ "Restricted setting". [Esper](https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis), [CleanBrowsing](https://cleanbrowsing.org/support/mobile/disable-restricted-settings-android) (secondary) | On Android 13+, the help is shown on page 3, under the steps. It is not detection-based: it shows on every Android 13+ phone, whatever the install source. The same page opens from **Setup help** on the home screen. Steps: App info, three dots, **Allow restricted settings**. The option may appear only after one blocked attempt ([droidwin](https://droidwin.com/allow-restricted-settings-missing-from-sideloaded-apps-in-android-13-fix/)). |
| 11 | README claims match the app, with source and signing-certificate hash so users can check the APK. | README and disclosure say the same things. The README gives the signing certificate's SHA-256 (the same value as `CERT` in `release.sh`) and the `apksigner` command to check it. The release notes list the APK's SHA-256. |
| 12 | Plan for developer verification. [Verification guide](https://developer.android.com/developer-verification/guides), [FAQ](https://developer.android.com/developer-verification/guides/faq) | A one-line README note, under "Verify the APK". |

**Developer verification note (2027).** Android is rolling out developer registration. Enforcement began 30 Sep 2026 in Brazil, Indonesia, Singapore and Thailand, for participating stores. The FAQ (updated 15 Jul 2026) says that for sideloaded apps "these new verification requirements won't apply to your app yet". A global rollout follows in 2027. Unregistered apps will then need an "advanced flow" (developer mode, a one-day wait, a biometric check). The maintainer should register the package name and signing key before 2027 ($25 per the FAQ). This is a note, not a promise.

**Play fit.** Whether a reel blocker is an allowed accessibility use on Play could not be verified. Android's guide says to build such a service only for "a general-purpose assistive tool" ([guide](https://developer.android.com/guide/topics/ui/accessibility/service)). That is why the app is sideloaded.

## 4. Material 3 / M3 Expressive notes we use

`docs/design-system.md` holds the full system. Below is the short version, with sources.

Sources, called **TOKENS** here: androidx Compose `material3/tokens/*.kt` in the [androidx repo](https://github.com/androidx/androidx/tree/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens). **MDC** is the [material-components-android docs](https://github.com/material-components/material-components-android/tree/master/docs). Quoting M3 Expressive: it is "an expansion of Material Design 3" that "complements the Android 16 visual style" ([Android docs](https://developer.android.com/develop/ui/compose/designsystems/material3)).

**Colour roles (tone, light / dark)** ([MDC Color](https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md), TOKENS `ColorLightTokens`, `ColorDarkTokens`)

- primary 40 / 80. on-primary 100 / 20. primary-container 90 / 30. on-primary-container 30 / 90 (the tones in `res/` and in `tools/check_contrast.py`).
- error 40 / 80. error-container 90 / 30.
- surface and background: neutral 98 / 6.
- Surface containers: lowest 100 / 4, low 96 / 10, container 94 / 12, high 92 / 17, highest 90 / 22.
- on-surface: neutral 10 / 90. on-surface-variant: neutral-variant 30 / 80.
- outline: neutral-variant 50 / 60. outline-variant: 80 / 30.

**Type scale** (size / line height in sp; TOKENS `TypeScaleTokens`, [MDC Typography](https://github.com/material-components/material-components-android/blob/master/docs/theming/Typography.md))

- Display 57/64, 45/52, 36/44. Headline 32/40, 28/36, 24/32.
- Title: L 22/28 (400), M 16/24 (500), S 14/20 (500).
- Body: L 16/24, M 14/20, S 12/16 (all 400). Label: L 14/20, M 12/16, S 11/16 (all 500).
- Emphasized styles keep the sizes and raise the weight (labels and Title M/S go to 700).

**Shape scale** (dp; TOKENS `ShapeTokens`, [MDC Shape](https://github.com/material-components/material-components-android/blob/master/docs/theming/Shape.md))

- none 0, extra-small 4, small 8, medium 12, large 16, **large-increased 20**, extra-large **28**, **extra-large-increased 32**, extra-extra-large 48, full.
- In the app, cards and dialogs are 28 dp, the status hero is 32 dp, and grouped list rows are 20 dp outside and 4 dp inside. Whether 20, 32 and 48 are officially "Expressive" tokens is unverified.

**Motion** (TOKENS `MotionTokens`, [MDC Motion](https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md))

- Durations: short 50, 100, 150, 200 ms; medium 250 to 400; long 450 to 600; extra-long 700 to 1000.
- **Emphasized-decelerate** cubic-bezier (0.05, 0.7, 0.1, 1) for things that enter.
- **Standard** cubic-bezier (0.2, 0, 0, 1) for things that move on screen.
- Expressive springs need AndroidX, which the app does not use. Platform `PathInterpolator` (API 21) covers the three curves the app uses: standard, emphasized (a path curve, for state changes that stay on screen) and emphasized-decelerate.

**Components**

- Switch: track **52 x 32 dp**, thumb 16 dp off, 24 dp on, 28 dp pressed (TOKENS `SwitchTokens`).
- Lists: one, two and three lines are **56 / 72 / 88 dp** (TOKENS `ListTokens`).
- Dialog: shape 28 dp, container surface-container-high, text headline-small and body-medium ([MDC Dialog](https://github.com/material-components/material-components-android/blob/master/docs/components/Dialog.md)).

**Dynamic colour** ([dynamic colour page](https://developer.android.com/develop/ui/views/theming/dynamic-colors), [R.color](https://developer.android.com/reference/android/R.color))

- API 31+: `android.R.color.system_accent1_*`, `accent2`, `accent3`, `neutral1`, `neutral2`, each with shades 0 to 1000.
- API 34+: role colours such as `system_primary_light` and `system_surface_container_high_dark`.
- minSdk is 26, so: API 34+ reads roles, API 31+ reads shades, older phones use fixed baseline hexes. The shade-to-tone mapping (accent1_600 = tone 40) is an AOSP convention and is not on the page (unverified).

## 5. Candidate features

"Cost" is a rough estimate of added code and APK size. For the four **BUILD** rows it is the real size of the file in lines (`wc -l`). Size in KB was not measured per feature. See "Cost in practice" in section 6. Verdicts are final for v2.0.

| Feature | User problem | Evidence | Cost | Risk to the core idea | Verdict |
|---|---|---|---|---|---|
| **Clear onboarding**: disclosure ("what it can do, keeps and never does") before Settings; Android 13+ "Restricted setting" help, shown on page 3 on every Android 13+ phone (not detection-based); Xiaomi Autostart help | People distrust accessibility prompts. The switch is greyed out on sideloaded apps. Xiaomi stops the service. | Breaker 7, builder 3 (section 2). [F-Droid #2680](https://gitlab.com/fdroid/fdroidclient/-/issues/2680), [spec #27](https://github.com/Moritz-Staat/block-instagram-reels/issues/27). Only README text ships in competitors, except [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin) (guides in EN and TR). | `Welcome.java`, 97 lines, plus a layout and strings | None. Strengthens trust. | **BUILD** |
| **Self-check**: (a) service health; (b) Instagram update check | (a) Android lists the service as on, but it is not running (Xiaomi "Not working", Samsung). (b) Instagram changes an ID and Reels silently come back. | (a) [Nudge #116](https://github.com/astraedus/nudge/issues/116), [Nudge #57](https://github.com/astraedus/nudge/issues/57), [Norton community](https://community.norton.com/t/accessibility-service-automatically-toggles-off-on-samsung-devices-s23-ultra-tab-s5e-device/494122). (b) Every open-source blocker warns of it ([Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker)). **No competitor ships (b).** Nudge covers (a) only. | `Check.java`, 72 lines, plus a `<queries>` entry in the manifest. The Instagram part runs on a background thread, once per Instagram install or update | None: no network, no user data read. Part (b) does not look at Instagram's screen. When Instagram's package cannot be read, it says "could not read" and does not guess "changed". | **BUILD** |
| **This-week chart**: on-device counts for the last 7 days, today, all time | The habit is invisible. A single total says little. | Local stats ship in [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin), [Blockfy](https://github.com/buenotty/Blockfy), [Odisej](https://github.com/paco208/odisej), [Nudge](https://github.com/astraedus/nudge). | `WeekChart.java`, 100 lines, plus counting in `State.java`. The service makes one small write per close (the all-time count and today's count together). On the first close of each day it also deletes per-day counts older than 30 days. | None. Stored only in app prefs, excluded from backup. | **BUILD** |
| **Quick Settings tile** | Pause and resume without opening the app. | Weak. No user requests found. Odisej has a state-only tile ([README](https://github.com/paco208/odisej)). Platform docs: [Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles). | `QsTile.java`, 63 lines, no permission. `BIND_QUICK_SETTINGS_TILE` is held by the system, not requested by the app. | Pausing must not be too easy ([Curbox #434](https://github.com/curbox-app/curbox-android/issues/434)). Design: tapping while blocking opens the pause-length dialog (also when the app is already open); tapping while paused resumes at once. When Android stopped the service the tile says "Not running" and opens the app. | **BUILD** |
| **Time-saved estimate** | "What did this give me back?" | Counts and streaks ship in [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin) and [Blockfy](https://github.com/buenotty/Blockfy). A minutes figure would be an estimate, so it would need a label. | An honest number needs usage access. | `PACKAGE_USAGE_STATS` is a new sensitive permission, and a made-up minutes-per-Reel guess is not honest. Both hurt trust. | **REJECTED** |
| **Streak** | Motivation. | [Blockfy](https://github.com/buenotty/Blockfy), [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin) have one. | Small | The counts already show the habit. A streak rewards "days without pausing" and adds pressure. | **LATER**: revisit with user feedback |
| **Home-screen widget with the count** | Quick glance. | None found in any competitor. Counters live in overlays instead ([Nudge](https://github.com/astraedus/nudge)). | RemoteViews layout + provider, ~5 KB ([widget docs](https://developer.android.com/develop/ui/views/appwidgets/)) | Low | **LATER**: the tile covers quick access |
| **Opt-in "breathe / why are you here" screen** | Hard blocks feel rigid. People want controlled access. | Strongest evidence in the category: one sec, Odisej, Nudge, Curbox ([one sec board](https://feedback.one-sec.app/p/allow-controlled-access-to-scrollable-videos-with-intervention), [Curbox #454](https://github.com/curbox-app/curbox-android/issues/454)). Counter: a one sec reviewer says the prompt "eventually becomes just another thing to tap through" ([search snippet](https://makeheadway.com/blog/one-sec-app-review/)). | A full-screen activity over Instagram and a third state machine | Changes the instant-close promise. | **LATER**: good candidate for v2.1 as opt-in |
| **Schedule** (block only at chosen hours) | Block at night or work hours only. | Common: [Blockfy](https://github.com/buenotty/Blockfy), [Reels Blocker](https://github.com/sahedalomsumit/reels-blocker), [Curbox](https://github.com/curbox-app/curbox-android), [Nudge #107](https://github.com/astraedus/nudge/issues/107) (title only). | Time pickers + alarm-free time checks | Inverts the default: Reels open outside the hours. The timed pause (15 min / 1 h) already covers short breaks. | **LATER** |
| **Allow Reels shared in DMs** | "stop scrolling TikTok without losing conversations with friends." | [Curbox #478](https://github.com/curbox-app/curbox-android/issues/478), [Nudge #18](https://github.com/astraedus/nudge/issues/18). The README says DM Reels are closed today ("not tested yet"). | Needs knowing how the viewer was opened | Means reading more of Instagram's screen. Risk to "reads only one view ID". | **LATER** |
| **Block Explore / other apps** (YouTube Shorts, TikTok) | Same habit in other places. | [FeedLock](https://github.com/caasion/FeedLock) blocks Explore. Nudge's Explore detection did not work ([#18](https://github.com/astraedus/nudge/issues/18)). | Extra packages in `accessibility.xml` | Other packages break "Instagram only". | **REJECTED** |
| **Allow N Reels per day** | Some want a small daily budget. | [Curbox #454](https://github.com/curbox-app/curbox-android/issues/454), [Blockfy](https://github.com/buenotty/Blockfy) (daily limit). | Medium | A budget is plausible later. | **LATER** |
| **Friction before disabling Accessibility** | "just pressing a button is too easy." | [Curbox #434](https://github.com/curbox-app/curbox-android/issues/434), [#473](https://github.com/curbox-app/curbox-android/issues/473) | n/a | The app cannot and must not stop the user from turning the service off in Android settings. Policy: never block disabling (item 9, section 3). | **REJECTED** |

## 6. What we build in v2.0

1. Clear onboarding with the disclosure screen, restricted-settings help and Xiaomi Autostart help.
2. Self-check for service health and for Instagram updates.
3. Quick Settings tile with the pause-length dialog.
4. This-week chart, kept on the phone.

Together the four files are 332 lines (`Welcome.java` 97, `Check.java` 72, `WeekChart.java` 100, `QsTile.java` 63). The whole app is 952 lines of Java in 8 files, and the part that acts on Instagram, `ReelBlockService.java`, is 56. Fewer good features beat many weak ones. Each answers a trust problem from section 2, and none touches the core idea.

**Cost in practice.** The APK went from about 30 KB (v1.4) to 93,981 bytes (about 94 KB; zipped, about 64 KB). GitHub CI measured it with the real Android SDK (build-tools `d8`, compileSdk 37) on commit `6d93a21`. The lines of code are not the main cost. `unzip -lv ReelBlock.apk` on that build shows where the bytes are:

| Part | Size | Notes |
|---|---|---|
| `resources.arsc` | 27,144 bytes | Stored uncompressed, as Android requires. |
| `classes.dex` | 41,176 bytes (19,495 compressed) | |
| `AndroidManifest.xml` | 3,924 bytes (1,248 compressed) | |
| `res/` files | 64 files, 86,124 bytes before compression | Layouts, drawables and colour lists. 61 have 2-character names such as `res/fN.xml`. The 3 colour lists in `res/color/` keep their names. |

The design system, vector icons, illustrations and dynamic-colour tokens are resources. To keep the file down:

- Colour roles, styles and drawables that no screen uses were removed (for example the outlined button and its drawable). All icons are vectors, and there is no AndroidX and no Material Components.
- `build.sh` runs `aapt2 optimize --shorten-resource-paths --collapse-resource-names`. Files inside the APK get short paths, and all resources share one name, because the app never looks up a resource by name. The `R` ids in the code do not change.
- `build.sh` compiles with `d8 --release` (no debug information).
- `build.sh` leaves the `R` classes out of the dex. `javac` already copies their ids into the code, and leaving them out saves about a third of the dex.

The earlier build had a `resources.arsc` of about 36 KB and a `classes.dex` of about 39 KB. They are now 27,144 and 41,176 bytes (the code grew in round 3; the table shrank). The effect of each step on its own was not measured.

## 7. Open questions

These could not be checked from the sandbox.

- **Verbatim Play text.** Policy pages 10964491, 10144311 and 9888379 were read only through search summaries. Re-read them before quoting.
- **Restricted settings per OEM.** The "Allow restricted settings" menu item differs on some phones, and some Infinix, Samsung and HiOS users reported no such option (unverified, from the source notes). Android 14 and 16 specifics were not found. Android 15's "Enhanced Confirmation Mode" is pre-release reporting only ([Android Authority](https://www.androidauthority.com/android-15-enhanced-confirmation-mode-3436697), search summary). Test with a real file-manager install, since `adb install` does not trigger the restriction.
- **Samsung cause.** Nudge #116 shows a Samsung service enabled but never bound. The cause is not confirmed.
- **HyperOS behaviour of `getEnabledAccessibilityServiceList`.** The self-check relies on it listing only bound services. This must be confirmed on the Xiaomi test phone, with the service frozen and not frozen. The app also keeps a flag that the service sets while Android has it bound, which covers the moment right after binding. For the same reason the home screen shows no status in the first second after it opens if the service looks stopped, and says "Android stopped Reel Block" only if it is still not running after that.
- **Instagram resource names.** The update check reads `clips_viewer_view_pager` by name from Instagram's resource table, once per Instagram install or update, on a background thread (the line says "Checking Instagram …" until it is done; tapping the Self-check row reads it again). The service finds the viewer by the same name, so a name that is gone means the service cannot find the viewer either. If Instagram's package cannot be read, the self-check says "could not read Instagram right now" and does not say "changed". Still open: this file has no test results for other Instagram versions.
- **`<queries>` for Instagram.** This is package visibility, not a permission. CI (`aapt2 dump badging`) and `release.sh` fail the build if the APK lists any permission.
- **Disclosure wording and storage (resolved).** The disclosure lists reading the Instagram version under "It can", not "It keeps": `State.java` stores only the settings and the counts, and `Check.java` holds Instagram's last update time and the lookup result in memory while the app runs.
- **On-phone script, not run.** `tools/device-check.sh` has not been run: it was written where no phone was available. Nothing in this file depends on its results. It cannot make "Android stopped Reel Block" or "Instagram has changed" on purpose, so those two screens are checked only in the Robolectric renders (`tools/screens/`, not on a phone).
- **Play review and Data safety claims** about one sec, ScreenZen and Opal are search snippets only.
- **Dynamic-colour shade mapping** and the "Expressive" label on the 20/32/48 dp shapes are unverified (section 4).
