package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class ApiUploadErrorState extends ErrorVirtualServeState {

    @Override
    public void run() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return API_UPLOAD_ERROR; }

    @Override
    @JsonIgnore
    public ServeStateType getPrevStateType() { return API_UPLOAD; }

    @Override
    public void nextState() { stateManager.transitionErrorState(stateContext, this); }

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
}
