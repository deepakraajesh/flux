package com.unbxd.skipper.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {

    public static final String TEMPLATE_ID = "templateId";
    public static final String VERTICAL = "vertical";

    private String imageURL;
    private String templateName;
    private String templateId;
    private String vertical;
    private JsConfig jsConfig;
}
