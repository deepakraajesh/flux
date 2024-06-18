package com.unbxd.skipper.states.dao.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.unbxd.skipper.states.util.StateDocumentUtils;
import org.bson.Document;

public class AutosuggestStateDaoImpl extends StateDaoImpl {

    @Inject
    public AutosuggestStateDaoImpl(@Named("autosuggest-state") MongoCollection<Document> stateCollection,
                                   StateDocumentUtils documentUtils) {
        super(stateCollection,documentUtils);
        /** to ensure duplicate documents are not created **/
        stateCollection.createIndex(new Document().append("siteKey",1),new IndexOptions().unique(true));
    }
}
