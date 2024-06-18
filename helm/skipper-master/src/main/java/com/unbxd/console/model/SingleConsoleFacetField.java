package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include. NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SingleConsoleFacetField {

    private String id;
    private Boolean enabled;
    private Integer position;

    @JsonProperty("facet_name")
    private String facetName;

    @JsonProperty("facet_type")
    private String facetType;

    @JsonProperty("facet_field")
    private String facetField;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("global_facet_id")
    private String globalFacetId;

    @JsonProperty("facet_attributes")
    private FacetAttributes facetAttributes;

    public static SingleConsoleFacetField fromOther(ConsoleFacetField consoleFacetField) {
        SingleConsoleFacetField singleConsoleFacetField = new SingleConsoleFacetField();
        singleConsoleFacetField.setGlobalFacetId(consoleFacetField.getGlobalFacetId());
        singleConsoleFacetField.setDisplayName(consoleFacetField.getDisplayName());
        singleConsoleFacetField.setFacetField(consoleFacetField.getFacetField());
        singleConsoleFacetField.setFacetName(consoleFacetField.getFacetName());
        singleConsoleFacetField.setFacetType(consoleFacetField.getFacetType());
        singleConsoleFacetField.setPosition(consoleFacetField.getPosition());
        singleConsoleFacetField.setEnabled(consoleFacetField.getEnabled());
        singleConsoleFacetField.setId(consoleFacetField.getId());

        FacetAttributes pathFacetAttributes = consoleFacetField.getPathFacetAttributes();
        FacetAttributes textFacetAttributes = consoleFacetField.getTextFacetAttributes();
        FacetAttributes rangeFacetAttributes = consoleFacetField.getRangeFacetAttributes();

        if(pathFacetAttributes != null) {
            singleConsoleFacetField.setFacetAttributes(pathFacetAttributes);
        } else if(textFacetAttributes != null) {
            singleConsoleFacetField.setFacetAttributes(textFacetAttributes);
        } else {
            singleConsoleFacetField.setFacetAttributes(rangeFacetAttributes);
        }

        return singleConsoleFacetField;
    }
}
