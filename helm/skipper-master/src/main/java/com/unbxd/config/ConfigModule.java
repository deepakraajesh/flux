package com.unbxd.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.config.controller.HealthCheckController;
import com.unbxd.config.service.HealthCheckRemoteService;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ro.pippo.controller.Controller;

import java.util.concurrent.TimeUnit;

public class ConfigModule extends AbstractModule {

    @Override
    protected void configure() {
        bindControllerModule();
    }

    private void bindControllerModule() {
        Multibinder<Controller> controllers = Multibinder.newSetBinder(binder(), Controller.class);
        controllers.addBinding().to(HealthCheckController.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public HealthCheckRemoteService getHealthCheckRemoteService() {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(58, TimeUnit.SECONDS)
                .followRedirects(false)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl("http://127.0.0.1")
                .client(okHttpClient)
                .build();

        return retrofit.create(HealthCheckRemoteService.class);
    }

}
