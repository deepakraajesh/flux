package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.size;

public class DagInfo {
    @JsonProperty(value = "begin_node_ids")
    private List<String> beginNodeId;

    @JsonProperty(value = "dag_structure")
    private Map<String, List<String>> dagStructure;

    @JsonProperty(value = "nodes_details")
    private Map<String, WorkflowNodeDetails> nodeDetails;

    public void dagWith2Node(WorkflowNodeDetails startNode, WorkflowNodeDetails endNode) {
        beginNodeId = Collections.singletonList(startNode.getNodeId());
        dagStructure = Collections.singletonMap(startNode.getNodeId(),
                Collections.singletonList(endNode.getNodeId()));
        nodeDetails = new HashMap<>();
        nodeDetails.put(startNode.getNodeId(), startNode);
        nodeDetails.put(endNode.getNodeId(), endNode);
    }

    public void dagWithNodeList(List<WorkflowNodeDetails> nodeList) {
        int size = size(nodeList);
        nodeDetails = getNodeDetails(nodeList, size);
        dagStructure = getDagStructure(nodeList, size);
        beginNodeId = Collections.singletonList(nodeList.get(0).getNodeId());
    }

    private Map<String, WorkflowNodeDetails> getNodeDetails(List<WorkflowNodeDetails> nodeList, int size) {
        Map<String, WorkflowNodeDetails> nodeDetails = new HashMap<>();
        for(int i = 0; i < size; ++i) {
            nodeDetails.put(nodeList.get(i).getNodeId(), nodeList.get(i));
        }
        return nodeDetails;
    }

    private Map<String, List<String>> getDagStructure(List<WorkflowNodeDetails> nodeList, int size) {
        Map<String, List<String>> dagStructure = new HashMap<>();
        for(int i = 0; i < size - 1; ++i) {
            dagStructure.put(nodeList.get(i).getNodeId(), Collections.singletonList(nodeList.get(i + 1).getNodeId()));
        }
        return dagStructure;
    }
}

