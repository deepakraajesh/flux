package com.unbxd.skipper.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacetIntelligence {
    private Integer noOfProducts;
    private Integer noOfUniqueValues; // this is used in case of text and path facets
    private Float minValue; // these
    private Float maxValue; // two are used in case of range facet
}
