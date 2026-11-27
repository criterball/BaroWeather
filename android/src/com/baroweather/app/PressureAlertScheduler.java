package com.baroweather.app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

/** Schedules the periodic pressure warning check. */
public final class PressureAlertScheduler {
    private static final int JOB_ID = 7301;
    private static final long INTERVAL_MS = 6L * 60L * 60L * 1000L;

    private PressureAlertScheduler() {
    }

    public static void sync(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            return;
        }
        SettingsStore store = new SettingsStore(context);
        if (!store.getNotificationsEnabled()) {
            scheduler.cancel(JOB_ID);
            return;
        }
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, PressureAlertJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);
        if (Build.VERSION.SDK_INT >= 24) {
            builder.setPeriodic(INTERVAL_MS, 30L * 60L * 1000L);
        } else {
            builder.setPeriodic(INTERVAL_MS);
        }
        scheduler.cancel(JOB_ID);
        scheduler.schedule(builder.build());
    }
}
