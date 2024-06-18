package com.unbxd.skipper.feed.service;

import com.unbxd.skipper.feed.exception.FeedException;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;

public interface FeedService {

    FeedIndexingStatus status(String siteKey, String feedId, Integer count, String type) throws FeedException;

    FeedIndexingStatus status(String siteKey, Integer count, String type) throws FeedException;

    String triggerFullUpload(String siteKey) throws FeedException;

    String reIndexSearchFeed(String siteKey) throws FeedException;
}

