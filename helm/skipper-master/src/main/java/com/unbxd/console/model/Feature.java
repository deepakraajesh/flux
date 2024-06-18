package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class Feature {
    @JsonProperty("feature_configs")
    Map<String, Object> config;
}

