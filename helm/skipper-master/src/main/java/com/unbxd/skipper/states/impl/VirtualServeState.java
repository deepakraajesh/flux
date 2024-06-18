package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import com.unbxd.skipper.relevancy.model.Job;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;

import java.util.List;
import java.util.Map;

public abstract class VirtualServeState implements ServeState {

    @Inject
    protected StateManager stateManager;

    protected StateContext stateContext;

    protected Map<String, String> stateData;

    @JsonIgnore
    public abstract ServeStateType getPrevStateType();

    @Override
    public Map<String, String> getStateData() { return stateData; }

    @Override
    public void setStateData(Map<String, String> stateData) {
        this.stateData = stateData;
    }

    protected boolean validatePrevState(ServeStateType prevState, ServeStateType actualPrevState) {
        return prevState == actualPrevState || getStateType() == actualPrevState;
    }

    protected boolean validatePrevState(ServeStateType[] prevStates, ServeStateType actualPrevState) {
        for(ServeStateType prevState: prevStates) {
            if(actualPrevState == prevState) {
                return true;
            }
        }
        return getStateType() == actualPrevState;
    }

    /** auxiliary methods */

    public void setStateManager(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void setStateContext(StateContext stateContext) {
        this.stateContext = stateContext;
    }

    @Override
    public void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobType) {
        // Doesn't do anything
    }
}
