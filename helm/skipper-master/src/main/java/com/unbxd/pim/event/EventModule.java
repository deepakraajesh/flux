package com.unbxd.pim.event;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.pim.event.controller.EventController;
import com.unbxd.pim.event.model.EventType;
import com.unbxd.pim.event.processor.PimUploadComplete;
import ro.pippo.controller.Controller;

import static com.unbxd.pim.event.model.EventType.PIM_UPLOAD_COMPLETE;

public class EventModule extends AbstractModule {

    @Override
    public void configure() {
        bindEventProcessor();
        bindEventProcessors();
        bindControllers();
    }

    protected void bindEventProcessor() {
        bind(EventProcessManager.class).to(DefaultEventProcessManager.class);
    }

    protected void bindEventProcessors() {
        MapBinder<EventType, EventProcessor> eventBinder = MapBinder.newMapBinder(binder(), EventType.class, EventProcessor.class);
        eventBinder.addBinding(PIM_UPLOAD_COMPLETE).to(PimUploadComplete.class).asEagerSingleton();
    }

    protected void bindControllers() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(EventController.class).asEagerSingleton();
    }
}