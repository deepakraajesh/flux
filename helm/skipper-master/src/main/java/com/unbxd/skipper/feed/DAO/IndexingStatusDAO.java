package com.unbxd.skipper.feed.DAO;

import com.unbxd.skipper.feed.model.IndexingStatus;

public interface IndexingStatusDAO {
    IndexingStatus fetchStatus(String siteKey);
    void updateStatus(IndexingStatus status);
    void addStatus(IndexingStatus status);
}
