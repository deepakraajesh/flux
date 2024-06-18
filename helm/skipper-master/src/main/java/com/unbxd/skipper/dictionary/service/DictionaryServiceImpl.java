package com.unbxd.skipper.dictionary.service;

import com.amazonaws.services.s3.AmazonS3URI;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.google.common.collect.Lists;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.recommend.dao.ContentDao;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.dictionary.dao.DictionaryDAO;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.filter.FilterService;
import com.unbxd.skipper.dictionary.knowledgeGraph.service.KnowledgeGraphService;
import com.unbxd.skipper.dictionary.model.*;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.transformer.AssetTransformer;
import com.unbxd.skipper.dictionary.validator.AssetValidatorHelper;
import com.unbxd.skipper.dictionary.validator.ValidatorService;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.unbxd.skipper.dictionary.model.DictionaryType.bck;
import static com.unbxd.skipper.dictionary.model.DictionaryType.suggested;
import static com.unbxd.skipper.dictionary.service.Constants.*;
import static com.unbxd.skipper.dictionary.validator.AssetValidator.ASSETS;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.DATABASE_ERROR;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
public class DictionaryServiceImpl implements DictionaryService {
    /* Dictionary Data Methods */

    private ValidatorService validatorService;
    private AssetValidatorHelper assetValidatorHelper;
    private Map<String, AssetTransformer> transformerMap;
    private DictionaryDAO mongoDao;
    private FilterService filterService;
    private AnalyserService analyserService;
    private static final String S3_REGION = "s3.region";
    private KnowledgeGraphService knowledgeGraphService;
    private AmazonS3Client amazonS3Client;

    private ContentDao contentDao;

    @Inject
    public DictionaryServiceImpl(ValidatorService validatorService,
                                 AssetValidatorHelper assetValidatorHelper,
                                 DictionaryDAO dictionaryDAO,
                                 FilterService filterService,
                                 Map<String, AssetTransformer> transformerMap,
                                 AnalyserService analyserService,
                                 KnowledgeGraphService knowledgeGraphService,
                                 AmazonS3Client amazonS3Client,
                                 ContentDao contentDao) {
        this.validatorService = validatorService;
        this.assetValidatorHelper = assetValidatorHelper;
        this.mongoDao = dictionaryDAO;
        this.contentDao = contentDao;
        this.transformerMap = transformerMap;
        this.filterService = filterService;
        this.analyserService = analyserService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.amazonS3Client = amazonS3Client;
    }

