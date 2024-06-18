package com.unbxd.analyser.service;

import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.model.Concepts;
import com.unbxd.analyser.model.StopWords;
import com.unbxd.analyser.model.UpdateConceptsRequest;
import com.unbxd.analyser.model.UpdateStopWordsRequest;
import com.unbxd.analyser.model.core.CoreConfig;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryCount;
import com.unbxd.skipper.dictionary.model.DictionaryData;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface AnalyserService {
    Concepts getConcepts(String siteKey) throws AnalyserException;

    void updateConcepts(String siteKey, UpdateConceptsRequest request) throws AnalyserException;

    StopWords getStopWords(String sitKey) throws AnalyserException;

    void updateStopWords(String siteKey, UpdateStopWordsRequest request) throws AnalyserException;

    void createAnalyserCore(String siteKey) throws AnalyserException;

    List<String> getDefaultConcepts() throws AnalyserException;

    List<String> getDefaultStopWords() throws AnalyserException;

    void updateAsset(String siteKey, String assetName, String request) throws AnalyserException;

    void bulkUpdateAsset(String siteKey, String assetName, String type, String s3Location) throws AnalyserException;

    InputStream bulkDownloadAsset(String siteKey, String assetName) throws AnalyserException;

    String getVersion(String siteKey) throws AnalyserException;

    CoreConfig getConfig(String siteKey) throws AnalyserException;

    void updateConfig(String siteKey, CoreConfig coreConfig) throws AnalyserException;

    void updateVersion(String siteKey, String version) throws AnalyserException;

    String analyse(String siteKey, String query) throws AnalyserException;

    File download(String siteKey, String dictionaryName, String type, boolean isIdNeeded) throws AnalyserException;

    void addDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException;

    DictionaryData getDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException;

    void deleteDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException;

    void updateDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException;

    void bulkUploadData(DictionaryContext dictionaryContext) throws AnalyserException;

    DictionaryCount getDictionaryCount(DictionaryContext dictionaryContext) throws AnalyserException;
}
