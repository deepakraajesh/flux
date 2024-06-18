package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.impl.PlatformSelectVirtualState.PLATFORM;
import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class PlatformUploadVirtualState extends VirtualServeState {

    private static final ServeStateType[] prevStates = {PLATFORM_SELECT, PLATFORM_UPLOAD_ERROR};

    @Override
    public ServeStateType getPrevStateType() { return PLATFORM_SELECT; }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        log.info("serving " + this.getStateType().name() + " for site: " + stateContext.getSiteKey());
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(prevStates, actualPrevState)) {
            if(stateData != null && stateData.containsKey(PLATFORM))
                stateContext.setFeedPlatform(stateData.get(PLATFORM));
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + getPrevStateType() +
                    " but got : " + actualPrevState;
            stateContext.setErrors(errMsg);
            log.error(errMsg);
        }
    }

    @Override
    public void run() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return PLATFORM_UPLOAD; }

}
