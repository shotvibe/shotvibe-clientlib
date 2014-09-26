package com.shotvibe.shotvibelib;

import android.os.Handler;
import android.os.Looper;

public final class ThreadUtil {
    interface Runnable {
        void run();
    }

    public static void runInBackgroundThread(Runnable runnable) {
        final Runnable finalRunnable = runnable;
        Thread backgroundThread = new Thread(new java.lang.Runnable() {
            @Override
            public void run() {
                finalRunnable.run();
            }
        });

        backgroundThread.start();
    }

    public static void runInMainThread(Runnable runnable) {
        final Runnable finalRunnable = runnable;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new java.lang.Runnable() {
            @Override
            public void run() {
                finalRunnable.run();
            }
        });
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private ThreadUtil() {
        // Not used
    }
}
