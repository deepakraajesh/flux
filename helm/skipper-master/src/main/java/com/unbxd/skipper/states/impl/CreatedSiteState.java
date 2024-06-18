package com.unbxd.skipper.states.impl;

import com.google.inject.Inject;
import com.unbxd.skipper.plugins.Plugin;
import com.unbxd.skipper.plugins.exception.PluginException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.stats.StatsProcessor;

import static com.unbxd.skipper.states.model.ServeStateType.*;

public class CreatedSiteState extends CreateSiteState {

    @Inject
    private StatsProcessor statsProcessor;

    @Override
    public ServeStateType getStateType() { return SITE_CREATED; }

    @Override
    public void processState() {
        statsProcessor.logExecutionTime(SITE_CREATED.name(), stateContext.getSiteKey(),
                System.currentTimeMillis() - stateContext.getTimestamp());
        stateManager.persistState(stateContext);
    }
}
