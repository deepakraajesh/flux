package com.unbxd.skipper.plugins;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.config.Config;
import com.unbxd.field.service.FieldService;
import ro.pippo.controller.Controller;

public class PluginModule extends AbstractModule {

    public static final String VIPER_BASE_URL_PROPERTY_NAME = "viper.url";

    @Override
    public void configure() {
        bindController();
    }

    private void bindController() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(PluginController.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    private Plugin geViperPluginService(Config config, FieldService fieldService) {
        String pluginUrl = getViperUrl(config);
        if(pluginUrl == null) {
            throw new IllegalArgumentException("Expected " + VIPER_BASE_URL_PROPERTY_NAME +
                    " property to be set before launching the application");
        }
        return new ViperPlugin(pluginUrl, fieldService);
    }

    protected String getViperUrl(Config config) {
        return config.getProperty(VIPER_BASE_URL_PROPERTY_NAME);
    }
}

