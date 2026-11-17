package dev.local.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// Presses Back whenever an Instagram screen that the user chose to block is visible (Reels, Stories, comments,
// Explore). Can also show a pause screen when Instagram opens. Instagram only, by design:
// res/xml/accessibility.xml limits events to Instagram, and the checks below refuse any other window.
// The ID was read from the live app with `adb shell dumpsys activity top`.
public class ReelBlockService extends AccessibilityService {
    static final String INSTAGRAM = "com.instagram.android";
    static final String VIEWER_NAME = "clips_viewer_view_pager"; // Instagram Reels (453.0.0.57.79)
    // The parts a user can block. A part is on screen when ALL its view names are visible. Only "reels" was
    // read from a live phone. The other names come from other open-source projects (docs/research.md) and
    // the self-check tells the user when the installed Instagram does not have them.
    static final String[] KEYS = {"reels", "stories", "comments", "explore"};
    static final String[][] NAMES = {
        {VIEWER_NAME},
        {"reel_viewer_root"},               // in Instagram's code a "reel" is a story
        {"layout_comment_thread_edittext"}, // the text box of the comments sheet
        {"explore_action_bar", "grid_card_layout_container"}, // both: the grid, not a plain search
    };
    // ponytail: Android sends this service no event when the user leaves Instagram, so "Instagram was opened"
    // means "no Instagram event for this long". A shorter return shows no pause screen. Tune on a phone.
    static final long SESSION_GAP = 120_000;
    private long lastSeen;
    private View gate;
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { closeGate(gate); }
    };
    // True while Android has this service bound. Read by the self-check in the app screen (same process).
    static volatile boolean connected;
    private long lastBack;

    @Override
    protected void onServiceConnected() {
        connected = true;
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF)); // system broadcast, no permission

    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected = false;
        closeGate(gate);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        connected = false;
        try { unregisterReceiver(screenOff); } catch (RuntimeException e) { /* never registered */ }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // While an app closes, Android can still report its old window. Wait so one Back does not become two.
        if (!INSTAGRAM.equals(String.valueOf(event.getPackageName()))) return;
        long up = SystemClock.elapsedRealtime();
        boolean opened = up - lastSeen > SESSION_GAP;
        if (State.paused(this)) { lastSeen = up; return; }
        if (gate != null) return;
        // The active window can belong to another app when the event arrives. Never look inside it.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || !INSTAGRAM.equals(String.valueOf(root.getPackageName()))) return;
        lastSeen = up;
        // Only now is it sure that Instagram is in front: never cover another app or a call screen.
        if (opened && State.gate(this)) { showGate(); return; }
        if (System.currentTimeMillis() - lastBack < 1500) return;
        for (int i = 0; i < KEYS.length; i++) {
            if (!State.block(this, KEYS[i]) || !visible(root, NAMES[i])) continue;
            lastBack = System.currentTimeMillis();
            performGlobalAction(GLOBAL_ACTION_BACK);
            // Back first, bookkeeping after: the count stays on the phone (no internet permission, no backup, no logs).
            State.recordClose(this);
            return;
        }
    }

    private static boolean visible(AccessibilityNodeInfo root, String[] names) {
        for (String name : names) {
            boolean seen = false;
            for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByViewId(INSTAGRAM + ":id/" + name))
                if (n.isVisibleToUser()) { seen = true; break; }
            if (!seen) return false;
        }
        return true;
    }

    // The pause screen: a moment to decide, with a way out. The study behind it (Gruening et al., PNAS 2023)
    // found that the "do not open" button is the part that works. Drawn as an accessibility overlay, so it
    // needs no permission. It goes away on a choice, when the screen turns off, when another window takes focus, or after 15 s
    // (uptime), so it cannot stay over the lock screen or another app for long.
    private void showGate() {
        if (gate != null) return;
        View v = LayoutInflater.from(new ContextThemeWrapper(this, R.style.Theme_ReelBlock)).inflate(R.layout.gate, null);
        v.findViewById(R.id.gate_leave).setOnClickListener(x -> { closeGate(v); performGlobalAction(GLOBAL_ACTION_HOME); });
        View go = v.findViewById(R.id.gate_continue);
        go.setEnabled(false);
        go.postDelayed(() -> go.setEnabled(true), 5000);
        go.setOnClickListener(x -> closeGate(v));
        v.postDelayed(() -> closeGate(v), 15_000);
        v.getViewTreeObserver().addOnWindowFocusChangeListener(focus -> { if (!focus) v.post(() -> closeGate(v)); });
        try {
            getSystemService(WindowManager.class).addView(v, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, PixelFormat.OPAQUE));
            gate = v;
        } catch (RuntimeException e) {
            // No window right now (for example the service is being unbound): Instagram opens as usual.
        }
    }

    private void closeGate(View v) {
        if (v == null || gate != v) return;
        gate = null;
        try { getSystemService(WindowManager.class).removeView(v); } catch (RuntimeException e) { /* already gone */ }
    }

    @Override
    public void onInterrupt() {}
}
