package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.csv.BasicCSV;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

public class BasicTransformer implements AssetTransformer {

    private CsvMapper csvMapper;
    private CsvSchema csvSchema;
    private boolean addIdInCSV = false;

    public BasicTransformer() {
        csvMapper = new CsvMapper();
        csvSchema = CsvSchema.builder()
                .setStrictHeaders(Boolean.FALSE)
                .addColumn(KEYWORD)
                .addColumn(ID)
                .addColumn(GOOD)
                .addColumn(BAD)
                .addColumn(OK)
                .addColumn(BEST)
                .build().withHeader()
                .withEscapeChar('\\');
    }

    @Override
    public String getDelimiter() { return Strings.EMPTY; }

    @Override
    public String getCSVHeader() {
        String result = KEYWORD;
        if(addIdInCSV) result = String.join(COMMA_DELIMITER, KEYWORD, ID);
        return result;
    }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo dictionary) {
        DictionaryEntry entry = DictionaryEntry
                .getInstance(dictionary.getData());
        entry.setId(dictionary.getId());
        entry.setGood(dictionary.getGood());
        entry.setBad(dictionary.getBad());
        entry.setBest(dictionary.getBest());
        return entry;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        return entry.getName();
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        try {
            List<String> content = new ArrayList<>();
            MappingIterator<BasicCSV> basicCSV = csvMapper
                    .readerFor(BasicCSV.class).with(csvSchema).readValues(csvData);

            while (basicCSV.hasNext()) {
                BasicCSV csv = basicCSV.next();
                content.add(csv.getKeyword());
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
            MappingIterator<BasicCSV> basicCSV = csvMapper
                    .readerFor(BasicCSV.class).with(csvSchema)
                    .readValues(csvData);
            validateEmptyContent(basicCSV);

            while (basicCSV.hasNext()) {
                index = basicCSV.getCurrentLocation().getLineNr();
                BasicCSV csv = basicCSV.next();
                entries.add(DictionaryEntry.getInstance(csv.getKeyword()));
            }
            return entries;
        }  catch (AssetException e) { throw e;

        } catch (Exception e) {
            throw new AssetException("Unable to parse CSV file[line:"
                    + (index) + "]. Please follow the csv format while" +
                    " uploading.", CSV_TRANSFORM_ERROR.getCode());
        }
    }

    @Override
    public void addIDInCSV() {
        this.addIdInCSV = true;
    }

    @Override
    public DictionaryEntry fromCSV(CSVData data) {
        BasicCSV csv = (BasicCSV)data;
        DictionaryEntry entry = DictionaryEntry.getInstance(csv.getKeyword());
        if(csv.getId() != null) {
            entry.setId(csv.getId());
        }
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
                    .readerFor(BasicCSV.class).with(csvSchema)
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
            MappingIterator<BasicCSV> basicCSV = csvMapper
                    .readerFor(BasicCSV.class).with(csvSchema).readValues(csvData);

            while (basicCSV.hasNext()) {
                BasicCSV csv = basicCSV.next();
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
    public String toCSV(DictionaryMongo entry) throws AssetException {
        String result = entry.getData();
        if (this.addIdInCSV) result = String.join(COMMA_DELIMITER, entry.getData(), entry.getId());
        return result;
    }
}
