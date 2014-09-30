package com.shotvibe.shotvibelib;

import java.util.List;

public interface BackgroundUploadSession<T> {
    public interface TaskDataFactory<T> {
        String serialize(T taskData);
        T deserialize(String s);
    }

    public interface Factory<T> {
        BackgroundUploadSession<T> startSession(TaskDataFactory<T> taskDataFactory, Listener<T> listener);
    }

    public static final class FinishedTask<T> {
        public FinishedTask(T taskData, int statusCode) {
            mTaskData = taskData;
            mStatusCode = statusCode;
            mError = null;
        }

        public FinishedTask(T taskData, Object error) {
            mTaskData = taskData;
            mStatusCode = -1;
            mError = error;
        }

        public T getTaskData() {
            return mTaskData;
        }

        public boolean completedWithStatusCode() {
            return mError == null;
        }

        public int getStatusCode() {
            return mStatusCode;
        }

        public Object getError() {
            return mError;
        }

        private final T mTaskData;
        private final int mStatusCode;
        private final Object mError;
    }

    public interface Listener<T> {
        void onTaskUploadProgress(T taskData, long bytesSent, long bytesTotal);

        void onTaskUploadFinished(FinishedTask<T> finishedTask);
    }

    public interface TaskProcessor<T> {
        /**
         * Do not save any tasks beyond the scope of the method.
         *
         * Do not modify the tasks list.
         *
         * You may cancel a task with {@link BackgroundUploadSession#cancelTask}. No more listener
         * methods will be called for the task.
         */
        void processTasks(List<Task<T>> currentTasks);
    }

    /**
     * May be called from any thread
     */
    void startUploadTask(T taskData, String url, String uploadFile);

    /**
     * May only be called from within a {@link TaskProcessor#processTasks} method.
     *
     * (Note that in there is the only place where you have access to a Task object).
     */
    void cancelTask(Task<T> task);

    void processCurrentTasks(TaskProcessor<T> taskProcessor);

    public abstract class Task<T> {
        public abstract T getTaskData();

        public abstract boolean isUploadInProgress();
    }
}
