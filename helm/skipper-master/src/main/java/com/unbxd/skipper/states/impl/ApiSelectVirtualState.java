package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class ApiSelectVirtualState extends FeedPlatformVirtualState {

    @Override
    public ServeStateType getPrevStateType() { return ServeStateType.SITE_CREATED; }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(prevStatesForPlugin, actualPrevState)) {
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + Arrays.toString(prevStatesForPlugin) +
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
    public ServeStateType getStateType() { return API_SELECT; }

}
