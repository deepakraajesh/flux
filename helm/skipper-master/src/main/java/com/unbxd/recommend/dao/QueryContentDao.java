package com.unbxd.recommend.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Inject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.IndexOptions;
import com.unbxd.recommend.exception.RecommendException;
import com.unbxd.recommend.model.Operation;
import com.unbxd.recommend.model.QueryStats;
import com.unbxd.recommend.model.RecommendContext;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.fasterxml.jackson.dataformat.csv.CsvSchema.builder;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.pull;
import static com.unbxd.recommend.model.Operation.IMPROVE_RECALL;
import static com.unbxd.recommend.model.Operation.ZERO_RECTIFIED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class QueryContentDao implements ContentDao {
    private final MongoClient mongo;
    private final ObjectMapper mapper;
    private final ObjectReader reader;

    @Inject
    public QueryContentDao(MongoClient mongo,
                           ObjectMapper mapper) {
        this.reader = getCSVReader();
        this.mapper = mapper;
        this.mongo = mongo;
    }

    @Override
    public void storeQueryStats(File file,
                                String sitekey,
                                String jobType,
                                String workflowId)
            throws RecommendException {
        Scanner scanner;
        FileInputStream inputStream;
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(QUERY_CONTENT_COLLECTION);
        createIndexes(collection);

        try {
            inputStream = new FileInputStream(file);
            scanner = new Scanner(inputStream, UTF_8);

            scanner.nextLine();
            while (scanner.hasNextLine()) {
                QueryStats queryStats = fromCSV(scanner.nextLine());
                collection.insertOne(getStatsDocument(jobType,
                        workflowId, queryStats));
            }
            scanner.close();
            inputStream.close();
        } catch (IOException | MongoException e) {
            throw new RecommendException(e);
        }
    }

    @Override
    public void flushQueryStats(String sitekey,
                                String jobType)
            throws RecommendException {
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(QUERY_CONTENT_COLLECTION);
        try {
            collection.deleteMany(eq(JOBTYPE, jobType));
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
    }

    @Override
    public long countQueryStats(RecommendContext recommendContext)
            throws RecommendException {
        MongoCollection<Document> collection = mongo.getDatabase
                (recommendContext.getSitekey()).getCollection
                (QUERY_CONTENT_COLLECTION);
        try {
            return collection.countDocuments(getStatsQuery
                    (recommendContext));
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
    }

    @Override
    public List<QueryStats> getQueryStats(RecommendContext recommendContext)
            throws RecommendException {
        MongoCollection<Document> collection = mongo.getDatabase
                (recommendContext.getSitekey()).getCollection
                (QUERY_CONTENT_COLLECTION);
        List<QueryStats> statsList = new ArrayList<>();
        int count = recommendContext.getCount();
        int page = recommendContext.getPage();

        try (MongoCursor<Document> iterator = collection
                .find( getStatsQuery(recommendContext))
                .skip(countElementsToSkip(page, count))
                .limit(count).iterator()) {
            while (iterator.hasNext()) {
                statsList.add(mapper.convertValue(iterator
                        .next(), QueryStats.class));
            }
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
        return statsList;
    }

    @Override
    public void deleteRowIds(String rowId,
                             String sitekey)
            throws RecommendException {
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(QUERY_CONTENT_COLLECTION);
        try {
            collection.updateMany(new Document(), pull(ROWS, rowId));
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
    }

    @Override
    public List<String> getRowIds(String filter,
                                  String sitekey)
            throws RecommendException {
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(QUERY_CONTENT_COLLECTION);
        try (MongoCursor<Document> iterator = collection
                .find(eq(QUERY, filter))
                .iterator()) {
            if (iterator.hasNext()) {
                return iterator.next().get(ROWS, new ArrayList<>());
            }
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
        return emptyList();
    }

    @Override
    public List<String> getLanguages(String sitekey)
            throws RecommendException {
        List<String> languages = new ArrayList<>();
        MongoCollection<Document> collection = mongo
                .getDatabase(sitekey).getCollection
                        (QUERY_CONTENT_COLLECTION);
        try (MongoCursor<String> iterator = collection
                .distinct(SECONDARY_LANGUAGES, eq(SITEKEY,
                        sitekey), String.class).iterator()) {
            while (iterator.hasNext()) {
                String language = iterator.next();
                if (isNotEmpty(language)) { languages.add(language); }
            }
        } catch (MongoException e) {
            throw new RecommendException(e);
        }
        return languages;
    }

    private Document getStatsDocument(String jobType,
                                      String workflowId,
                                      QueryStats queryStats) {
        Document document = mapper.convertValue(queryStats, Document.class);
        document.put(WORKFLOW_ID, workflowId);
        document.put(JOBTYPE, jobType);
        return document;
    }

    private QueryStats fromCSV(String queryStatsCSV)
            throws JsonProcessingException {
        return reader.readValue(queryStatsCSV);
    }

    private ObjectReader getCSVReader() {
        CsvSchema csvSchema = builder()
                .addColumn(QUERY)
                .addNumberColumn(HITS)
                .addNumberColumn(AFTER)
                .addNumberColumn(BEFORE)
                .addColumn(SECONDARY_LANGUAGES)
                .addBooleanColumn(OR_RECTIFIED)
                .addBooleanColumn(SPELL_CHECKED)
                .addBooleanColumn(CONCEPT_CORRECTED)
                .addArrayColumn(ROWS, COMMA_DELIMITER)
                .build().withoutHeader();
        return new CsvMapper()
                .readerFor(QueryStats.class)
                .with(csvSchema);
    }

    private Bson getStatsQuery(RecommendContext recommendContext) {
        Operation operation = recommendContext.getOperation();
        if (operation == ZERO_RECTIFIED) {
            return and(eq(BEFORE, 0),
                    not(size(ROWS, 0)),
                    eq(SPELL_CHECKED, false));
        } else if (operation == IMPROVE_RECALL) {
            return and(gt(BEFORE, 0),
                    not(size(ROWS, 0)),
                    eq(SPELL_CHECKED, false));
        } else if (operation == Operation.OR_RECTIFIED) {
            return and(not(size(ROWS, 0)),
                    eq(OR_RECTIFIED, true),
                    eq(SPELL_CHECKED, false));
        } else if (operation == Operation.SPELL_CHECKED) {
            return and(not(size(ROWS, 0)),
                    eq(SPELL_CHECKED, true));
        } else if (operation == Operation.LANGUAGE_BASED) {
            return and(not(size(ROWS, 0)),
                    eq(SECONDARY_LANGUAGES, recommendContext
                            .getLanguage()));
        } else if (operation == Operation.CONCEPT_CORRECTED) {
            return and(not(size(ROWS, 0)),
                    eq(CONCEPT_CORRECTED, true));
        }
        return new Document();
    }

    private void createIndexes(MongoCollection<Document> collection) {
        collection.createIndex(ascending(JOBTYPE), new IndexOptions()
                .name("jobType_index"));
        collection.createIndex(ascending(BEFORE), new IndexOptions()
                .name("before_index"));
        collection.createIndex(ascending(QUERY), new IndexOptions()
                .unique(true).name("query_index"));
    }

    private int countElementsToSkip(int page, int count) {
        return page <= 1 ? 0 : (page - 1) * count;
    }
}
