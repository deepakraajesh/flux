package com.unbxd.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RangeFacet {
    private String facetName;
    private RangeFacetValues values;
}
