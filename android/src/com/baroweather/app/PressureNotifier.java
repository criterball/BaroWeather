package com.baroweather.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Posts quiet, deduplicated pressure alerts using platform notification APIs. */
public final class PressureNotifier {
    private static final String CHANNEL_ID = "pressure_alerts";
    private static final int NOTIFICATION_ID = 7301;
    private static final long REPEAT_LIMIT_MS = 12L * 60L * 60L * 1000L;

    private PressureNotifier() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Pressure alerts", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Quiet alerts for incoming low or high pressure.");
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    public static void evaluateAndMaybeNotify(Context context, WeatherData weather) {
        SettingsStore store = new SettingsStore(context);
        if (!store.getNotificationsEnabled()) {
            return;
        }
        if (System.currentTimeMillis() < store.getAckUntil()) {
            return;
        }
        PressureEvaluation evaluation = PressureEvaluation.evaluate(weather, store);
        if (evaluation == null || evaluation.alert == null
                || !evaluation.alert.warning) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - store.getLastNotificationTime() < REPEAT_LIMIT_MS
                && store.getLastNotificationLevel() == evaluation.alert.level) {
            return;
        }
        if (!hasPermission(context)) {
            return;
        }
        createChannel(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        String place = store.getPlaceName();
        String pressure = NativeCore.formatPressureText(
                evaluation.currentHpa, store.getPressureUnit());
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent content = PendingIntent.getActivity(context, 0, open, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_LOW);
            builder.setSound(null);
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(evaluation.alert.title)
                .setContentText(place + " · " + pressure + " · " + evaluation.alert.message)
                .setStyle(new Notification.BigTextStyle().bigText(
                        place + "\n" + pressure + "\n" + evaluation.alert.message))
                .setContentIntent(content)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_EVENT)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        manager.notify(NOTIFICATION_ID, builder.build());
        store.recordNotification(evaluation.alert.level, now);
    }

    public static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
