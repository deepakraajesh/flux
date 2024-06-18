package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class PlatformUploadCompleteVirtualState extends VirtualServeState {

    ServeStateType[] prevStateTypes = {PLATFORM_UPLOAD, CREATE_SITE};
    @Override
    public ServeStateType getPrevStateType() { return ServeStateType.PLATFORM_UPLOAD; }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(prevStateTypes, actualPrevState)) {
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
    public ServeStateType getStateType() { return PLATFORM_UPLOAD_COMPLETE; }


}
