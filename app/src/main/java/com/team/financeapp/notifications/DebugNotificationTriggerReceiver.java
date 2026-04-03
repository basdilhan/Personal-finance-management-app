package com.team.financeapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

/**
 * Debug-only broadcast trigger for testing notifications when the app process is not running.
 */
public class DebugNotificationTriggerReceiver extends BroadcastReceiver {

    public static final String ACTION_DEBUG_TRIGGER = "com.team.financeapp.DEBUG_TRIGGER_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        if (!ACTION_DEBUG_TRIGGER.equals(intent.getAction())) {
            return;
        }

        if (!isDebugBuild(context)) {
            return;
        }

        if (!FinancialNotificationHelper.canPost(context)) {
            return;
        }

        FinancialNotificationHelper.showReminderNotification(
                context,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                "Background test notification",
                "Triggered while app is closed."
        );
    }

    private boolean isDebugBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
