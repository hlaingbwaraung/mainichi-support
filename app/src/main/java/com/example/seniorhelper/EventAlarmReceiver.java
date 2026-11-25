package com.example.seniorhelper;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class EventAlarmReceiver extends BroadcastReceiver {
    static final String CHANNEL_ID = "reminder_alarm_v2";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TIME = "time";
    private static final String CHANNEL_NAME = "お知らせアラーム";
    private static final String CHANNEL_DESCRIPTION = "予定・薬・歩行のお知らせを音で通知します";
    private static final String PREFS = "senior_helper_prefs";
    private static final String WALKING_ALARM_TITLE = "歩行アラーム";
    private static final String KEY_WALKING_ALARM_ACTIVE = "walking_alarm_active";
    private static final String KEY_WALKING_ALARM_TIME = "walking_alarm_time";
    private static final String KEY_WALKING_ALARM_MINUTES = "walking_alarm_minutes";

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureNotificationChannel(context);

        String title = intent.getStringExtra(EXTRA_TITLE);
        String time = intent.getStringExtra(EXTRA_TIME);
        if (title == null || title.trim().isEmpty()) {
            title = "予定の時間です";
        }
        if (time == null) {
            time = "";
        }
        if (WALKING_ALARM_TITLE.equals(title)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_WALKING_ALARM_ACTIVE, false)
                    .remove(KEY_WALKING_ALARM_TIME)
                    .remove(KEY_WALKING_ALARM_MINUTES)
                    .apply();
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                title.hashCode(),
                openIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        Notification.Builder builder = new Notification.Builder(context)
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

    static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESCRIPTION);
        channel.enableVibration(true);
        channel.setSound(alarmSound, audioAttributes);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
