package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.unbxd.lucene.analysis.en.PorterStemmer;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.Query;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.EMPTY_FILE;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.logging.log4j.core.util.Assert.isEmpty;

/**
 * Asset & Dictionary literally mean the same wrt. this class and it's implementations;
 * the difference in nomenclature exists to demarcate between existing functionality of
 * assets and those that are being added to support additional dictionary operations
 */

public interface AssetTransformer {

    String ID_DELIMITER = "\\#";
    String COMMA_DELIMITER = ",";
    String NEWLINE_DELIMITER = "\n";

    String KEYWORD = "Keyword";
    String SYNONYMS = "synonyms";
    String STEMDICT = "stemdict";
    String STOPWORDS = "stopwords";
    String MANDATORY = "mandatory";
    String MULTIWORDS = "multiwords";
    String SYNONYMS_V2 = "synonymsV2";
    String ASCII_MAPPING = "asciiMapping";
    String EXCLUDE_TERMS = "excludeTerms";
    String BIDIRECTIONAL = "Bidirectional";
    String UNIDIRECTIONAL = "Unidirectional";
    String GOOD = "Good";
    String BAD = "Bad";
    String OK = "Ok";
    String ID = "Id";
    String BEST = "Best";
    String DELETE = "delete";
    String UPDATE = "update";

    PorterStemmer stemmer = new PorterStemmer();

    String getDelimiter();

    String getCSVHeader();

    DictionaryEntry toEntry(DictionaryMongo data);

    String getData(DictionaryEntry entry);

    default DictionaryMongo toDBformat(DictionaryEntry entry) {
        DictionaryMongo result = new DictionaryMongo();
        result.setId(entry.getId());
        result.setData(getData(entry));
        return result;
    }

    List<String> fromCSV(String csvData) throws AssetException;

    String toCSV(DictionaryMongo dictionaryData) throws AssetException;

    default String toCSV(List<DictionaryMongo> dictionaryData) throws AssetException {
        List<String> csvRows = new ArrayList<>(dictionaryData.size());

        for (DictionaryMongo entry: dictionaryData) {
            csvRows.add(toCSV(entry));
        }
        return String.join(NEWLINE_DELIMITER, csvRows);
    }

    List<DictionaryEntry> fromCSVToEntry(String csvData) throws AssetException;

    // TODO: 28/03/22 Remove this method.
    void addIDInCSV();

    default List<DictionaryEntry> toEntries(List<DictionaryMongo> data) {
        if (isEmpty(data)) { return Collections.emptyList(); }
        List<DictionaryEntry> entries = new ArrayList<>();
        for (DictionaryMongo eachData: data) { entries.add(toEntry(eachData)); }
        return entries;
    }

    default List<String> fromEntries(List<DictionaryEntry> entries) {
        if (isEmpty(entries)) { return Collections.emptyList(); }
        List<String> strings = new ArrayList<>();

        for (DictionaryEntry entry: entries) { strings.add(getData(entry)); }
        return strings;
    }

    default List<DictionaryMongo> toDBformat(List<DictionaryEntry> entries) {
        if (isEmpty(entries)) { return Collections.emptyList(); }
        List<DictionaryMongo> result = new ArrayList<>();

        for (DictionaryEntry entry: entries) { result.add(toDBformat(entry)); }
        return result;
    }

    MappingIterator getMappingIterator(File csvData);

    DictionaryEntry fromCSV(CSVData csv);

    List<String> getRootFromCSV(String csvData) throws AssetException;

    default List<String> getRootWords(List<DictionaryEntry> entries) {
        List<String> rootWords = new ArrayList<>();
        for (DictionaryEntry entry : entries) {
            rootWords.add(deleteWhitespace(stemmer.stem(entry.getName())));
        }
        return rootWords;
    }

    default void validateEmptyContent(MappingIterator<?> iterator) throws AssetException {
        if (!iterator.hasNext()) { throw new AssetException("The uploaded file is empty." +
                " Please try uploading it in CSV format.", EMPTY_FILE.getCode());
        }
    }

    default List<Query> getQueries(String csv) {
        List<Query> queries = (csv != null && !csv.isEmpty())?
                Stream.of(csv.split(COMMA_DELIMITER)).
                        filter(string -> !string.isEmpty()).
                        map(String::trim).
                        map(string -> new Query(string)).
                        collect(Collectors.toList()):
                new ArrayList<>();

        if (queries.size() > 0) {
            return queries;
        }
        return null;
    }

    default Map<String,List<DictionaryEntry>> handleBlacklist(List<DictionaryEntry> aiDictionary,
                                                              List<DictionaryEntry> blackListDictionary) {
        Map<String,List<DictionaryEntry>> result = new HashMap<>(blackListDictionary.size());
        result.put(DELETE,new ArrayList<>());
        result.put(UPDATE,new ArrayList<>());
        Map<String, DictionaryEntry> blackListMap = new HashMap<>(blackListDictionary.size());
        blackListDictionary.forEach(dictionaryEntry -> blackListMap.put(dictionaryEntry.getName(),dictionaryEntry));
        for (DictionaryEntry entry : aiDictionary) {
            if(blackListMap.containsKey(entry.getName()))
                result.get(DELETE).add(entry);
        }
        return result;
    }

    default List<DictionaryEntry> appendData(List<DictionaryEntry> currentList,
                                             List<DictionaryEntry> newList) {
        // default impl: return entries which does exists in currentList
        List<DictionaryEntry> result = new ArrayList<>();
        Set<String> currentKeywords = new HashSet<>(currentList.size());
        currentList.forEach(dictionaryEntry -> currentKeywords.add(dictionaryEntry.getName()));
        for (DictionaryEntry entry :newList) {
            if(!currentKeywords.contains(entry.getName())) result.add(entry);
        }
        return result;
    }

    /** returns values present in entry1 and not in entry2
     *  returns null if not applicable to specific dictionary type
     * */
    default DictionaryEntry getMissingData(DictionaryEntry entry1, DictionaryEntry entry2) {
        return null;
    }
}

