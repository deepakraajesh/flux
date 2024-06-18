package com.unbxd.report;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.console.MockConsoleServer;
import com.unbxd.event.service.ToucanRemoteService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.MalformedURLException;

public class ReportTestModule extends AbstractModule {

    private static final MockConsoleServer reportServer;

    static {
        try {
            reportServer = new MockConsoleServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    public ToucanRemoteService getToucanRemoteService() {
        String consumerUrl = "http://" + reportServer.getHostName()
                + ":" + reportServer.getPort();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(consumerUrl)
                .build();

        return retrofit.create(ToucanRemoteService.class);
    }
}
