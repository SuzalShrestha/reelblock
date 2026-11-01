package dev.local.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// Presses Back whenever Instagram shows the full-screen Reels viewer ("clips" internally).
// IDs come from aapt2 dump of Instagram 453.0.0.57.79; re-check them if Instagram renames.
public class ReelBlockService extends AccessibilityService {
    private static final String REELS_VIEWER = "com.instagram.android:id/clips_viewer_view_pager";
    private long lastBack;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByViewId(REELS_VIEWER)) {
            if (n.isVisibleToUser() && System.currentTimeMillis() - lastBack > 600) {
                lastBack = System.currentTimeMillis();
                android.util.Log.i("ReelBlock", "Reels viewer visible, pressing Back");
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
