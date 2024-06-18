package com.unbxd.skipper.search;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.search.SearchRemoteService;
import com.unbxd.skipper.search.service.AutosuggestService;
import com.unbxd.skipper.search.service.FacetStatService;
import com.unbxd.skipper.search.service.impl.AutosuggestServiceImpl;
import com.unbxd.skipper.search.service.impl.FacetStatServiceImpl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.concurrent.TimeUnit;

public class SearchModule extends AbstractModule {

    public static final String SEARCH_BASE_URL_PROPERTY_NAME = "search.url";

    @Override
    public void configure() {
        buildFacetStatService();
        buildAutosuggestService();
    }

    private void buildFacetStatService() {
        bind(FacetStatService.class).to(FacetStatServiceImpl.class).asEagerSingleton();
    }

    private void buildAutosuggestService() {
        bind(AutosuggestService.class).to(AutosuggestServiceImpl.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public SearchRemoteService getSearchRemoteService(Config config) {
        String searchUrl = getSearchURL(config);
        if(searchUrl == null) {
            throw new IllegalArgumentException(SEARCH_BASE_URL_PROPERTY_NAME+
                    " property to be set before launching the application");
        }

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(58, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient)
                .baseUrl(searchUrl)
                .build();

        return retrofit.create(SearchRemoteService.class);
    }

    protected String getSearchURL(Config config) {
        return config.getProperty(SEARCH_BASE_URL_PROPERTY_NAME);
    }
}
