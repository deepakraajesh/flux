package com.unbxd.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;


public class AuthModule extends AbstractModule {

    private static final String SSO_BASE_URL = "ssobase.url";

    @Singleton
    @Provides
    protected Auth getAuth(Config config) {
        String ssoBaseURL = config.getProperty(SSO_BASE_URL);
        if(ssoBaseURL == null) {
            throw new IllegalArgumentException("No ssoBaseURL set in the environmental variable");
        }
        return new SSOAuth(ssoBaseURL);
    }
}

