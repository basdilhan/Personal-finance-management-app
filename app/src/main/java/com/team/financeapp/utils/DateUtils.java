package com.team.financeapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatMonth(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public static long normalizeEpochMillis(long timestamp) {
        // Adjusts seconds to milliseconds if necessary
        if (timestamp < 10000000000L) {
            return timestamp * 1000L;
        }
        return timestamp;
    }
}
