package com.unbxd.skipper.search.exception;

import lombok.Data;

@Data
public class AutosuggestServiceException extends Exception{
    public int statusCode;
    public int erroCode;
    public AutosuggestServiceException(int code, String message){
        super(message);
        this.statusCode = code;
    }
    public AutosuggestServiceException(int code,int errorCode, String message){
        super(message);
        this.statusCode = code;
        this.erroCode = errorCode;
    }
}
