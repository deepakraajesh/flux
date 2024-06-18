package com.unbxd.skipper.feed.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexingStatus {
    private String siteKey;
    private IndexingStatusData autosuggest;
    private IndexingStatusData catalog;
}
