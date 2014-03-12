package com.shotvibe.shotvibelib;

public class PhoneContactsManager implements DevicePhoneContactsLib.DeviceAddressBookListener {
    public interface Listener {
        void phoneContactsUpdated(ArrayList<PhoneContactDisplayData> phoneContacts);
    }

    public PhoneContactsManager(DevicePhoneContactsLib devicePhoneContactsLib, ShotVibeAPI shotVibeAPI, ShotVibeDB shotVibeDB) {
        if (devicePhoneContactsLib == null) {
            throw new IllegalArgumentException("devicePhoneContactsLib cannot be null");
        }
        if (shotVibeAPI == null) {
            throw new IllegalArgumentException("shotVibeAPI cannot be null");
        }
        if (shotVibeDB == null) {
            throw new IllegalArgumentException("shotVibeDB cannot be null");
        }

        mDevicePhoneContactsLib = devicePhoneContactsLib;
        mShotVibeAPI = shotVibeAPI;
        mShotVibeDB = shotVibeDB;

        mListener = null;

        mCurrentAddressBook = null;

        try {
            mCachedContacts = shotVibeDB.getAllCachedPhoneContacts();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final DevicePhoneContactsLib mDevicePhoneContactsLib;
    private final ShotVibeAPI mShotVibeAPI;
    private final ShotVibeDB mShotVibeDB;

    /**
     * Must be called from the main thread
     * @param listener
     */
    public void setListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (mListener != null) {
            throw new IllegalArgumentException("A Listener is already registered. Existing: " + listener + " New: " + listener);
        }
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from main thread");
        }

        synchronized (this) {
            mListener = listener;
        }

        mDevicePhoneContactsLib.registerDeviceAddressBookListener(this);

        mRefreshDeviceContactsTriggerableAction.trigger();
    }

    // Must be called from inside a synchronized (mCachedContacts) block
    private PhoneContactServerResult findCachedServerResult(PhoneContact contact) {
        return mCachedContacts.get(contact);
    }

    private final HashMap<PhoneContact, PhoneContactServerResult> mCachedContacts;

    /**
     * Must be called from the main thread
     */
    public void unsetListener() {
        if (mListener == null) {
            throw new IllegalArgumentException("No Listener is set");
        }
        if (!ThreadUtil.isMainThread()) {
            throw new IllegalStateException("Must be called from main thread");
        }

        mDevicePhoneContactsLib.unregisterDeviceAddressBookListener(this);

        synchronized (this) {
            mListener = null;
        }
    }

    @Override
    public void deviceAddressBookChanged() {
        mRefreshDeviceContactsTriggerableAction.trigger();
    }

    private final RefreshDeviceContactsTriggerableAction mRefreshDeviceContactsTriggerableAction = new RefreshDeviceContactsTriggerableAction();
    private final QueryServerContactsTriggerableAction mQueryServerContactsTriggerableAction = new QueryServerContactsTriggerableAction();

    private ArrayList<PhoneContact> mCurrentAddressBook;
    private final Object mAddressBookLock = new Object();

    private Listener mListener;

    private class RefreshDeviceContactsTriggerableAction extends TriggerableAction {

        @Override
        public void runAction() {
            ArrayList<PhoneContact> deviceContacts = mDevicePhoneContactsLib.getDevicePhoneContacts();
            synchronized (mAddressBookLock) {
                mCurrentAddressBook = deviceContacts;
            }
        }

        @Override
        public void actionComplete() {
            notifyListener();
        }
    }

    // How long to wait before refreshing a contact since it was last queried (in microseconds)
    private static final long CONTACT_QUERY_STALE_TIME = 5L * 60L * 1000L * 1000L;

