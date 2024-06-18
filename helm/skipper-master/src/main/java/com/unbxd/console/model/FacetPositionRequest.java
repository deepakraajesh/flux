package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacetPositionRequest {
    @JsonProperty("to_pos")
    private String toPos;

    @JsonProperty("from_pos")
    private String fromPos;

    @JsonProperty("product_type")
    private ProductType productType;
}
