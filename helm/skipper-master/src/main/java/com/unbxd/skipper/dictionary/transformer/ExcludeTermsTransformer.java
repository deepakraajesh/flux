package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.j2objc.annotations.ObjectiveCName;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.model.csv.ExcludeTermsCSV;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.quote;

public class ExcludeTermsTransformer implements AssetTransformer {

    private CsvMapper csvMapper;
    private CsvSchema csvSchema;
    private final Pattern excludeTermsPattern;
    private static final String DELIMITER = "-";
    private static final String EXCLUSIONS = "Exclusions";
    private static final String PATTERN = "([a-z0-" +
            "9A-Z~@#\\^\\$&\\*\\(\\)_\\+=\\[\\]\\{\\}\\|\\,\\.\\?]|\\s)-";
    private boolean addIDInCSV = false;

    public ExcludeTermsTransformer() {
        csvMapper = new CsvMapper();
        csvSchema = CsvSchema.builder()
                .setStrictHeaders(Boolean.FALSE)
                .addColumn(KEYWORD)
                .addColumn(EXCLUSIONS)
                .addColumn(ID)
                .addColumn(GOOD)
                .addColumn(BAD)
                .addColumn(OK)
                .addColumn(BEST)
                .build().withHeader()
                .withEscapeChar('\\');
        excludeTermsPattern = Pattern.compile(PATTERN);
    }

    @Override
    public String getDelimiter() { return DELIMITER; }

