package com.unbxd.skipper.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacetConfig {
    private String sortOrder;
    private Integer facetLength;
    private Integer gap;
    private Integer start;
    private Integer end;
}
