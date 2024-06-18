package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

public class AlphaNumericValidator extends AbstractAssetValidator {

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {
        if (isNonAlphaNumeric(entry)) {
            throw new AssetException(getValidationMessage(ALPHA_NUMERIC,
                    dictionaryName) + entry.getName(), ErrorCode.ALPHA_NUMERIC.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (isNonAlphaNumeric(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.ALPHA_NUMERIC.getCode(),
                                "Removed entry having no alpha-numeric characters.", entry));
                return true;
            }
            return false;
        });
    }

    private boolean isNonAlphaNumeric(DictionaryEntry entry) {
        return entry.getName().chars().noneMatch(Character::isLetterOrDigit);
    }
}
