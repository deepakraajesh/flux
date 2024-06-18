package com.unbxd.skipper.dictionary.knowledgeGraph;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.unbxd.skipper.dictionary.knowledgeGraph.service.KnowledgeGraph;
import com.unbxd.skipper.dictionary.knowledgeGraph.service.KnowledgeGraphService;
import com.unbxd.skipper.dictionary.knowledgeGraph.service.KnowledgeGraphServiceImpl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KnowledgeGraphModule extends AbstractModule {
    private static final String KG_BASE_URL_PROPERTY_NAME = "knowledge.graph.url";

    @Override
    protected void configure() {
        bind(KnowledgeGraphService.class).to(KnowledgeGraphServiceImpl.class).asEagerSingleton();
    }

    @Singleton
    @Provides
    protected KnowledgeGraph getKnowledgeGraph(Properties properties) {
        String relevancyBaseURL = properties.getProperty(KG_BASE_URL_PROPERTY_NAME,"http://giraffe.ss");

        try {
            new URL(relevancyBaseURL).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(KG_BASE_URL_PROPERTY_NAME + "property set as "
                    + relevancyBaseURL + " reason:" + e.getMessage());
        }
        OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(relevancyBaseURL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build().create(KnowledgeGraph.class);
    }
}
