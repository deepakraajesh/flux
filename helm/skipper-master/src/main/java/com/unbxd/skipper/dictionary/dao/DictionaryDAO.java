package com.unbxd.skipper.dictionary.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryAnalysis;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface DictionaryDAO {

    void deleteCore(String corename) throws DAOException;

    /* Dictionary DAO */
    void addDictionaryData(String coreName,
                           List<DictionaryMongo> content,
                           String dictionaryName) throws DAOException;

    void addDictionaryData(String coreName,
                           String dictionaryName,
                           DictionaryMongo data) throws DAOException;

    void updateDictionaryData(String coreName,
                              List<String> ids,
                              List<DictionaryMongo> content,
                              String dictionaryName) throws DAOException;

    void deleteDictionaryData(String coreName,
                              List<String> docIds,
                              String dictionaryName) throws DAOException;

    List<DictionaryMongo> searchDictionaryData(int page,
                                               int count,
                                               String coreName,
                                               String dictionaryKey,
                                               String dictionaryName) throws DAOException;

    List<DictionaryMongo> getDictionaryData(String coreName,
                                            String dictionaryName,
                                            List<String> docIds) throws DAOException;

    List<DictionaryMongo> getDictionaryData(int page,
                                            int count,
                                            String coreName,
                                            String dictionaryName) throws DAOException;

    File downloadDictionaryData(String coreName,
                                boolean includeId,
                                String dictionaryName,
                                String qualifiedDictionaryName) throws DAOException, IOException;

    long countEntries(String sitekey,
                      String dictionaryName) throws DAOException;

    List<DictionaryMongo> getFilteredData(int page,
                                          int count,
                                          String sitekey,
                                          List<String> rowIds,
                                          String dictionaryName) throws DAOException;

    long getCount(String coreName,
                  String dictionaryKey,
                  String dictionaryName) throws DAOException;

    List<String> searchDictionary(String coreName,
                                  String fieldName,
                                  List<String> content,
                                  List<String> dictionaryNames) throws DAOException;

    Set<String> findMatchesForDictionary(String coreName,
                                         String fieldName,
                                         List<String> content,
                                         List<String> dictionaryNames) throws DAOException;

    void flushDictionary(String coreName,
                         String dictionaryName) throws DAOException;

    DictionaryAnalysis getAnalysisOfDictionary(String siteKey, String dictionaryName,
                                               String analysisType, String id,
                                               int page, int count, String sortBy, DictionaryContext.ORDER_BY sortOrder);


    public static class AssetDAOProvider implements Provider<DictionaryDAO> {
        DictionaryDAO dao;

        @Inject
        public AssetDAOProvider(MongoDictionaryDao mongoDao) {
            this.dao = mongoDao;
        }

        @Override
        public DictionaryDAO get() {
            return dao;
        }
    }
}
