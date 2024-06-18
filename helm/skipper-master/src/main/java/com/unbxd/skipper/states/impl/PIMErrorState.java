package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbxd.skipper.states.model.ServeStateType;
import lombok.extern.log4j.Log4j2;

import static com.unbxd.skipper.states.model.ServeStateType.*;

@Log4j2
public class PIMErrorState extends ErrorVirtualServeState {

    @Override
    public void run() { }

    @Override
    public void nextState() { }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return PIM_ERROR; }

    @Override
    @JsonIgnore
    public ServeStateType getPrevStateType() { return PIM_SELECT; }

    @Override
    public void processState() {
        log.info("Processing PIM Error State.");
    }
}
