package com.unbxd.skipper.relevancy.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.unbxd.skipper.relevancy.model.Field;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.model.SearchableField;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.pushEach;
import static com.unbxd.skipper.relevancy.service.RelevancyService.SEARCHABLE_FIELD_JOB;
import static java.util.Objects.isNull;

@Slf4j
public class RelevancyDaoImpl implements RelevancyDao {

    private final ObjectMapper mapper;
    private MongoDatabase mongoDatabase;
    private static final ReplaceOptions REPLACE_OPTIONS
            = ReplaceOptions.createReplaceOptions(new UpdateOptions().upsert(true));

    @Inject
    public RelevancyDaoImpl(MongoDatabase mongoDatabase) {
        this.mapper = new ObjectMapper();
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public void saveRelevancyOutput(JobType type, RelevancyOutputModel relevancyOutputModel) {
        MongoCollection<Document> relevancyCollection = mongoDatabase.getCollection(type.toString());
        Document document = mapper.convertValue(relevancyOutputModel, Document.class);
        relevancyCollection.replaceOne(eq("siteKey", relevancyOutputModel.getSiteKey()),
                document,
                REPLACE_OPTIONS);
    }

    @Override
    public RelevancyOutputModel fetchRelevancyOutput(JobType type, String siteKey) {
        MongoCollection<Document> relevancyCollection = mongoDatabase.getCollection(type.toString());
        Document document = new Document();

        document.put(SITEKEY, siteKey);
        FindIterable<Document> documents = relevancyCollection.find(document).projection(excludeId());
        if(!documents.iterator().hasNext())
            return null;
        Document doc = documents.iterator().next();

        if(!RelevancyOutputModel.typeToClass.containsKey(type))
            throw new IllegalArgumentException("Wrong data is stored for " + siteKey + " type " + type);

        JavaType javaType = mapper.getTypeFactory().constructParametricType(RelevancyOutputModel.class,
                RelevancyOutputModel.typeToClass.get(type));
        RelevancyOutputModel relevancyOutputModel = mapper.convertValue(doc, javaType);
        return relevancyOutputModel;
    }

    @Override
    public List<SearchableField> getSearchableFields(String siteKey, List<String> fieldNames){
        MongoCollection<Document> searchableFieldCollection = mongoDatabase.getCollection(SEARCHABLE_FIELD_JOB);
        searchableFieldCollection.createIndex(new Document().append(SITEKEY,1), new IndexOptions().unique(true));
        Document document = searchableFieldCollection.aggregate(
                Arrays.asList(
                        match(eq(SITEKEY,siteKey)),
                        project(fields(computed(DATA,buildFilterQuery(fieldNames)))))
            ).first();
        if(isNull(document)){
            log.info("relevancy data of searchable fields is not found for the siteKey: "+siteKey);
            return null;
        }
        return mapper.convertValue(document.get(DATA),new TypeReference<>() {});
    }

    @Override
    public void appendData(JobType jobType, String siteKey, List<SearchableField> searchableFields) {
        MongoCollection<Document> searchableFieldCollection = mongoDatabase.getCollection(SEARCHABLE_FIELD_JOB);
        searchableFieldCollection.updateOne(eq(SITEKEY, siteKey), pushEach(DATA, mapper.convertValue(searchableFields,
                new TypeReference<List<Document>>(){})));
    }

    private Document buildFilterQuery(List<String> fieldNames){
        Document query = new Document();
        Document filterExpression = new Document();
        filterExpression.put("input","$data");
        filterExpression.put("as","item");
        Document inExpression = new Document();
        List<Object> inParams = new ArrayList<>(2);
        inParams.add("$$item.fieldName");
        inParams.add(fieldNames);
        inExpression.put("$in",inParams);
        filterExpression.put("cond",inExpression);
        query.put("$filter",filterExpression);
        return query;
    }


}
