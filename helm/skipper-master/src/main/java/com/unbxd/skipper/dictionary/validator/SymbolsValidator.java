package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.containsAny;

public class SymbolsValidator extends AbstractAssetValidator {

    private static final char[] SYMBOL_LIST = {',', '+', '{', '}', '*', '&', '\\', '~'};

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (containsSymbol(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.SYMBOLS.getCode(),
                                "Removed entry having invalid symbols.", entry));
                return true;
            }
            return false;
        });
    }

    @Override
    public void validateDictionary(DictionaryContext dictionaryContext) throws AssetException {
        List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());
        validateSymbols(dictionaryContext.getDictionaryName(), keywords, dictionaryContext
                .getDictionaryData().getEntries());
    }

    private void validateSymbols(String dictionaryName,
                                 List<String> keywords,
                                 List<DictionaryEntry> entries) throws AssetException {
        addStemWords(dictionaryName, keywords, entries);
        addSynonyms(dictionaryName, keywords, entries);
        for (String keyword : keywords) {
            if (containsAny(keyword, SYMBOL_LIST)) {
                throw new AssetException(getValidationMessage(SYMBOLS,
                        dictionaryName), ErrorCode.SYMBOLS.getCode());
            }
        }
    }

    private boolean containsSymbol(DictionaryEntry entry) {
        if (containsAny(entry.getName(), SYMBOL_LIST)) { return true; }
        if (containsAny(entry.getStemmed(), SYMBOL_LIST)) { return true;}
        for (String oneWaySynonym: emptyIfNull(entry.getOneWay())) {
            if (containsAny(oneWaySynonym, SYMBOL_LIST)) { return true; }
        }
        for (String twoWaySynonym: emptyIfNull(entry.getTwoWay())) {
            if (containsAny(twoWaySynonym, SYMBOL_LIST)) { return true; }
        }
        return false;
    }
}
