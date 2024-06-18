package com.unbxd.skipper.autosuggest;


import com.unbxd.config.Config;

import java.net.MalformedURLException;

public class AutosuggestTestModule extends AutosuggestModule {
    private static final MockHagridServer MOCK_HAGRID_SERVER;

    static {
        try {
            MOCK_HAGRID_SERVER = new MockHagridServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getHagridURL(Config config) {
        return "http://" + MOCK_HAGRID_SERVER.getHostName() + ":" + MOCK_HAGRID_SERVER.getPort();
    }
}