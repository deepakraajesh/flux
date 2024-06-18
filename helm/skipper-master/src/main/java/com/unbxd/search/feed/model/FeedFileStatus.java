package com.unbxd.search.feed.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedFileStatus {
    private String state;
    private String message;
    @JsonAlias("id")
    private String uploadId;
    private String duration;
    private List<String> errors;
    @JsonProperty("files")
    private List<FeedMetaInfo> feedMetaInfo;

    public boolean isCompleted() {
        if("FAILED".equals(state) || "INDEXED".equals(state))

            return Boolean.TRUE;
        return Boolean.FALSE;
    }

    public boolean isSuccessful() {
        return "INDEXED".equals(state);
    }
}

