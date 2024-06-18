package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class TriggerInfo {
    @JsonProperty(value="trigger_type")
    private String triggerType;

    @JsonProperty(value = "event_config")
    private EventConfig eventConfig;

    @JsonIgnore
    public void setTriggerByImport() {
        triggerType = "EVENT";
        eventConfig = new EventConfig("COMPLETION", "IMPORT", new HashMap<>());
    }
}

