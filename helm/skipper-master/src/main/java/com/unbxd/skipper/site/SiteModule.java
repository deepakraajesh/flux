package com.unbxd.skipper.site;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.skipper.site.DAO.SiteServiceDAOModule;
import com.unbxd.skipper.controller.SiteMetaController;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.site.service.SiteServiceModule;
import com.unbxd.skipper.site.service.impl.SiteServiceImpl;
import ro.pippo.controller.Controller;

public class SiteModule extends AbstractModule {

    @Override
    public void configure() {
        buildSiteServiceModule();
        buildSiteServiceDAOModule();
        bind(SiteService.class).to(SiteServiceImpl.class).asEagerSingleton();
        bindControllerModule();
        bindMultiRegionRouter();
    }

    private void bindMultiRegionRouter() {
        bind(MultiRegionRouter.class).to(DirectMultiRegionRouter.class);
    }

    private void buildSiteServiceModule() {
        install(new SiteServiceModule());
    }

    private void buildSiteServiceDAOModule() {
        install(new SiteServiceDAOModule());
    }

    private void bindControllerModule() {
        Multibinder<Controller> controllers = Multibinder.newSetBinder(binder(), Controller.class);
        controllers.addBinding().to(SiteMetaController.class).asEagerSingleton();
    }
}