    @Override
    public String getCSVHeader() {
        String result = String.join(COMMA_DELIMITER, KEYWORD, EXCLUSIONS);
        if (addIDInCSV) result = String.join(COMMA_DELIMITER, result, ID);
        return result;
    }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo model) {
        String data = model.getData();
        int index = getDelimiterIndex(data);
        DictionaryEntry entry = DictionaryEntry.getInstance(data
                .substring(0, index + 1));
        entry.setExcludeTerms(new ArrayList<>(asList(data.substring(index + 2)
                .split(COMMA_DELIMITER))));
        entry.setId(model.getId());
        entry.setGood(model.getGood());
        entry.setBad(model.getBad());
        entry.setBest(model.getBest());
        return entry;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        return entry.getName() + DELIMITER + String.join(COMMA_DELIMITER,
                entry.getExcludeTerms());
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        return null;
    }

    @Override
    public String toCSV(DictionaryMongo model) throws AssetException {
        String data = model.getData();
        int index = getDelimiterIndex(data);
        String result = StringUtils.join(data.substring(0, index + 1), COMMA_DELIMITER, "\"",
                data.substring(index + 2), "\"");
        if (this.addIDInCSV) result = StringUtils.join(result, COMMA_DELIMITER, model.getId());
        return result;
    }

    @Override
    public List<DictionaryEntry> fromCSVToEntry(String csvData) throws AssetException {
        int index = 0;
        try {
            List<DictionaryEntry> entries = new ArrayList<>();
            MappingIterator<ExcludeTermsCSV> excludeTermsCSV = csvMapper
                    .readerFor(ExcludeTermsCSV.class).with(csvSchema)
                    .readValues(csvData);
            validateEmptyContent(excludeTermsCSV);

            while (excludeTermsCSV.hasNext()) {
                index = excludeTermsCSV.getCurrentLocation().getLineNr();
                ExcludeTermsCSV csv = excludeTermsCSV.next();
                entries.add(fromCSV(csv));


            }
            return entries;
        } catch (AssetException e) { throw e;

        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file[line:"
                    + (index) + "]. Please follow the csv format while" +
                    " uploading.", CSV_TRANSFORM_ERROR.getCode());
        }
    }

    private DictionaryEntry fromCSVToEntry(ExcludeTermsCSV csv) {
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());
        entry.setExcludeTerms(csv.getExcludeTerms());
        return entry;
    }

    @Override
    public void addIDInCSV() {
        this.addIDInCSV = true;
    }

    @Override
    public DictionaryEntry fromCSV(CSVData data) {
        ExcludeTermsCSV csv = (ExcludeTermsCSV) data;
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());
        entry.setExcludeTerms(csv.getExcludeTerms());
        entry.setGood(getQueries(csv.getGood()));
        entry.setBad(getQueries(csv.getBad()));
        entry.setOk(getQueries(csv.getOk()));
        entry.setBest(getQueries(csv.getBest()));
        return entry;
    }

    @Override
    public MappingIterator getMappingIterator(File csvData) {
        try {
            return csvMapper
                    .readerFor(ExcludeTermsCSV.class).with(csvSchema)
                    .readValues(csvData);
        } catch (IOException e) {
            String msg = "Error while reading header, reason: " + e.getMessage();
            throw new AssetException(msg);
        }
    }

    @Override
    public List<String> getRootFromCSV(String csvData) throws AssetException {
        return null;
    }

    private int getDelimiterIndex(String data) {
        Matcher matcher = excludeTermsPattern.matcher(data);
        matcher.find(); return matcher.start();
    }

    @Override
    public Map<String,List<DictionaryEntry>> handleBlacklist(List<DictionaryEntry> aiDictionary,
                                                             List<DictionaryEntry> blackListDictionary) {
        Map<String,List<DictionaryEntry>> result = new HashMap<>(blackListDictionary.size());
        result.put(DELETE,new ArrayList<>());
        result.put(UPDATE,new ArrayList<>());
        Map<String, DictionaryEntry> blackListMap = new HashMap<>(blackListDictionary.size());
        blackListDictionary.forEach(dictionaryEntry -> blackListMap.put(dictionaryEntry.getName(),dictionaryEntry));
        for (DictionaryEntry entry : aiDictionary) {
            if(!blackListMap.containsKey(entry.getName())) continue;
            DictionaryEntry blacklist = blackListMap.get(entry.getName());
            removeTerm(entry,blacklist);
            if (isEntryEmpty(entry))
                result.get(DELETE).add(entry);
            else
                result.get(UPDATE).add(entry);
        }
        return result;
    }

    private void removeTerm(DictionaryEntry input,
                            DictionaryEntry blackListedEntry) {
        input.getExcludeTerms().removeAll(blackListedEntry.getExcludeTerms());
    }

    private boolean isEntryEmpty(DictionaryEntry entry) {
        return CollectionUtils.isEmpty(entry.getExcludeTerms());
    }

    @Override
    public List<DictionaryEntry> appendData(List<DictionaryEntry> currentList,
                                            List<DictionaryEntry> newList) {
        // if new entry is present in current list then append values of new entry to current entry
        List<DictionaryEntry> result = new ArrayList<>();
        Map<String,DictionaryEntry> currentEntryMap = new HashMap<>(currentList.size());
        currentList.forEach(dictionaryEntry -> currentEntryMap.put(dictionaryEntry.getName(), dictionaryEntry));
        for (DictionaryEntry newEntry :newList) {
            DictionaryEntry temp = newEntry;
            if(currentEntryMap.containsKey(newEntry.getName())) {
                temp = appendData(currentEntryMap.get(newEntry.getName()), newEntry);
            }
            result.add(temp);
        }
        return result;
    }

    private DictionaryEntry appendData(DictionaryEntry currentEntry, DictionaryEntry newEntry) {
        // if values of new entry is not present in current entry then append it
        for (String entry : newEntry.getExcludeTerms()) {
            if (!currentEntry.getExcludeTerms().contains(entry)) {
                currentEntry.getExcludeTerms().add(entry);
            }
        }
        return currentEntry;
    }

    /** returns values present in entry1 and not in entry2
     * */

    @Override
    public DictionaryEntry getMissingData(DictionaryEntry entry1, DictionaryEntry entry2) {
        DictionaryEntry entry1Copy = entry1.copy();
        entry1Copy.getExcludeTerms().removeAll(entry2.getExcludeTerms());
        return entry1Copy;
    }
}
