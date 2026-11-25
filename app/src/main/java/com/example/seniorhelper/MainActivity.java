package com.example.seniorhelper;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {
    private enum Screen {
        HOME,
        STEP_COUNTER,
        WALKING_ALARM,
        NOTES,
        CALENDAR,
        EVENT_FORM,
        MEDICINES,
        MEDICINE_FORM,
        EMERGENCY,
        TODOS,
        SHOPPING,
        PREMIUM,
        SETTINGS,
        ONBOARDING_TEXT_SIZE,
        ONBOARDING_EMERGENCY,
        ONBOARDING_MEDICINE
    }

    private static final String PREFS = "senior_helper_prefs";
    private static final int REQUEST_ACTIVITY_RECOGNITION = 30;
    private static final int REQUEST_NOTIFICATIONS = 32;
    private static final String KEY_NOTES = "notes";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MEDICINES = "medicines";
    private static final String KEY_TODOS = "todos";
    private static final String KEY_SHOPPING = "shopping";
    private static final String KEY_EMERGENCY_NAME = "emergency_name";
    private static final String KEY_EMERGENCY_PHONE = "emergency_phone";
    private static final String KEY_STEP_BASE = "step_base";
    private static final String KEY_STEP_DATE = "step_date";
    private static final String KEY_STEP_HISTORY = "step_history";
    private static final String KEY_WALKING_ALARM_ACTIVE = "walking_alarm_active";
    private static final String KEY_WALKING_ALARM_TIME = "walking_alarm_time";
    private static final String KEY_WALKING_ALARM_MINUTES = "walking_alarm_minutes";
    private static final String KEY_PREMIUM_ACTIVE = "premium_active";
    private static final String KEY_TEXT_SCALE = "text_scale";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked";
    private static final String KEY_ACTIVITY_PERMISSION_ASKED = "activity_permission_asked";
    private static final String KEY_PREMIUM_PROMPT_LAST_SHOWN = "premium_prompt_last_shown";
    private static final long PREMIUM_PROMPT_INTERVAL_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    private static final String APP_OP_SCHEDULE_EXACT_ALARM = "android:schedule_exact_alarm";
    private static final String WALKING_ALARM_TITLE = "歩行アラーム";
    private static final int COLOR_BG = Color.rgb(248, 248, 245);
    private static final int COLOR_CARD = Color.rgb(255, 255, 255);
    private static final int COLOR_SURFACE_TINT = Color.rgb(240, 243, 239);
    private static final int COLOR_TEXT = Color.rgb(31, 43, 45);
    private static final int COLOR_MUTED = Color.rgb(87, 101, 101);
    private static final int COLOR_PRIMARY = Color.rgb(37, 91, 96);
    private static final int COLOR_SECONDARY = Color.rgb(87, 101, 101);
    private static final int COLOR_ACCENT = Color.rgb(166, 105, 28);
    private static final int COLOR_LINE = Color.rgb(218, 225, 220);
    private static final int COLOR_HOLIDAY = Color.rgb(179, 58, 48);
    private static final int COLOR_STEPS = Color.rgb(39, 104, 108);
    private static final int COLOR_NOTES = Color.rgb(100, 85, 124);
    private static final int COLOR_SCHEDULE = Color.rgb(145, 86, 50);
    private static final int COLOR_MEDICINE = Color.rgb(145, 72, 98);
    private static final int COLOR_EMERGENCY = Color.rgb(168, 61, 52);
    private static final int COLOR_TODO = Color.rgb(78, 104, 58);
    private static final int COLOR_SHOPPING = Color.rgb(47, 101, 137);

    private LinearLayout root;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountView;
    private TextView stepSensorStatusView;
    private StepGraphView stepGraphView;
    private final Handler promptHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady = false;
    private boolean premiumPromptScheduled = false;
    private boolean premiumPromptShown = false;
    private int stepsToday = 0;
    private int draftEventHour = -1;
    private int draftEventMinute = -1;
    private long draftEventDateMillis = -1L;
    private String draftEventTitle = "";
    private int draftMedicineHour = -1;
    private int draftMedicineMinute = -1;
    private String draftMedicineName = "";
    private PendingReminderSave pendingReminderSave;
    private Long pendingWalkingAlarmTimeMillis;
    private int pendingWalkingAlarmMinutes = -1;
    private Screen currentScreen = Screen.HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        textToSpeech = new TextToSpeech(this, this);
        createEventAlarmChannel();
        clearExpiredWalkingAlarm();
        scheduleAllEventAlarms();
        if (isOnboardingDone()) {
            showHome();
        } else {
            showOnboardingTextSize();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerStepSensor();
        resumePendingAlarmSetup();
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        promptHandler.removeCallbacksAndMessages(null);
        premiumPromptScheduled = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (navigateBackWithinApp()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int result = textToSpeech.setLanguage(Locale.JAPAN);
            textToSpeechReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    private void showHome() {
        currentScreen = Screen.HOME;
        beginScreen("まいにちサポート", "今日");
        addTodayHeader();
        addTodaySummary();
        root.addView(sectionTitle("使いたい機能"));
        LinearLayout featureGrid = new LinearLayout(this);
        featureGrid.setOrientation(LinearLayout.VERTICAL);
        featureGrid.setLayoutParams(matchWrap());
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("歩数計", "歩数計を開く", COLOR_STEPS, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStepCounter();
            }
        }),
                homeFeatureButton("歩行アラーム", "歩行アラームを開く", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWalkingAlarm();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("メモ帳", "メモ帳を開く", COLOR_NOTES, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotes();
            }
        }),
                homeFeatureButton("予定", "予定を開く", COLOR_SCHEDULE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalendar();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("薬アラーム", "薬アラームを開く", COLOR_MEDICINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMedicines();
            }
        }),
                homeFeatureButton("緊急連絡", "緊急連絡を開く", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmergency();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("今日やること", "今日やることを開く", COLOR_TODO, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTodos();
            }
        }),
                homeFeatureButton("買い物リスト", "買い物リストを開く", COLOR_SHOPPING, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShopping();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton(isPremiumActive() ? "プレミアム会員" : "広告を消す", "プレミアム会員画面を開く", COLOR_PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumScreen();
            }
        }),
                homeFeatureButton("読み上げ", "今日の内容を読み上げ", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakText(buildTodaySpeech());
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("設定", "文字サイズとバックアップ", COLOR_SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        }),
                homeFeatureButton("家族へ共有", "家族へ今日の内容を共有", COLOR_PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareToFamily(buildTodaySpeech());
            }
        }));
        root.addView(featureGrid);
        addAdBanner();
        root.post(new Runnable() {
            @Override
            public void run() {
                schedulePremiumPrompt();
            }
        });
    }

    private void showStepCounter() {
        currentScreen = Screen.STEP_COUNTER;
        beginScreen("歩数計", "今日の歩数を大きく表示");
        root.addView(backButton());

        addStepCounterPanel();
        if (needsActivityRecognitionPermission()) {
            root.addView(permissionCard(
                    "歩数の許可",
                    "歩数を自動で数えるには、このスマートフォンの歩数の許可が必要です。",
                    activityPermissionActionLabel(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleActivityPermissionAction();
                        }
                    }
            ));
        }
        addAdBanner();
    }

    private void showWalkingAlarm() {
        currentScreen = Screen.WALKING_ALARM;
        clearExpiredWalkingAlarm();
        beginScreen("歩行アラーム", "歩く時間を音と振動でお知らせ");
        root.addView(backButton());
        if (!hasNotificationPermission()) {
            root.addView(permissionCard(
                    "通知の許可",
                    "歩行アラームを音と振動でお知らせするには、通知の許可が必要です。",
                    notificationPermissionActionLabel(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleNotificationPermissionAction();
                        }
                    }
            ));
        }
        if (!hasExactAlarmAccess()) {
            root.addView(permissionCard(
                    "正確なアラーム",
                    "15分後などの時刻どおりにお知らせするには、アラームの特別な許可が必要です。許可がないと数分以上遅れることがあります。",
                    "設定を開く",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestExactAlarmAccess();
                        }
                    }
            ));
        }

        LinearLayout statusPanel = card();
        statusPanel.addView(sectionTitle("いまの設定"));
        if (hasActiveWalkingAlarm()) {
            long timeMillis = walkingAlarmTimeMillis();
            statusPanel.addView(bodyText("次のお知らせ: " + formatEventTime(timeMillis)));
            int minutesLeft = Math.max(1, (int) Math.ceil((timeMillis - System.currentTimeMillis()) / 60000d));
            statusPanel.addView(bodyText("あと " + minutesLeft + " 分です。"));
            statusPanel.addView(smallButton("歩行アラームを止める", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelWalkingAlarm();
                    Toast.makeText(MainActivity.this, "歩行アラームを止めました", Toast.LENGTH_LONG).show();
                    showWalkingAlarm();
                }
            }));
        } else {
            statusPanel.addView(bodyText("まだ歩行アラームはありません。"));
            statusPanel.addView(compactBodyText("15分、30分、60分から選べます。"));
        }
        root.addView(statusPanel);

        LinearLayout quickPanel = card();
        quickPanel.addView(sectionTitle("すぐ設定"));
        quickPanel.addView(bigButton("15分後", "15分後に歩行アラームを設定", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWalkingAlarmFlow(15);
            }
        }));
        quickPanel.addView(bigButton("30分後", "30分後に歩行アラームを設定", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWalkingAlarmFlow(30);
            }
        }));
        quickPanel.addView(bigButton("60分後", "60分後に歩行アラームを設定", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWalkingAlarmFlow(60);
            }
        }));
        root.addView(quickPanel);
        addAdBanner();
    }

    private void addTodayHeader() {
        Calendar today = Calendar.getInstance(Locale.JAPAN);
        boolean holiday = isJapaneseHoliday(today);
        boolean weekend = isWeekend(today);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.START);
        panel.setPadding(dp(16), dp(12), dp(16), dp(12));
        panel.setBackground(japaneseBox(COLOR_CARD, 16, 1, COLOR_LINE));
        panel.setElevation(dp(1));

        TextView date = bodyText("");
        date.setText(formatTodayHeader(today, holiday));
        date.setTextSize(scaledTextSize(19));
        date.setTypeface(Typeface.DEFAULT_BOLD);
        date.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        date.setTextColor(COLOR_TEXT);
        date.setPadding(0, 0, 0, 0);
        panel.addView(date, matchWrapWithBottom(4));

        TextView holidayStatus = bodyText(holiday ? "今日は祝日です。" : "今日は祝日ではありません。");
        holidayStatus.setTextSize(scaledTextSize(18));
        holidayStatus.setGravity(Gravity.START);
        holidayStatus.setTypeface(Typeface.DEFAULT_BOLD);
        holidayStatus.setTextColor(COLOR_TEXT);
        holidayStatus.setMaxLines(3);
        holidayStatus.setPadding(0, 0, 0, 0);
        holidayStatus.setContentDescription(holidayStatus.getText());
        panel.addView(holidayStatus, matchWrap());

        root.addView(panel, matchWrapWithBottom(10));
    }

    private CharSequence formatTodayHeader(Calendar calendar, boolean holiday) {
        String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String weekday = weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        String dayLabel = weekday + (holiday ? "・祝" : "");
        String value = String.format(Locale.JAPAN, "%d年%d月%d日（%s）", year, month, day, dayLabel);
        SpannableString styled = new SpannableString(value);
        if (holiday || isWeekend(calendar)) {
            int open = value.indexOf("（");
            int close = value.indexOf("）");
            if (open >= 0 && close > open + 1) {
                styled.setSpan(new ForegroundColorSpan(COLOR_HOLIDAY), open + 1, close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return styled;
    }

    private boolean isWeekend(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    private void addTodaySummary() {
        cleanupOldCompletedTodos();
        LinearLayout panel = card();
        panel.setPadding(dp(14), dp(10), dp(14), dp(12));
        panel.setMinimumHeight(0);
        TextView title = sectionTitle("今日のまとめ");
        title.setTextSize(scaledTextSize(20));
        panel.addView(title);

        List<CalendarEvent> todayEvents = todayItems(loadEvents());
        List<CalendarEvent> todayMedicines = todayItems(loadTimedList(KEY_MEDICINES));
        List<TodoItem> todos = loadTodos();
        int remainingTodos = 0;
        for (TodoItem todo : todos) {
            if (!todo.done) {
                remainingTodos++;
            }
        }

        panel.addView(compactBodyText(todayEvents.isEmpty() ? "今日の予定: ありません" : "今日の予定: " + formatTodayItemList(todayEvents)));
        panel.addView(compactBodyText(todayMedicines.isEmpty() ? "今日の薬: ありません" : "今日の薬: " + formatTodayItemList(todayMedicines)));
        panel.addView(compactBodyText(remainingTodos == 0 ? "今日やること: ありません" : "今日やること: 残り " + remainingTodos + " 件"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button read = smallButton("読み上げ", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakText(buildTodaySpeech());
            }
        });
        Button family = smallButton("家族へ共有", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareToFamily(buildTodaySpeech());
            }
        });
        read.setMinHeight(dp(48));
        family.setMinHeight(dp(48));
        read.setTextSize(scaledTextSize(18));
        family.setTextSize(scaledTextSize(18));
        if (prefersSingleColumnLayout()) {
            row.setOrientation(LinearLayout.VERTICAL);
            read.setLayoutParams(matchWrapWithBottom(8));
            family.setLayoutParams(matchWrap());
            row.addView(read);
            row.addView(family);
            panel.addView(row, matchWrap());
        } else {
            row.addView(read, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams familyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            familyParams.setMargins(dp(8), 0, 0, 0);
            row.addView(family, familyParams);
            panel.addView(row, matchWrap());
        }
        root.addView(panel);
    }

    private CalendarEvent nextFutureItem(List<CalendarEvent> items) {
        long now = System.currentTimeMillis();
        CalendarEvent next = null;
        for (CalendarEvent item : items) {
            if (item.confirmed || item.timeMillis < now) {
                continue;
            }
            if (next == null || item.timeMillis < next.timeMillis) {
                next = item;
            }
        }
        return next;
    }

    private List<CalendarEvent> todayItems(List<CalendarEvent> items) {
        List<CalendarEvent> today = new ArrayList<>();
        Calendar start = Calendar.getInstance(Locale.JAPAN);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startMillis = start.getTimeInMillis();
        start.add(Calendar.DAY_OF_YEAR, 1);
        long endMillis = start.getTimeInMillis();
        for (CalendarEvent item : items) {
            if (!item.confirmed && item.timeMillis >= startMillis && item.timeMillis < endMillis) {
                today.add(item);
            }
        }
        return today;
    }

    private String formatTodayItemList(List<CalendarEvent> items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            CalendarEvent item = items.get(i);
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(formatEventTimeOnly(item.timeMillis)).append("  ").append(item.title);
        }
        return builder.toString();
    }

    private void addStepCounterPanel() {
        LinearLayout panel = card();
        TextView title = bodyText("今日の歩数");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        stepCountView = bodyText(stepsToday + " 歩");
        stepCountView.setTextSize(scaledTextSize(34));
        stepCountView.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(stepCountView);

        stepSensorStatusView = bodyText(stepCounterMessage());
        panel.addView(stepSensorStatusView);

        TextView graphTitle = bodyText("7日間の歩数");
        graphTitle.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(graphTitle);

        stepGraphView = new StepGraphView(this);
        stepGraphView.setSteps(getWeeklySteps());
        panel.addView(stepGraphView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(170)
        ));

        root.addView(panel);
        updateStepCounterText();
    }

    private void clearExpiredWalkingAlarm() {
        long timeMillis = prefs().getLong(KEY_WALKING_ALARM_TIME, 0L);
        boolean active = prefs().getBoolean(KEY_WALKING_ALARM_ACTIVE, false);
        if (!active || timeMillis <= 0L || timeMillis > System.currentTimeMillis()) {
            return;
        }
        AlarmScheduler.cancel(this, WALKING_ALARM_TITLE, timeMillis);
        prefs().edit()
                .putBoolean(KEY_WALKING_ALARM_ACTIVE, false)
                .remove(KEY_WALKING_ALARM_TIME)
                .remove(KEY_WALKING_ALARM_MINUTES)
                .apply();
    }

    private String stepCounterMessage() {
        if (stepCounterSensor == null) {
            return "このスマートフォンは歩数計に対応していません";
        }
        if (Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return "歩数を数えるには許可が必要です";
        }
        return "スマートフォンの歩数センサーで自動で数えます";
    }

    private void updateStepCounterText() {
        if (stepCountView != null) {
            stepCountView.setText(String.format(Locale.JAPAN, "%d 歩", stepsToday));
        }
        if (stepSensorStatusView != null) {
            stepSensorStatusView.setText(stepCounterMessage());
        }
        if (stepGraphView != null) {
            stepGraphView.setSteps(getWeeklySteps());
        }
    }

    private void registerStepSensor() {
        if (sensorManager == null || stepCounterSensor == null) {
            updateStepCounterText();
            return;
        }
        if (Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            updateStepCounterText();
            return;
        }
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }
        int totalSteps = Math.round(event.values[0]);
        String today = todayKey();
        SharedPreferences preferences = prefs();
        String savedDate = preferences.getString(KEY_STEP_DATE, "");
        int baseSteps = preferences.getInt(KEY_STEP_BASE, -1);

        if (!today.equals(savedDate) || baseSteps < 0 || totalSteps < baseSteps) {
            baseSteps = totalSteps;
            preferences.edit()
                    .putString(KEY_STEP_DATE, today)
                    .putInt(KEY_STEP_BASE, baseSteps)
                    .apply();
        }

        stepsToday = Math.max(0, totalSteps - baseSteps);
        saveTodaySteps(stepsToday);
        updateStepCounterText();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
    }

    private boolean isJapaneseHoliday(Calendar date) {
        Calendar target = copyDate(date);
        if (isBaseJapaneseHoliday(target) || isCitizensHoliday(target)) {
            return true;
        }
        for (int i = 1; i <= 7; i++) {
            Calendar previous = copyDate(target);
            previous.add(Calendar.DAY_OF_MONTH, -i);
            if (!isBaseJapaneseHoliday(previous)) {
                continue;
            }
            if (previous.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                continue;
            }
            boolean blocked = false;
            Calendar between = copyDate(previous);
            between.add(Calendar.DAY_OF_MONTH, 1);
            while (between.before(target)) {
                if (!isBaseJapaneseHoliday(between) && !isCitizensHoliday(between)) {
                    blocked = true;
                    break;
                }
                between.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (!blocked) {
                return true;
            }
        }
        return false;
    }

    private boolean isBaseJapaneseHoliday(Calendar date) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        if (month == 1 && day == 1) return true;
        if (month == 1 && isNthWeekday(date, Calendar.MONDAY, 2)) return true;
        if (month == 2 && day == 11) return true;
        if (month == 2 && day == 23 && year >= 2020) return true;
        if (month == 3 && day == springEquinoxDay(year)) return true;
        if (month == 4 && day == 29) return true;
        if (month == 5 && (day == 3 || day == 4 || day == 5)) return true;
        if (month == 7 && isNthWeekday(date, Calendar.MONDAY, 3)) return true;
        if (month == 8 && day == 11) return true;
        if (month == 9 && isNthWeekday(date, Calendar.MONDAY, 3)) return true;
        if (month == 9 && day == autumnEquinoxDay(year)) return true;
        if (month == 10 && isNthWeekday(date, Calendar.MONDAY, 2)) return true;
        if (month == 11 && (day == 3 || day == 23)) return true;
        return false;
    }

    private boolean isCitizensHoliday(Calendar date) {
        Calendar previous = copyDate(date);
        previous.add(Calendar.DAY_OF_MONTH, -1);
        Calendar next = copyDate(date);
        next.add(Calendar.DAY_OF_MONTH, 1);
        return date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                && !isBaseJapaneseHoliday(date)
                && isBaseJapaneseHoliday(previous)
                && isBaseJapaneseHoliday(next);
    }

    private boolean isNthWeekday(Calendar date, int weekday, int nth) {
        return date.get(Calendar.DAY_OF_WEEK) == weekday
                && ((date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1) == nth;
    }

    private int springEquinoxDay(int year) {
        return (int) (20.8431 + 0.242194 * (year - 1980) - ((year - 1980) / 4));
    }

    private int autumnEquinoxDay(int year) {
        return (int) (23.2488 + 0.242194 * (year - 1980) - ((year - 1980) / 4));
    }

    private Calendar copyDate(Calendar source) {
        Calendar copy = Calendar.getInstance(Locale.JAPAN);
        copy.clear();
        copy.set(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH));
        return copy;
    }

    private void saveTodaySteps(int steps) {
        JSONObject history = loadStepHistory();
        try {
            history.put(todayKey(), steps);
            prefs().edit().putString(KEY_STEP_HISTORY, history.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private JSONObject loadStepHistory() {
        String raw = prefs().getString(KEY_STEP_HISTORY, "{}");
        try {
            return new JSONObject(raw);
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    private int[] getWeeklySteps() {
        int[] values = new int[7];
        JSONObject history = loadStepHistory();
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        for (int i = 0; i < values.length; i++) {
            String key = keyFormat.format(calendar.getTime());
            values[i] = history.optInt(key, 0);
            if (i == values.length - 1) {
                values[i] = Math.max(values[i], stepsToday);
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return values;
    }

    private void showNotes() {
        currentScreen = Screen.NOTES;
        beginScreen("メモ帳", "大きな文字でかんたんにメモ");
        root.addView(backButton());

        Button add = bigButton("メモを書く", "メモを書く", COLOR_NOTES, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog();
            }
        });
        root.addView(add);

        List<String> notes = loadNotes();
        if (notes.isEmpty()) {
            root.addView(bodyText("まだメモはありません。\n思いついたことをすぐ書けます。"));
            addAdBanner();
            return;
        }

        for (int i = 0; i < notes.size(); i++) {
            final int index = i;
            LinearLayout card = card();
            TextView noteText = bodyText(notes.get(i));
            card.addView(noteText);
            Button delete = smallButton("削除", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNote(index);
                    showNotes();
                }
            });
            card.addView(delete);
            root.addView(card);
        }
        addAdBanner();
    }

    private void showNoteDialog() {
        final EditText input = new EditText(this);
        input.setTextSize(scaledTextSize(24));
        input.setMinLines(4);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("メモを書く");
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_MUTED);
        input.setBackground(inputBackground());
        input.setPadding(dp(14), dp(10), dp(14), dp(10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新しいメモ")
                .setView(input)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();

        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) {
                    showInputError(input, "メモを入力してください");
                    return;
                }
                addNote(value);
                dialog.dismiss();
                hideKeyboard(input);
                showNotes();
            });
        });

        dialog.show();
        input.requestFocus();
        showKeyboard(input);
    }

    private void showMedicines() {
        currentScreen = Screen.MEDICINES;
        beginScreen("薬アラーム", "薬の時間を音でお知らせ");
        root.addView(backButton());
        if (!hasNotificationPermission()) {
            root.addView(permissionCard(
                    "通知の許可",
                    "薬の時間を音と振動でお知らせするには、通知の許可が必要です。",
                    notificationPermissionActionLabel(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleNotificationPermissionAction();
                        }
                    }
            ));
        }
        if (!hasExactAlarmAccess()) {
            root.addView(permissionCard(
                    "正確なアラーム",
                    "薬の時間を予定どおりに鳴らすには、アラームの特別な許可が必要です。許可がないと数分以上遅れることがあります。",
                    "設定を開く",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestExactAlarmAccess();
                        }
                    }
            ));
        }
        root.addView(bigButton("薬を追加", "薬アラームを追加", COLOR_MEDICINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMedicineDialog();
            }
        }));

        List<CalendarEvent> medicines = loadTimedList(KEY_MEDICINES);
        if (medicines.isEmpty()) {
            root.addView(bodyText("まだ薬アラームはありません"));
            addAdBanner();
            return;
        }
        for (int i = 0; i < medicines.size(); i++) {
            final int index = i;
            CalendarEvent medicine = medicines.get(i);
            LinearLayout item = card();
            TextView name = bodyText(medicine.title);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            item.addView(name);
            item.addView(bodyText(formatEventTime(medicine.timeMillis)));
            if (medicine.confirmed) {
                item.addView(bodyText("確認済みです"));
            } else {
                item.addView(smallButton("確認しました", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmTimedItem(KEY_MEDICINES, index, true);
                        showMedicines();
                    }
                }));
            }
            item.addView(smallButton("削除", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteTimedItem(KEY_MEDICINES, index);
                    showMedicines();
                }
            }));
            root.addView(item);
        }
        addAdBanner();
    }

    private void showMedicineDialog() {
        currentScreen = Screen.MEDICINE_FORM;
        beginScreen("薬を追加", "薬の時間を選んで保存");
        root.addView(smallButton("薬アラームへ戻る", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDraftMedicineTime();
                showMedicines();
            }
        }));

        LinearLayout panel = card();
        panel.addView(sectionTitle("薬の名前"));
        final EditText input = new EditText(this);
        configureLargeInputField(input, "例：朝の薬", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        setInitialInputText(input, draftMedicineName);
        panel.addView(input, matchWrap());

        panel.addView(sectionTitle("時間"));
        prepareDraftMedicineTime();

        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(inputBackground());
        panel.addView(timePanel, matchWrap());

        final TextView timeDisplay = bodyText("");
        timeDisplay.setTextSize(scaledTextSize(40));
        timeDisplay.setGravity(Gravity.CENTER);
        timeDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        timePanel.addView(timeDisplay, matchWrap());

        LinearLayout hourRow = timeAdjustRow("時");
        hourRow.addView(timeAdjustButton("-1", "時間を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineHour = (draftMedicineHour + 23) % 24;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        hourRow.addView(timeAdjustButton("+1", "時間を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineHour = (draftMedicineHour + 1) % 24;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        timePanel.addView(hourRow, matchWrap());

        LinearLayout minuteRow = timeAdjustRow("分");
        minuteRow.addView(timeAdjustButton("-1", "分を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineMinute = (draftMedicineMinute + 59) % 60;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        minuteRow.addView(timeAdjustButton("+1", "分を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineMinute = (draftMedicineMinute + 1) % 60;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        timePanel.addView(minuteRow, matchWrap());
        updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);

        panel.addView(bigButton("保存", "薬アラームを保存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    showInputError(input, "薬の名前を入力してください");
                    return;
                }
                draftMedicineName = name;
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(Calendar.HOUR_OF_DAY, draftMedicineHour);
                calendar.set(Calendar.MINUTE, draftMedicineMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                CalendarEvent medicine = new CalendarEvent("薬: " + name, calendar.getTimeInMillis());
                if (!hasNotificationPermission()) {
                    queuePendingReminderSave(KEY_MEDICINES, medicine, false);
                    hideKeyboard(input);
                    return;
                }
                if (!hasExactAlarmAccess()) {
                    queuePendingReminderSave(KEY_MEDICINES, medicine, false);
                    hideKeyboard(input);
                    return;
                }
                addTimedItem(KEY_MEDICINES, medicine);
                scheduleEventAlarm(medicine);
                resetDraftMedicineTime();
                hideKeyboard(input);
                Toast.makeText(MainActivity.this, "薬アラームを保存しました", Toast.LENGTH_LONG).show();
                showMedicines();
            }
        }));
        root.addView(panel);
        input.requestFocus();
    }

    private void showEmergency() {
        currentScreen = Screen.EMERGENCY;
        beginScreen("緊急連絡", "家族や病院へすぐ電話");
        root.addView(backButton());
        String name = prefs().getString(KEY_EMERGENCY_NAME, "");
        String phone = prefs().getString(KEY_EMERGENCY_PHONE, "");
        LinearLayout panel = card();
        String currentText;
        if (phone.isEmpty()) {
            currentText = "連絡先が未設定です";
        } else if (name.isEmpty()) {
            currentText = "登録番号: " + phone;
        } else {
            currentText = name + "\n" + phone;
        }
        TextView current = bodyText(currentText);
        current.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(current);
        panel.addView(bigButton("電話する", "登録した番号に電話", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callEmergencyNumber();
            }
        }));
        panel.addView(smallButton("連絡先を設定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmergencyPhoneDialog();
            }
        }));
        root.addView(panel);
        addAdBanner();
    }

    private void showEmergencyPhoneDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), 0, dp(16), 0);

        final EditText nameInput = new EditText(this);
        configureLargeInputField(nameInput, "名前 例：長男", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(prefs().getString(KEY_EMERGENCY_NAME, ""));
        layout.addView(nameInput, matchWrap());

        final EditText phoneInput = new EditText(this);
        configureLargeInputField(phoneInput, "電話番号 例：09012345678", InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(prefs().getString(KEY_EMERGENCY_PHONE, ""));
        layout.addView(phoneInput, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("緊急連絡先")
                .setView(layout)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                if (phone.isEmpty()) {
                    showInputError(phoneInput, "電話番号を入力してください");
                    return;
                }
                prefs().edit()
                        .putString(KEY_EMERGENCY_NAME, name)
                        .putString(KEY_EMERGENCY_PHONE, phone)
                        .apply();
                dialog.dismiss();
                hideKeyboard(phoneInput);
                showEmergency();
            });
        });
        dialog.show();
        nameInput.requestFocus();
        showKeyboard(nameInput);
    }

    private void callEmergencyNumber() {
        String phone = prefs().getString(KEY_EMERGENCY_PHONE, "");
        if (phone.isEmpty()) {
            showEmergencyPhoneDialog();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "電話アプリが見つかりません", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void showTodos() {
        currentScreen = Screen.TODOS;
        cleanupOldCompletedTodos();
        beginScreen("今日やること", "終わったら線で消します");
        root.addView(backButton());
        root.addView(bigButton("やることを追加", "やることを追加", COLOR_TODO, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSimpleItemDialog("今日やること", KEY_TODOS, "例：薬を飲む", true);
            }
        }));

        List<TodoItem> todos = loadTodos();
        if (todos.isEmpty()) {
            root.addView(bodyText("まだありません"));
            addAdBanner();
            return;
        }
        for (int i = 0; i < todos.size(); i++) {
            final int index = i;
            TodoItem todo = todos.get(i);
            LinearLayout item = card();
            TextView text = bodyText(todo.text);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            if (todo.done) {
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                text.setTextColor(COLOR_MUTED);
                item.addView(bodyText("完了しました。明日、自動で消えます。"));
            }
            item.addView(text, 0);
            item.addView(smallButton(todo.done ? "戻す" : "完了", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleTodoDone(index);
                    showTodos();
                }
            }));
            item.addView(smallButton("削除", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteTodo(index);
                    showTodos();
                }
            }));
            root.addView(item);
        }
        addAdBanner();
    }

    private void showShopping() {
        currentScreen = Screen.SHOPPING;
        beginScreen("買い物リスト", "名前・数・番号を大きく表示");
        LinearLayout topRow = new LinearLayout(this);
        boolean stackedTopButtons = prefersSingleColumnLayout();
        topRow.setOrientation(stackedTopButtons ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        Button back = backButton();
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                stackedTopButtons ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                stackedTopButtons ? 0f : 1f
        );
        if (stackedTopButtons) {
            backParams.setMargins(0, 0, 0, dp(8));
        }
        topRow.addView(back, backParams);
        Button clear = smallButton("全て削除", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("買い物リストを全て削除")
                        .setMessage("全ての買い物メモを消します。よろしいですか。")
                        .setPositiveButton("削除する", (confirmDialog, which) -> {
                            clearShoppingItems();
                            showShopping();
                        })
                        .setNegativeButton("キャンセル", null)
                        .create();
                dialog.setOnShowListener(d -> styleDialogButtons(dialog));
                dialog.show();
            }
        });
        applyButtonColors(clear, COLOR_EMERGENCY, COLOR_EMERGENCY);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                stackedTopButtons ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                stackedTopButtons ? 0f : 1f
        );
        if (!stackedTopButtons) {
            clearParams.setMargins(dp(8), 0, 0, 0);
        }
        topRow.addView(clear, clearParams);
        root.addView(topRow, matchWrapWithBottom(8));

        root.addView(bigButton("買う物を追加", "買う物を追加", COLOR_SHOPPING, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShoppingDialog();
            }
        }));
        List<ShoppingItem> items = loadShoppingItems();
        if (items.isEmpty()) {
            root.addView(bodyText("まだありません"));
            addAdBanner();
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            ShoppingItem value = items.get(i);
            LinearLayout item = card();
            TextView text = bodyText(formatShoppingItem(value));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            if (value.bought) {
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                text.setTextColor(COLOR_MUTED);
            }
            item.addView(text);
            item.addView(smallButton(value.bought ? "戻す" : "買った", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleShoppingBought(index);
                    showShopping();
                }
            }));
            item.addView(smallButton("削除", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteShoppingItem(index);
                    showShopping();
                }
            }));
            root.addView(item);
        }
        addAdBanner();
    }

    private String formatShoppingItem(ShoppingItem item) {
        StringBuilder builder = new StringBuilder(item.name);
        if (!item.amount.isEmpty()) {
            builder.append("\n数: ").append(item.amount);
        }
        if (!item.number.isEmpty()) {
            builder.append("\n番号: ").append(item.number);
        }
        return builder.toString();
    }

    private void showShoppingDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), 0, dp(16), 0);

        final EditText nameInput = new EditText(this);
        configureLargeInputField(nameInput, "名前 例：牛乳", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(nameInput, matchWrap());

        final EditText amountInput = new EditText(this);
        configureLargeInputField(amountInput, "数 例：2本", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(amountInput, matchWrap());

        final EditText numberInput = new EditText(this);
        configureLargeInputField(numberInput, "番号 例：3番", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(numberInput, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("買う物を追加")
                .setView(layout)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    showInputError(nameInput, "名前を入力してください");
                    return;
                }
                addShoppingItem(new ShoppingItem(
                        name,
                        amountInput.getText().toString().trim(),
                        numberInput.getText().toString().trim(),
                        false
                ));
                dialog.dismiss();
                hideKeyboard(nameInput);
                showShopping();
            });
        });
        dialog.show();
        nameInput.requestFocus();
        showKeyboard(nameInput);
    }

    private void showSimpleItemDialog(String title, String key, String hint, boolean doneMode) {
        final EditText input = new EditText(this);
        configureLargeInputField(input, hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) {
                    showInputError(input, "入力してください");
                    return;
                }
                if (doneMode) {
                    addTodo(value);
                } else {
                    addStringItem(key, value);
                }
                dialog.dismiss();
                hideKeyboard(input);
                if (doneMode) {
                    showTodos();
                } else {
                    showShopping();
                }
            });
        });
        dialog.show();
        input.requestFocus();
        showKeyboard(input);
    }

    private void showCalendar() {
        currentScreen = Screen.CALENDAR;
        beginScreen("予定", "かんたんな予定のお知らせ");
        root.addView(backButton());
        if (!hasNotificationPermission()) {
            root.addView(permissionCard(
                    "通知の許可",
                    "予定の時間を確実にお知らせするには、通知の許可が必要です。",
                    notificationPermissionActionLabel(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleNotificationPermissionAction();
                        }
                    }
            ));
        }
        if (!hasExactAlarmAccess()) {
            root.addView(permissionCard(
                    "正確なアラーム",
                    "病院や予定の時刻どおりにお知らせするには、アラームの特別な許可が必要です。許可がないと数分以上遅れることがあります。",
                    "設定を開く",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestExactAlarmAccess();
                        }
                    }
            ));
        }

        root.addView(bigButton("予定を追加", "予定のお知らせを追加", COLOR_SCHEDULE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEventForm();
            }
        }));
        root.addView(bodyText("予定の時間になると通知音でお知らせします"));

        List<CalendarEvent> events = loadEvents();
        if (events.isEmpty()) {
            root.addView(bodyText("まだ予定はありません"));
            addAdBanner();
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            final int index = i;
            CalendarEvent event = events.get(i);
            LinearLayout card = card();
            TextView title = bodyText(event.title);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(title);
            card.addView(bodyText(formatEventTime(event.timeMillis)));
            if (event.confirmed) {
                card.addView(bodyText("確認済みです"));
            } else {
                card.addView(smallButton("確認しました", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmEvent(index);
                        showCalendar();
                    }
                }));
            }
            Button delete = smallButton("削除", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteEvent(index);
                    showCalendar();
                }
            });
            card.addView(delete);
            root.addView(card);
        }
        addAdBanner();
    }

    private void showEventForm() {
        currentScreen = Screen.EVENT_FORM;
        beginScreen("予定を追加", "日付と時間を選んで保存");
        root.addView(backToCalendarButton());

        LinearLayout panel = card();

        TextView nameLabel = sectionTitle("予定の名前");
        panel.addView(nameLabel);

        final EditText title = new EditText(this);
        configureLargeInputField(title, "例：病院", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        title.setSingleLine(false);
        title.setMinLines(1);
        setInitialInputText(title, draftEventTitle);
        panel.addView(title, matchWrap());

        TextView dateLabel = sectionTitle("日付");
        panel.addView(dateLabel);
        prepareDraftEventDate();

        LinearLayout datePanel = new LinearLayout(this);
        datePanel.setOrientation(LinearLayout.VERTICAL);
        datePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        datePanel.setBackground(inputBackground());
        panel.addView(datePanel, matchWrap());

        final TextView dateDisplay = bodyText("");
        dateDisplay.setTextSize(scaledTextSize(28));
        dateDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        dateDisplay.setGravity(Gravity.CENTER);
        datePanel.addView(dateDisplay, matchWrap());

        final boolean stackedDateButtons = prefersSingleColumnLayout();
        LinearLayout quickDateRow = new LinearLayout(this);
        quickDateRow.setOrientation(stackedDateButtons ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        quickDateRow.addView(dateAdjustButton("今日", "日付を今日にする", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDraftEventDateDaysFromToday(0);
                updateEventDateDisplay(dateDisplay);
            }
        }), weightedDateButtonParams(stackedDateButtons));
        quickDateRow.addView(dateAdjustButton("明日", "日付を明日にする", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDraftEventDateDaysFromToday(1);
                updateEventDateDisplay(dateDisplay);
            }
        }), weightedDateButtonParams(stackedDateButtons));
        quickDateRow.addView(dateAdjustButton("1週間後", "日付を1週間後にする", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDraftEventDateDaysFromToday(7);
                updateEventDateDisplay(dateDisplay);
            }
        }), weightedDateButtonParams(stackedDateButtons));
        datePanel.addView(quickDateRow, matchWrap());

        LinearLayout moveDateRow = new LinearLayout(this);
        moveDateRow.setOrientation(stackedDateButtons ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        moveDateRow.addView(dateAdjustButton("前の日", "日付を1日前にする", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftDraftEventDate(-1);
                updateEventDateDisplay(dateDisplay);
            }
        }), weightedDateButtonParams(stackedDateButtons));
        moveDateRow.addView(dateAdjustButton("次の日", "日付を1日後にする", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftDraftEventDate(1);
                updateEventDateDisplay(dateDisplay);
            }
        }), weightedDateButtonParams(stackedDateButtons));
        datePanel.addView(moveDateRow, matchWrap());
        updateEventDateDisplay(dateDisplay);

        TextView timeLabel = sectionTitle("時間");
        panel.addView(timeLabel);

        prepareDraftEventTime();

        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(inputBackground());
        panel.addView(timePanel, matchWrap());

        final TextView timeDisplay = bodyText("");
        timeDisplay.setTextSize(scaledTextSize(40));
        timeDisplay.setGravity(Gravity.CENTER);
        timeDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        timePanel.addView(timeDisplay, matchWrap());

        LinearLayout hourRow = timeAdjustRow("時");
        hourRow.addView(timeAdjustButton("-1", "時間を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftEventHour = (draftEventHour + 23) % 24;
                updateTimeDisplay(timeDisplay, draftEventHour, draftEventMinute);
            }
        }));
        hourRow.addView(timeAdjustButton("+1", "時間を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftEventHour = (draftEventHour + 1) % 24;
                updateTimeDisplay(timeDisplay, draftEventHour, draftEventMinute);
            }
        }));
        timePanel.addView(hourRow, matchWrap());

        LinearLayout minuteRow = timeAdjustRow("分");
        minuteRow.addView(timeAdjustButton("-1", "分を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftEventMinute = (draftEventMinute + 59) % 60;
                updateTimeDisplay(timeDisplay, draftEventHour, draftEventMinute);
            }
        }));
        minuteRow.addView(timeAdjustButton("+1", "分を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftEventMinute = (draftEventMinute + 1) % 60;
                updateTimeDisplay(timeDisplay, draftEventHour, draftEventMinute);
            }
        }));
        timePanel.addView(minuteRow, matchWrap());
        updateTimeDisplay(timeDisplay, draftEventHour, draftEventMinute);

        Button save = bigButton("保存", "予定を保存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = title.getText().toString().trim();
                if (name.isEmpty()) {
                    showInputError(title, "名前を入力してください");
                    return;
                }
                draftEventTitle = name;
                Calendar calendar = draftEventDateCalendar();
                calendar.set(Calendar.HOUR_OF_DAY, draftEventHour);
                calendar.set(Calendar.MINUTE, draftEventMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(MainActivity.this, "今より後の時間を選んでください", Toast.LENGTH_LONG).show();
                    return;
                }
                CalendarEvent event = new CalendarEvent(name, calendar.getTimeInMillis());
                if (!hasNotificationPermission()) {
                    queuePendingReminderSave(KEY_EVENTS, event, false);
                    hideKeyboard(title);
                    return;
                }
                if (!hasExactAlarmAccess()) {
                    queuePendingReminderSave(KEY_EVENTS, event, false);
                    hideKeyboard(title);
                    return;
                }
                addEvent(event);
                scheduleEventAlarm(event);
                resetDraftEventTime();
                hideKeyboard(title);
                Toast.makeText(MainActivity.this, "予定アラームを保存しました", Toast.LENGTH_LONG).show();
                showCalendar();
            }
        });
        panel.addView(save);

        root.addView(panel);
    }

    private void showPremiumScreen() {
        currentScreen = Screen.PREMIUM;
        beginScreen("プレミアム", "月額300円で広告なし");
        root.addView(backButton());

        LinearLayout panel = card();
        TextView title = bodyText("プレミアム会員");
        title.setTextSize(scaledTextSize(30));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);
        panel.addView(bodyText("月額 300円"));
        panel.addView(bodyText("Google広告を表示しません。"));
        panel.addView(bodyText("すべての機能をそのまま使えます。"));
        root.addView(panel);

        final boolean premiumActive = isPremiumActive();
        Button subscribe = bigButton(premiumActive ? "登録済みです" : "お申し込みは準備中です",
                premiumActive ? "プレミアム会員に登録済みです" : "プレミアム会員のお申し込みは準備中です",
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumUnavailable();
            }
        });
        applyButtonColors(subscribe, premiumActive ? COLOR_SECONDARY : COLOR_ACCENT, COLOR_ACCENT);
        subscribe.setEnabled(!premiumActive);
        subscribe.setAlpha(premiumActive ? 0.65f : 1f);
        root.addView(subscribe);

        if (premiumActive) {
            root.addView(bodyText("現在、広告は非表示です。"));
        } else {
            root.addView(bodyText("お申し込み機能は現在準備中です。"));
            addAdBanner();
        }
    }

    private void showSettings() {
        currentScreen = Screen.SETTINGS;
        beginScreen("設定", "文字サイズとバックアップ");
        root.addView(backButton());

        LinearLayout sizePanel = card();
        sizePanel.addView(sectionTitle("文字サイズ"));
        sizePanel.addView(bodyText("見やすい大きさを選べます。"));
        LinearLayout sizeRow = new LinearLayout(this);
        boolean stackedSizeButtons = prefersSingleColumnLayout();
        sizeRow.setOrientation(stackedSizeButtons ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        sizeRow.addView(settingSizeButton("大", 1.0f), settingsSizeButtonParams(stackedSizeButtons, false));
        sizeRow.addView(settingSizeButton("特大", 1.18f), settingsSizeButtonParams(stackedSizeButtons, true));
        sizeRow.addView(settingSizeButton("最大", 1.32f), settingsSizeButtonParams(stackedSizeButtons, false));
        sizePanel.addView(sizeRow, matchWrap());
        root.addView(sizePanel);

        LinearLayout backupPanel = card();
        backupPanel.addView(sectionTitle("バックアップ"));
        backupPanel.addView(bodyText("メモ、予定、薬、今日やること、連絡先、買い物リストを共有またはコピーできます。"));
        backupPanel.addView(bigButton("バックアップを共有", "バックアップを共有", COLOR_PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBackup();
            }
        }));
        backupPanel.addView(smallButton("バックアップをコピー", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyBackup();
            }
        }));
        root.addView(backupPanel);
        addAdBanner();
    }

    private void showOnboardingTextSize() {
        currentScreen = Screen.ONBOARDING_TEXT_SIZE;
        beginScreen("はじめての設定", "見やすい大きさを選びます");

        LinearLayout panel = card();
        panel.addView(sectionTitle("文字サイズ"));
        panel.addView(bodyText("あとから設定で変えられます。まず見やすい大きさを選んでください。"));

        panel.addView(bigButton("大", "文字サイズを大にする", COLOR_SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOnboardingTextSize(1.0f);
            }
        }));
        panel.addView(bigButton("特大", "文字サイズを特大にする", COLOR_PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOnboardingTextSize(1.18f);
            }
        }));
        panel.addView(bigButton("最大", "文字サイズを最大にする", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOnboardingTextSize(1.32f);
            }
        }));
        root.addView(panel);
    }

    private void saveOnboardingTextSize(float scale) {
        prefs().edit().putFloat(KEY_TEXT_SCALE, scale).apply();
        showOnboardingEmergency();
    }

    private void showOnboardingEmergency() {
        currentScreen = Screen.ONBOARDING_EMERGENCY;
        beginScreen("緊急連絡先", "困った時にすぐ電話できます");

        LinearLayout panel = card();
        panel.addView(sectionTitle("家族や病院の連絡先"));

        final EditText nameInput = new EditText(this);
        configureLargeInputField(nameInput, "名前 例：長男", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(prefs().getString(KEY_EMERGENCY_NAME, ""));
        panel.addView(nameInput, matchWrap());

        final EditText phoneInput = new EditText(this);
        configureLargeInputField(phoneInput, "電話番号 例：09012345678", InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(prefs().getString(KEY_EMERGENCY_PHONE, ""));
        panel.addView(phoneInput, matchWrap());

        panel.addView(bigButton("保存して次へ", "緊急連絡先を保存して次へ", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = phoneInput.getText().toString().trim();
                if (phone.isEmpty()) {
                    showInputError(phoneInput, "電話番号を入力してください");
                    return;
                }
                prefs().edit()
                        .putString(KEY_EMERGENCY_NAME, nameInput.getText().toString().trim())
                        .putString(KEY_EMERGENCY_PHONE, phone)
                        .apply();
                hideKeyboard(phoneInput);
                showOnboardingMedicine();
            }
        }));
        panel.addView(smallButton("あとで設定する", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(nameInput);
                showOnboardingMedicine();
            }
        }));
        root.addView(panel);
        nameInput.requestFocus();
    }

    private void showOnboardingMedicine() {
        currentScreen = Screen.ONBOARDING_MEDICINE;
        beginScreen("薬の時間", "毎日の薬を忘れにくくします");

        LinearLayout panel = card();
        panel.addView(sectionTitle("薬アラーム"));
        if (!hasExactAlarmAccess()) {
            panel.addView(compactBodyText("時間どおりに鳴らすには、あとでアラームの特別な許可が必要です。"));
            panel.addView(smallButton("設定を開く", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestExactAlarmAccess();
                }
            }));
        }

        final EditText medicineInput = new EditText(this);
        configureLargeInputField(medicineInput, "例：朝の薬", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        setInitialInputText(medicineInput, draftMedicineName);
        panel.addView(medicineInput, matchWrap());
        panel.addView(smallButton("あとで設定する", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDraftMedicineTime();
                hideKeyboard(medicineInput);
                finishOnboarding();
            }
        }));

        prepareDraftMedicineTime();
        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(inputBackground());
        panel.addView(timePanel, matchWrap());

        final TextView timeDisplay = bodyText("");
        timeDisplay.setTextSize(scaledTextSize(40));
        timeDisplay.setGravity(Gravity.CENTER);
        timeDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        timePanel.addView(timeDisplay, matchWrap());

        LinearLayout hourRow = timeAdjustRow("時");
        hourRow.addView(timeAdjustButton("-1", "時間を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineHour = (draftMedicineHour + 23) % 24;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        hourRow.addView(timeAdjustButton("+1", "時間を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineHour = (draftMedicineHour + 1) % 24;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        timePanel.addView(hourRow, matchWrap());

        LinearLayout minuteRow = timeAdjustRow("分");
        minuteRow.addView(timeAdjustButton("-1", "分を1つ下げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineMinute = (draftMedicineMinute + 59) % 60;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        minuteRow.addView(timeAdjustButton("+1", "分を1つ上げる", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                draftMedicineMinute = (draftMedicineMinute + 1) % 60;
                updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);
            }
        }));
        timePanel.addView(minuteRow, matchWrap());
        updateTimeDisplay(timeDisplay, draftMedicineHour, draftMedicineMinute);

        panel.addView(bigButton("保存して始める", "薬アラームを保存して始める", COLOR_MEDICINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = medicineInput.getText().toString().trim();
                if (name.isEmpty()) {
                    showInputError(medicineInput, "薬の名前を入力してください");
                    return;
                }
                draftMedicineName = name;
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(Calendar.HOUR_OF_DAY, draftMedicineHour);
                calendar.set(Calendar.MINUTE, draftMedicineMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                CalendarEvent medicine = new CalendarEvent("薬: " + name, calendar.getTimeInMillis());
                if (!hasNotificationPermission()) {
                    queuePendingReminderSave(KEY_MEDICINES, medicine, true);
                    hideKeyboard(medicineInput);
                    return;
                }
                if (!hasExactAlarmAccess()) {
                    queuePendingReminderSave(KEY_MEDICINES, medicine, true);
                    hideKeyboard(medicineInput);
                    return;
                }
                addTimedItem(KEY_MEDICINES, medicine);
                scheduleEventAlarm(medicine);
                resetDraftMedicineTime();
                hideKeyboard(medicineInput);
                finishOnboarding();
            }
        }));
        root.addView(panel);
        medicineInput.requestFocus();
    }

    private void finishOnboarding() {
        prefs().edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply();
        Toast.makeText(this, "設定が完了しました", Toast.LENGTH_LONG).show();
        showHome();
    }

    private Button settingSizeButton(String label, final float scale) {
        Button button = smallButton(label, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs().edit().putFloat(KEY_TEXT_SCALE, scale).apply();
                Toast.makeText(MainActivity.this, "文字サイズを変更しました", Toast.LENGTH_SHORT).show();
                showSettings();
            }
        });
        if (Math.abs(textScale() - scale) < 0.02f) {
            applyButtonColors(button, COLOR_ACCENT, COLOR_ACCENT);
        }
        return button;
    }

    private LinearLayout.LayoutParams settingsSizeButtonParams(boolean stacked, boolean middle) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                stacked ? 0f : 1f
        );
        if (stacked) {
            params.setMargins(0, 0, 0, dp(8));
        } else if (middle) {
            params.setMargins(dp(8), 0, dp(8), 0);
        }
        return params;
    }

    private void beginScreen(String title, String subtitle) {
        FrameLayout screen = new FrameLayout(this);
        screen.addView(new WashiBackgroundView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setPadding(0, getStatusBarHeight(), 0, 0);
        scrollView.setClipToPadding(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(36));
        root.setBackgroundColor(Color.TRANSPARENT);
        scrollView.addView(root, matchWrap());

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutParams(matchWrap());

        View accent = new View(this);
        accent.setBackground(japaneseBox(COLOR_ACCENT, 3, 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(5), dp(36));
        accentParams.setMargins(0, 0, dp(12), 0);
        titleRow.addView(accent, accentParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(scaledTextSize(30));
        titleView.setTextColor(COLOR_TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.START);
        titleView.setMaxLines(2);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        root.addView(titleRow);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(scaledTextSize(20));
        subtitleView.setTextColor(COLOR_MUTED);
        root.addView(subtitleView, matchWrapWithBottom(22));

        screen.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(screen);
    }

    private Button bigButton(String label, String contentDescription, View.OnClickListener listener) {
        return bigButton(label, contentDescription, COLOR_PRIMARY, listener);
    }

    private Button bigButton(String label, String contentDescription, int color, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(scaledTextSize(24));
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        applyButtonColors(button, color, COLOR_ACCENT);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(78));
        button.setContentDescription(contentDescription);
        button.setOnClickListener(listener);
        button.setPadding(dp(16), dp(12), dp(16), dp(12));
        button.setElevation(dp(1));
        button.setLayoutParams(matchWrap());
        return button;
    }

    private Button homeFeatureButton(String label, String contentDescription, int color, View.OnClickListener listener) {
        Button button = bigButton(label, contentDescription, color, listener);
        button.setTextSize(scaledTextSize(20));
        button.setMinHeight(dp(112));
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setGravity(Gravity.CENTER);
        button.setLineSpacing(0, 1.05f);
        button.setPadding(dp(12), dp(18), dp(12), dp(18));
        int fillColor = blendColors(color, COLOR_CARD, 0.88f);
        int borderColor = blendColors(color, COLOR_CARD, 0.55f);
        applyButtonColors(button, fillColor, borderColor);
        button.setTextColor(color);
        button.setElevation(0);
        return button;
    }

    private void addHomeFeatureRow(LinearLayout grid, Button left, Button right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(matchWrapWithBottom(12));

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMargins(0, 0, dp(6), 0);
        left.setLayoutParams(leftParams);
        row.addView(left);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMargins(dp(6), 0, 0, 0);
        right.setLayoutParams(rightParams);
        row.addView(right);

        grid.addView(row);
    }

    private Button smallButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(scaledTextSize(19));
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        applyButtonColors(button, COLOR_SURFACE_TINT, COLOR_LINE);
        button.setTextColor(COLOR_TEXT);
        button.setMinHeight(dp(56));
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setOnClickListener(listener);
        button.setLayoutParams(matchWrap());
        return button;
    }

    private void applyButtonColors(Button button, int fillColor, int strokeColor) {
        button.setTextColor(buttonTextColor(fillColor));
        StateListDrawable states = new StateListDrawable();
        int pressedFill = Color.alpha(fillColor) == 0
                ? Color.argb(28, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
                : blendColors(fillColor, COLOR_TEXT, 0.12f);
        states.addState(new int[]{android.R.attr.state_pressed}, japaneseBox(pressedFill, 14, 1, strokeColor));
        states.addState(new int[]{-android.R.attr.state_enabled},
                japaneseBox(blendColors(fillColor, COLOR_BG, 0.45f), 14, 1, COLOR_LINE));
        states.addState(new int[]{}, japaneseBox(fillColor, 14, 1, strokeColor));
        button.setBackground(states);
    }

    private int buttonTextColor(int fillColor) {
        int brightness = (Color.red(fillColor) * 299 + Color.green(fillColor) * 587 + Color.blue(fillColor) * 114) / 1000;
        return brightness >= 135 ? COLOR_TEXT : Color.WHITE;
    }

    private int blendColors(int fromColor, int toColor, float amount) {
        float safeAmount = Math.max(0f, Math.min(1f, amount));
        int alpha = Math.round(Color.alpha(fromColor) + (Color.alpha(toColor) - Color.alpha(fromColor)) * safeAmount);
        int red = Math.round(Color.red(fromColor) + (Color.red(toColor) - Color.red(fromColor)) * safeAmount);
        int green = Math.round(Color.green(fromColor) + (Color.green(toColor) - Color.green(fromColor)) * safeAmount);
        int blue = Math.round(Color.blue(fromColor) + (Color.blue(toColor) - Color.blue(fromColor)) * safeAmount);
        return Color.argb(alpha, red, green, blue);
    }

    private void configureLargeInputField(EditText input, String hint, int inputType) {
        input.setTextSize(scaledTextSize(24));
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_MUTED);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackground(inputBackground());
        if (prefersSingleColumnLayout()) {
            input.setSingleLine(false);
            input.setMaxLines(2);
            input.setHorizontallyScrolling(false);
            input.setMinHeight(dp(92));
        } else {
            input.setSingleLine(true);
            input.setMinHeight(dp(72));
        }
    }

    private void setInitialInputText(EditText input, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        input.setText(value);
        input.setSelection(value.length());
    }

    private LinearLayout permissionCard(String title, String message, String buttonLabel, View.OnClickListener listener) {
        LinearLayout card = card();
        card.addView(sectionTitle(title));
        card.addView(bodyText(message));
        card.addView(smallButton(buttonLabel, listener));
        return card;
    }

    private LinearLayout timeAdjustRow(String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = bodyText(label);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        row.addView(labelView, new LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private Button timeAdjustButton(String label, String contentDescription, View.OnClickListener listener) {
        Button button = smallButton(label, listener);
        button.setTextSize(scaledTextSize(24));
        button.setContentDescription(contentDescription);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1f);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private Button dateAdjustButton(String label, String contentDescription, View.OnClickListener listener) {
        Button button = smallButton(label, listener);
        button.setTextSize(scaledTextSize(18));
        button.setContentDescription(contentDescription);
        button.setMinHeight(dp(64));
        return button;
    }

    private LinearLayout.LayoutParams weightedDateButtonParams(boolean stacked) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                stacked ? 0f : 1f
        );
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        return params;
    }

    private void updateTimeDisplay(TextView display, int hour, int minute) {
        display.setText(String.format(Locale.JAPAN, "%02d:%02d", hour, minute));
    }

    private void updateEventDateDisplay(TextView display) {
        Calendar calendar = draftEventDateCalendar();
        String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String weekday = weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        int offset = daysFromToday(calendar);
        String suffix;
        if (offset == 0) {
            suffix = "今日";
        } else if (offset == 1) {
            suffix = "明日";
        } else {
            suffix = offset + "日後";
        }
        display.setText(String.format(Locale.JAPAN, "%d年%d月%d日（%s）\n%s", year, month, day, weekday, suffix));
    }

    private void prepareDraftEventTime() {
        if (draftEventHour >= 0 && draftEventMinute >= 0) {
            return;
        }
        Calendar initialTime = Calendar.getInstance(Locale.JAPAN);
        initialTime.set(Calendar.SECOND, 0);
        initialTime.set(Calendar.MILLISECOND, 0);
        initialTime.add(Calendar.MINUTE, 10);
        draftEventHour = initialTime.get(Calendar.HOUR_OF_DAY);
        draftEventMinute = initialTime.get(Calendar.MINUTE);
    }

    private void resetDraftEventTime() {
        draftEventHour = -1;
        draftEventMinute = -1;
        draftEventDateMillis = -1L;
        draftEventTitle = "";
    }

    private void prepareDraftEventDate() {
        if (draftEventDateMillis > 0L) {
            return;
        }
        draftEventDateMillis = startOfToday().getTimeInMillis();
    }

    private void setDraftEventDateDaysFromToday(int daysFromToday) {
        Calendar calendar = startOfToday();
        calendar.add(Calendar.DAY_OF_YEAR, daysFromToday);
        draftEventDateMillis = calendar.getTimeInMillis();
    }

    private void shiftDraftEventDate(int deltaDays) {
        Calendar calendar = draftEventDateCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, deltaDays);
        Calendar today = startOfToday();
        if (calendar.before(today)) {
            calendar = today;
        }
        draftEventDateMillis = calendar.getTimeInMillis();
    }

    private Calendar draftEventDateCalendar() {
        prepareDraftEventDate();
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        calendar.setTimeInMillis(draftEventDateMillis);
        return calendar;
    }

    private Calendar startOfToday() {
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private int daysFromToday(Calendar target) {
        long diff = target.getTimeInMillis() - startOfToday().getTimeInMillis();
        return (int) (diff / (24L * 60L * 60L * 1000L));
    }

    private void prepareDraftMedicineTime() {
        if (draftMedicineHour >= 0 && draftMedicineMinute >= 0) {
            return;
        }
        Calendar initialTime = Calendar.getInstance(Locale.JAPAN);
        initialTime.set(Calendar.SECOND, 0);
        initialTime.set(Calendar.MILLISECOND, 0);
        initialTime.add(Calendar.MINUTE, 10);
        draftMedicineHour = initialTime.get(Calendar.HOUR_OF_DAY);
        draftMedicineMinute = initialTime.get(Calendar.MINUTE);
    }

    private void resetDraftMedicineTime() {
        draftMedicineHour = -1;
        draftMedicineMinute = -1;
        draftMedicineName = "";
    }

    private Button backButton() {
        Button button = smallButton("戻る", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHome();
            }
        });
        button.setContentDescription("ホームに戻る");
        return button;
    }

    private Button backToCalendarButton() {
        Button button = smallButton("予定へ戻る", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDraftEventTime();
                showCalendar();
            }
        });
        button.setContentDescription("予定一覧へ戻る");
        return button;
    }

    private TextView sectionTitle(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(scaledTextSize(22));
        text.setTextColor(COLOR_TEXT);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setLayoutParams(matchWrapWithBottom(8));
        return text;
    }

    private TextView bodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(scaledTextSize(20));
        text.setTextColor(COLOR_TEXT);
        text.setLineSpacing(0, 1.15f);
        text.setPadding(0, dp(8), 0, dp(8));
        text.setLayoutParams(matchWrap());
        return text;
    }

    private TextView compactBodyText(String value) {
        TextView text = bodyText(value);
        text.setTextSize(scaledTextSize(18));
        text.setPadding(0, dp(3), 0, dp(3));
        text.setLayoutParams(matchWrapWithBottom(4));
        return text;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.setBackground(japaneseBox(COLOR_CARD, 18, 1, COLOR_LINE));
        card.setElevation(dp(2));
        card.setMinimumHeight(dp(96));
        card.setLayoutParams(matchWrapWithBottom(16));
        return card;
    }

    private GradientDrawable inputBackground() {
        return japaneseBox(COLOR_CARD, 14, 1, COLOR_LINE);
    }

    private GradientDrawable japaneseBox(int fillColor, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private LinearLayout.LayoutParams matchWrapWithBottom(int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float textScale() {
        return prefs().getFloat(KEY_TEXT_SCALE, 1.0f);
    }

    private float scaledTextSize(float baseSize) {
        return baseSize * textScale();
    }

    private float scaledCanvasTextSize(float baseSp) {
        return scaledTextSize(baseSp) * getResources().getDisplayMetrics().scaledDensity;
    }

    private boolean prefersSingleColumnLayout() {
        float systemFontScale = getResources().getConfiguration().fontScale;
        return textScale() * systemFontScale >= 1.18f;
    }

    private boolean navigateBackWithinApp() {
        switch (currentScreen) {
            case HOME:
            case ONBOARDING_TEXT_SIZE:
                return false;
            case EVENT_FORM:
                resetDraftEventTime();
                showCalendar();
                return true;
            case MEDICINE_FORM:
                resetDraftMedicineTime();
                showMedicines();
                return true;
            case ONBOARDING_EMERGENCY:
                showOnboardingTextSize();
                return true;
            case ONBOARDING_MEDICINE:
                resetDraftMedicineTime();
                showOnboardingEmergency();
                return true;
            default:
                showHome();
                return true;
        }
    }

    private void refreshCurrentScreen() {
        switch (currentScreen) {
            case STEP_COUNTER:
                showStepCounter();
                return;
            case WALKING_ALARM:
                showWalkingAlarm();
                return;
            case NOTES:
                showNotes();
                return;
            case CALENDAR:
                showCalendar();
                return;
            case EVENT_FORM:
                showEventForm();
                return;
            case MEDICINES:
                showMedicines();
                return;
            case MEDICINE_FORM:
                showMedicineDialog();
                return;
            case EMERGENCY:
                showEmergency();
                return;
            case TODOS:
                showTodos();
                return;
            case SHOPPING:
                showShopping();
                return;
            case PREMIUM:
                showPremiumScreen();
                return;
            case SETTINGS:
                showSettings();
                return;
            case ONBOARDING_TEXT_SIZE:
                showOnboardingTextSize();
                return;
            case ONBOARDING_EMERGENCY:
                showOnboardingEmergency();
                return;
            case ONBOARDING_MEDICINE:
                showOnboardingMedicine();
                return;
            case HOME:
            default:
                showHome();
        }
    }

    private int getStatusBarHeight() {
        if (getWindow() != null && getWindow().getDecorView() != null) {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                return insets.getSystemWindowInsetTop();
            }
        }
        return dp(24);
    }

    private List<String> loadNotes() {
        List<String> notes = new ArrayList<>();
        String raw = prefs().getString(KEY_NOTES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                notes.add(array.getString(i));
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(KEY_NOTES).apply();
        }
        return notes;
    }

    private void addNote(String note) {
        List<String> notes = loadNotes();
        notes.add(0, note);
        saveNotes(notes);
    }

    private void deleteNote(int index) {
        List<String> notes = loadNotes();
        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            saveNotes(notes);
        }
    }

    private void saveNotes(List<String> notes) {
        JSONArray array = new JSONArray();
        for (String note : notes) {
            array.put(note);
        }
        prefs().edit().putString(KEY_NOTES, array.toString()).apply();
    }

    private List<String> loadStringList(String key) {
        List<String> items = new ArrayList<>();
        String raw = prefs().getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                items.add(array.getString(i));
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(key).apply();
        }
        return items;
    }

    private void addStringItem(String key, String value) {
        List<String> items = loadStringList(key);
        items.add(0, value);
        saveStringList(key, items);
    }

    private void removeStringItem(String key, int index) {
        List<String> items = loadStringList(key);
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            saveStringList(key, items);
        }
    }

    private void saveStringList(String key, List<String> items) {
        JSONArray array = new JSONArray();
        for (String item : items) {
            array.put(item);
        }
        prefs().edit().putString(key, array.toString()).apply();
    }

    private List<ShoppingItem> loadShoppingItems() {
        List<ShoppingItem> items = new ArrayList<>();
        String raw = prefs().getString(KEY_SHOPPING, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                Object value = array.get(i);
                if (value instanceof JSONObject) {
                    JSONObject object = (JSONObject) value;
                    items.add(new ShoppingItem(
                            object.optString("name", ""),
                            object.optString("amount", ""),
                            object.optString("number", ""),
                            object.optBoolean("bought", false)
                    ));
                } else {
                    items.add(new ShoppingItem(String.valueOf(value), "", "", false));
                }
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(KEY_SHOPPING).apply();
        }
        return items;
    }

    private void addShoppingItem(ShoppingItem item) {
        List<ShoppingItem> items = loadShoppingItems();
        items.add(0, item);
        saveShoppingItems(items);
    }

    private void toggleShoppingBought(int index) {
        List<ShoppingItem> items = loadShoppingItems();
        if (index < 0 || index >= items.size()) {
            return;
        }
        items.get(index).bought = !items.get(index).bought;
        saveShoppingItems(items);
    }

    private void deleteShoppingItem(int index) {
        List<ShoppingItem> items = loadShoppingItems();
        if (index < 0 || index >= items.size()) {
            return;
        }
        items.remove(index);
        saveShoppingItems(items);
    }

    private void clearShoppingItems() {
        prefs().edit().putString(KEY_SHOPPING, "[]").apply();
    }

    private void saveShoppingItems(List<ShoppingItem> items) {
        JSONArray array = new JSONArray();
        for (ShoppingItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("name", item.name);
                object.put("amount", item.amount);
                object.put("number", item.number);
                object.put("bought", item.bought);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString(KEY_SHOPPING, array.toString()).apply();
    }

    private List<TodoItem> loadTodos() {
        List<TodoItem> items = new ArrayList<>();
        String raw = prefs().getString(KEY_TODOS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                Object value = array.get(i);
                if (value instanceof JSONObject) {
                    JSONObject item = (JSONObject) value;
                    items.add(new TodoItem(
                            item.optString("text", ""),
                            item.optBoolean("done", false),
                            item.optString("doneDate", "")
                    ));
                } else {
                    items.add(new TodoItem(String.valueOf(value), false, ""));
                }
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(KEY_TODOS).apply();
        }
        return items;
    }

    private void addTodo(String value) {
        List<TodoItem> items = loadTodos();
        items.add(0, new TodoItem(value, false, ""));
        saveTodos(items);
    }

    private void toggleTodoDone(int index) {
        List<TodoItem> items = loadTodos();
        if (index < 0 || index >= items.size()) {
            return;
        }
        TodoItem item = items.get(index);
        item.done = !item.done;
        item.doneDate = item.done ? todayKey() : "";
        saveTodos(items);
    }

    private void deleteTodo(int index) {
        List<TodoItem> items = loadTodos();
        if (index < 0 || index >= items.size()) {
            return;
        }
        items.remove(index);
        saveTodos(items);
    }

    private void cleanupOldCompletedTodos() {
        List<TodoItem> items = loadTodos();
        String today = todayKey();
        List<TodoItem> kept = new ArrayList<>();
        boolean changed = false;
        for (TodoItem item : items) {
            if (item.done && !today.equals(item.doneDate)) {
                changed = true;
            } else {
                kept.add(item);
            }
        }
        if (changed) {
            saveTodos(kept);
        }
    }

    private void saveTodos(List<TodoItem> items) {
        JSONArray array = new JSONArray();
        for (TodoItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("text", item.text);
                object.put("done", item.done);
                object.put("doneDate", item.doneDate);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString(KEY_TODOS, array.toString()).apply();
    }

    private List<CalendarEvent> loadEvents() {
        List<CalendarEvent> events = new ArrayList<>();
        String raw = prefs().getString(KEY_EVENTS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                events.add(new CalendarEvent(
                        item.getString("title"),
                        item.getLong("time"),
                        item.optBoolean("confirmed", false)
                ));
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(KEY_EVENTS).apply();
        }
        sortCalendarEvents(events);
        return events;
    }

    private void addEvent(CalendarEvent event) {
        List<CalendarEvent> events = loadEvents();
        events.add(0, event);
        saveEvents(events);
    }

    private void deleteEvent(int index) {
        List<CalendarEvent> events = loadEvents();
        if (index >= 0 && index < events.size()) {
            CalendarEvent event = events.remove(index);
            cancelEventAlarm(event);
            saveEvents(events);
        }
    }

    private void confirmEvent(int index) {
        List<CalendarEvent> events = loadEvents();
        if (index >= 0 && index < events.size()) {
            CalendarEvent event = events.get(index);
            cancelEventAlarm(event);
            event.confirmed = true;
            saveEvents(events);
            Toast.makeText(this, "確認しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEvents(List<CalendarEvent> events) {
        JSONArray array = new JSONArray();
        for (CalendarEvent event : events) {
            JSONObject item = new JSONObject();
            try {
                item.put("title", event.title);
                item.put("time", event.timeMillis);
                item.put("confirmed", event.confirmed);
                array.put(item);
            } catch (JSONException ignored) {
                // JSONObject only fails for unsupported values; these fields are simple.
            }
        }
        prefs().edit().putString(KEY_EVENTS, array.toString()).apply();
    }

    private List<CalendarEvent> loadTimedList(String key) {
        List<CalendarEvent> items = new ArrayList<>();
        String raw = prefs().getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                items.add(new CalendarEvent(
                        item.getString("title"),
                        item.getLong("time"),
                        item.optBoolean("confirmed", false)
                ));
            }
        } catch (JSONException ignored) {
            prefs().edit().remove(key).apply();
        }
        sortCalendarEvents(items);
        return items;
    }

    private void sortCalendarEvents(List<CalendarEvent> items) {
        Collections.sort(items, new Comparator<CalendarEvent>() {
            @Override
            public int compare(CalendarEvent first, CalendarEvent second) {
                return Long.compare(first.timeMillis, second.timeMillis);
            }
        });
    }

    private void addTimedItem(String key, CalendarEvent event) {
        List<CalendarEvent> items = loadTimedList(key);
        items.add(0, event);
        saveTimedList(key, items);
    }

    private void deleteTimedItem(String key, int index) {
        List<CalendarEvent> items = loadTimedList(key);
        if (index >= 0 && index < items.size()) {
            CalendarEvent event = items.remove(index);
            cancelEventAlarm(event);
            saveTimedList(key, items);
        }
    }

    private void confirmTimedItem(String key, int index, boolean moveToTomorrow) {
        List<CalendarEvent> items = loadTimedList(key);
        if (index < 0 || index >= items.size()) {
            return;
        }
        CalendarEvent item = items.get(index);
        cancelEventAlarm(item);
        if (moveToTomorrow) {
            Calendar time = Calendar.getInstance(Locale.JAPAN);
            time.setTimeInMillis(item.timeMillis);
            Calendar tomorrow = Calendar.getInstance(Locale.JAPAN);
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            time.set(Calendar.YEAR, tomorrow.get(Calendar.YEAR));
            time.set(Calendar.MONTH, tomorrow.get(Calendar.MONTH));
            time.set(Calendar.DAY_OF_MONTH, tomorrow.get(Calendar.DAY_OF_MONTH));
            item.timeMillis = time.getTimeInMillis();
            item.confirmed = false;
            scheduleEventAlarm(item);
            Toast.makeText(this, "確認しました。次は明日にしました", Toast.LENGTH_LONG).show();
        } else {
            item.confirmed = true;
            Toast.makeText(this, "確認しました", Toast.LENGTH_SHORT).show();
        }
        saveTimedList(key, items);
    }

    private void saveTimedList(String key, List<CalendarEvent> items) {
        JSONArray array = new JSONArray();
        for (CalendarEvent event : items) {
            JSONObject item = new JSONObject();
            try {
                item.put("title", event.title);
                item.put("time", event.timeMillis);
                item.put("confirmed", event.confirmed);
                array.put(item);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString(key, array.toString()).apply();
    }

    private String formatEventTime(long timeMillis) {
        DateFormat date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN);
        DateFormat time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.JAPAN);
        Date eventTime = new Date(timeMillis);
        return date.format(eventTime) + " " + time.format(eventTime);
    }

    private String formatEventTimeOnly(long timeMillis) {
        DateFormat time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.JAPAN);
        return time.format(new Date(timeMillis));
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void scheduleAllEventAlarms() {
        AlarmScheduler.rescheduleAll(this);
    }

    private void scheduleEventAlarm(CalendarEvent event) {
        AlarmScheduler.schedule(this, event.title, event.timeMillis);
    }

    private void cancelEventAlarm(CalendarEvent event) {
        AlarmScheduler.cancel(this, event.title, event.timeMillis);
    }

    private boolean hasActiveWalkingAlarm() {
        clearExpiredWalkingAlarm();
        return prefs().getBoolean(KEY_WALKING_ALARM_ACTIVE, false)
                && prefs().getLong(KEY_WALKING_ALARM_TIME, 0L) > System.currentTimeMillis();
    }

    private long walkingAlarmTimeMillis() {
        return prefs().getLong(KEY_WALKING_ALARM_TIME, 0L);
    }

    private void startWalkingAlarmFlow(int minutes) {
        long scheduledTimeMillis = System.currentTimeMillis() + minutes * 60_000L;
        if (!hasNotificationPermission()) {
            pendingWalkingAlarmMinutes = minutes;
            pendingWalkingAlarmTimeMillis = scheduledTimeMillis;
            requestNotificationPermission();
            return;
        }
        if (!hasExactAlarmAccess()) {
            pendingWalkingAlarmMinutes = minutes;
            pendingWalkingAlarmTimeMillis = scheduledTimeMillis;
            requestExactAlarmAccess();
            return;
        }
        saveWalkingAlarm(minutes, scheduledTimeMillis);
        Toast.makeText(this, minutes + "分後に歩行アラームを設定しました", Toast.LENGTH_LONG).show();
        showWalkingAlarm();
    }

    private void saveWalkingAlarm(int minutes, long timeMillis) {
        long previousTime = prefs().getLong(KEY_WALKING_ALARM_TIME, 0L);
        boolean wasActive = prefs().getBoolean(KEY_WALKING_ALARM_ACTIVE, false);
        if (wasActive && previousTime > 0L) {
            AlarmScheduler.cancel(this, WALKING_ALARM_TITLE, previousTime);
        }
        prefs().edit()
                .putBoolean(KEY_WALKING_ALARM_ACTIVE, true)
                .putLong(KEY_WALKING_ALARM_TIME, timeMillis)
                .putInt(KEY_WALKING_ALARM_MINUTES, minutes)
                .apply();
        AlarmScheduler.schedule(this, WALKING_ALARM_TITLE, timeMillis);
    }

    private void cancelWalkingAlarm() {
        long timeMillis = prefs().getLong(KEY_WALKING_ALARM_TIME, 0L);
        if (timeMillis > 0L) {
            AlarmScheduler.cancel(this, WALKING_ALARM_TITLE, timeMillis);
        }
        prefs().edit()
                .putBoolean(KEY_WALKING_ALARM_ACTIVE, false)
                .remove(KEY_WALKING_ALARM_TIME)
                .remove(KEY_WALKING_ALARM_MINUTES)
                .apply();
    }

    private void createEventAlarmChannel() {
        EventAlarmReceiver.ensureNotificationChannel(this);
    }

    private boolean needsActivityRecognitionPermission() {
        return stepCounterSensor != null
                && Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AppOpsManager appOpsManager = getSystemService(AppOpsManager.class);
        if (appOpsManager != null) {
            int mode = appOpsManager.unsafeCheckOpNoThrow(
                    APP_OP_SCHEDULE_EXACT_ALARM,
                    android.os.Process.myUid(),
                    getPackageName()
            );
            if (mode == AppOpsManager.MODE_ALLOWED) {
                return true;
            }
            if (mode == AppOpsManager.MODE_DEFAULT || mode == AppOpsManager.MODE_IGNORED) {
                return false;
            }
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return alarmManager == null || alarmManager.canScheduleExactAlarms();
    }

    private void requestActivityRecognitionPermission() {
        if (!needsActivityRecognitionPermission()) {
            return;
        }
        prefs().edit().putBoolean(KEY_ACTIVITY_PERMISSION_ASKED, true).apply();
        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY_RECOGNITION);
    }

    private void requestNotificationPermission() {
        if (hasNotificationPermission()) {
            return;
        }
        prefs().edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply();
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }

    private String activityPermissionActionLabel() {
        return shouldOpenActivityPermissionSettings() ? "設定を開く" : "許可する";
    }

    private void handleActivityPermissionAction() {
        if (shouldOpenActivityPermissionSettings()) {
            openAppSettings("歩数の設定を開きました");
            return;
        }
        requestActivityRecognitionPermission();
    }

    private boolean shouldOpenActivityPermissionSettings() {
        return needsActivityRecognitionPermission()
                && prefs().getBoolean(KEY_ACTIVITY_PERMISSION_ASKED, false);
    }

    private String notificationPermissionActionLabel() {
        return shouldOpenNotificationPermissionSettings() ? "設定を開く" : "許可する";
    }

    private void handleNotificationPermissionAction() {
        if (shouldOpenNotificationPermissionSettings()) {
            openAppSettings("通知の設定を開きました");
            return;
        }
        requestNotificationPermission();
    }

    private boolean shouldOpenNotificationPermissionSettings() {
        return Build.VERSION.SDK_INT >= 33
                && !hasNotificationPermission()
                && prefs().getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false);
    }

    private void openAppSettings(String message) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void requestExactAlarmAccess() {
        if (hasExactAlarmAccess() || Build.VERSION.SDK_INT < 31) {
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + getPackageName()));
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
        Toast.makeText(this, "アラームの特別な許可を開きました", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
            if (needsActivityRecognitionPermission()) {
                Toast.makeText(this, "歩数の許可は後から設定で変更できます", Toast.LENGTH_LONG).show();
            }
            registerStepSensor();
            updateStepCounterText();
            refreshCurrentScreen();
            return;
        }
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (hasNotificationPermission() && pendingReminderSave != null) {
                if (hasExactAlarmAccess()) {
                    completePendingReminderSave();
                } else {
                    requestExactAlarmAccess();
                }
                return;
            }
            if (hasNotificationPermission() && pendingWalkingAlarmTimeMillis != null && pendingWalkingAlarmMinutes > 0) {
                if (hasExactAlarmAccess()) {
                    completePendingWalkingAlarm();
                } else {
                    requestExactAlarmAccess();
                }
                return;
            }
            if (pendingReminderSave != null) {
                clearPendingReminderSave();
                Toast.makeText(this, "通知を許可すると予定や薬のお知らせを保存できます。後から設定で変更できます", Toast.LENGTH_LONG).show();
            }
            if (pendingWalkingAlarmTimeMillis != null) {
                clearPendingWalkingAlarm();
                Toast.makeText(this, "通知を許可すると歩行アラームを保存できます。後から設定で変更できます", Toast.LENGTH_LONG).show();
            }
            if (pendingReminderSave == null && pendingWalkingAlarmTimeMillis == null) {
                Toast.makeText(this, "通知の許可は後から設定で変更できます", Toast.LENGTH_LONG).show();
            }
            refreshCurrentScreen();
        }
    }

    private void queuePendingReminderSave(String key, CalendarEvent event, boolean finishOnboardingAfterSave) {
        pendingReminderSave = new PendingReminderSave(key, event, finishOnboardingAfterSave);
        if (!hasNotificationPermission()) {
            requestNotificationPermission();
            return;
        }
        if (!hasExactAlarmAccess()) {
            requestExactAlarmAccess();
        }
    }

    private void resumePendingAlarmSetup() {
        if (pendingReminderSave != null && hasNotificationPermission() && hasExactAlarmAccess()) {
            completePendingReminderSave();
            return;
        }
        if (pendingWalkingAlarmTimeMillis != null
                && pendingWalkingAlarmMinutes > 0
                && hasNotificationPermission()
                && hasExactAlarmAccess()) {
            completePendingWalkingAlarm();
        }
    }

    private void completePendingReminderSave() {
        if (pendingReminderSave == null) {
            return;
        }
        PendingReminderSave pending = pendingReminderSave;
        clearPendingReminderSave();
        if (KEY_EVENTS.equals(pending.key)) {
            addEvent(pending.event);
            scheduleEventAlarm(pending.event);
            resetDraftEventTime();
            Toast.makeText(this, "予定アラームを保存しました", Toast.LENGTH_LONG).show();
            showCalendar();
            return;
        }
        addTimedItem(pending.key, pending.event);
        scheduleEventAlarm(pending.event);
        resetDraftMedicineTime();
        if (pending.finishOnboarding) {
            finishOnboarding();
        } else {
            Toast.makeText(this, "薬アラームを保存しました", Toast.LENGTH_LONG).show();
            showMedicines();
        }
    }

    private void clearPendingReminderSave() {
        pendingReminderSave = null;
    }

    private void completePendingWalkingAlarm() {
        if (pendingWalkingAlarmTimeMillis == null || pendingWalkingAlarmMinutes <= 0) {
            return;
        }
        int minutes = pendingWalkingAlarmMinutes;
        long timeMillis = pendingWalkingAlarmTimeMillis;
        clearPendingWalkingAlarm();
        saveWalkingAlarm(minutes, timeMillis);
        Toast.makeText(this, minutes + "分後に歩行アラームを設定しました", Toast.LENGTH_LONG).show();
        showWalkingAlarm();
    }

    private void clearPendingWalkingAlarm() {
        pendingWalkingAlarmTimeMillis = null;
        pendingWalkingAlarmMinutes = -1;
    }

    private String buildTodaySpeech() {
        StringBuilder builder = new StringBuilder();
        Calendar today = Calendar.getInstance(Locale.JAPAN);
        builder.append(formatTodayHeader(today, isJapaneseHoliday(today))).append("。");
        List<CalendarEvent> todayEvents = todayItems(loadEvents());
        List<CalendarEvent> todayMedicines = todayItems(loadTimedList(KEY_MEDICINES));
        if (todayEvents.isEmpty()) {
            builder.append("今日の予定はありません。");
        } else {
            builder.append("今日の予定は、");
            for (CalendarEvent event : todayEvents) {
                builder.append(formatEventTimeOnly(event.timeMillis)).append("、").append(event.title).append("。");
            }
        }
        if (todayMedicines.isEmpty()) {
            builder.append("今日の薬はありません。");
        } else {
            builder.append("今日の薬は、");
            for (CalendarEvent medicine : todayMedicines) {
                builder.append(formatEventTimeOnly(medicine.timeMillis)).append("、").append(medicine.title).append("。");
            }
        }
        List<TodoItem> todos = loadTodos();
        int remaining = 0;
        for (TodoItem todo : todos) {
            if (!todo.done) {
                remaining++;
            }
        }
        if (remaining == 0) {
            builder.append("今日やることはありません。");
        } else {
            builder.append("今日やることは、残り").append(remaining).append("件です。");
        }
        return builder.toString();
    }

    private void speakText(String text) {
        if (!textToSpeechReady || textToSpeech == null) {
            Toast.makeText(this, "読み上げの準備中です", Toast.LENGTH_SHORT).show();
            return;
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mainichi-support");
    }

    private void shareToFamily(String message) {
        String phone = prefs().getString(KEY_EMERGENCY_PHONE, "");
        Intent intent;
        if (phone.isEmpty()) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
        } else {
            intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phone));
            intent.putExtra("sms_body", message);
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, message);
            }
        }
        Intent chooser = Intent.createChooser(intent, "家族へ共有");
        if (chooser.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "共有できるアプリが見つかりません", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(chooser);
    }

    private JSONObject backupObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("app", "まいにちサポート");
            object.put("createdAt", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPAN).format(new Date()));
            object.put("notes", new JSONArray(prefs().getString(KEY_NOTES, "[]")));
            object.put("events", new JSONArray(prefs().getString(KEY_EVENTS, "[]")));
            object.put("medicines", new JSONArray(prefs().getString(KEY_MEDICINES, "[]")));
            object.put("todos", new JSONArray(prefs().getString(KEY_TODOS, "[]")));
            object.put("shopping", new JSONArray(prefs().getString(KEY_SHOPPING, "[]")));
            object.put("emergencyName", prefs().getString(KEY_EMERGENCY_NAME, ""));
            object.put("emergencyPhone", prefs().getString(KEY_EMERGENCY_PHONE, ""));
        } catch (JSONException ignored) {
        }
        return object;
    }

    private void shareBackup() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "まいにちサポート バックアップ");
        intent.putExtra(Intent.EXTRA_TEXT, backupObject().toString());
        Intent chooser = Intent.createChooser(intent, "バックアップを共有");
        if (chooser.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "共有できるアプリが見つかりません", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(chooser);
    }

    private void copyBackup() {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("まいにちサポート バックアップ", backupObject().toString()));
            Toast.makeText(this, "バックアップをコピーしました", Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showInputError(EditText input, String message) {
        input.setError(message);
        input.requestFocus();
        showKeyboard(input);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showKeyboard(final View view) {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.requestFocus();
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (manager != null) {
                    manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 250L);
    }

    private void styleDialogButtons(AlertDialog dialog) {
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL));
    }

    private void styleDialogButton(Button button) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextSize(Math.min(22f, scaledTextSize(17f)));
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(8), button.getPaddingTop(), dp(8), button.getPaddingBottom());
    }

    private boolean isPremiumActive() {
        return prefs().getBoolean(KEY_PREMIUM_ACTIVE, false);
    }

    private boolean isOnboardingDone() {
        return prefs().getBoolean(KEY_ONBOARDING_DONE, false);
    }

    private void showPremiumUnavailable() {
        Toast.makeText(this, "お申し込み機能は現在準備中です", Toast.LENGTH_LONG).show();
    }

    private void schedulePremiumPrompt() {
        if (currentScreen != Screen.HOME
                || !isOnboardingDone()
                || isPremiumActive()
                || premiumPromptShown
                || premiumPromptScheduled
                || wasPremiumPromptShownRecently()) {
            return;
        }
        premiumPromptScheduled = true;
        promptHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                premiumPromptScheduled = false;
                if (currentScreen == Screen.HOME) {
                    showPremiumPrompt();
                }
            }
        }, 5000L);
    }

    private void showPremiumPrompt() {
        if (isFinishing() || isPremiumActive()) {
            return;
        }
        premiumPromptShown = true;
        prefs().edit().putLong(KEY_PREMIUM_PROMPT_LAST_SHOWN, System.currentTimeMillis()).apply();
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, 0, dp(18));
        panel.setBackground(premiumPanelBackground());
        panel.setElevation(dp(12));

        ShibaPremiumView shibaView = new ShibaPremiumView(this);
        shibaView.setContentDescription("柴犬が広告なしのプレミアムを、吹き出しでおすすめしています");
        panel.addView(shibaView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(prefersSingleColumnLayout() ? 250 : 230)
        ));

        TextView title = bodyText("広告なしにするワン");
        title.setTextSize(scaledTextSize(24));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(18), dp(8), dp(18), dp(2));
        panel.addView(title, matchWrapWithBottom(2));

        TextView message = bodyText("月額300円で、画面のGoogle広告を表示しません。");
        message.setTextSize(scaledTextSize(18));
        message.setGravity(Gravity.CENTER);
        message.setTextColor(COLOR_MUTED);
        message.setPadding(dp(22), 0, dp(22), dp(12));
        panel.addView(message, matchWrapWithBottom(6));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(dp(18), 0, dp(18), 0);

        Button detail = dialogButton("会員画面を見る", COLOR_ACCENT, true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showPremiumScreen();
            }
        });
        Button later = dialogButton("あとで", COLOR_SECONDARY, false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        buttons.addView(detail, matchWrapWithBottom(8));
        buttons.addView(later, matchWrap());
        panel.addView(buttons, matchWrap());

        dialog.setContentView(panel);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(dp(prefersSingleColumnLayout() ? 342 : 360), LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private boolean wasPremiumPromptShownRecently() {
        long lastShown = prefs().getLong(KEY_PREMIUM_PROMPT_LAST_SHOWN, 0L);
        return lastShown > 0L && System.currentTimeMillis() - lastShown < PREMIUM_PROMPT_INTERVAL_MILLIS;
    }

    private GradientDrawable premiumPanelBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(255, 252, 245));
        background.setCornerRadius(dp(28));
        background.setStroke(dp(2), Color.rgb(225, 196, 137));
        return background;
    }

    private Button dialogButton(String label, int color, boolean filled, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(scaledTextSize(18));
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(58));
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        applyButtonColors(button, filled ? color : Color.TRANSPARENT, filled ? COLOR_ACCENT : COLOR_LINE);
        button.setTextColor(filled ? Color.WHITE : color);
        button.setOnClickListener(listener);
        return button;
    }

    private void addAdBanner() {
        if (isPremiumActive()) {
            return;
        }
        LinearLayout ad = new LinearLayout(this);
        ad.setOrientation(LinearLayout.VERTICAL);
        ad.setPadding(dp(16), dp(14), dp(16), dp(14));
        ad.setBackground(japaneseBox(COLOR_SURFACE_TINT, 16, 1, COLOR_LINE));
        ad.setLayoutParams(matchWrapWithBottom(14));

        TextView label = bodyText("Google広告");
        label.setTextSize(scaledTextSize(18));
        label.setTextColor(COLOR_MUTED);
        ad.addView(label);

        TextView message = bodyText("広告なしで使えます");
        message.setTypeface(Typeface.DEFAULT_BOLD);
        ad.addView(message);

        Button removeAds = smallButton("広告を消す", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumScreen();
            }
        });
        applyButtonColors(removeAds, COLOR_ACCENT, COLOR_ACCENT);
        ad.addView(removeAds);
        root.addView(ad);
    }

    private static class CalendarEvent {
        final String title;
        long timeMillis;
        boolean confirmed;

        CalendarEvent(String title, long timeMillis) {
            this(title, timeMillis, false);
        }

        CalendarEvent(String title, long timeMillis, boolean confirmed) {
            this.title = title;
            this.timeMillis = timeMillis;
            this.confirmed = confirmed;
        }

        int alarmId() {
            return (title + timeMillis).hashCode();
        }
    }

    private static class PendingReminderSave {
        final String key;
        final CalendarEvent event;
        final boolean finishOnboarding;

        PendingReminderSave(String key, CalendarEvent event, boolean finishOnboarding) {
            this.key = key;
            this.event = event;
            this.finishOnboarding = finishOnboarding;
        }
    }

    private static class TodoItem {
        final String text;
        boolean done;
        String doneDate;

        TodoItem(String text, boolean done, String doneDate) {
            this.text = text;
            this.done = done;
            this.doneDate = doneDate;
        }
    }

    private static class ShoppingItem {
        final String name;
        final String amount;
        final String number;
        boolean bought;

        ShoppingItem(String name, String amount, String number, boolean bought) {
            this.name = name;
            this.amount = amount;
            this.number = number;
            this.bought = bought;
        }
    }

    private class ShibaPremiumView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private int shaderWidth = -1;
        private int shaderHeight = -1;
        private LinearGradient panelGradient;
        private RadialGradient faceGradient;

        ShibaPremiumView(Context context) {
            super(context);
        }

        private void updateShaders(int width, int height) {
            if (width <= 0 || height <= 0 || (width == shaderWidth && height == shaderHeight)) {
                return;
            }
            shaderWidth = width;
            shaderHeight = height;
            float faceRadius = Math.min(width, height) * 0.25f;
            float cx = width * 0.35f;
            float cy = height * 0.61f;
            panelGradient = new LinearGradient(0, 0, 0, height,
                    Color.rgb(255, 236, 183),
                    Color.rgb(255, 249, 231),
                    Shader.TileMode.CLAMP);
            faceGradient = new RadialGradient(cx - faceRadius * 0.35f, cy - faceRadius * 0.42f, faceRadius * 1.35f,
                    Color.rgb(245, 176, 83),
                    Color.rgb(191, 99, 42),
                    Shader.TileMode.CLAMP);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            updateShaders(width, height);
            float cx = width * 0.35f;
            float cy = height * 0.61f;
            float faceRadius = Math.min(width, height) * 0.25f;

            paint.setShader(panelGradient);
            rect.set(0, 0, width, height - dp(10));
            canvas.drawRoundRect(rect, dp(28), dp(28), paint);
            paint.setShader(null);

            paint.setColor(Color.argb(22, 128, 80, 25));
            rect.set(dp(18), dp(18), width - dp(18), height - dp(24));
            canvas.drawRoundRect(rect, dp(24), dp(24), paint);

            drawPremiumBadge(canvas, width);
            drawSparkle(canvas, width * 0.15f, height * 0.24f, dp(12), Color.argb(190, 255, 255, 255));
            drawSparkle(canvas, width * 0.84f, height * 0.63f, dp(10), Color.argb(170, 176, 137, 57));

            paint.setColor(Color.argb(46, 68, 40, 12));
            rect.set(cx - faceRadius * 1.05f, cy + faceRadius * 0.6f, cx + faceRadius * 1.18f, cy + faceRadius);
            canvas.drawOval(rect, paint);

            drawEar(canvas, cx - faceRadius * 0.68f, cy - faceRadius * 0.52f, faceRadius, true);
            drawEar(canvas, cx + faceRadius * 0.68f, cy - faceRadius * 0.52f, faceRadius, false);

            paint.setShader(faceGradient);
            rect.set(cx - faceRadius, cy - faceRadius, cx + faceRadius, cy + faceRadius);
            canvas.drawOval(rect, paint);
            paint.setShader(null);

            paint.setColor(Color.rgb(255, 244, 220));
            rect.set(cx - faceRadius * 0.62f, cy - faceRadius * 0.1f, cx + faceRadius * 0.62f, cy + faceRadius * 0.78f);
            canvas.drawOval(rect, paint);

            paint.setColor(Color.argb(80, 255, 255, 255));
            rect.set(cx - faceRadius * 0.52f, cy - faceRadius * 0.72f, cx + faceRadius * 0.18f, cy - faceRadius * 0.2f);
            canvas.drawOval(rect, paint);

            paint.setColor(Color.rgb(63, 45, 35));
            canvas.drawCircle(cx - faceRadius * 0.34f, cy - faceRadius * 0.18f, faceRadius * 0.075f, paint);
            canvas.drawCircle(cx + faceRadius * 0.34f, cy - faceRadius * 0.18f, faceRadius * 0.075f, paint);

            paint.setColor(Color.rgb(48, 34, 28));
            rect.set(cx - faceRadius * 0.13f, cy + faceRadius * 0.04f, cx + faceRadius * 0.13f, cy + faceRadius * 0.18f);
            canvas.drawOval(rect, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(93, 58, 43));
            rect.set(cx - faceRadius * 0.24f, cy + faceRadius * 0.12f, cx, cy + faceRadius * 0.38f);
            canvas.drawArc(rect, 8, 70, false, paint);
            rect.set(cx, cy + faceRadius * 0.12f, cx + faceRadius * 0.24f, cy + faceRadius * 0.38f);
            canvas.drawArc(rect, 102, 70, false, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.argb(105, 218, 85, 82));
            canvas.drawCircle(cx - faceRadius * 0.55f, cy + faceRadius * 0.16f, faceRadius * 0.11f, paint);
            canvas.drawCircle(cx + faceRadius * 0.55f, cy + faceRadius * 0.16f, faceRadius * 0.11f, paint);

            drawSpeechBubble(canvas, width, height);
        }

        private void drawPremiumBadge(Canvas canvas, float width) {
            float left = width - dp(128);
            float top = dp(18);
            float right = width - dp(18);
            float bottom = dp(54);

            paint.setColor(Color.argb(42, 60, 35, 10));
            rect.set(left + dp(2), top + dp(4), right + dp(2), bottom + dp(4));
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);

            paint.setShader(new LinearGradient(left, top, right, bottom,
                    Color.rgb(129, 83, 27),
                    Color.rgb(210, 154, 54),
                    Shader.TileMode.CLAMP));
            rect.set(left, top, right, bottom);
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            paint.setShader(null);

            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(scaledCanvasTextSize(15));
            canvas.drawText("プレミアム", (left + right) / 2f, top + dp(24), paint);
        }

        private void drawEar(Canvas canvas, float x, float y, float radius, boolean left) {
            path.reset();
            path.moveTo(x, y - radius * 0.62f);
            path.lineTo(x + (left ? -radius * 0.48f : radius * 0.48f), y + radius * 0.36f);
            path.lineTo(x + (left ? radius * 0.28f : -radius * 0.28f), y + radius * 0.3f);
            path.close();
            paint.setColor(Color.rgb(194, 98, 43));
            canvas.drawPath(path, paint);

            path.reset();
            path.moveTo(x, y - radius * 0.34f);
            path.lineTo(x + (left ? -radius * 0.25f : radius * 0.25f), y + radius * 0.25f);
            path.lineTo(x + (left ? radius * 0.15f : -radius * 0.15f), y + radius * 0.19f);
            path.close();
            paint.setColor(Color.rgb(255, 219, 190));
            canvas.drawPath(path, paint);
        }

        private void drawSpeechBubble(Canvas canvas, float width, float height) {
            float left = width * 0.49f;
            float top = height * 0.29f;
            float right = width - dp(18);
            float bottom = height * 0.82f;

            paint.setColor(Color.argb(45, 51, 31, 12));
            rect.set(left + dp(5), top + dp(7), right + dp(5), bottom + dp(7));
            canvas.drawRoundRect(rect, dp(24), dp(24), paint);

            paint.setShader(new LinearGradient(left, top, right, bottom,
                    Color.WHITE,
                    Color.rgb(255, 246, 226),
                    Shader.TileMode.CLAMP));
            rect.set(left, top, right, bottom);
            canvas.drawRoundRect(rect, dp(24), dp(24), paint);
            paint.setShader(null);

            path.reset();
            path.moveTo(left + dp(14), bottom - dp(30));
            path.lineTo(left - dp(24), bottom - dp(14));
            path.lineTo(left + dp(18), bottom - dp(9));
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(COLOR_ACCENT);
            paint.setStrokeWidth(dp(3));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(rect, dp(24), dp(24), paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(COLOR_TEXT);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(scaledCanvasTextSize(17));
            canvas.drawText("広告を", (left + right) / 2f, top + (bottom - top) * 0.38f, paint);
            canvas.drawText("消すワン!", (left + right) / 2f, top + (bottom - top) * 0.64f, paint);
        }

        private void drawSparkle(Canvas canvas, float cx, float cy, float size, int color) {
            path.reset();
            path.moveTo(cx, cy - size);
            path.lineTo(cx + size * 0.25f, cy - size * 0.25f);
            path.lineTo(cx + size, cy);
            path.lineTo(cx + size * 0.25f, cy + size * 0.25f);
            path.lineTo(cx, cy + size);
            path.lineTo(cx - size * 0.25f, cy + size * 0.25f);
            path.lineTo(cx - size, cy);
            path.lineTo(cx - size * 0.25f, cy - size * 0.25f);
            path.close();
            paint.setColor(color);
            canvas.drawPath(path, paint);
        }
    }

    private class StepGraphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF barRect = new RectF();
        private final String[] labels = {"月", "火", "水", "木", "金", "土", "日"};
        private int[] steps = new int[7];

        StepGraphView(Context context) {
            super(context);
        }

        void setSteps(int[] values) {
            steps = values;
            setContentDescription(buildGraphContentDescription(values));
            invalidate();
        }

        private String buildGraphContentDescription(int[] values) {
            Calendar calendar = Calendar.getInstance(Locale.JAPAN);
            calendar.add(Calendar.DAY_OF_YEAR, -6);
            StringBuilder builder = new StringBuilder("7日間の歩数");
            for (int i = 0; i < values.length; i++) {
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                builder.append("、")
                        .append(labels[(dayOfWeek + 5) % 7])
                        .append(" ")
                        .append(values[i])
                        .append("歩");
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return builder.toString();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            int left = dp(8);
            int right = width - dp(8);
            int top = dp(8);
            int bottom = height - dp(30);
            int chartHeight = bottom - top;
            int max = 1;
            for (int value : steps) {
                max = Math.max(max, value);
            }

            paint.setStrokeWidth(dp(1));
            paint.setColor(COLOR_LINE);
            canvas.drawLine(left, bottom, right, bottom, paint);

            float gap = dp(8);
            float barWidth = (right - left - gap * (steps.length - 1)) / steps.length;
            Calendar calendar = Calendar.getInstance(Locale.JAPAN);
            calendar.add(Calendar.DAY_OF_YEAR, -6);
            for (int i = 0; i < steps.length; i++) {
                float x = left + i * (barWidth + gap);
                float ratio = steps[i] / (float) max;
                float barTop = bottom - Math.max(dp(8), chartHeight * ratio);
                paint.setColor(i == steps.length - 1 ? COLOR_ACCENT : COLOR_PRIMARY);
                barRect.set(x, barTop, x + barWidth, bottom);
                canvas.drawRoundRect(barRect, dp(4), dp(4), paint);

                paint.setColor(COLOR_MUTED);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(scaledCanvasTextSize(13));
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                canvas.drawText(labels[(dayOfWeek + 5) % 7], x + barWidth / 2, height - dp(8), paint);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }

    private class WashiBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WashiBackgroundView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(COLOR_BG);

            int spacing = dp(52);
            paint.setStrokeWidth(1f);

            paint.setColor(Color.argb(4, 34, 36, 38));
            for (int x = spacing; x < getWidth(); x += spacing) {
                canvas.drawLine(x, 0, x, getHeight(), paint);
            }

            paint.setColor(Color.argb(5, 176, 137, 57));
            for (int y = spacing; y < getHeight(); y += spacing) {
                canvas.drawLine(0, y, getWidth(), y, paint);
            }
        }
    }
}
