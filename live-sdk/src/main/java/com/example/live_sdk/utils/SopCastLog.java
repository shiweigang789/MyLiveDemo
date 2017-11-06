package com.example.live_sdk.utils;

import android.util.Log;

/**
 * Created by swg on 2017/11/6.
 */

public class SopCastLog {

    private static boolean open = false;

    public static void isOpen(boolean isOpen) {
        open = isOpen;
    }

    public static void d(String tag, String msg) {
        if(open) {
            Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if(open) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if(open) {
            Log.e(tag, msg);
        }
    }

}
