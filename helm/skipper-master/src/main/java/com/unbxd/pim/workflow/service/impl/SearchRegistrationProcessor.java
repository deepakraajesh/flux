package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.field.service.FieldService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.SiteKeyCred;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PimSearchApp;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;

@Log4j2
public class SearchRegistrationProcessor implements WorkflowProcessor {

    private PimSearchApp pimSearchApp;
    private FieldService gimli;

    @Inject
    public SearchRegistrationProcessor(PimSearchApp pimSearchApp,
                                       FieldService gimli) {
        this.pimSearchApp = pimSearchApp;
        this.gimli = gimli;
    }

    @Override
    public String nextWorkflowProcessor() { return PIM_REGISTRATION; }

    @Override
    public void processWorkflow(WorkflowContext workflowContext)
            throws IOException, PimWorkflowException {
        JsonObject searchRegistrationRequest = fetchConfigObject(workflowContext.getTemplateObject(), SEARCH_REGISTRATION);
        if(searchRegistrationRequest == null) {
            String msg = "pim workflow template config is not set";
            log.error(msg + "for site: "+ workflowContext.getSiteKey());
            throw new PimWorkflowException(msg);
        }

        SiteKeyCred siteCred = null;
        try {
            siteCred = gimli.getSiteDetails(workflowContext.getSiteKey());
        } catch (FieldException e) {
            throw new PimWorkflowException("Error while fetching site credentials reason: " + e.getMessage());
        }

        JsonObject appObject = new JsonObject();
        appObject.addProperty(SECRET_KEY, siteCred.getSecretKey());
        appObject.addProperty(API_KEY, siteCred.getApiKey());
        appObject.addProperty(SITEKEY, workflowContext.getSiteKey());
        appObject.addProperty(REGION, workflowContext.getRegion());
        searchRegistrationRequest.add("unbxd", appObject);

        Response<JsonObject> searchRegistrationResponse = pimSearchApp
                .registerSearch(workflowContext.getCookie(), searchRegistrationRequest).execute();
        if(!searchRegistrationResponse.isSuccessful()) {
            throw new PimWorkflowException("Error while registring PIM+serch App " +
                    "code:" + searchRegistrationResponse.code()
                    + " reason:" + searchRegistrationResponse.errorBody().string());
        }
    }
}
