package dev.local.reelblock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

// Self-check: is Reel Block running, and does the installed Instagram still have the Reels screen it looks for?
// The Instagram check reads Instagram's resource table (the list of view names in its app package), never its
// screen and never your data. AndroidManifest.xml has a <queries> entry so this phone lets us see the package.
final class Check {
    static final int OK = 0, OFF = 1, STOPPED = 2, CHANGED = 3, NO_INSTAGRAM = 4;

    final int status;
    final String instagramVersion; // null when Instagram is not installed

    private Check(int status, String version) {
        this.status = status;
        this.instagramVersion = version;
    }

    // Loading Instagram's resource table takes a moment, so it is read once per Instagram install or update.
    private static long checkedUpdate = -1;
    private static boolean viewerFound;

    static Check run(Context c) {
        String version = null;
        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo p = pm.getPackageInfo(ReelBlockService.INSTAGRAM, 0);
            version = p.versionName;
            if (p.lastUpdateTime != checkedUpdate) {
                Resources r = pm.getResourcesForApplication(ReelBlockService.INSTAGRAM);
                viewerFound = r.getIdentifier(ReelBlockService.VIEWER_NAME, "id", ReelBlockService.INSTAGRAM) != 0;
                checkedUpdate = p.lastUpdateTime;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Not installed: nothing to block, and nothing is wrong with Reel Block.
            checkedUpdate = -1;
        }
        if (!State.enabled(c)) return new Check(OFF, version);
        if (!State.running(c)) return new Check(STOPPED, version);
        if (version == null) return new Check(NO_INSTAGRAM, null);
        return new Check(viewerFound ? OK : CHANGED, version);
    }
}
