package com.unbxd.skipper.relevancy.expection;

import lombok.Data;

@Data
public class RelevancyServiceException extends Exception{
   private Integer statusCode;
   private Integer errorCode;

    public RelevancyServiceException(Integer code, String message){
        super(message);
        this.statusCode = code;
    }

    public RelevancyServiceException(Integer statusCode, Integer errorCode ,String msg) {
        super(msg);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}
