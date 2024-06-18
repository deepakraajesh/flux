package com.unbxd.skipper.dictionary.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unbxd.analyser.exception.AnalyserException;


public class ErrorCode {

    // Missing analyzer
    public static final String MISSING_ANALYZER = "10002";

    // Missing Parameter
    public static final String INVALID_PARAMETER = "10003";

    // Asset Exception
    public static final String ASSET_ERROR = "10004";

    // Config Exception
    public static final String CONFIG_ERROR = "10005";

    // Missing asset
    public static final String MISSING_ASSET = "10006";

    // Analyzer Exception
    public static final String ANALYZER_ERROR = "10007";

    // Missing asset
    public static final String JSON_PROCESSING = "40001";


    public static int getErrorCode(Exception e) {
        if (e instanceof JsonProcessingException) {
            return Integer.parseInt(JSON_PROCESSING);
        } else if (e instanceof AssetException) {
            AssetException exception = (AssetException) e;
            return exception.getCode();
        } else if(e instanceof AnalyserException){
            AnalyserException exception = (AnalyserException) e;
            return exception.getStatusCode();
        }
        return 0;
    }

}