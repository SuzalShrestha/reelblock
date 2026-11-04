package dev.local.reelblock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import java.util.Calendar;

// Seven rounded bars: Reels closed per day, today last (on the right, or on the left in right-to-left
// languages). Colours and sizes are tokens; the text size follows the font scale. TalkBack reads one sentence.
public class WeekChart extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float barW, emptyD, chartH, gap;
    private final int colorBar, colorToday, colorEmpty, colorLabel;
    private int[] counts = new int[7];
    private final String[] days = new String[7];
    private float progress = 1;

    public WeekChart(Context c, AttributeSet a) {
        super(c, a);
        barW = getResources().getDimension(R.dimen.chart_bar_width);
        emptyD = getResources().getDimension(R.dimen.chart_empty_size);
        chartH = getResources().getDimension(R.dimen.chart_height);
        gap = getResources().getDimension(R.dimen.space_2);
        colorBar = c.getColor(R.color.chart_bar);
        colorToday = c.getColor(R.color.chart_bar_today);
        colorEmpty = c.getColor(R.color.chart_empty);
        colorLabel = c.getColor(R.color.text_secondary);
        label.setTextSize(getResources().getDimension(R.dimen.type_label_medium));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void setCounts(int[] week, boolean animate) {
        counts = week;
        // Narrow weekday names ("M", "T") under the bars and full names for TalkBack, in the phone's language.
        android.icu.text.DateFormatSymbols sym = new android.icu.text.DateFormatSymbols();
        String[] narrow = sym.getWeekdays(android.icu.text.DateFormatSymbols.FORMAT, android.icu.text.DateFormatSymbols.NARROW);
        String[] wide = sym.getWeekdays(android.icu.text.DateFormatSymbols.FORMAT, android.icu.text.DateFormatSymbols.WIDE);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        StringBuilder desc = new StringBuilder(getContext().getString(R.string.week_title)).append(". ");
        for (int i = 0; i < 7; i++) {
            days[i] = narrow[cal.get(Calendar.DAY_OF_WEEK)];
            String spoken = i == 6 ? getContext().getString(R.string.chart_today) : wide[cal.get(Calendar.DAY_OF_WEEK)];
            desc.append(getResources().getQuantityString(R.plurals.chart_day, counts[i], spoken, counts[i])).append(". ");
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        setContentDescription(desc);
        if (animate && ValueAnimator.areAnimatorsEnabled()) {
            ValueAnimator va = ValueAnimator.ofFloat(0, 1);
            va.setDuration(getResources().getInteger(R.integer.motion_extra_long1));
            va.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.emphasized_decelerate));
            va.addUpdateListener(x -> { progress = (float) x.getAnimatedValue(); invalidate(); });
            va.start();
        } else {
            progress = 1;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int w, int h) {
        Paint.FontMetrics fm = label.getFontMetrics();
        int height = (int) Math.ceil(chartH + gap + fm.descent - fm.ascent);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), w), resolveSize(height, h));
    }

    @Override
    protected void onDraw(Canvas c) {
        int max = 1;
        for (int n : counts) max = Math.max(max, n);
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / 7f;
        Paint.FontMetrics fm = label.getFontMetrics();
        for (int i = 0; i < 7; i++) {
            int pos = rtl ? 6 - i : i;
            float cx = getPaddingLeft() + slot * pos + slot / 2;
            boolean today = i == 6;
            if (counts[i] == 0) {
                bar.setColor(colorEmpty);
                c.drawCircle(cx, chartH - emptyD / 2, emptyD / 2, bar);
            } else {
                float h = Math.max(barW, chartH * counts[i] / max * progress);
                r.set(cx - barW / 2, chartH - h, cx + barW / 2, chartH);
                bar.setColor(today ? colorToday : colorBar);
                c.drawRoundRect(r, barW / 2, barW / 2, bar);
            }
            label.setColor(today ? colorToday : colorLabel);
            c.drawText(days[i] == null ? "" : days[i], cx, chartH + gap - fm.ascent, label);
        }
    }
}
