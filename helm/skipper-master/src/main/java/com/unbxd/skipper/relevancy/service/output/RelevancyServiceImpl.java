package com.unbxd.skipper.relevancy.service.output;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.console.model.ProductType;
import com.unbxd.event.EventFactory;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.FSSearchableField;
import com.unbxd.field.model.SearchableFieldsResponse;
import com.unbxd.field.service.FieldService;
import com.unbxd.pim.workflow.service.PIMService;
import com.unbxd.search.feed.SearchFeedService;
import com.unbxd.search.feed.model.FeedStatus;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.feed.exception.FeedException;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;
import com.unbxd.skipper.feed.service.FeedService;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.*;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.relevancy.service.RelevancyRemoteService;
import com.unbxd.skipper.relevancy.service.RelevancyService;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ro.pippo.core.HttpConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.unbxd.config.Config.AWS_DEFAULT_REGION;
import static com.unbxd.skipper.relevancy.model.JobType.*;
import static com.unbxd.skipper.states.model.ServeStateType.*;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static com.unbxd.toucan.eventfactory.EventTag.INFO;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

@Log4j2
public class RelevancyServiceImpl implements RelevancyService {

    private AmazonS3 s3Client;
    private PIMService pimService;
    private SiteService siteService;
    private FieldService fieldService;
    private RelevancyDao relevancyDao;
    private StateManager stateManager;
    private EventFactory eventFactory;
    private SearchFeedService searchFeedService;
    private RelevancyRemoteService relevancyRemoteService;
    private Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors;
    private FeedService feedService;
    private ObjectMapper mapper;
    private Config config;

    private static final String RELEVANCY_TRIGGER = "relevancy_trigger";
    private static final String SELF_SERVE_S_3_BUCKET = "selfServe.s3Bucket";
    private static final String S3_ACCESS_DENIED_ERROR_CODE = "AccessDenied";

    private static final String DOMAIN_NAME_PROPERTY = "domain.name";

    @Inject
    public RelevancyServiceImpl(PIMService pimService,
                                SiteService siteService,
                                StateManager stateManager,
                                RelevancyDao relevancyDao,
                                FieldService fieldService,
                                EventFactory eventFactory,
                                SearchFeedService searchFeedService,
                                RelevancyRemoteService relevancyRemoteService,
                                Map<JobType, RelevancyOutputUpdateProcessor> updateProcessors,
                                FeedService feedService,
                                Config config) {
        this.pimService = pimService;
        this.siteService = siteService;
        this.stateManager = stateManager;
        this.relevancyDao = relevancyDao;
        this.fieldService = fieldService;
        this.eventFactory = eventFactory;
        this.searchFeedService = searchFeedService;
        this.relevancyRemoteService = relevancyRemoteService;
        String awsDefaultRegion = config.getProperty(AWS_DEFAULT_REGION);
        if (isNull(awsDefaultRegion))
            throw new IllegalArgumentException(AWS_DEFAULT_REGION + " property not set");
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(awsDefaultRegion) // The first region to try your request against
                .withForceGlobalBucketAccessEnabled(true) // If a bucket is in a different region, try again in the correct region
                .build();
        this.updateProcessors = updateProcessors;
        this.feedService = feedService;
        this.config = config;
        mapper = new ObjectMapper();
    }


