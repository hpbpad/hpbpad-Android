package com.justsystems.hpb.pad.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public final class Debug {
    //ログを出力するか
    private static final boolean DEBUG = false;

    public static void logd(String msg) {
        if (DEBUG) {
            logd("DEBUG", msg);
        }
    }

    public static void logd(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void toast(Context context, String msg) {
        if (DEBUG) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }
}
