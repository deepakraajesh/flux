package com.unbxd.pim.workflow.service;

import com.google.gson.JsonObject;
import com.unbxd.pim.workflow.exception.PimWorkflowException;
import com.unbxd.pim.workflow.model.WorkflowContext;

import java.io.IOException;

public interface WorkflowProcessor {

    String ORG_ID = "orgId";
    String ORG_KEY = "orgKey";
    String API_KEY = "apiKey";
    String SITEKEY = "siteKey";
    String REGION = "region";
    String APP_ID_KEY = "appId";
    String SITE_NAME = "siteName";
    String CHANNELID = "channelId";
    String CONFIGS_KEY = "configs";
    String SECRET_KEY = "secretKey";
    String IDENTIFIER = "identifier";
    String ORG_APP_ID = "org_app_id";
    String ADAPTER_ID = "adapter_id";
    String CHANNEL_ID = "channel_id";
    String UN_SSO_UID = "_un_sso_uid";
    String EXPORT_TYPE = "export_type";
    String APP_CUSTOM_ID = "appCustomId";
    String WORKFLOW_ID_KEY = "workflowId";
    String CHANNEL_EXPORT = "CHANNEL_EXPORT";
    String WORKFLOW_IDS_KEY = "workflow_ids";
    String CHECK_READINESS = "check_readiness";
    String JOB_ACTION_IDENTIFIER = "job_action_identifier";
    String SCHEDULED_CHANNEL_EXPORT = "SCHEDULED_CHANNEL_EXPORT";

    String ADD_NODE = "addNode";
    String EXPORT_TO_NETWORK = "exportToNetwork";
    String API_KEY_GEN = "apiKeyGen";
    String ADD_PROPERTIES = "addProperties";
    String START_WORKFLOW = "startWorkflow";
    String CREATE_WORKFLOW = "createWorkflow";
    String UPDATE_WORKFLOW = "updateWorkflow";
    String PIM_REGISTRATION = "pimRegistration";
    String COMPLETE_WORKFLOW = "workflowComplete";
    String SEARCH_REGISTRATION = "searchRegistration";

    String nextWorkflowProcessor();

    void processWorkflow(WorkflowContext workflowContext) throws IOException, PimWorkflowException;

    default JsonObject fetchConfigObject(JsonObject templateObject, String templateName) {
        return templateObject.getAsJsonObject(templateName);
    }
}
