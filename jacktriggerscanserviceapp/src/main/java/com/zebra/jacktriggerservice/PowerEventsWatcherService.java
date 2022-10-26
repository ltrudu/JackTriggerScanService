package com.zebra.jacktriggerservice;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class PowerEventsWatcherService extends Service {
    private static final int SERVICE_ID = 2;

    private PowerConnectionReceiver mPowerConnectionReceiver = null;

    private Notification mNotification;

    public PowerEventsWatcherService()
    {
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate()
    {
        logD("PowerEventsWatcherService::onCreate");
        mPowerConnectionReceiver = new PowerConnectionReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("PowerEventsWatcherService::onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("PowerEventsWatcherService::onDestroy");
        stopService();
    }

    private void logD(String message)
    {
        Log.d(Constants.TAG, message);
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        logD("PowerEventsWatcherService::startService");
        try
        {
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.powereventswatcherservice_notification_title))
                    .setContentText(getString(R.string.powereventswatcherservice_notification_text))
                    .setTicker(getString(R.string.powereventswatcherservice_notification_tickle))
                    .setPriority(PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
            localTaskStackBuilder.addParentStack(MainActivity.class);
            localTaskStackBuilder.addNextIntent(mainActivityIntent);
            notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

            // Start foreground service
            startForeground(SERVICE_ID, mNotification);

            // Register Power Actions Receiver
            // Register power connected and disconnected broadcast receiver
            IntentFilter myIntentFilter = new IntentFilter();
            myIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            myIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(mPowerConnectionReceiver, myIntentFilter);

            logD("PowerEventsWatcherService::startService:Service started without error.");
        }
        catch(Exception e)
        {
            logD("PowerEventsWatcherService::startService:Error while starting service.");
            e.printStackTrace();
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.powereventswatcherservice_channel_id), getString(R.string.powereventswatcherservice_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.powereventswatcherservice_channel_id);
    }

    private void stopService()
    {
        try
        {
            logD("PowerEventsWatcherService::stopService.");

            // Stop service
            stopForeground(true);

            // Unregister power actions
            unregisterReceiver(mPowerConnectionReceiver);
            logD("PowerEventsWatcherService::stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            logD("PowerEventsWatcherService::stopService:Error while stopping service.");
            e.printStackTrace();

        }

    }

    public static void startService(Context context)
    {
        Intent myIntent = new Intent(context, PowerEventsWatcherService.class);
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
    }

    public static void stopService(Context context)
    {
        Intent myIntent = new Intent(context, PowerEventsWatcherService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PowerEventsWatcherService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
