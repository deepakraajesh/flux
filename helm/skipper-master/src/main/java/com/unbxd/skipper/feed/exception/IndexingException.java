package com.unbxd.skipper.feed.exception;

import lombok.Data;

@Data
public class IndexingException extends Exception {
    public Integer statusCode;
    public Integer errorCode;

    public IndexingException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public IndexingException(Integer statusCode, Integer errorCode ,String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}
