package com.unbxd.event.controller;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import ro.pippo.controller.Controller;

public class ControllerModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<Controller> controllerBinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerBinder.addBinding().to(ReportController.class).asEagerSingleton();
    }
}
