package com.unbxd.event;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.event.controller.ControllerModule;
import com.unbxd.event.service.ToucanRemoteService;
import com.unbxd.okhttp.LoggingInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ReportModule extends AbstractModule {

    private static final String TOUCAN_URL = "consumer.url";

    @Override
    public void configure() { install(new ControllerModule()); }

    @Provides
    @Singleton
    public ToucanRemoteService getToucanRemoteService(Config config) {
        String facetUrl = config.getProperty(TOUCAN_URL);
        if(facetUrl == null) {
            throw new IllegalArgumentException("Expected consumer.url " +
                    "property to be set before launching the application");
        }

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(58, TimeUnit.SECONDS)
                .addInterceptor(new LoggingInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .client(okHttpClient)
                .baseUrl(facetUrl)
                .build();

        return retrofit.create(ToucanRemoteService.class);
    }
}
