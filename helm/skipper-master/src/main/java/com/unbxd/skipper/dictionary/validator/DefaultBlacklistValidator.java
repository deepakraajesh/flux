package com.unbxd.skipper.dictionary.validator;

import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.dao.MongoDictionaryDao;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.DAOException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import com.unbxd.skipper.dictionary.model.DictionaryMongo;
import com.unbxd.skipper.dictionary.model.Omission;
import com.unbxd.skipper.dictionary.transformer.AssetTransformer;

import java.util.*;

import static com.unbxd.skipper.dictionary.service.Constants.AI_TYPE_NAME;
import static com.unbxd.skipper.dictionary.service.Constants.BLACKLIST_TYPE_NAME;


public class DefaultBlacklistValidator extends AbstractAssetValidator{
    private MongoDictionaryDao mongoDao;
    private Map<String, AssetTransformer> transformerMap;
    protected static final String BLACKLIST_REASON_VALIDATION = "blacklistReasons";

    @Inject
    public DefaultBlacklistValidator(MongoDictionaryDao mongoDao, Map<String,AssetTransformer> transformerMap) {
        this.mongoDao = mongoDao;
        this.transformerMap = transformerMap;
    }


    @Override
    public void validateDictionary(DictionaryContext dictionaryContext) throws AssetException {
        validateAIDictionary(dictionaryContext);
    }

    @Override
    public void validateSilently(DictionaryContext dictionaryContext) {
        if (!isTypeAi(dictionaryContext)) return;
        Map<String,DictionaryEntry> blacklistMap = getBlacklistMap(dictionaryContext);
        dictionaryContext.getDictionaryData().getEntries().removeIf(entry -> {
            if(blacklistMap.containsKey(entry.getName())){
                dictionaryContext.getDictionaryData().getOmissions()
                        .add(Omission.getInstance(ErrorCode.BLACKLIST_VALIDATION.getCode(),
                                "Removed blacklisted entry.", entry));
                return true;
            }
            return false;
        });
    }

    protected void validateAIDictionary(DictionaryContext dictionaryContext) throws AssetException {
        if(!isTypeAi(dictionaryContext)) return;
        List<DictionaryEntry> input = dictionaryContext.getDictionaryData().getEntries();
        Map<String,DictionaryEntry> blacklistMap = getBlacklistMap(dictionaryContext);
        for(DictionaryEntry entry: input) {
            if(blacklistMap.containsKey(entry.getName())){
                String msg =  getValidationMessage(BLACKLIST_VALIDATION, dictionaryContext.getDictionaryName())
                        + entry.getName();
                throw new AssetException(msg, ErrorCode.BLACKLIST_VALIDATION.getCode());
            }
        }
    }


    protected boolean isTypeAi(DictionaryContext dictionaryContext) {
        return  AI_TYPE_NAME.equals(dictionaryContext.getType());
    }

    protected boolean isTypeBlacklist(DictionaryContext dictionaryContext) {
        return  BLACKLIST_TYPE_NAME.equals(dictionaryContext.getType());
    }

    protected Map<String,DictionaryEntry> getBlacklistMap(DictionaryContext dictionaryContext) throws AssetException {
        List<DictionaryEntry> input = dictionaryContext.getDictionaryData().getEntries();
        List<String> keywords = getNames(input);
        AssetTransformer assetTransformer = transformerMap.get(dictionaryContext.getDictionaryName());
        try {
            List<DictionaryMongo> dictionaryList = new ArrayList<>();
            for (String keyword : keywords) {
                dictionaryList.addAll(
                        mongoDao.searchDictionaryData(1,
                                keywords.size(),
                                dictionaryContext.getVersionedCoreName(),
                                keyword,
                                dictionaryContext.getDictionaryName().concat("-").concat(BLACKLIST_TYPE_NAME))
                );
            }
            List<DictionaryEntry> blacklistEntries = assetTransformer.toEntries(dictionaryList);
            Map<String,DictionaryEntry> blacklistMap = new HashMap<>(blacklistEntries.size());
            blacklistEntries.forEach(entry -> blacklistMap.put(entry.getName(), entry));
            return blacklistMap;
        } catch (DAOException e) {
            throw new AssetException("AI Blacklist validation failed for dictionary: " + dictionaryContext
                    .getDictionaryName(), ErrorCode.STOPWORDS.getCode());
        }
    }

}