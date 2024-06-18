package com.unbxd.skipper.search.exception;

import lombok.Data;
@Data
public class FacetStatServiceException extends Exception{
    int code;
    public FacetStatServiceException(int code, String message){
        super(message);
        this.code = code;
    }
}