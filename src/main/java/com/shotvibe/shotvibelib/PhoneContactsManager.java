package com.shotvibe.shotvibelib;

public class PhoneContactsManager implements DevicePhoneContactsLib.DeviceAddressBookListener {
    public interface Listener {
        void phoneContactsUpdated(ArrayList<PhoneContactDisplayData> phoneContacts);
    }

    public PhoneContactsManager(DevicePhoneContactsLib devicePhoneContactsLib, ShotVibeAPI shotVibeAPI) {
        if (devicePhoneContactsLib == null) {
            throw new IllegalArgumentException("devicePhoneContactsLib cannot be null");
        }

        if (shotVibeAPI == null) {
            throw new IllegalArgumentException("shotVibeAPI cannot be null");
        }

        mDevicePhoneContactsLib = devicePhoneContactsLib;
        mShotVibeAPI = shotVibeAPI;

        mListener = null;

        mCurrentAddressBook = null;
        mCachedContacts = new ArrayList<PhoneContactServerResult>();
    }

    private final DevicePhoneContactsLib mDevicePhoneContactsLib;
    private final ShotVibeAPI mShotVibeAPI;

    /**
     * Must be called from the main thread
     * @param listener
     */
    public void setListener(Listener listener) {
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
        for (PhoneContactServerResult serverResult : mCachedContacts) {
            if (contact.equals(serverResult.getPhoneContact())) {
                return serverResult;
            }
        }

        // Not found
        return null;
    }

    private final ArrayList<PhoneContactServerResult> mCachedContacts;

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

    private void notifyListener() {
        final ArrayList<PhoneContactDisplayData> displayContacts = new ArrayList<PhoneContactDisplayData>();

        ArrayList<PhoneContact> deviceContacts;
        synchronized (mAddressBookLock) {
            deviceContacts = mCurrentAddressBook;
        }

        boolean shouldQuery = false;

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
                    // TODO check if cachedServerResult is old, and if so then set shouldQuery = true
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


            for (PhoneContact deviceContact : deviceContacts) {
                PhoneContactServerResult cachedServerResult = findCachedServerResult(deviceContact);
                if (cachedServerResult == null) {
                    toQuery.add(deviceContact);
                }
                // TODO If cachedServerResult is old then also add it to toQuery
            }

            ArrayList<PhoneContactServerResult> results;
            try {
                results = mShotVibeAPI.queryPhoneNumbers(toQuery, mShotVibeAPI.getAuthData().getDefaultCountryCode());
            } catch (APIException e) {
                e.printStackTrace();
                // TODO queue for retry
                return;
            }

            synchronized (mCachedContacts) {
                for (PhoneContactServerResult result : results) {
                    updatePhoneContactServerResult(result);
                }
            }

            notifyListener();
        }
    }

    // Must be called within a synchronized (mCachedContacts) block
    private void updatePhoneContactServerResult(PhoneContactServerResult result) {
        for (int i = 0; i < mCachedContacts.size(); ++i) {
            PhoneContactServerResult c = mCachedContacts.get(i);

            if (result.getPhoneContact().equals(c.getPhoneContact())) {
                mCachedContacts.set(i, result);
                return;
            }
        }

        mCachedContacts.add(result);
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
