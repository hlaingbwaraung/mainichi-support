package com.example.seniorhelper;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

final class MainichiTimeFormatter {
    private MainichiTimeFormatter() {
    }

    static String formatEventTime(long timeMillis) {
        DateFormat date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.JAPAN);
        DateFormat time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.JAPAN);
        Date eventTime = new Date(timeMillis);
        return date.format(eventTime) + " " + time.format(eventTime);
    }
}
