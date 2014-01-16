package com.shotvibe.shotvibelib;

public class SQLException extends Exception {
    public SQLException() {
        super();
    }

    public SQLException(String detailMessage) {
        super(detailMessage);
    }

    public SQLException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SQLException(Throwable throwable) {
        super(throwable);
    }
}
