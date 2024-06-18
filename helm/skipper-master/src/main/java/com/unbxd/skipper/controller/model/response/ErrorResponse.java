package com.unbxd.skipper.controller.model.response;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;


@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
    private String status;
    private Map<String, String> errorMap;
    private static final String MESSAGE = "message";
    private Integer errorCode;

    public ErrorResponse(String statusText) {
        this.status = statusText;
        this.errorCode = 500;
    }

    public ErrorResponse(String statusText,Integer errorCode) {
        this.status = statusText;
        this.errorCode = errorCode;
    }

    public ErrorResponse(Integer errorCode) {
        this.errorCode = errorCode;
    }

    private ErrorResponse(Map<String, String> errorMap) {
        this.errorMap = errorMap;
        setMessageAsStatus(errorMap);
    }

    private void setMessageAsStatus(Map<String, String> errorMap) {
        if(MapUtils.isNotEmpty(errorMap) && errorMap.containsKey(MESSAGE)) {
            this.status = errorMap.get(MESSAGE);
            this.errorMap.remove(MESSAGE);
        }
    }

    @JsonGetter("errorMap")
    public Map<String, String> getErrorMap() { return errorMap; }

    public static ErrorResponse getInstance(Map<String, String> errorMap) {
        return new ErrorResponse(errorMap);
    }

    @JsonSetter("error")
    public void setErrorMap(Map<String, String> errorMap) {
        this.errorMap = errorMap;
        setMessageAsStatus(this.errorMap);
    }

    public static ErrorResponse getInstance(String status) {
        return new ErrorResponse(status);
    }

    public static ErrorResponse getInstance(Integer errorCode) {
        return new ErrorResponse(errorCode);
    }

}
