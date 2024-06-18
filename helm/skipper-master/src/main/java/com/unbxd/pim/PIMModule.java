package com.unbxd.pim;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.unbxd.config.Config;
import com.unbxd.pim.channel.SearchPIMChannelAdapter;
import com.unbxd.pim.channel.PIMChannelAdapter;
import com.unbxd.pim.channel.PIMRemoteChannelService;
import com.unbxd.pim.imports.DefaultPIMImportAdapter;
import com.unbxd.pim.imports.PIMImportAdapter;
import com.unbxd.pim.imports.PIMRemoteImportService;
import com.unbxd.pim.workflow.dao.MongoWorkflowDao;
import com.unbxd.pim.workflow.dao.WorkflowDao;
import com.unbxd.pim.workflow.service.*;
import com.unbxd.pim.workflow.service.impl.*;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.*;


public class PIMModule extends AbstractModule {

    @Override
    public void configure() {
        bindImport();
        bindExport();
        bindWorkflow();
        bindWorkflowProcessors();
        bindPIMService();
        bind(PIMOrchestrationService.class).to(PIMOrchestrationServiceImpl.class).asEagerSingleton();
    }

    private void bindPIMService() {
        bind(PIMService.class).to(PimServiceImpl.class);
    }

    private void bindExport() {
        bind(PIMChannelAdapter.class).to(SearchPIMChannelAdapter.class).asEagerSingleton();
    }

    protected void bindImport() {
        bind(PIMImportAdapter.class).to(DefaultPIMImportAdapter.class).asEagerSingleton();
    }

    public void bindWorkflowProcessors() {
        MapBinder<String, WorkflowProcessor> workflowProcessorBinder = MapBinder
                .newMapBinder(binder(), String.class, WorkflowProcessor.class);

        workflowProcessorBinder.addBinding(ADD_NODE).to(ExportToNetwork.class).asEagerSingleton();
        workflowProcessorBinder.addBinding(API_KEY_GEN).to(KeyGenProcessor.class).asEagerSingleton();
        workflowProcessorBinder.addBinding(START_WORKFLOW).to(StartWorkflowProcessor.class).asEagerSingleton();
        workflowProcessorBinder.addBinding(CREATE_WORKFLOW).to(CreateWorkflowProcessor.class).asEagerSingleton();
        workflowProcessorBinder.addBinding(PIM_REGISTRATION).to(PIMRegistrationProcessor.class).asEagerSingleton();
        workflowProcessorBinder.addBinding(SEARCH_REGISTRATION).to(SearchRegistrationProcessor.class).asEagerSingleton();
    }

    public void bindWorkflow() {
        bind(WorkflowDao.class).to(MongoWorkflowDao.class).asEagerSingleton();
    }

    public String getPIMSearchAppDomain(Config config) {
        String appRegistrationBaseURL = config.getProperty("pim-searchapp.url");
        if(appRegistrationBaseURL == null) {
            throw new IllegalArgumentException("Expected pim-searchapp.url " +
                    "property to be set before launching the application");
        }
        return appRegistrationBaseURL;
    }

    @Provides
    @Singleton
    public PIMRemoteImportService getImportService(Config config) {
        String appRegistrationBaseURL = getPIMInternalDomain(config);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(58, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(appRegistrationBaseURL)
                .client(okHttpClient)
                .build();

        return retrofit.create(PIMRemoteImportService.class);
    }

    @Provides
    @Singleton
    public PIMRemoteChannelService getChannelService(Config config) {
        String appRegistrationBaseURL = getPIMInternalDomain(config);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(58, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(appRegistrationBaseURL)
                .client(okHttpClient)
                .build();

        return retrofit.create(PIMRemoteChannelService.class);
    }

    @Provides
    @Singleton
    public PimSearchApp getAppRegistrationRemoteService(Config config) {
        String appRegistrationBaseURL = getPIMSearchAppDomain(config);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(58, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(appRegistrationBaseURL)
                .client(okHttpClient)
                .build();

        return retrofit.create(PimSearchApp.class);
    }

    public String getPIMRemoteServiceDomain(Config config) {
        String pimBaseURL = config.getProperty("pim.url");
        if(pimBaseURL == null) {
            throw new IllegalArgumentException("Expected pim.url property to be set before launching the application");
        }
        return pimBaseURL;
    }
    public String getPIMInternalDomain(Config config) {
        String pimBaseURL = config.getProperty("pim.internal.url");
        if(pimBaseURL == null) {
            throw new IllegalArgumentException("Expected pim.internal.url property to be set before launching the " +
                    "application");
        }
        return pimBaseURL;
    }

    @Provides
    @Singleton
    public PIMRemoteService getPIMRemoteService(Config config) {
        String pimBaseURL = getPIMRemoteServiceDomain(config);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(58, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .baseUrl(pimBaseURL)
                .build();

        return retrofit.create(PIMRemoteService.class);
    }
}
