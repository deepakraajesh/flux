package com.unbxd.skipper.site.DAO;

import com.google.inject.AbstractModule;
import com.unbxd.skipper.site.DAO.impl.MongoDAO;

public class SiteServiceDAOModule extends AbstractModule {
    @Override
    public void configure() {
        bind(SiteDAO.class).to(MongoDAO.class).asEagerSingleton();
    }
}
