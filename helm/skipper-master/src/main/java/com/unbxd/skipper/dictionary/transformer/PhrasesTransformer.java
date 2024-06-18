package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.model.csv.PhrasesCSV;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class PhrasesTransformer implements AssetTransformer {

    private CsvMapper csvMapper;
    private CsvSchema csvSchema;
    private static final String TYPE = "Type";
    private static final String DELIMITER = "|";
    private static final String PHRASE = "Phrase";
    private boolean addIDInCSV = false;

    public PhrasesTransformer() {
        csvMapper = new CsvMapper();
        csvSchema = CsvSchema.builder()
                .setStrictHeaders(Boolean.FALSE)
                .addColumn(PHRASE)
                .addColumn(TYPE)
                .addColumn(ID)
                .addColumn(GOOD)
                .addColumn(BAD)
                .addColumn(OK)
                .addColumn(BEST)
                .build().withHeader()
                .withEscapeChar('\\');
    }

    @Override
    public String getDelimiter() { return DELIMITER; }

    @Override
    public String getCSVHeader() {
        String result = String.join(COMMA_DELIMITER, PHRASE, TYPE);
        if (addIDInCSV) result = String.join(COMMA_DELIMITER,result,ID);
        return result;
    }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo dictionary) {
        DictionaryEntry entry = null;
        if (dictionary.getData().contains(DELIMITER)) {
            entry = getDirectionalPhrase(dictionary.getId(), dictionary.getData());
        } else {
            entry = getFullPhrase(dictionary.getId(), dictionary.getData());
        }
        entry.setId(dictionary.getId());
        entry.setGood(dictionary.getGood());
        entry.setBad(dictionary.getBad());
        entry.setBest(dictionary.getBest());
        return entry;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        DictionaryEntry.Type type = entry.getType();

        if (type == DictionaryEntry.Type.full) {
            return entry.getName();
        } else {
            return entry.getName() + DELIMITER + entry.getType().name();
        }
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        try {
            List<String> content = new ArrayList<>();
            MappingIterator<PhrasesCSV> PhrasesCSV = csvMapper
                    .readerFor(PhrasesCSV.class).with(csvSchema).readValues(csvData);

            while (PhrasesCSV.hasNext()) {
                PhrasesCSV csv = PhrasesCSV.next();
                content.add(toString(csv));
            }
            return content;
        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file." +
                    " Please follow the csv format while uploading.",
                    CSV_TRANSFORM_ERROR.getCode());
        }
    }

    @Override
    public List<DictionaryEntry> fromCSVToEntry(String csvData) throws AssetException {
        int index = 0;
        try {
            List<DictionaryEntry> entries = new ArrayList<>();
            MappingIterator<PhrasesCSV> phrasesCSV = csvMapper
                    .readerFor(PhrasesCSV.class).with(csvSchema).readValues(csvData);
            validateEmptyContent(phrasesCSV);

            while (phrasesCSV.hasNext()) {
                index = phrasesCSV.getCurrentLocation().getLineNr();
                PhrasesCSV csv = phrasesCSV.next();
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

    @Override
    public List<String> getRootFromCSV(String csvData) throws AssetException {
        try {
            List<String> rootWords = new ArrayList<>();
            MappingIterator<PhrasesCSV> phrasesCSV = csvMapper
                    .readerFor(PhrasesCSV.class).with(csvSchema)
                    .readValues(csvData);

            while (phrasesCSV.hasNext()) {
                PhrasesCSV csv = phrasesCSV.next();
                rootWords.add(deleteWhitespace(stemmer.stem(csv.getPhrase())));
            }
            return rootWords;
        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file." +
                    " Please follow the csv format while uploading.",
                    CSV_TRANSFORM_ERROR.getCode());
        }
    }

    public MappingIterator getMappingIterator(File csvData) {
        try {
            return csvMapper
                    .readerFor(PhrasesCSV.class).with(csvSchema)
                    .readValues(csvData);
        } catch (IOException e) {
            String msg = "Error while reading header, reason: " + e.getMessage();
            throw new AssetException(msg);
        }
    }

    private String toString(PhrasesCSV csv) {
        DictionaryEntry.Type type = csv.getType();
        
        if (type == DictionaryEntry.Type.full) {
            return csv.getPhrase();
        } else {
            return csv.getPhrase() + DELIMITER + csv.getType();
        }
    }

    @Override
    public DictionaryEntry fromCSV(CSVData data) {
        PhrasesCSV csv = (PhrasesCSV) data;
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getPhrase());
        if(csv.getId() != null) {
            entry.setId(csv.getId());
        }
        entry.setType(csv.getType());
        entry.setGood(getQueries(csv.getGood()));
        entry.setBad(getQueries(csv.getBad()));
        entry.setOk(getQueries(csv.getOk()));
        entry.setBest(getQueries(csv.getBest()));
        return entry;
    }

    @Override
    public String toCSV(DictionaryMongo entry) throws AssetException {
        String result = null;
        String data = entry.getData();
        if (data.contains(DELIMITER)) {
            result = getDirectionalPhraseCSV(data);
        } else {
            result = getFullPhraseCSV(data);
        }
        if (this.addIDInCSV) result = String.join(COMMA_DELIMITER, result, entry.getId());
        return result;
    }

    private DictionaryEntry getDirectionalPhrase(String id, String data) {
        String[] keyValueArray = data.split(quote(DELIMITER));
        assert keyValueArray.length > 1;

        DictionaryEntry entry = DictionaryEntry.getInstance(keyValueArray[0]);
        entry.setType(DictionaryEntry.Type.valueOf(keyValueArray[1]));
        entry.setId(id);
        return entry;
    }

    private DictionaryEntry getFullPhrase(String id, String data) {
        DictionaryEntry entry = DictionaryEntry.getInstance(data);
        entry.setType(DictionaryEntry.Type.full);
        entry.setId(id);
        return entry;
    }

    private String getDirectionalPhraseCSV(String data) {
        String[] keyValueArray = data.split(quote(DELIMITER));
        assert keyValueArray.length > 1;
        
        return keyValueArray[0] + COMMA_DELIMITER + keyValueArray[1];
    }

    private String getFullPhraseCSV(String data) {
        return String.join(COMMA_DELIMITER, data, DictionaryEntry.Type.full.toString());
    }

    @Override
    public void addIDInCSV() {
        this.addIDInCSV = true;
    }
}
