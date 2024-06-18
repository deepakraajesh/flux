package com.unbxd.analyser;

import com.unbxd.config.Config;

import java.net.MalformedURLException;

public class AnalyserTestModule extends AnalyserModule {
    private static final MockAsterixServer mockedAsterixServer;

    static {
        try {
            mockedAsterixServer = new MockAsterixServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAsterixURL(Config config) {
        return "http://" + mockedAsterixServer.getHostName() + ":" + mockedAsterixServer.getPort();
    }
}
