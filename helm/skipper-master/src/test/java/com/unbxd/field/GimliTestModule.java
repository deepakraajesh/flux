package com.unbxd.field;

import com.unbxd.config.Config;

import java.net.MalformedURLException;

public class GimliTestModule extends FieldModule {

    private static final MockGimliServer mockGimliServer;

    static {
        try {
            mockGimliServer = new MockGimliServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getGimliURL(Config config) {
        return "http://" + mockGimliServer.getHostName() + ":" + mockGimliServer.getPort();
    }
}

