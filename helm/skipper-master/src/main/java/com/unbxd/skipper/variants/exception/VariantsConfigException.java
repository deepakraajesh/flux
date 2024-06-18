package com.unbxd.skipper.variants.exception;

import lombok.Data;

@Data
public class VariantsConfigException extends Exception{
    public Integer code;
    public VariantsConfigException(Integer code , String message) {
        super(message);
        this.code = code;
    }
}
