package com.shotvibe.shotvibelib;

public class NetworkStatusManager {
    public NetworkStatusManager() {
        // We assume that we are initially online and connected to the internet
        mNetworkOnline = true;

        mLogEntries = new ArrayList<LogEntry>();

        mListeners = new ArrayList<Listener>();
        mListenersLock = new Object();
    }

    public interface Listener {
        /**
         * This will be called from an arbitrary background thread
         *
         * @param networkOnline true if the internet connection is online
         */
        void networkStatusChanged(boolean networkOnline);
    }

    /**
     * Registers a Listener object that will be notified whenever the network status changes
     *
     * @param listener The listener that should be notified about changes
     * @return The current status of the network: true if the internet connection is online
     */
    public boolean registerListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }

        synchronized (mListenersLock) {
            if (mListeners.contains(listener)) {
                throw new IllegalArgumentException("listener is already registered");
            }

            mListeners.add(listener);

            return mNetworkOnline;
        }
    }

    public void unregisterListener(Listener listener) {
        synchronized (mListenersLock) {
            if (!mListeners.remove(listener)) {
                throw new IllegalArgumentException("listener is not registered");
            }
        }
    }

    private ArrayList<Listener> mListeners;
    private final Object mListenersLock;

    /**
     * This method is thread safe and may be called from any thread
     *
     * @param response The HTTPResponse from the request
     */
    public void logNetworkRequest(HTTPResponse response) {
        synchronized (this) {
            addLogEntry(LogEntry.SuccessfulRequest(
                    response.getMethod(),
                    response.getUrl(),
                    response.getStatusCode(),
                    response.getRequestTime()));
            if (!mNetworkOnline) {
                // Network status is changing from offline to online
                mNetworkOnline = true;

                notifyListeners();
            }
        }
    }

    /**
     * This method is thread safe and may be called from any thread
     *
     * @param apiException The APIException object that resulted from the failed network request
     */
    public void logNetworkRequestFailure(APIException apiException) {
        synchronized (this) {
            addLogEntry(LogEntry.FailedRequest(apiException));
            if (mNetworkOnline) {
                // Network status is changing from online to offline
                mNetworkOnline = false;

                notifyListeners();
            }
        }
    }

    private void notifyListeners() {
        // TODO There is a race condition bug here where it is possible for a later notification to
        // actually fire before an earlier notification

        ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                synchronized (mListenersLock) {
                    for (Listener listener : mListeners) {
                        listener.networkStatusChanged(mNetworkOnline);
                    }
                }
            }
        });
    }

    /**
     * This method is thread safe and may be called from any thread
     *
     * @return A list of "LogEntry" objects, oldest first
     */
    public ArrayList<LogEntry> getNetworkRequestsLog() {
        synchronized (this) {
            // Return a copy to ensure thread safety
            return new ArrayList<LogEntry>(mLogEntries);
        }
    }

    private boolean mNetworkOnline;
    private ArrayList<LogEntry> mLogEntries;

    private static final int MAX_LOG_ENTRIES_SAVED = 100;

    private void addLogEntry(LogEntry logEntry) {
        mLogEntries.add(logEntry);

        if (mLogEntries.size() > MAX_LOG_ENTRIES_SAVED) {
            mLogEntries.remove(0);
        }
    }

    public static final class LogEntry {
        // TODO Should also contain a DateTime of the time the request was logged

        public static LogEntry SuccessfulRequest(String httpMethod, String url, int httpStatusCode, long requestTime) {
            if (httpMethod == null) {
                throw new IllegalArgumentException("httpMethod cannot be null");
            }
            if (url == null) {
                throw new IllegalArgumentException("url cannot be null");
            }

            return new LogEntry(httpMethod, url, httpStatusCode, requestTime, null);
        }

        public static LogEntry FailedRequest(APIException error) {
            if (error == null) {
                throw new IllegalArgumentException("error cannot be null");
            }

            return new LogEntry(null, null, null, 0, error);
        }

        private LogEntry(String httpMethod,
                         String url,
                         Integer httpStatusCode,
                         long requestTime,
                         APIException error) {
            mHttpMethod = httpMethod;
            mUrl = url;
            mHttpStatusCode = httpStatusCode;
            mRequestTime = requestTime;
            mError = error;
        }

        public boolean isSuccessfulRequest() {
            return mError == null;
        }

        public String getHttpMethod() {
            if (!isSuccessfulRequest()) {
                throw new IllegalArgumentException("cannot call getHttpMethod when !isSuccessfulRequest()");
            }

            return mHttpMethod;
        }

        public String getUrl() {
            if (!isSuccessfulRequest()) {
                throw new IllegalArgumentException("cannot call getUrl when !isSuccessfulRequest()");
            }

            return mUrl;
        }

        public int getStatusCode() {
            if (!isSuccessfulRequest()) {
                throw new IllegalArgumentException("cannot call getStatusCode when !isSuccessfulRequest()");
            }

            return mHttpStatusCode;
        }

        public long getRequestTime() {
            if (!isSuccessfulRequest()) {
                throw new IllegalArgumentException("cannot call getRequestTime when !isSuccessfulRequest()");
            }
            return mRequestTime;
        }

        public APIException getError() {
            if (isSuccessfulRequest()) {
                throw new IllegalArgumentException("cannot call getError when isSuccessfulRequest()");
            }

            return mError;
        }

        private final String mHttpMethod;
        private final String mUrl;
        private final Integer mHttpStatusCode;
        private final long mRequestTime;
        private final APIException mError;
    }
}
