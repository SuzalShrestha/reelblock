package dev.local.reelblock;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

// First run, 3 pages, inside MainActivity (one activity, so the HyperOS fix in MainActivity.onStop still holds):
//   1. what Reel Block is   2. what it can and cannot see (the disclosure)   3. how to turn it on
// Android settings open only from page 3, and page 3 is reached only by tapping "Agree and continue".
// "Not now" goes home and is remembered: the app does not show the first run again by itself.
final class Welcome {
    static final int PAGES = 3, DISCLOSURE = 1, SETUP = 2;
    private final MainActivity a;
    private final View[] pages;
    private final LinearLayout dots;
    // Opened from the home screen (Turn on, Setup help, What it can see): Back returns home.
    final boolean fromHome;
    private int page = -1;

    Welcome(MainActivity a, int start, boolean fromHome) {
        this.a = a;
        this.fromHome = fromHome;
        a.setContentView(R.layout.welcome);
        pages = new View[]{a.findViewById(R.id.page1), a.findViewById(R.id.page2), a.findViewById(R.id.page3)};
        dots = a.findViewById(R.id.dots);
        int s = a.getResources().getDimensionPixelSize(R.dimen.dot_size);
        for (int i = 0; i < PAGES; i++) {
            View d = new View(a);
            d.setBackgroundResource(R.drawable.dot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            lp.setMarginStart(i == 0 ? 0 : a.getResources().getDimensionPixelSize(R.dimen.space_2));
            dots.addView(d, lp);
        }
        Ui.edgeToEdge(a, a.findViewById(R.id.welcome_root), a.findViewById(R.id.welcome_actions));
        a.findViewById(R.id.welcome_back).setOnClickListener(v -> { if (!back()) a.finish(); });
        a.findViewById(R.id.welcome_next).setOnClickListener(v -> next());
        a.findViewById(R.id.welcome_skip).setOnClickListener(v -> { State.setSeen(a); a.showHome(false); });
        a.findViewById(R.id.read_code).setOnClickListener(v -> Ui.start(a, Ui.web(MainActivity.REPO)));
        // Android 13+ blocks this setting for apps from outside an app store ("Restricted setting").
        a.findViewById(R.id.restricted).setVisibility(Build.VERSION.SDK_INT >= 33 ? View.VISIBLE : View.GONE);
        a.findViewById(R.id.app_info).setOnClickListener(v -> Ui.start(a, new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName()))));
        show(Math.max(0, Math.min(PAGES - 1, start)));
    }

    int page() {
        return page;
    }

    private void next() {
        if (page == 0) show(DISCLOSURE);
        else if (page == DISCLOSURE) { State.setConsent(a); show(SETUP); }
        else Ui.start(a, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    // Opened from home: Back goes home. First run: Back goes to the previous page, and from page 1 leaves.
    boolean back() {
        if (fromHome) {
            a.showHome(false);
            return true;
        }
        if (page == 0) return false;
        show(page - 1);
        return true;
    }

    private void show(int p) {
        boolean forward = p > page;
        for (int i = 0; i < PAGES; i++) if (i != p) pages[i].setVisibility(View.GONE);
        // Pages slide in from the side they come from (mirrored in right-to-left languages).
        float dx = page < 0 ? 0 : a.getResources().getDimension(R.dimen.space_8)
            * (forward ? 1 : -1) * (pages[p].getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1 : 1);
        Ui.enter(pages[p], dx, 0);
        page = p;
        pages[p].scrollTo(0, 0);
        a.findViewById(R.id.welcome_back).setVisibility(p == 0 && !fromHome ? View.INVISIBLE : View.VISIBLE);
        Button next = a.findViewById(R.id.welcome_next);
        // After agreeing once, the disclosure is for reading again: no second "Agree".
        boolean agreed = State.consent(a);
        next.setText(p == 0 ? R.string.get_started : p == DISCLOSURE ? (agreed ? R.string.next : R.string.agree) : R.string.open_settings);
        a.findViewById(R.id.welcome_skip).setVisibility(p == DISCLOSURE && !agreed ? View.VISIBLE : View.GONE);
        for (int i = 0; i < PAGES; i++) {
            View d = dots.getChildAt(i);
            ViewGroup.LayoutParams lp = d.getLayoutParams();
            lp.width = a.getResources().getDimensionPixelSize(i == p ? R.dimen.dot_active_width : R.dimen.dot_size);
            d.setLayoutParams(lp);
            d.setBackgroundTintList(a.getColorStateList(i == p ? R.color.indicator_active : R.color.indicator_inactive));
        }
        dots.setContentDescription(a.getString(R.string.page_of, p + 1, PAGES));
        pages[p].announceForAccessibility(a.getString(R.string.page_of, p + 1, PAGES));
    }
}
