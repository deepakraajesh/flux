package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.unbxd.console.model.ProductType.search;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsoleFacetFieldRequest {
    private SingleConsoleFacetField facet;
    private List<ConsoleFacetField> facets;

    @JsonProperty("product_type")
    private ProductType productType = search;

    public ConsoleFacetFieldRequest(List<ConsoleFacetField> facets) { this.facets = facets; }

    public ConsoleFacetFieldRequest(List<ConsoleFacetField> facets, ProductType productType) {
        this.facets = facets;
        this.productType = productType;
    }
}
