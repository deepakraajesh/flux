package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetSearchableFieldsResponse {
    private int count;
    private int total;
    @JsonProperty("entries")
    private List<SearchableField> searchableFields;
    private Boolean relevancyJobRun;
}
