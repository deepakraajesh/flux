package com.unbxd.search.feed;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.okhttp.LoggingInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

public class SearchFeedModule extends AbstractModule {

    @Provides
    @Singleton
    public SearchFeedService getFeedsService(Config config) {
        String consoleUrl = config.getProperty("feed.url");
        if(consoleUrl == null) {
            throw new IllegalArgumentException("Expected feed.url property to be set before launching the application");
        }

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .readTimeout(58, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(consoleUrl)
                .client(okHttpClient)
                .build();

        return retrofit.create(SearchFeedService.class);
    }

    @Provides
    @Singleton
    public MozartService getMozartService(Config config) {
        String consoleUrl = config.getProperty("mozart.url");
        if(consoleUrl == null) {
            throw new IllegalArgumentException("Expected mozart.url property to be set before launching the application");
        }

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(58, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(consoleUrl)
                .client(okHttpClient)
                .build();

        return retrofit.create(MozartService.class);
    }
}

