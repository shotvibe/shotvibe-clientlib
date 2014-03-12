package com.shotvibe.shotvibelib;

public final class PhoneContactServerResult {
    public enum PhoneType {
        INVALID,
        MOBILE,
        LANDLINE
    }

    public static PhoneContactServerResult createNonMobileResult(PhoneContact phoneContact, PhoneType phoneType, DateTime queryTime) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }
        if (phoneType == null) {
            throw new IllegalArgumentException("phoneType cannot be null");
        }
        if (phoneType == PhoneType.MOBILE) {
            throw new IllegalArgumentException("phoneType cannot be MOBILE");
        }
        if (queryTime == null) {
            throw new IllegalArgumentException("queryTime cannot be null");
        }

        return new PhoneContactServerResult(phoneContact, phoneType, null, null, null, queryTime);
    }

    /**
     * Creates a PhoneContactServerResult for a MOBILE number
     *
     * @param phoneContact The PhoneContact
     * @param userId The userId that the phone number belongs to, or null if no user exists
     * @param avatarUrl The avatarUrl of the phone number
     * @param canonicalPhoneNumber The canonical E164 format of the phone number
     * @return A PhoneContactServerResult object
     */
    public static PhoneContactServerResult createMobileResult(PhoneContact phoneContact, Long userId, String avatarUrl, String canonicalPhoneNumber, DateTime queryTime) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }
        if (avatarUrl == null) {
            throw new IllegalArgumentException("avatarUrl cannot be null");
        }
        if (canonicalPhoneNumber == null) {
            throw new IllegalArgumentException("canonicalPhoneNumber cannot be null");
        }
        if (queryTime == null) {
            throw new IllegalArgumentException("queryTime cannot be null");
        }

        return new PhoneContactServerResult(phoneContact, PhoneType.MOBILE, userId, avatarUrl, canonicalPhoneNumber, queryTime);
    }

    public PhoneContact getPhoneContact() {
        return mPhoneContact;
    }

    public PhoneType getPhoneType() {
        return mPhoneType;
    }

    /**
     * Will be null if the phone number does not belong to a registered user
     *
     * This is only valid if mPhoneType == MOBILE
     * @return
     */
    public Long getUserId() {
        return mUserId;
    }

    /**
     * This is only valid if mPhoneType == MOBILE
     * @return
     */
    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    /**
     * This is only valid if mPhoneType == MOBILE
     * @return
     */
    public String getCanonicalPhoneNumber() {
        return mCanonicalPhoneNumber;
    }

    public DateTime getQueryTime() {
        return mQueryTime;
    }

    private PhoneContactServerResult(PhoneContact phoneContact,
                                     PhoneType phoneType,
                                     Long userId,
                                     String avatarUrl,
                                     String canonicalPhoneNumber,
                                     DateTime queryTime) {
        mPhoneContact = phoneContact;
        mPhoneType = phoneType;
        mUserId = userId;
        mAvatarUrl = avatarUrl;
        mCanonicalPhoneNumber = canonicalPhoneNumber;
        mQueryTime = queryTime;
    }

    private final PhoneContact mPhoneContact;
    private final PhoneType mPhoneType;

    private final Long mUserId;

    private final String mAvatarUrl;

    private final String mCanonicalPhoneNumber;

    private final DateTime mQueryTime;
}
