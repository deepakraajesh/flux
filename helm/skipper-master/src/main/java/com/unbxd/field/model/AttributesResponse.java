package com.unbxd.field.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributesResponse extends PageResponse {
    @JsonProperty(value = "entries")
    @JsonAlias("indexFields")
    private List<Attribute> attributes;
}
