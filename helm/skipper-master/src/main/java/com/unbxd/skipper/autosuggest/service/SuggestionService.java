package com.unbxd.skipper.autosuggest.service;

import com.unbxd.autosuggest.model.AutosuggestIndexingStatus;
import com.unbxd.skipper.autosuggest.exception.SuggestionServiceException;
import com.unbxd.skipper.autosuggest.model.Suggestions;

public interface SuggestionService {

    Suggestions getSuggestions(String siteKey) throws SuggestionServiceException;

    void addSuggestions(String siteKey,
                        Suggestions suggestions,
                        boolean ignoreDuplicateFieldError) throws SuggestionServiceException;

    void deleteSuggestions(String siteKey,
                           Suggestions suggestions) throws SuggestionServiceException;

    void indexSuggestions(String siteKey) throws SuggestionServiceException;

    AutosuggestIndexingStatus getIndexingStatus(String siteKey) throws SuggestionServiceException;


}
