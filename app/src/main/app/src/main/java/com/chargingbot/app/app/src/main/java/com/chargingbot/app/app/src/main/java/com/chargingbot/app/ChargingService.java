package com.chargingbot.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class ChargingService extends Service {

    private static final String CHANNEL_ID = "charging_bot_channel";
    private static final int NOTIF_ID = 1;

    private TextToSpeech tts;
    private BroadcastReceiver batteryReceiver;
    private int lastAnnouncedMilestone = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int pct = (int) ((level / (float) scale) * 100);

                boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);

                updateNotification(pct, charging);

                if (charging) {
                    announceMilestones(pct);
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification("Charging Bot active", "Monitoring battery..."));

        String event = intent != null ? intent.getStringExtra("event") : null;
        if ("connected".equals(event)) {
            lastAnnouncedMilestone = -1;
            int pct = getCurrentBatteryPercent();
            speak("Boss, your phone is now in charge mode. Full power incoming! Currently at " + pct + " percent.");
        } else if ("disconnected".equals(event)) {
            speak("Boss, charger disconnected. Battery is at " + getCurrentBatteryPercent() + " percent.");
        }

        return START_STICKY;
    }

    private void announceMilestones(int pct) {
        int[] milestones = {25, 50, 75, 100};
        for (int m : milestones) {
            if (pct >= m && lastAnnouncedMilestone < m) {
                lastAnnouncedMilestone = m;
                if (m == 100) {
                    speak("Boss, battery fully charged at 100 percent!");
                } else {
                    speak("Boss, battery reached " + m + " percent.");
                }
            }
        }
    }

    private int getCurrentBatteryPercent() {
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return -1;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) ((level / (float) scale) * 100);
    }

    private void speak(String message) {
        if (tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_ADD, null, "utteranceId_" + System.currentTimeMillis());
        }
    }

    private void updateNotification(int pct, boolean charging) {
        String title = charging ? "⚡ Charging: " + pct + "%" : "Battery: " + pct + "%";
        String text = charging ? "Full power incoming, boss!" : "Not charging";
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIF_ID, buildNotification(title, text));
        }
    }

    private Notification buildNotification(String title, String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Charging Bot", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows charging status and battery updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
