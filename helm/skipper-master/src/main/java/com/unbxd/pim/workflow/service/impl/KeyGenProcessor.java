package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static com.unbxd.pim.workflow.service.PIMOrchestrationService.DATA_KEY;

@Log4j2
public class KeyGenProcessor implements WorkflowProcessor {

    private PIMRemoteService pimRemoteService;

    private static final String ORG_APP_ID = "org_app_id";

    @Inject
    public KeyGenProcessor(PIMRemoteService pimRemoteService) {
        this.pimRemoteService = pimRemoteService;
    }

    @Override
    public String nextWorkflowProcessor() { return SEARCH_REGISTRATION; }

    @Override
    public void processWorkflow(WorkflowContext workflowContext) throws
            PimWorkflowException, IOException {
        JsonObject apiKeyGenRequest = fetchConfigObject(workflowContext.getTemplateObject(), API_KEY_GEN);

        apiKeyGenRequest.addProperty(SITE_NAME, workflowContext.getSiteKey());
        apiKeyGenRequest.addProperty(APP_CUSTOM_ID, workflowContext.getAppId());
        Call<JsonObject> apiKeyGenResponse = pimRemoteService.generateAPIKey(workflowContext.getAuthToken(),
                workflowContext.getCookie(), workflowContext.getOrgId(), apiKeyGenRequest);

        Response<JsonObject> response = apiKeyGenResponse.execute();
        if(!response.isSuccessful()) {
            String msg = "Error while creating apiKey in PIM";
            log.error(msg + " for site: " + workflowContext.getSiteKey()
                    + " response Code: " + response.code() + " reason:" + response.errorBody());
            throw new PimWorkflowException(msg);
        }
        parseKeyGenResponse(response, workflowContext);
    }

    private void parseKeyGenResponse(Response<JsonObject> apiKeyGenResponse, WorkflowContext workflowContext) {
        JsonObject jsonResponse = apiKeyGenResponse.body();

        JsonObject dataObject = jsonResponse.getAsJsonObject(DATA_KEY);

        JsonElement adapterIdElement = dataObject.get(ADAPTER_ID);
        if(adapterIdElement == null) {
            throw new RuntimeException("Incorrect response from KeyGen API: " + dataObject.getAsString());
        }

        workflowContext.setAdapterId(adapterIdElement.getAsString());
        workflowContext.setApiKey(dataObject.get(API_KEY).getAsString());
        workflowContext.setChannelId(dataObject.get(CHANNEL_ID).getAsString());
        workflowContext.setOrgAppId(dataObject.get(ORG_APP_ID).getAsString());
    }
}
