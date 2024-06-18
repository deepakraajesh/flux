package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import retrofit2.Response;

import java.io.IOException;

public class StartWorkflowProcessor implements WorkflowProcessor {

    private PIMRemoteService pimRemoteService;

    @Inject
    public StartWorkflowProcessor(PIMRemoteService pimRemoteService) {
        this.pimRemoteService = pimRemoteService;
    }

    @Override
    public void processWorkflow(WorkflowContext workflowContext) throws
            IOException, PimWorkflowException {
        JsonObject startWorkflowRequest = fetchConfigObject(workflowContext.getTemplateObject(), START_WORKFLOW);

        JsonArray workflowIdArray = new JsonArray();
        workflowIdArray.add(workflowContext.getWorkflowId());
        startWorkflowRequest.add(WORKFLOW_IDS_KEY, workflowIdArray);

        Response<JsonObject> startWorkflowResponse = pimRemoteService.startWorkflow(workflowContext.getAuthToken(),
                workflowContext.getCookie(), workflowContext.getOrgId(), startWorkflowRequest).execute();
        if(!startWorkflowResponse.isSuccessful()) {
            throw new PimWorkflowException("Error while triggerring workflow code:" + startWorkflowResponse.code() +
                    " reason:" + startWorkflowResponse.errorBody().string());
        }
    }

    @Override
    public String nextWorkflowProcessor() { return COMPLETE_WORKFLOW; }
}
