package com.example.seniorhelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

final class AlarmScheduler {
    private static final String PREFS = "senior_helper_prefs";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MEDICINES = "medicines";
    private static final String KEY_WALKING_ALARM_ACTIVE = "walking_alarm_active";
    private static final String KEY_WALKING_ALARM_TIME = "walking_alarm_time";
    private static final String KEY_WALKING_ALARM_MINUTES = "walking_alarm_minutes";
    private static final String WALKING_ALARM_TITLE = "歩行アラーム";

    private AlarmScheduler() {
    }

    static void rescheduleAll(Context context) {
        long now = System.currentTimeMillis();
        rescheduleFromKey(context, KEY_EVENTS, now);
        rescheduleFromKey(context, KEY_MEDICINES, now);
        rescheduleWalkingAlarm(context, now);
    }

    static void schedule(Context context, String title, long timeMillis) {
        if (timeMillis <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = eventAlarmPendingIntent(context, title, timeMillis);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            }
        } catch (SecurityException ignored) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
        }
    }

    static void cancel(Context context, String title, long timeMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(eventAlarmPendingIntent(context, title, timeMillis));
        }
    }

    private static void rescheduleFromKey(Context context, String key, long now) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = preferences.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                long time = item.optLong("time", 0L);
                boolean confirmed = item.optBoolean("confirmed", false);
                String title = item.optString("title", "");
                if (!confirmed && !title.trim().isEmpty() && time > now) {
                    schedule(context, title, time);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void rescheduleWalkingAlarm(Context context, long now) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean active = preferences.getBoolean(KEY_WALKING_ALARM_ACTIVE, false);
        long time = preferences.getLong(KEY_WALKING_ALARM_TIME, 0L);
        if (!active || time <= now) {
            if (active) {
                preferences.edit()
                        .putBoolean(KEY_WALKING_ALARM_ACTIVE, false)
                        .remove(KEY_WALKING_ALARM_TIME)
                        .remove(KEY_WALKING_ALARM_MINUTES)
                        .apply();
            }
            return;
        }
        schedule(context, WALKING_ALARM_TITLE, time);
    }

    private static PendingIntent eventAlarmPendingIntent(Context context, String title, long timeMillis) {
        Intent intent = new Intent(context, EventAlarmReceiver.class);
        intent.putExtra(EventAlarmReceiver.EXTRA_TITLE, title);
        intent.putExtra(EventAlarmReceiver.EXTRA_TIME, MainichiTimeFormatter.formatEventTime(timeMillis));
        return PendingIntent.getBroadcast(
                context,
                (title + timeMillis).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
