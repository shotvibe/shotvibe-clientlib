package com.shotvibe.shotvibelib;

public class HTTPException extends Exception {
    public HTTPException() {
        super();
    }

    public HTTPException(String detailMessage) {
        super(detailMessage);
    }

    public HTTPException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HTTPException(Throwable throwable) {
        super(throwable);
    }
}
