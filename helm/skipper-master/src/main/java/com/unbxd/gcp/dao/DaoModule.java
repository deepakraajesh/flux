package com.unbxd.gcp.dao;

import com.google.inject.AbstractModule;

public class DaoModule extends AbstractModule {

    @Override
    public void configure() {
        bind(ProcurementDao.class).to(GCPProcurementDao.class).asEagerSingleton();
    }
}
