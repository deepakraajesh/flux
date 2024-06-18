package com.unbxd.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RangeFacetValues {
    private List<Object> counts;
    private Integer gap;
    private Integer start;
    private Integer end;
}
