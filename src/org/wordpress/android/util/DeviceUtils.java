package org.wordpress.android.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.DisplayMetrics;

import org.wordpress.android.WordPress;

/**
 * As of January 20 2012: The BlackBerry Runtime for Android Apps supports
 * Android 2.3.3 applications.
 * 
 * Unsupported App Types: - Widget apps : Apps that are only meant to be widgets
 * are not supported - Apps that include no launchable Activity - Apps that
 * include more than one launchable Activity - Apps whose minimum required
 * Android API level is more than 10, and whose maximum supported level is less
 * than 10
 * 
 * Unsupported Hardware Features: - Telephony (including SMS and MMS) -
 * Bluetooth - Camera: The intent to launch the camera is supported. However,
 * currently the Camera class in the Android SDK is not supported. As a result,
 * although you can launch the camera application, you cannot access the Camera
 * hardware. - NFC - Barometers - Ambient light sensor - Proximity sensor - VoIP
 * 
 * Unsupported Software Features: - Vending (In App Payments):
 * com.android.vending - Cloud To Device Messaging (Push):
 * com.google.android.c2dm - Google Maps: com.google.android.maps - Text to
 * Speech: com.google.tts
 * 
 * 
 * Major Details here: https://bdsc.webapps.blackberry.com/android/apisupport
 * 
 * 
 * @author daniloercoli
 * 
 */

public class DeviceUtils {

    private DeviceUtils() {
        /*
         * isPlayBook = android.os.Build.MANUFACTURER.equalsIgnoreCase(
         * "Research in Motion" ) && android.os.Build.MODEL.startsWith(
         * "BlackBerry Runtime for Android" );
         */
    };

    public static boolean isBlackBerry() {
        return System.getProperty("os.name").equalsIgnoreCase("qnx");
    }

    public boolean isKindleFire() {
        return android.os.Build.MODEL.equalsIgnoreCase("kindle fire");
    }

    public static String getBlackBerryUserAgent() {
        return "wp-blackberry/" + WordPress.versionName;
    }

    /**
     * Checks camera availability recursively based on API level.
     * 
     * TODO: change "android.hardware.camera.front" and
     * "android.hardware.camera.any" to
     * {@link PackageManager#FEATURE_CAMERA_FRONT} and
     * {@link PackageManager#FEATURE_CAMERA_ANY}, respectively, once they become
     * accessible or minSdk version is incremented.
     * 
     * @param context
     *            The context.
     * @return Whether camera is available.
     */
    public static boolean hasCamera(Context context) {
        final PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                    || pm.hasSystemFeature("android.hardware.camera.front");
        }

        return pm.hasSystemFeature("android.hardware.camera.any");
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    }

    public static int getSmallestWidthPixcel(Resources r) {
        DisplayMetrics metrics = r.getDisplayMetrics();
        if (r.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return metrics.heightPixels;
        } else {
            return metrics.widthPixels;
        }

    }

    public static boolean isOverEqualThanHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isLessThanJB() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;
    }

}
