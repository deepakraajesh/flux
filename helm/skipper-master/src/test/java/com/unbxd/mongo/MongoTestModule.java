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
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.io.IOException;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoTestModule extends AbstractModule {

    private static final MockMongoServer mockMongoServer;

    static {
        try {
            mockMongoServer = new MockMongoServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Singleton
    @Provides
    private MongoDatabase getMongoDatabase() {
        String connectionString = "mongodb://localhost:" + mockMongoServer.getPort();
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        return MongoClients.create(connectionString).getDatabase(MongoModule.DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);
    }

    @Singleton
    @Provides
    private MongoClient getMongoClient() {
        String connectionString = "mongodb://localhost:" + mockMongoServer.getPort();
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        return MongoClients.create(connectionString);
    }

    @Singleton
    @Provides
    @Named("state")
    private MongoCollection<Document> getSiteStateCollection() {
        MongoDatabase mongoDatabase = getMongoDatabase();
        return mongoDatabase.getCollection(MongoModule.STATE_COLLECTION);
    }

    @Singleton
    @Provides
    @Named("autosuggest-state")
    private MongoCollection<Document> getAutosuggestStateCollection() {
        MongoDatabase mongoDatabase = getMongoDatabase();
        return mongoDatabase.getCollection(MongoModule.AUTOSUGGEST_STATE_COLLECTION);
    }
}

