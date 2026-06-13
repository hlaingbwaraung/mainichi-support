package com.example.seniorhelper;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.text.TextUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {
    private static final String PREFS = "senior_helper_prefs";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MEDICINES = "medicines";
    private static final String KEY_TODOS = "todos";
    private static final String KEY_SHOPPING = "shopping";
    private static final String KEY_FAMILY_CONTACTS = "family_contacts";
    private static final String KEY_EMERGENCY_NAME = "emergency_name";
    private static final String KEY_EMERGENCY_PHONE = "emergency_phone";
    private static final String KEY_STEP_BASE = "step_base";
    private static final String KEY_STEP_DATE = "step_date";
    private static final String KEY_STEP_HISTORY = "step_history";
    private static final String KEY_PREMIUM_ACTIVE = "premium_active";
    private static final String KEY_PREMIUM_PLAN = "premium_plan";
    private static final String KEY_REAL_BILLING_MIGRATED = "real_billing_migrated";
    private static final String KEY_TEXT_SCALE = "text_scale";
    private static final String KEY_BOLD_TEXT = "bold_text";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final long WEATHER_LOCATION_TIMEOUT_MS = 6000L;
    private static final long MAX_LAST_LOCATION_AGE_MS = 30L * 60L * 1000L;
    private static final float GOOD_WEATHER_LOCATION_ACCURACY_METERS = 250f;
    private static final int COLOR_BG = Color.rgb(250, 248, 243);
    private static final int COLOR_CARD = Color.rgb(255, 255, 252);
    private static final int COLOR_TEXT = Color.rgb(29, 30, 32);
    private static final int COLOR_MUTED = Color.rgb(96, 93, 86);
    private static final int COLOR_PRIMARY = Color.rgb(34, 36, 38);
    private static final int COLOR_SECONDARY = Color.rgb(92, 88, 80);
    private static final int COLOR_ACCENT = Color.rgb(176, 137, 57);
    private static final int COLOR_LINE = Color.rgb(224, 215, 195);
    private static final int COLOR_HOLIDAY = Color.rgb(166, 55, 48);
    private static final int COLOR_STEPS = Color.rgb(39, 87, 94);
    private static final int COLOR_NOTES = Color.rgb(98, 77, 126);
    private static final int COLOR_SCHEDULE = Color.rgb(119, 74, 45);
    private static final int COLOR_MEDICINE = Color.rgb(116, 59, 82);
    private static final int COLOR_EMERGENCY = Color.rgb(139, 55, 45);
    private static final int COLOR_TODO = Color.rgb(80, 94, 52);
    private static final int COLOR_SHOPPING = Color.rgb(48, 91, 124);

    private LinearLayout root;
    private ScrollView currentScrollView;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepCountView;
    private TextView stepSensorStatusView;
    private TextView stepGraphSelectionView;
    private StepGraphView stepGraphView;
    private TextView weatherView;
    private final Handler promptHandler = new Handler(Looper.getMainLooper());
    private final Handler weatherHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady = false;
    private boolean premiumPromptScheduled = false;
    private boolean premiumPromptShown = false;
    private boolean billingStateChecked = false;
    private String currentScreenTitle = "";
    private PlayBillingManager billingManager;
    private int stepsToday = 0;
    private int selectedStepDayOffset = 0;
    private int selectedStepGraphIndex = 6;
    private int draftEventHour = -1;
    private int draftEventMinute = -1;
    private int draftMedicineHour = -1;
    private int draftMedicineMinute = -1;
    private LocationManager weatherLocationManager;
    private LocationListener weatherLocationListener;
    private Location bestWeatherLocation;
    private int weatherRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        textToSpeech = new TextToSpeech(this, this);
        createEventAlarmChannel();
        requestNeededPermissions();
        scheduleAllEventAlarms();
        migrateFromTestPremium();
        billingManager = new PlayBillingManager(this, new PlayBillingManager.Listener() {
            @Override
            public void onPremiumStatusChanged(PlayBillingManager.PremiumPlan plan, boolean firstCheck) {
                boolean active = plan != PlayBillingManager.PremiumPlan.NONE;
                boolean changed = isPremiumActive() != active;
                prefs().edit()
                        .putBoolean(KEY_PREMIUM_ACTIVE, active)
                        .putString(KEY_PREMIUM_PLAN, plan.name())
                        .apply();
                billingStateChecked = true;
                if ("プレミアム".equals(currentScreenTitle)) {
                    showPremiumScreen();
                } else if (changed && "まいにちサポート".equals(currentScreenTitle)) {
                    showHome();
                }
                if (!active) {
                    schedulePremiumPrompt();
                }
            }

            @Override
            public void onBillingProductChanged(boolean available) {
                if ("プレミアム".equals(currentScreenTitle)) {
                    showPremiumScreen();
                }
            }

            @Override
            public void onBillingMessage(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
        billingManager.start();
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
        if ("まいにちサポート".equals(currentScreenTitle)
                && weatherView != null
                && weatherView.getText().toString().contains("取得中")) {
            fetchWeather();
        }
        if (billingManager != null) {
            billingManager.refreshPurchases(false);
        }
        schedulePremiumPrompt();
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        promptHandler.removeCallbacksAndMessages(null);
        weatherHandler.removeCallbacksAndMessages(null);
        cancelWeatherLocationRequest();
        premiumPromptScheduled = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (billingManager != null) {
            billingManager.endConnection();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int result = textToSpeech.setLanguage(Locale.JAPAN);
            textToSpeechReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    private void showHome() {
        beginScreen("まいにちサポート", "今日");
        addTodayHeader();
        addTodaySummary();
        root.addView(sectionTitle("使いたい機能を選んでください"));
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
                homeFeatureButton("メモ", "メモを開く", COLOR_NOTES, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotes();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("予定", "予定を開く", COLOR_SCHEDULE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalendar();
            }
        }),
                homeFeatureButton("薬アラーム", "薬アラームを開く", COLOR_MEDICINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMedicines();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("家族", "家族連絡を開く", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmergency();
            }
        }),
                homeFeatureButton("今日やること", "今日やることを開く", COLOR_TODO, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTodos();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("買い物リスト", "買い物リストを開く", COLOR_SHOPPING, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShopping();
            }
        }),
                homeFeatureButton(isPremiumActive() ? "プレミアム会員" : "広告を消す", "プレミアム会員画面を開く", COLOR_PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumScreen();
            }
        }));
        addHomeFeatureRow(featureGrid,
                homeFeatureButton("設定", "文字サイズとバックアップ", COLOR_SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        }),
                homeFeatureButton("読み上げ", "今日の内容を読み上げ", COLOR_ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakText(buildTodaySpeech());
            }
        }));
        root.addView(featureGrid);
        addAdBanner();
    }

    private void showStepCounter() {
        beginScreen("歩数計", "今日の歩数を大きく表示");
        root.addView(backButton());

        addStepCounterPanel();
        addAdBanner();
    }

    private void addTodayHeader() {
        Calendar today = Calendar.getInstance(Locale.JAPAN);
        boolean holiday = isJapaneseHoliday(today);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        panel.setBackground(japaneseBox(holiday ? Color.rgb(255, 248, 246) : COLOR_CARD, 6, 1, holiday ? COLOR_HOLIDAY : COLOR_LINE));

        TextView date = bodyText(formatTodayHeader(today, holiday));
        date.setTextSize(scaledTextSize(19));
        date.setTypeface(Typeface.DEFAULT_BOLD);
        date.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        date.setTextColor(holiday ? COLOR_HOLIDAY : COLOR_TEXT);
        date.setPadding(0, 0, dp(8), 0);
        panel.addView(date, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.35f));

        weatherView = bodyText("--℃ 天気");
        weatherView.setTextSize(scaledTextSize(18));
        weatherView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        weatherView.setTypeface(Typeface.DEFAULT_BOLD);
        weatherView.setPadding(dp(8), 0, 0, 0);
        weatherView.setContentDescription("現在地の天気。押すと更新");
        weatherView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchWeather();
            }
        });
        panel.addView(weatherView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(panel, matchWrapWithBottom(10));
        fetchWeather();
    }

    private String formatTodayHeader(Calendar calendar, boolean holiday) {
        String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String weekday = weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        String value = String.format(Locale.JAPAN, "%d年%d月%d日（%s）", year, month, day, weekday);
        return holiday ? value + " 祝日" : value;
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
        List<TodoItem> remainingTodos = remainingTodos();

        panel.addView(compactBodyText(todayEvents.isEmpty() ? "今日の予定: ありません" : "今日の予定: " + formatTodayItemList(todayEvents)));
        panel.addView(compactBodyText(todayMedicines.isEmpty() ? "今日の薬: ありません" : "今日の薬: " + formatTodayItemList(todayMedicines)));
        panel.addView(compactBodyText(remainingTodos.isEmpty()
                ? "今日やること: ありません"
                : "今日やること:\n" + formatTodoDisplayList(remainingTodos)));

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
        row.addView(read, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams familyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        familyParams.setMargins(dp(8), 0, 0, 0);
        row.addView(family, familyParams);
        panel.addView(row, matchWrap());
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

    private List<TodoItem> remainingTodos() {
        List<TodoItem> remaining = new ArrayList<>();
        for (TodoItem todo : loadTodos()) {
            if (!todo.done && !todo.text.trim().isEmpty()) {
                remaining.add(todo);
            }
        }
        return remaining;
    }

    private String formatTodoDisplayList(List<TodoItem> todos) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < todos.size(); i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append("・").append(todos.get(i).text);
        }
        return builder.toString();
    }

    private void fetchWeather() {
        if (weatherView == null) {
            return;
        }
        final int requestId = ++weatherRequestId;
        weatherHandler.removeCallbacksAndMessages(null);
        cancelWeatherLocationRequest();
        weatherView.setText("--℃ 取得中");
        if (!hasLocationPermission()) {
            weatherView.setText("--℃ 位置情報を許可");
            return;
        }
        requestWeatherLocation(requestId);
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private Location getBestRecentLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null || !hasLocationPermission()) {
            return null;
        }
        Location best = null;
        try {
            List<String> providers = manager.getProviders(true);
            for (String provider : providers) {
                Location location = manager.getLastKnownLocation(provider);
                if (isRecentWeatherLocation(location) && isBetterWeatherLocation(location, best)) {
                    best = location;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return best;
    }

    private void requestWeatherLocation(final int requestId) {
        final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null || !hasLocationPermission()) {
            showWeatherStatus(requestId, "--℃ 位置情報なし");
            return;
        }
        weatherLocationManager = manager;
        bestWeatherLocation = getBestRecentLocation();
        weatherLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (requestId != weatherRequestId
                        || weatherLocationListener != this
                        || !isRecentWeatherLocation(location)) {
                    return;
                }
                if (isBetterWeatherLocation(location, bestWeatherLocation)) {
                    bestWeatherLocation = location;
                }
                if (location.hasAccuracy()
                        && location.getAccuracy() <= GOOD_WEATHER_LOCATION_ACCURACY_METERS) {
                    finishWeatherLocation(requestId);
                }
            }
        };
        try {
            boolean requested = false;
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        weatherLocationListener,
                        Looper.getMainLooper()
                );
                requested = true;
            }
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        weatherLocationListener,
                        Looper.getMainLooper()
                );
                requested = true;
            }
            if (!requested) {
                finishWeatherLocation(requestId);
                return;
            }
            weatherHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishWeatherLocation(requestId);
                }
            }, WEATHER_LOCATION_TIMEOUT_MS);
        } catch (Exception ignored) {
            finishWeatherLocation(requestId);
        }
    }

    private void finishWeatherLocation(int requestId) {
        if (requestId != weatherRequestId) {
            return;
        }
        Location location = bestWeatherLocation;
        cancelWeatherLocationRequest();
        bestWeatherLocation = null;
        weatherHandler.removeCallbacksAndMessages(null);
        if (location == null) {
            showWeatherStatus(requestId, "--℃ 位置情報なし");
            return;
        }
        fetchWeatherForLocation(location.getLatitude(), location.getLongitude(), requestId);
    }

    private void cancelWeatherLocationRequest() {
        if (weatherLocationManager != null && weatherLocationListener != null) {
            try {
                weatherLocationManager.removeUpdates(weatherLocationListener);
            } catch (SecurityException ignored) {
            }
        }
        weatherLocationManager = null;
        weatherLocationListener = null;
    }

    private boolean isRecentWeatherLocation(Location location) {
        if (location == null
                || location.getLatitude() < -90
                || location.getLatitude() > 90
                || location.getLongitude() < -180
                || location.getLongitude() > 180) {
            return false;
        }
        long age = Math.abs(System.currentTimeMillis() - location.getTime());
        return location.getTime() > 0 && age <= MAX_LAST_LOCATION_AGE_MS;
    }

    private boolean isBetterWeatherLocation(Location candidate, Location current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        long timeDifference = candidate.getTime() - current.getTime();
        if (timeDifference > 2L * 60L * 1000L) {
            return true;
        }
        if (timeDifference < -2L * 60L * 1000L) {
            return false;
        }
        float candidateAccuracy = candidate.hasAccuracy() ? candidate.getAccuracy() : Float.MAX_VALUE;
        float currentAccuracy = current.hasAccuracy() ? current.getAccuracy() : Float.MAX_VALUE;
        return candidateAccuracy < currentAccuracy;
    }

    private void showWeatherStatus(int requestId, String text) {
        if (requestId == weatherRequestId
                && "まいにちサポート".equals(currentScreenTitle)
                && weatherView != null) {
            weatherView.setText(text);
        }
    }

    private void fetchWeatherForLocation(
            final double latitude,
            final double longitude,
            final int requestId
    ) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = "--℃ 取得失敗";
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(String.format(Locale.US,
                            "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,weather_code,precipitation,rain,showers,snowfall&timezone=auto&timeformat=unixtime&forecast_days=1",
                            latitude,
                            longitude));
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        throw new IllegalStateException("Weather request failed");
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();
                    JSONObject rootObject = new JSONObject(builder.toString());
                    JSONObject current = rootObject.getJSONObject("current");
                    long weatherTimeMillis = current.getLong("time") * 1000L;
                    if (Math.abs(System.currentTimeMillis() - weatherTimeMillis) > 3L * 60L * 60L * 1000L) {
                        throw new IllegalStateException("Weather data is stale");
                    }
                    int temperature = (int) Math.round(current.getDouble("temperature_2m"));
                    int code = current.getInt("weather_code");
                    double precipitation = current.optDouble("precipitation", 0);
                    double rain = current.optDouble("rain", 0);
                    double showers = current.optDouble("showers", 0);
                    double snowfall = current.optDouble("snowfall", 0);
                    boolean snowing = snowfall > 0 || isSnowCode(code);
                    boolean raining = !snowing
                            && (precipitation > 0 || rain > 0 || showers > 0 || isRainCode(code));
                    result = temperature + "℃ " + simpleWeatherName(code, raining, snowing);
                } catch (Exception ignored) {
                    result = "--℃ 取得失敗";
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                final String weatherText = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showWeatherStatus(requestId, weatherText);
                    }
                });
            }
        }).start();
    }

    private String simpleWeatherName(int code, boolean raining, boolean snowing) {
        if (snowing) {
            return "雪";
        }
        if (raining) {
            return "雨";
        }
        if (code == 0 || code == 1) {
            return "晴れ";
        }
        if (code == 2 || code == 3 || code == 45 || code == 48) {
            return "曇り";
        }
        return "天気不明";
    }

    private boolean isRainCode(int code) {
        return (code >= 51 && code <= 67)
                || (code >= 80 && code <= 82)
                || (code >= 95 && code <= 99);
    }

    private boolean isSnowCode(int code) {
        return (code >= 71 && code <= 77)
                || code == 85
                || code == 86;
    }

    private void addStepCounterPanel() {
        LinearLayout panel = card();
        TextView title = bodyText(selectedStepDayOffset == 0 ? "今日の歩数" : "昨日の歩数");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        Button today = smallButton("今日", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedStepDayOffset = 0;
                showStepCounter();
            }
        });
        Button yesterday = smallButton("昨日", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedStepDayOffset = -1;
                showStepCounter();
            }
        });
        today.setBackground(japaneseBox(selectedStepDayOffset == 0 ? COLOR_ACCENT : COLOR_SECONDARY, 6, 1, COLOR_LINE));
        yesterday.setBackground(japaneseBox(selectedStepDayOffset == -1 ? COLOR_ACCENT : COLOR_SECONDARY, 6, 1, COLOR_LINE));
        switchRow.addView(today, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams yesterdayParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        yesterdayParams.setMargins(dp(8), 0, 0, 0);
        switchRow.addView(yesterday, yesterdayParams);
        panel.addView(switchRow, matchWrapWithBottom(8));

        stepCountView = bodyText(displayedStepCount() + " 歩");
        stepCountView.setTextSize(34);
        stepCountView.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(stepCountView);

        stepSensorStatusView = bodyText(stepCounterMessage());
        panel.addView(stepSensorStatusView);

        TextView graphTitle = bodyText("最近7日間の歩数（日別）");
        graphTitle.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(graphTitle);

        stepGraphSelectionView = bodyText(formatWeeklyStepSelection(
                selectedStepGraphIndex,
                getWeeklySteps()[selectedStepGraphIndex]
        ));
        stepGraphSelectionView.setTextSize(scaledTextSize(24));
        stepGraphSelectionView.setTypeface(Typeface.DEFAULT_BOLD);
        stepGraphSelectionView.setGravity(Gravity.CENTER);
        stepGraphSelectionView.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        stepGraphSelectionView.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.addView(stepGraphSelectionView, matchWrapWithBottom(8));

        stepGraphView = new StepGraphView(this);
        stepGraphView.setSteps(getWeeklySteps());
        stepGraphView.setSelectedIndex(selectedStepGraphIndex);
        stepGraphView.setOnDaySelectedListener(new OnStepGraphDaySelectedListener() {
            @Override
            public void onDaySelected(int index, int steps) {
                selectedStepGraphIndex = index;
                if (stepGraphSelectionView != null) {
                    stepGraphSelectionView.setText(formatWeeklyStepSelection(index, steps));
                }
            }
        });
        panel.addView(stepGraphView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(210)
        ));

        root.addView(panel);
        updateStepCounterText();
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
            stepCountView.setText(displayedStepCount() + " 歩");
        }
        if (stepSensorStatusView != null) {
            stepSensorStatusView.setText(stepCounterMessage());
        }
        if (stepGraphView != null) {
            int[] weeklySteps = getWeeklySteps();
            stepGraphView.setSteps(weeklySteps);
            if (stepGraphSelectionView != null) {
                stepGraphSelectionView.setText(formatWeeklyStepSelection(
                        selectedStepGraphIndex,
                        weeklySteps[selectedStepGraphIndex]
                ));
            }
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

    private String dateKeyWithOffset(int dayOffset) {
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(calendar.getTime());
    }

    private int displayedStepCount() {
        if (selectedStepDayOffset == 0) {
            return stepsToday;
        }
        return loadStepHistory().optInt(dateKeyWithOffset(selectedStepDayOffset), 0);
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

    private String formatWeeklyStepSelection(int index, int steps) {
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        calendar.add(Calendar.DAY_OF_YEAR, index - 6);
        String[] weekdays = {"日", "月", "火", "水", "木", "金", "土"};
        return String.format(
                Locale.JAPAN,
                "%d月%d日（%s） %,d歩",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1],
                steps
        );
    }

    private void showNotes() {
        beginScreen("メモ", "大きな文字で見やすいメモ");
        root.addView(backButton());

        Button add = bigButton("メモを追加", "メモを追加", COLOR_NOTES, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog();
            }
        });
        root.addView(add);

        List<String> notes = loadNotes();
        if (notes.isEmpty()) {
            root.addView(bodyText("まだメモはありません"));
            addAdBanner();
            return;
        }

        for (int i = 0; i < notes.size(); i++) {
            final int index = i;
            String note = notes.get(i);
            LinearLayout card = card();
            TextView noteText = bodyText(note);
            card.addView(noteText);
            card.addView(compactDeleteOnlyRow(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmation(
                            "メモを削除",
                            "削除するメモ",
                            note,
                            "メモを削除しました",
                            () -> {
                                deleteNote(index);
                                showNotes();
                            }
                    );
                }
            }));
            root.addView(card);
        }
        addAdBanner();
    }

    private void showNoteDialog() {
        final EditText input = new EditText(this);
        input.setTextSize(24);
        input.setMinLines(4);
        input.setGravity(Gravity.TOP);
        input.setHint("メモを書く");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新しいメモ")
                .setView(input)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) {
                    input.setError("メモを入力してください");
                    return;
                }
                addNote(value);
                dialog.dismiss();
                hideKeyboard(input);
                showNotes();
            });
        });

        dialog.show();
        applyGlobalBold(dialog);
        input.requestFocus();
        showKeyboard(input);
    }

    private void showMedicines() {
        beginScreen("薬アラーム", "薬の時間を音でお知らせ");
        root.addView(backButton());
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
                item.addView(compactDeleteOnlyRow(v -> showDeleteConfirmation(
                        "薬を削除",
                        "削除する薬",
                        medicine.title,
                        "薬を削除しました",
                        () -> {
                            deleteTimedItem(KEY_MEDICINES, index);
                            showMedicines();
                        }
                )));
            } else {
                item.addView(primaryDeleteActionRow(
                        "確認しました",
                        COLOR_MEDICINE,
                        v -> {
                            confirmTimedItem(KEY_MEDICINES, index, true);
                            showMedicines();
                        },
                        v -> showDeleteConfirmation(
                                "薬を削除",
                                "削除する薬",
                                medicine.title,
                                "薬を削除しました",
                                () -> {
                                    deleteTimedItem(KEY_MEDICINES, index);
                                    showMedicines();
                                }
                        )
                ));
            }
            root.addView(item);
        }
        addAdBanner();
    }

    private void showMedicineDialog() {
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
        input.setTextSize(24);
        input.setHint("例：朝の薬");
        input.setSingleLine(true);
        input.setMaxLines(1);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        input.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.addView(input, matchWrap());

        prepareDraftMedicineTime();

        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));

        input.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterPressed = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || enterPressed) {
                moveFromKeyboardToSection(input, timePanel);
                return true;
            }
            return false;
        });
        Button moveToTimeButton = smallButton("次へ：時間を選ぶ", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveFromKeyboardToSection(input, timePanel);
            }
        });
        moveToTimeButton.setBackground(japaneseBox(COLOR_ACCENT, 6, 1, COLOR_ACCENT));
        panel.addView(moveToTimeButton);

        panel.addView(sectionTitle("時間"));
        panel.addView(timePanel, matchWrap());

        final TextView timeDisplay = bodyText("");
        timeDisplay.setTextSize(40);
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
                    input.setError("薬の名前を入力してください");
                    return;
                }
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(Calendar.HOUR_OF_DAY, draftMedicineHour);
                calendar.set(Calendar.MINUTE, draftMedicineMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                CalendarEvent medicine = new CalendarEvent("薬: " + name, calendar.getTimeInMillis());
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
        showKeyboard(input);
    }

    private void showEmergency() {
        beginScreen("家族", "家族や病院へすぐ電話");
        root.addView(backButton());
        List<FamilyContact> contacts = loadFamilyContacts();
        int limit = isPremiumActive() ? 5 : 1;

        root.addView(bigButton("家族を追加", "家族の連絡先を追加", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadFamilyContacts().size() >= (isPremiumActive() ? 5 : 1)) {
                    Toast.makeText(MainActivity.this, isPremiumActive() ? "家族は最大5人までです" : "無料版は1人までです", Toast.LENGTH_LONG).show();
                    if (!isPremiumActive()) {
                        showPremiumScreen();
                    }
                    return;
                }
                showFamilyContactDialog(-1);
            }
        }));

        root.addView(bodyText(isPremiumActive() ? "プレミアム会員: 最大5人まで登録できます。" : "無料版: 1人まで登録できます。"));

        if (contacts.isEmpty()) {
            root.addView(bodyText("家族が未設定です"));
            addAdBanner();
            return;
        }

        for (int i = 0; i < contacts.size(); i++) {
            final int index = i;
            FamilyContact contact = contacts.get(i);
            LinearLayout panel = card();
            TextView current = bodyText(contact.name.isEmpty() ? contact.phone : contact.name + "\n" + contact.phone);
            current.setTypeface(Typeface.DEFAULT_BOLD);
            panel.addView(current);
            panel.addView(bigButton("電話する", "登録した番号に電話", COLOR_EMERGENCY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callFamilyContact(index);
                }
            }));
            panel.addView(primaryDeleteActionRow(
                    "編集",
                    COLOR_SECONDARY,
                    v -> showFamilyContactDialog(index),
                    v -> showDeleteConfirmation(
                            "家族を削除",
                            "削除する連絡先",
                            contact.name.isEmpty() ? contact.phone : contact.name,
                            "家族の連絡先を削除しました",
                            () -> {
                                deleteFamilyContact(index);
                                showEmergency();
                            }
                    )
            ));
            root.addView(panel);
        }
        addAdBanner();
    }

    private void showFamilyContactDialog(final int editIndex) {
        List<FamilyContact> contacts = loadFamilyContacts();
        FamilyContact existing = editIndex >= 0 && editIndex < contacts.size() ? contacts.get(editIndex) : null;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), 0, dp(16), 0);

        final EditText nameInput = new EditText(this);
        nameInput.setTextSize(24);
        nameInput.setHint("名前 例：長男");
        nameInput.setText(existing == null ? "" : existing.name);
        layout.addView(nameInput, matchWrap());

        final EditText phoneInput = new EditText(this);
        phoneInput.setTextSize(24);
        phoneInput.setHint("電話番号 例：09012345678");
        phoneInput.setText(existing == null ? "" : existing.phone);
        layout.addView(phoneInput, matchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("家族の連絡先")
                .setView(layout)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                if (phone.isEmpty()) {
                    phoneInput.setError("電話番号を入力してください");
                    return;
                }
                saveFamilyContact(editIndex, new FamilyContact(name, phone));
                dialog.dismiss();
                hideKeyboard(phoneInput);
                showEmergency();
            });
        });
        dialog.show();
        applyGlobalBold(dialog);
        nameInput.requestFocus();
        showKeyboard(nameInput);
    }

    private void callFamilyContact(int index) {
        List<FamilyContact> contacts = loadFamilyContacts();
        if (index < 0 || index >= contacts.size()) {
            showFamilyContactDialog(-1);
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contacts.get(index).phone)));
    }

    private void showTodos() {
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
            item.addView(primaryDeleteActionRow(
                    todo.done ? "戻す" : "完了",
                    COLOR_TODO,
                    v -> {
                        toggleTodoDone(index);
                        showTodos();
                    },
                    v -> showDeleteConfirmation(
                            "やることを削除",
                            "削除する内容",
                            todo.text,
                            "やることを削除しました",
                            () -> {
                                deleteTodo(index);
                                showTodos();
                            }
                    )
            ));
            root.addView(item);
        }
        addAdBanner();
    }

    private void showShopping() {
        beginScreen("買い物リスト", "商品名と数量を大きく表示");
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        Button back = backButton();
        topRow.addView(back, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button clear = smallButton("全て削除", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation(
                        "買い物リストを削除",
                        "削除する内容",
                        "登録されている買う物をすべて",
                        "買い物リストを全て削除しました",
                        () -> {
                            clearShoppingItems();
                            showShopping();
                        }
                );
            }
        });
        clear.setBackground(japaneseBox(COLOR_EMERGENCY, 6, 1, COLOR_EMERGENCY));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clearParams.setMargins(dp(8), 0, 0, 0);
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

            item.addView(primaryDeleteActionRow(
                    value.bought ? "戻す" : "買った",
                    COLOR_SHOPPING,
                    v -> {
                        toggleShoppingBought(index);
                        showShopping();
                    },
                    v -> showDeleteConfirmation(
                            "買う物を削除",
                            "削除する物",
                            value.name,
                            "買う物を削除しました",
                            () -> {
                                deleteShoppingItem(index);
                                showShopping();
                            }
                    )
            ));
            root.addView(item);
        }
        addAdBanner();
    }

    private String formatShoppingItem(ShoppingItem item) {
        StringBuilder builder = new StringBuilder(item.name);
        if (!item.amount.isEmpty()) {
            builder.append("\n数量: ").append(item.amount);
        }
        return builder.toString();
    }

    private void showShoppingDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), 0, dp(16), 0);

        final EditText nameInput = new EditText(this);
        nameInput.setTextSize(scaledTextSize(24));
        nameInput.setHint("商品名");
        nameInput.setSingleLine(true);
        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        layout.addView(nameInput, matchWrap());

        final EditText amountInput = new EditText(this);
        amountInput.setTextSize(scaledTextSize(24));
        amountInput.setHint("数量");
        amountInput.setSingleLine(true);
        amountInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        layout.addView(amountInput, matchWrap());

        nameInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                amountInput.requestFocus();
                return true;
            }
            return false;
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("買う物を追加")
                .setView(layout)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    nameInput.setError("名前を入力してください");
                    return;
                }
                addShoppingItem(new ShoppingItem(
                        name,
                        amountInput.getText().toString().trim(),
                        false
                ));
                dialog.dismiss();
                hideKeyboard(nameInput);
                showShopping();
            });
        });
        dialog.show();
        applyGlobalBold(dialog);
        nameInput.requestFocus();
        showKeyboard(nameInput);
    }

    private void showSimpleItemDialog(String title, String key, String hint, boolean doneMode) {
        final EditText input = new EditText(this);
        input.setTextSize(24);
        input.setHint(hint);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) {
                    input.setError("入力してください");
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
        applyGlobalBold(dialog);
        input.requestFocus();
        showKeyboard(input);
    }

    private void showCalendar() {
        beginScreen("予定", "かんたんな予定のお知らせ");
        root.addView(backButton());

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
                card.addView(compactDeleteOnlyRow(v ->
                        showDeleteEventConfirmation(index, event.title)));
            } else {
                card.addView(primaryDeleteActionRow(
                        "確認しました",
                        COLOR_SCHEDULE,
                        v -> {
                            confirmEvent(index);
                            showCalendar();
                        },
                        v -> showDeleteEventConfirmation(index, event.title)
                ));
            }
            root.addView(card);
        }
        addAdBanner();
    }

    private void showDeleteEventConfirmation(final int index, String eventTitle) {
        showDeleteConfirmation(
                "予定を削除",
                "削除する予定",
                eventTitle,
                "予定を削除しました",
                () -> {
                    deleteEvent(index);
                    showCalendar();
                }
        );
    }

    private void showDeleteConfirmation(
            String dialogTitle,
            String itemLabel,
            String itemName,
            String successMessage,
            Runnable deleteAction
    ) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(22), dp(24), dp(24));
        panel.setBackground(japaneseBox(COLOR_CARD, 8, 1, COLOR_ACCENT));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        View accent = new View(this);
        accent.setBackgroundColor(COLOR_ACCENT);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(4), dp(38));
        accentParams.setMargins(0, 0, dp(12), 0);
        titleRow.addView(accent, accentParams);

        TextView title = new TextView(this);
        title.setText(dialogTitle);
        title.setTextSize(scaledTextSize(28));
        title.setTextColor(COLOR_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(title);
        panel.addView(titleRow, matchWrapWithBottom(18));

        TextView question = bodyText("削除してよろしいですか？");
        question.setTextSize(scaledTextSize(21));
        panel.addView(question);

        LinearLayout eventBox = new LinearLayout(this);
        eventBox.setOrientation(LinearLayout.VERTICAL);
        eventBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        eventBox.setBackground(japaneseBox(COLOR_BG, 6, 1, COLOR_LINE));

        TextView eventLabel = new TextView(this);
        eventLabel.setText(itemLabel);
        eventLabel.setTextSize(scaledTextSize(16));
        eventLabel.setTextColor(COLOR_ACCENT);
        eventLabel.setTypeface(Typeface.DEFAULT_BOLD);
        eventBox.addView(eventLabel);

        TextView eventName = new TextView(this);
        eventName.setText(itemName);
        eventName.setTextSize(scaledTextSize(25));
        eventName.setTextColor(COLOR_TEXT);
        eventName.setTypeface(Typeface.DEFAULT_BOLD);
        eventName.setMaxLines(5);
        eventName.setEllipsize(TextUtils.TruncateAt.END);
        eventName.setPadding(0, dp(6), 0, 0);
        eventBox.addView(eventName);
        panel.addView(eventBox, matchWrapWithBottom(12));

        TextView warning = new TextView(this);
        warning.setText("削除すると元に戻せません");
        warning.setTextSize(scaledTextSize(18));
        warning.setTextColor(COLOR_EMERGENCY);
        warning.setGravity(Gravity.CENTER);
        warning.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(warning, matchWrapWithBottom(20));

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button cancelButton = dialogActionButton("戻る", COLOR_SECONDARY);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(68), 1f);
        cancelParams.setMargins(0, 0, dp(6), 0);
        buttonRow.addView(cancelButton, cancelParams);

        Button deleteButton = dialogActionButton("削除する", COLOR_EMERGENCY);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(68), 1f);
        deleteParams.setMargins(dp(6), 0, 0, 0);
        buttonRow.addView(deleteButton, deleteParams);
        panel.addView(buttonRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(panel)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteAction.run();
            Toast.makeText(MainActivity.this, successMessage, Toast.LENGTH_SHORT).show();
        });
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
        applyGlobalBold(dialog);
    }

    private Button dialogActionButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(scaledTextSize(21));
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setBackground(japaneseBox(color, 6, 1, color));
        return button;
    }

    private LinearLayout primaryDeleteActionRow(
            String primaryLabel,
            int primaryColor,
            View.OnClickListener primaryListener,
            View.OnClickListener deleteListener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button primaryButton = smallButton(primaryLabel, primaryListener);
        primaryButton.setBackground(japaneseBox(primaryColor, 6, 1, primaryColor));
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(0, dp(58), 3f);
        primaryParams.setMargins(0, 0, dp(6), 0);
        row.addView(primaryButton, primaryParams);

        Button deleteButton = compactDeleteButton(deleteListener);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        deleteParams.setMargins(dp(6), dp(5), 0, dp(5));
        row.addView(deleteButton, deleteParams);
        return row;
    }

    private LinearLayout compactDeleteOnlyRow(View.OnClickListener deleteListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(48), 3f));

        Button deleteButton = compactDeleteButton(deleteListener);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        deleteParams.setMargins(dp(12), 0, 0, 0);
        row.addView(deleteButton, deleteParams);
        return row;
    }

    private Button compactDeleteButton(View.OnClickListener listener) {
        Button button = smallButton("削除", listener);
        button.setTextSize(scaledTextSize(16));
        button.setMinHeight(0);
        button.setPadding(dp(6), dp(4), dp(6), dp(4));
        button.setBackground(japaneseBox(COLOR_EMERGENCY, 6, 1, COLOR_EMERGENCY));
        return button;
    }

    private void showEventForm() {
        beginScreen("予定を追加", "日付と時間を選んで保存");
        root.addView(backToCalendarButton());

        LinearLayout panel = card();

        TextView nameLabel = sectionTitle("予定の名前");
        panel.addView(nameLabel);

        final EditText title = new EditText(this);
        title.setTextSize(24);
        title.setHint("例：病院");
        title.setSingleLine(false);
        title.setMinLines(1);
        title.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        title.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.addView(title, matchWrap());

        TextView dateLabel = sectionTitle("日付");
        panel.addView(dateLabel);

        final DatePicker datePicker = new DatePicker(this);
        datePicker.setCalendarViewShown(false);
        panel.addView(datePicker, matchWrap());

        TextView timeLabel = sectionTitle("時間");
        panel.addView(timeLabel);

        prepareDraftEventTime();

        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        panel.addView(timePanel, matchWrap());

        final TextView timeDisplay = bodyText("");
        timeDisplay.setTextSize(40);
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
                    title.setError("名前を入力してください");
                    return;
                }
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(Calendar.YEAR, datePicker.getYear());
                calendar.set(Calendar.MONTH, datePicker.getMonth());
                calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                calendar.set(Calendar.HOUR_OF_DAY, draftEventHour);
                calendar.set(Calendar.MINUTE, draftEventMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                CalendarEvent event = new CalendarEvent(name, calendar.getTimeInMillis());
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
        title.requestFocus();
        showKeyboard(title);
    }

    private void showPremiumScreen() {
        beginScreen("プレミアム", "2つのプランから選べます");
        root.addView(backButton());

        LinearLayout benefitsPanel = card();
        TextView title = bodyText("プレミアム会員");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        benefitsPanel.addView(title);
        benefitsPanel.addView(bodyText("Google広告を表示しません。"));
        benefitsPanel.addView(bodyText("家族を最大5人まで登録できます。"));
        root.addView(benefitsPanel);

        if (isPremiumActive()) {
            LinearLayout statusPanel = card();
            PlayBillingManager.PremiumPlan plan = premiumPlan();
            TextView statusTitle = bodyText(plan == PlayBillingManager.PremiumPlan.LIFETIME
                    ? "買い切りプランを購入済み"
                    : "月額プランを利用中");
            statusTitle.setTextSize(26);
            statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
            statusPanel.addView(statusTitle);
            statusPanel.addView(bodyText("現在、広告は非表示です。"));
            if (plan == PlayBillingManager.PremiumPlan.LIFETIME) {
                statusPanel.addView(bodyText("追加のお支払いはありません。ずっと利用できます。"));
            } else {
                statusPanel.addView(bodyText("毎月自動更新。Google Playでいつでも解約できます。"));
                statusPanel.addView(bigButton("定期購入を管理・解約", "Google Playで月額プランを管理", COLOR_EMERGENCY, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (billingManager != null) {
                            billingManager.openManageSubscription(MainActivity.this);
                        }
                    }
                }));
            }
            root.addView(statusPanel);
        } else {
            String monthlyPrice = billingManager == null ? "¥500 / 月" : billingManager.getMonthlyPrice();
            LinearLayout monthlyPanel = card();
            TextView monthlyTitle = bodyText("月額プラン");
            monthlyTitle.setTextSize(26);
            monthlyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            monthlyPanel.addView(monthlyTitle);
            monthlyPanel.addView(bodyText(monthlyPrice));
            monthlyPanel.addView(bodyText("毎月自動更新。いつでも解約できます。"));
            monthlyPanel.addView(bigButton(monthlyPrice + "で登録", "Google Playで月額プランに登録", COLOR_ACCENT, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (billingManager != null) {
                        billingManager.launchMonthlyPurchase(MainActivity.this);
                    }
                }
            }));
            root.addView(monthlyPanel);

            String lifetimePrice = billingManager == null ? "¥3,000" : billingManager.getLifetimePrice();
            LinearLayout lifetimePanel = card();
            TextView lifetimeTitle = bodyText("買い切りプラン");
            lifetimeTitle.setTextSize(26);
            lifetimeTitle.setTypeface(Typeface.DEFAULT_BOLD);
            lifetimePanel.addView(lifetimeTitle);
            lifetimePanel.addView(bodyText(lifetimePrice));
            lifetimePanel.addView(bodyText("一度のお支払いで、ずっと利用できます。"));
            lifetimePanel.addView(bigButton(lifetimePrice + "で購入", "Google Playで買い切りプランを購入", COLOR_PRIMARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (billingManager != null) {
                        billingManager.launchLifetimePurchase(MainActivity.this);
                    }
                }
            }));
            root.addView(lifetimePanel);

            if (billingManager != null && !billingManager.isAnyProductAvailable()) {
                root.addView(bodyText(billingManager.isProductQueryFinished()
                        ? "Google Playで商品を取得できませんでした。Playストア版でお試しください。"
                        : "Google Playの商品情報を確認しています。"));
            }
            addAdBanner();
        }

        Button restore = bigButton("購入を復元", "以前のプレミアム購入を復元", COLOR_SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (billingManager != null) {
                    billingManager.refreshPurchases(true);
                }
            }
        });
        root.addView(restore);
    }

    private void showSettings() {
        beginScreen("設定", "文字の見やすさとバックアップ");
        root.addView(backButton());

        LinearLayout sizePanel = card();
        sizePanel.addView(sectionTitle("文字サイズ"));
        sizePanel.addView(bodyText("見やすい大きさを選べます。"));
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.addView(settingSizeButton("大", 1.0f), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        middle.setMargins(dp(8), 0, dp(8), 0);
        sizeRow.addView(settingSizeButton("特大", 1.18f), middle);
        sizeRow.addView(settingSizeButton("最大", 1.32f), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sizePanel.addView(sizeRow, matchWrap());

        sizePanel.addView(sectionTitle("文字の太さ"));
        sizePanel.addView(bodyText("すべての文字を太くできます。"));
        LinearLayout boldRow = new LinearLayout(this);
        boldRow.setOrientation(LinearLayout.HORIZONTAL);
        Button standardText = settingBoldButton("標準", false);
        Button boldText = settingBoldButton("すべて太字", true);
        boldRow.addView(standardText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams boldParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        boldParams.setMargins(dp(8), 0, 0, 0);
        boldRow.addView(boldText, boldParams);
        sizePanel.addView(boldRow, matchWrap());
        root.addView(sizePanel);

        LinearLayout backupPanel = card();
        backupPanel.addView(sectionTitle("バックアップ"));
        backupPanel.addView(bodyText("メモ、予定、薬、連絡先、買い物リストを共有またはコピーできます。"));
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
        beginScreen("家族の連絡先", "困った時にすぐ電話できます");

        LinearLayout panel = card();
        panel.addView(sectionTitle("家族や病院の連絡先"));

        final EditText nameInput = new EditText(this);
        nameInput.setTextSize(scaledTextSize(24));
        nameInput.setHint("名前 例：長男");
        List<FamilyContact> existingContacts = loadFamilyContacts();
        if (!existingContacts.isEmpty()) {
            nameInput.setText(existingContacts.get(0).name);
        }
        nameInput.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        nameInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.addView(nameInput, matchWrap());

        final EditText phoneInput = new EditText(this);
        phoneInput.setTextSize(scaledTextSize(24));
        phoneInput.setHint("電話番号 例：09012345678");
        if (!existingContacts.isEmpty()) {
            phoneInput.setText(existingContacts.get(0).phone);
        }
        phoneInput.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        phoneInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.addView(phoneInput, matchWrap());

        panel.addView(bigButton("保存して次へ", "家族の連絡先を保存して次へ", COLOR_EMERGENCY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = phoneInput.getText().toString().trim();
                if (phone.isEmpty()) {
                    phoneInput.setError("電話番号を入力してください");
                    return;
                }
                saveFamilyContact(0, new FamilyContact(nameInput.getText().toString().trim(), phone));
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
        showKeyboard(nameInput);
    }

    private void showOnboardingMedicine() {
        beginScreen("薬の時間", "毎日の薬を忘れにくくします");

        LinearLayout panel = card();
        panel.addView(sectionTitle("薬アラーム"));

        final EditText medicineInput = new EditText(this);
        medicineInput.setTextSize(scaledTextSize(24));
        medicineInput.setHint("例：朝の薬");
        medicineInput.setSingleLine(true);
        medicineInput.setMaxLines(1);
        medicineInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        medicineInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        medicineInput.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));
        medicineInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        panel.addView(medicineInput, matchWrap());

        prepareDraftMedicineTime();
        LinearLayout timePanel = new LinearLayout(this);
        timePanel.setOrientation(LinearLayout.VERTICAL);
        timePanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePanel.setBackground(japaneseBox(Color.WHITE, 6, 1, COLOR_LINE));

        medicineInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterPressed = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || enterPressed) {
                moveFromKeyboardToSection(medicineInput, timePanel);
                return true;
            }
            return false;
        });
        Button moveToTimeButton = smallButton("次へ：時間を選ぶ", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveFromKeyboardToSection(medicineInput, timePanel);
            }
        });
        moveToTimeButton.setBackground(japaneseBox(COLOR_ACCENT, 6, 1, COLOR_ACCENT));
        panel.addView(moveToTimeButton);

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
                    medicineInput.setError("薬の名前を入力してください");
                    return;
                }
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(Calendar.HOUR_OF_DAY, draftMedicineHour);
                calendar.set(Calendar.MINUTE, draftMedicineMinute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                CalendarEvent medicine = new CalendarEvent("薬: " + name, calendar.getTimeInMillis());
                addTimedItem(KEY_MEDICINES, medicine);
                scheduleEventAlarm(medicine);
                resetDraftMedicineTime();
                hideKeyboard(medicineInput);
                finishOnboarding();
            }
        }));
        panel.addView(smallButton("あとで設定する", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDraftMedicineTime();
                hideKeyboard(medicineInput);
                finishOnboarding();
            }
        }));
        root.addView(panel);
        medicineInput.requestFocus();
        showKeyboard(medicineInput);
    }

    private void finishOnboarding() {
        prefs().edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
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
            button.setBackground(japaneseBox(COLOR_ACCENT, 6, 1, COLOR_ACCENT));
        }
        return button;
    }

    private Button settingBoldButton(String label, final boolean enabled) {
        Button button = smallButton(label, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs().edit().putBoolean(KEY_BOLD_TEXT, enabled).apply();
                Toast.makeText(
                        MainActivity.this,
                        enabled ? "すべての文字を太字にしました" : "文字の太さを標準にしました",
                        Toast.LENGTH_SHORT
                ).show();
                showSettings();
            }
        });
        if (isBoldTextEnabled() == enabled) {
            button.setBackground(japaneseBox(COLOR_ACCENT, 6, 1, COLOR_ACCENT));
        }
        return button;
    }

    private void beginScreen(String title, String subtitle) {
        currentScreenTitle = title;
        FrameLayout screen = new FrameLayout(this);
        screen.addView(new WashiBackgroundView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        currentScrollView = new ScrollView(this);
        ScrollView scrollView = currentScrollView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setPadding(0, getStatusBarHeight(), 0, 0);
        scrollView.setClipToPadding(true);

        root = new BoldAwareLinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        root.setBackgroundColor(Color.TRANSPARENT);
        scrollView.addView(root, matchWrap());

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutParams(matchWrap());

        View accent = new View(this);
        accent.setBackgroundColor(COLOR_ACCENT);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(4), dp(34));
        accentParams.setMargins(0, 0, dp(12), 0);
        titleRow.addView(accent, accentParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(32);
        titleView.setTextColor(COLOR_TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.START);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        root.addView(titleRow);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextSize(20);
        subtitleView.setTextColor(COLOR_MUTED);
        root.addView(subtitleView, matchWrapWithBottom(24));

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
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(japaneseBox(color, 6, 1, COLOR_ACCENT));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(78));
        button.setContentDescription(contentDescription);
        button.setOnClickListener(listener);
        button.setPadding(dp(16), dp(12), dp(16), dp(12));
        button.setLayoutParams(matchWrap());
        return button;
    }

    private Button homeFeatureButton(String label, String contentDescription, int color, View.OnClickListener listener) {
        Button button = bigButton(label, contentDescription, color, listener);
        button.setTextSize(scaledTextSize(21));
        button.setMinHeight(dp(88));
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
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
        button.setTextColor(Color.WHITE);
        button.setBackground(japaneseBox(COLOR_SECONDARY, 6, 1, COLOR_LINE));
        button.setMinHeight(dp(56));
        button.setOnClickListener(listener);
        button.setLayoutParams(matchWrap());
        return button;
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
        button.setTextSize(24);
        button.setContentDescription(contentDescription);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1f);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private void updateTimeDisplay(TextView display, int hour, int minute) {
        display.setText(String.format(Locale.JAPAN, "%02d:%02d", hour, minute));
    }

    private void prepareDraftEventTime() {
        if (draftEventHour >= 0 && draftEventMinute >= 0) {
            return;
        }
        Calendar initialTime = Calendar.getInstance(Locale.JAPAN);
        draftEventHour = initialTime.get(Calendar.HOUR_OF_DAY);
        draftEventMinute = initialTime.get(Calendar.MINUTE);
    }

    private void resetDraftEventTime() {
        draftEventHour = -1;
        draftEventMinute = -1;
    }

    private void prepareDraftMedicineTime() {
        if (draftMedicineHour >= 0 && draftMedicineMinute >= 0) {
            return;
        }
        Calendar initialTime = Calendar.getInstance(Locale.JAPAN);
        draftMedicineHour = initialTime.get(Calendar.HOUR_OF_DAY);
        draftMedicineMinute = initialTime.get(Calendar.MINUTE);
    }

    private void resetDraftMedicineTime() {
        draftMedicineHour = -1;
        draftMedicineMinute = -1;
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
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(japaneseBox(COLOR_CARD, 6, 1, COLOR_LINE));
        card.setElevation(dp(1));
        card.setMinimumHeight(dp(96));
        card.setLayoutParams(matchWrapWithBottom(14));
        return card;
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

    private boolean isBoldTextEnabled() {
        return prefs().getBoolean(KEY_BOLD_TEXT, false);
    }

    private void applyGlobalBold(View view) {
        if (!isBoldTextEnabled() || view == null) {
            return;
        }
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.DEFAULT_BOLD);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyGlobalBold(group.getChildAt(i));
            }
        }
    }

    private void applyGlobalBold(AlertDialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            applyGlobalBold(dialog.getWindow().getDecorView());
        }
    }

    private float scaledTextSize(float baseSize) {
        return baseSize * textScale();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
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
                            object.optBoolean("bought", false)
                    ));
                } else {
                    items.add(new ShoppingItem(String.valueOf(value), "", false));
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
                object.put("bought", item.bought);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString(KEY_SHOPPING, array.toString()).apply();
    }

    private List<FamilyContact> loadFamilyContacts() {
        List<FamilyContact> contacts = new ArrayList<>();
        String raw = prefs().getString(KEY_FAMILY_CONTACTS, "");
        if (!raw.isEmpty()) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    String phone = item.optString("phone", "").trim();
                    if (!phone.isEmpty()) {
                        contacts.add(new FamilyContact(item.optString("name", ""), phone));
                    }
                }
            } catch (JSONException ignored) {
                prefs().edit().remove(KEY_FAMILY_CONTACTS).apply();
            }
        }
        if (contacts.isEmpty()) {
            String legacyPhone = prefs().getString(KEY_EMERGENCY_PHONE, "").trim();
            if (!legacyPhone.isEmpty()) {
                contacts.add(new FamilyContact(prefs().getString(KEY_EMERGENCY_NAME, ""), legacyPhone));
                saveFamilyContacts(contacts);
            }
        }
        int limit = isPremiumActive() ? 5 : 1;
        if (contacts.size() > limit) {
            return new ArrayList<>(contacts.subList(0, limit));
        }
        return contacts;
    }

    private void saveFamilyContact(int index, FamilyContact contact) {
        List<FamilyContact> contacts = loadFamilyContacts();
        int limit = isPremiumActive() ? 5 : 1;
        if (index >= 0 && index < contacts.size()) {
            contacts.set(index, contact);
        } else {
            if (contacts.size() >= limit) {
                return;
            }
            contacts.add(contact);
        }
        saveFamilyContacts(contacts);
    }

    private void deleteFamilyContact(int index) {
        List<FamilyContact> contacts = loadFamilyContacts();
        if (index >= 0 && index < contacts.size()) {
            contacts.remove(index);
            saveFamilyContacts(contacts);
        }
    }

    private void saveFamilyContacts(List<FamilyContact> contacts) {
        JSONArray array = new JSONArray();
        int limit = isPremiumActive() ? 5 : 1;
        for (int i = 0; i < contacts.size() && i < limit; i++) {
            FamilyContact contact = contacts.get(i);
            JSONObject object = new JSONObject();
            try {
                object.put("name", contact.name);
                object.put("phone", contact.phone);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs().edit().putString(KEY_FAMILY_CONTACTS, array.toString()).apply();
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
            events.get(index).confirmed = true;
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
        return items;
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
        List<CalendarEvent> events = loadEvents();
        long now = System.currentTimeMillis();
        for (CalendarEvent event : events) {
            if (!event.confirmed && event.timeMillis > now) {
                scheduleEventAlarm(event);
            }
        }
        List<CalendarEvent> medicines = loadTimedList(KEY_MEDICINES);
        for (CalendarEvent medicine : medicines) {
            if (!medicine.confirmed && medicine.timeMillis > now) {
                scheduleEventAlarm(medicine);
            }
        }
    }

    private void scheduleEventAlarm(CalendarEvent event) {
        if (event.timeMillis <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = eventAlarmPendingIntent(event);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.timeMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.timeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.timeMillis, pendingIntent);
            }
        } catch (SecurityException ignored) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, event.timeMillis, pendingIntent);
        }
    }

    private void cancelEventAlarm(CalendarEvent event) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(eventAlarmPendingIntent(event));
        }
    }

    private PendingIntent eventAlarmPendingIntent(CalendarEvent event) {
        Intent intent = new Intent(this, EventAlarmReceiver.class);
        intent.putExtra(EventAlarmReceiver.EXTRA_TITLE, event.title);
        intent.putExtra(EventAlarmReceiver.EXTRA_TIME, formatEventTime(event.timeMillis));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, event.alarmId(), intent, flags);
    }

    private void createEventAlarmChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                EventAlarmReceiver.CHANNEL_ID,
                "予定アラーム",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("予定の時間を通知音でお知らせします");
        channel.enableVibration(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), 40);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 30 || requestCode == 40) {
            registerStepSensor();
            updateStepCounterText();
            fetchWeather();
        }
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
        List<TodoItem> todos = remainingTodos();
        if (todos.isEmpty()) {
            builder.append("今日やることはありません。");
        } else {
            builder.append("今日やることは、");
            for (TodoItem todo : todos) {
                builder.append(todo.text).append("。");
            }
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
        List<FamilyContact> contacts = loadFamilyContacts();
        if (contacts.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(intent, "家族へ共有"));
            return;
        }
        if (isPremiumActive() && contacts.size() > 1) {
            String[] names = new String[contacts.size()];
            for (int i = 0; i < contacts.size(); i++) {
                FamilyContact contact = contacts.get(i);
                names[i] = contact.name.isEmpty() ? contact.phone : contact.name + "  " + contact.phone;
            }
            new AlertDialog.Builder(this)
                    .setTitle("送る家族を選ぶ")
                    .setItems(names, (dialog, which) -> sendSummaryToFamilyContact(contacts.get(which), message))
                    .setNegativeButton("キャンセル", null)
                    .show();
            return;
        }
        sendSummaryToFamilyContact(contacts.get(0), message);
    }

    private void sendSummaryToFamilyContact(FamilyContact contact, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + contact.phone));
        intent.putExtra("sms_body", message);
        startActivity(Intent.createChooser(intent, "家族へ共有"));
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
            object.put("familyContacts", new JSONArray(prefs().getString(KEY_FAMILY_CONTACTS, "[]")));
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
        startActivity(Intent.createChooser(intent, "バックアップを共有"));
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

    private void moveFromKeyboardToSection(EditText input, View section) {
        input.clearFocus();
        hideKeyboard(input);
        section.setFocusableInTouchMode(true);
        section.requestFocus();
        ScrollView scrollView = currentScrollView;
        LinearLayout screenRoot = root;
        if (scrollView == null || screenRoot == null) {
            return;
        }
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Rect sectionBounds = new Rect();
                section.getDrawingRect(sectionBounds);
                screenRoot.offsetDescendantRectToMyCoords(section, sectionBounds);
                scrollView.smoothScrollTo(0, Math.max(0, sectionBounds.top - dp(16)));
            }
        }, 250L);
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

    private boolean isPremiumActive() {
        return prefs().getBoolean(KEY_PREMIUM_ACTIVE, false);
    }

    private PlayBillingManager.PremiumPlan premiumPlan() {
        String value = prefs().getString(KEY_PREMIUM_PLAN, PlayBillingManager.PremiumPlan.NONE.name());
        try {
            return PlayBillingManager.PremiumPlan.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return PlayBillingManager.PremiumPlan.NONE;
        }
    }

    private boolean isOnboardingDone() {
        return prefs().getBoolean(KEY_ONBOARDING_DONE, false);
    }

    private void migrateFromTestPremium() {
        if (!prefs().getBoolean(KEY_REAL_BILLING_MIGRATED, false)) {
            prefs().edit()
                    .putBoolean(KEY_PREMIUM_ACTIVE, false)
                    .putString(KEY_PREMIUM_PLAN, PlayBillingManager.PremiumPlan.NONE.name())
                    .putBoolean(KEY_REAL_BILLING_MIGRATED, true)
                    .apply();
        }
    }

    private void schedulePremiumPrompt() {
        if (!billingStateChecked || !isOnboardingDone() || isPremiumActive() || premiumPromptShown || premiumPromptScheduled) {
            return;
        }
        premiumPromptScheduled = true;
        promptHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                premiumPromptScheduled = false;
                showPremiumPrompt();
            }
        }, 5000L);
    }

    private void showPremiumPrompt() {
        premiumPromptShown = true;
        if (isFinishing() || isPremiumActive()) {
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("広告なしで使えます")
                .setMessage("月額500円、または3,000円の買い切りで広告を表示しません。")
                .setPositiveButton("詳しく見る", (d, which) -> showPremiumScreen())
                .setNegativeButton("あとで", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
        });
        dialog.show();
        applyGlobalBold(dialog);
    }

    private void addAdBanner() {
        if (isPremiumActive()) {
            return;
        }
        LinearLayout ad = new LinearLayout(this);
        ad.setOrientation(LinearLayout.VERTICAL);
        ad.setPadding(dp(16), dp(12), dp(16), dp(12));
        ad.setBackground(japaneseBox(Color.rgb(248, 245, 238), 6, 1, COLOR_LINE));
        ad.setLayoutParams(matchWrapWithBottom(14));

        TextView label = bodyText("Google広告");
        label.setTextSize(18);
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
        removeAds.setBackground(japaneseBox(COLOR_ACCENT, 6, 1, COLOR_ACCENT));
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
        boolean bought;

        ShoppingItem(String name, String amount, boolean bought) {
            this.name = name;
            this.amount = amount;
            this.bought = bought;
        }
    }

    private static class FamilyContact {
        final String name;
        final String phone;

        FamilyContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    private class BoldAwareLinearLayout extends LinearLayout {
        BoldAwareLinearLayout(Context context) {
            super(context);
        }

        @Override
        public void onViewAdded(View child) {
            super.onViewAdded(child);
            applyGlobalBold(child);
        }
    }

    private interface OnStepGraphDaySelectedListener {
        void onDaySelected(int index, int steps);
    }

    private class StepGraphView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String[] labels = {"月", "火", "水", "木", "金", "土", "日"};
        private int[] steps = new int[7];
        private int selectedIndex = 6;
        private OnStepGraphDaySelectedListener onDaySelectedListener;

        StepGraphView(Context context) {
            super(context);
            setClickable(true);
            setContentDescription("最近7日間の日別歩数グラフ");
        }

        void setSteps(int[] values) {
            steps = values;
            invalidate();
        }

        void setSelectedIndex(int index) {
            selectedIndex = Math.max(0, Math.min(steps.length - 1, index));
            invalidate();
        }

        void setOnDaySelectedListener(OnStepGraphDaySelectedListener listener) {
            onDaySelectedListener = listener;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            int left = dp(8);
            int right = getWidth() - dp(8);
            if (event.getX() < left || event.getX() > right) {
                return true;
            }
            float segmentWidth = (right - left) / (float) steps.length;
            int index = (int) ((event.getX() - left) / segmentWidth);
            selectedIndex = Math.max(0, Math.min(steps.length - 1, index));
            setContentDescription(formatWeeklyStepSelection(selectedIndex, steps[selectedIndex]));
            invalidate();
            performClick();
            if (onDaySelectedListener != null) {
                onDaySelectedListener.onDaySelected(selectedIndex, steps[selectedIndex]);
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            int left = dp(8);
            int right = width - dp(8);
            int top = dp(8);
            int bottom = height - dp(54);
            int chartHeight = bottom - top;
            int max = 1;
            for (int value : steps) {
                max = Math.max(max, value);
            }

            paint.setStrokeWidth(dp(1));
            paint.setColor(COLOR_LINE);
            canvas.drawLine(left, bottom, right, bottom, paint);

            float segmentWidth = (right - left) / (float) steps.length;
            float gap = dp(6);
            float barWidth = segmentWidth - gap;
            Calendar calendar = Calendar.getInstance(Locale.JAPAN);
            calendar.add(Calendar.DAY_OF_YEAR, -6);
            for (int i = 0; i < steps.length; i++) {
                float x = left + i * segmentWidth + gap / 2f;
                float ratio = steps[i] / (float) max;
                float barTop = bottom - Math.max(dp(8), chartHeight * ratio);
                paint.setColor(i == selectedIndex ? COLOR_ACCENT : COLOR_PRIMARY);
                RectF bar = new RectF(x, barTop, x + barWidth, bottom);
                canvas.drawRoundRect(bar, dp(4), dp(4), paint);

                paint.setColor(i == selectedIndex ? COLOR_TEXT : COLOR_MUTED);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(i == selectedIndex ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                paint.setTextSize(dp(12));
                float centerX = x + barWidth / 2;
                canvas.drawText(
                        (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DAY_OF_MONTH),
                        centerX,
                        height - dp(26),
                        paint
                );
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                canvas.drawText(labels[(dayOfWeek + 5) % 7], centerX, height - dp(7), paint);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            paint.setTypeface(Typeface.DEFAULT);
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
