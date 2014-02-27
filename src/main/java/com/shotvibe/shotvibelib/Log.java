package com.shotvibe.shotvibelib;

public final class Log {

    private Log() {
        //not called
    }

    public static void d(String tag, String message) {
        android.util.Log.d(tag, message);
    }

}
