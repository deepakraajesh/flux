package com.unbxd.skipper.relevancy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelevancyJobMetric {
    long totalTokens;
    long tokensMatch;

    public RelevancyJobMetric(String content) {
        String[] lines = content.split("/n");
        if(lines.length > 0)
            totalTokens = parseLong(lines[0]);
        if(lines.length > 1)
            tokensMatch = parseLong(lines[1]);
    }

    private long parseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0l;
        }
    }
}
