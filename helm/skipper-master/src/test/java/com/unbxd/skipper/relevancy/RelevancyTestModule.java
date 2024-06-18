package com.unbxd.skipper.relevancy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.skipper.relevancy.controller.RelevancyController;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.dao.RelevancyDaoImpl;
import com.unbxd.skipper.relevancy.model.AutosuggestConfig;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.service.RelevancyOutputProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.relevancy.service.RelevancyService;
import com.unbxd.skipper.relevancy.service.output.RelevancyServiceImpl;
import com.unbxd.skipper.relevancy.service.output.update.AutosuggestConfigUpdate;
import com.unbxd.skipper.relevancy.service.output.update.DictionaryUpdate;
import com.unbxd.skipper.relevancy.service.output.update.SearchableFieldUpdateProcessor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ro.pippo.controller.Controller;

public class RelevancyTestModule extends AbstractModule {

    private static final MockRelevancyServer relevancyServer = new MockRelevancyServer();

    @Override
    public void configure() {
        bind(RelevancyDao.class).to(RelevancyDaoImpl.class).asEagerSingleton();
        bind(RelevancyService.class).to(RelevancyServiceImpl.class).asEagerSingleton();

        bindControllers();
        bindRelevancyWorkflowUpdateProcessors();
    }

    protected void bindRelevancyWorkflowUpdateProcessors() {
        MapBinder<JobType, RelevancyOutputUpdateProcessor> updateProcessors = MapBinder
                .newMapBinder(binder(), JobType.class, RelevancyOutputUpdateProcessor.class);
        updateProcessors.addBinding(JobType.searchableFields).
                to(SearchableFieldUpdateProcessor.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.noStemWords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.mandatoryTerms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.multiwords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.synonyms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.autosuggest).to(AutosuggestConfigUpdate.class).asEagerSingleton();
    }

    protected void bindControllers() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(RelevancyController.class).asEagerSingleton();
    }
    
    @Provides
    @Singleton
    public RelevancyRemoteService getRelevancyRemoteService() {
        String relevancyRemoteUrl = "http://" + relevancyServer.getHost() + ":" + relevancyServer.getPort();
        if(relevancyRemoteUrl == null) {
            throw new IllegalArgumentException("Expected relevancy.url property to be set before launching the application");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(relevancyRemoteUrl)
                .build();

        return retrofit.create(RelevancyRemoteService.class);
    }
}
