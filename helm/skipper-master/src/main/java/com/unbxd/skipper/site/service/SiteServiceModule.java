package com.unbxd.skipper.site.service;

import com.google.inject.AbstractModule;
import com.unbxd.skipper.site.service.impl.SiteServiceImpl;

public class SiteServiceModule extends AbstractModule {
    @Override
    public void configure() {
        bind(SiteService.class).to(SiteServiceImpl.class).asEagerSingleton();
    }
}
