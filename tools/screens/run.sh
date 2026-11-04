#!/bin/sh
# Renders every scene in scenes.txt and audits it: tools/screens/run.sh [Shots|ShotsR] [variants]
#   Shots  = Android 14 (colours follow the default wallpaper), ShotsR = Android 11 (fixed brand colours).
#   variants: comma list of light, dark, font2, dark-font2, rtl, small (default light,dark,font2).
# Needs Maven and a JDK 17+, and the same BUILD_TOOLS / ANDROID_JAR as build.sh (run ./build.sh first).
# Output: out/screens/<class>/*.png and audit.txt (every clickable view: a label, and at least 48dp).
set -e
cd "$(dirname "$0")"
ROOT=$(cd ../.. && pwd)
CLASS=${1:-Shots}; VARIANTS=${2:-light,dark,font2}
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}
JAR=${ANDROID_JAR:-$(ls "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)}
OUT=$ROOT/out/screens/$CLASS; APP=$ROOT/out/screens/app
[ -d "$ROOT/out/classes" ] || { echo "run ./build.sh first"; exit 1; }
rm -rf "$OUT" "$APP"; mkdir -p "$OUT" "$APP" src/test/resources/com/android/tools
(cd "$ROOT/out/classes" && zip -qr "$APP/app.jar" .)
# Robolectric needs the resources with their names, so link them again without build.sh's name collapsing.
"$BT/aapt2" link -I "$JAR" --manifest "$ROOT/AndroidManifest.xml" -o "$APP/res.apk" "$ROOT/out/res.zip"
printf 'android_merged_manifest=%s\nandroid_resource_apk=%s\nandroid_custom_package=dev.local.reelblock\n' \
  "$ROOT/AndroidManifest.xml" "$APP/res.apk" > src/test/resources/com/android/tools/test_config.properties
cp scenes.txt "$OUT/scenes.txt"
mvn -q -B test -Dappjar="$APP/app.jar" -Dshotsout="$OUT" -Dshotsvariants="$VARIANTS" -Dshotsclass="$CLASS"
grep -c ' OK' "$OUT/audit.txt" | xargs echo "screens audited OK:"; ! grep FAIL "$OUT/audit.txt"
