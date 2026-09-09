package com.chargingbot.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIF_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);
        Button startBtn = findViewById(R.id.startBtn);
        Button stopBtn = findViewById(R.id.stopBtn);

        statusText.setText("Charging Bot is ready.\nTap Start to activate background monitoring.");

        startBtn.setOnClickListener(v -> {
            requestNotificationPermissionIfNeeded();
            Intent serviceIntent = new Intent(this, ChargingService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, "Charging Bot activated in background", Toast.LENGTH_SHORT).show();
        });

        stopBtn.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, ChargingService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Charging Bot stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERMISSION_CODE);
            }
        }
    }
}
