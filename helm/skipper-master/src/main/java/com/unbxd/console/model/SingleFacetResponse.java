package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SingleFacetResponse {
    private Boolean success;
    private ConsoleFacetField facets;
    private Map<String, String> error;

    private SingleFacetResponse(Boolean success) { this.success = success; }

    public static SingleFacetResponse getInstance(Boolean success) {
        return new SingleFacetResponse(success);
    }
}
