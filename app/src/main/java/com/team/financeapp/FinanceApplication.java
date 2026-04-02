package com.team.financeapp;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.os.Bundle;

import com.team.financeapp.data.sync.PendingSyncWorker;

/**
 * Initializes app-wide settings at process start.
 */
public class FinanceApplication extends Application {

    private int startedActivityCount;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferenceManager.applySavedTheme(this);
        PendingSyncWorker.schedule(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof AppLockActivity) {
                    return;
                }
                if (!AppLockManager.shouldRequireLock(activity)) {
                    return;
                }
                if (AppLockManager.isUnlockScreenVisible()) {
                    return;
                }

                AppLockManager.setUnlockScreenVisible(true);
                Intent intent = new Intent(activity, AppLockActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount = Math.max(0, startedActivityCount - 1);
                if (startedActivityCount == 0) {
                    AppLockManager.onAppBackgrounded();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            AppLockManager.onAppBackgrounded();
        }
    }
}
