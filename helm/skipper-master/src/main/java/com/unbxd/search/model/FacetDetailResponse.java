package com.unbxd.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacetDetailResponse {
    private ResponseDetail response;
    private Facets facets;
    @JsonProperty("stats")
    private StatsWrapper statsWrapper;
}
