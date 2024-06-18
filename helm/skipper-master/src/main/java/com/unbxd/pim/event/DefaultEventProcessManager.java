package com.unbxd.pim.event;

import com.google.inject.Inject;
import com.unbxd.pim.event.exception.EventException;
import com.unbxd.pim.event.model.Event;
import com.unbxd.pim.event.model.EventType;

import java.util.Map;

public class DefaultEventProcessManager implements EventProcessManager {

    Map<EventType, EventProcessor> eventProcessors;

    @Inject
    public DefaultEventProcessManager(Map<EventType, EventProcessor> eventProcessors) {
        this.eventProcessors = eventProcessors;
    }

    @Override
    public void trigger(Event event) throws EventException {
        eventProcessors.get(event.getName()).process(event);
    }
}

