package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.CollectionUtils.size;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class FacetResponse {
    private Integer page;
    private Boolean success;
    private Integer numberOfFacets;
    private Map<String, String> error;
    private Long numberOfEnableFacets;
    private List<ConsoleFacetField> facets;
    private Boolean hasJobRun;

    public FacetResponse(Boolean success, String message) {
        this.success = success;
        this.error = new HashMap<>();
        this.error.put("message", message);
    }

    @JsonProperty("count")
    public Integer getCount() { return size(facets); }

    @JsonGetter("total")
    public Integer getNumberOfFacets() { return numberOfFacets; }

    @JsonSetter("numberOfFacets")
    public void setNumberOfFacets(Integer numberOfFacets) { this.numberOfFacets = numberOfFacets; }
}

