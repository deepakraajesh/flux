package com.unbxd.skipper.feed.DAO.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.unbxd.skipper.feed.DAO.IndexingStatusDAO;
import com.unbxd.skipper.feed.model.IndexingStatus;
import org.bson.Document;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static java.util.Objects.nonNull;


public class IndexingStatusDAOImpl implements IndexingStatusDAO {

    private MongoCollection<Document> indexingStatusCollection;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String INDEXING_STATUS_COLLECTION_NAME = "indexingStatus";
    private static final String SITE_KEY = "siteKey";
    private static final String AUTOSUGGEST_FEED_ID = "autosuggest.feedId";
    private static final String CATALOG_FEED_ID = "catalog.feedId";
    private static final Document REVERSE_NATURAL_SORT = new Document().append("$natural",-1);
    private static final String DATE = "date";


    @Inject
    public IndexingStatusDAOImpl(MongoDatabase mongoDatabase) {
        this.indexingStatusCollection = mongoDatabase.getCollection(INDEXING_STATUS_COLLECTION_NAME);
        indexingStatusCollection.createIndex(Indexes.ascending(DATE),new IndexOptions()
                .expireAfter(6L, TimeUnit.HOURS));
        indexingStatusCollection.createIndex(Indexes.ascending(CATALOG_FEED_ID));
        indexingStatusCollection.createIndex(Indexes.ascending(AUTOSUGGEST_FEED_ID));
        indexingStatusCollection.createIndex(Indexes.ascending(SITE_KEY));
    }

    @Override
    public IndexingStatus fetchStatus(String siteKey) {
        Document query = new Document().append(SITE_KEY,siteKey);
        return MAPPER.convertValue(
                indexingStatusCollection.find(query).hint(REVERSE_NATURAL_SORT).first(),
                IndexingStatus.class);
    }

    @Override
    public void updateStatus(IndexingStatus status) {
        Document data = MAPPER.convertValue(status, Document.class);
        data.append(DATE,new Date());
        if(nonNull(status.getCatalog()))
        indexingStatusCollection.replaceOne(
                eq(CATALOG_FEED_ID, status.getCatalog().getFeedId()),
                data);
        else if(nonNull(status.getAutosuggest()))
            indexingStatusCollection.replaceOne(
                    eq(AUTOSUGGEST_FEED_ID, status.getAutosuggest().getFeedId()),
                    data);
    }

    @Override
    public void addStatus(IndexingStatus status) {
        Document data = MAPPER.convertValue(status, Document.class);
        data.append(DATE,new Date());
        indexingStatusCollection.insertOne(data);
    }
}
