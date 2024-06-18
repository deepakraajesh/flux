package com.unbxd.gcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import ro.pippo.controller.Controller;

public class ControllerModule extends AbstractModule {

    @Provides
    @Singleton
    public ObjectMapper getMapper() { return new ObjectMapper(); }

    @Override
    protected void configure() {
        Multibinder<Controller> controllerBinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerBinder.addBinding().to(EntitlementsController.class).asEagerSingleton();
        controllerBinder.addBinding().to(ActivationController.class).asEagerSingleton();
        controllerBinder.addBinding().to(AccountsController.class).asEagerSingleton();
    }
}
