package com.unbxd.skipper.states.impl;

import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.unbxd.console.model.ProductType.search;
import static com.unbxd.skipper.relevancy.model.JobType.suggestedJobMap;
import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
public class IndexingVirtualState extends VirtualServeState {
    protected static final ServeStateType[] previousStates = {RELEVANCY_JOB_COMPLETE, RELEVANCY_ERROR_STATE,
            INDEXING_STATE};

    @Inject
    private Config config;
    @Inject
    private EventFactory eventFactory;
    @Inject
    private RelevancyDao relevancyDao;
    @Inject
    private RelevancyRemoteService relevancyRemoteService;
    @Inject
    private Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors;


    @Override
    public ServeStateType getPrevStateType() {
        return null;
    }

    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        ServeStateType actualPrevState = stateContext.getServeState().getStateType();
        String siteKey = stateContext.getSiteKey();
        if(validatePrevState(previousStates, actualPrevState)) {
            nextState();
        } else {
            String errMsg = "Previous State Mismatch, expected: " + Arrays.toString(previousStates) +
                    " but got : " + actualPrevState;
            stateContext.setErrors(errMsg);
            log.error(errMsg);
        }
    }

    @Override
    public ServeStateType getStateType() {
        return ServeStateType.INDEXING_STATE;
    }

    @Override
    public ServeStateType nextStateType() {
        return null;
    }

    @Override
    public void run() {

    }

    /**
     * <pre>
     * When current state in RelevancyJobError state and relevancy job callback is received, then following should be update:
     * 1. Update the dictionaryJobs as corresponding suggestedDictionaryjobs
     * 2. Update the suggestedDictionaryjobs, Jobs such that suggestedByAI synonyms or concepts should work fine
     *
     * No need to change the state, once the operation is complete
     * </pre>
     * @param stateContext
     * @param jobs
     */
    @Override
    public void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobs) {
        String siteKey = stateContext.getSiteKey();
        String errors = null;
        Map<String, String> stats = getStateData();
        stats = stats == null? Collections.emptyMap(): stats;
        for (JobType job : jobs) {
            try {
                // If outputProcessorMap doesn't contain current enum,
                // Then ignore the response(This is done to handle autoNer map job
                if (!updateProcessors.containsKey(job)) {
                    String msg = "No updateProcessor has been configured for the job: " + job;
                    log.error(msg);
                    continue;
                }
                job = getSuggestedJob(job, siteKey);
                updateProcessors.get(job).update(siteKey, job, search);
                log.info(job + " relevancy workflow got updated for site " + siteKey);

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
    }

    private JobType getSuggestedJob(JobType job,
                                    String sitekey) {
        if (suggestedJobMap.containsKey(job)) {
            JobType suggestedJob = suggestedJobMap.get(job);
            cloneRelevanceData(job, sitekey, suggestedJob);
            return suggestedJob;
        }
        return job;
    }

    private void cloneRelevanceData(JobType job,
                                    String sitekey,
                                    JobType suggestedJob) {
            relevancyDao.saveRelevancyOutput(suggestedJob,
                    relevancyDao.fetchRelevancyOutput(job, sitekey));
    }
}
