package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PimSearchApp;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Log4j2
public class PIMRegistrationProcessor implements WorkflowProcessor {

    private static final String GROUP_BY_PARENT = "groupByParent";
    private PimSearchApp pimSearchApp;

    @Inject
    public PIMRegistrationProcessor(PimSearchApp pimSearchApp) {
        this.pimSearchApp = pimSearchApp;
    }

    @Override
    public String nextWorkflowProcessor() { return CREATE_WORKFLOW; }

    @Override
    public void processWorkflow(WorkflowContext workflowContext) throws
            IOException, PimWorkflowException {
        JsonObject pimRegistrationRequest = fetchConfigObject(workflowContext.getTemplateObject(), PIM_REGISTRATION);
        if(pimRegistrationRequest == null) {
            String msg = "pim workflow template config is not set for " +PIM_REGISTRATION;
            log.error(msg + "for site: "+ workflowContext.getSiteKey());
            throw new PimWorkflowException(msg);
        }

        pimRegistrationRequest.addProperty(ORG_KEY, workflowContext.getOrgId());
        pimRegistrationRequest.addProperty(API_KEY, workflowContext.getApiKey());
        pimRegistrationRequest.addProperty(APP_ID_KEY, workflowContext.getAppId());
        pimRegistrationRequest.addProperty(SITE_NAME, workflowContext.getSiteName());
        pimRegistrationRequest.addProperty(IDENTIFIER, workflowContext.getSiteKey());

        // TODO: Fix the variant logic
        pimRegistrationRequest.addProperty(GROUP_BY_PARENT, Boolean.FALSE);

        Call<JsonObject> pimRegistrationResponse = pimSearchApp
                .registerPIM(workflowContext.getCookie(), pimRegistrationRequest);
        Response<JsonObject> response = pimRegistrationResponse.execute();
        if(!response.isSuccessful()) {
            String msg = "Error while processing PIM search app registration code: " + response.code()
                    + " reason: " + response.errorBody().string();
            throw new PimWorkflowException(msg);
        }
    }
}
