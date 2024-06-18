package com.unbxd.skipper.search;


import com.unbxd.config.Config;

import java.net.MalformedURLException;

public class SearchTestModule extends SearchModule{
    private static final MockSearchServer mockSearchServer;

    static {
        try {
            mockSearchServer = new MockSearchServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getSearchURL(Config config) {
        return "http://" + mockSearchServer.getHostName() + ":" + mockSearchServer.getPort();
    }
}
