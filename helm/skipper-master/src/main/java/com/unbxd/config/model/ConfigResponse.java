package com.unbxd.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigResponse {
    private int code;
    private String service;
    private String response;
    private String healthCheckUrl;

    private ConfigResponse(int code, String service, String response, String healthCheckUrl) {
        this.code = code;
        this.service = service;
        this.response = response;
        this.healthCheckUrl = healthCheckUrl;
    }

    public static ConfigResponse getInstance(int code, String service, String response, String healthCheckUrl) {
        return new ConfigResponse(code, service, response, healthCheckUrl);
    }
}
