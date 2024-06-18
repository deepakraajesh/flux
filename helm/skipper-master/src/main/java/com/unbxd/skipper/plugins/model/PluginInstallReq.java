package com.unbxd.skipper.plugins.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PluginInstallReq {
    private String dbDocId;
    private String siteId;
    private String shop;
    private String app;
    private String siteKey;
    private String region;
    private String apiKey;
    private String secretKey;
    private String appToken;
}

