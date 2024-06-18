package com.unbxd.search;

import com.unbxd.config.Config;
import com.unbxd.skipper.autosuggest.MockHagridServer;

import java.net.MalformedURLException;

public class SearchTestModule extends SearchModule {
    private static final com.unbxd.skipper.autosuggest.MockHagridServer MOCK_HAGRID_SERVER;

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
