package dev.local.reelblock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

// Self-check: is Reel Block running, and does the installed Instagram still have the Reels screen it looks for?
// The Instagram check reads Instagram's resource table (the list of view names in its app package), never its
// screen and never your data. AndroidManifest.xml has a <queries> entry so this phone lets us see the package.
// Why the name is a good signal: the service finds the Reels viewer by this same name, so if Instagram's
// resource table no longer has it, the service cannot find the viewer either.
final class Check {
    static final int OK = 0, OFF = 1, STOPPED = 2, CHANGED = 3, NO_INSTAGRAM = 4, UNKNOWN = 5, CHECKING = 6;

    final int status;
    final String instagramVersion; // null when Instagram is not installed

    private Check(int status, String version) {
        this.status = status;
        this.instagramVersion = version;
    }

    // Reading Instagram's resource table takes a moment, so it runs once per Instagram install or update,
    // off the main thread. Until it is done the status is CHECKING.
    private static volatile long checkedUpdate = -1;
    private static volatile Boolean viewerFound; // null: could not read it (for example while Instagram updates)
    private static volatile boolean loading, recheck;
    // Who to tell when a read finishes: the screen that asked last (a rotated screen replaces the old one).
    private static volatile Runnable listener;

    // Read again (the self-check row). Until the new read is done, the last result stays on screen.
    static void forget() {
        recheck = true;
    }

    // A screen that goes away stops listening, so a finished read never holds on to it.
    static void unlisten(Runnable done) {
        if (listener == done) listener = null;
    }

    static boolean reading() {
        return loading;
    }

    // `done` runs on the main thread when a background read finishes.
    static Check run(Context c, Runnable done) {
        listener = done;
        String version = null;
        long update = -1;
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(ReelBlockService.INSTAGRAM, 0);
            version = p.versionName;
            update = p.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            // Not installed: nothing to block, and nothing is wrong with Reel Block.
        }
        if (version != null && (update != checkedUpdate || recheck) && !loading) load(c.getApplicationContext(), update);
        if (!State.enabled(c)) return new Check(OFF, version);
        if (!State.running(c)) return new Check(STOPPED, version);
        if (version == null) return new Check(NO_INSTAGRAM, null);
        if (update != checkedUpdate) return new Check(CHECKING, version);
        if (viewerFound == null) return new Check(UNKNOWN, version);
        return new Check(viewerFound ? OK : CHANGED, version);
    }

    private static void load(Context app, long update) {
        loading = true;
        recheck = false;
        new Thread(() -> {
            Boolean found;
            try {
                found = app.getPackageManager().getResourcesForApplication(ReelBlockService.INSTAGRAM)
                    .getIdentifier(ReelBlockService.VIEWER_NAME, "id", ReelBlockService.INSTAGRAM) != 0;
            } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                found = null; // Instagram's package could not be read right now. Say so, do not guess.
            }
            viewerFound = found;
            checkedUpdate = update;
            loading = false;
            Runnable l = listener;
            if (l != null) new Handler(Looper.getMainLooper()).post(l);
        }, "reelblock-check").start();
    }
}
