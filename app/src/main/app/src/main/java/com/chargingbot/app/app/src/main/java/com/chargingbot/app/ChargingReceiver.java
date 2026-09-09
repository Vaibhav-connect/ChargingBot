package com.chargingbot.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Intent serviceIntent = new Intent(context, ChargingService.class);

        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            serviceIntent.putExtra("event", "connected");
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            serviceIntent.putExtra("event", "disconnected");
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            serviceIntent.putExtra("event", "boot");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
