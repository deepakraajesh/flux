package com.unbxd.search.config.model;

import lombok.Data;

import java.util.List;

/**
 * {
 *   precision: true/false,
 *   recall: {
 *     enabled: true/false
 *     min: 10
 *   },
 *   vertical: [<Vertical>]
 * }
 */
@Data
public class NEREnablingConfig {
    private boolean precision;
    private RecallEnablingConfig recall;
    private List<String> vertical;

    public NEREnablingConfig(List<String> vertical) {
        enable(vertical);
    }

    public void enable(List<String> vertical) {
        precision = Boolean.TRUE;
        recall = new RecallEnablingConfig(Boolean.FALSE, 0);
        this.vertical = vertical;
    }

}

