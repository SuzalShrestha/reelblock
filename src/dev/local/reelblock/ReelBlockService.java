package dev.local.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// Presses Back whenever a full-screen short-video viewer is visible.
// Each ID was read from the live app with `adb shell dumpsys activity top`. Add a line to block another app,
// and add its package to res/xml/accessibility.xml.
public class ReelBlockService extends AccessibilityService {
    static final String[] VIEWERS = {
        "com.instagram.android:id/clips_viewer_view_pager", // Instagram Reels (453.0.0.57.79)
        "com.google.android.youtube:id/reel_recycler",      // YouTube Shorts
    };
    private long lastBack;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        // While an app closes, Android can still report its old window. Wait so one Back does not become two.
        if (root == null || System.currentTimeMillis() - lastBack < 1500) return;
        String pkg = root.getPackageName() + ":";
        for (String id : VIEWERS) {
            if (!id.startsWith(pkg)) continue;
            for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByViewId(id)) {
                if (!n.isVisibleToUser()) continue;
                lastBack = System.currentTimeMillis();
                android.util.Log.i("ReelBlock", "Viewer visible, pressing Back: " + id);
                performGlobalAction(GLOBAL_ACTION_BACK);
                SharedPreferences p = getSharedPreferences("stats", MODE_PRIVATE);
                p.edit().putInt("blocked", p.getInt("blocked", 0) + 1).apply();
                return;
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
