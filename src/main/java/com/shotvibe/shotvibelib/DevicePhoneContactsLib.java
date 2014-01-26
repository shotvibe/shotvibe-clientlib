package com.shotvibe.shotvibelib;

public interface DevicePhoneContactsLib {
    public interface DeviceAddressBookListener {
        void deviceAddressBookChanged();
    }

    ArrayList<PhoneContact> getDevicePhoneContacts();

    void registerDeviceAddressBookListener(DeviceAddressBookListener listener);

    void unregisterDeviceAddressBookListener(DeviceAddressBookListener listener);
}
