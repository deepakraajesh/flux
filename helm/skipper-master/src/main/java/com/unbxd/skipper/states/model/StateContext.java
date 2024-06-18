package com.unbxd.skipper.states.model;

import com.fasterxml.jackson.annotation.*;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.toucan.eventfactory.EventBuilder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StateContext {
    @JsonAlias("id")
    private String siteId;
    private String region;
    private String currency;
    private String importId;
    private String platform;
    @JsonAlias("name")
    private String siteName;
    private String language;
    private String vertical;
    private String adapterId;
    private String channelId;
    private String templateId;
    private String environment;
    private String feedPlatform;
    private Boolean variantsEnabled;
    private List<String> secondaryLanguages;

    private int code;
    private String errors;

    private String appId;
    @JsonProperty("_id")
    private String id;
    @JsonAlias("site_key")
    private String siteKey;
    private String workflowId;
    // We need to store metadata
    // private SiteMetaData meta;
    private ServeState serveState;

    @JsonIgnore
    private String authToken;
    @JsonIgnore
    private String appToken;
    @JsonIgnore
    private String shopName;
    private long timestamp;
    private String orgId;
    private String email;

    @JsonIgnore
    private String traceId;
    @JsonIgnore
    private EventBuilder eventBuilder;

    @JsonIgnore
    private Map<String, String> cookie = new HashMap<>();

    public Boolean getVariantsEnabled() {
        if(variantsEnabled == null)
            this.variantsEnabled = Boolean.FALSE;
        return variantsEnabled;
    }

    public String getCurrency() {
        if (isEmpty(this.currency)) {
            return "USD";
        }
        return this.currency;
    }
}
