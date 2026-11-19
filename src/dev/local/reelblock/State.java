package dev.local.reelblock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

// Everything Reel Block stores, in two private preference files on this phone. Backup and device transfer
// copy none of it (res/xml/data_rules.xml). Nothing here touches the network: the app has no INTERNET permission.
//   settings: paused (until I turn it on), paused_until (timed pause end, wall clock), consent (disclosure agreed),
//             seen (first run shown; "Not now" does not bring it back), pro (Google Play says Pro is bought),
//             sched, sched_start, sched_end, sched_days (Pro schedule)
//   stats:    blocked (all-time count), d<day> (count per local day: 30 days, or 366 with Pro). No times of day.
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

    // What the service asks on each Instagram event: not paused, and (Pro) inside the schedule.
    static boolean blocking(Context c) {
        return !paused(c) && !outsideSchedule(c);
    }

    // Pro schedule is on and now is outside it: Reels are allowed until the next window.
    static boolean outsideSchedule(Context c) {
        if (!scheduleOn(c)) return false;
        Calendar now = Calendar.getInstance();
        SharedPreferences p = settings(c);
        return !inWindow(p.getInt("sched_start", 9 * 60), p.getInt("sched_end", 17 * 60), scheduleDays(c),
            now.get(Calendar.DAY_OF_WEEK) - 1, now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE));
    }

    // Pure, for tools/ScheduleTest.java. days: bit 0 = Sunday ... bit 6 = Saturday. dow: 0 = Sunday.
    // start == end: the whole day. start > end: overnight; the part after midnight belongs to the day before.
    static boolean inWindow(int start, int end, int days, int dow, int minute) {
        boolean today = (days & (1 << dow)) != 0, yesterday = (days & (1 << ((dow + 6) % 7))) != 0;
        if (start == end) return today;
        if (start < end) return today && minute >= start && minute < end;
        return (today && minute >= start) || (yesterday && minute < end);
    }

    static boolean pro(Context c) {
        return settings(c).getBoolean("pro", false);
    }

    static void setPro(Context c, boolean on) {
        settings(c).edit().putBoolean("pro", on).apply();
    }

    static boolean scheduleOn(Context c) {
        return pro(c) && settings(c).getBoolean("sched", false);
    }

    static int scheduleStart(Context c) {
        return settings(c).getInt("sched_start", 9 * 60);
    }

    static int scheduleEnd(Context c) {
        return settings(c).getInt("sched_end", 17 * 60);
    }

    static int scheduleDays(Context c) {
        return settings(c).getInt("sched_days", 0b0111110); // Monday to Friday
    }

    static void setSchedule(Context c, boolean on, int start, int end, int days) {
        settings(c).edit().putBoolean("sched", on).putInt("sched_start", start).putInt("sched_end", end)
            .putInt("sched_days", days).apply();
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
        settings(c).edit().putBoolean("consent", true).putBoolean("seen", true).apply();
    }

    static boolean seen(Context c) {
        return settings(c).getBoolean("seen", false);
    }

    static void setSeen(Context c) {
        settings(c).edit().putBoolean("seen", true).apply();
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
        boolean newDay = !p.contains(key);
        p.edit().putInt("blocked", p.getInt("blocked", 0) + 1).putInt(key, p.getInt(key, 0) + 1).apply();
        if (newDay) prune(c); // once a day: keep only 30 days (Pro: 366), even if the app is never opened
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

    // Pro: counts for the last 12 months, oldest first; the last entry is this month.
    static int[] months(Context c) {
        Calendar now = Calendar.getInstance(), d = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int thisMonth = now.get(Calendar.YEAR) * 12 + now.get(Calendar.MONTH);
        int[] m = new int[12];
        for (Map.Entry<String, ?> en : stats(c).getAll().entrySet()) {
            String k = en.getKey();
            if (!k.matches("d\\d+") || !(en.getValue() instanceof Integer)) continue;
            d.setTimeInMillis(Long.parseLong(k.substring(1)) * 86_400_000L); // a local day number, read as UTC
            int i = 11 - (thisMonth - (d.get(Calendar.YEAR) * 12 + d.get(Calendar.MONTH)));
            if (i >= 0 && i < 12) m[i] += (Integer) en.getValue();
        }
        return m;
    }

    // Keep 30 days of per-day counts (Pro: 366, for the 12-month chart), drop the rest.
    static void prune(Context c) {
        SharedPreferences p = stats(c);
        long oldest = day(System.currentTimeMillis()) - (pro(c) ? 365 : 29); // today and the days before it
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
