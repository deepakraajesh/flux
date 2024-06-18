package com.unbxd.skipper.analyser.migration;

import lombok.Data;

@Data
public class AnalyserMigrationException extends Exception {
    public Integer statusCode;
    public Integer errorCode;

    public AnalyserMigrationException (Integer code, String message) {
        super(message);
        this.statusCode = code;
    }

    public AnalyserMigrationException (Integer code, Integer errorCode, String message) {
        super(message);
        this.statusCode = code;
        this.errorCode = errorCode;
    }
}
