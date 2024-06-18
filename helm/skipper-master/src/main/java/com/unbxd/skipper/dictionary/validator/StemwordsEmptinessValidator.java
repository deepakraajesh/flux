package com.unbxd.skipper.dictionary.validator;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;

import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isWhitespace;

public class StemwordsEmptinessValidator extends AbstractAssetValidator {

    @Override
    public void validateDictionary(String coreName,
                                   String dictionaryName,
                                   DictionaryEntry entry) throws AssetException {
        if (isEntryEmpty(entry)) {
            throw new AssetException(getValidationMessage(STEMWORDS_EMPTINESS,
                    dictionaryName) + entry.getName(), ErrorCode.STEMWORDS_EMPTINESS
                    .getCode());
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
                        .add(Omission.getInstance(ErrorCode.STEMWORDS_EMPTINESS.getCode(),
                                "Removed entry having empty stemmed word.", entry));
                iterator.remove();
            } else {
                trimEntry(entry);
            }
        }
    }

    private void trimEntry(DictionaryEntry entry) {
        entry.setStemmed(entry.getStemmed().trim());
    }

    private boolean isEntryEmpty(DictionaryEntry entry) {
        String name = entry.getStemmed();
        return isEmpty(name) || isWhitespace(name);
    }

}
