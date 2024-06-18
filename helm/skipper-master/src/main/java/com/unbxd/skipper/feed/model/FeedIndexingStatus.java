package com.unbxd.skipper.feed.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FeedIndexingStatus {
    private String status;
    private String message;
    private String duration;
    private List<String> errors;

    public FeedIndexingStatus(String status,
                              String message,
                              String duration,
                              List<String> errors) {
        this.status = status;
        this.errors = errors;
        this.message = message;
        this.duration = duration;
    }
}

