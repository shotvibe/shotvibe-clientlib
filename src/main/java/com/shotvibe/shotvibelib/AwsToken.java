package com.shotvibe.shotvibelib;

public class AwsToken {
    public AwsToken(String awsAccessKey, String awsSecretKey, String awsSessionToken, DateTime expires) {
        if (awsAccessKey == null) {
            throw new IllegalArgumentException("awsAccessKey cannot be null");
        }
        if (awsSecretKey == null) {
            throw new IllegalArgumentException("awsSecretKey cannot be null");
        }
        if (awsSessionToken == null) {
            throw new IllegalArgumentException("awsSessionToken cannot be null");
        }
        if (expires == null) {
            throw new IllegalArgumentException("expires cannot be null");
        }

        mAwsAccessKey = awsAccessKey;
        mAwsSecretKey = awsSecretKey;
        mAwsSessionToken = awsSessionToken;
        mExpires = expires;
    }

    public String getAwsAccessKey() {
        return mAwsAccessKey;
    }

    public String getAwsSecretKey() {
        return mAwsSecretKey;
    }

    public String getAwsSessionToken() {
        return mAwsSessionToken;
    }

    public DateTime getExpires() {
        return mExpires;
    }

    private final String mAwsAccessKey;
    private final String mAwsSecretKey;
    private final String mAwsSessionToken;
    private final DateTime mExpires;
}
