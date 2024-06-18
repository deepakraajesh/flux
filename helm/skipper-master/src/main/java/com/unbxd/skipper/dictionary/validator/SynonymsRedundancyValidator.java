package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.filterValues;
import static org.apache.commons.collections4.CollectionUtils.*;

public class SynonymsRedundancyValidator extends AbstractAssetValidator {

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (isSynonymEmpty(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.EMPTY_SYNONYM.getCode(),
                                "Removed synonym entry having empty entries" +
                                        " as unidirectional/bidirectional synonyms.", entry));
                return true;
            } else if (isEntryRedundant(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.REDUNDANT_SYNONYM.getCode(),
                                "Removed synonym entry having redundant terms" +
                                        " as unidirectional/bidirectional synonym.", entry));
                return true;
            }
            trimWhitespaces(entry);
            return false;
        });
    }

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {
        if (isSynonymEmpty(entry)) {
            throw new AssetException("No synonyms detected for one or more " +
                    "Search Keyword. Kindly delete the Search Keyword or add a" +
                    " synonym to save the changes.", ErrorCode.EMPTY_SYNONYM.getCode());
        } else if (isEntryRedundant(entry)) {
            throw new AssetException(getValidationMessage(REDUNDANT_SYNONYM,
                    dictionaryName) + entry.getName(), ErrorCode.REDUNDANT_SYNONYM.getCode());
        }
        trimWhitespaces(entry);
    }

    private void trimWhitespaces(DictionaryEntry entry) {
        List<String> oneWay = entry.getOneWay();
        List<String> twoWay = entry.getTwoWay();
        if (isNotEmpty(oneWay)) { oneWay.removeIf(StringUtils::isWhitespace); }
        if (isNotEmpty(twoWay)) { twoWay.removeIf(StringUtils::isWhitespace); }
    }

    private boolean isSynonymEmpty(DictionaryEntry entry) {
        return isEmpty(entry.getOneWay()) && isEmpty(entry.getTwoWay());
    }

    private boolean isEntryRedundant(DictionaryEntry entry) {
        Collection<String> oneWay = emptyIfNull(entry.getOneWay());
        Collection<String> twoWay = emptyIfNull(entry.getTwoWay());

        return oneWay.contains(entry.getName()) ||
                twoWay.contains(entry.getName()) ||
                containsAny(oneWay, twoWay) ||
                valuesRedundant(oneWay) ||
                valuesRedundant(twoWay);
    }

    private boolean valuesRedundant(Collection<String> keywords) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String keyword: keywords) { countMap.put(keyword,
                countMap.getOrDefault(keyword, 0) + 1); }
        return MapUtils.isNotEmpty(filterValues(countMap, value -> value > 1));
    }
}
