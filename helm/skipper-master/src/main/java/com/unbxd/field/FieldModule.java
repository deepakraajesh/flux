package com.unbxd.field;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.field.service.FieldService;
import com.unbxd.field.service.GimliRemoteService;
import com.unbxd.field.service.impl.GimliFieldService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class FieldModule extends AbstractModule {
    public static final String GIMLI_BASE_URL_PROPERTY_NAME = "gimli.url";

    @Override
    public void configure() {
        bind(FieldService.class).to(GimliFieldService.class).asEagerSingleton();
    }

    protected String getGimliURL(Config config) {
        return config.getProperty(GIMLI_BASE_URL_PROPERTY_NAME);
    }

    @Provides
    @Singleton
    private GimliRemoteService initializeGimliRemoteService(Config config) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.MINUTES)
                .connectionPool(new ConnectionPool())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(getGimliURL(config))
                .client(client).build();

        return retrofit.create(GimliRemoteService.class);
    }


}
