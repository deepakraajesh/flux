package com.unbxd.skipper.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FacetDetail {
    private FacetConfig facetConfig;
    private FacetIntelligence facetIntelligence;
}
