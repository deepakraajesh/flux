package com.unbxd.analyser.model.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyserConfig {
    @JsonProperty(value = "name")
    private String commonName;

    @JsonProperty(value = "factories")
    private List<FilterConfig> factories;
}
