package com.unbxd.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unbxd.toucan.eventfactory.EventHistory;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportResponse {
    private int page;
    private int count;
    private long total;
    private EventHistory eventHistory;

    public static ReportResponse getInstance(int page,
                                             EventHistory eventHistory) {
        ReportResponse response = new ReportResponse();

        response.setCount(eventHistory.getEventHistory().size());
        response.setTotal(eventHistory.getCount());
        response.setEventHistory(eventHistory);
        response.setPage(page);
        return response;
    }
}
