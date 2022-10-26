package com.zebra.jacktriggerservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(Constants.TAG, "BootCompletedBroadcastReceiver::onReceive");
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
        Log.d(Constants.TAG, startService ? "Auto start service" : "Do nothing on boot");
        if(startService)
        {
            ForegroundService.startService(context);
        }
        // Launch PowerEventsWatcherService if running on a build >= Oreo
        boolean watchForPowerEvents = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
        if(watchForPowerEvents)
        {
            PowerEventsWatcherService.startService(context);
            // Let's check if we are already connected on power
            BatteryManager myBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if(myBatteryManager.isCharging() && !ForegroundService.isRunning(context))
                ForegroundService.startService(context);
        }
    }
}
