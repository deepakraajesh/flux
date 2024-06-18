package com.unbxd.skipper.states.impl;

import com.google.inject.Inject;
import com.unbxd.pim.event.EventProcessManager;
import com.unbxd.skipper.states.model.ServeStateType;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.PIM_UPLOAD_COMPLETE;

@Log4j2
@NoArgsConstructor
public class PIMUploadCompleteVirtualState extends VirtualServeState {

    private EventProcessManager eventProcessor;
    private static final String UNO_SSO_ID = "_un_sso_uid";

    @Inject
    public PIMUploadCompleteVirtualState(EventProcessManager eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public ServeStateType getPrevStateType() { return ServeStateType.PIM_UPLOAD_START; }

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
    public void run() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return PIM_UPLOAD_COMPLETE; }
}
