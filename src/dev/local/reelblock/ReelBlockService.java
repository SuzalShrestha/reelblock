package dev.local.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// Presses Back whenever the full-screen Instagram Reels viewer is visible. Instagram only, by design:
// res/xml/accessibility.xml limits events to Instagram, and the checks below refuse any other window.
// The ID was read from the live app with `adb shell dumpsys activity top`.
public class ReelBlockService extends AccessibilityService {
    static final String INSTAGRAM = "com.instagram.android";
    static final String VIEWER_NAME = "clips_viewer_view_pager"; // Instagram Reels (453.0.0.57.79)
    static final String VIEWER = INSTAGRAM + ":id/" + VIEWER_NAME;
    // True while Android has this service bound. Read by the self-check in the app screen (same process).
    static volatile boolean connected;
    private long lastBack;

    @Override
    protected void onServiceConnected() {
        connected = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        connected = false;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // While an app closes, Android can still report its old window. Wait so one Back does not become two.
        if (!INSTAGRAM.equals(String.valueOf(event.getPackageName()))
            || System.currentTimeMillis() - lastBack < 1500 || State.paused(this)) return;
        // The active window can belong to another app when the event arrives. Never look inside it.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || !INSTAGRAM.equals(String.valueOf(root.getPackageName()))) return;
        for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByViewId(VIEWER)) {
            if (!n.isVisibleToUser()) continue;
            lastBack = System.currentTimeMillis();
            performGlobalAction(GLOBAL_ACTION_BACK);
            // Back first, bookkeeping after: the count stays on the phone (no internet permission, no backup, no logs).
            State.recordClose(this);
            return;
        }
    }

    @Override
    public void onInterrupt() {}
}
