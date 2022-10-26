package com.zebra.jacktriggerservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

public class SetupServiceBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::onReceive");
        String sStartOnBoot = intent.getExtras().getString(Constants.EXTRA_CONFIGURATION_START_ON_BOOT, null);
        if(sStartOnBoot != null)
        {
            Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::onReceive:Start on boot extra found with value:" + sStartOnBoot);
            boolean bStartOnBoot = sStartOnBoot.equalsIgnoreCase("true") || sStartOnBoot.equalsIgnoreCase("1");
            setSharedPreference(context, Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, bStartOnBoot);
            // Update GUI if necessary
            MainActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::onReceive:No start on boot extra found.");
        }

        String sStartOnCharging = intent.getExtras().getString(Constants.EXTRA_CONFIGURATION_START_ON_CHARGING, null);
        if(sStartOnCharging != null)
        {
            Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::onReceive:Start on charging extra found with value:" + sStartOnCharging);
            boolean bStartOnCharging = sStartOnCharging.equalsIgnoreCase("true") || sStartOnBoot.equalsIgnoreCase("1");
            setSharedPreference(context, Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, bStartOnCharging);
            // Launch service if necessary
            if(bStartOnCharging)
            {
                if(!PowerEventsWatcherService.isRunning(context))
                    PowerEventsWatcherService.startService(context);

                // Let's check if we are already connected on power to launch ForegroundService if necessary
                BatteryManager myBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if(myBatteryManager.isCharging() && !ForegroundService.isRunning(context))
                    ForegroundService.startService(context);
            }
            // Update GUI if necessary
            MainActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::onReceive:No start on charging extra found.");
        }
    }

    private void setSharedPreference(Context context, String key, boolean value)
    {
        Log.d(Constants.TAG, "SetupServiceBroadcastReceiver::setSharedPreference: Key=" + key + " | Value=" + value);
        // Setup shared preferences for next reboot
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
