package com.unbxd.pim.workflow.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unbxd.pim.workflow.model.WorkflowStatus;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;


@Log4j2
public class MongoWorkflowDao implements WorkflowDao {

    MongoDatabase mongoDatabase;
    private static final String TEMPLATE_COLLECTION = "pimWorkflowTemplate";
    private static final String WORKFLOW_COLLECTION = "pimWorkflow";
    private static final String ORG_ID_PARAM = "orgId";
    private static final String APP_ID_PARAM = "appId";
    private static final String WORKFLOW_STATE = "workflowState";
    private static final String CONFIG_KEY = "config";

    private static ObjectMapper mapper = new ObjectMapper();


    @Inject
    public MongoWorkflowDao(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }
    @Override
    public JsonObject fetchTemplate(String field, String value) {
        MongoCollection<Document> templateCollection = mongoDatabase.getCollection(TEMPLATE_COLLECTION);
        Document document = new Document();
        document.put(field, value);

        FindIterable<Document> documents = templateCollection.find(document);
        if(documents == null || !documents.iterator().hasNext())
            return null;
        Document doc = documents.iterator().next();
        String stringTemplate = doc.toJson();

        JsonElement jsonTemplate = JsonParser.parseString(stringTemplate);
        return jsonTemplate.getAsJsonObject().getAsJsonObject(CONFIG_KEY);
    }

    @Override
    public void saveWorkflowSnapshot(WorkflowStatus status) {
        MongoCollection<Document> workflowCollection = mongoDatabase.getCollection(WORKFLOW_COLLECTION);
        Document statusDoc = mapper.convertValue(status, Document.class);
        workflowCollection.insertOne(statusDoc);
    }
}

