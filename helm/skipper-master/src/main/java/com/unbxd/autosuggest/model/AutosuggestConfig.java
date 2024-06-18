package com.unbxd.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutosuggestConfig {
    private Integer topQueriesCount;
}
