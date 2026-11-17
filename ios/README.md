# Reel Block for iOS

Reel Block for iOS hides parts of instagram.com in Safari. It has no network code, no analytics and no logs.

Design: one Safari content blocker extension for each part, and no App Group. The result is zero entitlements
in the default build. You turn each part on or off in Settings. The app shows the state of each part.

## What it can do

- Safari, on instagram.com: hide Reels, Stories, the Comment button, Explore, and the suggested accounts row.
- Each part is a different switch.

## What it cannot do

- It cannot change the Instagram app. iOS does not permit that.
- It cannot hide suggested posts that are between the posts of friends.
- It does not hide the list of comments on a post page.
- Instagram can change its page. Then a rule can stop. `SELECTORS.md` shows which rules are confirmed.

The optional Screen Time part (below) can put a shield on the Instagram app. The shield blocks the full app.

## Build

You need Xcode. Accept the Xcode license one time: `sudo xcodebuild -license accept`.

    ./check.sh

This command does these steps:

1. It checks the JSON syntax of the rule lists.
2. It writes `ReelBlock.xcodeproj` again from `gen_project.py`.
3. It compiles the rule lists with WebKit and tests them on a fixture page (`check.swift`).
4. It builds the app for the iOS Simulator.
5. It starts the app in a simulator and saves `screens/home.png`.

Build only:

    DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -project ReelBlock.xcodeproj \
      -scheme ReelBlock -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO

To add a file or a part, change `gen_project.py`. Do not edit the project file.

## Turn the blocker on

1. Open the Settings app.
2. Tap Apps, then Safari, then Extensions. (iOS 16 and 17: Settings, then Safari, then Extensions.)
3. Turn on each Reel Block part that you want.
4. Open instagram.com in Safari.

## Files

| Path | Function |
|---|---|
| `App/ReelBlockApp.swift` | The app: state of each part, instructions, privacy card |
| `App/ScreenTime.swift` | Optional Screen Time part. Not in the default build |
| `Blocker/` | The extension code. All five extensions use it |
| `Rules/<part>/blockerList.json` | The rules of one part |
| `SELECTORS.md` | Source and status of each selector |

## Optional: Screen Time shield for the Instagram app

This part uses FamilyControls and ManagedSettings. The user selects Instagram in the system picker.
Then iOS shows a shield when the user opens the app.

It needs the entitlement `com.apple.developer.family-controls`. Thus it is not in the default build. Build it with:

    xcodebuild ... REELBLOCK_FLAGS=SCREENTIME REELBLOCK_ENTITLEMENTS=App/ScreenTime.entitlements

Do not use `CODE_SIGNING_ALLOWED=NO` for this build. Without a signature that has the entitlement,
the request for permission fails.

## App Store release: steps for the owner

Only the owner can do these steps.

1. Join the Apple Developer Program (developer.apple.com/programs). It has an annual fee.
2. Select the bundle ID. Replace `dev.local.reelblock` in `gen_project.py` and run it. The app reads its bundle ID
   at run time, thus no other file changes. Register the app ID and the five extension IDs
   (`<bundle id>.Reels`, `.Stories`, `.Comments`, `.Explore`, `.Suggested`). Automatic signing in Xcode can do this.
3. Open the project in Xcode. For each of the six targets, set your team in Signing & Capabilities.
4. Add an app icon (1024 x 1024). The project has no icon.
5. Only for the Screen Time part: request the Family Controls (Distribution) entitlement at
   developer.apple.com/contact/request/family-controls-distribution. Wait for the approval from Apple.
   Then add the Family Controls capability to the app ID.
6. In App Store Connect, make the app record. Fill in the privacy answers: the app collects no data.
7. In Xcode: Product, then Archive, then Distribute App, then App Store Connect. Then send the build for review.
