package com.unbxd.skipper.states.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.config.Config;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyRequest;
import com.unbxd.skipper.relevancy.model.RelevancyResponse;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.console.model.ProductType.search;
import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Setter
@NoArgsConstructor
public class RelevancyJobStart extends RelevancyState {

    @JsonIgnore
    @Inject
    protected StateManager stateManager;
    @JsonIgnore
    protected StateContext stateContext;

    @Inject
    private RelevancyRemoteService relevancyRemoteService;
    @Inject
    private AnalyserService analyserService;
    @Inject
    private Config config;

    @Inject
    private Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors;

    @Inject
    private EventFactory eventFactory;


    @Override
    public void nextState() {
        stateManager.transitionStateBeforePersistence(stateContext, this);
    }

    @Override
    public void processState() {
        String siteKey = stateContext.getSiteKey();
        RelevancyRequest relevancyRequest = new RelevancyRequest(config, stateContext
                .getVertical(), stateContext.getLanguage(),
                stateContext.getFeedPlatform());
        relevancyRequest.setSecondaryLanguages(stateContext.getSecondaryLanguages());
        Call<RelevancyResponse> relevancyResponseCall = relevancyRemoteService
                .triggerRelevancyJob(RelevancyRemoteService.APPLICATION_JSON_HEADER, siteKey, relevancyRequest);
        createAnalyserCore(siteKey);

        try {
            Response<RelevancyResponse> relevancyResponseObj = relevancyResponseCall.execute();
            int statusCode = relevancyResponseObj.code();
            stateContext.setCode(statusCode);

            if(statusCode == 200) {
                stateContext.setWorkflowId(relevancyResponseObj.body().getWorkflowId());
                nextState();
            } else {
                String msg = "Error while parsing the relevancy service response";
                log.error(msg + " site:" + siteKey + " code:" + statusCode + " reason:"
                        + relevancyResponseObj.errorBody().string());
                stateContext.setErrors(msg);
            }
        } catch(IOException e) {
            String msg = "Error while parsing the relevancy service response";
            log.error(msg + " site:" + siteKey + " reason:" + e.getMessage());
            stateContext.setErrors(msg);
        }
    }

    @Override
    public void run() { processState(); }

    @Override
    public ServeStateType getStateType() { return RELEVANCY_JOB_START; }

    @Override
    public ServeStateType nextStateType() { return RELEVANCY_JOB_COMPLETE; }

    private void createAnalyserCore(String siteKey) {
        try {
            analyserService.createAnalyserCore(siteKey);
        } catch (AnalyserException e){
            log.error("Error while creating analyser core from analyser service , error message:  " + e.getMessage());
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
        this.stateContext = stateContext;
        this.stateContext.setErrors(errors);
        ServeStateType nextStateType = errors == null?RELEVANCY_JOB_COMPLETE:RELEVANCY_ERROR_STATE;
        ServeState nextState = stateManager.getStateInstance(nextStateType);
        this.stateContext.setServeState(nextState);
        this.stateContext.getServeState().setStateData(stats);

        // there should be atleast one successfull job and current set of job should not contain suggestedDictionaries
        if(stats.size() > 0 && (Collections.disjoint(jobs, JobType.suggestedDictionaries))) {
            log.info("Transistioning to " + nextStateType + " for site " + siteKey);
            stateManager.transitionStateBeforePersistence(stateContext, nextState);
        }
    }
}