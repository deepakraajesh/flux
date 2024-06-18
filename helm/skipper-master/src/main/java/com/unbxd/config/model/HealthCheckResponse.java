package com.unbxd.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthCheckResponse {
    private int code;
    private String status;
    private List<ConfigResponse> healthCheck;

    private HealthCheckResponse(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public static HealthCheckResponse getInstance() {
        HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
        healthCheckResponse.setHealthCheck(new ArrayList<>());
        healthCheckResponse.setCode(200);
        return healthCheckResponse;
    }

    public static HealthCheckResponse getInstance(int code, String status) {
        return new HealthCheckResponse(code, status);
    }
}
