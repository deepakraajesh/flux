package com.unbxd.skipper.autosuggest.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.unbxd.skipper.autosuggest.exception.AutosuggestStateException;
import com.unbxd.skipper.autosuggest.service.AutosuggestStateService;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.statemanager.StateManager;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.dao.StateDao;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

import static com.unbxd.skipper.states.model.ServeStateType.SELECT_TEMPLATE;
import static java.util.Objects.nonNull;

@Log4j2
public class AutosuggestStateServiceImpl implements AutosuggestStateService {

    private StateDao stateDao;
    private StateManager stateManager;

    @Inject
    public AutosuggestStateServiceImpl(@Named("autosuggest") StateManager stateManager,
                                       @Named("autosuggest") StateDao stateDao) {
        this.stateManager = stateManager;
        this.stateDao = stateDao;
    }

    @Override
    public StateContext setAutosuggestState(String siteKey,
                                            ServeState serveState) throws AutosuggestStateException {
        ServeState state = stateManager.getStateInstance(serveState.getStateType());
        StateContext stateContext = null;
        try {
            stateContext = stateDao.fetchState(siteKey);
        } catch (SiteNotFoundException e) {
            throw new AutosuggestStateException(404,"autosuggest state is not set");
        }
        state.setStateContext(stateContext);
        state.setStateManager(stateManager);
        Map<String,String> stateDataFromRequest = serveState.getStateData();
        if(nonNull(stateDataFromRequest) && !stateDataFromRequest.isEmpty())
            state.setStateData(stateDataFromRequest);
        else
            state.setStateData(stateContext.getServeState().getStateData());
        state.processState();
        if(nonNull(stateContext.getErrors()) && !stateContext.getErrors().isEmpty())
            throw new AutosuggestStateException(stateContext.getCode(),stateContext.getErrors());

        /** return the updated the state **/
        try {
            return stateDao.fetchState(siteKey);
        } catch (SiteNotFoundException ignored) {
            return null;
        }
    }

    private void setDefaultAutosuggestState(String siteKey) {
        ServeState defaultState = stateManager.getStateInstance(SELECT_TEMPLATE);
        StateContext stateContext = new StateContext();
        stateContext.setSiteKey(siteKey);
        stateContext.setServeState(defaultState);
        defaultState.setStateContext(stateContext);
        defaultState.setStateManager(stateManager);
        defaultState.processState();
    }

    @Override
    public StateContext getAutosuggestState(String siteKey) throws AutosuggestStateException {
        try {
            return stateDao.fetchState(siteKey);
        } catch (SiteNotFoundException e) {
            log.info("default autosuggest state is set for siteKey:"+siteKey);
            setDefaultAutosuggestState(siteKey);
        }
        try {
            return stateDao.fetchState(siteKey);
        } catch (SiteNotFoundException ignored){
            return null;
        }
    }
}
