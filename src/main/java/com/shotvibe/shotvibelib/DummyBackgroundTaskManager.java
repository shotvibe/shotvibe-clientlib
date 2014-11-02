package com.shotvibe.shotvibelib;

public class DummyBackgroundTaskManager implements BackgroundTaskManager {
    @Override
    public BackgroundTask beginBackgroundTask(ExpirationHandler expirationHandler) {
        return new DummyBackgroundTask();
    }

    private class DummyBackgroundTask implements BackgroundTask {

        @Override
        public void reportFinished() {
            // Dummy implementation does nothing
        }
    }

    @Override
    public void showNotificationMessage(NotificationMessage message) {
        // Dummy implementation does nothing
    }
}
