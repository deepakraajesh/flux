package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.CONSOLE_ERROR;
import static com.unbxd.skipper.states.model.ServeStateType.CREATE_SITE_CONSOLE;

@Log4j2
public class ConsoleErrorState extends ErrorVirtualServeState {

    @Override
    public void run() { }

    @Override
    public void nextState() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return CONSOLE_ERROR; }

    @Override
    @JsonIgnore
    public ServeStateType getPrevStateType() { return CREATE_SITE_CONSOLE; }

    @Override
    public void processState() {
        log.info("Processing ConsoleErrorState");
    }
}
