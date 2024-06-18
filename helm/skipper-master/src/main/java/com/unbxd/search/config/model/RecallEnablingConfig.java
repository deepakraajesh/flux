package com.unbxd.search.config.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecallEnablingConfig {
    private Boolean enabled;
    private Integer min;
}

