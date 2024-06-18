package com.unbxd.skipper.model;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.WORKFLOW_ID_KEY;

public interface Constants {

    String EMAIL = "email";
    String EVENT_BUILDER = "eventBuilder";

    String ID_PARAM = "id";
    String TAG_PARAM = "tag";
    String ORG_ID_PARAM = "orgId";
    String APP_ID_PARAM = "appId";
    String REGION_PARAM = "region";
    String SITEKEY_PARAM = "siteKey";
    String LANGUAGE_PARAM = "language";
    String SITENAME_PARAM = "sitename";
    String VERTICAL_PARAM = "vertical" ;
    String AUTH_HEADER = "Authorization";
    String WORKFLOW_STATE = "workflowState";
    String UN_SSO_UID = "_un_sso_uid";

    String[] requestParams = {TAG_PARAM, SITEKEY_PARAM, REGION_PARAM, LANGUAGE_PARAM, SITENAME_PARAM, APP_ID_PARAM,
            ORG_ID_PARAM, WORKFLOW_ID_KEY, VERTICAL_PARAM, WORKFLOW_STATE};
}