    @Override
    public APIResponse<RelevancyResponse> triggerJob(String siteKey, RelevancyRequest jobRequest)
            throws SiteNotFoundException {
        StateContext stateContext = siteService.getSiteStatus(siteKey);
        jobRequest.setStatusWebhook(config.getProperty(DOMAIN_NAME_PROPERTY));
        jobRequest.setSecondaryLanguages(stateContext.getSecondaryLanguages());
        if(stateContext.getVertical() == null || stateContext.getLanguage() == null) {
            String msg = "vertical and language is not set";
            log.error(msg);
            return new APIResponse<>(Collections.singletonList(new ErrorResponse(msg, 400)));
        }
        ServeStateType prevStateType = stateContext.getServeState().getStateType();
        ServeState relevancyStartState = stateManager.getStateInstance(RELEVANCY_JOB_START);

        relevancyStartState.setStateContext(stateContext);
        relevancyStartState.setStateManager(stateManager);

        jobRequest.setVertical(stateContext.getVertical());
        jobRequest.setLanguage(stateContext.getLanguage());

        if (checkStateBeforeRelevancyTrigger(prevStateType)) {
            relevancyStartState.processState();
            int statusCode = stateContext.getCode();
            if (statusCode == 200) {
                RelevancyResponse relevancyResponse = new
                        RelevancyResponse(statusCode, stateContext.getWorkflowId());
                return new APIResponse<>(relevancyResponse);
            } else {
                eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                        siteKey, System.currentTimeMillis() * 1000, siteKey,
                        "Error while triggering relevancy job",
                        ERROR, RELEVANCY_TRIGGER, emptyMap(),
                        null);
                return new APIResponse<>(Collections.singletonList(ErrorResponse
                        .getInstance("Error while triggering relevancy job, status code: " + statusCode)));
            }
        } else if(prevStateType == INDEXING_STATE){
            Call<RelevancyResponse> relevancyResponseCall = relevancyRemoteService
                    .triggerRelevancyJob(RelevancyRemoteService.APPLICATION_JSON_HEADER, siteKey, jobRequest);

            eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                    siteKey, System.currentTimeMillis() * 1000, siteKey,
                    "Triggering rerun relevance job with job " +
                            (jobRequest.getJobs() != null?jobRequest.getJobs().toString():"[]"),
                    INFO, RELEVANCY_TRIGGER, emptyMap(),
                    null);
            try {
                Response<RelevancyResponse> relevancyResponseObj = relevancyResponseCall.execute();
                if(relevancyResponseObj.isSuccessful()) {
                    stateContext.setWorkflowId(relevancyResponseObj.body().getWorkflowId());
                    stateManager.persistState(stateContext);
                    RelevancyResponse relevancyResponse = new
                            RelevancyResponse(200, stateContext.getWorkflowId());
                    return new APIResponse<>(relevancyResponse);
                } else {
                    String msg = "Error while triggering rerun relevance job with job " +
                            jobRequest.getJobs().toString() + " reason: " + relevancyResponseObj.errorBody().string();
                    eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                            siteKey, System.currentTimeMillis() * 1000, siteKey, msg,
                            ERROR, RELEVANCY_TRIGGER, emptyMap(),
                            null);
                    log.error(msg);
                    return new APIResponse<>(Collections.singletonList(ErrorResponse
                            .getInstance(relevancyResponseObj.code())));
                }
            } catch (IOException e) {
                String msg = " " +
                        jobRequest.getJobs().toString() + " reason: " + e.getMessage();
                eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                        siteKey, System.currentTimeMillis() * 1000, siteKey, msg,
                        ERROR, RELEVANCY_TRIGGER, emptyMap(),
                        null);
                log.error(msg);
                return new APIResponse<>(Collections.singletonList(ErrorResponse
                        .getInstance(500)));
            }
        } else {
            String msg = "Please complete the FTU(first time user) flow before triggering relevancy job";
            log.error(msg);
            return new APIResponse<>(Collections.singletonList(new ErrorResponse(msg, 400)));
        }
    }

    @Override
    public void processRelevancyStatus(String siteKey, String workflowId, String workflowName)
            throws RelevancyServiceException, SiteNotFoundException {
        WorkflowStatus workflowStatus = fetchRelevancyStatus(siteKey, workflowId);

        Map<String, String> stats = new HashMap<>();
        String errors = null;

        if (workflowStatus == null || isEmpty(workflowStatus.getJobs())) {
            try {
                // sleep for 30s
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // retrying for 1 time
            workflowStatus = fetchRelevancyStatus(siteKey, workflowId);
            if (workflowStatus == null || isEmpty(workflowStatus.getJobs())) {
                // This is a case for internal server error,
                // But in this case we have to transition the state to RELEVANCY_JOB_ERROR otherwise,
                // there will no action item on the UI
                errors = "Error while processing relevancy job for siteKey: "
                        + siteKey + " with workflowId: " + workflowId + ". Received "
                        + "empty jobs list or no workflow status from Relevancy Service.";
                eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                        siteKey, System.currentTimeMillis() * 1000, siteKey,
                        "Error while processing relevancy job : " + errors,
                        ERROR, ServeState.RELEVANCY_STATUS, emptyMap(), null);
                log.error(errors);
                return;
            }
        }
        StateContext context = siteService.getSiteStatus(siteKey);
        List<JobType> jobTypes = new ArrayList<>(workflowStatus.getJobs().length);

        for (Job job : workflowStatus.getJobs()) {
            Output[] outputs = job.getOutputs();
            if (isNotEmpty(outputs)) {
                for (Output output : outputs) {
                    String bucketPath = output.getPath();
                    RelevancyJobMetric feedMetric = getMetric(bucketPath,
                            "feed_" + job.getName() + "_metrics.txt");
                    RelevancyJobMetric queryMetrics = getMetric(bucketPath,
                            "query_" + job.getName() + "_metrics.txt");
                    RelevancyOutputModel relevancyOutputModel = new
                            RelevancyOutputModel(siteKey, workflowId, bucketPath,
                            feedMetric, queryMetrics);
                    relevancyDao.saveRelevancyOutput(job.getName(), relevancyOutputModel);
                    jobTypes.add(job.getName());
                }
            }
        }
        if(jobTypes.contains(JobType.synonyms))
            triggerImpactMetricsWorkflow(siteKey, JobType.enrichSynonyms);
        else if(jobTypes.contains(JobType.suggestedSynonyms))
            triggerImpactMetricsWorkflow(siteKey, enrichSuggestedSynonyms);

        if(jobTypes.size() > 0)
            // TODO: remove storing data and then fetching again from relevancy dao
            stateManager.getStateInstance(context.getServeState().getStateType()).
                updateRelevancyJobOutput(context, jobTypes);
    }

    private String[] getJobsToTigger(JobType jobType) {
        if (jobType == enrichSuggestedSynonyms) {
            return new String[] {recommendSynonyms
                    .name(), enrichSuggestedSynonyms.name()};
        }
        return new String[] {enrichSynonyms.name()};
    }

    private void triggerImpactMetricsWorkflow(String siteKey,
                                              JobType job)  {
        StateContext stateContext;
        try {
            stateContext = siteService.getSiteStatus(siteKey);
        } catch (SiteNotFoundException e) {
            log.error("Error while triggering Impact Metrics Workflow , No Site found error , sitekey:" + siteKey);
            return;
        }

        RelevancyRequest relevancyRequest = new RelevancyRequest("impact-metrics",
                getJobsToTigger(job), config, stateContext.getVertical(),
                stateContext.getLanguage(), stateContext.getFeedPlatform());
        relevancyRequest.setSecondaryLanguages(stateContext.getSecondaryLanguages());
        Call<RelevancyResponse> relevancyImpactCall = relevancyRemoteService
                .triggerRelevancyJob(RelevancyRemoteService.APPLICATION_JSON_HEADER, siteKey, relevancyRequest);

        relevancyImpactCall.enqueue(new Callback<RelevancyResponse>() {
            @SneakyThrows
            @Override
            public void onResponse(Call<RelevancyResponse> call, Response<RelevancyResponse> relevancyResponse) {
                if (relevancyResponse.isSuccessful()) {
                    log.info("Impact metrics successfully triggered for siteKey:" + siteKey);
                } else {
                    String msg = "Error while making a call to relevancy workflow svc for impact metrics workflow";
                    log.error(msg + " site:" + siteKey + " code:" + relevancyResponse.code() + " reason:"
                            + relevancyResponse.errorBody().string());
                }
            }
            @Override
            public void onFailure(Call<RelevancyResponse> call, Throwable t) {
                String msg = "Error while parsing the relevancy workflow service for impact metrics , response";
                log.error(msg + " site:" + siteKey + " reason:" + t.getLocalizedMessage());
            }
        });
    }


    private void updateState(String siteKey, ServeStateType state, String errors, Map<String, String> metaData)
            throws SiteNotFoundException {
        siteService.updateContext(siteKey, null, errors, state, metaData);
    }

    @Override
    public APIResponse<RelevancyOutputModel> getFields(String siteKey, JobType type) {
        try {
            RelevancyOutputModel relevancyOutputModel = relevancyDao.fetchRelevancyOutput(type, siteKey);
            return new APIResponse<>(relevancyOutputModel);
        } catch(NoSuchElementException e) {
            log.error("Sitekey not found: ", e);
        }
        return null;
    }

    @Override
    public GetSearchableFieldsResponse getSearchableFields(String siteKey, PageRequest request)
            throws RelevancyServiceException, SiteNotFoundException {
        GetSearchableFieldsResponse response = new GetSearchableFieldsResponse();
        SearchableFieldsResponse fieldServiceSearchableFieldsResponse =
                getSearchableFieldsFromFieldService(siteKey,request);
        List<FSSearchableField> searchableFieldsFromFieldService = fieldServiceSearchableFieldsResponse
                .getSearchableFields();
        if(nonNull(searchableFieldsFromFieldService) && !searchableFieldsFromFieldService.isEmpty()) {
            Boolean hasJobRun = hasJobRun(siteKey, JobType.searchableFields);
            response.setRelevancyJobRun(hasJobRun);
            List<SearchableField> searchableFieldsFromRelevancy = null;
            if(hasJobRun){
                List<String> fieldNames = getFieldNames(searchableFieldsFromFieldService);
                searchableFieldsFromRelevancy  = relevancyDao.getSearchableFields(siteKey, fieldNames);
            }
            List<SearchableField> resultantSearchableFields;
            if (isNull(searchableFieldsFromRelevancy) || searchableFieldsFromRelevancy.isEmpty())
                resultantSearchableFields = getResultantDataFromFieldServiceFields(searchableFieldsFromFieldService);
            else
                resultantSearchableFields = getCombinedDataOfSearchableField(
                        searchableFieldsFromFieldService,
                        searchableFieldsFromRelevancy);
            response.setCount(fieldServiceSearchableFieldsResponse.getCount());
            response.setTotal(fieldServiceSearchableFieldsResponse.getTotal());
            response.setSearchableFields(resultantSearchableFields);
        } else
            log.info("no data found while fetching searchableFields from field service for siteKey:" + siteKey);
        return response;
    }

    @Override
    public void updateSearchableFieldsInFieldService(String siteKey, List<SearchableField> searchableFields)
            throws RelevancyServiceException {
        try {
            if (isNull(searchableFields) || searchableFields.isEmpty())
                throw new RelevancyServiceException(HttpConstants.StatusCode.BAD_REQUEST, "fields are not passed");
            else {
                List<FSSearchableField> transformedFieldList = FSSearchableField.transform(searchableFields);
                fieldService.updateSearchableFields(siteKey, transformedFieldList);
            }
        } catch (FieldException e) {
            throw new RelevancyServiceException(e.getCode(), e.getMessage());
        }
    }

    @Override
    public String triggerCatalogIndexing(String siteKey) throws RelevancyServiceException, SiteNotFoundException {
        try {
            StateContext site = siteService.getSiteStatus(siteKey);
            if (site == null) {
                String msg = "No site found with siteKey:" + siteKey;
                log.info(msg);
                throw new RelevancyServiceException(400, msg);
            }
            String feedId = feedService.reIndexSearchFeed(siteKey);
            Map<String, String> data = new HashMap<>();
            data.put("feedId", feedId);
            siteService.appendStateData(siteKey, data);
            return feedId;
        } catch (FeedException e) {
            throw new RelevancyServiceException(e.getStatusCode(), e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public FeedIndexingStatus getStatus(String siteKey,
                                        String feedId,
                                        Integer count,
                                        String type) throws RelevancyServiceException, SiteNotFoundException {
        try {
            Response<FeedStatus> feedStatusResponse = searchFeedService
                    .feedStatus(fieldService.getSiteDetails(siteKey)
                                    .getApiKey(), siteKey, feedId, count,
                            type).execute();
            if (!feedStatusResponse.isSuccessful()) {
                log.error("Error while fetching feed status for site: " + siteKey
                        + " reason:" + feedStatusResponse.errorBody().string());
                throw new RelevancyServiceException(500, "Error while fetching feed status from search service");
            }
            FeedStatus feedStatus = feedStatusResponse.body();
            if (feedStatus == null) {
                return new FeedIndexingStatus();
            }
            if (feedStatus.isCompleted()) {
                siteService.removeData(siteKey, "feedId", feedId);
            }
            return new FeedIndexingStatus(feedStatus.getStatus(),
                    feedStatus.getMessage(), feedStatus.getDuration(), feedStatus.getErrors());
        } catch (IOException | FieldException e) {
            log.error("Error while fetching feed status for site: " + siteKey + " reason:" + e.getMessage());
            throw new RelevancyServiceException(500, "Error while fetching feed status from search service");
        }
    }

    @Override
    public void reset(String cookie, String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException {
        if (!updateProcessors.containsKey(jobType))
            throw new RelevancyServiceException(400, "Invalid jobType passed");
        updateProcessors.get(jobType).reset(cookie, siteKey, jobType, productType);
    }

    @Override
    public Map<String, String> uploadFileToS3(String siteKey, File file) throws RelevancyServiceException {
        if (file.length() > 1024 * 1024 * 1024)
            throw new RelevancyServiceException(400, ErrorCode.FileSizeNotSupported.getCode(),
                    "file size cannot be greater than 1GB");
        if (!file.getName().endsWith(".csv"))
            throw new RelevancyServiceException(400, ErrorCode.FileFormatNotSupported.getCode(),
                    "only csv format is supported");
        return uploadFileToS3(siteKey, null, file);
    }

    @Override
    public Map<String, String> uploadFileToS3(String siteKey, String prefix, File file) throws RelevancyServiceException {
        if (file.length() > 1024 * 1024 * 1024)
            throw new RelevancyServiceException(400, ErrorCode.FileSizeNotSupported.getCode(),
                    "file size cannot be greater than 1GB");
        String bucketName = config.getProperty(SELF_SERVE_S_3_BUCKET);
        String mewFileName = java.time.LocalDateTime.now() + "_" + file.getName();
        String key = nonNull(prefix) ? prefix + "/" + siteKey + "/" + mewFileName : siteKey + "/" + mewFileName;
        try {
            s3Client.putObject(bucketName, key, file);
        } catch (AmazonS3Exception e) {
            if (S3_ACCESS_DENIED_ERROR_CODE.equals(e.getErrorCode()))
                throw new RelevancyServiceException(500, ErrorCode.S3WriteAccessDenied.getCode(),
                        "s3 write access denied");
            else
                throw e;
        }
        String s3Location = "s3://" + bucketName + "/" + key;
        log.info("file uploaded for siteKey:" + siteKey + " at s3 location : " + s3Location);
        boolean deleted = file.delete();
        if (!deleted) log.error("deletion of file:" + file.toString() + "failed");
        Map<String, String> result = new HashMap<>(1);
        result.put("s3Location", s3Location);
        return result;
    }

    /**
     * NOTE: this will be removed very soon
     * this will tell if the state is RELEVANCY_JOB_COMPLETE state or INDEXING STATE
     * @param siteKey
     * @param job
     * @return
     */
    @Deprecated
    @Override
    public boolean hasJobRun(String siteKey, JobType job) throws SiteNotFoundException {
        StateContext state = siteService.getSiteStatus(siteKey);
        return state.getServeState().getStateType().equals(RELEVANCY_JOB_COMPLETE) ||
                state.getServeState().getStateType().equals(INDEXING_STATE);
    }

    private String fetchContentFromBucket(String bucketPath) {
        AmazonS3URI amazonS3URI = new AmazonS3URI(bucketPath);
        return s3Client.getObjectAsString(amazonS3URI.getBucket(), amazonS3URI.getKey());
    }

    private RelevancyJobMetric getMetric(String bucketPath, String fileName) throws RelevancyServiceException {
        AmazonS3URI amazonS3URI = new AmazonS3URI(bucketPath);
        String[] tokens = amazonS3URI.getKey().split("/");
        String parentPath = String.join("/", Arrays.copyOf(tokens, tokens.length - 1));
        try {
            String content = s3Client.getObjectAsString(amazonS3URI.getBucket(), parentPath + "/" + fileName);
            return (content != null)?new RelevancyJobMetric(content):null;
        } catch (AmazonS3Exception e) {
            if(e.getStatusCode() == 404)
                // File doesn't exists
                return null;
            throw new RelevancyServiceException(500, e.getMessage());
        }
    }

    private SearchableFieldsResponse getSearchableFieldsFromFieldService(String siteKey,
                                                                         PageRequest request)
            throws RelevancyServiceException {
        SearchableFieldsResponse fieldServiceSearchableFieldsResponse;
        try {
            fieldServiceSearchableFieldsResponse = fieldService.getSearchableFields(siteKey, request);
        } catch (FieldException e) {
            throw new RelevancyServiceException(e.getCode(), e.getMessage());
        }
        return fieldServiceSearchableFieldsResponse;
    }

    @Override
    public WorkflowStatus fetchRelevancyStatus(String siteKey, String workflowId)
            throws RelevancyServiceException {
        try {
            Call<WorkflowStatus> statusCallObj = relevancyRemoteService.
                    getWorkflowStatus(siteKey, workflowId);
            Response<WorkflowStatus> statusResponse = statusCallObj.execute();

            if (statusResponse.code() == 200) {
                return statusResponse.body();
            } else {
                String error = statusResponse.errorBody().string();
                log.error("Error while fetching relevancy status for siteKey: " + siteKey +
                        " and workflowId: " + workflowId + " : ", error);
                eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                        siteKey, System.currentTimeMillis() * 1000, siteKey,
                        "Error while fetching relevancy job status: "+ error,
                        ERROR, ServeState.RELEVANCY_STATUS, emptyMap(), null);
                throw new RelevancyServiceException(500, error);
            }
        } catch (IOException e) {
            log.error("Exception while fetching relevancy status for siteKey: "
                    + siteKey + " and workflowId: " + workflowId + " : ", e);
            eventFactory.createAndFireEvent(eventFactory.getEmail("relevancy-bot"),
                    siteKey, System.currentTimeMillis() * 1000, siteKey,
                    "Error while fetching relevancy job status: "
                    + e.getMessage(), ERROR, ServeState.RELEVANCY_STATUS,
                    emptyMap(), null);
            throw new RelevancyServiceException(500, "Infra error occurred, please retry(or reload the page)");
        }
    }

    private List<String> getFieldNames(List<FSSearchableField> searchableFieldsFromFieldService) {
        List<String> fieldNames = new ArrayList<>(searchableFieldsFromFieldService.size());
        searchableFieldsFromFieldService.forEach(searchableField -> fieldNames.add(searchableField.getFieldName()));
        return fieldNames;
    }

    private List<SearchableField> getCombinedDataOfSearchableField(
            List<FSSearchableField> searchableFieldsFromFieldService,
            List<SearchableField> searchableFieldsFromRelevancy) {

        List<SearchableField> resultantSearchableFields = new ArrayList<>(searchableFieldsFromFieldService.size());
        Map<String, SearchableField> fieldNameToRelevancySearchableFieldMapping =
                new HashMap<>(searchableFieldsFromRelevancy.size());
        for (SearchableField searchableField : searchableFieldsFromRelevancy)
            fieldNameToRelevancySearchableFieldMapping.put(searchableField.getFieldName(), searchableField);

        SearchableField temporarySearchableField;
        for (FSSearchableField searchableFieldFromFieldService : searchableFieldsFromFieldService) {
            if (fieldNameToRelevancySearchableFieldMapping.containsKey(searchableFieldFromFieldService.getFieldName())) {
                temporarySearchableField =
                        fieldNameToRelevancySearchableFieldMapping.get(
                                searchableFieldFromFieldService.getFieldName()
                        );
                SearchWeightage aiRecc = temporarySearchableField.getSearchWeightage();
                temporarySearchableField.setAiRecc(aiRecc);
                temporarySearchableField.setSearchWeightageFromFieldServiceData(
                        searchableFieldFromFieldService.getSearchWeightage()
                );
            } else
                temporarySearchableField = SearchableField.getSearchableField(searchableFieldFromFieldService);
            resultantSearchableFields.add(temporarySearchableField);
        }

        return resultantSearchableFields;
    }

    private List<SearchableField> getResultantDataFromFieldServiceFields(List<FSSearchableField>
                                                                                 searchableFieldsFromFieldService) {
        List<SearchableField> resultantSearchableFields = new ArrayList<>(searchableFieldsFromFieldService.size());
        SearchableField temporarySearchableField;
        for (FSSearchableField searchableFieldFromFieldService : searchableFieldsFromFieldService) {
            temporarySearchableField = SearchableField.getSearchableField(searchableFieldFromFieldService);
            resultantSearchableFields.add(temporarySearchableField);
        }
        return resultantSearchableFields;
    }

    private boolean checkStateBeforeRelevancyTrigger(ServeStateType previousState) {
        return previousState == RELEVANCY_JOB_COMPLETE ||
                previousState == RELEVANCY_ERROR_STATE ||
                previousState == RELEVANCY_JOB_START ||
                previousState == AI_SETUP ||
                previousState == MANUAL_SETUP ||
                previousState == SETUP_SEARCH;
    }

    private ServeStateType getRelevancyNextState(String siteKey,
                                                 boolean isError) {
        try     {
            ServeStateType stateType = siteService.getSiteStatus(siteKey)
                    .getServeState().getStateType();
            if (stateType == DIMENSION_MAPPING_START) {
                return DIMENSION_UPDATE;
            }
        } catch (SiteNotFoundException e) {
            log.error("Exception while trying to fetch site: "
                    + siteKey + e.getMessage());
        }
        return (isError) ? RELEVANCY_ERROR_STATE : RELEVANCY_JOB_COMPLETE;
    }
}
