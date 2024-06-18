package com.unbxd.pim.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowStatus {
    private String state;
    private String orgId;
    private String appId;
    private Boolean status;
    private Long createdAt;
    private String message;

}

