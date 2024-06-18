package com.unbxd.autosuggest.exception;

import lombok.Data;

@Data
public class AutosuggestException extends Exception {
    public Integer statusCode;
    public Integer errorCode;

    public AutosuggestException(Integer code , String message) {
        super(message);
        this.statusCode = code;
    }
    public AutosuggestException(Integer code ,Integer errorCode, String message) {
        super(message);
        this.statusCode = code;
        this.errorCode  = errorCode;
    }
}

