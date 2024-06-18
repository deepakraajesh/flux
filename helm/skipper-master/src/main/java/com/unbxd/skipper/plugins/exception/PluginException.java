package com.unbxd.skipper.plugins.exception;

import lombok.Data;

@Data
public class PluginException extends Exception {
    public int code;

    public PluginException(int code, String message) {
        super(message);
        this.code = code;
    }
}

