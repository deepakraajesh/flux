package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.logging.log4j.core.util.Assert.isEmpty;

public class SynonymsV2Transformer implements AssetTransformer {

    private final CsvMapper csvMapper;
    private final ObjectWriter writer;
    private final CsvSchema csvReaderSchema;
    private static final String DELIMITER = "\\|";
    private boolean addIDInCSV = false;

    public SynonymsV2Transformer() {
        csvMapper = new CsvMapper();
        csvReaderSchema = CsvSchema.builder()
                .addColumn(KEYWORD)
                .addColumn(UNIDIRECTIONAL)
                .addColumn(BIDIRECTIONAL)
                .addColumn(ID)
                .addColumn(BEST)
                .addColumn(GOOD)
                .addColumn(BAD)
                .addColumn(OK)
                .build().withHeader()
                .withEscapeChar('\\');
        CsvSchema csvSchema = CsvSchema.builder()
                .addColumn(KEYWORD)
                .addColumn(UNIDIRECTIONAL)
                .addColumn(BIDIRECTIONAL)
                .build().withoutHeader()
                .withLineSeparator("");
        writer = csvMapper.writerFor
                        (SynonymsCSV.class)
                .with(csvSchema);
    }

    @Override
    public String getDelimiter() {
        return DELIMITER;
    }

