package com.shotvibe.shotvibelib;

public class AuthData {
    public AuthData(long userId, String authToken, String defaultCountryCode) {
        if (authToken == null) {
            throw new IllegalArgumentException("authToken must not be null");
        }
        if (defaultCountryCode == null) {
            throw new IllegalArgumentException("defaultCountryCode must not be null");
        }

        mUserId = userId;
        mAuthToken = authToken;
        mDefaultCountryCode = defaultCountryCode;
    }

    public long getUserId() {
        return mUserId;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public String getDefaultCountryCode() {
        return mDefaultCountryCode;
    }

    private final long mUserId;
    private final String mAuthToken;
    private final String mDefaultCountryCode;
}
