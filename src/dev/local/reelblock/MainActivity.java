package dev.local.reelblock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.StatusBarManager;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;

// The app screen: first-run onboarding (Welcome.java), then home: one status with one action, this week's
// count, the self-check, shortcuts, and the privacy promises. Layouts: res/layout/{main,welcome}.xml.
public class MainActivity extends Activity {
    static final String REPO = "https://github.com/SuzalShrestha/reelblock";
    static final String EXTRA_PAUSE = "pause"; // from the Quick Settings tile: ask "pause for how long?"
    private enum Hero { ON, PAUSED, SCHEDULE, OFF, STOPPED, CHANGED }

    private Welcome welcome;
    private Pro pro;
    private Hero shown;
    private boolean firstRender;
    private long graceUntil;
    private Dialog dialog;
    private boolean announceCheck;
    private final Runnable rerender = this::render;
    private final Runnable checkDone = () -> { if (!isDestroyed()) render(); };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // After a rotation or a theme change, stay on the same onboarding page.
        if (b != null && b.containsKey("page")) welcome = new Welcome(this, b.getInt("page"), b.getBoolean("fromHome"));
        // First run only: explain before asking. "Not now" (seen) and an already enabled service skip it.
        else if (!State.seen(this) && !State.consent(this) && !State.enabled(this)) welcome = new Welcome(this, 0, false);
        else showHome(b == null && wantsPause(getIntent()));
        // Android 13+: Back goes through this callback (targetSdk 36 no longer calls onBackPressed).
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
    }

    // The tile can open the app while it is already on screen.
    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (wantsPause(i)) showHome(true);
    }

    private static boolean wantsPause(Intent i) {
        boolean ask = i.getBooleanExtra(EXTRA_PAUSE, false);
        i.removeExtra(EXTRA_PAUSE); // once: a rotation must not ask again
        return ask;
    }

    void showHome(boolean askPause) {
        welcome = null;
        setContentView(R.layout.main);
        View scroll = findViewById(R.id.scroll);
        Ui.edgeToEdge(this, scroll, scroll);
        firstRender = true;
        shown = null;
        // Right after the app process starts, Android may still be binding the service. Do not call it
        // "stopped" during the first second: show no status rather than a wrong one.
        graceUntil = SystemClock.uptimeMillis() + 1000; // same clock as postDelayed, immune to clock changes
        ((Switch) findViewById(R.id.block_switch)).setOnCheckedChangeListener((v, on) -> {
            if (on == !State.paused(this)) return;
            if (on) { State.resume(this); render(); }
            else askPause();
        });
        findViewById(R.id.check_row).setOnClickListener(v -> {
            Check.forget();
            announceCheck = true; // said when the new result is on screen
            render();
        });
        findViewById(R.id.tile_row).setOnClickListener(v -> addTile());
        if (pro == null) pro = new Pro(this, this::render);
        findViewById(R.id.buy_row).setOnClickListener(v -> {
            if (!pro.buy()) info(R.string.buy_unavailable_title, R.string.buy_unavailable_body);
        });
        findViewById(R.id.schedule_row).setOnClickListener(v -> editSchedule());
        findViewById(R.id.help_row).setOnClickListener(v -> welcome(Welcome.SETUP));
        findViewById(R.id.see_row).setOnClickListener(v -> welcome(Welcome.DISCLOSURE));
        findViewById(R.id.code_row).setOnClickListener(v -> Ui.start(this, Ui.web(REPO)));
        findViewById(R.id.share_row).setOnClickListener(v -> Ui.start(this, Intent.createChooser(
            new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText()),
            getString(R.string.share))));
        // Xiaomi only: without Autostart, the phone does not restart the service after it stops the app.
        // setPackage: only Xiaomi's own app may answer, never another app that copies the action.
        Intent autostart = new Intent("miui.intent.action.OP_AUTO_START").setPackage("com.miui.securitycenter");
        if (autostart.resolveActivity(getPackageManager()) != null) {
            findViewById(R.id.autostart_row).setVisibility(View.VISIBLE);
            findViewById(R.id.autostart_row).setOnClickListener(v -> Ui.start(this, autostart));
        }
        try {
            ((TextView) findViewById(R.id.version)).setText(getString(R.string.version,
                getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception e) { findViewById(R.id.version).setVisibility(View.GONE); }
        State.prune(this);
        render();
        if (askPause && State.enabled(this) && !State.paused(this)) askPause();
    }

    // Turn on, Setup help, What it can see. Without consent, everything goes through the disclosure first.
    private void welcome(int page) {
        welcome = new Welcome(this, State.consent(this) ? page : Welcome.DISCLOSURE, true);
    }

    // A timed pause ends by itself, so a pause you forget does not turn the blocker off for good.
    // Back, a tap outside or Cancel keep blocking on.
    private void askPause() {
        if (dialog != null) dialog.dismiss();
        Dialog d = dialog = Ui.dialog(this, R.layout.dialog_pause);
        int[] ids = {R.id.pause_15, R.id.pause_60, R.id.pause_forever};
        int[] minutes = {15, 60, 0};
        for (int i = 0; i < ids.length; i++) {
            int m = minutes[i];
            d.findViewById(ids[i]).setOnClickListener(v -> { State.pause(this, m); d.dismiss(); });
        }
        d.findViewById(R.id.dialog_cancel).setOnClickListener(v -> d.cancel());
        d.setOnDismissListener(x -> render()); // No choice: the switch goes back on.
        d.show();
    }

    // Pro. Times and days use Android's own pickers.
    private void editSchedule() {
        if (dialog != null) dialog.dismiss();
        Dialog d = dialog = Ui.dialog(this, R.layout.dialog_schedule);
        int[] v = {State.scheduleStart(this), State.scheduleEnd(this), State.scheduleDays(this)};
        Switch on = d.findViewById(R.id.schedule_switch);
        on.setChecked(State.scheduleOn(this));
        TextView start = d.findViewById(R.id.schedule_start), end = d.findViewById(R.id.schedule_end),
            days = d.findViewById(R.id.schedule_days);
        Runnable show = () -> {
            start.setText(getString(R.string.schedule_from, time(v[0])));
            end.setText(getString(R.string.schedule_to, time(v[1])));
            days.setText(dayNames(v[2]));
        };
        show.run();
        start.setOnClickListener(x -> pickTime(v, 0, show));
        end.setOnClickListener(x -> pickTime(v, 1, show));
        days.setOnClickListener(x -> pickDays(v, show));
        d.findViewById(R.id.dialog_cancel).setOnClickListener(x -> d.cancel());
        d.findViewById(R.id.dialog_ok).setOnClickListener(x -> {
            State.setSchedule(this, on.isChecked(), v[0], v[1], v[2]);
            d.dismiss();
        });
        d.setOnDismissListener(x -> render());
        d.show();
    }

    private void pickTime(int[] v, int i, Runnable show) {
        new TimePickerDialog(this, (p, h, m) -> { v[i] = h * 60 + m; show.run(); }, v[i] / 60, v[i] % 60,
            DateFormat.is24HourFormat(this)).show();
    }

    // Days in the order of this phone's week (Monday first in most of Europe, Sunday first in the US).
    private void pickDays(int[] v, Runnable show) {
        int first = Calendar.getInstance().getFirstDayOfWeek() - 1;
        String[] names = new android.icu.text.DateFormatSymbols().getWeekdays();
        String[] items = new String[7];
        boolean[] checked = new boolean[7];
        int[] mask = {v[2]};
        for (int i = 0; i < 7; i++) {
            int dow = (first + i) % 7;
            items[i] = names[dow + 1];
            checked[i] = (mask[0] & (1 << dow)) != 0;
        }
        new AlertDialog.Builder(this).setTitle(R.string.schedule_days_title)
            .setMultiChoiceItems(items, checked, (x, i, c) -> mask[0] ^= 1 << ((first + i) % 7))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, (x, w) -> { v[2] = mask[0]; show.run(); }).show();
    }

    private String time(int minutes) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, minutes / 60);
        c.set(Calendar.MINUTE, minutes % 60);
        return DateFormat.getTimeFormat(this).format(c.getTime());
    }

    private String dayNames(int mask) {
        if (mask == 0b1111111) return getString(R.string.every_day);
        if (mask == 0) return getString(R.string.no_days);
        int first = Calendar.getInstance().getFirstDayOfWeek() - 1;
        String[] names = new android.icu.text.DateFormatSymbols().getShortWeekdays();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            int dow = (first + i) % 7;
            if ((mask & (1 << dow)) == 0) continue;
            if (b.length() > 0) b.append(", ");
            b.append(names[dow + 1]);
        }
        return b.toString();
    }

    private String scheduleText() {
        int start = State.scheduleStart(this), end = State.scheduleEnd(this);
        String days = dayNames(State.scheduleDays(this));
        return start == end ? getString(R.string.schedule_all_day, days)
            : getString(R.string.schedule_summary, time(start), time(end), days);
    }

    private void addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            StatusBarManager sbm = getSystemService(StatusBarManager.class);
            if (sbm != null) {
                sbm.requestAddTileService(new ComponentName(this, QsTile.class), getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile_brand), getMainExecutor(), r -> {
                        if (isFinishing()) return;
                        if (r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) info(R.string.tile_row_title, R.string.tile_already);
                        // Errors (1000+: no status bar service, request in progress...): explain how to add it by hand.
                        else if (r >= 1000) info(R.string.tile_how_title, R.string.tile_how_body);
                    });
                return;
            }
        }
        info(R.string.tile_how_title, R.string.tile_how_body);
    }

    private void info(int title, int body) {
        if (dialog != null) dialog.dismiss();
        dialog = Ui.info(this, title, body);
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if (welcome != null) {
            b.putInt("page", welcome.page());
            b.putBoolean("fromHome", welcome.fromHome);
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) dialog.dismiss();
        if (pro != null) pro.end();
        Check.unlisten(checkDone);
        super.onDestroy();
    }

    // Android 12 and lower. Android 13+ uses the callback registered in onCreate.
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (welcome == null || !welcome.back()) finish();
    }

    // Leave no Recents card. On Xiaomi/HyperOS, swiping the card away kills every process of the app
    // (SwipeUpClean), the blocker too, and Android then shows it as "Not working" until it is turned on again.
    @Override
    protected void onStop() {
        super.onStop();
        // Only our own task: if another app started us inside its task (no NEW_TASK), leave that task alone.
        // Not while Google Play's purchase screen is on top: the purchase result comes back to this screen.
        if (!isChangingConfigurations() && (pro == null || !pro.buying)) { if (isTaskRoot()) finishAndRemoveTask(); else finish(); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (welcome == null) render();
    }

    private void render() {
        View heroView = findViewById(R.id.hero);
        if (heroView == null) return;
        heroView.removeCallbacks(rerender);
        Check check = Check.run(this, checkDone);
        boolean paused = State.paused(this);
        long until = State.pausedUntil(this);
        Hero h = check.status == Check.OFF ? Hero.OFF
            : check.status == Check.STOPPED ? Hero.STOPPED
            : paused ? Hero.PAUSED
            : check.status == Check.CHANGED ? Hero.CHANGED
            : State.outsideSchedule(this) ? Hero.SCHEDULE : Hero.ON;
        long wait = graceUntil - SystemClock.uptimeMillis();
        if (h == Hero.STOPPED && wait > 0) {
            // Say nothing yet (no red hero, no "Not running" line): look again when the grace ends.
            heroView.setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.check_body)).setText(null);
            heroView.postDelayed(rerender, wait);
            renderWeek();
            firstRender = false;
            return;
        }

        TextView title = findViewById(R.id.hero_title), body = findViewById(R.id.hero_body);
        ImageView badge = findViewById(R.id.hero_badge);
        int bg, fg, badgeBg, badgeFg, icon;
        switch (h) {
            case ON: bg = R.color.hero_on_bg; fg = R.color.hero_on_fg; badgeBg = R.color.hero_on_badge;
                badgeFg = R.color.hero_on_badge_icon; icon = R.drawable.ic_status_on;
                title.setText(R.string.hero_on_title);
                body.setText(check.status == Check.NO_INSTAGRAM ? R.string.hero_on_no_instagram : R.string.hero_on_body); break;
            case PAUSED: bg = R.color.hero_paused_bg; fg = R.color.hero_paused_fg; badgeBg = R.color.hero_paused_badge;
                badgeFg = R.color.hero_paused_badge_icon; icon = R.drawable.ic_status_paused;
                title.setText(R.string.hero_paused_title);
                body.setText(until > 0 ? getString(R.string.hero_paused_until,
                    DateFormat.getTimeFormat(this).format(new Date(until))) : getString(R.string.hero_paused_body));
                // Show "on" again when the pause ends while this screen is open.
                if (until > 0) heroView.postDelayed(rerender, until - System.currentTimeMillis() + 500);
                break;
            case SCHEDULE: bg = R.color.hero_paused_bg; fg = R.color.hero_paused_fg; badgeBg = R.color.hero_paused_badge;
                badgeFg = R.color.hero_paused_badge_icon; icon = R.drawable.ic_schedule;
                title.setText(R.string.hero_schedule_title);
                body.setText(getString(R.string.hero_schedule_body, scheduleText()));
                // ponytail: no timer for the schedule edge; the hero updates on the next onResume.
                break;
            case OFF: bg = R.color.hero_off_bg; fg = R.color.hero_off_fg; badgeBg = R.color.hero_off_badge;
                badgeFg = R.color.hero_off_badge_icon; icon = R.drawable.ic_status_off;
                title.setText(R.string.hero_off_title); body.setText(R.string.hero_off_body); break;
            case STOPPED: bg = R.color.hero_problem_bg; fg = R.color.hero_problem_fg; badgeBg = R.color.hero_problem_badge;
                badgeFg = R.color.hero_problem_badge_icon; icon = R.drawable.ic_status_problem;
                title.setText(R.string.hero_stopped_title); body.setText(R.string.hero_stopped_body); break;
            default: bg = R.color.hero_problem_bg; fg = R.color.hero_problem_fg; badgeBg = R.color.hero_problem_badge;
                badgeFg = R.color.hero_problem_badge_icon; icon = R.drawable.ic_status_problem;
                title.setText(R.string.hero_changed_title);
                body.setText(getString(R.string.hero_changed_body, check.instagramVersion));
        }
        Ui.tint(heroView, getColor(bg));
        if (heroView.getVisibility() != View.VISIBLE) Ui.enter(heroView, 0, getResources().getDimension(R.dimen.space_4));
        title.setTextColor(getColor(fg));
        body.setTextColor(getColor(fg));
        Button action = findViewById(R.id.hero_action), action2 = findViewById(R.id.hero_action2);
        action2.setTextColor(getColor(fg));
        badge.setImageResource(icon);
        badge.setBackgroundTintList(getColorStateList(badgeBg));
        badge.setImageTintList(getColorStateList(badgeFg));
        if (shown != null && shown != h) Ui.pop(badge);
        shown = h;

        // One main action per state.
        Switch sw = findViewById(R.id.block_switch);
        sw.setVisibility(h == Hero.ON || h == Hero.PAUSED || h == Hero.SCHEDULE ? View.VISIBLE : View.GONE);
        sw.setChecked(!paused);
        action.setVisibility(h == Hero.OFF || h == Hero.STOPPED || h == Hero.CHANGED ? View.VISIBLE : View.GONE);
        action2.setVisibility(h == Hero.CHANGED ? View.VISIBLE : View.GONE);
        if (h == Hero.OFF) { action.setText(R.string.turn_on); action.setOnClickListener(v -> welcome(Welcome.SETUP)); }
        if (h == Hero.STOPPED) {
            action.setText(R.string.fix_stopped);
            action.setOnClickListener(v -> Ui.start(this, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }
        if (h == Hero.CHANGED) {
            action.setText(R.string.get_update);
            // Play policy: an app from Google Play updates only through Google Play.
            action.setOnClickListener(v -> Ui.start(this, Ui.web(fromPlay()
                ? "https://play.google.com/store/apps/details?id=" + getPackageName() : REPO + "/releases/latest")));
            action2.setText(R.string.report);
            action2.setOnClickListener(v -> Ui.start(this, Ui.web(REPO + "/issues/new?template=id-changed.md")));
        }

        renderWeek();
        renderPro();
        TextView checkBody = findViewById(R.id.check_body);
        String v = check.instagramVersion;
        checkBody.setText(check.status == Check.OK ? getString(R.string.check_ok, v)
            : check.status == Check.OFF ? getString(R.string.check_off)
            : check.status == Check.STOPPED ? getString(R.string.check_stopped)
            : check.status == Check.CHANGED ? getString(R.string.check_changed, v)
            : check.status == Check.UNKNOWN ? getString(R.string.check_unknown, v)
            : check.status == Check.CHECKING ? getString(R.string.check_checking, v)
            : getString(R.string.check_no_instagram));
        if (announceCheck && !check.reading) {
            announceCheck = false;
            checkBody.announceForAccessibility(getString(R.string.check_done) + ". " + checkBody.getText());
        }
        boolean bad = h == Hero.STOPPED || h == Hero.CHANGED;
        ImageView checkIcon = findViewById(R.id.check_icon);
        checkIcon.setImageResource(bad ? R.drawable.ic_status_problem : R.drawable.ic_health);
        checkIcon.setBackgroundTintList(getColorStateList(bad ? R.color.error_container : R.color.secondary_container));
        checkIcon.setImageTintList(getColorStateList(bad ? R.color.on_error_container : R.color.on_secondary_container));
        firstRender = false;
    }

    private void renderWeek() {
        int[] week = State.week(this);
        int sum = 0;
        for (int n : week) sum += n;
        int total = State.total(this);
        boolean empty = total == 0;
        findViewById(R.id.week_numbers).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.chart).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.week_more).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;
        NumberFormat f = NumberFormat.getInstance();
        TextView count = findViewById(R.id.week_count);
        if (firstRender) Ui.countUp(count, sum);
        else count.setText(f.format(sum));
        String label = getResources().getQuantityString(R.plurals.week_count, sum);
        ((TextView) findViewById(R.id.week_label)).setText(label);
        findViewById(R.id.week_numbers).setContentDescription(f.format(sum) + " " + label + ", " + getString(R.string.week_title));
        ((WeekChart) findViewById(R.id.chart)).setCounts(week, firstRender);
        ((TextView) findViewById(R.id.week_more)).setText(getString(R.string.today_count, f.format(week[6]))
            + getString(R.string.stats_separator) + getString(R.string.total_count, f.format(total)));
    }

    private void renderPro() {
        boolean on = State.pro(this);
        findViewById(R.id.buy_row).setVisibility(on ? View.GONE : View.VISIBLE);
        findViewById(R.id.schedule_row).setVisibility(on ? View.VISIBLE : View.GONE);
        findViewById(R.id.year_card).setVisibility(on && State.total(this) > 0 ? View.VISIBLE : View.GONE);
        String price = pro == null ? null : pro.price();
        ((TextView) findViewById(R.id.buy_body)).setText(price == null ? getString(R.string.buy_body)
            : getString(R.string.buy_body_price, price));
        if (!on) return;
        ((TextView) findViewById(R.id.schedule_body)).setText(State.scheduleOn(this) ? scheduleText() : getString(R.string.schedule_off));
        int[] months = State.months(this);
        int year = 0;
        for (int n : months) year += n;
        NumberFormat f = NumberFormat.getInstance();
        ((WeekChart) findViewById(R.id.year_chart)).setMonths(months, firstRender);
        ((TextView) findViewById(R.id.year_more)).setText(getString(R.string.month_count, f.format(months[11]))
            + getString(R.string.stats_separator) + getString(R.string.year_count, f.format(year)));
    }

    @SuppressWarnings("deprecation")
    private boolean fromPlay() {
        try {
            String i = Build.VERSION.SDK_INT >= 30
                ? getPackageManager().getInstallSourceInfo(getPackageName()).getInstallingPackageName()
                : getPackageManager().getInstallerPackageName(getPackageName());
            return "com.android.vending".equals(i);
        } catch (Exception e) {
            return false;
        }
    }

    private String shareText() {
        int n = State.total(this);
        return (n > 0 ? getString(R.string.share_some, n) : getString(R.string.share_none))
            + getString(R.string.share_body, REPO);
    }
}
