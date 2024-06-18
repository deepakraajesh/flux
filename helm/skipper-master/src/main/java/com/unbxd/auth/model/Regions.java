package com.unbxd.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Regions {
    @JsonProperty("us")
    Region region;
}

