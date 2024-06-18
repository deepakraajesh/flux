package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodeDetails {
    @JsonProperty(value = "node_id")
    private String nodeId;
    @JsonProperty(value = "node_type")
    private String nodeType;
}

