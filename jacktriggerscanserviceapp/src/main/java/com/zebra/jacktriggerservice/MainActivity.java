package com.zebra.jacktriggerservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

// Copyright Zebra Technologies 2019.
// See included licence for details
//
// Original code: Pietro Maggi
// Rewrite + Fixes for Oreo: Laurent Trudu
//
//
// The service can be launched using the graphical user interface, intent actions or adb.
//
// If the option "Start on boot" is enabled, the service will be automatically launched when the boot is complete.
//
// Power events occur when the device is connected to a power source (AC/USB/Wireless).
// If the option "Start when charging / Stop when charging" is enabled, the power events will be monitored.
// The ForegroundService will be launched when the device is connected to a power source
//
//
// The service respond to two intent actions (both uses the category: android.intent.category.DEFAULT)
// - "com.zebra.jacktriggerservice.startservice" sent on the component "com.zebra.jacktriggerservice/com.zebra.jacktriggerservice.StartServiceBroadcastReceiver":
//   Start the service.
//   If the device get rebooted the service will start automatically once the reboot is completed.
// - "com.zebra.jacktriggerservice.stopservice" sent on the component "com.zebra.jacktriggerservice/com.zebra.jacktriggerservice.StopServiceBroadcastReceiver":
//   Stop the service.
//   If the device is rebooted, the service will not be started.
//
// The service can be started and stopped manually using the following adb commands:
//  - Start service:
//      adb shell am broadcast -a com.zebra.jacktriggerservice.startservice -n com.zebra.jacktriggerservice/com.zebra.jacktriggerservice.StartServiceBroadcastReceiver
//  - Stop service:
//      adb shell am broadcast -a com.zebra.jacktriggerservice.stopservice -n com.zebra.jacktriggerservice/com.zebra.jacktriggerservice.StopServiceBroadcastReceiver
//  - Setup service
//          The service can be configured using the following intent:
//          adb shell am broadcast -a com.zebra.jacktriggerservice.setupservice -n com.zebra.jacktriggerservice/com.zebra.jacktriggerservice.SetupServiceBroadcastReceiver --es startonboot "true"
//          The command must contain at least one of the extras:
//          - Configure autostart on boot:
//          --es startonboot "true"
//          The extras value can be set to "true" or "1" to enable the option and "false" or "0" to disable the option.
public class MainActivity extends AppCompatActivity {

    private Switch mStartStopServiceSwitch = null;
    private Switch mAutoStartServiceOnBootSwitch = null;
    private Switch mAutoStartServiceOnCraddleSwitch = null;
    public static MainActivity mMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ZebraDeviceHelper.isZebraDevice(this) == false)
        {
            Toast.makeText(this, "Jacktrigger only runs on Zebra devices.", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finishAffinity();
            finish();
            System.exit(0);
        }

        setContentView(R.layout.activity_main);

        ((Button)findViewById(R.id.btLicense)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ltrudu/NoSleepService/blob/master/README.md"));
                Intent myIntent = new Intent(MainActivity.this, LicenceActivity.class);
                startActivity(myIntent);
            }
        });

        mStartStopServiceSwitch = (Switch)findViewById(R.id.startStopServiceSwitch);
        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStarted));
                    if(!ForegroundService.isRunning(MainActivity.this))
                        ForegroundService.startService(MainActivity.this);
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(ForegroundService.isRunning(MainActivity.this))
                        ForegroundService.stopService(MainActivity.this);
                }
            }
        });

        mAutoStartServiceOnBootSwitch = (Switch)findViewById(R.id.startOnBootSwitch);
        mAutoStartServiceOnBootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.startOnBoot));
                }
                else
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.doNothingOnBoot));
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, isChecked);
                editor.commit();
            }
        });

        mAutoStartServiceOnCraddleSwitch = (Switch)findViewById(R.id.startOnCraddle);
        mAutoStartServiceOnCraddleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAutoStartServiceOnCraddleSwitch.setText(getString(R.string.startOnCharging));
                    // Launch the watcher service
                    if(!PowerEventsWatcherService.isRunning(MainActivity.this))
                        PowerEventsWatcherService.startService(MainActivity.this);
                    // Let's check if we are already connected on power to launch ForegroundService if necessary
                    BatteryManager myBatteryManager = (BatteryManager) MainActivity.this.getSystemService(Context.BATTERY_SERVICE);
                    if(myBatteryManager.isCharging() && !ForegroundService.isRunning(MainActivity.this))
                        ForegroundService.startService(MainActivity.this);
                }
                else
                {
                    mAutoStartServiceOnCraddleSwitch.setText(getString(R.string.doNothingOnCharging));
                    // Stop the watcher service
                    if(PowerEventsWatcherService.isRunning(MainActivity.this))
                        PowerEventsWatcherService.stopService(MainActivity.this);
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, isChecked);
                editor.commit();
            }
        });

        updateSwitches();
        launchPowerEventsWatcherServiceIfNecessary();
    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        super.onResume();
        updateSwitches();
        launchPowerEventsWatcherServiceIfNecessary();
    }

    public void updateSwitches()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(ForegroundService.isRunning(MainActivity.this))
                {
                    setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
                }
                else
                {
                    setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
                }

                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean startServiceOnBoot = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
                setAutoStartServiceOnBootSwitch(startServiceOnBoot, startServiceOnBoot ? getString(R.string.startOnBoot) : getString(R.string.doNothingOnBoot));

                boolean startServiceOnCharging = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
                setAutoStartServiceOnChargingSwitch(startServiceOnCharging, startServiceOnCharging ? getString(R.string.startOnCharging) : getString(R.string.doNothingOnCharging));
            }
        });

    }

    private void launchPowerEventsWatcherServiceIfNecessary()
    {
        // We need to launch the PowerEventsWatcher Service if necessary
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startServiceOnCharging = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
        if(startServiceOnCharging)
        {
            // Launch the service if it was not running
            if(!PowerEventsWatcherService.isRunning(this))
                PowerEventsWatcherService.startService(this);

            // Let's check if we are already connected on power to launch ForegroundService if necessary
            BatteryManager myBatteryManager = (BatteryManager) MainActivity.this.getSystemService(Context.BATTERY_SERVICE);
            if(myBatteryManager.isCharging() && !ForegroundService.isRunning(MainActivity.this))
                ForegroundService.startService(MainActivity.this);
        }
    }

    @Override
    protected void onPause() {
        mMainActivity = null;
        super.onPause();
    }

    private void setServiceStartedSwitchValues(final boolean checked, final String text)
    {
        mStartStopServiceSwitch.setChecked(checked);
        mStartStopServiceSwitch.setText(text);
    }

    private void setAutoStartServiceOnBootSwitch(final boolean checked, final String text)
    {
        mAutoStartServiceOnBootSwitch.setChecked(checked);
        mAutoStartServiceOnBootSwitch.setText(text);
    }

    private void setAutoStartServiceOnChargingSwitch(final boolean checked, final String text)
    {
        mAutoStartServiceOnCraddleSwitch.setChecked(checked);
        mAutoStartServiceOnCraddleSwitch.setText(text);
    }


    public static void updateGUISwitchesIfNecessary()
    {
        // Update GUI if necessary
        if(MainActivity.mMainActivity != null) // The application default activity has been opened
        {
            MainActivity.mMainActivity.updateSwitches();
        }
    }
}
