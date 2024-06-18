package com.unbxd.autosuggest;

import com.unbxd.config.Config;
import com.unbxd.field.MockGimliServer;

import java.net.MalformedURLException;

public class AutosuggestTestModule extends AutosuggestModule{
    private static final MockGimliServer mockedGimliServer;

    static {
        try {
            mockedGimliServer = new MockGimliServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getGimliBaseUrl(Config config) {
        return "http://" + mockedGimliServer.getHostName() + ":" + mockedGimliServer.getPort();
    }

    @Override
    public String getFeedBaseUrl(Config config) {
        return "http://" + mockedGimliServer.getHostName() + ":" + mockedGimliServer.getPort();
    }
}
