package com.unbxd.skipper.feed;


import com.google.inject.AbstractModule;
import com.unbxd.skipper.feed.DAO.IndexingStatusDAO;
import com.unbxd.skipper.feed.DAO.impl.IndexingStatusDAOImpl;
import com.unbxd.skipper.feed.service.FeedService;
import com.unbxd.skipper.feed.service.IndexingService;
import com.unbxd.skipper.feed.service.impl.IndexingServiceImpl;
import com.unbxd.skipper.feed.service.impl.SearchFeedServiceImpl;

public class FeedModule extends AbstractModule {

    @Override
    public void configure() {
        buildFeedService();
        buildIndexingStatusDAO();
        buildIndexingService();
    }

    private void buildFeedService() {
        bind(FeedService.class).to(SearchFeedServiceImpl.class).asEagerSingleton();
    }

    private void buildIndexingService() {
        bind(IndexingService.class).to(IndexingServiceImpl.class).asEagerSingleton();
    }

    private void buildIndexingStatusDAO() {
        bind(IndexingStatusDAO.class).to(IndexingStatusDAOImpl.class).asEagerSingleton();
    }

}

