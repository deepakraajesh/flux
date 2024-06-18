package com.unbxd.skipper.states.statemanager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.StateThreadExecutor;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.dao.StateDao;

import java.util.Map;

public class AutosuggestStateManager extends StateManager {
    @Inject
    public AutosuggestStateManager(Injector injector,
                                   EventFactory eventFactory,
                                   @Named("autosuggest") StateDao stateDao,
                                   StateThreadExecutor stateThreadExecutor) {
       super(stateDao, injector, eventFactory, stateThreadExecutor);
    }
}
