package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include. NON_NULL)
public class FacetAttributes {

    private Integer id;

    @JsonProperty("sort_order")
    private String sortOrder;

    @JsonProperty("range_gap")
    private Integer rangeGap;

    @JsonProperty("range_end")
    private Integer rangeEnd;

    @JsonProperty("range_start")
    private Integer rangeStart;

    @JsonProperty("facet_length")
    private Integer facetLength;

    @JsonProperty("display_range")
    private Integer displayRange;

}
