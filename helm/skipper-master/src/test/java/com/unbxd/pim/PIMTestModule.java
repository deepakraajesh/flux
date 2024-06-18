package com.unbxd.pim;

import com.unbxd.config.Config;

import java.net.MalformedURLException;

public class PIMTestModule extends PIMModule {

    private static final MockPIMerver pimServer;

    static {
        try {
            pimServer = new MockPIMerver();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPIMRemoteServiceDomain(Config config) {
        return  "http://" + pimServer.getHostName() + ":" + pimServer.getPort();
    }

    @Override
    public String getPIMSearchAppDomain(Config config) {
        return  "http://" + pimServer.getHostName() + ":" + pimServer.getPort();
    }

    @Override
    public String getPIMInternalDomain(Config config) {
        return  "http://" + pimServer.getHostName() + ":" + pimServer.getPort();
    }
}

