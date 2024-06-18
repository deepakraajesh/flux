package com.unbxd.skipper.autosuggest.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.unbxd.skipper.autosuggest.dao.TemplateDAO;
import com.unbxd.skipper.autosuggest.model.Template;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.unbxd.skipper.autosuggest.model.Template.TEMPLATE_ID;
import static com.unbxd.skipper.autosuggest.model.Template.VERTICAL;

public class MongoTemplateDAO implements TemplateDAO {

    private static final String TEMPLATE_COLLECTION_NAME = "template";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ReplaceOptions REPLACE_OPTION_UPSERT_TRUE = new ReplaceOptions().upsert(true);
    private MongoCollection<Document> templateCollection;

    @Inject
    public MongoTemplateDAO(MongoDatabase mongoDatabase) {
        this.templateCollection = mongoDatabase.getCollection(TEMPLATE_COLLECTION_NAME);
    }

    @Override
    public void add(Template template){
        String templateId = template.getTemplateId();
        String vertical = template.getVertical();
        Document data = MAPPER.convertValue(template, Document.class);
        templateCollection.replaceOne(
                and(eq(TEMPLATE_ID, templateId), eq(VERTICAL, vertical)),
                data, REPLACE_OPTION_UPSERT_TRUE);
    }

    @Override
    public List<Template> getTemplates() {
        List<Template> templates = new ArrayList<>();
        try (MongoCursor<Document> iterator = templateCollection.find().iterator()){
            while(iterator.hasNext())
                templates.add(MAPPER.convertValue(iterator.next(),Template.class));
        }
        return  templates;
    }

    @Override
    public Template getTemplate(String templateId) {
        Document query = new Document().append(TEMPLATE_ID,templateId);
        return MAPPER.convertValue(templateCollection.find(query).first(),Template.class);
    }

}
