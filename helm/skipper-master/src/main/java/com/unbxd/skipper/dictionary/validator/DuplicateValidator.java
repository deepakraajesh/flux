package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.DictionaryDAO;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.filterValues;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Log4j2
public class DuplicateValidator extends AbstractAssetValidator {

    private DictionaryDAO mongoDAO;

    @Inject
    public DuplicateValidator(DictionaryDAO mongoDao) {
        this.mongoDAO = mongoDao;
    }

    @Override
    public void validateDictionary(DictionaryContext dictionaryContext) throws AssetException {
        List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());

        try {
            validateInput(keywords, dictionaryContext);
            keywords = mongoDAO.searchDictionary(dictionaryContext.getSiteKey(),
                    ASSET_ROOT, keywords, singletonList(dictionaryContext.getQualifiedDictionaryName()));
            if (isNotEmpty(keywords)) { throw new AssetException(getValidationMessage(DUPLICATE,
                    dictionaryContext.getDictionaryName()), ErrorCode.DUPLICATE.getCode()); }
        } catch (DAOException e) {
            throw new AssetException("Duplicate validation failed for dictionary: " + dictionaryContext
                    .getDictionaryName(), ErrorCode.DUPLICATE.getCode());
        }
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        List<String> keywords = getNames(dictionaryContext.getDictionaryData().getEntries());

        try {
            filterInput(keywords, dictionaryContext);
            keywords = mongoDAO.searchDictionary(dictionaryContext.getSiteKey(),
                    getIndexField(dictionaryContext.getDictionaryName()), keywords,
                    singletonList(dictionaryContext.getQualifiedDictionaryName()));
            filterDuplicates(keywords, dictionaryContext);

        } catch (DAOException e) {
            log.error("Exception while trying to validate duplicate" +
                    " dictionaries for corename: " + dictionaryContext
                    .getSiteKey() + ", message: " + e.getMessage());
        }
    }

    private void validateInput(List<String> keywords,
                               DictionaryContext dictionaryContext) throws AssetException {
        Map<String, Integer> duplicateCounts = filterValues(getDuplicateCounts(keywords),
                value -> value > 1);
        if (MapUtils.isNotEmpty(duplicateCounts)) {
            throw new AssetException(getValidationMessage(DUPLICATE,
                dictionaryContext.getDictionaryName()), ErrorCode.DUPLICATE.getCode());
        }
    }

    private void filterInput(List<String> keywords,
                             DictionaryContext dictionaryContext) {
        Map<String, Integer> duplicateCounts = getDuplicateCounts(keywords);
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            String stemmed = getStemmed(entry.getName());
            if (duplicateCounts.getOrDefault(stemmed,0) > 1) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.DUPLICATE.getCode(),
                                "Removed duplicate entry.", entry));
                duplicateCounts.put(stemmed, duplicateCounts.get(stemmed)-1);
                return true;
            }
            return false;
        });
    }

    private void filterDuplicates(List<String> duplicates,
                                  DictionaryContext dictionaryContext) {
        dictionaryContext.getDictionaryData().getEntries()
                .removeIf(entry -> {
                    if (isDuplicate(duplicates, entry)) {
                        dictionaryContext.getDictionaryData().getOmissions()
                                .add(Omission.getInstance(ErrorCode.DUPLICATE.getCode(),
                                        "Removed duplicate entry.", entry));
                        return true;
                    }
                    return false;
                });
    }

    private boolean isDuplicate(List<String> duplicates,
                                DictionaryEntry entry) {
        return duplicates.contains(getStemmed(entry.getName()));
    }

    /** stemmed version of getNames(), stems each name/keyword */
    @Override
    protected List<String> getNames(List<DictionaryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (DictionaryEntry entry: entries) {
            names.add(getStemmed(entry.getName()));
        }
        return names;
    }

    /** stemmed version of getSynonyms(), stems each word */
    protected List<String> getSynonyms(List<DictionaryEntry> entries) {
        List<String> synonyms = new ArrayList<>();
        for (DictionaryEntry entry: entries) {
            for (String synonym: emptyIfNull(entry.getTwoWay())) {
                synonyms.add(getStemmed(synonym));
            }
        }
        return synonyms;
    }

    private String getIndexField(String dictionaryName) {
        if (equalsIgnoreCase(dictionaryName, STOPWORDS)) {
            return ASSET_DATA;
        }
        return ASSET_ROOT;
    }

    private Map<String, Integer> getDuplicateCounts(List<String> keywords) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String keyword: keywords) { countMap.put(keyword,
                countMap.getOrDefault(keyword, 0) + 1); }
        return countMap;
    }
}
