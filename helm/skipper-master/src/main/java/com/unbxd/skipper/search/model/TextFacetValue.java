package com.unbxd.skipper.search.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TextFacetValue {
    private String name;
    @JsonProperty("value")
    private Integer count;
}

