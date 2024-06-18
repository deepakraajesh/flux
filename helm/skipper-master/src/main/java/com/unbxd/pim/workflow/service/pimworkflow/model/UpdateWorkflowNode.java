package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class UpdateWorkflowNode {
    @JsonProperty(value = "dag_info")
   private DagInfo dagInfo;
   @JsonProperty(value = "trigger_infos")
   private List<TriggerInfo> triggerInfo;

   @JsonIgnore
   public void setExportToNetwork(WorkflowNodeDetails startNode, WorkflowNodeDetails endNode) {
       this.dagInfo = new DagInfo();
       dagInfo.dagWith2Node(startNode, endNode);
       TriggerInfo importTrigger = new TriggerInfo();
       importTrigger.setTriggerByImport();
       this.triggerInfo = Collections.singletonList(importTrigger);
   }

    @JsonIgnore
    public void setExportToNetwork(List<WorkflowNodeDetails> nodeList) {
        this.dagInfo = new DagInfo();
        dagInfo.dagWithNodeList(nodeList);
        TriggerInfo importTrigger = new TriggerInfo();
        importTrigger.setTriggerByImport();
        this.triggerInfo = Collections.singletonList(importTrigger);
    }
}

