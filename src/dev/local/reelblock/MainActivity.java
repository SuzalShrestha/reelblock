package dev.local.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// The screen you see when you tap the app icon: is the blocker on, how many Reels it closed, and a share button.
public class MainActivity extends Activity {
    static final String REPO = "https://github.com/SuzalShrestha/reelblock";
    private TextView status, count;
    private Button turnOn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(pad, pad * 2, pad, pad);

        text(l,"Closes Instagram Reels and YouTube Shorts when they open. The rest of each app works as before.", 16);
        status = text(l, "", 18);
        count = text(l, "", 16);

        turnOn = button(l, "Turn on Reel Block", () ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        text(l, "Find \"Reel Block\" in the list (it can be under \"Downloaded apps\") and turn it on.\n\n"
            + "Switch is grey? Tap the button below, then ⋮ → \"Allow restricted settings\", then try again.", 14);
        button(l, "Open app info", () -> startActivity(new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        button(l, "Share Reel Block", () -> startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
            .setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText()), "Share Reel Block")));
        setContentView(l);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean enabled = on != null && on.contains(getPackageName() + "/");
        status.setText(enabled ? "✅ On. Reels and Shorts will close." : "⚠️ Off. Turn it on to block Reels and Shorts.");
        turnOn.setText(enabled ? "Accessibility settings" : "Turn on Reel Block");
        count.setText("Reels and Shorts closed: " + blocked());
    }

    private int blocked() {
        return getSharedPreferences("stats", MODE_PRIVATE).getInt("blocked", 0);
    }

    private String shareText() {
        int n = blocked();
        return (n > 0 ? "Reel Block closed " + n + " Reels and Shorts for me. " : "I stopped doom-scrolling Reels. ")
            + "It's a free, open-source Android app that closes Instagram Reels and YouTube Shorts "
            + "and keeps the rest of the apps. " + REPO;
    }

    private TextView text(LinearLayout l, String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setPadding(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        l.addView(t);
        return t;
    }

    private Button button(LinearLayout l, String s, Runnable r) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setOnClickListener(v -> r.run());
        l.addView(btn);
        return btn;
    }
}
