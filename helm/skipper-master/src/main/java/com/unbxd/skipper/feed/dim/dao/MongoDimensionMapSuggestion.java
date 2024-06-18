package com.unbxd.skipper.feed.dim.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.unbxd.skipper.feed.dim.model.DimensionMap;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

public class MongoDimensionMapSuggestion implements DimensionMapSuggestion {

    private MongoCollection<Document> mongoCollection;
    private static final String COLLECTION_NAME = "dimensionMappingFields";
    private static final String VERTICAL_PROPERTY_NAME = "vertical";
    private static final String ESSENTIAL = "essential";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public MongoDimensionMapSuggestion(MongoDatabase mongoDatabase) {
        mongoCollection = mongoDatabase.getCollection(COLLECTION_NAME);
    }

    /**
     * Method will look in collection, cast it to DimensionMap model and return it
     * @param vertical
     * @return
     */
    @Override
    public DimensionMap get(String vertical) {
        Document essentailDim =
                mongoCollection.find(eq(VERTICAL_PROPERTY_NAME, ESSENTIAL)).projection((excludeId())).first();
        DimensionMap dim = mapper.convertValue(essentailDim, DimensionMap.class);
        Document verticalSpecificDIM =
                mongoCollection.find(eq(VERTICAL_PROPERTY_NAME, vertical)).projection(excludeId()).first();
        dim.setVerticalSpecificMapping(mapper.convertValue(verticalSpecificDIM, DimensionMap.class));
        return dim;
    }

    /**
     * Method to update dimensionMap
     * @param mapping
     */
   @Override
    public void save(DimensionMap mapping) {
       ReplaceOptions options = new ReplaceOptions().upsert(true);
       ObjectMapper mapper = new ObjectMapper();
       mongoCollection.replaceOne(eq(VERTICAL_PROPERTY_NAME, mapping.getVertical()),
               mapper.convertValue(mapping, Document.class), options);
    }
}

