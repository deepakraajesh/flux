package com.unbxd.pim.event;

import com.unbxd.pim.event.exception.EventException;
import com.unbxd.pim.event.model.Event;

public interface EventProcessManager {
    void trigger(Event event) throws EventException;
}
