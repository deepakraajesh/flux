package com.unbxd.skipper.autosuggest.service;

import com.unbxd.skipper.autosuggest.exception.AutosuggestStateException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.StateContext;

public interface AutosuggestStateService {
    StateContext setAutosuggestState(String siteKey,
                                     ServeState serveState) throws AutosuggestStateException;
    StateContext getAutosuggestState(String siteKey) throws AutosuggestStateException;
}
