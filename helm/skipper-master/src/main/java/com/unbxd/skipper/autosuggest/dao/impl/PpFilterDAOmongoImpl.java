package com.unbxd.skipper.autosuggest.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.unbxd.skipper.autosuggest.dao.PpFilterDAO;
import com.unbxd.skipper.autosuggest.model.PopularProductsFilter;
import org.bson.Document;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class PpFilterDAOmongoImpl implements PpFilterDAO {
    private static final String PP_FILTER_COLLECTION_NAME = "autosuggest.popularProductsFilter";
    private static final String SITE_KEY = "siteKey";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ReplaceOptions REPLACE_OPTION_UPSERT_TRUE = new ReplaceOptions().upsert(true);
    private MongoCollection<Document> popularProductsFilterCollection;

    @Inject
    public PpFilterDAOmongoImpl(MongoDatabase mongoDatabase) {
        this.popularProductsFilterCollection = mongoDatabase.getCollection(PP_FILTER_COLLECTION_NAME);
    }

    public void save(PopularProductsFilter PopularProductsFilter) {
        String siteKey = PopularProductsFilter.getSiteKey();
        Document data = MAPPER.convertValue(PopularProductsFilter, Document.class);
        popularProductsFilterCollection.replaceOne(and(eq(SITE_KEY, siteKey)), data, REPLACE_OPTION_UPSERT_TRUE);
    }

    public PopularProductsFilter get(String siteKey) {
        Document query =  new Document().append(SITE_KEY, siteKey);
        Document result = popularProductsFilterCollection.find(query).first();
        return MAPPER.convertValue(result, PopularProductsFilter.class);
    }
}
