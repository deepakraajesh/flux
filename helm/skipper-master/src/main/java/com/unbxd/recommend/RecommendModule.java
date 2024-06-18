package com.unbxd.recommend;

import com.google.inject.AbstractModule;
import com.unbxd.recommend.controller.ControllerModule;
import com.unbxd.recommend.dao.DaoModule;


public class RecommendModule extends AbstractModule {

    @Override
    public void configure() {
        install(new DaoModule());
        install(new ControllerModule());
    }
}
