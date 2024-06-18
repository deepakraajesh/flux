package com.unbxd.skipper.feed.dim;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.skipper.feed.dim.dao.DimensionMapSuggestion;
import com.unbxd.skipper.feed.dim.dao.MongoDimensionMapSuggestion;
import ro.pippo.controller.Controller;

public class DIMModule extends AbstractModule {


    @Override
    public void configure() {

        /**
         * DAO's -> Service -> Controller
         */
        bindDao();
        bindService();
        bindController();
    }

    private void bindController() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(DimensionMapController.class).asEagerSingleton();
    }

    private void bindService() {
        bind(DimensionMappingService.class).to(DefaultDimensionMappingServiceImpl.class).asEagerSingleton();
    }

    private void bindDao() {
        bind(DimensionMapSuggestion.class).to(MongoDimensionMapSuggestion.class).asEagerSingleton();
    }
}

