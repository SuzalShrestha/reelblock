---
name: Reels came back
about: Reel Block stopped closing Instagram Reels, maybe after an app update
labels: id-changed
---

**Instagram version:** (Settings → Apps → Instagram → version at the bottom)
**Phone and Android version:**
**Where you opened it:** Reels tab / DM / Explore / profile / link

**Is the service on?** Open Reel Block. Does it say "On"?

If you have a computer with `adb`, open the viewer with Reel Block turned off and paste the output of:

```sh
adb shell dumpsys activity top | grep clips_viewer
```
