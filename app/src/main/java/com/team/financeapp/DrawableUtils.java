package com.team.financeapp;

import android.content.res.Resources;
import android.widget.ImageView;
import android.view.View;

public final class DrawableUtils {

    private DrawableUtils() {
    }

    public static void safeSetImageResource(ImageView imageView, int resId, int fallbackResId) {
        if (imageView == null) {
            return;
        }
        try {
            imageView.setImageResource(resId);
        } catch (Resources.NotFoundException exception) {
            imageView.setImageResource(fallbackResId);
        }
    }

    public static void safeSetBackgroundResource(View view, int resId, int fallbackResId) {
        if (view == null) {
            return;
        }
        try {
            view.setBackgroundResource(resId);
        } catch (Resources.NotFoundException exception) {
            view.setBackgroundResource(fallbackResId);
        }
    }
}