package com.unbxd.skipper.dictionary.transformer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.unbxd.console.model.FacetAttributes;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.csv.AsciiMappingCSV;
import com.unbxd.skipper.dictionary.model.csv.BasicCSV;
import com.unbxd.skipper.dictionary.model.csv.CSVData;
import com.unbxd.skipper.dictionary.model.csv.SynonymsCSV;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.unbxd.skipper.dictionary.model.DictionaryEntry.getInstance;
import static com.unbxd.skipper.dictionary.validator.ErrorCode.CSV_TRANSFORM_ERROR;
import static java.util.regex.Pattern.quote;

public class AsciiMappingTransformer implements AssetTransformer {

    private CsvMapper csvMapper;
    private CsvSchema csvSchema;
    private static final String KEY = "Key";
    private static final String DELIMITER = "=>";
    private static final String MAPPING = "Mapping";

    public AsciiMappingTransformer() {
        csvMapper = new CsvMapper();
        csvSchema = CsvSchema.builder()
                .setStrictHeaders(Boolean.FALSE)
                .addColumn(KEY)
                .addColumn(MAPPING)
                .addColumn(ID)
                .addColumn(GOOD)
                .addColumn(BAD)
                .addColumn(OK)
                .addColumn(BEST)
                .build().withHeader()
                .withEscapeChar('~');
    }

    @Override
    public String getDelimiter() { return DELIMITER; }

    @Override
    public String getCSVHeader() { return String.join(COMMA_DELIMITER, KEY, MAPPING); }

    @Override
    public DictionaryEntry toEntry(DictionaryMongo data) {
        String[] keyValuesArray = data.getData().split(DELIMITER);
        assert keyValuesArray.length > 1;

        DictionaryEntry entry = getInstance(keyValuesArray[0]);
        entry.setMapping(keyValuesArray[1]);
        entry.setId(data.getId());
        entry.setId(data.getId());
        entry.setGood(data.getGood());
        entry.setBad(data.getBad());
        entry.setBest(data.getBest());
        return entry;
    }

    @Override
    public String getData(DictionaryEntry entry) {
        return entry.getName() + DELIMITER + entry.getMapping();
    }

    @Override
    public List<String> fromCSV(String csvData) throws AssetException {
        return null;
    }

    @Override
    public String toCSV(DictionaryMongo dictionaryData) throws AssetException {
        return null;
    }

    @Override
    public List<DictionaryEntry> fromCSVToEntry(String csvData) throws AssetException {
        int index = 0;
        try {
            List<DictionaryEntry> entries = new ArrayList<>();
            MappingIterator<AsciiMappingCSV> mappingCSV = csvMapper
                    .readerFor(AsciiMappingCSV.class).with(csvSchema)
                    .readValues(csvData);
            validateEmptyContent(mappingCSV);

            while (mappingCSV.hasNext()) {
                index = mappingCSV.getCurrentLocation().getLineNr();
                AsciiMappingCSV csv = mappingCSV.next();
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
    public DictionaryEntry fromCSV(CSVData data) {
        AsciiMappingCSV csv = (AsciiMappingCSV) data;
        DictionaryEntry entry = getInstance(getQuotedString(csv.getAsciiString()));
        entry.setMapping(csv.getMapping());
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
                    .readerFor(AsciiMappingCSV.class).with(csvSchema)
                    .readValues(csvData);
        } catch (IOException e) {
            String msg = "Error while reading header, reason: " + e.getMessage();
            throw new AssetException(msg);
        }
    }

    private String getQuotedString(String string) {
        return "\"" + string + "\"";
    }

    @Override
    public List<String> getRootFromCSV(String csvData) throws AssetException {
        return null;
    }

    @Override
    public void addIDInCSV() {

    }

}
