package com.unbxd.skipper.states.impl;

import com.unbxd.skipper.states.ServeState;

import java.util.Map;

public abstract class RelevancyState implements ServeState {

    protected Map<String, String> stateData;

    @Override
    public Map<String, String> getStateData() {
        return stateData;
    }

    @Override
    public void setStateData(Map<String, String> stateData) {
        this.stateData = stateData;
    }
}

