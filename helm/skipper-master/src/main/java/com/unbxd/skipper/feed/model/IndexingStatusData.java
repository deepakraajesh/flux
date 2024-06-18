package com.unbxd.skipper.feed.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexingStatusData {
    private String feedId;
    private String status;
    private String triggeredAt;
    private String message;
    private Integer code;
    private Long previousIndexingTime;
    private static String INDEXED = "INDEXED";
    private static String FAILED = "FAILED";
    private static String REJECTED = "REJECTED";

    public boolean isCompleted() {
        if(FAILED.equals(status) || INDEXED.equals(status) || REJECTED.equals(status))
            return Boolean.TRUE;
        return Boolean.FALSE;
    }
}
