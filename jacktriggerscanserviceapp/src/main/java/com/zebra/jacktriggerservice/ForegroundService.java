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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;
import android.view.KeyEvent;

import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.media.AudioTrack.PLAYSTATE_PLAYING;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class ForegroundService extends Service {
    private static final int SERVICE_ID = 1;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private AudioTrack mAudioTrack;
    private MediaSession mMediaSession;

    public ForegroundService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        logD("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("onDestroy");
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        logD("startService");
        try
        {
            if(mNotificationManager == null)
                mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));

            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service Notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, createNotificationChannel(mNotificationManager));
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_jacktrigger)
                    .setContentTitle(getString(R.string.no_sleep_service_notification_title))
                    .setContentText(getString(R.string.no_sleep_service_notification_text))
                    .setTicker(getString(R.string.no_sleep_service_notification_tickle))
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

            if(mMediaSession == null)
                mMediaSession = new MediaSession(getApplicationContext(), getPackageName());
            mMediaSession.setActive(true);

            mMediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    // Stop mediaSession
                    mMediaSession.setActive(false);
                    boolean returnValue = false;
                    if(mediaButtonIntent.getAction().equalsIgnoreCase("android.intent.action.MEDIA_BUTTON") == false)
                    {
                        returnValue = super.onMediaButtonEvent(mediaButtonIntent);
                    }
                    else
                    {
                        KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event == null) {
                            returnValue = super.onMediaButtonEvent(mediaButtonIntent);
                        }
                        else
                        {
                            int action = event.getAction();
                            if (action == KeyEvent.ACTION_DOWN) {
                                DWScannerStartScan startScan = new DWScannerStartScan(ForegroundService.this);
                                DWProfileBaseSettings baseSettings = new DWProfileBaseSettings();
                                startScan.execute(baseSettings, null);
                                returnValue = true;
                            }
                            else
                            {
                                Log.e("hmhm", "hmhm media button");
                                returnValue = super.onMediaButtonEvent(mediaButtonIntent);
                            }
                        }
                    }
                    // Restart audio file and mediasession
                    mMediaSession.setActive(true);
                    playDummyAudioFile();
                    return returnValue;
                }
            });

            playDummyAudioFile();

            logD("startService:Service started without error.");
        }
        catch(Exception e)
        {
            logD("startService:Error while starting service.");
            e.printStackTrace();
        }


    }

    private void stopService()
    {
        try
        {
            logD("stopService.");

            if(mMediaSession != null)
            {
                if(mMediaSession.isActive())
                    mMediaSession.setActive(false);
                mMediaSession.release();
                mMediaSession = null;
            }

            // TODO: Release your stuffs here
            if(mNotificationManager != null)
            {
                mNotificationManager.cancelAll();
                mNotificationManager = null;
            }


            stopForeground(true);
            logD("stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            logD("Error while stopping service.");
            e.printStackTrace();

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.nosleepservice_channel_id), getString(R.string.nosleepservice_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.nosleepservice_channel_id);
    }

    private void playDummyAudioFile()
    {
        // Use an audio dummy track (Oreo Fix) to detect media event
        if(mAudioTrack != null && mAudioTrack.getPlayState() == PLAYSTATE_PLAYING)
        {
            mAudioTrack.stop();
        }

        if(mAudioTrack == null)
        {
            // Create dummy audio file
            mAudioTrack = new AudioTrack(AudioManager.STREAM_DTMF, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        }

        // Play then stop audio track
        mAudioTrack.play();
        // a little sleep
        /*
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */
        mAudioTrack.stop();
    }

    private void releaseDummyAudioFile()
    {
        if(mAudioTrack != null)
        {
            if(mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
            {
                mAudioTrack.stop();
            }
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    private void logD(String message)
    {
        Log.d(Constants.TAG, message);
    }

    public static void startService(Context context)
    {
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
    }

    public static void stopService(Context context)
    {
        Intent myIntent = new Intent(context, ForegroundService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
