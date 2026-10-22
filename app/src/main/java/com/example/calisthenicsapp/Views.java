package com.example.calisthenicsapp;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small builders shared by the screens that create cards and labels in code. */
final class Views {

    private Views() {
    }

    /** dp to pixels on this device. */
    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** A plain background in one color with rounded corners; radius is in pixels. */
    static GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    /** A label; size is in sp. */
    static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    /** A small bold tag with a round background, as on the workout and exercise cards. */
    static TextView pill(Context context, String text, int backgroundColor, int textColor) {
        TextView view = text(context, text, 11, textColor, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        view.setBackground(rounded(backgroundColor, dp(context, 20)));
        view.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(context, 30)));
        return view;
    }
}
