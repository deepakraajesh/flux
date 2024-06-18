package com.unbxd.mongo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unbxd.config.Config;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoModule extends AbstractModule {

    public static final String MONGO_CONNECTION_STRING_PROPERTY_NAME = "mongo";

    protected static final String DATABASE_NAME = "skipper";
    protected static final String STATE_COLLECTION = "stateCollection";
    protected static final String AUTOSUGGEST_STATE_COLLECTION = "autosugggestStateCollection";


    @Singleton
    @Provides
    private MongoDatabase getMongoDatabase(Config config) {
        String connectionString = config.getProperty(MONGO_CONNECTION_STRING_PROPERTY_NAME);
        MongoClient client = (connectionString == null || connectionString.isEmpty())?
                MongoClients.create(): MongoClients.create(connectionString);
        client.listDatabaseNames();
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        return client.getDatabase(DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);
    }

    @Singleton
    @Provides
    private MongoClient getMongoClient(Config config) {
        String connectionString = config.getProperty(MONGO_CONNECTION_STRING_PROPERTY_NAME);
        MongoClient client = (connectionString == null || connectionString.isEmpty())?
                MongoClients.create(): MongoClients.create(connectionString);
        client.listDatabaseNames();
        return client;
    }

    public MongoDatabase getDatabase(Config config) {
        return  getMongoDatabase(config);
    }

    @Singleton
    @Provides
    @Named("state")
    private MongoCollection<Document> getSiteStateCollection(Config config) {
         MongoDatabase mongoDatabase = getMongoDatabase(config);
         return mongoDatabase.getCollection(STATE_COLLECTION);
    }

    @Singleton
    @Provides
    @Named("autosuggest-state")
    private MongoCollection<Document> getAutosuggestStateCollection(Config config) {
        MongoDatabase mongoDatabase = getMongoDatabase(config);
        return mongoDatabase.getCollection(AUTOSUGGEST_STATE_COLLECTION);
    }
}

