package com.unbxd.skipper.feed.dim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DimensionIndividualMap {
    private String id;
    private String label;
    private String dataType;
    private String mapping;
}