    @Override
    public String getCSVHeader() {
        String result = String.join(COMMA_DELIMITER, KEYWORD, UNIDIRECTIONAL, BIDIRECTIONAL);
        if (addIDInCSV) result = String.join(COMMA_DELIMITER, result, ID);
        return result;
    }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo dictionary) {
        String[] keyValuesArray = dictionary.getData().split(quote(DELIMITER));
        assert keyValuesArray.length > 1;

        DictionaryEntry entry = DictionaryEntry.getInstance(keyValuesArray[0]);
        entry.setOneWay(getOneWayEntries(keyValuesArray));
        entry.setTwoWay(getTwoWayEntries(keyValuesArray));
        entry.setId(dictionary.getId());
        entry.setGood(dictionary.getGood());
        entry.setBad(dictionary.getBad());
        entry.setBest(dictionary.getBest());
        return entry;
    }

    @Override
    public List<DictionaryEntry> toEntries(List<DictionaryMongo> data) {
        if (isEmpty(data)) { return Collections.emptyList(); }
        List<DictionaryEntry> entries = new ArrayList<>();

        for (DictionaryMongo eachData: data) {
            entries.add(toEntry(eachData));
        }
        return entries;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        return String.join(DELIMITER, entry.getName(),
                asString(entry.getOneWay()),
                asString(entry.getTwoWay()));
    }

    @Override
    public List<String> getRootFromCSV(String csvData) throws AssetException {
        try {
            List<String> rootWords = new ArrayList<>();
            MappingIterator<SynonymsCSV> synonymsCSV = csvMapper
                    .readerFor(SynonymsCSV.class).with(csvReaderSchema).readValues(csvData);

            while (synonymsCSV.hasNext()) {
                SynonymsCSV csv = synonymsCSV.next();
                rootWords.add(getRootFromCSV(csv));
            }
            return rootWords;
        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file." +
                    " Please follow the csv format while uploading.",
                    CSV_TRANSFORM_ERROR.getCode());
        }
    }

    /** removes terms from input1 if they are also present in input2
     *  */
    private void removeTerms(DictionaryEntry input1,
                             DictionaryEntry input2) {
        if(nonNull(input1.getOneWay())) input1.getOneWay().removeAll(emptyIfNull(input2.getOneWay()));
        if(nonNull(input1.getTwoWay())) input1.getTwoWay().removeAll(emptyIfNull(input2.getTwoWay()));
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
            removeTerms(entry,blacklist);
            if (isEntryEmpty(entry)) {
                result.get(DELETE).add(entry);
            } else {
                result.get(UPDATE).add(entry);
            }
        }
        return result;
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
        if(isNull(currentEntry.getOneWay())) currentEntry.setOneWay(new ArrayList<>());
        if(isNull(currentEntry.getTwoWay())) currentEntry.setTwoWay(new ArrayList<>());

        for (String entry : emptyIfNull(newEntry.getOneWay())) {
            if (!currentEntry.getOneWay().contains(entry)) {
                currentEntry.getOneWay().add(entry);
            }
        }
        for (String entry : emptyIfNull(newEntry.getTwoWay())) {
            if (!currentEntry.getTwoWay().contains(entry)) {
                currentEntry.getTwoWay().add(entry);
            }
        }
        return currentEntry;
    }

    private boolean isEntryEmpty(DictionaryEntry entry) {
        return CollectionUtils.isEmpty(entry.getOneWay()) && CollectionUtils.isEmpty(entry.getTwoWay());
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        try {
            List<String> content = new ArrayList<>();
            MappingIterator<SynonymsCSV> synonymsCSV = csvMapper
                    .readerFor(SynonymsCSV.class).with(csvReaderSchema).readValues(csvData);

            while (synonymsCSV.hasNext()) {
                SynonymsCSV csv = synonymsCSV.next();
                content.add(String.join(DELIMITER, csv.getKeyword(),
                        fromOneWayCSV(csv), fromTwoWayCSV(csv)));
            }
            return content;
        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file." +
                    " Please follow the csv format while uploading.",
                    CSV_TRANSFORM_ERROR.getCode());
        }
    }

    @Override
    public MappingIterator getMappingIterator(File csvData) {
        try {
            return csvMapper
                    .readerFor(SynonymsCSV.class).with(csvReaderSchema)
                    .readValues(csvData);
        } catch (IOException e) {
            String msg = "Error while reading header, reason: " + e.getMessage();
            throw new AssetException(msg);
        }
    }

    @Override
    public List<DictionaryEntry> fromCSVToEntry(String csvData) throws AssetException {
        int index = 0;
        try {
            List<DictionaryEntry> entries = new ArrayList<>();
            MappingIterator<SynonymsCSV> synonymsCSV = csvMapper
                    .readerFor(SynonymsCSV.class).with(csvReaderSchema)
                    .readValues(csvData);
            validateEmptyContent(synonymsCSV);

            while (synonymsCSV.hasNext()) {
                index = synonymsCSV.getCurrentLocation().getLineNr();
                SynonymsCSV csv = synonymsCSV.next();
                entries.add(fromCSV(csv));
            }
            return entries;
        } catch (AssetException e) { throw e;

        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file[line:"
                    + (index) + "]. Please follow the csv format while" +
                    " uploading: " + e.getMessage(), CSV_TRANSFORM_ERROR.getCode());
        }
    }

    @Override
    public String toCSV(DictionaryMongo entry) throws AssetException {
        try {
            String[] keyValuesArray = entry.getData().split(quote(DELIMITER));
            SynonymsCSV synonymsCSV = new SynonymsCSV(keyValuesArray[0],
                    getTwoWayCSV(keyValuesArray),
                    getOneWayCSV(keyValuesArray));
            String result = writer.writeValueAsString(synonymsCSV);
            if (addIDInCSV) result = String.join(COMMA_DELIMITER, result, entry.getId());
            return result;
        } catch (JsonProcessingException e) {
            throw new AssetException("Exception while trying to convert" +
                    "entry[" + entry.getData() + "] to csv: " +
                    e.getMessage());
        }
    }

    /** returns values present in entry1 and not in entry2
     * */
    @Override
    public DictionaryEntry getMissingData(DictionaryEntry entry1, DictionaryEntry entry2) {
        DictionaryEntry entry1Copy = entry1.copy();
        DictionaryEntry entry2Copy = entry2.copy();
        removeTerms(entry1Copy,entry2Copy);
        return entry1Copy;
    }

    private String getOneWayCSV(String[] data) {
        if (data.length >= 2 && StringUtils.isNotEmpty(data[1])) {
            return data[1];
        }
        return EMPTY;
    }

    private String getTwoWayCSV(String[] data) {
        if (data.length >= 3 && StringUtils.isNotEmpty(data[2])) {
            return data[2];
        }
        return EMPTY;
    }

    private List<String> getOneWayEntries(String[] data) {
        if (data.length >= 2 && StringUtils.isNotEmpty(data[1])) {
            return new ArrayList<>(Arrays.asList(data[1].split(COMMA_DELIMITER)));
        }
        return null;
    }

    private List<String> getTwoWayEntries(String[] data) {
        if (data.length >= 3 && StringUtils.isNotEmpty(data[2])) {
            return new ArrayList<>(Arrays.asList(data[2].split(COMMA_DELIMITER)));
        }
        return null;
    }

    private String asString(List<String> values) {
        if (isEmpty(values)) { return EMPTY; }
        return String.join(COMMA_DELIMITER, values);
    }

    private String fromOneWayCSV(SynonymsCSV csv) {
        if (StringUtils.isNotEmpty(csv.getUnidirectional())) {
            return deleteWhitespace(csv.getUnidirectional());
        }
        return EMPTY;
    }

    private String fromTwoWayCSV(SynonymsCSV csv) {
        if (StringUtils.isNotEmpty(csv.getBidirectional())) {
            return deleteWhitespace(csv.getBidirectional());
        }
        return EMPTY;
    }


    private String getRootWord(DictionaryEntry entry) {
        List<String> rootWords = new ArrayList<>();
        for (String synonym: emptyIfNull(entry.getTwoWay())) {
            rootWords.add(deleteWhitespace(stemmer.stem(synonym)));
        }
        rootWords.add(deleteWhitespace(stemmer.stem(entry.getName())));
        return join(rootWords, COMMA_DELIMITER);
    }

    private String getRootFromCSV(SynonymsCSV csv) {
        String[] synonyms = csv.getBidirectional().split(COMMA_DELIMITER);
        List<String> rootWords = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(synonyms)) {
            for (String synonym : synonyms) {
                rootWords.add(deleteWhitespace(stemmer.stem(synonym)));
            }
        }
        rootWords.add(deleteWhitespace(stemmer.stem(csv.getKeyword())));
        return join(rootWords, COMMA_DELIMITER);
    }

    @Override
    public DictionaryEntry fromCSV(CSVData data) {
        SynonymsCSV csv = (SynonymsCSV)data;
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());

        if(csv.getId()!=null)
            entry.setId(csv.getId());

        String[] oneWay = csv.getUnidirectional().split(COMMA_DELIMITER);
        oneWay = ArrayUtils.removeAllOccurrences(oneWay, "");
        if (ArrayUtils.isNotEmpty(oneWay)) {
            entry.setOneWay(newArrayList(oneWay));
        }

        String[] twoWay = csv.getBidirectional().split(COMMA_DELIMITER);
        twoWay = ArrayUtils.removeAllOccurrences(twoWay, "");
        if (ArrayUtils.isNotEmpty(twoWay)) {
            entry.setTwoWay(newArrayList(twoWay));
        }
        entry.setGood(getQueries(csv.getGood()));
        entry.setBad(getQueries(csv.getBad()));
        entry.setOk(getQueries(csv.getOk()));
        entry.setBest(getQueries(csv.getBest()));
        return entry;
    }

    @Override
    public void addIDInCSV() {
        this.addIDInCSV = true;
    }
}
