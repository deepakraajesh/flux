package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static com.unbxd.pim.workflow.service.PIMOrchestrationService.DATA_KEY;
import static com.unbxd.pim.workflow.service.PIMOrchestrationService.ID_KEY;

public class CreateWorkflowProcessor implements WorkflowProcessor {

    private PIMRemoteService pimRemoteService;

    @Inject
    public CreateWorkflowProcessor(PIMRemoteService pimRemoteService) {
        this.pimRemoteService = pimRemoteService;
    }

    @Override
    public void processWorkflow(WorkflowContext workflowContext)
            throws IOException, PimWorkflowException {
        JsonObject createWorkflowRequest = fetchConfigObject(workflowContext.getTemplateObject(), CREATE_WORKFLOW);
        if(createWorkflowRequest == null) {
            throw new PimWorkflowException("No config present to CreateWorkflow in PIM");
        }
        Call<JsonObject> createWorkflowResponse = pimRemoteService.createWorkflow(workflowContext.getAuthToken(),
                 workflowContext.getCookie(), workflowContext.getOrgId(), createWorkflowRequest);
        Response<JsonObject> jsonResponse = createWorkflowResponse.execute();
        if(!jsonResponse.isSuccessful()) {
            throw new PimWorkflowException("Internal server error while creating PIM workflow reason: "
                    + jsonResponse.errorBody().string());
        }
        workflowContext.setWorkflowId(extractWorkflowId(jsonResponse.body()));
    }

    @Override
    public String nextWorkflowProcessor() { return ADD_NODE; }

    private String extractWorkflowId(JsonObject createWorkflowResponse) {
        return createWorkflowResponse.getAsJsonObject(DATA_KEY).get(ID_KEY).getAsString();
    }


}
