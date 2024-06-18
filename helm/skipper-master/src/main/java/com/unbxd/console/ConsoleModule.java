package com.unbxd.console;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.console.service.ConsoleRemoteService;
import com.unbxd.console.service.FacetRemoteService;
import com.unbxd.console.service.SiteValidationService;
import com.unbxd.console.service.impl.ConsoleOrchestrationServiceImpl;
import com.unbxd.console.service.impl.SiteValidationServiceImpl;
import com.unbxd.config.Config;
import com.unbxd.okhttp.LoggingInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ConsoleModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SiteValidationService.class).to(SiteValidationServiceImpl.class).asEagerSingleton();
        bind(ConsoleOrchestrationService.class).to(ConsoleOrchestrationServiceImpl.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public FacetRemoteService getFacetRemoteService(Config config) {

        String facetUrl = config.getProperty("console-backend.url");
        if(facetUrl == null) {
            throw new IllegalArgumentException("Expected console-backend.url property to be set before launching the application");
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

        return retrofit.create(FacetRemoteService.class);
    }

    @Provides
    @Singleton
    public ConsoleRemoteService getConsoleRemoteService(Config config) {
        String consoleUrl = config.getProperty("console.url");
        if(consoleUrl == null) {
            throw new IllegalArgumentException("Expected console.url property to be set before launching the application");
        }

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .baseUrl(consoleUrl)
                .build();

        return retrofit.create(ConsoleRemoteService.class);
    }
}
