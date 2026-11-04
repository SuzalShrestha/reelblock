package dev.local.reelblock;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import java.text.NumberFormat;

// Small shared UI helpers: edge-to-edge, M3 motion with the motion tokens, and the dialog shell.
// Every animation checks "Remove animations" (ValueAnimator.areAnimatorsEnabled) and jumps to the end if it is on.
final class Ui {
    private Ui() {}

    // Android 11+: draw behind the transparent system bars, then pad the content by the bar sizes.
    // Older Android keeps the bars in the window colour (styles.xml), which looks the same.
    // `root` gets the top and side insets, `bottom` (can be the same view) the bottom inset.
    static void edgeToEdge(Activity a, View root, View bottom) {
        if (Build.VERSION.SDK_INT < 30) return;
        Window w = a.getWindow();
        w.setDecorFitsSystemWindows(false);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        int l = root.getPaddingLeft(), t = root.getPaddingTop(), r = root.getPaddingRight(), b = bottom.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, in) -> {
            android.graphics.Insets s = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            root.setPadding(l + s.left, t + s.top, r + s.right, root.getPaddingBottom());
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), b + s.bottom);
            return WindowInsets.CONSUMED;
        });
    }

    static boolean animate() {
        return ValueAnimator.areAnimatorsEnabled();
    }

    static Interpolator curve(Context c, int id) {
        return AnimationUtils.loadInterpolator(c, id);
    }

    static int ms(Context c, int id) {
        return c.getResources().getInteger(id);
    }

    // Colour change that stays on screen (the hero): M3 emphasized easing, medium duration.
    static void tint(View v, int to) {
        ColorStateList now = v.getBackgroundTintList();
        int from = now == null ? to : now.getDefaultColor();
        if (from == to || !animate() || !v.isLaidOut()) { v.setBackgroundTintList(ColorStateList.valueOf(to)); return; }
        ValueAnimator va = ValueAnimator.ofArgb(from, to);
        va.setDuration(ms(v.getContext(), R.integer.motion_long2));
        va.setInterpolator(curve(v.getContext(), R.interpolator.emphasized));
        va.addUpdateListener(x -> v.setBackgroundTintList(ColorStateList.valueOf((int) x.getAnimatedValue())));
        va.start();
    }

    // A number that counts up to its value, once, when the screen opens.
    static void countUp(TextView t, int value) {
        NumberFormat f = NumberFormat.getInstance();
        if (!animate() || value < 2) { t.setText(f.format(value)); return; }
        ValueAnimator va = ValueAnimator.ofInt(0, value);
        va.setDuration(ms(t.getContext(), R.integer.motion_extra_long1));
        va.setInterpolator(curve(t.getContext(), R.interpolator.emphasized_decelerate));
        va.addUpdateListener(x -> t.setText(f.format((int) x.getAnimatedValue())));
        va.start();
    }

    // Things that enter: fade and rise a little, M3 emphasized decelerate.
    static void enter(View v, float dx, float dy) {
        v.setVisibility(View.VISIBLE);
        if (!animate()) return;
        v.setAlpha(0);
        v.setTranslationX(dx);
        v.setTranslationY(dy);
        v.animate().alpha(1).translationX(0).translationY(0).setDuration(ms(v.getContext(), R.integer.motion_long2))
            .setInterpolator(curve(v.getContext(), R.interpolator.emphasized_decelerate)).start();
    }

    // A status badge that pops in: scale from 0.6 with a small overshoot, like an M3 Expressive spring.
    static void pop(View v) {
        if (!animate()) return;
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.animate().scaleX(1).scaleY(1).setDuration(ms(v.getContext(), R.integer.motion_long2))
            .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
    }

    // M3 basic dialog with our layout. Back and a tap outside cancel.
    static Dialog dialog(Context c, int layout) {
        Dialog d = new Dialog(c, R.style.Theme_ReelBlock_Dialog);
        d.setContentView(layout);
        d.setCanceledOnTouchOutside(true);
        return d;
    }

    static Dialog info(Context c, int title, int body) {
        Dialog d = dialog(c, R.layout.dialog_info);
        ((TextView) d.findViewById(R.id.dialog_title)).setText(title);
        ((TextView) d.findViewById(R.id.dialog_body)).setText(body);
        d.findViewById(R.id.dialog_ok).setOnClickListener(v -> d.dismiss());
        d.show();
        return d;
    }
}
