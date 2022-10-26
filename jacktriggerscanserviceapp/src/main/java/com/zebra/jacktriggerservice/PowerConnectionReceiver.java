package com.zebra.jacktriggerservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "PowerConnectionReceiver::onReceive");
        String action = intent.getAction();
        if(action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED)
        {
            //Handle power connected
            SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean startService = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
            if(startService)
            {
                if(action == Intent.ACTION_POWER_CONNECTED)
                {
                    // We are connected, start service
                    Log.d(Constants.TAG, "Start service on charging.");
                    // We are plugged in and charging, let's start the service
                    Intent myIntent = new Intent(context, ForegroundService.class);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        // Use start foreground service to prevent the runtime error:
                        // "not allowed to start service intent app is in background"
                        // to happen when running on OS >= Oreo
                        context.startForegroundService(myIntent);
                    }
                    else
                    {
                        context.startService(myIntent);
                    }
                    // Update GUI if necessary
                    if(MainActivity.mMainActivity != null) // The application default activity has been opened
                    {
                        MainActivity.mMainActivity.updateSwitches();
                    }
                }
                else
                {
                    Log.d(Constants.TAG, "Stop service when disconnected.");
                    // We are disconnected, let's stop the service
                    Intent myIntent = new Intent(context, ForegroundService.class);
                    context.stopService(myIntent);

                    // Update GUI if necessary
                    if(MainActivity.mMainActivity != null) // The application default activity has been opened
                    {
                        MainActivity.mMainActivity.updateSwitches();
                    }
                }

            }
            else
            {
                Log.d(Constants.TAG, "Start service on Power event disabled.");
            }
        }

        /*
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
        if(startService)
        {
            Log.d(Constants.TAG, "Start service on charging.");
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean isPlugged = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) ||
                    (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) ||
                    (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);

            if(isPlugged && isCharging)
            {
                // We are plugged in and charging, let's start the service
                Intent myIntent = new Intent(context, ForegroundService.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    // Use start foreground service to prevent the runtime error:
                    // "not allowed to start service intent app is in background"
                    // to happen when running on OS >= Oreo
                    context.startForegroundService(myIntent);
                }
                else
                {
                    context.startService(myIntent);
                }
                // Update GUI if necessary
                if(MainActivity.mMainActivity != null) // The application default activity has been opened
                {
                    MainActivity.mMainActivity.setServiceStartedSwitchValues(true, MainActivity.mMainActivity.getString(R.string.serviceStarted));
                }
            }
            else
            {
                // We are unplugged or not charging
                // Let's stop the service
                Intent myIntent = new Intent(context, ForegroundService.class);
                context.stopService(myIntent);

                // Update GUI if necessary
                if(MainActivity.mMainActivity != null) // The application default activity has been opened
                {
                    MainActivity.mMainActivity.setServiceStartedSwitchValues(false, MainActivity.mMainActivity.getString(R.string.serviceStopped));
                }
            }
        }
        else
        {
            Log.d(Constants.TAG, "Do nothing on power event.");

        }
        */
    }
}
