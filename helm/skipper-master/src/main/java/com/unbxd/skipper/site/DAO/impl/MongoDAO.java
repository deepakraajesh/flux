package com.unbxd.skipper.site.DAO.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import com.unbxd.skipper.site.DAO.SiteDAO;
import com.unbxd.skipper.site.model.*;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;


import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Log4j2
public class MongoDAO implements SiteDAO {
    private static final String DATA_CENTER_COLLECTION_NAME = "dataCenters";
    private static final String ENVIRONMENT_COLLECTION_NAME = "site.meta.environments";
    private static final String VERTICAL_COLLECTION_NAME = "site.meta.verticals";
    private static final String PLATFORM_COLLECTION_NAME = "site.meta.platforms";
    private static final String LANGUAGE_COLLECTION_NAME = "site.meta.languages";
    private static final String NAME = "name";
    private static final String LAT_LONG = "lat_long";
    private static final String ID = "_id";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ReplaceOptions replaceOptionsUpsert = new ReplaceOptions().upsert(true);


    private MongoCollection<Document> dataCenterCollection;
    private MongoCollection<Document> environmentCollection;
    private MongoCollection<Document> verticalCollection;
    private MongoCollection<Document> platformCollection;
    private MongoCollection<Document> languageCollection;

    @Inject
    public MongoDAO(MongoDatabase mongoDatabase) {
        this.dataCenterCollection = mongoDatabase.getCollection(DATA_CENTER_COLLECTION_NAME);
        this.environmentCollection = mongoDatabase.getCollection(ENVIRONMENT_COLLECTION_NAME);
        this.verticalCollection =  mongoDatabase.getCollection(VERTICAL_COLLECTION_NAME);
        this.platformCollection = mongoDatabase.getCollection(PLATFORM_COLLECTION_NAME );
        this.languageCollection = mongoDatabase.getCollection(LANGUAGE_COLLECTION_NAME);
    }

    @Override
    public DataCenter getDataCenter(String region) {
        Document result = dataCenterCollection.find(eq(ID, region)).first();
        if(result == null) {
            return null;
        }
        return mapper.convertValue(result, DataCenter.class);
    }

    @Override
    public DataCenterData getDataCenterData()
    {
        List<DataCenter> dataCenters = new ArrayList<>();
        try (MongoCursor<Document> cursor = dataCenterCollection.find().iterator()){
            while(cursor.hasNext()) {
                dataCenters.add(mapper.convertValue(cursor.next(), DataCenter.class));
            }
        }
        return new DataCenterData(dataCenters);
    }

    @Override
    public void setDataCenterData(DataCenterData dataCenterData) {
        for (DataCenter dataCenter : dataCenterData.getDataCenters()) {
            Document query = new Document();
            query.put(ID,dataCenter.getId());
            Document doc = mapper.convertValue(dataCenter, Document.class);
            UpdateResult updateResult =  dataCenterCollection.replaceOne(query,doc, replaceOptionsUpsert);
            log.info(updateResult.toString());
        }
    }

    @Override
    public SiteMeta getSiteMeta() {
       SiteMeta siteMeta = new SiteMeta();
       siteMeta.setEnvironments(getEnvironments());
       siteMeta.setVerticals(getVerticals());
       siteMeta.setPlatforms(getPlatforms());
       siteMeta.setLanguages(getLanguages());
       return siteMeta;
    }

    @Override
    public void setEnvironments(List<Environment> environments) {
        if(isNull(environments)) return;
        for(Environment environment : environments) {
            Document query = new Document();
            query.put(ID,environment.getId());
            Document doc = mapper.convertValue(environment, Document.class);
            UpdateResult updateResult =  environmentCollection.replaceOne(query,doc, replaceOptionsUpsert);
            log.info(updateResult.toString());
        }
    }

    private List<Environment> getEnvironments() {
        List<Environment> environments = new ArrayList<>();
        try (MongoCursor<Document> iterator = environmentCollection.find().iterator()){
            while(iterator.hasNext())
                environments.add(mapper.convertValue(iterator.next(),Environment.class));
        }
        return  environments;
    }

    @Override
    public void setVerticals(List<Vertical> verticals) {
        if(isNull(verticals)) return;
        for(Vertical vertical : verticals) {
            Document query = new Document();
            query.put(ID,vertical.getId());
            Document doc = mapper.convertValue(vertical, Document.class);
            UpdateResult updateResult =  verticalCollection.replaceOne(query,doc, replaceOptionsUpsert);
            log.info(updateResult.toString());
        }
    }

    private List<Vertical> getVerticals(){
        List<Vertical> verticals = new ArrayList<>();
        try (MongoCursor<Document> iterator = verticalCollection.find().iterator()){
            while(iterator.hasNext())
                verticals.add(mapper.convertValue(iterator.next(),Vertical.class));
        }
        return  verticals;
    }

    @Override
    public void setPlatforms(List<Platform> platforms) {
        if(isNull(platforms)) return;
        for(Platform platform : platforms) {
            Document query = new Document();
            query.put(ID,platform.getId());
            Document doc = mapper.convertValue(platform, Document.class);
            UpdateResult updateResult = platformCollection.replaceOne(query,doc,replaceOptionsUpsert);
            log.info(updateResult.toString());
        }
    }

    private List<Platform> getPlatforms() {
        List<Platform> platforms = new ArrayList<>();
        try (MongoCursor<Document> iterator = platformCollection.find().iterator()){
            while(iterator.hasNext())
                platforms.add(mapper.convertValue(iterator.next(),Platform.class));
        }
        return  platforms;
    }

    @Override
    public void setLanguages(List<Language> languages) {
        if(isNull(languages)) return;
        for(Language language : languages) {
            Document query = new Document();
            query.put(ID,language.getId());
            Document doc = mapper.convertValue(language, Document.class);
            UpdateResult updateResult = languageCollection.replaceOne(query,doc,replaceOptionsUpsert);
                    log.info(updateResult.toString());
        }
    }

    private List<Language> getLanguages() {
        List<Language> languages = new ArrayList<>();
        try (MongoCursor<Document> iterator = languageCollection.find().iterator()){
            while(iterator.hasNext())
                languages.add(mapper.convertValue(iterator.next(),Language.class));
        }
        return  languages;
    }

    private boolean isNull(Object object) {
        return object == null;
    }

    private boolean checkIfIdExistsInCollection(String id, MongoCollection<Document> collection){
        if(id == null) {
            return Boolean.FALSE;
        }
        Document document = collection.find(eq(ID,id)).first();
        return !isNull(document);
    }
    @Override
    public boolean validateDataCenter(String region){
        return checkIfIdExistsInCollection(region,dataCenterCollection);
    }

    @Override
    public boolean validateVertical(String verticalId){
        return checkIfIdExistsInCollection(verticalId,verticalCollection);
    }

    @Override
    public boolean validateEnvironment(String environmentId){
        return checkIfIdExistsInCollection(environmentId,environmentCollection);
    }

    @Override
    public boolean validateLanguage(String languageId) {
        return checkIfIdExistsInCollection(languageId, languageCollection);
    }

    @Override
    public boolean validatePlatform(String platformId) {
        return checkIfIdExistsInCollection(platformId, platformCollection);
    }
}

