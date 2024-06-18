package com.unbxd.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Region {
    @JsonProperty("ref_user_id")
    String refId;
}

