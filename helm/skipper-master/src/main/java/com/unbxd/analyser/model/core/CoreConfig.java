package com.unbxd.analyser.model.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoreConfig {
    @JsonProperty(value = "analyzers")
    private List<AnalyserConfig> analyzerConfigs;
}
