package com.zebra.jacktriggerservice;

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ZebraDeviceHelper {
    public static String TAG = "ZDeviceHelper";


    public static boolean isZebraDevice(Context context)
    {
        if(Build.MANUFACTURER.contains("Zebra"))
            return true;
        /*
        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Log.d(TAG, "device name:" + android.os.Build.MODEL);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equalsIgnoreCase("com.symbol.datawedge") &&
            packageInfo.sourceDir.equalsIgnoreCase("/system/priv-app/com.symbol.datawedge/com.symbol.datawedge.apk") &&
            Build.MANUFACTURER.contains("Zebra"))
                return true;
        }
         */
        return false;
    }

    public static boolean PING(String ipAddress)
    {
        boolean reachable = false;
        InetAddress inet = null;
        try {
            inet = InetAddress.getByName(ipAddress);
            reachable = inet.isReachable(5000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reachable ;
    }

}
