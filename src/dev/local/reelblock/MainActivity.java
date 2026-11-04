package dev.local.reelblock;

import android.app.Activity;
import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Date;

// The app screen: first-run onboarding (Welcome.java), then home: one status with one action, this week's
// count, the self-check, shortcuts, and the privacy promises. Layouts: res/layout/{main,welcome}.xml.
public class MainActivity extends Activity {
    static final String REPO = "https://github.com/SuzalShrestha/reelblock";
    static final String EXTRA_PAUSE = "pause"; // from the Quick Settings tile: ask "pause for how long?"
    private enum Hero { ON, PAUSED, OFF, STOPPED, CHANGED }

    private Welcome welcome;
    private Hero shown;
    private boolean firstRender;
    private long graceUntil;
    private Dialog dialog;
    private final Runnable rerender = this::render;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // After a rotation or a theme change, stay on the same onboarding page.
        if (b != null && b.containsKey("page")) welcome = new Welcome(this, b.getInt("page"), b.getBoolean("fromHome"));
        // First run only: explain before asking. "Not now" (seen) and an already enabled service skip it.
        else if (!State.seen(this) && !State.consent(this) && !State.enabled(this)) welcome = new Welcome(this, 0, false);
        else showHome(b == null && wantsPause(getIntent()));
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
        graceUntil = System.currentTimeMillis() + 1000;
        ((Switch) findViewById(R.id.block_switch)).setOnCheckedChangeListener((v, on) -> {
            if (on == !State.paused(this)) return;
            if (on) { State.resume(this); render(); }
            else askPause();
        });
        findViewById(R.id.check_row).setOnClickListener(v -> {
            render();
            v.announceForAccessibility(getString(R.string.check_done));
        });
        findViewById(R.id.tile_row).setOnClickListener(v -> addTile());
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

    private void addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            StatusBarManager sbm = getSystemService(StatusBarManager.class);
            if (sbm != null) {
                sbm.requestAddTileService(new ComponentName(this, QsTile.class), getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile_brand), getMainExecutor(), r -> {
                        if (r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED && !isFinishing())
                            dialog = Ui.info(this, R.string.tile_row_title, R.string.tile_already);
                    });
                return;
            }
        }
        dialog = Ui.info(this, R.string.tile_how_title, R.string.tile_how_body);
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
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (welcome != null && welcome.back()) return;
        super.onBackPressed();
    }

    // Leave no Recents card. On Xiaomi/HyperOS, swiping the card away kills every process of the app
    // (SwipeUpClean), the blocker too, and Android then shows it as "Not working" until it is turned on again.
    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) finishAndRemoveTask();
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
        Check check = Check.run(this);
        boolean paused = State.paused(this);
        long until = State.pausedUntil(this);
        Hero h = check.status == Check.OFF ? Hero.OFF
            : check.status == Check.STOPPED ? Hero.STOPPED
            : paused ? Hero.PAUSED
            : check.status == Check.CHANGED ? Hero.CHANGED : Hero.ON;
        long wait = graceUntil - System.currentTimeMillis();
        if (h == Hero.STOPPED && wait > 0) {
            heroView.setVisibility(View.INVISIBLE);
            heroView.postDelayed(rerender, wait);
        } else if (heroView.getVisibility() != View.VISIBLE) {
            Ui.enter(heroView, 0, getResources().getDimension(R.dimen.space_4));
        }

        TextView title = findViewById(R.id.hero_title), body = findViewById(R.id.hero_body);
        ImageView badge = findViewById(R.id.hero_badge);
        int bg, fg, badgeBg, badgeFg, icon;
        switch (h) {
            case ON: bg = R.color.hero_on_bg; fg = R.color.hero_on_fg; badgeBg = R.color.hero_on_badge;
                badgeFg = R.color.hero_on_badge_icon; icon = R.drawable.ic_status_on;
                title.setText(R.string.hero_on_title); body.setText(R.string.hero_on_body); break;
            case PAUSED: bg = R.color.hero_paused_bg; fg = R.color.hero_paused_fg; badgeBg = R.color.hero_paused_badge;
                badgeFg = R.color.hero_paused_badge_icon; icon = R.drawable.ic_status_paused;
                title.setText(R.string.hero_paused_title);
                body.setText(until > 0 ? getString(R.string.hero_paused_until,
                    DateFormat.getTimeFormat(this).format(new Date(until))) : getString(R.string.hero_paused_body));
                // Show "on" again when the pause ends while this screen is open.
                if (until > 0) heroView.postDelayed(rerender, until - System.currentTimeMillis() + 500);
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
        sw.setVisibility(h == Hero.ON || h == Hero.PAUSED ? View.VISIBLE : View.GONE);
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
            action.setOnClickListener(v -> Ui.start(this, Ui.web(REPO + "/releases/latest")));
            action2.setText(R.string.report);
            action2.setOnClickListener(v -> Ui.start(this, Ui.web(REPO + "/issues/new?template=id-changed.md")));
        }

        renderWeek();
        TextView checkBody = findViewById(R.id.check_body);
        String v = check.instagramVersion;
        checkBody.setText(check.status == Check.OK ? getString(R.string.check_ok, v)
            : check.status == Check.OFF ? getString(R.string.check_off)
            : check.status == Check.STOPPED ? getString(R.string.check_stopped)
            : check.status == Check.CHANGED ? getString(R.string.check_changed, v)
            : check.status == Check.UNKNOWN ? getString(R.string.check_unknown, v)
            : getString(R.string.check_no_instagram));
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

    private String shareText() {
        int n = State.total(this);
        return (n > 0 ? getString(R.string.share_some, n) : getString(R.string.share_none))
            + getString(R.string.share_body, REPO);
    }
}
