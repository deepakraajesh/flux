package com.unbxd.skipper.states.statemanager;

import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.StateThreadExecutor;
import com.unbxd.skipper.states.dao.StateDao;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.toucan.eventfactory.Event;
import com.unbxd.toucan.eventfactory.EventBuilder;
import com.unbxd.toucan.eventfactory.EventTag;
import com.unbxd.toucan.exception.EventDispatchException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.unbxd.event.EventFactory.STATE_TRANSITION;
import static com.unbxd.skipper.model.Constants.EVENT_BUILDER;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static com.unbxd.toucan.eventfactory.EventTag.INFO;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Log4j2
@Getter
@Singleton
public class StateManager {

    private StateDao stateDao;
    private Injector injector;
    private EventFactory eventFactory;
    private StateThreadExecutor stateThreadExecutor;

    @Inject
    public StateManager(StateDao stateDao,
                        Injector injector,
                        EventFactory eventFactory,
                        StateThreadExecutor stateThreadExecutor) {
        this.stateDao = stateDao;
        this.injector = injector;
        this.eventFactory = eventFactory;
        this.stateThreadExecutor = stateThreadExecutor;
    }

    public ServeState getStateSnapshot(StateContext stateContext) {
        return stateContext.getServeState();
    }

    public void transitionStateInThread(StateContext stateContext, ServeState nextState) {
        persistState(stateContext);
        stateContext.setServeState(nextState);
        fireStateTransitionEvent(stateContext, INFO, "skipper transitioned to state: " + nextState.getStateType().name());

        executeStateInThread(nextState);
    }
    public void transitionState(StateContext stateContext, ServeState nextState) {
        persistState(stateContext);
        stateContext.setServeState(nextState);
        fireStateTransitionEvent(stateContext, INFO, "skipper transitioned to state: " + nextState.getStateType().name());

        executeState(nextState);
    }

    public void transitionErrorState(StateContext stateContext, ServeState errorState) {
        stateContext.setServeState(errorState);
        fireStateTransitionEvent(stateContext, ERROR, "Transitioning to error state: " + errorState.getStateType().name()
                + " : " + stateContext.getErrors());

        persistState(stateContext);
    }

    public void transitionStateBeforePersistence(StateContext stateContext, ServeState nextState) {
        stateContext.setServeState(nextState);
        fireStateTransitionEvent(stateContext, INFO,
                "skipper transitioned to state: " + nextState.getStateType().name());
        log.info("Transitioning to state: " + nextState.getStateType().name()
                + " for site:" + stateContext.getSiteKey());

        persistState(stateContext);
    }

    public ServeState getStateInstance(ServeStateType serveStateType) {
        Key<ServeState> serveStateKey = Key.get(ServeState.class, Names.named(serveStateType.name()));
        return this.injector.getInstance(serveStateKey);
    }

    public void executeStateInThread(ServeState state) {
        stateThreadExecutor.submitState(state);
    }

    public void executeState(ServeState state) { state.processState(); }

    public void persistState(StateContext stateContext) {
        stateDao.saveState(stateContext);
    }

    public void fireStateTransitionEvent(StateContext stateContext,
                                         EventTag eventTag,
                                         String msg) {
        String siteKey = stateContext.getSiteKey();
        siteKey = (isNotEmpty(siteKey) ? siteKey: "null");
        eventFactory.createAndFireEvent(stateContext.getEmail(), siteKey,
                System.currentTimeMillis() * 1000, siteKey, msg,
                eventTag, STATE_TRANSITION, Collections.emptyMap(), stateContext);
    }
}
