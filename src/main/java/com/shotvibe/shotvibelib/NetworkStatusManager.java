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
     *
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
     * @param httpMethod The HTTP method of the call (such as "GET" or "POST")
     *
     * @param url The full URL that was called
     *
     * @param requestTime How long the request took to complete, in milliseconds. Or 0 if the
     *                    request was not timed
     *
     * @param networkError Should be "true" if the request failed as a result of a network error
     *                     (such as no internet connection, or even a server 500 error). In this
     *                     case, the app will display in the UI that there is no internet connection
     *
     * @param platformErrorObject If networkError == true, then a platform-specific object
     *                            containing the details of the error may be passed here. This is
     *                            allowed to be null (even if networkError is true, for example for
     *                            server 500 errors, where no platform error object is available)
     *
     *                            For Android, this should be an "Exception" object.
     *
     *                            For iOS, this should be an "NSError" object.
     *
     */
    public void logNetworkRequest(String httpMethod, String url, long requestTime, boolean networkError, Object platformErrorObject) {
        if (httpMethod == null) {
            throw new IllegalArgumentException("httpMethod cannot be null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (!networkError && platformErrorObject != null) {
            throw new IllegalArgumentException("There cannot be a platformErrorObject when networkError is false");
        }

        synchronized (this) {
            addLogEntry(new LogEntry(httpMethod, url, requestTime, networkError, platformErrorObject));
            if (mNetworkOnline && networkError) {
                // Network status is changing from online to offline
                mNetworkOnline = false;

                notifyListeners();
            } else if (!mNetworkOnline && !networkError) {
                // Network status is changing from offline to online
                mNetworkOnline = true;

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

    public static class LogEntry {
        // TODO Should also contain a DateTime of the time the request was logged

        public LogEntry(String httpMethod, String url, long requestTime, boolean networkError, Object platformErrorObject) {
            mHttpMethod = httpMethod;
            mUrl = url;
            mRequestTime = requestTime;
            mNetworkError = networkError;
            mPlatformErrorObject = platformErrorObject;
        }

        public String getHttpMethod() {
            return mHttpMethod;
        }

        public String getUrl() {
            return mUrl;
        }

        public long getRequestTime() {
            return mRequestTime;
        }

        public boolean getNetworkError() {
            return mNetworkError;
        }

        public Object getPlatformErrorObject() {
            if (!mNetworkError) {
                throw new IllegalArgumentException("Can only be called when getNetworkError() is true");
            }

            return mPlatformErrorObject;
        }

        private String mHttpMethod;
        private String mUrl;
        private long mRequestTime;
        private boolean mNetworkError;
        private Object mPlatformErrorObject;
    }
}
