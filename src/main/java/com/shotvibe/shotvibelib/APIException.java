package com.shotvibe.shotvibelib;

public class APIException extends Exception {
    /**
     * @param userFriendlyMessage A localized string that should be shown to the user
     * @param requestTime         How long the request took to complete, in milliseconds. Or 0 if
     *                            the request was not timed
     * @param httpMethod          The HTTP method that was called
     * @param url                 The URL that was called
     * @param httpStatusCode      The HTTP status code that the server returned, or null if none
     * @param httpBody            The HTTP response body that the server returned, or null if none
     * @param extraException      An optional Exception object that caused the request to fail, may
     *                            be null
     */
    public APIException(String userFriendlyMessage,
                        long requestTime,
                        String httpMethod,
                        String url,
                        Integer httpStatusCode,
                        String httpBody,
                        Exception extraException) {
        super(userFriendlyMessage);

        mUserFriendlyMessage = userFriendlyMessage;
        mRequestTime = requestTime;
        mHttpMethod = httpMethod;
        mUrl = url;
        mHttpStatusCode = httpStatusCode;
        mHttpBody = httpBody;
        mExtraException = extraException;
    }

    public static APIException ErrorStatusCodeException(HTTPResponse response) {
        // TODO The userFriendlyMessage should be somehow localized
        String userFriendlyMessage;
        if (response.getStatusCode() >= 500 && response.getStatusCode() < 600) {
            userFriendlyMessage = "The Server is down (" + response.getStatusCode() + ")";
        } else {
            userFriendlyMessage = "Unknown error received from server (" + response.getStatusCode() + ")";
        }
        return new APIException(
                userFriendlyMessage,
                response.getRequestTime(),
                response.getMethod(),
                response.getUrl(),
                response.getStatusCode(),
                response.bodyAsUTF8String(),
                null);
    }

    public static APIException FromHttpException(HTTPException httpException) {
        String userFriendlyMessage =
                "Check that you are connected to the internet.\n\n"
                        + "Error: " + httpException.getUserFriendlyMessage();
        return new APIException(
                userFriendlyMessage,
                httpException.getRequestTime(),
                httpException.getHttpMethod(),
                httpException.getUrl(),
                httpException.getHttpStatusCode(),
                httpException.getHttpBody(),
                httpException);
    }

    public static APIException FromJSONException(HTTPResponse response,
                                                 JSONException jsonException) {
        // TODO The userFriendlyMessage should be somehow localized
        String userFriendlyMessage = "There was an error reading the content from the server (JSON parse error)";
        return new APIException(
                userFriendlyMessage,
                response.getRequestTime(),
                response.getMethod(),
                response.getUrl(),
                response.getStatusCode(),
                response.bodyAsUTF8String(),
                jsonException);
    }

    public String getUserFriendlyMessage() {
        return mUserFriendlyMessage;
    }

    public String getTechnicalMessage() {
        String result = mHttpMethod + " " + mUrl;
        result += "\n\nRequest time: " + mRequestTime + "ms";

        if (mHttpStatusCode != null) {
            result += "\nHTTP status code: " + mHttpStatusCode;
        }
        if (mHttpBody != null) {
            result += "\nHTTP Response body: " + mHttpBody;
        }
        if (mExtraException != null) {
            result += "\n\nException: " + mExtraException.getMessage();
        }
        return result;
    }

    private final String mUserFriendlyMessage;
    private final long mRequestTime;

    private final String mHttpMethod;
    private final String mUrl;

    private final Integer mHttpStatusCode;
    private final String mHttpBody;
    private final Exception mExtraException;
}
