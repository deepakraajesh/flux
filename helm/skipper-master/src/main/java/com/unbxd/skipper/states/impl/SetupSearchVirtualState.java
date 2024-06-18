package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class SetupSearchVirtualState extends VirtualServeState {

    private static final ServeStateType[] prevStates = {DIMENSION_UPDATE, PIM_UPLOAD_COMPLETE};

    @Override
    public ServeStateType getPrevStateType() { return ServeStateType.PIM_UPLOAD_COMPLETE; }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(prevStates, actualPrevState)) {
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + Arrays.toString(prevStates) +
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
    public ServeStateType getStateType() { return SETUP_SEARCH; }
}
