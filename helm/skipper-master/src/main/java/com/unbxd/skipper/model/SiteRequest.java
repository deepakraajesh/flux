package com.unbxd.skipper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SiteRequest {

    @JsonProperty("site_name")
    private String name;
    private String regions;
    private String language;
    private String platform;
    private String vertical;
    private String environment;
    private String tag;
    private String email;
    private String feedPlatform;
    private String appToken;
    private String shopName;
}
