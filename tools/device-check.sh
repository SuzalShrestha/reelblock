#!/bin/sh
# On-phone checks for a release. Needs adb, python3 and a phone with Instagram signed in.
#   ANDROID_SERIAL=9de4a89b tools/device-check.sh
# Options: REELS_TAP="324 2286" (Reels tab on a 1080x2400 screen), OUT=out/device (screenshots and dumps),
#          FIRST_RUN=1 (also the 3 first-run pages; this clears Reel Block's data, so the count goes back to 0).
# Steps: install the build; turn the service on through adb (adb may; this is not a "Restricted setting");
# a Reel closes and the count goes up; the Quick Settings tile pauses (asks how long) and resumes; the
# self-check line; no Recents card and no crashed service after leaving the app (HyperOS); then, in light,
# dark and font scale 2.0, a screenshot of each screen (home top to bottom, pause dialog, paused, off,
# disclosure, setup, first run) and a check that every clickable node there has text or a content description.
# Screens it cannot make on purpose: "Android stopped Reel Block" and "Instagram has changed".
# At the end (also on Ctrl-C) it puts back font scale, dark mode and the accessibility settings it changed.
cd "$(dirname "$0")/.." || exit 1
PKG=dev.local.reelblock
SVC=$PKG/$PKG.ReelBlockService
TILE=$PKG/$PKG.QsTile
OUT=${OUT:-out/device}
TAP=${REELS_TAP:-324 2286}
mkdir -p "$OUT"
A() { adb shell "$@" | tr -d '\r'; }
unnull() { [ "$1" = null ] && echo "" || echo "$1"; }
FONT0=$(unnull "$(A settings get system font_scale)"); NIGHT0=$(A cmd uimode night | sed 's/.*: //')
SVCS0=$(unnull "$(A settings get secure enabled_accessibility_services)")
AE0=$(unnull "$(A settings get secure accessibility_enabled)")
restore() {
  A settings put system font_scale "${FONT0:-1.0}" >/dev/null
  A cmd uimode night "${NIGHT0:-no}" >/dev/null
  if [ -z "$SVCS0" ]; then A settings delete secure enabled_accessibility_services >/dev/null
  else A settings put secure enabled_accessibility_services "$SVCS0" >/dev/null; fi
  if [ -z "$AE0" ]; then A settings delete secure accessibility_enabled >/dev/null
  else A settings put secure accessibility_enabled "$AE0" >/dev/null; fi
}
trap restore EXIT
trap 'exit 130' INT TERM
FAILED=0
pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; FAILED=1; }
SIZE=$(A wm size | grep -o '[0-9]*x[0-9]*' | tail -1); W=${SIZE%x*}; H=${SIZE#*x}

# Fresh dump of the screen into $OUT/$1.xml. Fails (returns 1) instead of reusing an old dump.
dump() {
  A rm -f /sdcard/rb.xml
  A uiautomator dump /sdcard/rb.xml | grep -q "dumped to" && adb pull /sdcard/rb.xml "$OUT/$1.xml" >/dev/null 2>&1
}
# One line per node: clickable|text|content-desc|resource-id|bounds
nodes() {
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
bad = [n.get('resource-id') or n.get('class') for n in ET.parse(sys.argv[1]).iter('node')
       if n.get('package') == sys.argv[2] and n.get('clickable') == 'true'
       and not any(c.get('text') or c.get('content-desc') for c in n.iter('node'))]
for x in bad: print('unlabelled:', x)
sys.exit(1 if bad else 0)
EOF
}
shot() {
  A rm -f /sdcard/rb.png; A screencap -p /sdcard/rb.png; adb pull /sdcard/rb.png "$OUT/$1.png" >/dev/null 2>&1 \
    || { fail "$1: no screenshot"; return; }
  dump "$1" || { fail "$1: uiautomator dump failed"; return; }
  labels "$1" >"$OUT/$1.labels.txt" && pass "$1: every clickable node is labelled" || fail "$1: see $OUT/$1.labels.txt"
}
scroll_down() { A input swipe $((W / 2)) $((H * 3 / 4)) $((W / 2)) $((H / 4)) 300; sleep 1; }
# Tap the node with this exact text, scrolling down up to 4 times to find it.
tap_text() {
  i=0
  while [ $i -le 4 ]; do
    dump tap && b=$(nodes tap | awk -F'|' -v t="$1" '$2==t {print $5; exit}')
    if [ -n "$b" ]; then
      # shellcheck disable=SC2046  # split "[x1,y1][x2,y2]" into four numbers on purpose
      set -- $(echo "$b" | tr -c '0-9' ' '); A input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )); sleep 2; return 0
    fi
    scroll_down; i=$((i + 1))
  done
  return 1
}
go() { tap_text "$1" || { fail "$2: could not find \"$1\""; return 1; }; }
on_screen() { dump probe && nodes probe | grep -q "|$1|"; }
# Home first: leaving closes Reel Block's task (HyperOS fix), so it opens fresh at the top. Never "am start -S":
# a force-stop makes Android remove the app's accessibility service from the enabled list.
open_app() { A input keyevent KEYCODE_HOME; sleep 1; A am start -W -n $PKG/.MainActivity >/dev/null; sleep 2; }
# "All time 128" on the home screen -> 128 (0 when the empty state shows).
count() { open_app; dump count; n=$(nodes count 2>/dev/null | grep -o 'All time [0-9,.]*' | head -1 | tr -dc '0-9'); echo "${n:-0}"; }
open_reels() { A monkey -p com.instagram.android -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 5; A input tap $TAP; sleep 4; }
service_on() {
  case ":$SVCS0:" in *":$SVC:"*) L=$SVCS0;; *) L=${SVCS0:+$SVCS0:}$SVC;; esac
  A settings put secure enabled_accessibility_services "$L"; A settings put secure accessibility_enabled 1; sleep 3
}
service_off() { A settings put secure enabled_accessibility_services "$(echo ":$SVCS0:" | sed "s#:$SVC:#:#g; s#^:##; s#:\$##")"; sleep 2; }
bound() { A dumpsys accessibility | grep -q "Bound services:{.*label=Reel Block"; }
crashed() { A dumpsys accessibility | grep "Crashed services" | grep -q "$PKG"; }
# The Reels viewer is on screen: its view line in "dumpsys activity top" has the V (visible) flag. This sees
# only the view's own flag; the count going up is the main proof that Reel Block pressed Back.
viewer_visible() { A dumpsys activity top | grep "clips_viewer_view_pager" | grep -qE '\{[0-9a-f]+ V'; }

echo "== Install and turn on"
NO_INSTALL='' ./build.sh "$ANDROID_SERIAL" >/dev/null || { echo "FAIL  build or install"; exit 1; }
service_on
bound && pass "service is bound" || fail "service is not bound"

echo "== A Reel closes and the count goes up"
before=$(count); open_reels
viewer_visible && fail "Reels viewer still visible" || pass "Reels viewer not visible"
after=$(count)
[ "$after" -gt "$before" ] 2>/dev/null && pass "count $before -> $after" || fail "count did not go up ($before -> $after)"

echo "== Self-check"
open_app; on_screen "Running. Instagram .* has the Reels screen." && pass "self-check: running, Reels screen found" || fail "self-check line not found"

echo "== Quick Settings tile: pause asks how long, resume is one tap"
A cmd statusbar add-tile $TILE >/dev/null 2>&1
A cmd statusbar click-tile $TILE; sleep 2
on_screen "Pause blocking?" && pass "tile opens the pause dialog" || fail "tile did not open the pause dialog"
go "For 15 minutes" "tile pause"
before=$(count); open_reels; after=$(count)
[ "$after" = "$before" ] && pass "paused: Reels stay open" || fail "paused, but the count went up"
A cmd statusbar click-tile $TILE; sleep 1
open_app; on_screen "Reels are blocked" && pass "tile resumed blocking" || fail "tile did not resume"
echo "note  the tile stays in Quick Settings; remove it by hand if you do not want it"

echo "== HyperOS: no Recents card, no crashed service after leaving the app"
open_app; A input keyevent KEYCODE_HOME; sleep 2
A dumpsys activity recents | grep -q "realActivity=$PKG" && fail "Recents still has a Reel Block card" || pass "no Recents card"
crashed && fail "listed under crashed services" || pass "not in crashed services"
bound && pass "still bound after leaving the app" || fail "not bound after leaving the app"

echo "== Every screen in light, dark and font scale 2.0"
for v in light dark font2; do
  A cmd uimode night "$([ $v = dark ] && echo yes || echo no)" >/dev/null
  A settings put system font_scale "$([ $v = font2 ] && echo 2.0 || echo 1.0)"
  sleep 1
  open_app; shot "home-1-$v"; scroll_down; shot "home-2-$v"; scroll_down; shot "home-3-$v"; scroll_down; shot "home-4-$v"
  open_app; go "Block Reels" "pause-$v" && { shot "pause-dialog-$v"; go "For 15 minutes" "paused-$v" && { open_app; shot "home-paused-$v"; go "Block Reels" "resume-$v"; }; }
  open_app; go "What Reel Block can see" "disclosure-$v" && { shot "disclosure-1-$v"; scroll_down; shot "disclosure-2-$v"; scroll_down; shot "disclosure-3-$v"; }
  open_app; go "Setup help" "setup-$v" && { shot "setup-1-$v"; scroll_down; shot "setup-2-$v"; }
  service_off; open_app; shot "home-off-$v"; service_on
done
if [ -n "$FIRST_RUN" ]; then
  A pm clear $PKG >/dev/null
  for v in light dark font2; do
    A cmd uimode night "$([ $v = dark ] && echo yes || echo no)" >/dev/null
    A settings put system font_scale "$([ $v = font2 ] && echo 2.0 || echo 1.0)"
    A pm clear $PKG >/dev/null; service_off
    open_app; shot "first-run-1-$v"
    go "Get started" "first-run-2-$v" && { shot "first-run-2-$v"; go "Agree and continue" "first-run-3-$v" && shot "first-run-3-$v"; }
  done
  service_on
fi

echo
[ "$FAILED" = 0 ] && echo "All device checks passed. Files in $OUT/." || { echo "Some device checks failed. Files in $OUT/."; exit 1; }
