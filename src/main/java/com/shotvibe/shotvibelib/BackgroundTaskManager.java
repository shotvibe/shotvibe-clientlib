package com.shotvibe.shotvibelib;

public interface BackgroundTaskManager {
    enum NotificationMessage {
        UPLOADS_STILL_SAVING,
        UPLOADS_STILL_PROCESSING
    }

    interface BackgroundTask {
        void reportFinished();
    }

    interface ExpirationHandler {
        void onAppWillTerminate();
    }

    BackgroundTask beginBackgroundTask(ExpirationHandler expirationHandler);

    void showNotificationMessage(NotificationMessage message);
}
