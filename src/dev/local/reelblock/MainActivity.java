package dev.local.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.NumberFormat;

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
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        // Match whole entries ("pkg/service", split by ':'). contains() would also match "x.dev.local.reelblock/...".
        boolean enabled = false;
        if (on != null) for (String s : on.split(":")) enabled |= s.startsWith(getPackageName() + "/");
        TextView pill = findViewById(R.id.pill);
        pill.setText(enabled ? R.string.status_on : R.string.status_off);
        pill.setTextColor(getColor(enabled ? R.color.ok : R.color.warn));
        pill.setBackgroundTintList(getColorStateList(enabled ? R.color.ok_soft : R.color.warn_soft));
        ((TextView) findViewById(R.id.headline)).setText(enabled ? R.string.headline_on : R.string.headline_off);
        ((TextView) findViewById(R.id.body)).setText(enabled ? R.string.body_on : R.string.body_off);
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
