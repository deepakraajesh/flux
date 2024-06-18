package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

import java.util.Iterator;

import static com.unbxd.skipper.dictionary.validator.ErrorCode.EMPTINESS;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isWhitespace;

public class EmptinessValidator extends AbstractAssetValidator {

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {
        if (isEntryEmpty(entry)) {
            throw new AssetException(getValidationMessage(EMPTYNESS,
                    dictionaryName), EMPTINESS.getCode());
        }
        trimEntry(entry);
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        Iterator<DictionaryEntry> iterator = dictionaryContext.getDictionaryData()
                .getEntries().iterator();

        while (iterator.hasNext()) {
            DictionaryEntry entry = iterator.next();
            if (isEntryEmpty(entry)) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(EMPTINESS.getCode(),
                                "Removed empty entry.", entry));
                iterator.remove();
            } else {
                trimEntry(entry);
            }
        }
    }

    private void trimEntry(DictionaryEntry entry) {
        entry.setName(entry.getName().trim());
    }

    private boolean isEntryEmpty(DictionaryEntry entry) {
        String name = entry.getName();
        return isEmpty(name) ||
                isWhitespace(name);
    }
}
