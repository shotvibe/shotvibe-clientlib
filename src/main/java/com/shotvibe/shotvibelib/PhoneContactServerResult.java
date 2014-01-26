package com.shotvibe.shotvibelib;

public final class PhoneContactServerResult {
    public enum PhoneType {
        INVALID,
        MOBILE,
        LANDLINE
    }

    public static PhoneContactServerResult createNonMobileResult(PhoneContact phoneContact, PhoneType phoneType) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }
        if (phoneType == null) {
            throw new IllegalArgumentException("phoneType cannot be null");
        }
        if (phoneType == PhoneType.MOBILE) {
            throw new IllegalArgumentException("phoneType cannot be MOBILE");
        }

        return new PhoneContactServerResult(phoneContact, phoneType, null, null, null);
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
    public static PhoneContactServerResult createMobileResult(PhoneContact phoneContact, Long userId, String avatarUrl, String canonicalPhoneNumber) {
        if (phoneContact == null) {
            throw new IllegalArgumentException("phoneContact cannot be null");
        }
        if (avatarUrl == null) {
            throw new IllegalArgumentException("avatarUrl cannot be null");
        }
        if (canonicalPhoneNumber == null) {
            throw new IllegalArgumentException("canonicalPhoneNumber cannot be null");
        }

        return new PhoneContactServerResult(phoneContact, PhoneType.MOBILE, userId, avatarUrl, canonicalPhoneNumber);
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

    private PhoneContactServerResult(PhoneContact phoneContact,
                                     PhoneType phoneType,
                                     Long userId,
                                     String avatarUrl,
                                     String canonicalPhoneNumber) {
        mPhoneContact = phoneContact;
        mPhoneType = phoneType;
        mUserId = userId;
        mAvatarUrl = avatarUrl;
        mCanonicalPhoneNumber = canonicalPhoneNumber;
    }

    private final PhoneContact mPhoneContact;
    private final PhoneType mPhoneType;

    private final Long mUserId;

    private final String mAvatarUrl;

    private final String mCanonicalPhoneNumber;
}
