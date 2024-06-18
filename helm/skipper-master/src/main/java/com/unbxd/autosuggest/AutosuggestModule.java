package com.unbxd.autosuggest;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class AutosuggestModule extends AbstractModule {
    public static final String GIMLI_BASE_URL_PROPERTY_NAME = "gimli.url";
    public static final String FEED_BASE_URL_PROPERTY_NAME = "mozart.url";

    @Singleton
    @Provides
    protected AutosuggestService getAutosuggestService(Config config) {
        String fieldBaseUrl = getGimliBaseUrl(config);

        if (fieldBaseUrl == null || fieldBaseUrl.isEmpty()) {
            throw new IllegalArgumentException(GIMLI_BASE_URL_PROPERTY_NAME + " property is not set");
        }

        try {
            new URL(fieldBaseUrl).toURI();
        } catch (MalformedURLException |URISyntaxException e) {
            throw new IllegalArgumentException(GIMLI_BASE_URL_PROPERTY_NAME + "property set as "
                    + fieldBaseUrl + " reason:" + e.getMessage());
        }

        String autosuggestBaseUrl = getFeedBaseUrl(config);
        if (autosuggestBaseUrl == null || autosuggestBaseUrl.isEmpty()) {
            throw new IllegalArgumentException(FEED_BASE_URL_PROPERTY_NAME + " property is not set");
        }

        try {
            new URL(autosuggestBaseUrl).toURI();
        } catch (MalformedURLException |URISyntaxException e) {
            throw new IllegalArgumentException(FEED_BASE_URL_PROPERTY_NAME + "property set as "
                    + fieldBaseUrl + " reason:" + e.getMessage());
        }

        return new GimliAutosuggestService(fieldBaseUrl, autosuggestBaseUrl);
    }

    protected String getGimliBaseUrl(Config config) {
        return config.getProperty(GIMLI_BASE_URL_PROPERTY_NAME);
    }

    protected String getFeedBaseUrl(Config config) {
        return config.getProperty(FEED_BASE_URL_PROPERTY_NAME);
    }
}
