package com.unbxd.skipper.controller;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import ro.pippo.controller.Controller;

public class ControllerModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(SiteConfigController.class).asEagerSingleton();
        controllerMultibinder.addBinding().to(SiteController.class).asEagerSingleton();
    }
}
