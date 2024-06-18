package com.unbxd.skipper.relevancy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.unbxd.config.Config;
import com.unbxd.okhttp.LoggingInterceptor;
import com.unbxd.skipper.relevancy.controller.RelevancyController;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.dao.RelevancyDaoImpl;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.relevancy.service.RelevancyService;
import com.unbxd.skipper.relevancy.service.output.RelevancyServiceImpl;
import com.unbxd.skipper.relevancy.service.output.update.*;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ro.pippo.controller.Controller;

import java.util.concurrent.TimeUnit;

@Log4j2
public class RelevancyModule extends AbstractModule {

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
        updateProcessors.addBinding(JobType.suggestedSearchableFields).
                to(SearchableFieldStatsUpdateProcessor.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.noStemWords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.mandatoryTerms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.multiwords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.synonyms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.facets).to(FacetUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.autosuggest).to(AutosuggestConfigUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.dimensionMap).to(DimensionMappingUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.variants).to(VariantUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.suggestedSynonyms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.suggestedMandatoryTerms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.suggestedMultiwords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.suggestedNoStemWords).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.enrichSynonyms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.enrichSuggestedSynonyms).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.recommendPhrases).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.recommendConcepts).to(DictionaryUpdate.class).asEagerSingleton();
        updateProcessors.addBinding(JobType.recommendSynonyms).to(DictionaryUpdate.class).asEagerSingleton();
    }

    protected void bindControllers() {
        Multibinder<Controller> controllerMultibinder = Multibinder.newSetBinder(binder(), Controller.class);
        controllerMultibinder.addBinding().to(RelevancyController.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public RelevancyRemoteService getRelevancyRemoteService(Config config) {
        String relevancyBaseURL = getRelevancyRemoteServiceDomain(config);

        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(58, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Response response = chain.proceed(request);

                    int tryCount = 0;
                    while (!response.isSuccessful() && tryCount < 3) {
                        log.error("[Interceptor]" + " Request is not successful - tryCount: " + tryCount);
                        tryCount++;
                        // retry the request
                        response.close();
                        response = chain.proceed(request);
                    }
                    // otherwise just pass the original response on
                    return response;
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(relevancyBaseURL)
                .client(okHttpClient)
                .build();

        return retrofit.create(RelevancyRemoteService.class);
    }

    public String getRelevancyRemoteServiceDomain(Config config) {
        String relevancyBaseURL = config.getProperty("relevancy.url");
        if(relevancyBaseURL == null) {
            throw new IllegalArgumentException("Expected relevancy.url property to be set before launching the application");
        }
        return relevancyBaseURL;
    }
}
