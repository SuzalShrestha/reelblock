package dev.local.reelblock;

import android.content.Context;
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

    static Check run(Context c) {
        String version = null;
        boolean viewerFound = false;
        PackageManager pm = c.getPackageManager();
        try {
            version = pm.getPackageInfo(ReelBlockService.INSTAGRAM, 0).versionName;
            Resources r = pm.getResourcesForApplication(ReelBlockService.INSTAGRAM);
            viewerFound = r.getIdentifier(ReelBlockService.VIEWER_NAME, "id", ReelBlockService.INSTAGRAM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            // Not installed: nothing to block, and nothing is wrong with Reel Block.
        }
        if (!State.enabled(c)) return new Check(OFF, version);
        if (!State.running(c)) return new Check(STOPPED, version);
        if (version == null) return new Check(NO_INSTAGRAM, null);
        return new Check(viewerFound ? OK : CHANGED, version);
    }
}
