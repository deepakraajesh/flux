package com.unbxd.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextFacet {
    private String facetName;
    private Integer count;
    private List<Object> values;
}
