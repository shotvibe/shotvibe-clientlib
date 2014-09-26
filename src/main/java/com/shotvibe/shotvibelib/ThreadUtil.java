package com.shotvibe.shotvibelib;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public interface Executor {
        void execute(Runnable runnable);
    }

    public static Executor createSingleThreadExecutor() {
        return new Executor() {
            private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
            @Override
            public void execute(final Runnable runnable) {
                mExecutorService.execute(new java.lang.Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                });
            }
        };
    }

    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ThreadUtil() {
        // Not used
    }
}
