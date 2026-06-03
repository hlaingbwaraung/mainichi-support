package com.example.seniorhelper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class EventAlarmReceiver extends BroadcastReceiver {
    static final String CHANNEL_ID = "event_alarm";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TIME = "time";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String title = intent.getStringExtra(EXTRA_TITLE);
        String time = intent.getStringExtra(EXTRA_TIME);
        if (title == null || title.trim().isEmpty()) {
            title = "予定の時間です";
        }
        if (time == null) {
            time = "";
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                title.hashCode(),
                openIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        android.app.Notification.Builder builder = new android.app.Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(time)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setSound(alarmSound)
                .setPriority(android.app.Notification.PRIORITY_MAX)
                .setCategory(android.app.Notification.CATEGORY_ALARM);

        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId(CHANNEL_ID);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean canNotify = Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (manager != null && canNotify) {
            manager.notify((title + time).hashCode(), builder.build());
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "予定アラーム",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("予定の時間を通知音でお知らせします");
        channel.enableVibration(true);
        channel.setSound(alarmSound, null);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
