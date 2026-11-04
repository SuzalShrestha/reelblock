package dev.local.reelblock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Date;

// The screen you see when you tap the app icon: is the blocker on, how many Reels it closed, and a share button.
// Layout and design tokens live in res/layout/main.xml and res/values/{colors,styles}.xml.
public class MainActivity extends Activity {
    static final String REPO = "https://github.com/SuzalShrestha/reelblock";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        // NEW_TASK: these screens get their own task, so onStop() below does not close them.
        findViewById(R.id.turn_on).setOnClickListener(v -> startActivity(new Intent(
            Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        findViewById(R.id.app_info).setOnClickListener(v -> startActivity(new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        findViewById(R.id.share).setOnClickListener(v -> startActivity(Intent.createChooser(
            new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText()),
            getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        // Xiaomi only: without Autostart, the phone does not restart the service after it stops the app.
        // setPackage: only Xiaomi's own app may answer, never another app that copies the action.
        Intent autostart = new Intent("miui.intent.action.OP_AUTO_START").setPackage("com.miui.securitycenter")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (autostart.resolveActivity(getPackageManager()) != null) {
            findViewById(R.id.autostart_card).setVisibility(View.VISIBLE);
            findViewById(R.id.autostart).setOnClickListener(v -> startActivity(autostart));
        }
        // Pause without turning the service off: the service stays bound and only skips the Back press.
        ((Switch) findViewById(R.id.block_switch)).setOnCheckedChangeListener((v, on) -> {
            if (on == !paused(this)) return;
            if (on) { settings().edit().remove("paused").remove("paused_until").apply(); render(); return; }
            // A timed pause ends by itself, so a pause you forget does not turn the blocker off for good.
            new AlertDialog.Builder(this, R.style.Dialog).setTitle(R.string.pause_title)
                .setItems(R.array.pause_options, (d, i) -> {
                    if (i == PAUSE_MINUTES.length) settings().edit().putBoolean("paused", true).apply();
                    else settings().edit().putLong("paused_until",
                        System.currentTimeMillis() + PAUSE_MINUTES[i] * 60_000L).apply();
                    render();
                })
                .setOnCancelListener(d -> render()) // No choice: blocking stays on, the switch goes back on.
                .show();
        });
    }

    // Same order as R.array.pause_options; the last option there ("until I turn it on") has no time.
    static final int[] PAUSE_MINUTES = {15, 60};
    private final Runnable rerender = this::render;

    private SharedPreferences settings() {
        return getSharedPreferences("settings", MODE_PRIVATE);
    }

    // Read by ReelBlockService on each event. Same process, so the change applies at once.
    // Wall clock, not elapsedRealtime: a timed pause must also end after a restart.
    static boolean paused(Context c) {
        SharedPreferences p = c.getSharedPreferences("settings", MODE_PRIVATE);
        return p.getBoolean("paused", false) || System.currentTimeMillis() < p.getLong("paused_until", 0);
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
        render();
    }

    private void render() {
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        // Match whole entries ("pkg/service", split by ':'). contains() would also match "x.dev.local.reelblock/...".
        boolean enabled = false;
        if (on != null) for (String s : on.split(":")) enabled |= s.startsWith(getPackageName() + "/");
        boolean paused = paused(this);
        boolean active = enabled && !paused;
        Switch sw = findViewById(R.id.block_switch);
        sw.setVisibility(enabled ? View.VISIBLE : View.GONE);
        sw.setChecked(!paused);
        TextView pill = findViewById(R.id.pill);
        pill.setText(active ? R.string.status_on : enabled ? R.string.status_paused : R.string.status_off);
        pill.setTextColor(getColor(active ? R.color.ok : R.color.warn));
        pill.setBackgroundTintList(getColorStateList(active ? R.color.ok_soft : R.color.warn_soft));
        ((TextView) findViewById(R.id.headline)).setText(
            active ? R.string.headline_on : enabled ? R.string.headline_paused : R.string.headline_off);
        long until = settings().getLong("paused_until", 0);
        boolean timed = paused && !settings().getBoolean("paused", false);
        TextView body = findViewById(R.id.body);
        if (enabled && timed) {
            body.setText(getString(R.string.body_paused_until, DateFormat.getTimeFormat(this).format(new Date(until))));
            // Show ON again when the pause ends while this screen is open.
            body.removeCallbacks(rerender);
            body.postDelayed(rerender, until - System.currentTimeMillis() + 500);
        } else body.setText(active ? R.string.body_on : enabled ? R.string.body_paused : R.string.body_off);
        ((TextView) findViewById(R.id.count)).setText(NumberFormat.getInstance().format(blocked()));
        Button turnOn = findViewById(R.id.turn_on);
        turnOn.setText(enabled ? R.string.manage : R.string.turn_on);
        // Once it is on, the setup help is noise. The button turns quiet too.
        turnOn.setBackgroundResource(enabled ? R.drawable.btn_secondary : R.drawable.btn_primary);
        turnOn.setTextColor(getColor(enabled ? R.color.text : R.color.on_accent));
        findViewById(R.id.setup).setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private int blocked() {
        return getSharedPreferences("stats", MODE_PRIVATE).getInt("blocked", 0);
    }

    private String shareText() {
        int n = blocked();
        return (n > 0 ? getString(R.string.share_some, n) : getString(R.string.share_none))
            + getString(R.string.share_body, REPO);
    }
}
