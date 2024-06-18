package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include. NON_NULL)
public class ConsoleFacetField {

    private String id;
    private Boolean enabled;
    private Integer position;

    @JsonProperty("global_facet_id")
    private String globalFacetId;

    @JsonProperty("facet_name")
    private String facetName;

    @JsonProperty("facet_type")
    private String facetType;

    @JsonProperty("facet_field")
    private String facetField;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("text_facet_attributes")
    private FacetAttributes textFacetAttributes;

    @JsonProperty("path_facet_attributes")
    private FacetAttributes pathFacetAttributes;

    @JsonProperty("range_facet_attributes")
    private FacetAttributes rangeFacetAttributes;
}
