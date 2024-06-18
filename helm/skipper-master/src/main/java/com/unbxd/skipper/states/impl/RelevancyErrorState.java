package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.console.model.ProductType.search;
import static com.unbxd.skipper.relevancy.model.JobType.getEnrichedJobs;
import static com.unbxd.skipper.relevancy.model.JobType.suggestedDictionaries;
import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
public class RelevancyErrorState extends ErrorVirtualServeState {

    @Inject
    private Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors;

    @Inject
    private EventFactory eventFactory;

    @Override
    public void run() { }

    @Override
    public void nextState() {
        stateManager.transitionErrorState(stateContext,this);
    }

    @Override
    public ServeStateType nextStateType() { return null; }

    @Override
    public ServeStateType getStateType() { return RELEVANCY_ERROR_STATE; }

    @Override
    @JsonIgnore
    public ServeStateType getPrevStateType() { return RELEVANCY_JOB_START; }

    @Override
    public void processState() {
        log.info("Processing Relevancy Error State for siteKey: " + stateContext.getSiteKey());
        nextState();
    }

    /**
     * <pre>
     * When current state in RelevancyJobError state and relevancy job callback is received, then following should be update:
     * 1. update those relevancy job which has failed in the previous state.
     * How do we get to know which job has failed? If in "stats" metadata, the value is stored as "0" or "-1" or doesnt exist
     * 2. Update the suggestedjobs, Jobs such that suggestedByAI synonyms or concepts should work fine
     * </pre>
     * @param stateContext
     * @param jobs
     */
    @Override
    public void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobs) {
        String siteKey = stateContext.getSiteKey();
        String errors = null;
        Map<String, String> stats = getStateData();
        stats = stats == null? new HashMap<>(): stats;
        for (JobType job : jobs) {
            try {
                // If outputProcessorMap doesn't contain current enum,
                // Then ignore the response(This is done to handle autoNer map job
                if (!updateProcessors.containsKey(job)) {
                    String msg = "No updateProcessor has been configured for the job: " + job;
                    log.error(msg);
                    continue;
                }
                if((stats.containsKey(job) &&
                        (stats.get(job).equals("0") || stats.get(job).equals("-1"))) ||
                        suggestedDictionaries.contains(job)||
                        getEnrichedJobs().contains(job)) {
                    int count = updateProcessors.get(job).update(siteKey, job, search);
                    log.info(job + " relevancy workflow got updated for site " + siteKey);
                    stats.put(job.name(), String.valueOf(count));
                }
            } catch (Exception e) {
                errors = "Error while processing job:" + job.name() + " for siteKey: " + siteKey
                        + " , failed with error:" + e.getMessage() + " , error type:" + e.getClass();
                eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                        siteKey, System.currentTimeMillis() * 1000, siteKey,
                        "Error while processing relevancy job : " + errors,
                        ERROR, RELEVANCY_STATUS, emptyMap(), null);

                stats.put(job.name(), EMPTY);
                log.error(errors);
            }
        }
        this.stateContext = stateContext;
        this.stateContext.setErrors(errors);
        ServeStateType nextStateType = errors == null?RELEVANCY_JOB_COMPLETE:RELEVANCY_ERROR_STATE;
        ServeState nextState = stateManager.getStateInstance(nextStateType);
        this.stateContext.setServeState(nextState);
        this.stateContext.getServeState().setStateData(stats);
        // there should be atleast one successfull job and current set of job should not contain suggestedDictionaries
        if(stats.size() > 0 && Collections.disjoint(jobs, suggestedDictionaries)) {
            log.info("Transistioning to " + nextStateType + " for site " + siteKey);
            stateManager.transitionStateBeforePersistence(stateContext, nextState);
        }
    }
}
