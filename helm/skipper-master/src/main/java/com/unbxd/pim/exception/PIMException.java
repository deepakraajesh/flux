package com.unbxd.pim.exception;

import lombok.Data;

@Data
public class PIMException extends Exception {
    private Integer code;

    public PIMException(String msg) {
        super(msg);
    }

    public PIMException(String msg, Integer code){
        super(msg);
        this.code = code;
    }
}

