package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.contains;

public class ExcludeTermsEmptinessValidator extends AbstractAssetValidator {

    private static final String DELIMITER = "-";

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {
        if (isEntryEmpty(entry)) {
            throw new AssetException(getValidationMessage(EXCLUDE_TERMS_EMPTINESS,
                    dictionaryName) + entry.getName(), ErrorCode.EXCLUDE_TERMS_EMPTINESS.getCode());
        } else if (isEntryRedundant(entry)) {
            throw new AssetException("Found redundant entry as keyword & also as " +
                    "exclude term for : " + entry.getName(), ErrorCode.EXCLUDE_TERMS_REDUNDANCY.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (isEntryEmpty(entry)) {
                dictionaryContext.getDictionaryData().getOmissions().add(Omission
                        .getInstance(ErrorCode.EXCLUDE_TERMS_EMPTINESS.getCode(),
                                "Removed entry having empty exclude terms.", entry));
                return true;
            } else if (isEntryRedundant(entry)) {
                dictionaryContext.getDictionaryData().getOmissions().add(Omission
                        .getInstance(ErrorCode.EXCLUDE_TERMS_REDUNDANCY.getCode(),
                                "Removed entry having keyword as exclude term.", entry));
                return true;
            }
            return false;
        });
    }

    private boolean isEntryEmpty(DictionaryEntry entry) {
        return isEmpty(entry.getExcludeTerms());
    }

    private boolean isEntryRedundant(DictionaryEntry entry) {
        return entry.getExcludeTerms().contains(entry.getName());
    }

    private boolean containsHyphen(DictionaryEntry entry) {
        for (String excludeTerm: entry.getExcludeTerms()) {
            if (excludeTerm.contains(DELIMITER)) { return true; }
        }
        return contains(entry.getName(), DELIMITER);
    }
}
