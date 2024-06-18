package com.unbxd.recommend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RectifyResponse {
    private int page;
    private int count;
    private long total;
    private List<QueryStats> entries;

    private RectifyResponse(int page,
                            int count,
                            long total,
                            List<QueryStats> entries) {
        this.page = page;
        this.count = count;
        this.total = total;
        this.entries = entries;
    }

    public static RectifyResponse getInstance(int page,
                                              int count,
                                              long total,
                                              List<QueryStats> entries) {
        return new RectifyResponse(page, count, total, entries);
    }
}
