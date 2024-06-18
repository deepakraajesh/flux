package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbxd.skipper.relevancy.model.Job;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.statemanager.StateManager;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

@Setter
@NoArgsConstructor
public class CreateSiteState implements ServeState {


    @JsonIgnore
    protected StateManager stateManager;
    @JsonIgnore
    protected StateContext stateContext;
    @JsonIgnore
    protected ArrayDeque<ServeState> stateQueue;

    public CreateSiteState(StateManager stateManager,
                           StateContext stateContext) {
        stateQueue = new ArrayDeque<>();
        this.stateManager = stateManager;
        this.stateContext = stateContext;
    }

    @Override
    public ServeStateType getStateType() {
        return ServeStateType.CREATE_SITE;
    }

    @Override
    public void nextState() {
        ServeStateType nextStateType = nextStateType();
        ServeState nextServeState = stateManager.getStateInstance(nextStateType);

        nextServeState.setStateContext(stateContext);
        nextServeState.setStateManager(stateManager);

        stateManager.transitionStateInThread(stateContext, nextServeState);
    }

    @Override
    public void setStateData(Map<String, String> stateData) { }

    @Override
    public Map<String, String> getStateData() { return null; }

    @Override
    public void processState() { nextState(); }

    @Override
    public void run() {
        processState();
    }

    @Override
    public ServeStateType nextStateType() {
        return ServeStateType.CREATE_SITE_CONSOLE;
    }

    @Override
    public void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobType) {
        // Doesn't do anything
    }
}
