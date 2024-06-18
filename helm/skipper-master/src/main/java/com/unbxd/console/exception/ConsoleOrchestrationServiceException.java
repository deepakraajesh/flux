package com.unbxd.console.exception;

import lombok.Data;

@Data
public class ConsoleOrchestrationServiceException extends Exception {
    private Integer statusCode;
    private Integer errorCode;

    public ConsoleOrchestrationServiceException(int statusCode, String message){
        super(message);
        this.statusCode = statusCode;
    }

    public ConsoleOrchestrationServiceException(Integer statusCode, Integer errorCode ,String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

}
