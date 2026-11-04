package dev.local.reelblock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import java.util.Map;
import java.util.TimeZone;

// Everything Reel Block stores, in two private preference files on this phone. Backup and device transfer
// copy none of it (res/xml/data_rules.xml). Nothing here touches the network: the app has no INTERNET permission.
//   settings: paused (until I turn it on), paused_until (timed pause end, wall clock), consent (disclosure agreed)
//   stats:    blocked (all-time count), d<day> (count per local day, last 30 days), last (time of the last close)
final class State {
    private State() {}

    static SharedPreferences settings(Context c) {
        return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static SharedPreferences stats(Context c) {
        return c.getSharedPreferences("stats", Context.MODE_PRIVATE);
    }

    // Read by ReelBlockService on each Instagram event. Same process, so a change applies at once.
    // Wall clock, not elapsedRealtime: a timed pause must also end after a restart.
    static boolean paused(Context c) {
        SharedPreferences p = settings(c);
        return p.getBoolean("paused", false) || System.currentTimeMillis() < p.getLong("paused_until", 0);
    }

    // The end of a timed pause, or 0 when blocking is on or paused until the user turns it on.
    static long pausedUntil(Context c) {
        SharedPreferences p = settings(c);
        long until = p.getLong("paused_until", 0);
        return !p.getBoolean("paused", false) && until > System.currentTimeMillis() ? until : 0;
    }

    // minutes <= 0: until the user turns blocking on again.
    static void pause(Context c, int minutes) {
        SharedPreferences.Editor e = settings(c).edit().remove("paused").remove("paused_until");
        if (minutes <= 0) e.putBoolean("paused", true);
        else e.putLong("paused_until", System.currentTimeMillis() + minutes * 60_000L);
        e.apply();
    }

    static void resume(Context c) {
        settings(c).edit().remove("paused").remove("paused_until").apply();
    }

    static boolean consent(Context c) {
        return settings(c).getBoolean("consent", false);
    }

    static void setConsent(Context c) {
        settings(c).edit().putBoolean("consent", true).apply();
    }

    // Turned on in Android's Accessibility settings. Match whole entries ("pkg/service", split by ':');
    // contains() would also match "x.dev.local.reelblock/...".
    static boolean enabled(Context c) {
        String on = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (on != null) for (String s : on.split(":")) if (s.startsWith(c.getPackageName() + "/")) return true;
        return false;
    }

    // Actually running. Android lists only bound services here, so on Xiaomi a service that shows "On" in
    // Settings but was stopped ("Not working") is missing. The static flag covers the moment right after binding.
    static boolean running(Context c) {
        if (ReelBlockService.connected) return true;
        AccessibilityManager am = c.getSystemService(AccessibilityManager.class);
        if (am == null) return false;
        for (AccessibilityServiceInfo i : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            if (i.getResolveInfo() != null && c.getPackageName().equals(i.getResolveInfo().serviceInfo.packageName)) return true;
        return false;
    }

    // Days are local calendar days, counted from 1970-01-01.
    static long day(long millis) {
        return (millis + TimeZone.getDefault().getOffset(millis)) / 86_400_000L;
    }

    // Called by the service after each Back press. One small write, off the main thread (apply()).
    static void recordClose(Context c) {
        SharedPreferences p = stats(c);
        long now = System.currentTimeMillis();
        String key = "d" + day(now);
        p.edit().putInt("blocked", p.getInt("blocked", 0) + 1).putInt(key, p.getInt(key, 0) + 1)
            .putLong("last", now).apply();
    }

    static int total(Context c) {
        return stats(c).getInt("blocked", 0);
    }

    // Counts for the last 7 days, oldest first; the last entry is today.
    static int[] week(Context c) {
        SharedPreferences p = stats(c);
        long today = day(System.currentTimeMillis());
        int[] w = new int[7];
        for (int i = 0; i < 7; i++) w[i] = p.getInt("d" + (today - 6 + i), 0);
        return w;
    }

    // Keep 30 days of per-day counts, drop the rest.
    static void prune(Context c) {
        SharedPreferences p = stats(c);
        long oldest = day(System.currentTimeMillis()) - 30;
        SharedPreferences.Editor e = null;
        for (Map.Entry<String, ?> en : p.getAll().entrySet()) {
            String k = en.getKey();
            if (!k.matches("d\\d+") || Long.parseLong(k.substring(1)) >= oldest) continue;
            if (e == null) e = p.edit();
            e.remove(k);
        }
        if (e != null) e.apply();
    }
}
