package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNode<T> {

    public static final String CALLBACK = "CALLBACK";

    @JsonProperty(value = "node_id")
    private String id;

    @JsonProperty(value = "node_type")
    private String type;

    private String orgId;

    private String workflowId;

    private T configs;

    public static WorkflowNode<CallbackConfig> getInstance(String orgId,
                                                           String urlPath,
                                                           boolean isSync,
                                                           String workflowId,
                                                           String description) {
        WorkflowNode<CallbackConfig> callBackNode = new WorkflowNode<>();
        callBackNode.setConfigs(CallbackConfig.getInstance(urlPath, isSync, description));
        callBackNode.setWorkflowId(workflowId);
        callBackNode.setType(CALLBACK);
        callBackNode.setOrgId(orgId);
        return callBackNode;
    }
}

