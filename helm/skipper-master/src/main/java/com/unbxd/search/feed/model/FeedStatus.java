package com.unbxd.search.feed.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedStatus {
    private String status;
    private String message;
    @JsonAlias("id")
    private String uploadId;
    private String duration;
    private List<String> errors;

    @JsonAlias("date")
    private String triggeredAt;
    private Integer code;
    private Long totalTime; // not supported in feed v1
    public boolean isCompleted() {
        if("FAILED".equals(status) || "INDEXED".equals(status))

            return Boolean.TRUE;
        return Boolean.FALSE;
    }

    public boolean isSuccessful() {
        return "INDEXED".equals(status);
    }
}

