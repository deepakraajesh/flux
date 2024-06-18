package com.unbxd.skipper.dictionary.model;

import com.amazonaws.services.eks.model.Update;
import com.amazonaws.services.s3.internal.ObjectExpirationHeaderHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DictionaryMongo {

    public static final String DATA = "data";
    public static final String GOOD = "good";
    public static final String BAD = "bad";
    public static final String BEST = "best";
    public static final String MONGO_ID_KEY = "_id";
    public static final String TOTAL_NUMBER_OF_QUERIES = "totalNumberOfQueries";
    public static final String TOTAL_NUMBER_OF_BEST_QUERIES = "totalBestQueries";
    public static final String TOTAL_NUMBER_OF_GOOD_QUERIES = "totalGoodQueries";
    public static final String TOTAL_NUMBER_OF_BAD_QUERIES = "totalBadQueries";
    private static ObjectMapper mapper = new ObjectMapper();
    private String id;
    private String data;
    private static final String ASSET_DATA_KEY = "data";
    List<Query> good;
    List<Query> bad;
    List<Query> best;
    long totalNumberOfQueries;
    long totalBestQueries;
    long totalBadQueries;
    long totalGoodQueries;
    String quality;

    public DictionaryMongo(String id, String content, List<Query> good,
                           List<Query> bad, List<Query> best) {
        this.id = id;
        this.data = content;
        this.good = good;
        this.bad = bad;
        this.best = best;
        this.totalNumberOfQueries = lengthOfQuery(good) + lengthOfQuery(bad) + lengthOfQuery(best);
        this.totalBestQueries = lengthOfQuery(best);
        this.totalGoodQueries = lengthOfQuery(good);
        this.totalBadQueries = lengthOfQuery(bad);
    }

    private long lengthOfQuery(List<Query> queries) {
        return (queries != null)? queries.size():0;
    }

    public Bson toMongoUpdateDoc() {
        List<Bson> updates = new ArrayList<>();
        updates.add(set(DATA, data));

        setQueriesList(updates);
        setQueriesCount(updates);
        return combine(updates);
    }

    public static DictionaryMongo getInstance(Document document) {
        DictionaryMongo result = new DictionaryMongo();
        result.setId(document.getObjectId(MONGO_ID_KEY).toString());
        result.setData(document.getString(ASSET_DATA_KEY));
        return result;
    }

    public Document toDocument() {
        Document document = new Document();
        ObjectId id =  Objects.nonNull(this.id) ? new ObjectId(this.id) : new ObjectId();
        document.put(MONGO_ID_KEY,id);
        document.put(ASSET_DATA_KEY,data);
        return document;
    }

    private void setQueriesList(List<Bson> updates){
        if(good != null)
            updates.add(set(GOOD, mapper.convertValue(good, new TypeReference<List<Document>>(){})));
        if(bad != null)
            updates.add(set(BAD, mapper.convertValue(bad, new TypeReference<List<Document>>(){})));
        if(best != null)
            updates.add(set(BEST, mapper.convertValue(best, new TypeReference<List<Document>>(){})));
    }

    private void setQueriesCount(List<Bson> updates){
        if(totalNumberOfQueries > 0)
            updates.add(set(TOTAL_NUMBER_OF_QUERIES, totalNumberOfQueries));
        if(totalBestQueries > 0)
            updates.add(set(TOTAL_NUMBER_OF_BEST_QUERIES, totalBestQueries));
        if(totalGoodQueries > 0)
            updates.add(set(TOTAL_NUMBER_OF_GOOD_QUERIES, totalGoodQueries));
        if(totalBadQueries > 0)
            updates.add(set(TOTAL_NUMBER_OF_BAD_QUERIES, totalBadQueries));
    }
}
