package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

public class MultiTermValidator extends AbstractAssetValidator {

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {

        if (!isMultiTermEntry(entry)) {
            throw new AssetException(getValidationMessage(MULTITERM,
                    dictionaryName), ErrorCode.MULTITERM.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (!isMultiTermEntry(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.MULTITERM.getCode(),
                                "Removed single-term entry.", entry));
                return true;
            }
            return false;
        });
    }

    private boolean isMultiTermEntry(DictionaryEntry entry) {
        String name = entry.getName();
        String[] terms = name.split("\\s+");

        return terms.length > 1;
    }
}
