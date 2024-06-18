package com.unbxd.search;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.search.config.SearchConfigService;
import com.unbxd.search.config.SearchConfigServiceImpl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class SearchModule extends AbstractModule {

    public static final String HAGRID_BASE_URL_PROPERTY_NAME = "Hagrid.url";

    protected String getHagridURL(Config config) {
        return config.getProperty(HAGRID_BASE_URL_PROPERTY_NAME);
    }
    
    @Singleton
    @Provides
    protected SearchConfigService getSearchConfigService(Config config) {
        String HagridBaseUrl =  getHagridURL(config);

        if (HagridBaseUrl == null || HagridBaseUrl.isEmpty()) {
            throw new IllegalArgumentException(HAGRID_BASE_URL_PROPERTY_NAME + " property is not set");
        }

        try {
            new URL(HagridBaseUrl).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(HAGRID_BASE_URL_PROPERTY_NAME + "property set as "
                    + HagridBaseUrl + " reason:" + e.getMessage());
        }

        return new SearchConfigServiceImpl(HagridBaseUrl);
    }
}
