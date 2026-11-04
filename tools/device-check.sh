#!/bin/sh
# On-phone checks for a release. Needs adb, python3 and a phone with Instagram signed in.
#   ANDROID_SERIAL=9de4a89b tools/device-check.sh
# Optional: REELS_TAP="324 2286" (Reels tab on a 1080x2400 screen), OUT=out/device (screenshots + dumps).
# It installs the build, turns the service on through adb (adb is allowed to; no "Restricted setting"),
# then checks: a Reel closes and the count goes up; the Quick Settings tile pauses and resumes; the
# self-check line; no Recents card and no crashed service after leaving the app (HyperOS); screenshots
# in light, dark and font scale 2.0; and that every clickable node has text or a content description.
# Font scale and dark mode are put back at the end, even when a check fails.
set -e
cd "$(dirname "$0")/.."
PKG=dev.local.reelblock
SVC=$PKG/$PKG.ReelBlockService
TILE=$PKG/$PKG.QsTile
OUT=${OUT:-out/device}
TAP=${REELS_TAP:-324 2286}
mkdir -p "$OUT"
A() { adb shell "$@"; }
FONT0=$(A settings get system font_scale | tr -d '\r'); NIGHT0=$(A cmd uimode night | tr -d '\r' | sed 's/.*: //')
restore() { A settings put system font_scale "${FONT0:-1.0}" >/dev/null; A cmd uimode night "${NIGHT0:-no}" >/dev/null || true; }
trap restore EXIT
pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; FAILED=1; }
FAILED=0

# Dump the screen and print one line per node: clickable|text|content-desc|resource-id|bounds
dump() {
  A uiautomator dump /sdcard/rb.xml >/dev/null 2>&1; adb pull /sdcard/rb.xml "$OUT/$1.xml" >/dev/null 2>&1
  python3 - "$OUT/$1.xml" <<'EOF'
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    print('|'.join([n.get('clickable'), n.get('text', ''), n.get('content-desc', ''), n.get('resource-id', ''), n.get('bounds')]))
EOF
}
# "All time 128" on the home screen -> 128 (0 when the empty state shows).
count() { open_app; dump count | grep -o 'All time [0-9,]*' | head -1 | tr -dc '0-9' | sed 's/^$/0/'; }
open_app() { A am start -W -n $PKG/.MainActivity >/dev/null; sleep 2; }
tap_text() { b=$(dump tap | awk -F'|' -v t="$1" '$2==t {print $5; exit}'); [ -n "$b" ] || return 1
  set -- $(echo "$b" | tr -c '0-9' ' '); A input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )); sleep 1; }
open_reels() { A monkey -p com.instagram.android -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 5; A input tap $TAP; sleep 4; }

echo "== Install and turn on"
NO_INSTALL= ./build.sh "$ANDROID_SERIAL" >/dev/null
A settings put secure enabled_accessibility_services $SVC
A settings put secure accessibility_enabled 1
sleep 3
A dumpsys accessibility | grep -q "$PKG" && pass "service is bound" || fail "service is not bound"

echo "== A Reel closes and the count goes up"
before=$(count); open_reels; after=$(count)
[ "$after" -gt "$before" ] && pass "count $before -> $after" || fail "count did not go up ($before -> $after)"
A dumpsys activity top | grep -q "clips_viewer_view_pager.*V" && fail "Reels viewer still visible" || pass "Reels viewer closed"

echo "== Self-check"
open_app; dump self | grep -q "Running. Instagram" && pass "self-check says running" || fail "self-check line missing"

echo "== Quick Settings tile: pause asks for how long, resume is one tap"
A cmd statusbar add-tile $TILE >/dev/null 2>&1 || true
A cmd statusbar click-tile $TILE; sleep 2
dump tile | grep -q "|Pause blocking?|" && pass "tile opens the pause dialog" || fail "tile did not open the pause dialog"
tap_text "For 15 minutes" || fail "no 15-minute option"
before=$(count); open_reels; after=$(count)
[ "$after" = "$before" ] && pass "paused: Reels stay open" || fail "paused but the count went up"
A cmd statusbar click-tile $TILE; sleep 1
open_app; dump resumed | grep -q "|Reels are blocked|" && pass "tile resumed blocking" || fail "tile did not resume"
A cmd statusbar remove-tile $TILE >/dev/null 2>&1 || true

echo "== HyperOS: no Recents card, no crashed service after leaving the app"
open_app; A input keyevent KEYCODE_HOME; sleep 2
A dumpsys activity recents | grep -q "$PKG" && fail "Recents still has a Reel Block card" || pass "no Recents card"
A dumpsys accessibility | sed -n '/[Cc]rashed/,/^$/p' | grep -q "$PKG" && fail "listed as crashed" || pass "not in crashed services"

echo "== Screenshots and labels: light, dark, font scale 2.0"
for v in light dark font2; do
  A cmd uimode night $([ $v = dark ] && echo yes || echo no) >/dev/null
  A settings put system font_scale $([ $v = font2 ] && echo 2.0 || echo 1.0)
  open_app; A screencap -p /sdcard/rb.png; adb pull /sdcard/rb.png "$OUT/home-$v.png" >/dev/null
  bad=$(dump "labels-$v" | awk -F'|' '$1=="true" && $2=="" && $3=="" {print $4 " " $5}')
  # A clickable row without its own label is fine when TalkBack reads its children's text: check that.
  unlabeled=0
  for b in $(echo "$bad" | awk '{print $2}'); do
    python3 - "$OUT/labels-$v.xml" "$b" <<'EOF' || unlabeled=$((unlabeled + 1))
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    if n.get('bounds') == sys.argv[2]:
        sys.exit(0 if any(c.get('text') or c.get('content-desc') for c in n.iter('node')) else 1)
sys.exit(1)
EOF
  done
  [ "$unlabeled" = 0 ] && pass "$v: every clickable node has text or content-desc" || fail "$v: $unlabeled clickable nodes without a label"
done

echo
[ "$FAILED" = 0 ] && echo "All device checks passed. Files in $OUT/." || { echo "Some device checks failed. Files in $OUT/."; exit 1; }
