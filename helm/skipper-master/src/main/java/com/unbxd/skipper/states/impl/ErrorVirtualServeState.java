package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbxd.skipper.states.model.ServeStateType;

public abstract class ErrorVirtualServeState extends VirtualServeState {

    @JsonIgnore
    public abstract ServeStateType getPrevStateType();

    public void sendNotification() { }
}
