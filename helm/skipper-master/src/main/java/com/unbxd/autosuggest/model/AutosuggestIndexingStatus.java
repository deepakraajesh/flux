package com.unbxd.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutosuggestIndexingStatus {
    private String status;
    @JsonAlias({"createdAt","date"})
    private String lastIndexedTime;
    private Integer code;
    private Long totalTime;
    private Map<String,String> updates;
}

