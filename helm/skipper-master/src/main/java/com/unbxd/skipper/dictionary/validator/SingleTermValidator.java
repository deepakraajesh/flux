package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

public class SingleTermValidator extends AbstractAssetValidator {

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {

        if (!isSingleTermEntry(entry)) {
            throw new AssetException(getValidationMessage(SINGLETERM,
                    dictionaryName), ErrorCode.SINGLETERM.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
           if (!isSingleTermEntry(entry)) {
               dictionaryContext.getDictionaryData().getOmissions()
                       .add(Omission.getInstance(ErrorCode.SINGLETERM.getCode(),
                               "Removed multi-term entry.", entry));
               return true;
           }
           return false;
        });
    }

    private boolean isSingleTermEntry(DictionaryEntry entry) {
        String name = entry.getName();
        String[] terms = name.split("\\s+");

        return terms.length <= 1;
    }
}
