# まいにちサポート Project Summary

Android app for older users with large buttons, simple Japanese UI, and daily support features.

## Main Features

- Home screen with compact date, weather, and today summary.
- Weather uses a recent phone location from GPS/network and shows only temperature plus current conditions.
- Weather never silently falls back to a fixed city when the current location is unavailable.
- Step counter with a tappable 7-day daily graph showing the selected date and step count.
- Notes with large text input.
- Schedule alarms with date/time controls and notification sound.
- Medicine alarms with daily confirmation.
- Emergency contact with name and phone number.
- Today todo list:
  - Completed items get a strikethrough.
  - Completed items are removed automatically the next day.
  - If no remaining item, home shows `今日やること: ありません`.
  - Home summary, speech, and family share show the actual unfinished items.
  - Complete/restore is large and delete is compact in the same row.
- Shopping list:
  - Stores only item name and amount.
  - Bought items get a strikethrough instead of being deleted.
  - `全て削除` button is placed at top right.
  - Bought/restore and compact delete actions are shown side by side.
- List actions:
  - Notes, medicine, family, todos, shopping, and schedules use compact delete actions.
  - Destructive actions show the same large Japanese-style confirmation panel.
- Premium screen:
  - Google Play monthly subscription at 500 yen.
  - Subscription product ID: `premium_monthly`.
  - Subscription base plan ID: `monthly`.
  - One-time lifetime purchase at 3,000 yen.
  - One-time product ID: `premium_lifetime`.
  - Premium hides ad placeholders.
  - Purchase restore and Google Play subscription management.
  - Non-premium shows a subscribe recommendation after 5 seconds.
- First-time setup:
  - Text size selection.
  - Emergency contact registration.
  - Medicine time registration.
  - Medicine names use one-line input; keyboard Next closes the keyboard and moves to the time controls.
- Text-to-speech for today's summary.
- Family share button for today's summary.
- Backup share/copy from settings.
- App icon uses selected logo pattern A.

## Build

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Current served APK name:

```text
mainichi-support-v2.apk
```

## Notes

- Package: `com.example.seniorhelper`
- App label: `まいにちサポート`
- Main source: `app/src/main/java/com/example/seniorhelper/MainActivity.java`
- Billing source: `app/src/main/java/com/example/seniorhelper/PlayBillingManager.java`
- Alarm receiver: `app/src/main/java/com/example/seniorhelper/EventAlarmReceiver.java`
