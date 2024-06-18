package com.unbxd.skipper.feed.service;

import com.unbxd.skipper.feed.exception.IndexingException;
import com.unbxd.skipper.feed.model.IndexingStatus;

/** service supports indexing status cache **/

public interface IndexingService {
    void indexAutosuggest(String siteKey) throws IndexingException;
    void indexCatalog(String siteKey) throws IndexingException;
    IndexingStatus getStatus(String siteKey) throws IndexingException;
}
