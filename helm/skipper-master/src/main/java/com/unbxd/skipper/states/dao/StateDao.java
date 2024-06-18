package com.unbxd.skipper.states.dao;

import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.StateContext;

import java.util.Map;
import java.util.NoSuchElementException;

public interface StateDao {

    String MONGO_ID = "_id";
    String SITEKEY = "siteKey";
    String SERVE_STATE = "serveState";
    String WORKFLOW_ID = "workflowId";

    String STATE_COLLECTION = "stateCollection";

    /** set the state for current site Key */
    void saveState(StateContext stateContext);

    /** fetch the current state of this site-key from DB */
    StateContext fetchState(String siteKey) throws NoSuchElementException, SiteNotFoundException;

    StateContext fetchState(String fieldName, String fieldValue) throws NoSuchElementException;

    long deleteSite(String siteKey);

    void reset(String siteKey, ServeState state);

    void updateState(String siteKey, Map<String, String> fields) throws SiteNotFoundException;
}
