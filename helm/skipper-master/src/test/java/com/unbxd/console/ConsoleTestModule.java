package com.unbxd.console;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.console.service.ConsoleRemoteService;
import com.unbxd.console.service.FacetRemoteService;
import com.unbxd.console.service.SiteValidationService;
import com.unbxd.console.service.impl.ConsoleOrchestrationServiceImpl;
import com.unbxd.console.service.impl.SiteValidationServiceImpl;
import com.unbxd.search.SearchRemoteService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ro.pippo.core.Application;

import java.net.MalformedURLException;

import static com.unbxd.console.service.FacetRemoteService.FACET_AUTH_KEY;

public class ConsoleTestModule extends AbstractModule {

    private static final MockConsoleServer consoleServer;

    static {
        try {
            consoleServer = new MockConsoleServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configure() {
        bind(SiteValidationService.class).to(SiteValidationServiceImpl.class).asEagerSingleton();
        bind(ConsoleOrchestrationService.class).to(ConsoleOrchestrationServiceImpl.class).asEagerSingleton();
    }

    @Provides
    @Named(FACET_AUTH_KEY)
    public String getFacetAuth() {
        return "Basic cmlzaGkuamFpbkB1bmJ4ZC5jb206MjB1bjh4ZDEw";
    }

    @Provides
    @Singleton
    public FacetRemoteService getFacetRemoteService() {
        String facetUrl = "http://" + consoleServer.getHostName() + ":" + consoleServer.getPort();
        if(facetUrl == null) {
            throw new IllegalArgumentException("Expected console-backend.url property to be set before launching the application");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(facetUrl)
                .build();

        return retrofit.create(FacetRemoteService.class);
    }

    @Provides
    @Singleton
    public ConsoleRemoteService getConsoleRemoteService() {
        String consoleBaseUrl = "http://" + consoleServer.getHostName() + ":" + consoleServer.getPort();
        if(consoleBaseUrl == null) {
            throw new IllegalArgumentException("Expected console.url property to be set before launching the application");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(consoleBaseUrl)
                .build();

        return retrofit.create(ConsoleRemoteService.class);
    }
}

