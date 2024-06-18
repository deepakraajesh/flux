package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventConfig {


    private String action;
    private String entity;
    @JsonProperty(value = "additional_configs")
    private Map<String, Object> additionalConfigs;
}

