package com.example.feedapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

public class DebugExposureActivity extends AppCompatActivity {

    private TextView tvLogs;
    private Button btnClear;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_exposure);

        Toolbar toolbar = findViewById(R.id.debugToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 左上角返回箭头
        }

        tvLogs = findViewById(R.id.tvLogs);
        btnClear = findViewById(R.id.btnClear);

        refreshLogs();

        btnClear.setOnClickListener(v -> {
            ExposureLogger.clear();
            refreshLogs();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 点返回箭头关闭当前页
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogs();
    }

    private void refreshLogs() {
        List<String> logs = ExposureLogger.getLogs();
        StringBuilder sb = new StringBuilder();
        for (String line : logs) {
            sb.append(line).append('\n');
        }
        tvLogs.setText(sb.toString());
    }
}