    @Override
    public void addDictionaryData(DictionaryContext dictionaryContext) throws AssetException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {
            validatorService.validate(dictionaryContext);
            AssetTransformer assetTransformer = transformerMap.get(dictionaryContext.getDictionaryName());
            List<DictionaryMongo> content = assetTransformer.toDBformat(dictionaryContext.getDictionaryData()
                    .getEntries());

            mongoDao.addDictionaryData(getVersionedCoreName(dictionaryContext), content,
                    dictionaryContext.getQualifiedDictionaryName());
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public void deleteDictionaryData(DictionaryContext dictionaryContext) throws AssetException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {

            mongoDao.deleteDictionaryData(getVersionedCoreName(dictionaryContext), dictionaryContext
                    .getDictionaryData().entryIds(), dictionaryContext.getQualifiedDictionaryName());
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public void updateDictionaryData(DictionaryContext dictionaryContext) throws AssetException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {
            AssetTransformer assetTransformer = transformerMap.get(dictionaryContext.getDictionaryName());
            List<DictionaryMongo> content = assetTransformer.toDBformat(dictionaryContext.getDictionaryData()
                    .getEntries());

            validatorService.validate(dictionaryContext);
            mongoDao.updateDictionaryData(getVersionedCoreName(dictionaryContext), dictionaryContext
                    .getDictionaryData().entryIds(), content, dictionaryContext
                    .getQualifiedDictionaryName());
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public DictionaryData getDictionaryData(DictionaryContext dictionaryContext) throws AssetException {
        if (isNotEmpty(dictionaryContext.getSearch())) {
            return searchDictionaryData(dictionaryContext);
        }
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {
            long total = mongoDao.getCount(getVersionedCoreName(dictionaryContext), EMPTY,
                    dictionaryContext.getQualifiedDictionaryName());
            long unbxdCount = mongoDao.getCount(getVersionedCoreName(dictionaryContext), EMPTY,
                    dictionaryContext.getDictionaryName().concat("-ai"));
            DictionaryData dictionaryData = DictionaryData.getInstance(dictionaryContext.getPage(),
                    dictionaryContext.getCount(), total, unbxdCount);
            List<DictionaryMongo> dictionaryList = mongoDao.getDictionaryData(dictionaryContext.getPage(),
                    dictionaryContext.getCount(), getVersionedCoreName(dictionaryContext),
                    dictionaryContext.getQualifiedDictionaryName());
            List<DictionaryEntry> entries = transformerMap.get(dictionaryContext.getDictionaryName())
                    .toEntries(dictionaryList);

            dictionaryData.setEntries(entries);
            return dictionaryData;
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public DictionaryData searchDictionaryData(DictionaryContext dictionaryContext) throws AssetException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {
            long total = mongoDao.getCount(getVersionedCoreName(dictionaryContext),
                    dictionaryContext.getSearch(), dictionaryContext.getQualifiedDictionaryName());
            long unbxdCount = mongoDao.getCount(getVersionedCoreName(dictionaryContext), EMPTY,
                    dictionaryContext.getDictionaryName().concat("-ai"));
            DictionaryData dictionaryData = DictionaryData.getInstance(dictionaryContext.getPage(),
                    dictionaryContext.getCount(), total, unbxdCount);
            List<DictionaryMongo> dictionaryList = mongoDao.searchDictionaryData(dictionaryContext.getPage(),
                    dictionaryContext.getCount(), getVersionedCoreName(dictionaryContext),
                    dictionaryContext.getSearch(), dictionaryContext.getQualifiedDictionaryName());
            List<DictionaryEntry> entries = transformerMap.get(dictionaryContext.getDictionaryName())
                    .toEntries(dictionaryList);

            dictionaryData.setEntries(entries);
            return dictionaryData;
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public DictionaryAnalysis getAnalysisOfDictionary(DictionaryContext context, String id, String analysisType) {
        return mongoDao.getAnalysisOfDictionary(context.getSiteKey(), context.getQualifiedDictionaryName(),
                analysisType, id, context.getPage(), context.getCount(), context.getSortBy(), context.getSortOrder());
    }

    @Override
    public void delete(String siteKey) throws AssetException {
        try {
            mongoDao.deleteCore(siteKey);
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public void bulkUploadDictionary(DictionaryContext dictionaryContext) throws AnalyserException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        flushDictionary(dictionaryContext);
        DictionaryData dictionaryData = new DictionaryData();
        dictionaryContext.setDictionaryData(dictionaryData);
        File file = analyserService.download(dictionaryContext.getSiteKey(),
                dictionaryContext.getDictionaryName(), dictionaryContext
                        .getType().name(), Boolean.TRUE);
        bulkUploadDictionary(file, dictionaryContext);
    }

    @Override
    public void bulkUploadDictionary(File file, DictionaryContext dictionaryContext) throws AssetException {
        AssetTransformer assetTransformer = transformerMap.get(dictionaryContext.getDictionaryName());
        MappingIterator<CSVData> dictionaryDataIterator = assetTransformer.getMappingIterator(file);
        dictionaryContext.getDictionaryData().setOmissions(Lists.newArrayList());
        int batch = 100;
        dictionaryContext.getDictionaryData().setEntries(new ArrayList<>(batch));
        flushDictionary(dictionaryContext);
        int index = 0;
        try {
            while (dictionaryDataIterator.hasNext()) {
                index = dictionaryDataIterator.getCurrentLocation().getLineNr();
                dictionaryContext.getDictionaryData().getEntries().
                        add(assetTransformer.fromCSV(dictionaryDataIterator.next()));
                if (index % batch == 0) {
                    addDictionary(dictionaryContext);
                    dictionaryContext.getDictionaryData().setEntries(new ArrayList<>());
                }
            }
            addDictionary(dictionaryContext);
        } catch (RuntimeJsonMappingException|IllegalArgumentException e) {
            String msg = "Unable to parse CSV file[line:"
                    + (index) + "]. Please follow the csv format while" +
                    " uploading: " + e.getMessage();
            log.error(msg);
            throw new AssetException(msg, CSV_TRANSFORM_ERROR.getCode());
        } catch (AssetException e) {
            String msg = e.getMessage() + " at line #: " + (index);
            log.error(msg);
            throw e;
        }
    }

    @Override
    public File bulkDownloadDictionary(DictionaryContext dictionaryContext) throws AssetException {
        assetValidatorHelper.validate(ASSETS, dictionaryContext.getDictionaryName());
        try {
            return mongoDao.downloadDictionaryData(getVersionedCoreName
                    (dictionaryContext), dictionaryContext.isGetId(), dictionaryContext
                    .getDictionaryName(), dictionaryContext.getQualifiedDictionaryName());
        } catch (DAOException | IOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    private String fetchContentFromBucket(String bucketPath) throws AssetException {
        AmazonS3URI amazonS3URI = new AmazonS3URI(bucketPath);
        return amazonS3Client.getClient().getObjectAsString(amazonS3URI.getBucket(), amazonS3URI.getKey());
    }

    private void flushDictionary(DictionaryContext dictionaryContext) throws AssetException {
        try {
            if (dictionaryContext.isFlushAll()) {
                mongoDao.flushDictionary(getVersionedCoreName(dictionaryContext),
                        dictionaryContext.getQualifiedDictionaryName());
            }
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    /**
     * Updates existing blacklist entry , does not creates new entry.
     * Appends deleted blacklist entry values to ai entries (this applies only to synonyms & exclude terms).
     *
     * @param dictionaryContext contains blacklist entries
     */
    @Override
    public void updateBlackList(DictionaryContext dictionaryContext) throws AssetException {
        try {
            // update only entries which are already present in the db
            List<DictionaryEntry> updateList = dictionaryContext.getDictionaryData().getEntries();
            List<String> keywords = new ArrayList<>(updateList.size());
            updateList.forEach(entry -> keywords.add(entry.getName()));

            List<DictionaryMongo> dictionaryList = searchKeywords(dictionaryContext, keywords);
            AssetTransformer transformer = transformerMap.get(dictionaryContext.getDictionaryName());
            List<DictionaryEntry> currentBlackList = transformer.toEntries(dictionaryList);
            Map<String, DictionaryEntry> currentMap = new HashMap<>(currentBlackList.size());
            currentBlackList.forEach(entry -> currentMap.put(entry.getName(), entry));
            updateList.removeIf(entry -> !currentMap.containsKey(entry.getName()));
            updateList.forEach(entry -> entry.setId(currentMap.get(entry.getName()).getId()));

            List<String> deleteIds = new ArrayList<>();

            // in case of synonyms and exclude terms (dictionary types which holds multiple values per entry)
            // few terms can be deleted during update, these deleted terms should be added back to Ai dictionary

            List<DictionaryEntry> deletedValues = new ArrayList<>();
            for (DictionaryEntry newEntry : updateList) {
                DictionaryEntry currentEntry = currentMap.get(newEntry.getName());
                if (!newEntry.isEqual(currentEntry)) {
                    DictionaryEntry deletedValue = transformer.getMissingData(currentEntry, newEntry);
                    if (nonNull(deletedValue)) {
                        deletedValues.add(deletedValue);
                        // consider there is a blacklist entry 'a -> x,y'
                        // and user wants to remove both x and y then user sends `a -> null` to asterix
                        // in this case we have to delete the whole entry
                        if (currentEntry.isEqual(deletedValue))
                            deleteIds.add(currentEntry.getId());
                    }
                }
            }
            updateList.removeIf(entry -> deleteIds.contains(entry.getId()));
            mongoDao.updateDictionaryData(
                    dictionaryContext.getVersionedCoreName(),
                    null,
                    transformer.toDBformat(updateList),
                    dictionaryContext.getQualifiedDictionaryName());

            mongoDao.deleteDictionaryData(
                    dictionaryContext.getVersionedCoreName(),
                    deleteIds,
                    dictionaryContext.getQualifiedDictionaryName());

            dictionaryContext.getDictionaryData().setEntries(deletedValues);
            appendAiDictionary(dictionaryContext);
        } catch (DAOException | AnalyserException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    private List<DictionaryMongo> searchKeywords(DictionaryContext dictionaryContext,
                                                 List<String> keywords) throws DAOException {
        List<DictionaryMongo> dictionaryList = new ArrayList<>();
        for (String keyword : keywords) {
            dictionaryList.addAll(
                    mongoDao.searchDictionaryData(1,
                            100,
                            getVersionedCoreName(dictionaryContext),
                            keyword,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(),
                                    dictionaryContext.getType().toString()))
            );
        }
        return dictionaryList;
    }

    private void appendAiDictionary(DictionaryContext dictionaryContext) throws AssetException, AnalyserException {
        try {
            List<DictionaryEntry> blackListEntries = dictionaryContext.getDictionaryData().getEntries();
            List<String> keywords = new ArrayList<>(blackListEntries.size());
            blackListEntries.forEach(entry -> keywords.add(entry.getName()));
            // set type =ai
            dictionaryContext.setType(DictionaryType.ai);
            List<DictionaryMongo> dictionaryList = searchKeywords(dictionaryContext, keywords);

            AssetTransformer transformer = transformerMap.get(dictionaryContext.getDictionaryName());
            List<DictionaryEntry> AiDictionaryList = transformer.toEntries(dictionaryList);
            List<DictionaryEntry> appendedData = transformer.appendData(AiDictionaryList, blackListEntries);
            List<DictionaryMongo> updateList = transformer.toDBformat(appendedData);
            mongoDao.updateDictionaryData(
                    dictionaryContext.getVersionedCoreName(),
                    null,
                    updateList,
                    getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), AI_TYPE_NAME));
            
            dictionaryContext.getDictionaryData().setEntries(appendedData);
            analyserService.updateDictionaryData(dictionaryContext);
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    /**
     * removes blacklisted entries from AI entries.
     *
     * @param dictionaryContext contains blacklist entries
     */

    private void blackListAIDictionary(DictionaryContext dictionaryContext,
                                       String dictionaryType) throws AssetException, AnalyserException {
        try {
            List<DictionaryEntry> blacklistEntries = dictionaryContext.getDictionaryData().getEntries();
            List<String> keywords = new ArrayList<>(blacklistEntries.size());
            blacklistEntries.forEach(entry -> keywords.add(entry.getName()));
            dictionaryContext.setType(DictionaryType.valueOf(dictionaryType));
            List<DictionaryMongo> AIDictionaryList = searchKeywords(dictionaryContext, keywords);

            AssetTransformer transformer = transformerMap.get(dictionaryContext.getDictionaryName());

            Map<String, List<DictionaryEntry>> blacklistOutput =
                    transformer.handleBlacklist(transformer.toEntries(AIDictionaryList), blacklistEntries);
            List<DictionaryMongo> updateList = transformer.toDBformat(blacklistOutput.get("update"));
            List<DictionaryEntry> deleteList = blacklistOutput.get("delete");
            List<String> deleteIdList = new ArrayList<>();
            deleteList.forEach(entry -> deleteIdList.add(entry.getId()));

            for(DictionaryMongo data: updateList) {
                mongoDao.addDictionaryData(
                        dictionaryContext.getVersionedCoreName(),
                        getQualifiedDictionaryName(
                                dictionaryContext.getDictionaryName(),
                                dictionaryType), data);
            }

            mongoDao.deleteDictionaryData(
                    dictionaryContext.getVersionedCoreName(),
                    deleteIdList,
                    getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), dictionaryType));

            dictionaryContext.getDictionaryData().setEntries(blacklistOutput.get("update"));
            analyserService.updateDictionaryData(dictionaryContext);

            dictionaryContext.getDictionaryData().setEntries(blacklistOutput.get("delete"));
            analyserService.deleteDictionaryData(dictionaryContext);

        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    /**
     * Adds new entry in case not present.
     * In case of synonyms & exclude terms : if the entry is already present
     * then appends the new values to the existing entry.
     * Deletes given blacklisted values from ai dictionary.
     *
     * @param dictionaryContext contains blacklist entries
     */

    @Override
    public void appendBlackList(DictionaryContext dictionaryContext,
                                String dictionaryType) throws AssetException, AnalyserException{
        try {
            knowledgeGraphService.sendFeedback(dictionaryContext);
            List<DictionaryEntry> blacklistEntries = dictionaryContext.getDictionaryData().getEntries();
            List<String> keywords = new ArrayList<>(blacklistEntries.size());
            blacklistEntries.forEach(entry -> keywords.add(entry.getName()));

            List<DictionaryMongo> dictionaryList = searchKeywords(dictionaryContext, keywords);
            AssetTransformer transformer = transformerMap.get(dictionaryContext.getDictionaryName());
            List<DictionaryEntry> currentBlackList = transformer.toEntries(dictionaryList);
            List<DictionaryMongo> updateList = transformer.toDBformat(
                    transformer.appendData(currentBlackList, blacklistEntries)
            );
            mongoDao.updateDictionaryData(
                    dictionaryContext.getVersionedCoreName(),
                    null,
                    updateList,
                    dictionaryContext.getQualifiedDictionaryName());
            // remove blacklisted entries from AI or Suggested entries
            blackListAIDictionary(dictionaryContext, dictionaryType);
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public void deleteBlackList(DictionaryContext dictionaryContext) throws AssetException, AnalyserException{
        try {
            List<DictionaryMongo> dictionaryList = mongoDao.getDictionaryData(
                    getVersionedCoreName(dictionaryContext),
                    dictionaryContext.getQualifiedDictionaryName(),
                    dictionaryContext.getDictionaryData().entryIds()
            );
            List<DictionaryEntry> deleteEntries = transformerMap.get(dictionaryContext.getDictionaryName())
                    .toEntries(dictionaryList);
            deleteDictionaryData(dictionaryContext);
            // add deleted blacklist entries to AI set
            dictionaryContext.getDictionaryData().setEntries(deleteEntries);
            appendAiDictionary(dictionaryContext);
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public BlacklistReasonsData getBlacklistReasons(DictionaryContext dictionaryContext) throws AssetException {
        BlacklistReasonsData reasonsData = new BlacklistReasonsData();
        reasonsData.setReasons(
                DictionaryService.BLACKLIST_REASONS.getOrDefault(dictionaryContext.getDictionaryName(), new ArrayList<>())
        );
        return reasonsData;
    }

    private String getQualifiedDictionaryName(String dictionaryName, String type) {
        return DictionaryService.ALIAS_CONFIG.get(type).get(dictionaryName);
    }

    private String getVersionedCoreName(DictionaryContext dictionaryContext) {
        return dictionaryContext.getVersionedCoreName();
    }

    @Override
    public DictionaryCount getDictionaryCount(DictionaryContext dictionaryContext) throws AssetException {
        try {
            DictionaryCount result = new DictionaryCount();
            result.setFront(
                    mongoDao.getCount(
                            getVersionedCoreName(dictionaryContext),
                            EMPTY,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), FRONT_TYPE_NAME))
            );
            result.setBck(
                    mongoDao.getCount(
                            getVersionedCoreName(dictionaryContext),
                            EMPTY,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), BACK_TYPE_NAME))
            );
            result.setAi(
                    mongoDao.getCount(
                            getVersionedCoreName(dictionaryContext),
                            EMPTY,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), AI_TYPE_NAME))
            );
            result.setBlacklist(
                    mongoDao.getCount(
                            getVersionedCoreName(dictionaryContext),
                            EMPTY,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), BLACKLIST_TYPE_NAME))
            );
            result.setSuggested(
                    mongoDao.getCount(
                            getVersionedCoreName(dictionaryContext),
                            EMPTY,
                            getQualifiedDictionaryName(dictionaryContext.getDictionaryName(), SUGGESTED_TYPE_NAME))
            );
            return result;
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    @Override
    public void acceptSuggested(DictionaryContext dictionaryContext)
            throws AssetException{
        deleteDictionaryData(dictionaryContext);
        try {
            analyserService.deleteDictionaryData(dictionaryContext);
            dictionaryContext.setType(bck);
            analyserService.addDictionaryData(dictionaryContext);
            contentDao.deleteRowIds(dictionaryContext.getDictionaryData()
                    .entryIds().get(0), dictionaryContext.getSiteKey());
        } catch (AnalyserException e){
            dictionaryContext.setType(suggested);
            addDictionary(dictionaryContext);
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }

    private void addDictionary(DictionaryContext dictionaryContext) throws AssetException {
        AssetTransformer assetTransformer = transformerMap.get(dictionaryContext.getDictionaryName());
        try {
            filterService.filter(dictionaryContext);
            validatorService.validateSilently(dictionaryContext);

            for (DictionaryEntry entry : dictionaryContext.getDictionaryData().getEntries()) {
                String content = assetTransformer.getData(entry);
                DictionaryMongo data =  new DictionaryMongo(entry.getId(), content,
                        entry.getGood(), entry.getBad(), entry.getBest());
                if (entry.getId() != null) {
                    mongoDao.addDictionaryData(dictionaryContext.getSiteKey(),
                            dictionaryContext.getQualifiedDictionaryName(),
                            data);
                } else {
                    mongoDao.addDictionaryData(dictionaryContext.getSiteKey(),
                            Collections.singletonList(data), dictionaryContext.getQualifiedDictionaryName());
                }
            }
        } catch (DAOException e) {
            throw new AssetException(e.getMessage(), DATABASE_ERROR.getCode());
        }
    }
}
