package com.shotvibe.shotvibelib;

public class HTTPException extends Exception {
    public HTTPException(String technicalMessage,
                         String userFriendlyMessage,
                         long requestTime,
                         String httpMethod,
                         String url,
                         Integer httpStatusCode,
                         String httpBody) {
        super(technicalMessage);

        mUserFriendlyMessage = userFriendlyMessage;
        mRequestTime = requestTime;
        mHttpMethod = httpMethod;
        mUrl = url;
        mHttpStatusCode = httpStatusCode;
        mHttpBody = httpBody;
    }

    public String getUserFriendlyMessage() {
        return mUserFriendlyMessage;
    }

    public long getRequestTime() {
        return mRequestTime;
    }

    public String getHttpMethod() {
        return mHttpMethod;
    }

    public String getUrl() {
        return mUrl;
    }

    public Integer getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public String getHttpBody() {
        return mHttpBody;
    }

    private final String mUserFriendlyMessage;
    private final long mRequestTime;

    private final String mHttpMethod;
    private final String mUrl;

    private final Integer mHttpStatusCode;
    private final String mHttpBody;
}
