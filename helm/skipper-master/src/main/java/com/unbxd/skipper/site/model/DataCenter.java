package com.unbxd.skipper.site.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCenter {

    private String id;
    private String name;
    @JsonProperty("lat_long")
    private String latLong;

    private DataCenterType type = DataCenterType.AWS;

    private String skipperEndPoint;
}
