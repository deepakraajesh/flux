package com.unbxd.pim.workflow.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.gson.JsonObject;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class WorkflowContext {

    private String appId;
    private String orgId;
    private String apiKey;
    private String orgKey;
    private String cookie;
    private String siteKey;
    private String siteName;
    private String region;
    private String adapterId;
    private String channelId;
    private String authToken;
    private String workflowId;
    private String orgAppId;
    private JsonObject templateObject;
    private Map<String, String> params = new HashMap<>();

    @JsonAnySetter
    public void set(String name, String value) {
        params.put(name, value);
    }
}
