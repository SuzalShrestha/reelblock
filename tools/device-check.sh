#!/bin/sh
# On-phone checks for a release. Needs adb, python3 and a phone with Instagram signed in.
#   ANDROID_SERIAL=9de4a89b tools/device-check.sh
# Options: REELS_TAP="324 2286" (Reels tab on a 1080x2400 screen), OUT=out/device (screenshots and dumps),
#          FIRST_RUN=1 (also shoot the first-run page 1: clears Reel Block's data, so the count goes back to 0).
# It installs the build, turns the service on through adb (adb may do this; it is not a "Restricted setting"),
# then checks: a Reel closes and the count goes up; the Quick Settings tile pauses and resumes; the self-check;
# no Recents card and no crashed service after leaving the app (HyperOS); and, in light, dark and font scale
# 2.0, a screenshot of every screen plus a check that every clickable node has text or a content description.
# Font scale, dark mode and the list of enabled accessibility services are put back at the end.
set -e
cd "$(dirname "$0")/.."
PKG=dev.local.reelblock
SVC=$PKG/$PKG.ReelBlockService
TILE=$PKG/$PKG.QsTile
OUT=${OUT:-out/device}
TAP=${REELS_TAP:-324 2286}
mkdir -p "$OUT"
A() { adb shell "$@" | tr -d '\r'; }
FONT0=$(A settings get system font_scale); NIGHT0=$(A cmd uimode night | sed 's/.*: //')
SVCS0=$(A settings get secure enabled_accessibility_services)
restore() {
  A settings put system font_scale "${FONT0:-1.0}" >/dev/null
  A cmd uimode night "${NIGHT0:-no}" >/dev/null || true
  # Keep Reel Block on if it was on before; never drop TalkBack or any other service.
  if [ -z "$SVCS0" ] || [ "$SVCS0" = null ]; then A settings delete secure enabled_accessibility_services >/dev/null
  else A settings put secure enabled_accessibility_services "$SVCS0" >/dev/null; fi
}
trap restore EXIT
FAILED=0
pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; FAILED=1; }

# Dump the screen; print one line per node: clickable|text|content-desc|resource-id|bounds
dump() {
  A uiautomator dump /sdcard/rb.xml >/dev/null 2>&1; adb pull /sdcard/rb.xml "$OUT/$1.xml" >/dev/null 2>&1
  python3 - "$OUT/$1.xml" <<'EOF'
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    print('|'.join([n.get('clickable'), n.get('text', ''), n.get('content-desc', ''), n.get('resource-id', ''), n.get('bounds')]))
EOF
}
# Every clickable Reel Block node needs a label of its own or text inside it (TalkBack reads a row's children).
labels() {
  python3 - "$OUT/$1.xml" "$PKG" <<'EOF'
import sys, xml.etree.ElementTree as ET
bad = []
for n in ET.parse(sys.argv[1]).iter('node'):
    if n.get('package') != sys.argv[2] or n.get('clickable') != 'true': continue
    if not any(c.get('text') or c.get('content-desc') for c in n.iter('node')):
        bad.append(n.get('resource-id') or n.get('class'))
for x in bad: print('UNLABELED', x)
sys.exit(1 if bad else 0)
EOF
}
shot() { A screencap -p /sdcard/rb.png; adb pull /sdcard/rb.png "$OUT/$1.png" >/dev/null; dump "$1" >/dev/null
  labels "$1" >/dev/null && pass "$1: every clickable node is labelled" || fail "$1: unlabelled clickable node (see $OUT/$1.xml)"; }
open_app() { A am start -W -n $PKG/.MainActivity >/dev/null; sleep 2; }
tap_text() { b=$(dump tap | awk -F'|' -v t="$1" '$2==t {print $5; exit}'); [ -n "$b" ] || return 1
  set -- $(echo "$b" | tr -c '0-9' ' '); A input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )); sleep 2; }
