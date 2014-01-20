package com.shotvibe.shotvibelib;

public class JSONException extends Exception {
    public JSONException() {
        super();
    }

    public JSONException(String detailMessage) {
        super(detailMessage);
    }

    public JSONException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public JSONException(Throwable throwable) {
        super(throwable);
    }
}
