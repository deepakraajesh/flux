package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class DimensionUpdate extends VirtualServeState {


    @Override
    public ServeStateType getPrevStateType() {
        return DIMENSION_MAPPING_START;
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
            log.error(errMsg);
        }
    }

    @Override
    public ServeStateType getStateType() {
        return DIMENSION_UPDATE;
    }

    @Override
    public ServeStateType nextStateType() {
        return null;
    }

    @Override
    public void run() {

    }
}

