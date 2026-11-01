---
name: Block another app
about: Ask Reel Block to close the short-video feed in another app
labels: new-app
---

**App and package name:** (for example Facebook, `com.facebook.katana`)
**Where the short-video feed is:** (tab name, button)

If you have `adb`, open the feed and paste the view IDs that look like the full-screen viewer:

```sh
adb shell dumpsys activity top | grep -E 'app:id/' | grep -iE 'reel|short|clip|video|pager'
```
