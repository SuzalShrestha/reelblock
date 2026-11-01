---
name: Reels or Shorts came back
about: Reel Block stopped closing Reels or Shorts, maybe after an app update
labels: id-changed
---

**App:** Instagram / YouTube
**App version:** (Settings → Apps → Instagram → version at the bottom)
**Phone and Android version:**
**Where you opened it:** Reels tab / Shorts tab / DM / Explore / profile / link

**Is the service on?** Open Reel Block. Does it say "On"?

If you have a computer with `adb`, open the viewer with Reel Block turned off and paste the output of:

```sh
adb shell dumpsys activity top | grep -E 'clips_viewer|reel_recycler'
```
