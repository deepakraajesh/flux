package com.unbxd.skipper.relevancy.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Status {
    private String code;
    private String message;

    private Status(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Status getInstance(String code) {
        return new Status(code, null);
    }

    public static Status getInstance(String code, String message) {
        return new Status(code, message);
    }
}