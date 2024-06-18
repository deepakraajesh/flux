package com.unbxd.search.feed;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.MalformedURLException;

public class FeedTestModule extends AbstractModule {

    private static final MockFeedServer mockFeedServer;
    private static final MockMozartServer mockMozartServer;

    static {
        try {
            mockFeedServer = new MockFeedServer();
            mockMozartServer = new MockMozartServer();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    public SearchFeedService getFeedRemoteService() {
        String feedBaseUrl = "http://" + mockFeedServer.getHostName() + ":" + mockFeedServer.getPort();
        if(feedBaseUrl == null) {
            throw new IllegalArgumentException("Expected console.url property to be set before launching the application");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(feedBaseUrl)
                .build();

        return retrofit.create(SearchFeedService.class);
    }

    @Provides
    @Singleton
    public MozartService getMozartService() {
        String feedBaseUrl = "http://" + mockFeedServer.getHostName() + ":" + mockFeedServer.getPort();
        if(feedBaseUrl == null) {
            throw new IllegalArgumentException("Expected console.url property to be set before launching the application");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(feedBaseUrl)
                .build();

        return retrofit.create(MozartService.class);
    }
}

