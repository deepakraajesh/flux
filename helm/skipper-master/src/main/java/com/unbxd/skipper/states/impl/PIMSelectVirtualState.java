package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMOrchestrationService;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.variants.service.VariantConfigService;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.Map;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static com.unbxd.skipper.states.model.ServeStateType.PIM_SELECT;

@Log4j2
public class PIMSelectVirtualState extends FeedPlatformVirtualState {

    public static final String PLATFORM = "platform";
    @Inject
    @JsonIgnore
    public PIMOrchestrationService pimOrchestrationService;

    @Inject
    @JsonIgnore
    public VariantConfigService variantConfigService;

    @Override
    public ServeStateType getPrevStateType() { return ServeStateType.SITE_CREATED; }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        try {
            ServeStateType actualPrevState = stateContext.getServeState().getStateType();
            if(!validatePrevState(prevStatesForPlugin, actualPrevState)) {
                String errMsg = "Previous State Mismatch, expected: " + Arrays.toString(prevStatesForPlugin) +
                        " but got : " + actualPrevState;
                stateContext.setErrors(errMsg);
                return;
            }
            String cookie = getSSOCookie(stateContext.getCookie());
            if(this.stateContext.getOrgId() == null) {
                WorkflowContext workflowContext = pimOrchestrationService.triggerWorkflow(stateContext.getAppId(),
                        stateContext.getAuthToken(), cookie, stateContext.getSiteName(),
                        stateContext.getSiteKey(), stateContext.getRegion(), stateContext.getEmail());
                stateContext.setChannelId(workflowContext.getChannelId());
                stateContext.setAdapterId(workflowContext.getAdapterId());
                stateContext.setOrgId(workflowContext.getOrgId());
                stateContext.setCode(200);
                stateContext.setFeedPlatform(stateData.get(PLATFORM));
            }
            if(stateContext.getVariantsEnabled()) {
                variantConfigService.setVariantsInPim(stateContext.getSiteKey(), Boolean.TRUE);
            }
            nextState();

        } catch (Exception e) {
            String msg = "Error while updating PIM_SELECT state, reason " + e.getMessage();
            log.error("Error while creating site in PIM: for site:"
                    + stateContext.getSiteKey() + " reason ", e);
            stateContext.setCode(500);
            stateContext.setErrors(e.getMessage());
        }

    }

    private String getSSOCookie(Map<String, String> cookieMap) {
        return UN_SSO_UID + "=" + cookieMap.get(UN_SSO_UID);
    }

    @Override
    public void run() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return PIM_SELECT; }

}
