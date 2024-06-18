package com.unbxd.skipper.plugins;

import com.unbxd.config.Config;

public class PluginTestModule extends PluginModule {

    @Override
    protected String getViperUrl(Config config) {
        return "http://bleh";
    }
}

