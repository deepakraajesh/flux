package com.unbxd.analyser.exception;

import com.unbxd.cbo.response.Error;
import lombok.Data;

import java.util.List;

@Data
public class AnalyserException extends Exception{
    private Integer statusCode;
    private Integer errorCode;
    public AnalyserException(Integer statusCode, String message){
        super(message);
        this.statusCode = statusCode;
    }

    public AnalyserException(Integer statusCode, Integer errorCode ,String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public AnalyserException(Error errors){
        this(errors.getCode(), errors.getStatus());
    }
}
