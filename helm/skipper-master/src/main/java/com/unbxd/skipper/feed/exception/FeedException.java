package com.unbxd.skipper.feed.exception;

import lombok.Data;

@Data
public class FeedException extends Exception {

    Integer statusCode;
    Integer errorCode;
    public FeedException(String msg) {
        super(msg);
    }
    public FeedException(Integer statusCode, String msg) {
        super(msg);
        this.statusCode = statusCode;
    }
    public FeedException(Integer statusCode, Integer errorCode ,String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}

