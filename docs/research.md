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

- This repo: the README, launch posts, both Java files, `accessibility.xml`, the manifest and `strings.xml`.
- GitHub READMEs and issue trackers of about a dozen similar apps (AwayDoomscrollin, Odisej, Nudge, Curbox, Blockfy and others). These were fetched directly and are first-hand.
- `developer.android.com` pages on accessibility services, tiles, widgets, dynamic colour and developer verification.
- Material 3 token source files on GitHub (section 4).

**Repo state on the research date**

- The repo's GitHub issue tracker has **0 issues** (checked 2026-11-04). There is no user feedback to learn from yet. Every "user problem" below comes from other projects.
- `docs/launch-posts.md` is **stale**. It still pitches "Instagram Reels and YouTube Shorts" and "about 120 lines". The app has been Instagram only since v1.2. The posts must be rewritten before anyone uses them.

**Limits**

- The sandbox blocked `m3.material.io`, `support.google.com`, `play.google.com`, `reddit.com` and the competitor websites (one sec, ScreenZen, Opal).
- The Google Play policy text and the Android Help restricted-settings article could not be opened. What this file says about them comes from **search summaries** of the official pages, not the pages. Those claims are marked "(search summary)". The exact wording must be checked before it is quoted anywhere.
- Play reviews, Play Data safety sections and Reddit threads were not read. Anything about one sec, ScreenZen and Opal is "(search snippet, unverified at source)". No Reddit thread is cited.
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
| 6 | **Too easy to bypass.** "just pressing a button is too easy." | [Curbox #434](https://github.com/curbox-app/curbox-android/issues/434) |
| 7 | **Sideload friction that looks like malware.** "Android 13+ doesn't allow access to notifications and Accessibility APIs for sideloaded apps." | [F-Droid #2680](https://gitlab.com/fdroid/fdroidclient/-/issues/2680), [block-instagram-reels #37](https://github.com/Moritz-Staat/block-instagram-reels/issues/37) |
| 8 | **Closed code or "all rights reserved" with no support route.** | [dev.to (search summary)](https://dev.to/astraedus/block-youtube-shorts-and-reels-on-android-keep-the-app-1b9h) |

**What this means for Reel Block.** The two strongest builders (1 and 2) are already true. The biggest gaps are builder 3 (no disclosure screen before settings) and breakers 1 and 2 (the app cannot tell the user when it stopped working or when Instagram changed). One more point: "Users can always disable the service in Accessibility settings." ([Odisej README](https://github.com/paco208/odisej)). That is true for every such app, and we should say it plainly.

## 3. Play policy as a trust checklist

Reel Block is not on Play. Play's rules are still the best public description of what a careful accessibility app should show the user. Sources: [Play accessibility policy 10964491](https://support.google.com/googleplay/android-developer/answer/10964491) and [User Data policy 10144311](https://support.google.com/googleplay/android-developer/answer/10144311). **Both are (search summary): the pages were not readable.** Android's own pages were read directly.

Today the **Turn on Reel Block** button opens Accessibility settings straight away. The privacy text sits lower on the same screen.

| # | Checklist item | What Reel Block will do |
|---|---|---|
| 1 | Show the disclosure in the app, before the settings handoff. It cannot live only in a README or privacy policy. (search summary) | A disclosure screen at first launch, before any button leads to Settings. |
| 2 | Keep it a separate notice, not bundled with other terms. (search summary) | The screen holds only this disclosure and two buttons. |
| 3 | Say why the permission is needed, what is read, how it is used and whether it is shared. (search summary; exact wording of 10144311 unverified) | Plain text: Reel Block checks whether Instagram's Reels screen is open and presses Back. It reads one view ID in Instagram only, never text, messages or photos. It runs on the phone only and has no internet permission. It also describes the one fixed action, because the policy prohibits autonomous action ("strictly prohibited", search summary). |
| 4 | Consent is a tap. Dismissing or pressing Back is not consent. (search summary) | Two buttons: **Agree and continue** and **Not now**. Back counts as Not now. |
| 5 | Show the flow for consent declined as well as granted. (search summary) | Not now leaves the app in a calm "off" state. No repeat prompts. |
| 6 | Do not open settings before consent, and do not change settings silently. [Android guide](https://developer.android.com/guide/topics/ui/accessibility/service) | Settings opens only after **Agree and continue**. The user flips the switch. |
| 7 | Do not set `isAccessibilityTool=true`. The default is false, and Android then shows a privacy notification after a while. [AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo) | `isAccessibilityTool` stays false (it is not in `accessibility.xml` today). The disclosure mentions the Android notice. |
| 8 | The `description` string must match the disclosure. It is not a substitute for the screen. (search summary) | One plain `service_description` string, in the same words as the screen. |
| 9 | Explain how to turn it off, and offer an in-app pause. Never nag. [Deceptive Behavior policy 9888379](https://support.google.com/googleplay/android-developer/answer/9888379) (URL and text unverified) | The disclosure and README show Settings > Accessibility. The pause switch stays in the app. The app never tries to stop the user from turning the service off. |
| 10 | Help with Android 13+ "Restricted setting". [Esper](https://www.esper.io/blog/android-13-sideloading-restriction-harder-malware-abuse-accessibility-apis), [CleanBrowsing](https://cleanbrowsing.org/support/mobile/disable-restricted-settings-android) (secondary) | On Android 13+, when the app was not installed by a store, the help is shown first. Steps: App info, three dots, **Allow restricted settings**. The option may appear only after one blocked attempt ([droidwin](https://droidwin.com/allow-restricted-settings-missing-from-sideloaded-apps-in-android-13-fix/)). |
| 11 | README claims match the app, with source and signing-certificate hash so users can check the APK. | README and disclosure say the same things. The release notes list the APK's SHA-256. |
| 12 | Plan for developer verification. [Verification guide](https://developer.android.com/developer-verification/guides), [FAQ](https://developer.android.com/developer-verification/guides/faq) | A short README note for 2027 (below). |

**Developer verification note (2027).** Android is rolling out developer registration. Enforcement began 30 Sep 2026 in Brazil, Indonesia, Singapore and Thailand, for participating stores. The FAQ (updated 15 Jul 2026) says that for sideloaded apps "these new verification requirements won't apply to your app yet". A global rollout follows in 2027. Unregistered apps will then need an "advanced flow" (developer mode, a one-day wait, a biometric check). The maintainer should register the package name and signing key before 2027 ($25 per the FAQ).

**Play fit.** Whether a reel blocker is an allowed accessibility use on Play could not be verified. Android's guide says to build such a service only for "a general-purpose assistive tool" ([guide](https://developer.android.com/guide/topics/ui/accessibility/service)). That is why the app is sideloaded.

## 4. Material 3 / M3 Expressive notes we use

`docs/design-system.md` holds the full system. Below is the short version, with sources.

Sources, called **TOKENS** here: androidx Compose `material3/tokens/*.kt` in the [androidx repo](https://github.com/androidx/androidx/tree/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens). **MDC** is the [material-components-android docs](https://github.com/material-components/material-components-android/tree/master/docs). Quoting M3 Expressive: it is "an expansion of Material Design 3" that "complements the Android 16 visual style" ([Android docs](https://developer.android.com/develop/ui/compose/designsystems/material3)).

**Colour roles (tone, light / dark)** ([MDC Color](https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md), TOKENS `ColorLightTokens`, `ColorDarkTokens`)

- primary 40 / 80. on-primary 100 / 20. primary-container 90 / 30. on-primary-container 10 / 90.
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
- Cards are 12 dp. Dialogs are 28 dp. Whether 20, 32 and 48 are officially "Expressive" tokens is unverified.

**Motion** (TOKENS `MotionTokens`, [MDC Motion](https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md))

- Durations: short 50, 100, 150, 200 ms; medium 250 to 400; long 450 to 600; extra-long 700 to 1000.
- **Emphasized-decelerate** cubic-bezier (0.05, 0.7, 0.1, 1) for things that enter.
- **Standard** cubic-bezier (0.2, 0, 0, 1) for things that move on screen.
- Expressive springs need AndroidX, which the app does not use. Platform `PathInterpolator` (API 21) covers the two curves.

**Components**

- Switch: track **52 x 32 dp**, thumb 16 dp off, 24 dp on, 28 dp pressed (TOKENS `SwitchTokens`).
- Lists: one, two and three lines are **56 / 72 / 88 dp** (TOKENS `ListTokens`).
- Dialog: shape 28 dp, container surface-container-high, text headline-small and body-medium ([MDC Dialog](https://github.com/material-components/material-components-android/blob/master/docs/components/Dialog.md)).

**Dynamic colour** ([dynamic colour page](https://developer.android.com/develop/ui/views/theming/dynamic-colors), [R.color](https://developer.android.com/reference/android/R.color))

- API 31+: `android.R.color.system_accent1_*`, `accent2`, `accent3`, `neutral1`, `neutral2`, each with shades 0 to 1000.
- API 34+: role colours such as `system_primary_light` and `system_surface_container_high_dark`.
- minSdk is 26, so: API 34+ reads roles, API 31+ reads shades, older phones use fixed baseline hexes. The shade-to-tone mapping (accent1_600 = tone 40) is an AOSP convention and is not on the page (unverified).

## 5. Candidate features

"Cost" is a rough estimate of added code and APK size. Verdicts are final for v2.0.

| Feature | User problem | Evidence | Cost | Risk to the core idea | Verdict |
|---|---|---|---|---|---|
| **Clear onboarding**: disclosure ("what it can and cannot see") before Settings; Android 13+ "Restricted setting" help, shown first on Android 13+ when the app was not installed by a store; Xiaomi Autostart help | People distrust accessibility prompts. The switch is greyed out on sideloaded apps. Xiaomi stops the service. | Breaker 7, builder 3 (section 2). [F-Droid #2680](https://gitlab.com/fdroid/fdroidclient/-/issues/2680), [spec #27](https://github.com/Moritz-Staat/block-instagram-reels/issues/27). Only README text ships in competitors, except [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin) (guides in EN and TR). | ~150 lines + strings, ~6 KB | None. Strengthens trust. | **BUILD** |
| **Self-check**: (a) service health; (b) Instagram update check | (a) Android lists the service as on, but it is not running (Xiaomi "Not working", Samsung). (b) Instagram changes an ID and Reels silently come back. | (a) [Nudge #116](https://github.com/astraedus/nudge/issues/116), [Nudge #57](https://github.com/astraedus/nudge/issues/57), [Norton community](https://community.norton.com/t/accessibility-service-automatically-toggles-off-on-samsung-devices-s23-ultra-tab-s5e-device/494122). (b) Every open-source blocker warns of it ([Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker)). **No competitor ships (b).** Nudge covers (a) only. | ~80 lines, ~2 KB | None: no network, no user data read. Part (b) does not look at Instagram's screen. | **BUILD** |
| **This-week chart**: on-device counts for the last 7 days, today, all time | The habit is invisible. A single total says little. | Local stats ship in [AwayDoomscrollin](https://github.com/ResolveCommunity/AwayDoomscrollin), [Blockfy](https://github.com/buenotty/Blockfy), [Odisej](https://github.com/paco208/odisej), [Nudge](https://github.com/astraedus/nudge). | The service writes one extra int per close, ~1 KB | None. Stored only in app prefs, excluded from backup. | **BUILD** |
| **Quick Settings tile** | Pause and resume without opening the app. | Weak. No user requests found. Odisej has a state-only tile ([README](https://github.com/paco208/odisej)). Platform docs: [Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles). | ~60 lines, no permission. `BIND_QUICK_SETTINGS_TILE` is held by the system, not requested by the app. | Pausing must not be too easy ([Curbox #434](https://github.com/curbox-app/curbox-android/issues/434)). Design: tapping while blocking opens the pause-length dialog; tapping while paused resumes at once. | **BUILD** |
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

Together they cost roughly 10 KB and about 300 lines. Fewer good features beat many weak ones. Each answers a trust problem from section 2, and none touches the core idea.

## 7. Open questions

These could not be checked from the sandbox.

- **Verbatim Play text.** Policy pages 10964491, 10144311 and 9888379 were read only through search summaries. Re-read them before quoting.
- **Restricted settings per OEM.** The "Allow restricted settings" menu item differs on some phones, and some Infinix, Samsung and HiOS users reported no such option (unverified, from the source notes). Android 14 and 16 specifics were not found. Android 15's "Enhanced Confirmation Mode" is pre-release reporting only ([Android Authority](https://www.androidauthority.com/android-15-enhanced-confirmation-mode-3436697), search summary). Test with a real file-manager install, since `adb install` does not trigger the restriction.
- **Samsung cause.** Nudge #116 shows a Samsung service enabled but never bound. The cause is not confirmed.
- **HyperOS behaviour of `getEnabledAccessibilityServiceList`.** The self-check relies on it listing only bound services. This must be confirmed on the Xiaomi test phone, with the service frozen and not frozen.
- **Instagram resource names.** The update check assumes `clips_viewer_view_pager` is readable by name in Instagram's resource table. If Instagram shrinks or renames resources, `getIdentifier` could fail for a reason that is not a changed viewer. Test on the installed Instagram, and show "cannot tell" rather than "broken" when unsure.
- **`<queries>` for Instagram.** This is package visibility, not a permission. Confirm the APK still lists no permissions after the change.
- **Play review and Data safety claims** about one sec, ScreenZen and Opal are search snippets only.
- **Dynamic-colour shade mapping** and the "Expressive" label on the 20/32/48 dp shapes are unverified (section 4).
