package com.unbxd.field.exception;

import lombok.Data;

@Data
public class FieldException extends Exception {
    public Integer code;

    public FieldException (Integer code) {
        this(code, null);
    }

    public FieldException (Integer code, String message) {
        super(message);
        this.code = code;
    }
}
