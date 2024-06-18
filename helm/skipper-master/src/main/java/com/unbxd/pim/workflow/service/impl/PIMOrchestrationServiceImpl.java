package com.unbxd.pim.workflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.dao.WorkflowDao;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.model.WorkflowStatus;
import com.unbxd.pim.workflow.service.PIMOrchestrationService;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.pim.workflow.service.WorkflowProcessor;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.io.IOException;
import java.util.Map;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.API_KEY_GEN;
import static com.unbxd.pim.workflow.service.WorkflowProcessor.APP_ID_KEY;

@Slf4j
public class PIMOrchestrationServiceImpl implements PIMOrchestrationService {

    private WorkflowDao workflowDao;
    private PIMRemoteService pimRemoteService;
    private Map<String, WorkflowProcessor> workflowProcessors;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public PIMOrchestrationServiceImpl(WorkflowDao workflowDao,
                                       PIMRemoteService pimRemoteService,
                                       Map<String, WorkflowProcessor> workflowProcessors) {
        this.workflowDao = workflowDao;
        this.pimRemoteService = pimRemoteService;
        this.workflowProcessors = workflowProcessors;
    }

    @Override
    public WorkflowContext triggerWorkflow(String appId, String authToken, String cookie,
                                       String siteName, String siteKey, String region, String email)
            throws PimWorkflowException, IllegalArgumentException, IOException {
        WorkflowContext workflowContext = new WorkflowContext();
        Response<JsonObject> createOrgResponse = null;
        try {
            JsonObject req = new JsonObject();
            req.addProperty("name", siteName);
            req.addProperty("email", email);
            req.addProperty("account_type", "SEARCH");
            createOrgResponse = pimRemoteService.getOrganisation(authToken, cookie, req).execute();
        } catch (IOException e) {
            String msg = "Error while create organisation in PIM";
            log.error(msg + " for site: " + siteKey + " reason:" + e.getMessage());
            throw new PimWorkflowException(msg);
        }
        if(!createOrgResponse.isSuccessful()) {
            String msg = "Error while create organisation in PIM";
            log.error(msg + " for site: " + siteKey + " for code: "  +  createOrgResponse.code()
                    + " reason: " + createOrgResponse.errorBody().string());
            throw new PimWorkflowException(msg + " for site: " + siteKey + " for code: "  +  createOrgResponse.code()
                    + " reason: " + createOrgResponse.errorBody().string());
        }

        workflowContext.setAppId(appId);
        workflowContext.setCookie(cookie);
        workflowContext.setSiteKey(siteKey);
        workflowContext.setSiteName(siteName);
        workflowContext.setAuthToken(authToken);
        workflowContext.setRegion(region);
        workflowContext.setOrgId(extractOrgId(createOrgResponse.body()));

        executeWorkflow(workflowContext, API_KEY_GEN);
        return workflowContext;
    }

    @Override
    public WorkflowContext triggerWorkflow(WorkflowContext workflowContext, String workflowState) throws IOException, PimWorkflowException {
        executeWorkflow(workflowContext, workflowState);
        return workflowContext;
    }

    private void executeWorkflow(WorkflowContext workflowContext, String workflowState) throws IOException, PimWorkflowException {
        JsonObject templateObject = workflowDao.fetchTemplate(APP_ID_KEY, workflowContext.getAppId());
        if(templateObject == null)
            throw new IllegalArgumentException("No Config set for " + workflowContext.getAppId());

        WorkflowProcessor workflowProcessor = workflowProcessors.get(workflowState);
        if(workflowProcessor == null)
            throw new IllegalArgumentException("No workflow state exists for app " + workflowContext.getAppId());
        workflowContext.setTemplateObject(templateObject);

        while(workflowProcessor != null) {
            try {
                workflowProcessor.processWorkflow(workflowContext);
            } catch(PimWorkflowException e) {
                log.error(e.getMessage() + " for site: " + workflowContext.getSiteKey());
                updateFailed(workflowState, workflowContext, e.getMessage());
                throw e;
            } catch(IOException e) {
                log.error("Exception: Failed with internal network issue for site: "
                        + workflowContext.getSiteKey() + " message: " + e.getMessage());
                // TODO: retry the upload
                updateFailed(workflowState, workflowContext, "Failed with internal network issue");
                throw new PimWorkflowException("Exception for PIM Workflow in " + workflowProcessor.getClass().getName()
                        + " : " + e.getMessage());
            }
            updateSuccess(workflowState, workflowContext);
            workflowState = workflowProcessor.nextWorkflowProcessor();
            workflowProcessor =  workflowProcessors.get(workflowState);
        }
    }


    private void updateSuccess(String workflowState, WorkflowContext workflowContext) {
        workflowDao.saveWorkflowSnapshot(new WorkflowStatus(workflowState, workflowContext.getOrgId(),
                workflowContext.getAppId(), Boolean.TRUE, System.currentTimeMillis(), null));
    }

    private void updateFailed(String workflowState, WorkflowContext workflowContext, String message) {
        workflowDao.saveWorkflowSnapshot(new WorkflowStatus(workflowState, workflowContext.getOrgId(),
                workflowContext.getAppId(), Boolean.FALSE, System.currentTimeMillis(), message));
    }

    private int fetchStatusCode(Response<JsonObject> workflowProcessorResponse) {
        return workflowProcessorResponse.code();
    }

    private boolean isValid(int statusCode) { return statusCode >= 200 && statusCode < 300; }

    private String extractOrgId(JsonObject createOrgResponse) {
        return createOrgResponse.getAsJsonObject(DATA_KEY).getAsJsonObject(DETAILS_KEY).get(ORG_ID_KEY).getAsString();
    }
}
