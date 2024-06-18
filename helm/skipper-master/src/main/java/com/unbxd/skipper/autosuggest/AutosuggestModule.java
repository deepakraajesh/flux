package com.unbxd.skipper.autosuggest;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.unbxd.config.Config;
import com.unbxd.skipper.feed.DAO.IndexingStatusDAO;
import com.unbxd.skipper.feed.DAO.impl.IndexingStatusDAOImpl;
import com.unbxd.skipper.autosuggest.dao.impl.MongoTemplateDAO;
import com.unbxd.skipper.autosuggest.dao.PpFilterDAO;
import com.unbxd.skipper.autosuggest.dao.impl.PpFilterDAOmongoImpl;
import com.unbxd.skipper.autosuggest.dao.TemplateDAO;
import com.unbxd.skipper.autosuggest.service.*;
import com.unbxd.skipper.feed.service.impl.IndexingServiceImpl;
import com.unbxd.skipper.autosuggest.service.impl.AutosuggestStateServiceImpl;
import com.unbxd.skipper.autosuggest.service.impl.SuggestionServiceImpl;
import com.unbxd.skipper.autosuggest.service.impl.TemplateServiceImpl;
import com.unbxd.skipper.feed.service.IndexingService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class AutosuggestModule extends AbstractModule {
    protected static final String HAGRID_BASE_URL_PROPERTY_NAME = "Hagrid.url";
    @Override
    public void configure() {
        buildTemplateService();
        buildTemplateDAO();
        buildSuggestionService();
        buildPpFilterDAO();
        buildAutosuggestStateService();
    }

    private void buildTemplateService() {
        bind(TemplateService.class).to(TemplateServiceImpl.class).asEagerSingleton();
    }

    private void buildSuggestionService() {
        bind(SuggestionService.class).to(SuggestionServiceImpl.class).asEagerSingleton();
    }

    private void buildTemplateDAO() {
        bind(TemplateDAO.class).to(MongoTemplateDAO.class).asEagerSingleton();
    }

    private void buildPpFilterDAO() {
        bind(PpFilterDAO.class).to(PpFilterDAOmongoImpl.class).asEagerSingleton();
    }

    private void buildAutosuggestStateService() {
        bind(AutosuggestStateService.class).to(AutosuggestStateServiceImpl.class).asEagerSingleton();
    }

    protected String getHagridURL(Config config){
        return config.getProperty(HAGRID_BASE_URL_PROPERTY_NAME);
    }

    @Provides
    private HagridRemoteService getHagridRemoteService(Config config) {
        String hagridBaseUrl =  getHagridURL(config);
        if (hagridBaseUrl == null || hagridBaseUrl.isEmpty()) {
            throw new IllegalArgumentException(HAGRID_BASE_URL_PROPERTY_NAME + " property is not set");
        }
        try {
            new URL(hagridBaseUrl).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(HAGRID_BASE_URL_PROPERTY_NAME + "property set as "
                    + hagridBaseUrl + " reason:" + e.getMessage());
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(58, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(hagridBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client).build();

        return retrofit.create(HagridRemoteService.class);
    }
}
