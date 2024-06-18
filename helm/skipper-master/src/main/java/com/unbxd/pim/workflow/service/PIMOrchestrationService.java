package com.unbxd.pim.workflow.service;

import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;

import java.io.IOException;

public interface PIMOrchestrationService {

    String ID_KEY = "id";
    String DATA_KEY = "data";
    String ORG_ID_KEY = "org_id";
    String DETAILS_KEY = "details";

    WorkflowContext triggerWorkflow(String appId, String authToken, String cookie,
                                String siteName, String siteKey, String region, String user) throws Exception;

    WorkflowContext triggerWorkflow(WorkflowContext context, String workflowState) throws IOException, PimWorkflowException;
}
