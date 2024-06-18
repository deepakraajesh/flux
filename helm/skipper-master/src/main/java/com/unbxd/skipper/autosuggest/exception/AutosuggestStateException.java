package com.unbxd.skipper.autosuggest.exception;

import lombok.Data;

@Data
public class AutosuggestStateException extends Exception {
    public Integer code;

    public AutosuggestStateException (Integer code, String message) {
        super(message);
        this.code = code;
    }
}
