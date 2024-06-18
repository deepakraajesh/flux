package com.unbxd.gcp;

import com.google.inject.AbstractModule;
import com.unbxd.gcp.controller.ControllerModule;
import com.unbxd.gcp.dao.DaoModule;

public class GCPModule extends AbstractModule {

    @Override
    public void configure() {
        install(new DaoModule());
        install(new ControllerModule());
    }
}
