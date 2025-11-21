package com.example.feedapp;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExposureLogger {

    private static final String TAG = "Exposure";
    private static final int MAX_LOGS = 200;

    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    public static synchronized void log(String msg) {
        String time = sdf.format(new Date());
        String line = time + "  " + msg;
        logs.add(0, line); // 最新的放前面
        if (logs.size() > MAX_LOGS) {
            logs.remove(MAX_LOGS);
        }
        Log.d(TAG, line);
    }

    public static synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public static synchronized void clear() {
        logs.clear();
    }
}
