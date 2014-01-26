package com.shotvibe.shotvibelib;

public final class PhoneContactDisplayData {
    public PhoneContact getPhoneContact() {
        return mPhoneContact;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public Long getUserId() {
        if (mIsLoading) {
            throw new IllegalArgumentException("cannot call getUserId for Loading contact");
        }

        return mUserId;
    }

    public String getAvatarUrl() {
        if (mIsLoading) {
            throw new IllegalArgumentException("cannot call getUserId for Loading contact");
        }

        return mAvatarUrl;
    }

    public static PhoneContactDisplayData createLoaded(PhoneContact phoneContact, Long userId, String avatarUrl) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }

        if (avatarUrl == null) {
            throw new IllegalArgumentException("avatarUrl cannot be null");
        }

        return new PhoneContactDisplayData(phoneContact, false, userId, avatarUrl);
    }

    public static PhoneContactDisplayData createLoading(PhoneContact phoneContact) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }

        return new PhoneContactDisplayData(phoneContact, true, null, null);
    }

    private PhoneContactDisplayData(PhoneContact phoneContact, boolean isLoading, Long userId, String avatarUrl) {
        mPhoneContact = phoneContact;
        mIsLoading = isLoading;
        mUserId = userId;
        mAvatarUrl = avatarUrl;
    }

    private final PhoneContact mPhoneContact;
    private final boolean mIsLoading;
    private final Long mUserId;
    private final String mAvatarUrl;
}
