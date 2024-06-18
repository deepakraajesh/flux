package com.unbxd.gcp.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.unbxd.gcp.exception.GCPException;
import com.unbxd.gcp.model.AccountMeta;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class GCPProcurementDao implements ProcurementDao {

    private ObjectMapper mapper;
    private MongoDatabase database;
    private final String PROCUREMENT_COLLECTION = "google-procurement";

    @Inject
    public GCPProcurementDao(MongoDatabase database) {
        this.database = database;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void saveAccountMeta(AccountMeta meta) throws GCPException {
        MongoCollection<Document> collection = database.getCollection
                (PROCUREMENT_COLLECTION);
        try {
            collection.insertOne(mapper.convertValue(meta, Document.class));
        } catch (MongoException e) {
            throw new GCPException("Exception while trying to save account meta" +
                    " for account["+ meta.getAccountName() + "] : " + e.getMessage());
        }
    }

    @Override
    public AccountMeta getAccountMeta(String accountName) throws GCPException {
        MongoCollection<Document> collection = database.getCollection
                (PROCUREMENT_COLLECTION);
        try {
            FindIterable<Document> accountMeta = collection.find(eq
                    ("accountName", accountName)).sort
                    (new BasicDBObject("$natural", -1));
            if (accountMeta.iterator().hasNext()) {
                return mapper.convertValue(accountMeta
                        .iterator().next(), AccountMeta.class);
            }
        } catch (MongoException e) {
            throw new GCPException("Exception while trying to retrieve account" +
                    " meta for account["+ accountName + "] : " + e.getMessage());
        }
        return null;
    }
}
