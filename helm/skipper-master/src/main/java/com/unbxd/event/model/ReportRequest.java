package com.unbxd.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportRequest {
    private int from = 0;
    private int size = 10;
    private String[] tagKeys;
    private String[] siteKeys;
    private String[] traceIds;
    private boolean inner = true;
    private String operationName;

    public static ReportRequest getInstance(int from,
                                            int size,
                                            String tagKey,
                                            String siteKey,
                                            String traceId,
                                            String operationName) {
        ReportRequest request = new ReportRequest();
        request.setSiteKeys(new String[]{siteKey});
        request.setTraceIds(new String[]{traceId});
        request.setTagKeys(new String[]{tagKey});
        request.setOperationName(operationName);
        request.setFrom(from);
        request.setSize(size);
        return request;
    }


    public static ReportRequest getInstance(String traceId, int size) {
        ReportRequest request = new ReportRequest();
        request.setTraceIds(new String[]{traceId});
        request.setSize(size);
        return request;
    }
}
