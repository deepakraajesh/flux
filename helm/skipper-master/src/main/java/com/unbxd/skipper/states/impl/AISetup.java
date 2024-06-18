package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

import static com.unbxd.skipper.states.model.ServeStateType.*;


@Log4j2
public class AISetup extends VirtualServeState {

    private static final ServeStateType[] previousStates = {SETUP_SEARCH, MANUAL_SETUP, AI_SETUP};

    @Override
    public ServeStateType getPrevStateType() {
        return ServeStateType.SETUP_SEARCH;
    }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        if(validatePrevState(previousStates, actualPrevState)) {
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + Arrays.toString(previousStates) +
                    " but got : " + actualPrevState;
            stateContext.setErrors(errMsg);
            log.error(errMsg);
        }
    }

    @Override
    public ServeStateType getStateType() {
        return ServeStateType.AI_SETUP;
    }

    @Override
    public ServeStateType nextStateType() {
        return null;
    }

    @Override
    public void run() {

    }
}

