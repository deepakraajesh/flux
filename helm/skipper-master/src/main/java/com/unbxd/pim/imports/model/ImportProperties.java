package com.unbxd.pim.imports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportProperties {
    private long total;
    @JsonProperty(value="entries")
    private List<Map<String, Object>> properties;

}

