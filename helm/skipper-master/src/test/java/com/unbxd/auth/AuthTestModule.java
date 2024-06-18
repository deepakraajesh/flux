package com.unbxd.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.net.MalformedURLException;

public class AuthTestModule extends AbstractModule {

    private static final MockSSOServer ssoServer;

    static {
        try {
            ssoServer = new MockSSOServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Singleton
    @Provides
    protected Auth getFieldService() {
        String fieldBaseUrl = "http://" + ssoServer.getHostName()
                + ":" + ssoServer.getPort();
        return new SSOAuth(fieldBaseUrl);
    }
}

