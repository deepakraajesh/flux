package com.unbxd.skipper.dictionary.validator;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.lucene.analysis.en.PorterStemmer;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.util.IoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface AssetValidator {

    String ASSETS = "assets";
    String ASSET_DATA = "data";
    String ASSET_ROOT = "root";
    String DEFAULT = "default";
    String SYMBOLS = "symbols";
    String EMPTYNESS = "emptiness";
    String STOPWORDS = "stopwords";
    String DUPLICATE = "duplicate";
    String MULTITERM = "multiterm";
    String SINGLETERM = "singleterm";
    String VALIDATIONS = "validations";
    String ALPHA_NUMERIC = "alphaNumeric";
    String PRODUCT_COUNT = "productCount";
    String STOPWORDS_FRONT = "stopwords-front";
    String REDUNDANT_SYNONYM = "redundantSynonym";
    String STEMWORDS_EMPTINESS = "stemwordsEmptiness";
    String EXCLUDE_TERMS_EMPTINESS = "excludeTermsEmptiness";
    String SYNONYMS_BLACKLIST_VALIDATION = "synonymsBlacklist";
    String EXCLUDE_TERMS_BLACKLIST_VALIDATION = "excludeTermsBlacklist";
    String BLACKLIST_VALIDATION = "blacklist";

    PorterStemmer stemmer = new PorterStemmer();

    boolean validate(String content) throws AssetException;

    boolean validate(List<String> content) throws AssetException;

    default void validateSilently(DictionaryContext dictionaryContext) { }

    default void validateDictionary(String coreName,
                                    String dictionaryName,
                                    DictionaryEntry entry) throws AssetException { }

    default void validateDictionary(DictionaryContext dictionaryContext) throws AssetException {
        for (DictionaryEntry entry : dictionaryContext.getDictionaryData().getEntries()) {
            validateDictionary(dictionaryContext.getSiteKey(), dictionaryContext
                    .getDictionaryName(), entry);
        }
    }

    String VALIDATION_MSG_CONFIG_NAME = "validation-messages.json";
    Map<String, Map<String, String>> VALIDATION_NSG_CONFIG = getValidationMessagesJson();
    public static final String FORBIDDEN_ASSETS = "asset.forbidden";

    static Map<String, Map<String, String>> getValidationMessagesJson() {
        try (InputStream stream = AssetValidator.class.getClassLoader()
                .getResourceAsStream(VALIDATION_MSG_CONFIG_NAME)) {

            byte[] bytes = IoUtils.getBytes(stream);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(bytes, new TypeReference<>() {});

        } catch (IOException e){
            throw new PippoRuntimeException("Error while loading config validation-messages.json :"+ e.getMessage());
        }
    }
}
