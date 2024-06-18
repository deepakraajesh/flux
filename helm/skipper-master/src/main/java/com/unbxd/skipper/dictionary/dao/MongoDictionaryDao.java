package com.unbxd.skipper.dictionary.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryAnalysis;
import com.unbxd.skipper.dictionary.model.DictionaryAnalysisSegment;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.transformer.AssetTransformer;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Indexes.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.orderBy;
import static com.mongodb.client.model.Updates.combine;
import static com.unbxd.skipper.dictionary.model.DictionaryContext.ORDER_BY.ASC;
import static com.unbxd.skipper.dictionary.model.DictionaryMongo.*;
import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.NEWLINE_DELIMITER;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class MongoDictionaryDao implements DictionaryDAO {

    private static final ObjectMapper mapper = new ObjectMapper();

    private enum HEALTH_STATUS {
        STARTED, SUCCEEDED, FAILED
    }

    private HEALTH_STATUS status;
    private String mongoHost;
    private Integer mongoPort;
    private Boolean isSharded;
    private String connString;

    private static final String SUGGESTED_TEMPLATE = "skipper-%s-suggested.txt";
    private static final Integer connectionsPerHost = 10;
    private static final String ASSET_DATA_KEY = "data";
    private static final String ASSET_SUFFIX = ".txt";
    private static final String MONGO_ID_KEY = "_id";


    private MongoClient mongo;
    private Map<String, AssetTransformer> transformerMap;

    @Inject
    public MongoDictionaryDao(MongoClient mongo,
                              Map<String, AssetTransformer> transformerMap) {
        this.transformerMap = transformerMap;
        this.mongo = mongo;
    }

    @Override
    public void deleteCore(String corename) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null)
            throw new DAOException("Unable to connect to MongoDao");

        try {
            MongoDatabase database = mongo.getDatabase(corename);
            database.drop();
        } catch (MongoException ex) {
            throw new DAOException(ex);
        }
    }

    /* Dictionary DAO */
    @Override
    public void addDictionaryData(String coreName,
                                  List<DictionaryMongo> content,
                                  String dictionaryName) throws DAOException {
        if(content == null || content.isEmpty())
            return;

        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);
            List<Document> documentsFromContent = getDocument(content);

            collection.insertMany(documentsFromContent);
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void addDictionaryData(String coreName,
                                  String dictionaryName,
                                  DictionaryMongo data) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }
        MongoDatabase db = mongo.getDatabase(coreName);
        String collectionName = dictionaryName + ASSET_SUFFIX;
        MongoCollection<Document> collection = db.getCollection(collectionName);
        collection.createIndex(compoundIndex(descending(TOTAL_NUMBER_OF_BEST_QUERIES),
                descending(TOTAL_NUMBER_OF_QUERIES),
                descending(MONGO_ID_KEY)), new IndexOptions().name("defaultDictionarySort"));

        try {
            Bson doc = data.toMongoUpdateDoc();
            UpdateOptions updateOptions = new UpdateOptions().upsert(true);
            collection.updateOne(eq(MONGO_ID_KEY, new ObjectId(data.getId())), combine(doc), updateOptions);
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void updateDictionaryData(String coreName,
                                     List<String> ids,
                                     List<DictionaryMongo> content,
                                     String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            ReplaceOptions updateOptions = new ReplaceOptions().upsert(true);
            MongoCollection<Document> collection = db.getCollection(collectionName);
            for (DictionaryMongo entry : content) {
                Document doc = entry.toDocument();
                collection.replaceOne(new BasicDBObject(MONGO_ID_KEY, doc.get(MONGO_ID_KEY)), doc, updateOptions);
            }
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void deleteDictionaryData(String coreName,
                                     List<String> docIds,
                                     String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);

            if (isNotEmpty(docIds)) {
                List<ObjectId> documentIds = getDocumentIds(docIds);
                collection.deleteMany(in(MONGO_ID_KEY, documentIds));
            }

        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<DictionaryMongo> searchDictionaryData(int page,
                                                      int count,
                                                      String coreName,
                                                      String dictionaryKey,
                                                      String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            List<DictionaryMongo> content = new ArrayList<>();
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);
            FindIterable<Document> documents = collection.find((Bson) buildPrefixMatchQuery(dictionaryKey))
                    .sort(orderBy(descending(TOTAL_NUMBER_OF_BEST_QUERIES, TOTAL_NUMBER_OF_QUERIES, MONGO_ID_KEY)))
                    .skip(getElementsToSkip(page, count))
                    .limit(count);

            for (Document document : documents) {
                content.add(getInstance(document));
            }
            return content;
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<DictionaryMongo> getDictionaryData(int page,
                                                   int count,
                                                   String coreName,
                                                   String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }
        try {
            List<DictionaryMongo> entries = new ArrayList<>();
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);
            FindIterable<Document> documents = collection.find()
                    .projection(fields(include(MONGO_ID_KEY, ASSET_DATA_KEY)))
                    .sort(orderBy(descending(TOTAL_NUMBER_OF_BEST_QUERIES, TOTAL_NUMBER_OF_QUERIES, MONGO_ID_KEY)))
                    .skip(getElementsToSkip(page, count))
                    .limit(count);

            for (Document document : documents) {
                entries.add(getInstance(document));
            }
            return entries;
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public File downloadDictionaryData(String coreName,
                                       boolean includeId,
                                       String dictionaryName,
                                       String qualifiedDictionaryName)
            throws DAOException, IOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }
        File file = getFileHandle(dictionaryName);
        FileWriter fileWriter = getFileWriter(file);
        AssetTransformer transformer = transformerMap.get(dictionaryName);
        MongoCollection<Document> collection = mongo.getDatabase(coreName)
                .getCollection(qualifiedDictionaryName + ASSET_SUFFIX);

        if (includeId) { transformer.addIDInCSV(); }
        writeData(fileWriter, transformer, collection);
        fileWriter.close();
        return file;
    }

    private void writeData(FileWriter fileWriter,
                           AssetTransformer transformer,
                           MongoCollection<Document> collection)
            throws DAOException, IOException {
        List<DictionaryMongo> entries = new ArrayList<>();
        writeHeader(transformer.getCSVHeader(), fileWriter);

        try (MongoCursor<Document> iterator = collection
                .find().projection(fields(include(MONGO_ID_KEY,
                        ASSET_DATA_KEY))).iterator()) {

            int count = 1;
            while (iterator.hasNext()) {
                if (count % 1000 == 0) {
                    toFile(fileWriter, transformer,
                            entries);
                    entries.clear();
                }
                entries.add(getInstance(iterator
                        .next()));
                count++;
            }
            toFile(fileWriter, transformer, entries);
        } catch (MongoException e) {
            fileWriter.close();
            throw new DAOException(e);
        }
    }

    private File getFileHandle(String dictionaryName) throws IOException {
        return createTempFile(dictionaryName, ".csv");
    }

    private void writeHeader(String csvHeader,
                             FileWriter fileWriter) throws IOException {
        fileWriter.write(csvHeader);
    }

    private FileWriter getFileWriter(File file) throws IOException {
        return new FileWriter(file);
    }

    private void toFile(FileWriter fileWriter,
                        AssetTransformer transformer,
                        List<DictionaryMongo> entries)
            throws IOException {
            fileWriter.write(NEWLINE_DELIMITER);
            fileWriter.write(transformer.toCSV(entries));
    }

    @Override
    public List<DictionaryMongo> getDictionaryData(String coreName,
                                                   String dictionaryName,
                                                   List<String> docIds) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            List<DictionaryMongo> content = new ArrayList<>();
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);

            if (isNotEmpty(docIds)) {
                List<ObjectId> documentIds = getDocumentIds(docIds);
                FindIterable<Document> documents = collection.find(in(MONGO_ID_KEY, documentIds));
                for (Document document : documents) {
                    content.add(getInstance(document));
                }
            }
            return content;
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public DictionaryAnalysis getAnalysisOfDictionary(String siteKey, String dictionaryName,
                                                      String analysisType, String id,
                                                      int page, int count,
                                                      String sortBy, DictionaryContext.ORDER_BY sortOrder) {

        String nameOfSizeOfBad = "sizeOf" + BAD;
        String nameOfSizeOfGood = "sizeOf" + GOOD;
        String nameOfSizeOfBest = "sizeOf" + BEST;
        MongoDatabase db = mongo.getDatabase(siteKey);
        String collectionName = dictionaryName + ASSET_SUFFIX;
        MongoCollection<Document> collection = db.getCollection(collectionName);
        Document doc = collection.aggregate(analyisQuery(id, analysisType, page, count, sortBy, sortOrder)).first();
        if (doc == null || doc.isEmpty()) {
            return null;
        }
        return new DictionaryAnalysis(
                new DictionaryAnalysisSegment(doc.getInteger(nameOfSizeOfGood), (ArrayList) doc.get(GOOD)),
                new DictionaryAnalysisSegment(doc.getInteger(nameOfSizeOfBad), (ArrayList) doc.get(BAD)),
                new DictionaryAnalysisSegment(doc.getInteger(nameOfSizeOfBest), (ArrayList) doc.get(BEST)));
    }

    /**
     * Construct the following query
     * db["synonyms-ai.txt"].aggregate([{$match: {"_id":ObjectId("61b85a4f000b250008ff8ae7")}}, {$unwind: "$bad"},
     * {$group:{   "_id": "$_id", "bad": {$push : "$bad"}, "sizeOfbad": {$sum: 1}, "good": {$first : "$good"}  } },
     * {$unwind: "$good"},
     * {$group:{   "_id": "$_id", "bad": {$first :"$bad"}, "sizeOfbad": {$first :"$sizeOfbad"},
     * "good": {$push : "$good"}, "sizeOfGood": {$sum: 1},  } } ])
     *
     * @param id
     * @param analysisType
     * @param page
     * @param count
     * @param sortBy
     * @param sortOrder
     * @return
     */
    protected List<Bson> analyisQuery(String id, String analysisType, int page, int count,
                                      String sortBy, DictionaryContext.ORDER_BY sortOrder) {
        String nameOfSizeOfBad = "sizeOf" + BAD;
        String nameOfSizeOfGood = "sizeOf" + GOOD;
        String nameOfSizeOfBest = "sizeOf" + BEST;
        String MONGO_PREPAND_OPERATOR = "$";
        String sliceOperator = "$slice";
        int skip = getElementsToSkip(page, count);
        List<Bson> aggregateQuery = null;
        if (analysisType == null) {
            aggregateQuery = new ArrayList<>(Arrays.asList(
                    Aggregates.match(eq(MONGO_ID_KEY, new ObjectId(id))),
                    Aggregates.unwind(MONGO_PREPAND_OPERATOR + BAD,
                            new UnwindOptions().preserveNullAndEmptyArrays(Boolean.TRUE)),
                    Aggregates.group(MONGO_PREPAND_OPERATOR + MONGO_ID_KEY,
                            Accumulators.push(BAD, MONGO_PREPAND_OPERATOR + BAD),
                            Accumulators.sum(nameOfSizeOfBad, 1),
                            Accumulators.first(GOOD, MONGO_PREPAND_OPERATOR + GOOD),
                            Accumulators.first(BEST, MONGO_PREPAND_OPERATOR + BEST)),
                    Aggregates.unwind(MONGO_PREPAND_OPERATOR + GOOD,
                            new UnwindOptions().preserveNullAndEmptyArrays(Boolean.TRUE)),
                    Aggregates.group(MONGO_PREPAND_OPERATOR + MONGO_ID_KEY,
                            Accumulators.push(GOOD, MONGO_PREPAND_OPERATOR + GOOD),
                            Accumulators.sum(nameOfSizeOfGood, 1),
                            Accumulators.first(BAD, MONGO_PREPAND_OPERATOR + BAD),
                            Accumulators.first(nameOfSizeOfBad, MONGO_PREPAND_OPERATOR + nameOfSizeOfBad),
                            Accumulators.first(BEST, MONGO_PREPAND_OPERATOR + BEST)),
                    Aggregates.unwind(MONGO_PREPAND_OPERATOR + BEST,
                            new UnwindOptions().preserveNullAndEmptyArrays(Boolean.TRUE)),
                    Aggregates.group(MONGO_PREPAND_OPERATOR + MONGO_ID_KEY,
                            Accumulators.push(BEST, MONGO_PREPAND_OPERATOR + BEST),
                            Accumulators.sum(nameOfSizeOfBest, 1),
                            Accumulators.first(BAD, MONGO_PREPAND_OPERATOR + BAD),
                            Accumulators.first(nameOfSizeOfBad, MONGO_PREPAND_OPERATOR + nameOfSizeOfBad),
                            Accumulators.first(GOOD, MONGO_PREPAND_OPERATOR + GOOD),
                            Accumulators.first(nameOfSizeOfGood, MONGO_PREPAND_OPERATOR + nameOfSizeOfGood))));
        } else {
            aggregateQuery = new ArrayList<>(Arrays.asList(
                    Aggregates.match(eq(MONGO_ID_KEY, new ObjectId(id))),
                    Aggregates.unwind(MONGO_PREPAND_OPERATOR + analysisType,
                            new UnwindOptions().preserveNullAndEmptyArrays(Boolean.TRUE))));
            if (sortBy != null) {
                String fieldName = analysisType + "." + sortBy;
                Bson sortCond = (sortOrder == ASC) ? ascending(fieldName) :
                        descending(fieldName);
                aggregateQuery.add(Aggregates.sort(sortCond));
            }
            aggregateQuery.add(Aggregates.group(MONGO_PREPAND_OPERATOR + MONGO_ID_KEY,
                    Accumulators.push(analysisType, MONGO_PREPAND_OPERATOR + analysisType),
                    Accumulators.sum("sizeOf" + analysisType, 1)));
        }
        aggregateQuery.add(Aggregates.project(
                fields(include(MONGO_ID_KEY, nameOfSizeOfBad, nameOfSizeOfGood, nameOfSizeOfBest),
                        new Document(GOOD, new Document(sliceOperator,
                                Arrays.asList(MONGO_PREPAND_OPERATOR + GOOD, skip, count))),
                        new Document(BAD, new Document(sliceOperator,
                                Arrays.asList(MONGO_PREPAND_OPERATOR + BAD, skip, count))),
                        new Document(BEST, new Document(sliceOperator,
                                Arrays.asList(MONGO_PREPAND_OPERATOR + BEST, skip, count))))));
        return aggregateQuery;
    }

    protected List<String> getList(BasicDBList list) {
        if (list == null || list.size() == 0)
            return Collections.emptyList();
        List<String> values = new ArrayList<>(list.size());
        for (Object element : list) {
            values.add((String) element);
        }
        return values;
    }

    @Override
    public long getCount(String coreName,
                         String dictionaryKey,
                         String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            MongoCollection<Document> collection = db.getCollection(collectionName);
            return collection.count((Bson) buildPrefixMatchQuery(dictionaryKey));

        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<String> searchDictionary(String coreName,
                                         String fieldName,
                                         List<String> content,
                                         List<String> dictionaryNames) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            List<String> results = new ArrayList<>();
            MongoDatabase db = mongo.getDatabase(coreName);
            for (String dictionaryName : dictionaryNames) {
                String collectionName = dictionaryName + ASSET_SUFFIX;
                MongoCollection<Document> collection = db.getCollection(collectionName);
                collection.createIndex(Indexes.text(fieldName), new IndexOptions().defaultLanguage("none"));

                for (String entry : content) {
                    FindIterable<Document> documents = collection.find(Filters.text(entry));
                    if (documents.iterator().hasNext()) {
                        results.add(entry);
                    }
                }
            }
            return results;
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Set<String> findMatchesForDictionary(String coreName,
                                                String fieldName,
                                                List<String> content,
                                                List<String> dictionaryNames) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            Set<String> results = new HashSet<>();
            MongoDatabase db = mongo.getDatabase(coreName);
            for (String dictionaryName : dictionaryNames) {
                String collectionName = dictionaryName + ASSET_SUFFIX;
                MongoCollection<Document> collection = db.getCollection(collectionName);
                collection.createIndex(Indexes.text(fieldName), new IndexOptions().defaultLanguage("none"));

                for (String entry : content) {
                    FindIterable<Document> documents = collection.find(Filters.text(entry));
                    documents.iterator().forEachRemaining(e -> results.add(e.getString(fieldName)));
                }
            }
            return results;
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void flushDictionary(String coreName,
                                String dictionaryName) throws DAOException {
        if (status == HEALTH_STATUS.FAILED || mongo == null) {
            throw new DAOException("Unable to connect to MongoDao");
        }

        try {
            MongoDatabase db = mongo.getDatabase(coreName);
            String collectionName = dictionaryName + ASSET_SUFFIX;
            db.getCollection(collectionName).deleteMany(new Document());

        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<DictionaryMongo> getFilteredData(int page,
                                                 int count,
                                                 String sitekey,
                                                 List<String> rowIds,
                                                 String dictionaryName)
            throws DAOException {
        List<DictionaryMongo> entries = new ArrayList<>();
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(format(SUGGESTED_TEMPLATE, dictionaryName));
        try (MongoCursor<Document> iterator = collection.find(in(MONGO_ID_KEY,
                getObjectIds(rowIds))).skip(getElementsToSkip(page,
                count)).limit(count).iterator()) {
            while (iterator.hasNext()) {
                entries.add(getInstance(iterator.next()));
            }
        } catch (MongoException e) {
            throw new DAOException(e);
        }
        return entries;
    }

    @Override
    public long countEntries(String sitekey,
                             String dictionaryName)
            throws DAOException {
        MongoCollection<Document> collection = mongo.getDatabase(sitekey)
                .getCollection(format(SUGGESTED_TEMPLATE, dictionaryName));
        try {
            return collection.countDocuments();
        } catch (MongoException e) {
            throw new DAOException(e);
        }
    }

    private List<ObjectId> getObjectIds(List<String> rowIds) {
        List<ObjectId> objectIds = new ArrayList<>();
        for (String rowId: emptyIfNull(rowIds)) {
            objectIds.add(new ObjectId(rowId));
        }
        return objectIds;
    }

    private List<Document> getAssetDocumentFromContent(List<String> content) {
        return content.stream()
                .map(data -> new Document(ASSET_DATA_KEY, data))
                .collect(Collectors.toList());
    }

    private int getElementsToSkip(int page, int count) {
        return page <= 1 ? 0 : (page - 1) * count;
    }

    private List<ObjectId> getDocumentIds(List<String> docIds) {
        List<ObjectId> objectIds = new ArrayList<>();
        for (String docId : docIds) {
            objectIds.add(new ObjectId(docId));
        }
        return objectIds;
    }

    private DBObject buildPrefixMatchQuery(String queryStr) {
        DBObject query = new BasicDBObject();
        DBObject queryAction = new BasicDBObject()
                .append("$options", 'i')
                .append("$regex", queryStr);
        query.put(ASSET_DATA_KEY, queryAction);
        return query;
    }

    private List<Document> getDocument(List<DictionaryMongo> content) {
        return content.stream()
                .map(DictionaryMongo::toDocument)
                .collect(Collectors.toList());
    }
}
