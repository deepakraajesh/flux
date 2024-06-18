package com.unbxd.autosuggest;

import com.unbxd.autosuggest.exception.AutosuggestException;
import com.unbxd.autosuggest.model.*;

import java.util.List;


public interface AutosuggestService {

    void setKeywordSuggestion(String siteKey,
                              KeywordSuggestion keywordSuggestion ,
                              boolean ignoreDuplicateFieldError) throws AutosuggestException;

    void setInfield(String siteKey,
                    String fieldName,
                    boolean ignoreDuplicateFieldError) throws AutosuggestException;

    void setPPSearchableField(String siteKey,
                              String fieldName,
                              boolean ignoreDuplicateFieldError) throws AutosuggestException;

    void setPopularProductField(String siteKey,
                                PopularProductField popularProductField,
                                boolean ignoreDuplicateFieldError) throws AutosuggestException;

    void setTopQueriesCount(String siteKey , Integer topQueriesCount) throws AutosuggestException;

    List<KeywordSuggestion> getKeywordSuggestions(String siteKey) throws AutosuggestException;

    List<InField> getInFields(String string) throws AutosuggestException;

    List<PopularProductField> getPopularProducts(String siteKey) throws AutosuggestException;

    List<String> getPPSearchableFields(String siteKey) throws AutosuggestException;

    Integer getTopQueriesCount(String siteKey) throws AutosuggestException;

    AutosuggestIndexingStatus getIndexingStatus(String siteKey) throws  AutosuggestException;

    AutosuggestIndexingStatus getIndexingStatus(String siteKey,String feedId) throws  AutosuggestException;

    void deleteKeywordSuggestion(String siteKey , String suggestionName) throws AutosuggestException;

    void deleteInField(String siteKey , String fieldName) throws AutosuggestException;

    void deletePopularProductField(String siteKey , String fieldName) throws  AutosuggestException;

    void deletePPSearchableField(String siteKey , String fieldName) throws AutosuggestException;

    AutosuggestIndexResponse indexSuggestions(String siteKey) throws  AutosuggestException;

    AutosuggestIndexResponse indexSuggestions(String siteKey, String autosuggestThreshold) throws  AutosuggestException;

    Long fetchLatestIndexingTime(String siteKey) throws  AutosuggestException;
}
