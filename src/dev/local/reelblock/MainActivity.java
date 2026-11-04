package dev.local.reelblock;

import android.app.Activity;
import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
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
    static final String EXTRA_PAUSE = "pause", EXTRA_SETUP = "setup";
    private enum Hero { ON, PAUSED, OFF, STOPPED, CHANGED }

    private Welcome welcome;
    private Hero shown;
    private boolean firstRender;
    private final Runnable rerender = this::render;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // First run: explain before asking. Also when "Turn on" is tapped before the disclosure was agreed.
        if (getIntent().getBooleanExtra(EXTRA_SETUP, false)) welcome = new Welcome(this, State.consent(this) ? 2 : 1);
        else if (!State.consent(this) && !State.enabled(this)) welcome = new Welcome(this, 0);
        else showHome(getIntent().getBooleanExtra(EXTRA_PAUSE, false));
    }

    void showHome(boolean askPause) {
        welcome = null;
        setContentView(R.layout.main);
        View scroll = findViewById(R.id.scroll);
        Ui.edgeToEdge(this, scroll, scroll);
        firstRender = true;
        shown = null;
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
        findViewById(R.id.help_row).setOnClickListener(v -> setup());
        findViewById(R.id.code_row).setOnClickListener(v -> openUrl(REPO));
        findViewById(R.id.share_row).setOnClickListener(v -> startActivity(Intent.createChooser(
            new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText()),
            getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        // Xiaomi only: without Autostart, the phone does not restart the service after it stops the app.
        // setPackage: only Xiaomi's own app may answer, never another app that copies the action.
        Intent autostart = new Intent("miui.intent.action.OP_AUTO_START").setPackage("com.miui.securitycenter")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (autostart.resolveActivity(getPackageManager()) != null) {
            findViewById(R.id.autostart_row).setVisibility(View.VISIBLE);
            findViewById(R.id.autostart_row).setOnClickListener(v -> startActivity(autostart));
        }
        try {
            ((TextView) findViewById(R.id.version)).setText(getString(R.string.version,
                getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (Exception e) { findViewById(R.id.version).setVisibility(View.GONE); }
        State.prune(this);
        render();
        if (askPause && State.enabled(this) && !State.paused(this)) askPause();
    }

    // A timed pause ends by itself, so a pause you forget does not turn the blocker off for good.
    // Back, a tap outside or Cancel keep blocking on.
    private void askPause() {
        Dialog d = Ui.dialog(this, R.layout.dialog_pause);
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

    private void setup() {
        welcome = new Welcome(this, State.consent(this) ? 2 : 1);
        getIntent().putExtra(EXTRA_SETUP, true);
    }

    private void addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            StatusBarManager sbm = getSystemService(StatusBarManager.class);
            if (sbm != null) {
                sbm.requestAddTileService(new ComponentName(this, QsTile.class), getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile_brand), getMainExecutor(), r -> {});
                return;
            }
        }
        Ui.info(this, R.string.tile_how_title, R.string.tile_how_body);
    }

    void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
        // Just turned on in Settings: go straight home and say so.
        else if (State.enabled(this) && State.running(this)) showHome(false);
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
        if (h == Hero.STOPPED && firstRender) {
            // Android may still be binding the service right after the app process starts. Look again
            // before saying it stopped, and show no status until then rather than a wrong one.
            heroView.setVisibility(View.INVISIBLE);
            heroView.postDelayed(rerender, 1000);
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
        ((Button) findViewById(R.id.hero_action2)).setTextColor(getColor(fg));
        badge.setImageResource(icon);
        badge.setBackgroundTintList(getColorStateList(badgeBg));
        badge.setImageTintList(getColorStateList(badgeFg));
        if (shown != h && (shown != null || h == Hero.ON)) Ui.pop(badge);

        // One main action per state.
        Switch sw = findViewById(R.id.block_switch);
        sw.setVisibility(h == Hero.ON || h == Hero.PAUSED ? View.VISIBLE : View.GONE);
        sw.setChecked(!paused);
        Button action = findViewById(R.id.hero_action), action2 = findViewById(R.id.hero_action2);
        action.setVisibility(h == Hero.OFF || h == Hero.STOPPED || h == Hero.CHANGED ? View.VISIBLE : View.GONE);
        action2.setVisibility(h == Hero.CHANGED ? View.VISIBLE : View.GONE);
        if (h == Hero.OFF) { action.setText(R.string.turn_on); action.setOnClickListener(v -> setup()); }
        if (h == Hero.STOPPED) {
            action.setText(R.string.fix_stopped);
            action.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        }
        if (h == Hero.CHANGED) {
            action.setText(R.string.get_update);
            action.setOnClickListener(v -> openUrl(REPO + "/releases/latest"));
            action2.setText(R.string.report);
            action2.setOnClickListener(v -> openUrl(REPO + "/issues/new?template=id-changed.md"));
        }
        if (shown == Hero.OFF && h == Hero.ON) title.announceForAccessibility(getString(R.string.announce_on));
        shown = h;

        renderWeek();
        TextView checkBody = findViewById(R.id.check_body);
        String v = check.instagramVersion;
        checkBody.setText(check.status == Check.OK ? getString(R.string.check_ok, v)
            : check.status == Check.OFF ? getString(R.string.check_off)
            : check.status == Check.STOPPED ? getString(R.string.check_stopped)
            : check.status == Check.CHANGED ? getString(R.string.check_changed, v)
            : getString(R.string.check_no_instagram));
        boolean bad = check.status == Check.STOPPED || check.status == Check.CHANGED;
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
        ((TextView) findViewById(R.id.week_label)).setText(getResources().getQuantityString(R.plurals.week_count, sum));
        findViewById(R.id.week_numbers).setContentDescription(f.format(sum) + " "
            + getResources().getQuantityString(R.plurals.week_count, sum) + ", " + getString(R.string.week_title));
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
