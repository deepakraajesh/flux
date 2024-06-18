package com.unbxd.skipper.states.impl;

import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyRequest;
import com.unbxd.skipper.relevancy.model.RelevancyResponse;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.dao.StateDao;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.console.model.ProductType.search;
import static com.unbxd.skipper.relevancy.model.RelevancyRequest.getInstance;
import static com.unbxd.skipper.relevancy.service.RelevancyRemoteService.APPLICATION_JSON_HEADER;
import static com.unbxd.skipper.states.dao.StateDao.WORKFLOW_ID;
import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static java.util.Collections.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
public class DimensionMappingStart extends VirtualServeState {

    @Inject
    private Config config;

    @Inject
    private StateDao stateDao;

    @Inject
    private Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors;

    @Inject
    private EventFactory eventFactory;

    @Inject
    private RelevancyRemoteService relevancyRemoteService;

    protected static final ServeStateType[] previousStates =
            { API_UPLOAD_COMPLETE, PLATFORM_UPLOAD_COMPLETE, PIM_UPLOAD_COMPLETE, FILE_FEED_UPLOAD_COMPLETE,
                    PIM_ERROR, PLATFORM_UPLOAD_ERROR, API_UPLOAD_ERROR };

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
        if(validatePrevState(previousStates, actualPrevState)) {
            triggerRelevancy();
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
        return DIMENSION_MAPPING_START;
    }

    @Override
    public ServeStateType nextStateType() {
        return null;
    }

    @Override
    public void run() { }

    /*     State Process Methods      */

    private void triggerRelevancy() {
        RelevancyRequest request = getInstance(config,
                new String[]{JobType.dimensionMap.toString(), JobType.variants.toString()},
                stateContext.getVertical(), stateContext.getLanguage(),
                stateContext.getFeedPlatform(), emptyList());
        Call<RelevancyResponse> relevancyResponse = relevancyRemoteService
                .triggerRelevancyJob(APPLICATION_JSON_HEADER,
                stateContext.getSiteKey(), request);
        try {
            Response<RelevancyResponse> response = relevancyResponse.execute();
            stateContext.setCode(response.code());

            if (response.isSuccessful()) {
                this.setStateData(singletonMap(WORKFLOW_ID,
                        response.body().getWorkflowId()));
            } else {
                String errorStr = "Error while triggering relevancy job for siteKey[" +
                        stateContext.getSiteKey() + "] for dimension mapping: " +
                        response.errorBody().string();
                stateContext.setErrors(errorStr);
                log.error(errorStr);
            }
        } catch (IOException e) {
            String errorStr = "Error while triggering relevancy job for siteKey[" +
                    stateContext.getSiteKey() + "] for dimension mapping: " +
                    e.getMessage();
            stateContext.setErrors(errorStr);
            log.error(errorStr);
        }
    }

    @Override
    public void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobs) {
        Map<String, String> stats = new HashMap<>();
        String siteKey = stateContext.getSiteKey();
        String errors = null;
        for (JobType job : jobs) {
            try {
                // If outputProcessorMap doesn't contain current enum,
                // Then ignore the response(This is done to handle autoNer map job
                if (!updateProcessors.containsKey(job)) {
                    String msg = "No updateProcessor has been configured for the job: " + job;
                    log.error(msg);
                    continue;
                }

                int count = updateProcessors.get(job).update(siteKey, job, search);
                log.info(job + " relevancy workflow got updated for site " + siteKey);
                stats.put(job.name(), String.valueOf(count));
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
        try {
            stateContext = stateDao.fetchState(siteKey);
        } catch (SiteNotFoundException e) {
            log.error("Exception while trying to fetch site: " + siteKey);
        }
        this.stateContext = stateContext;
        this.stateContext.setErrors(errors);
        ServeStateType nextStateType = DIMENSION_UPDATE;
        ServeState nextState = stateManager.getStateInstance(nextStateType);
        this.stateContext.setServeState(nextState);
        this.stateContext.getServeState().setStateData(stats);
        stateManager.transitionStateBeforePersistence(stateContext, nextState);
    }
}

