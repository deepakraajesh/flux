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
import com.unbxd.skipper.dictionary.model.csv.StemdictCSV;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.lang.String.join;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class StemdictTransformer implements AssetTransformer {

    private CsvMapper csvMapper;
    private CsvSchema csvSchema;
    private static final String DELIMITER = "\t";
    private static final String STEMMED = "Stemmed";
    private boolean addIDInCSV = false;

    public StemdictTransformer() {
        csvMapper = new CsvMapper();
        csvSchema = CsvSchema.builder()
                .setStrictHeaders(Boolean.FALSE)
                .addColumn(KEYWORD)
                .addColumn(STEMMED)
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
        String result = String.join(COMMA_DELIMITER, KEYWORD, STEMMED);
        if (addIDInCSV) result = String.join(COMMA_DELIMITER, result, ID);
        return result;
    }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo data) {
        String[] keyValuesArray = data.getData().split(DELIMITER);
        assert keyValuesArray.length > 1;

        DictionaryEntry entry = DictionaryEntry.getInstance(keyValuesArray[0]);
        entry.setStemmed(keyValuesArray[1]);
        entry.setId(data.getId());
        entry.setGood(data.getGood());
        entry.setBad(data.getBad());
        entry.setBest(data.getBest());
        return entry;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        return entry.getName() + DELIMITER + entry.getStemmed();
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        try {
            List<String> content = new ArrayList<>();
            MappingIterator<StemdictCSV> stemdictCSV = csvMapper
                    .readerFor(StemdictCSV.class).with(csvSchema).readValues(csvData);

            while (stemdictCSV.hasNext()) {
                StemdictCSV csv = stemdictCSV.next();
                content.add(csv.getKeyword() + DELIMITER + csv.getStemmed());
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
            MappingIterator<StemdictCSV> stemdictCSV = csvMapper
                    .readerFor(StemdictCSV.class).with(csvSchema)
                    .readValues(csvData);
            validateEmptyContent(stemdictCSV);

            while (stemdictCSV.hasNext()) {
                index = stemdictCSV.getCurrentLocation().getLineNr();
                StemdictCSV csv = stemdictCSV.next();
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

    private DictionaryEntry fromCSVToEntry(StemdictCSV csv) {
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());
        entry.setStemmed(csv.getStemmed());
        return entry;
    }

    @Override
    public DictionaryEntry fromCSV(CSVData data) {
        StemdictCSV csv = (StemdictCSV) data;
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());
        if(csv.getId() != null) {
            entry.setId(csv.getId());
        }
        entry.setStemmed(csv.getStemmed());
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
                    .readerFor(StemdictCSV.class).with(csvSchema)
                    .readValues(csvData);
        } catch (IOException e) {
            String msg = "Error while reading header, reason: " + e.getMessage();
            throw new AssetException(msg);
        }
    }

    @Override
    public List<String> getRootFromCSV(String csvData) throws AssetException {
        try {
            List<String> rootWords = new ArrayList<>();
            MappingIterator<StemdictCSV> stemdictCSV = csvMapper
                    .readerFor(StemdictCSV.class).with(csvSchema)
                    .readValues(csvData);

            while (stemdictCSV.hasNext()) {
                StemdictCSV csv = stemdictCSV.next();
                rootWords.add(deleteWhitespace(stemmer.stem(csv.getKeyword())));
            }
            return rootWords;
        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file." +
                    " Please follow the csv format while uploading.",
                    CSV_TRANSFORM_ERROR.getCode());
        }
    }

    @Override
    public String toCSV(DictionaryMongo dictionaryData) throws AssetException {
        String[] keyValue = dictionaryData.getData().split(DELIMITER);
        String result = String.join(COMMA_DELIMITER, keyValue[0], keyValue[1]);
        if (this.addIDInCSV) result = String.join(COMMA_DELIMITER, result, dictionaryData.getId());
        return result;
    }

    @Override
    public void addIDInCSV() {
        this.addIDInCSV = true;
    }
}
