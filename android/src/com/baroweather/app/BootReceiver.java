package com.baroweather.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores periodic checks and the cached widget after a device restart. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PressureNotifier.createChannel(context);
        PressureAlertScheduler.sync(context);
        WidgetUpdater.updateFromCache(context);
    }
}
