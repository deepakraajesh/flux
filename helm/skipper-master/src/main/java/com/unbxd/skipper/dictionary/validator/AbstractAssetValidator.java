package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.STEMDICT;
import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.SYNONYMS;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.logging.log4j.util.Strings.EMPTY;

public abstract class AbstractAssetValidator implements AssetValidator {

    @Override
    public boolean validate(String content) throws AssetException { return false; }

    @Override
    public boolean validate(List<String> content) throws AssetException { return false; }

    protected List<String> getNames(List<DictionaryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (DictionaryEntry entry: entries) {
            names.add(entry.getName());
        }
        return names;
    }

    protected List<String> getSynonyms(List<DictionaryEntry> entries) {
        List<String> synonyms = new ArrayList<>();
        for (DictionaryEntry entry: entries) {
            List<String> oneWaySynonyms = entry.getOneWay();
            List<String> twoWaySynonyms = entry.getTwoWay();
            if (isNotEmpty(oneWaySynonyms)) { synonyms.addAll(oneWaySynonyms); }
            if (isNotEmpty(twoWaySynonyms)) { synonyms.addAll(twoWaySynonyms); }
        }
        return synonyms;
    }

    protected void addSynonyms(String dictionaryName,
                               List<String> keywords,
                               List<DictionaryEntry> entries) {
        if (dictionaryName.equalsIgnoreCase(SYNONYMS)) {
            keywords.addAll(getSynonyms(entries));
        }
    }

    protected void addStemWords(String dictionaryName,
                                List<String> keywords,
                                List<DictionaryEntry> entries) {
        if (dictionaryName.equalsIgnoreCase(STEMDICT)) {
            for (DictionaryEntry entry: entries) {
                keywords.add(entry.getStemmed());
            }
        }
    }

    protected String getValidationMessage(String validator,
                                          String dictionaryName) {
        Map<String, String> messageMap = VALIDATION_NSG_CONFIG.get(validator);

        if (MapUtils.isNotEmpty(messageMap)) {
            return messageMap.getOrDefault(dictionaryName, messageMap.get(DEFAULT));
        }
        return EMPTY;
    }

    protected String getStemmed(String string) {
        return deleteWhitespace(stemmer.stem(string));
    }
}
