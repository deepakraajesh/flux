package com.unbxd.skipper.autosuggest.exception;

import lombok.Data;

@Data
public class SuggestionServiceException extends Exception{
    public Integer statusCode;
    public Integer errorCode;

    public SuggestionServiceException (Integer code, String message) {
        super(message);
        this.statusCode = code;
    }

    public SuggestionServiceException (Integer code, Integer errorCode, String message) {
        super(message);
        this.statusCode = code;
        this.errorCode = errorCode;
    }
}
