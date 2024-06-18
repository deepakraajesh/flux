package com.unbxd.skipper.dictionary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.*;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.util.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DictionaryService {

    Map<String, Map<String, String>> ALIAS_CONFIG = getAliasJson();

    String ALIAS_CONFIG_NAME = "asset-alias.json";
    String BLACKLIST_REASONS_FILE = "blacklist-reasons.json";
    Map<String, List<BlackListReason>> BLACKLIST_REASONS = getBlacklistReasonsJson();

    static Map<String, Map<String, String>> getAliasJson() {
        try (InputStream stream = DictionaryService.class.getClassLoader()
                .getResourceAsStream(ALIAS_CONFIG_NAME)) {

            byte[] bytes = IoUtils.getBytes(stream);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, new TypeReference<>() {});

        } catch (IOException e){
            throw new PippoRuntimeException("Error while loading config validations.json :"+ e.getMessage());
        }
    }

    static Map<String, List<BlackListReason>> getBlacklistReasonsJson() {
        try (InputStream stream = DictionaryService.class.getClassLoader()
                .getResourceAsStream(BLACKLIST_REASONS_FILE)) {

            byte[] bytes = IoUtils.getBytes(stream);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, new TypeReference<>() {});

        } catch (IOException e){
            throw new PippoRuntimeException("Error while loading config blacklist-reasons.json :"+ e.getMessage());
        }
    }

    void addDictionaryData(DictionaryContext dictionaryContext) throws AssetException;

    void bulkUploadDictionary(DictionaryContext dictionaryContext) throws AnalyserException;

    void bulkUploadDictionary(File file, DictionaryContext dictionaryContext) throws AssetException;

    void deleteDictionaryData(DictionaryContext dictionaryContext) throws AssetException;

    void updateDictionaryData(DictionaryContext dictionaryContext) throws AssetException;

    void updateBlackList(DictionaryContext dictionaryContext) throws AssetException;

    BlacklistReasonsData getBlacklistReasons(DictionaryContext dictionaryContext) throws AssetException;

    File bulkDownloadDictionary(DictionaryContext dictionaryContext) throws AssetException;

    DictionaryData getDictionaryData(DictionaryContext dictionaryContext) throws AssetException;

    DictionaryData searchDictionaryData(DictionaryContext dictionaryContext) throws AssetException;

    void delete(String siteKey) throws AssetException;

    DictionaryAnalysis getAnalysisOfDictionary(DictionaryContext context, String id, String analysisType);

    DictionaryCount getDictionaryCount(DictionaryContext dictionaryContext) throws AssetException;

    void deleteBlackList(DictionaryContext dictionaryContext) throws AssetException, AnalyserException;

    void appendBlackList(DictionaryContext dictionaryContext, String dictionaryType) throws AssetException, AnalyserException;

    void acceptSuggested(DictionaryContext dictionaryContext) throws AssetException, AnalyserException;
}
