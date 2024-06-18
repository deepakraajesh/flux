package com.unbxd.skipper.states.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.dao.StateDao;
import com.unbxd.skipper.states.util.StateDocumentUtils;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Log4j2
public class StateDaoImpl implements StateDao {

    protected MongoCollection<Document> stateCollection;
    protected ReplaceOptions replaceOptions;
    protected StateDocumentUtils stateDocumentUtils;
    protected static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public StateDaoImpl(@Named("state") MongoCollection<Document> stateCollection,
                        StateDocumentUtils documentUtils) {
        this.stateCollection = stateCollection;
        this.stateDocumentUtils = documentUtils;
        this.replaceOptions = new ReplaceOptions();

        this.replaceOptions.upsert(true);
    }

    public StateContext fetchState(String fieldName, String fieldValue) throws NoSuchElementException {
        Document document = new Document();
        document.put(fieldName, fieldValue);

        FindIterable<Document> documents = stateCollection.find(document);
        Document doc = documents.iterator().next();
        if(doc == null)
            return null;

        StateContext stateContext = mapper.convertValue(doc, StateContext.class);
        return stateContext;
    }

    @Override
    public long deleteSite(String siteKey) {
        return stateCollection.deleteOne(eq(SITEKEY,siteKey)).getDeletedCount();
    }

    @Override
    public void reset(String siteKey, ServeState state) {
        stateCollection.updateOne(eq(SITEKEY, siteKey), set(SERVE_STATE, mapper.convertValue(state, Document.class)));
        stateCollection.updateOne(eq(SITEKEY, siteKey), unset(WORKFLOW_ID));
    }

    @Override
    public StateContext fetchState(String siteKey) throws SiteNotFoundException {
        Document document = new Document();
        document.put(SITEKEY, siteKey);

        FindIterable<Document> documents = stateCollection.find(document);
        if(!documents.iterator().hasNext()) {
            throw new SiteNotFoundException();
        }
        Document doc = documents.iterator().next();
        StateContext stateContext = mapper.convertValue(doc, StateContext.class);

        return stateContext;
    }

    @Override
    public void saveState(StateContext stateContext) {
            Document siteKeyDoc = new Document();
            if(isEmpty(stateContext.getId())) {
                stateContext.setId(UUID.randomUUID().toString());
            }

            siteKeyDoc.put(MONGO_ID, stateContext.getId());
            Document doc = mapper.convertValue(stateContext, Document.class);
            stateCollection.replaceOne(siteKeyDoc, doc, replaceOptions);
    }

    @Override
    public void updateState(String siteKey,
                            Map<String, String> fields)
            throws SiteNotFoundException {
        Bson filter = eq(SITEKEY, siteKey);
        if (stateCollection.countDocuments(filter) == 0) {
            throw new SiteNotFoundException("Site key is not present");
        }

        List<Bson> updateValues = new ArrayList<>();
        for(String key: fields.keySet()){
            updateValues.add(set(key, fields.get(key)));
        }
        stateCollection.updateOne(filter, combine(updateValues));
    }
}