scroll_down() { A input swipe 540 1800 540 600 300; sleep 1; }
# "All time 128" on the home screen -> 128 (0 when the empty state shows).
count() { open_app; n=$(dump count | grep -o 'All time [0-9,.]*' | head -1 | tr -dc '0-9'); echo "${n:-0}"; }
open_reels() { A monkey -p com.instagram.android -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 5; A input tap $TAP; sleep 4; }
bound() { A dumpsys accessibility | grep -q "Bound services:{.*label=Reel Block"; }
crashed() { A dumpsys accessibility | grep "Crashed services" | grep -q "$PKG"; }
# The Reels viewer is on screen: its view line in "dumpsys activity top" has the V (visible) flag.
viewer_visible() { A dumpsys activity top | grep "clips_viewer_view_pager" | grep -qE '\{[0-9a-f]+ V'; }

echo "== Install and turn on"
NO_INSTALL= ./build.sh "$ANDROID_SERIAL" >/dev/null
case ":$SVCS0:" in *":$SVC:"*) ;; *) A settings put secure enabled_accessibility_services "${SVCS0:+$SVCS0:}$SVC";; esac
A settings put secure accessibility_enabled 1
sleep 3
bound && pass "service is bound" || fail "service is not bound"

echo "== A Reel closes and the count goes up"
before=$(count); open_reels; viewer_visible && fail "Reels viewer still visible" || pass "Reels viewer closed"
after=$(count)
[ "$after" -gt "$before" ] && pass "count $before -> $after" || fail "count did not go up ($before -> $after)"

echo "== Self-check"
open_app; dump self | grep -q "Running. Instagram" && pass "self-check: running, Reels screen found" || fail "self-check line missing"

echo "== Quick Settings tile: pause asks for how long, resume is one tap"
A cmd statusbar add-tile $TILE >/dev/null 2>&1 || true
A cmd statusbar click-tile $TILE; sleep 2
dump tile | grep -q "|Pause blocking?|" && pass "tile opens the pause dialog" || fail "tile did not open the pause dialog"
tap_text "For 15 minutes" || fail "no 15-minute option"
before=$(count); open_reels; after=$(count)
[ "$after" = "$before" ] && pass "paused: Reels stay open" || fail "paused, but the count went up"
A cmd statusbar click-tile $TILE; sleep 1
open_app; dump resumed | grep -q "|Reels are blocked|" && pass "tile resumed blocking" || fail "tile did not resume"
A cmd statusbar remove-tile $TILE >/dev/null 2>&1 || true

echo "== HyperOS: no Recents card, no crashed service after leaving the app"
open_app; A input keyevent KEYCODE_HOME; sleep 2
A dumpsys activity recents | grep -q "realActivity=$PKG" && fail "Recents still has a Reel Block card" || pass "no Recents card"
crashed && fail "listed under crashed services" || pass "not in crashed services"
bound && pass "still bound after leaving the app" || fail "not bound after leaving the app"

echo "== Every screen in light, dark and font scale 2.0"
for v in light dark font2; do
  A cmd uimode night $([ $v = dark ] && echo yes || echo no) >/dev/null
  A settings put system font_scale $([ $v = font2 ] && echo 2.0 || echo 1.0)
  sleep 1
  open_app; shot "home-$v"; scroll_down; shot "home-lower-$v"; scroll_down; shot "home-end-$v"
  open_app; tap_text "Block Reels" && shot "pause-dialog-$v"; tap_text "Cancel" || A input keyevent KEYCODE_BACK
  open_app; scroll_down; tap_text "What Reel Block can see" && { shot "disclosure-$v"; scroll_down; shot "disclosure-end-$v"; }
  open_app; scroll_down; tap_text "Setup help" && shot "setup-$v"
done
if [ -n "$FIRST_RUN" ]; then
  A pm clear $PKG >/dev/null; A settings put system font_scale 1.0; A cmd uimode night no >/dev/null
  open_app; shot "first-run-1-light"; tap_text "Get started" && shot "first-run-2-light"
fi

echo
[ "$FAILED" = 0 ] && echo "All device checks passed. Files in $OUT/." || { echo "Some device checks failed. Files in $OUT/."; exit 1; }
