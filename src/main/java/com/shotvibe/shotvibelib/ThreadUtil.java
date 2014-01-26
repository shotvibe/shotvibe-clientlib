package com.shotvibe.shotvibelib;

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

    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private ThreadUtil() {
        // Not used
    }
}
