package com.unbxd.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GimliBaseResponse {
    private static final String MESSAGE = "message";
    private static final String CODE = "code";
    public static final String GIMLI_DUPLICATE_ADDITION_ERROR_CODE = "DBDataAlreadyPresent";


    private String status;
    private List<Map<String, Object>> errors;

    public String getErrors() {
        if(errors == null)
            return null;
        for(Map<String, Object> eachError: errors) {
            if(eachError.containsKey(MESSAGE)) {
                return String.valueOf(eachError.get(MESSAGE));
            }
        }
        return null;
    }
    public String getErrorCode() {
        if(errors == null)
            return null;
        for(Map<String, Object> eachError: errors) {
            if(eachError.containsKey(CODE)) {
                return String.valueOf(eachError.get(CODE));
            }
        }
        return null;
    }
}

