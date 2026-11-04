package shots;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ScrollView;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

// Renders app screens to PNG with Robolectric native graphics (real Skia), and audits clickable nodes
// like `uiautomator dump` would: every clickable node needs text or content-desc, and 48dp bounds.
// Dev tool only (tools/screens/run.sh); nothing here is part of the APK.
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class Shots {
    static final String PKG = "dev.local.reelblock";
    static final File OUT = new File(System.getProperty("shots.out", "/tmp/shots"));
    static final PrintWriter AUDIT;
    static { OUT.mkdirs(); try { AUDIT = new PrintWriter(new FileWriter(new File(OUT, "audit.txt"), true)); } catch (IOException e) { throw new RuntimeException(e); } }

    // Scene file: one scene per line: name|activity|state|extra   (state: off,on,paused,timed,onboard...)
    @Test public void render() throws Exception {
        File scenes = new File(System.getProperty("shots.scenes", OUT + "/scenes.txt"));
        String[] variants = System.getProperty("shots.variants", "light,dark,font2").split(",");
        for (String line : read(scenes)) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            for (String v : variants) shot(f[0], f[1], f[2], f.length > 3 ? f[3] : "", v.trim());
        }
        AUDIT.flush();
    }

    static List<String> read(File f) throws IOException {
        List<String> l = new ArrayList<>(); try (BufferedReader r = new BufferedReader(new FileReader(f))) { String s; while ((s = r.readLine()) != null) l.add(s); } return l;
    }

    void shot(String name, String activity, String state, String extra, String variant) throws Exception {
        String q = "w" + (variant.contains("small") ? 360 : 411) + "dp-h" + (variant.contains("small") ? 740 : 914) + "dp";
        q += (variant.contains("dark") ? "-night" : "-notnight") + "-420dpi";
        if (variant.contains("rtl")) q = "ar-ldrtl-" + q;
        RuntimeEnvironment.setQualifiers(q);
        RuntimeEnvironment.setFontScale(variant.contains("font2") ? 2.0f : 1.0f);
        Context c = RuntimeEnvironment.getApplication();
        // Fresh state for each scene.
        for (String p : new String[]{"settings", "stats"}) c.getSharedPreferences(p, 0).edit().clear().commit();
        boolean enabled = !state.contains("off");
        Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            enabled ? PKG + "/" + PKG + ".ReelBlockService" : "");
        SharedPreferences st = c.getSharedPreferences("stats", 0), se = c.getSharedPreferences("settings", 0);
        // extra: k=v;k=v  with prefix s. (settings) t. (stats); values: int, long (L suffix), bool, now+Nmin, string
        for (String kv : extra.split(";")) {
            if (!kv.contains("=")) continue;
            String k = kv.substring(0, kv.indexOf('=')), val = kv.substring(kv.indexOf('=') + 1);
            SharedPreferences.Editor e = (k.startsWith("t.") ? st : se).edit(); k = k.substring(2);
            if (val.startsWith("now")) e.putLong(k, System.currentTimeMillis() + Long.parseLong(val.substring(3)) * 60000L);
            else if (val.equals("true") || val.equals("false")) e.putBoolean(k, Boolean.parseBoolean(val));
            else if (val.endsWith("L")) e.putLong(k, Long.parseLong(val.substring(0, val.length() - 1)));
            else if (val.matches("-?\\d+")) e.putInt(k, Integer.parseInt(val));
            else e.putString(k, val);
            e.commit();
        }
        // week=a,b,c,d,e,f,g : per-day counts, oldest first, today last (keys d<localEpochDay>).
        if (extra.contains("week=")) {
            String[] w = extra.replaceAll(".*week=([\\d,]+).*", "$1").split(",");
            long now = System.currentTimeMillis(), today = (now + java.util.TimeZone.getDefault().getOffset(now)) / 86400000L;
            SharedPreferences.Editor e = st.edit();
            for (int k = 0; k < 7; k++) e.putInt("d" + (today - 6 + k), Integer.parseInt(w[k]));
            e.commit();
        }
        // Service bound or not (the self-check reads this flag).
        Class<?> svc = Class.forName(PKG + ".ReelBlockService");
        try { java.lang.reflect.Field fl = svc.getDeclaredField("connected"); fl.setAccessible(true); fl.setBoolean(null, enabled && !state.contains("stopped")); } catch (NoSuchFieldException ignored) {}
        // Fake Instagram package: ig=ok (has the Reels view name) or ig=changed (renamed).
        if (extra.contains("ig=")) {
            String which = extra.replaceAll(".*ig=(\\w+).*", "$1");
            android.content.pm.PackageInfo pi = new android.content.pm.PackageInfo();
            pi.packageName = "com.instagram.android"; pi.lastUpdateTime = which.equals("ok") ? 1 : 2; pi.versionName = which.equals("ok") ? "453.0.0.57.79" : "461.0.0.12.80";
            pi.applicationInfo = new android.content.pm.ApplicationInfo();
            pi.applicationInfo.packageName = pi.packageName;
            Shadows.shadowOf(c.getPackageManager()).installPackage(pi);
            // Instagram's resource table: has the Reels view name (ok) or not (changed).
            final boolean has = which.equals("ok");
            org.robolectric.shadows.ShadowPackageManager.resources.put("com.instagram.android",
                new android.content.res.Resources(c.getAssets(), c.getResources().getDisplayMetrics(), c.getResources().getConfiguration()) {
                    @Override public int getIdentifier(String n, String t, String p) { return has && n.equals("clips_viewer_view_pager") ? 0x7f0b0001 : 0; }
                });
        }
        if (state.contains("paused")) se.edit().putBoolean("paused", true).commit();
        if (state.contains("timed")) se.edit().putLong("paused_until", System.currentTimeMillis() + 42 * 60000L).commit();
        Intent i = new Intent().setClassName(PKG, PKG + "." + activity);
        if (state.contains("page")) i.putExtra("page", Integer.parseInt(state.replaceAll(".*page(\\d+).*", "$1")));
        ActivityController<Activity> ac = Robolectric.buildActivity((Class<Activity>) Class.forName(PKG + "." + activity), i).setup();
        Activity a = ac.get();
        ShadowLooper.idleMainLooper(); ShadowLooper.idleMainLooper(1200, java.util.concurrent.TimeUnit.MILLISECONDS); ShadowLooper.idleMainLooper(800, java.util.concurrent.TimeUnit.MILLISECONDS);
        // Optional: click a view by id name to open a dialog or a page.
        if (state.contains("click:")) {
            for (String id : state.replaceAll(".*click:([\\w,]+).*", "$1").split(",")) {
                View b = a.findViewById(a.getResources().getIdentifier(id, "id", PKG));
                if (b != null) b.performClick();
                ShadowLooper.idleMainLooper();
            }
        }
        Thread.sleep(300); // the self-check thread
        ShadowLooper.idleMainLooper(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
        View root = a.getWindow().getDecorView();
        // "screen": exactly one phone screen (bottom buttons visible), otherwise the full scroll length.
        Bitmap bmp = state.contains("screen") ? renderScreen(root, a) : renderFull(root, a);
        Dialog d = ShadowDialog.getLatestDialog();
        if (d != null && d.isShowing()) bmp = overlay(bmp, d.getWindow().getDecorView(), a);
        String file = name + "_" + variant + ".png";
        try (OutputStream o = new FileOutputStream(new File(OUT, file))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, o); }
        audit(file, root, a);
        if (d != null && d.isShowing()) { audit(file + "[dialog]", d.getWindow().getDecorView(), a); d.dismiss(); }
        ac.pause().stop().destroy();
        ShadowDialog.reset();
    }

    // Full-length capture: lay the content out with unbounded height so a long ScrollView is visible in one image.
    static Bitmap renderFull(View root, Activity a) {
        int w = a.getResources().getDisplayMetrics().widthPixels, h = a.getResources().getDisplayMetrics().heightPixels;
        View content = a.findViewById(android.R.id.content);
        ScrollView sv = findScroll(content);
        int full = h;
        if (sv != null && sv.getChildCount() > 0) {
            View child = sv.getChildAt(0);
            child.measure(View.MeasureSpec.makeMeasureSpec(sv.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            full = Math.max(h, h - sv.getHeight() + child.getMeasuredHeight());
        }
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(full, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, full);
        Bitmap b = Bitmap.createBitmap(w, full, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        root.draw(cv);
        return b;
    }

    static Bitmap renderScreen(View root, Activity a) {
        int w = a.getResources().getDisplayMetrics().widthPixels, h = a.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        return b;
    }

    static ScrollView findScroll(View v) {
        if (v.getVisibility() != View.VISIBLE) return null;
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { ScrollView s = findScroll(((ViewGroup) v).getChildAt(i)); if (s != null) return s; }
        return null;
    }

    static Bitmap overlay(Bitmap base, View dlg, Activity a) {
        int w = a.getResources().getDisplayMetrics().widthPixels, h = a.getResources().getDisplayMetrics().heightPixels;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawBitmap(base, 0, 0, null);
        cv.drawColor(Color.argb(0x52, 0, 0, 0));
        int dw = Math.min(w - 2 * (int) (24 * a.getResources().getDisplayMetrics().density), (int) (560 * a.getResources().getDisplayMetrics().density));
        dlg.measure(View.MeasureSpec.makeMeasureSpec(dw, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
        dlg.layout(0, 0, dw, dlg.getMeasuredHeight());
        cv.save(); cv.translate((w - dw) / 2f, (h - dlg.getMeasuredHeight()) / 2f); dlg.draw(cv); cv.restore();
        return b;
    }

    // Like uiautomator: walk the accessibility node of every visible view.
    static void audit(String file, View root, Activity a) {
        float dp = a.getResources().getDisplayMetrics().density;
        List<String> bad = new ArrayList<>(); int clickable = 0;
        Deque<View> q = new ArrayDeque<>(); q.add(root);
        while (!q.isEmpty()) {
            View v = q.poll();
            if (v.getVisibility() != View.VISIBLE) continue;
            if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) q.add(((ViewGroup) v).getChildAt(i));
            if (!(v.isClickable() || v.isLongClickable()) || !v.isEnabled()) continue;
            if (v.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) continue;
            clickable++;
            AccessibilityNodeInfo n = v.createAccessibilityNodeInfo();
            CharSequence label = n.getText() != null && n.getText().length() > 0 ? n.getText() : n.getContentDescription();
            if (label == null || label.length() == 0) label = childText(v);
            String id = v.getId() > 0 ? a.getResources().getResourceEntryName(v.getId()) : v.getClass().getSimpleName();
            if (label == null || label.length() == 0) bad.add("NO LABEL: " + id);
            if (v.getWidth() < 48 * dp - 1 || v.getHeight() < 48 * dp - 1) bad.add("SMALL " + (int) (v.getWidth() / dp) + "x" + (int) (v.getHeight() / dp) + "dp: " + id + " \"" + label + "\"");
        }
        AUDIT.println(file + ": clickable=" + clickable + (bad.isEmpty() ? " OK" : " FAIL " + bad));
    }

    // TalkBack reads the text of non-focusable children of a clickable group.
    static CharSequence childText(View v) {
        if (!(v instanceof ViewGroup)) return null;
        StringBuilder sb = new StringBuilder();
        Deque<View> q = new ArrayDeque<>(); q.add(v);
        while (!q.isEmpty()) {
            View c = q.poll();
            if (c != v && c instanceof android.widget.TextView && c.getVisibility() == View.VISIBLE) sb.append(((android.widget.TextView) c).getText()).append(' ');
            if (c.getContentDescription() != null && c != v) sb.append(c.getContentDescription()).append(' ');
            if (c instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) c).getChildCount(); i++) q.add(((ViewGroup) c).getChildAt(i));
        }
        return sb.toString().trim();
    }
}
