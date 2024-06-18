package com.unbxd.analyser.model.core;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterConfig {
    @JsonProperty(value = "type")
    private String type;

    @JsonProperty(value = "name")
    private String commonName;

    @JsonProperty(value = "class")
    private String className;

    @JsonProperty(value = "analysis")
    private boolean analysisRequired;

    @JsonProperty(value = "inform")
    private boolean informRequired;

    @JsonProperty(value = "args")
    private Map<String, String> args;

    @JsonProperty(value = "analyzer")
    private String analyzerCommonName;

    @JsonIgnore
    public static FilterConfig getInstance(String type,
                                           String name,
                                           Boolean analysis,
                                           Map<String, String> args,
                                           String className,
                                           String analyzerCommonName,
                                           Boolean informRequired) {
        FilterConfig filterConfig = new FilterConfig();
        filterConfig.setType(type);
        filterConfig.setCommonName(name);
        filterConfig.setClassName(className);
        filterConfig.setAnalysisRequired(analysis);
        filterConfig.setAnalyzerCommonName(analyzerCommonName);
        filterConfig.setArgs(args);
        filterConfig.setInformRequired(informRequired);
        return filterConfig;
    }
}