    private void notifyListener() {
        final ArrayList<PhoneContactDisplayData> displayContacts = new ArrayList<PhoneContactDisplayData>();

        ArrayList<PhoneContact> deviceContacts;
        synchronized (mAddressBookLock) {
            deviceContacts = mCurrentAddressBook;
        }

        boolean shouldQuery = false;

        DateTime now = DateTime.NowUTC();

        synchronized (mCachedContacts) {
            for (PhoneContact deviceContact : deviceContacts) {
                PhoneContactServerResult cachedServerResult = findCachedServerResult(deviceContact);
                if (cachedServerResult == null) {
                    PhoneContactDisplayData entry = PhoneContactDisplayData.createLoading(deviceContact);
                    displayContacts.add(entry);
                    shouldQuery = true;
                } else if (cachedServerResult.getPhoneType() == PhoneContactServerResult.PhoneType.MOBILE) {
                    PhoneContactDisplayData entry = PhoneContactDisplayData.createLoaded(deviceContact, cachedServerResult.getUserId(), cachedServerResult.getAvatarUrl());
                    displayContacts.add(entry);

                    // Check if the time when the cached result is old and we need it to be refreshed
                    if (now.getTimeStamp() - cachedServerResult.getQueryTime().getTimeStamp() > CONTACT_QUERY_STALE_TIME) {
                        shouldQuery = true;
                    }
                }
            }
        }

        if (shouldQuery) {
            mQueryServerContactsTriggerableAction.trigger();
        }

        ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
            @Override
            public void run() {
                Listener listener;
                synchronized (PhoneContactsManager.this) {
                    listener = mListener;
                }

                if (listener != null) {
                    listener.phoneContactsUpdated(displayContacts);
                }
            }
        });
    }

    private class QueryServerContactsTriggerableAction extends TriggerableAction {

        @Override
        public void runAction() {
            ArrayList<PhoneContact> deviceContacts;
            synchronized (mAddressBookLock) {
                deviceContacts = mCurrentAddressBook;
            }

            ArrayList<PhoneContact> toQuery = new ArrayList<PhoneContact>();

            DateTime now = DateTime.NowUTC();

            for (PhoneContact deviceContact : deviceContacts) {
                PhoneContactServerResult cachedServerResult = findCachedServerResult(deviceContact);
                if (cachedServerResult == null) {
                    toQuery.add(deviceContact);
                } else if (now.getTimeStamp() - cachedServerResult.getQueryTime().getTimeStamp() > CONTACT_QUERY_STALE_TIME) {
                    // The contact was queried long enough ago that we need to refresh it from the server
                    toQuery.add(deviceContact);
                }
            }

            ArrayList<PhoneContactServerResult> results;
            try {
                results = mShotVibeAPI.queryPhoneNumbers(toQuery, mShotVibeAPI.getAuthData().getDefaultCountryCode());
            } catch (APIException e) {
                // TODO queue for retry
                return;
            }

            synchronized (mCachedContacts) {
                for (PhoneContactServerResult result : results) {
                    updatePhoneContactServerResult(result);
                }
            }

            notifyListener();

            try {
                mShotVibeDB.addQueriedPhoneContacts(results);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Must be called within a synchronized (mCachedContacts) block
    private void updatePhoneContactServerResult(PhoneContactServerResult result) {
        mCachedContacts.put(result.getPhoneContact(), result);
    }

    private abstract static class TriggerableAction {
        public TriggerableAction() {
            mRunState = RunState.NOT_RUNNING;
        }

        public void trigger() {
            boolean shouldStart = false;
            synchronized (this) {
                switch (mRunState) {
                    case NOT_RUNNING:
                        shouldStart = true;
                        mRunState = RunState.RUNNING;
                        break;
                    case RUNNING:
                        mRunState = RunState.REFRESH_TRIGGERED;
                        break;
                    case REFRESH_TRIGGERED:
                        // Nothing needs to be done
                        break;
                }
            }

            if (shouldStart) {
                ThreadUtil.runInBackgroundThread(new ThreadUtil.Runnable() {
                    @Override
                    public void run() {
                        boolean done = false;
                        while (!done) {
                            runAction();

                            synchronized (this) {
                                if (mRunState == RunState.RUNNING) {
                                    mRunState = RunState.NOT_RUNNING;
                                    done = true;
                                } else if (mRunState == RunState.REFRESH_TRIGGERED) {
                                    mRunState = RunState.RUNNING;
                                } else {
                                    throw new IllegalStateException("IllegalState: mRunState == " + mRunState);
                                }
                            }
                        }

                        actionComplete();
                    }
                });
            }
        }

        public abstract void runAction();
        public void actionComplete() { }

        private enum RunState {
            NOT_RUNNING,
            RUNNING,
            REFRESH_TRIGGERED
        }

        private RunState mRunState;
    }
}
