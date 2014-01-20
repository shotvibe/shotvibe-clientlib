package com.shotvibe.shotvibelib;

public class APIException extends Exception {
    public APIException() {
        super();
    }

    public APIException(String detailMessage) {
        super(detailMessage);
    }

    public APIException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public APIException(Throwable throwable) {
        super(throwable);
    }

    public static APIException ErrorStatusCodeException(HTTPResponse response) {
        return new APIException("HTTP Error Status Code: " + response.getStatusCode());
    }
}
