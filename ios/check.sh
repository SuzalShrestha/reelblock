#!/bin/sh
# One command for all checks: ./check.sh
# Steps 1 to 3 need only the Command Line Tools. Steps 4 and 5 need Xcode with its license accepted.
set -e
cd "$(dirname "$0")"
TMP=$(mktemp -d)

echo "== 1. JSON syntax"
for f in Rules/*/blockerList.json; do python3 -m json.tool "$f" >/dev/null && echo "ok $f"; done

echo "== 2. Xcode project file"
python3 gen_project.py
plutil -lint ReelBlock.xcodeproj/project.pbxproj

echo "== 3. Rule lists in WebKit"
cp check.swift "$TMP/main.swift"
xcrun swiftc -o "$TMP/check" "$TMP/main.swift"
"$TMP/check" Rules

export DEVELOPER_DIR="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
echo "== 4. Simulator build"
if ! xcodebuild -license check >/dev/null 2>&1; then
    echo "SKIPPED: the Xcode license is not accepted. Run: sudo xcodebuild -license accept"
    exit 2
fi
xcodebuild -project ReelBlock.xcodeproj -scheme ReelBlock -destination 'generic/platform=iOS Simulator' \
    -derivedDataPath build build CODE_SIGNING_ALLOWED=NO | tail -3

echo "== 5. Simulator screenshot"
DEVICE=$(xcrun simctl list devices available | grep -m1 -oE 'iPhone[^(]*\([0-9A-F-]{36}\)' | grep -oE '[0-9A-F-]{36}' || true)
if [ -z "$DEVICE" ]; then
    echo "SKIPPED: no iOS Simulator runtime is installed. Xcode > Settings > Components installs one."
    exit 2
fi
xcrun simctl bootstatus "$DEVICE" -b
xcrun simctl install "$DEVICE" build/Build/Products/Debug-iphonesimulator/ReelBlock.app
xcrun simctl launch "$DEVICE" dev.local.reelblock
sleep 10; mkdir -p screens  # the first frame can take 6 s on a busy Mac

xcrun simctl io "$DEVICE" screenshot screens/home.png
echo "ok screens/home.png"
