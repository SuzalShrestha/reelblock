package dev.local.reelblock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

// Self-check: is Reel Block running, and does the installed Instagram still have the Reels screen it looks for?
// The Instagram check reads Instagram's resource table (the list of view names in its app package), never its
// screen and never your data. AndroidManifest.xml has a <queries> entry so this phone lets us see the package.
// Why the name is a good signal: the service finds the Reels viewer by this same name, so if Instagram's
// resource table no longer has it, the service cannot find the viewer either.
final class Check {
    static final int OK = 0, OFF = 1, STOPPED = 2, CHANGED = 3, NO_INSTAGRAM = 4, UNKNOWN = 5;

    final int status;
    final String instagramVersion; // null when Instagram is not installed

    private Check(int status, String version) {
        this.status = status;
        this.instagramVersion = version;
    }

    // Loading Instagram's resource table takes a moment, so it is read once per Instagram install or update.
    private static long checkedUpdate = -1;
    private static Boolean viewerFound; // null: could not read it (for example while Instagram updates)

    static Check run(Context c) {
        String version = null;
        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo p = pm.getPackageInfo(ReelBlockService.INSTAGRAM, 0);
            version = p.versionName;
            if (p.lastUpdateTime != checkedUpdate) {
                checkedUpdate = p.lastUpdateTime;
                viewerFound = null;
                viewerFound = pm.getResourcesForApplication(ReelBlockService.INSTAGRAM)
                    .getIdentifier(ReelBlockService.VIEWER_NAME, "id", ReelBlockService.INSTAGRAM) != 0;
            }
        } catch (PackageManager.NameNotFoundException e) {
            checkedUpdate = -1; // Not installed (or being replaced): look again next time.
        } catch (RuntimeException e) {
            checkedUpdate = -1; // Instagram's package could not be read right now. Say so, do not guess.
        }
        if (!State.enabled(c)) return new Check(OFF, version);
        if (!State.running(c)) return new Check(STOPPED, version);
        if (version == null) return new Check(NO_INSTAGRAM, null);
        if (viewerFound == null) return new Check(UNKNOWN, version);
        return new Check(viewerFound ? OK : CHANGED, version);
    }
}
