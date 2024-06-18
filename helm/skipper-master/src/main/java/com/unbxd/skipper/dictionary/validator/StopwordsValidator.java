package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.MongoDictionaryDao;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Log4j2
public class StopwordsValidator extends AbstractAssetValidator {

    private MongoDictionaryDao mongoDao;

    @Inject
    public StopwordsValidator(MongoDictionaryDao mongoDao) {
        this.mongoDao = mongoDao;
    }

    @Override
    public void validateDictionary(DictionaryContext dictionaryContext) throws AssetException {
        List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());
        addSynonyms(dictionaryContext.getDictionaryName(), keywords, dictionaryContext
                .getDictionaryData().getEntries());

        try {
            keywords = mongoDao.searchDictionary(dictionaryContext.getSiteKey(), ASSET_DATA,
                    keywords, asList(STOPWORDS, STOPWORDS_FRONT));
            if (isNotEmpty(keywords)) { throw new AssetException(getValidationMessage(STOPWORDS,
                    dictionaryContext.getDictionaryName()) + keywords.get(0), ErrorCode.STOPWORDS
                    .getCode()); }

        } catch (DAOException e) {
            throw new AssetException("Stopword validation failed for dictionary: " + dictionaryContext
                    .getDictionaryName(), ErrorCode.STOPWORDS.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());
        addSynonyms(dictionaryContext.getDictionaryName(), keywords, dictionaryContext
                .getDictionaryData().getEntries());

        try {
            keywords = mongoDao.searchDictionary(dictionaryContext.getSiteKey(), ASSET_DATA,
                    keywords, asList(STOPWORDS, STOPWORDS_FRONT));
            filterEntries(keywords, dictionaryContext);

        } catch (DAOException e) {
            log.error("Exception while trying to validate " +
                    "stopwords in dictionaries for corename: " +
                    dictionaryContext.getSiteKey() + ", message" + e.getMessage());
        }
    }

    private void filterEntries(List<String> stopwords,
                               DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries()
                .removeIf(entry -> {
                    if (containsStopword(stopwords, entry)) {
                        dictionaryContext.getDictionaryData().getOmissions()
                                .add(Omission.getInstance(ErrorCode.STOPWORDS.getCode(),
                                        "Removed entry having stopwords.", entry));
                        return true;
                    }
                    return false;
                });
    }

    private boolean containsStopword(List<String> stopwords,
                                     DictionaryEntry entry) {
        if (stopwords.contains(entry.getName())) { return true; }
        if (CollectionUtils.containsAny(stopwords, emptyIfNull(entry.getOneWay()))) { return true; }
        if (CollectionUtils.containsAny(stopwords, emptyIfNull(entry.getTwoWay()))) { return true; }

        return false;
    }

}
