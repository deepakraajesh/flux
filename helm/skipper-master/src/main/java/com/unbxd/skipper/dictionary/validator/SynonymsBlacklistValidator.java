package com.unbxd.skipper.dictionary.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.MongoDictionaryDao;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.Omission;
import com.unbxd.skipper.dictionary.transformer.AssetTransformer;

import java.util.*;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class SynonymsBlacklistValidator extends DefaultBlacklistValidator {

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public SynonymsBlacklistValidator(MongoDictionaryDao mongoDao, Map<String, AssetTransformer> transformerMap) {
        super(mongoDao, transformerMap);
    }

    @Override
    public void validateAIDictionary(DictionaryContext dictionaryContext) throws AssetException {
        try {
            if (!isTypeAi(dictionaryContext)) return;
            List<DictionaryEntry> input = dictionaryContext.getDictionaryData().getEntries();
            Map<String, DictionaryEntry> blacklistMap = getBlacklistMap(dictionaryContext);
            for (DictionaryEntry entry : input) {
                if (blacklistMap.containsKey(entry.getName())) {
                    if (containsBlacklist(entry, blacklistMap.get(entry.getName()))) {
                        String msg  = getValidationMessage(BLACKLIST_VALIDATION, dictionaryContext.getDictionaryName())
                                + mapper.writeValueAsString(entry);
                        throw new AssetException(msg, ErrorCode.BLACKLIST_VALIDATION.getCode());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new AssetException("Blacklist validation failed", ErrorCode.JSON_PARSE_ERROR.getCode());
        }

    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        if (!isTypeAi(dictionaryContext)) return;
        Map<String,DictionaryEntry> blacklistMap = getBlacklistMap(dictionaryContext);
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if (blacklistMap.containsKey(entry.getName()) &&
                    containsBlacklist(entry, blacklistMap.get(entry.getName()))) {
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.BLACKLIST_VALIDATION.getCode(),
                                "Removed blacklisted entry.", entry));
                return true;
            }
            return false;
        });
    }

    private boolean containsBlacklist(DictionaryEntry aiEntry, DictionaryEntry blacklistEntry) {
        Set<String> blacklistValues = new HashSet<>(emptyIfNull(blacklistEntry.getOneWay()));
        blacklistValues.addAll(emptyIfNull(blacklistEntry.getTwoWay()));
        List<String> aiSynonyms = new ArrayList<>(emptyIfNull(aiEntry.getOneWay()));
        aiSynonyms.addAll(emptyIfNull(aiEntry.getTwoWay()));
        for (String term : aiSynonyms) {
            if (blacklistValues.contains(term)) return true;
        }
        return false;
    }
}