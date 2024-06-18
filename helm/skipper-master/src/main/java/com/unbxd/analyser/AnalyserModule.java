package com.unbxd.analyser;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.analyser.service.impl.AsterixService;
import com.unbxd.config.Config;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class AnalyserModule  extends AbstractModule {
    public static final String ASTERIX_BASE_URL_PROPERTY_NAME = "asterix.url";

    protected String getAsterixURL(Config config) {
        return config.getProperty(ASTERIX_BASE_URL_PROPERTY_NAME);
    }

    @Singleton
    @Provides
    protected AnalyserService getAnalyserService(Config config) {
        String AnalyserBaseUrl =  getAsterixURL(config);

        if (AnalyserBaseUrl == null || AnalyserBaseUrl.isEmpty()) {
            throw new IllegalArgumentException(ASTERIX_BASE_URL_PROPERTY_NAME + " property is not set");
        }

        try {
            new URL(AnalyserBaseUrl).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(ASTERIX_BASE_URL_PROPERTY_NAME + "property set as "
                    + AnalyserBaseUrl + " reason:" + e.getMessage());
        }

        return new AsterixService(AnalyserBaseUrl);
    }

}
