package com.unbxd.skipper.feed.dim;

public class DimException extends Exception {

    int code;
    public DimException(String msg, int code) {
        super(msg);
    }

    public int getCode() {
        return code;
    }
}

