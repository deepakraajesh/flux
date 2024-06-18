package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.CONFIGURE_TEMPLATE;
import static com.unbxd.skipper.states.model.ServeStateType.SELECT_TEMPLATE;

@Log4j2
public class ConfigureTemplate extends VirtualServeState {
    @Override
    public ServeStateType getPrevStateType() {
        return SELECT_TEMPLATE;
    }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(getPrevStateType(), actualPrevState)) {
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + getPrevStateType() +
                    " but got : " + actualPrevState;
            stateContext.setErrors(errMsg);
            stateContext.setCode(400);
            log.error(errMsg);
        }
    }

    @Override
    public ServeStateType getStateType() { return CONFIGURE_TEMPLATE; }

    @Override
    public ServeStateType nextStateType() { return SELECT_TEMPLATE; }

    @Override
    public void run() { }
}
