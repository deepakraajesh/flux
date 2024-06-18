package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackConfig {
    private Tolerance tolerance;
    private List<Header> headers;
    private String method = "POST";
    private boolean synchronous = true;
    private String url = "http://" + System.getProperty("domain.name");

    private CallbackConfig(String urlPath,
                           boolean isSync,
                           String description) {
        this.url += urlPath;
        this.synchronous = isSync;
        this.tolerance = new Tolerance();
        headers = Collections.singletonList(Header
                .getInstance("content-type",
                        "application/json",
                        description));
    }

    public static CallbackConfig getInstance(String urlPath,
                                             boolean isSync,
                                             String description) {
        return new CallbackConfig(urlPath, isSync, description);
    }
}
