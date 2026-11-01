#!/bin/sh
# Publishes a GitHub release from main. Usage: ./release.sh notes.md
# The version comes from versionName in AndroidManifest.xml (bump it and versionCode first).
# Signs with the local debug.keystore, so the key never leaves this computer.
# Build tool overrides (BUILD_TOOLS, ANDROID_JAR) work as in build.sh.
set -e
cd "$(dirname "$0")"
NOTES=${1:?usage: ./release.sh notes.md}
[ -f "$NOTES" ] || { echo "no notes file: $NOTES"; exit 1; }
# Every user who installed an older release has this certificate. Another key cannot upgrade them.
CERT=7c30d87c9351ad58ed3ec783ebf29bb3f21595d1d08447690f4633dfa0310981
TAG=v$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' AndroidManifest.xml)

[ "$(git branch --show-current)" = main ] || { echo "switch to main first"; exit 1; }
git fetch -q origin
[ -z "$(git status --porcelain)" ] || { echo "commit or stash your changes first"; exit 1; }
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || { echo "main is not the same as origin/main"; exit 1; }
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null && { echo "$TAG exists: bump versionName and versionCode"; exit 1; }

NO_INSTALL=1 ./build.sh
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}
"$BT/apksigner" verify --print-certs ReelBlock.apk | grep -q "certificate SHA-256 digest: $CERT" \
  || { echo "ReelBlock.apk is not signed with the release key"; exit 1; }
! "$BT/aapt2" dump badging ReelBlock.apk | grep -q uses-permission \
  || { echo "ReelBlock.apk asks for a permission: Reel Block must have none"; exit 1; }

SUM=$(shasum -a 256 ReelBlock.apk | cut -d' ' -f1)
git tag -a "$TAG" -m "Reel Block $TAG"
git push -q origin "$TAG"
{ cat "$NOTES"; printf '\nSHA-256: `%s`\n' "$SUM"; } > out/notes.md
gh release create "$TAG" ReelBlock.apk --title "Reel Block $TAG" --notes-file out/notes.md --latest
