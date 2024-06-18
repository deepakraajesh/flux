package com.unbxd.skipper.relevancy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.console.model.*;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.feed.exception.IndexingException;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;
import com.unbxd.skipper.feed.model.IndexingStatus;
import com.unbxd.skipper.feed.service.IndexingService;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.*;
import com.unbxd.skipper.relevancy.service.RelevancyService;
import com.unbxd.skipper.search.exception.FacetStatServiceException;
import com.unbxd.skipper.search.model.PathFacetDetail;
import com.unbxd.skipper.search.model.RangeFacetDetail;
import com.unbxd.skipper.search.model.SampleValues;
import com.unbxd.skipper.search.model.TextFacetDetail;
import com.unbxd.skipper.search.service.FacetStatService;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.toucan.eventfactory.EventTag;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import ro.pippo.controller.*;
import ro.pippo.core.FileItem;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.Request;
import ro.pippo.core.route.RouteContext;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.unbxd.event.EventFactory.FACET;
import static com.unbxd.event.EventFactory.SEARCHABLE_FIELDS;
import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static com.unbxd.skipper.relevancy.model.PageRequest.getPageRequestFromRouteContext;
import static com.unbxd.skipper.relevancy.model.RelevancyRequest.RELEVANCY_STATUS_WEBHOOK;
import static com.unbxd.skipper.relevancy.model.SearchableField.getSearchableFieldsFromRouteContext;
import static com.unbxd.skipper.search.constants.Constants.STATS;
import static com.unbxd.toucan.eventfactory.EventTag.ERROR;
import static com.unbxd.toucan.eventfactory.EventTag.INFO;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.*;

@Log4j2
public class RelevancyController extends Controller {

    private ObjectMapper mapper;
    private RelevancyService relevancyService;
    private static final String END = "end";
    private static final String GAP = "gap";
    private static final String PAGE = "page";
    private static final String SORT = "sort";
    private static final String TYPE = "type";
    private static final String PATH = "path";
    private static final String TEXT = "text";
    private static final String QUERY = "query";
    private static final String FIELD = "field";
    private static final String RANGE = "range";
    private static final String START = "start";
    private static final String PER_PAGE = "per_page";
    private static final String FACETNAME = "facetName";
    private static final String UN_SSO_UID = "_un_sso_uid";
    private static final String WORKFLOW_ID = "workflowId";
    private static final String PRODUCT_TYPE = "product_type";
    private static final String SKIP_AUTOSUGGEST_PARAM = "skipAutosuggest";
    private static final String COUNT = "count";

    private static final HashMap<String,Integer> DEFAULT_COUNT_PARAM_VALUES = new HashMap<>(){{
        put(TEXT,5);
        put(PATH,5);
    }};

    private EventFactory eventFactory;
    private FacetStatService facetStatService;
    private ConsoleOrchestrationService consoleOrchestrationService;
    private IndexingService indexingService;
    private SiteService siteService;


    @Inject
    public RelevancyController(EventFactory eventFactory,
                               RelevancyService relevancyService,
                               FacetStatService facetStatService,
                               ConsoleOrchestrationService consoleOrchestrationService,
                               IndexingService indexingService,
                               SiteService siteService) {
        this.mapper = new ObjectMapper();
        this.eventFactory = eventFactory;
        this.relevancyService = relevancyService;
        this.facetStatService = facetStatService;
        this.consoleOrchestrationService = consoleOrchestrationService;
        this.indexingService = indexingService;
        this.siteService = siteService;
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/relevancy/job/{field}")
    public void getFields() {
        RouteContext routeContext = getRouteContext();
        String field = getRouteParam(FIELD, routeContext);
        JobType type = JobType.valueOf(field);
        String siteKey = getRouteParam(SITEKEY_PARAM, routeContext);

        if(isNotEmpty(siteKey)) {
            APIResponse<RelevancyOutputModel> relevancyOutput = relevancyService.getFields(siteKey, type);
            routeContext.json().send(relevancyOutput);
        }
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/facets/{"+FACETNAME+"}")
    public void fetchFacetDetail() {
        RouteContext routeContext = getRouteContext();
        String facetName = getRouteParam(FACETNAME, routeContext);
        String siteKey = getRouteParam(SITEKEY_PARAM, routeContext);
        String facetType = getOrDefaultParam(TYPE, null,routeContext);
        String count = getOrDefaultParam(COUNT, null, routeContext);
        Boolean stats = routeContext.getParameter(STATS).toBoolean(false);
        if(isNotEmpty(siteKey)) {
            try {
                if(RANGE.equals(facetType)) {
                    String start = getOrDefaultParam(START, "0",routeContext);
                    String end = getOrDefaultParam(END, "100",routeContext);
                    String gap = getOrDefaultParam(GAP, "5", routeContext);
                    RangeFacetDetail rangeFacetDetail = facetStatService
                            .fetchRangeFacetDetails(siteKey, facetName , parseInt(start) ,
                                    parseInt(end) , parseInt(gap));
                    routeContext.status(200).json().send(new APIResponse<>(rangeFacetDetail));
                } else if(PATH.equals(facetType)){
                    PathFacetDetail pathFacetDetail = facetStatService
                            .fetchPathFacetDetails(siteKey, facetName, parseCount(count,facetType) );
                    routeContext.status(200).json().send(new APIResponse<>(pathFacetDetail));
                } else if(TEXT.equals(facetType)){
                    TextFacetDetail textFacetDetail = facetStatService
                          .fetchTextFacetDetails(siteKey, facetName, parseCount(count,facetType));
                    routeContext.status(200).json().send(new APIResponse<>(textFacetDetail));
                } else //  facetType == default
                   {
                    SampleValues sampleValues = facetStatService.
                            fetchFieldValues(siteKey, facetName, parseCount(count,facetType), stats);
                    routeContext.status(200).json().send(new APIResponse<>(sampleValues));
                }
            } catch (FacetStatServiceException e) {
                routeContext.status(e.getCode());
                ErrorResponse errorResponse = new ErrorResponse( e.getMessage());
                routeContext.json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
            }
        }
    }

    private static Integer parseCount(String count,String facetType) {
        return isNull(count) ? DEFAULT_COUNT_PARAM_VALUES.getOrDefault(facetType,10) :
                parseInt(count);
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/relevancy/trigger")
    public void triggerWorkflow() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteParam(SITEKEY_PARAM, routeContext);
        Request request = routeContext.getRequest();
        RelevancyRequest jobRequest = null;
        if (!request.getBody().isEmpty()) {
            try {
                jobRequest = mapper.readValue(request.getBody(), RelevancyRequest.class);
            } catch (JsonProcessingException e) {
                String msg = "Error while parsing request for triggering the relevancy job";
                log.error(msg + " reason: " + e.getMessage());
                routeContext.status(400);
                ErrorResponse errorResponse = new ErrorResponse(msg);
                routeContext.json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
                return;
            }
        }
        if(isNotEmpty(siteKey)) {
            APIResponse<RelevancyResponse> relevancyResponse = null;
            try {
                relevancyResponse = relevancyService.triggerJob(siteKey, jobRequest);
            } catch (SiteNotFoundException siteNotFoundException) {
                siteNotFoundException.printStackTrace();
            }

            if(relevancyResponse != null) {
                routeContext.json().send(relevancyResponse);
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Error while triggering relevancy job.");
                routeContext.json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
            }
        }
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/workflow/{workflowId}/status")
    public void getWorkflowStatus() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteParam(SITEKEY_PARAM, routeContext);
        String workflowId = getRouteParam(WORKFLOW_ID, routeContext);

        if(isNotEmpty(siteKey) && isNotEmpty(workflowId)) {
            WorkflowStatus relevancyStatusResponse = null;
            try {
                relevancyStatusResponse = relevancyService.fetchRelevancyStatus(siteKey, workflowId);
            } catch (RelevancyServiceException e) {
                ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), e.getStatusCode());
                routeContext.json().status(e.getStatusCode()).
                        send(new APIResponse<>(Collections.singletonList(errorResponse)));
            }
            if(relevancyStatusResponse != null) {
                routeContext.json().send(new APIResponse<>(relevancyStatusResponse));
                return;
            }
        }


    }

    @POST(RELEVANCY_STATUS_WEBHOOK)
    public void postRelevancyStatus() {
        RouteContext routeContext = getRouteContext();
        Request request = routeContext.getRequest();
        log.info("Received status webhook call");
        String siteKey = null;
        try {
            StatusWebhookRequest statusWebhookRequest = mapper.readValue(request.getBody(), StatusWebhookRequest.class);
            siteKey = statusWebhookRequest.getSitekey();
            String workflowId = statusWebhookRequest.getWorkflowId();
            relevancyService.processRelevancyStatus(siteKey, workflowId, statusWebhookRequest.getWorkflow());
        } catch(JsonProcessingException e) {
            log.error("Error parsing status webhook request : ", e);
        } catch (RelevancyServiceException e) {
            log.error("Error while processing relevancy status webhook for siteKey:"+ siteKey +
                    ", error message: " + e.getMessage());
        } catch (SiteNotFoundException e) {
            log.error("Error while processing relevancy status webhook for siteKey:"+ siteKey +
                    ", no site exists");
        }

    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/searchableFields/filters")
    public void getSearchableFields(){
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteParam(SITEKEY_PARAM,routeContext);
        try {
            PageRequest request = getPageRequestFromRouteContext(routeContext);
            APIResponse<GetSearchableFieldsResponse> response =
                    new APIResponse<>(
                            relevancyService.getSearchableFields(siteKey,request)
                    );
            routeContext.status(HttpConstants.StatusCode.OK).json().send(response);
        } catch (JsonProcessingException e){
            log.error("Error while parsing request json for siteKey: "+siteKey+" , ", e);
            APIResponse response = new APIResponse(Collections.singletonList(
                    new ErrorResponse("Error while parsing request json due to: "+e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(response);
        } catch (RelevancyServiceException e) {
            APIResponse response = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(response);
        } catch (SiteNotFoundException e) {
            APIResponse response = new APIResponse(Collections.singletonList(
                    new ErrorResponse("No site exists with siteKey:" + siteKey)));
            routeContext.status(400).json().send(response);
        }
    }

    // this api is used to update searchWieghtage of given searchable fields
    @PATCH("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/searchableFields")
    public void updateSearchableFields(){
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteParam(SITEKEY_PARAM,routeContext);
        try {
            List<SearchableField> fieldsTobeUpdated = getSearchableFieldsFromRouteContext(routeContext);
            relevancyService.updateSearchableFieldsInFieldService(siteKey,fieldsTobeUpdated);
            routeContext.status(HttpConstants.StatusCode.OK).json().send(
                    getSuccessResponse("updated successfully")
            );
        } catch (JsonProcessingException e){
            log.error("Error while parsing request json for siteKey: "+siteKey+" , ", e);
            APIResponse response = new APIResponse(Collections.singletonList(
                    new ErrorResponse("Error while parsing request json due to: "+e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(response);
        } catch (RelevancyServiceException e){
            fireEvent("error while updating searchable fields: " + e.getMessage(), siteKey, ERROR, SEARCHABLE_FIELDS);
            APIResponse response = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(response);
        }
    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/catalog/indexing")
    public void catalogIndexing() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            String feedId = relevancyService.triggerCatalogIndexing(siteKey);
            routeContext.json().send(new APIResponse<>(Collections.singletonMap("feedId", feedId)));
        } catch (RelevancyServiceException e) {
            routeContext.json().status(e.getStatusCode()).send(
                    new APIResponse<>(Collections.singletonList(new ErrorResponse(e.getMessage()))));
        } catch (SiteNotFoundException e) {
            getRouteContext().json().status(400).send(new APIResponse<>("Site Not found"));
        }
    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/indexing")
    public void triggerIndexing() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        Boolean skipAutosuggest = routeContext.getParameter(SKIP_AUTOSUGGEST_PARAM).toBoolean(true);

        try {
            siteService.updateContext(siteKey,null,null, ServeStateType.INDEXING_STATE,null);
            indexingService.indexCatalog(siteKey);
            if(!skipAutosuggest) indexingService.indexAutosuggest(siteKey);
            APIResponse<IndexingStatus> response = fetchIndexingStatus(siteKey);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send(response);
        } catch (IndexingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage(),
                    e.errorCode)));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        } catch (SiteNotFoundException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(404).json().send(errResp);
        }
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/indexing/status")
    public void getIndexingStatus() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            APIResponse<IndexingStatus> response = fetchIndexingStatus(siteKey);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send(response);
        } catch (IndexingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage(),
                    e.errorCode)));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/relevancy/catalog/status")
    public void getFeedStatus() {
        RouteContext routeContext = getRouteContext();
        String countString = routeContext.getParameter("count").toString();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        String feedId = routeContext.getParameter("feedId").toString();
        String type = routeContext.getParameter("type").toString();
        countString = (isEmpty(countString)) ? "10": countString;
        type = (isEmpty(type)) ? "full" : type;
        Integer count = parseInt(countString);

        try {
            FeedIndexingStatus status = relevancyService.getStatus(siteKey, feedId, count, type);
            routeContext.json().send(new APIResponse<>(status));
        } catch (RelevancyServiceException e) {
            routeContext.json().status(e.getStatusCode()).send(
                    new APIResponse<>(Collections.singletonList(new ErrorResponse(e.getMessage()))));
        } catch (SiteNotFoundException e) {
            getRouteContext().json().status(400).send(new APIResponse<>("Site Not found"));
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/relevancy/{jobType}/reset")
    public void processJob() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        JobType jobType = JobType.valueOf(getRouteContext().getParameter("jobType").toString());
        try {
            Cookie cookie = routeContext.getRequest().getCookie(UN_SSO_UID);
            String body = routeContext.getRequest().getBody();
            ProductType type = null;
            if(body != null && !body.isEmpty()) {
                ProductTypeReq req = mapper.readValue(body, ProductTypeReq.class);
                if(req != null)
                    type = req.getProductType();
            }
            if(type == null) {
                routeContext.json().status(400).
                        send(new APIResponse<>(new ErrorResponse("no product_type passed")));
                return;
            }

            relevancyService.reset(UN_SSO_UID + "=" + cookie.getValue(),
                    siteKey, jobType, type);
            routeContext.json().send(new APIResponse<>());
        } catch (RelevancyServiceException e) {
            log.error(e.getMessage() + " site: " + siteKey);
            fireEvent("error while resetting job: "+ e.getMessage(), siteKey, ERROR, jobType.name());
            routeContext.json().status(e.getStatusCode()).send(new APIResponse<>(new ErrorResponse(e.getMessage())));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage() + " site: " + siteKey);
            routeContext.json().status(500).send(new APIResponse<>(new ErrorResponse(e.getMessage())));
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/upload")
    public void uploadFileToS3() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            FileItem fileItem = routeContext.getRequest().getFile("file");
            File uploadedFile = new File(fileItem.getSubmittedFileName());
            fileItem.write(uploadedFile);
            fileItem.delete();
            APIResponse<Map<String, String>> response =
                    new APIResponse<>(
                            relevancyService.uploadFileToS3(siteKey, uploadedFile)
                    );
            routeContext.json().send(response);
        } catch (IOException e) {
            log.error(e.getMessage() + " site: " + siteKey);
            routeContext.json().status(500).send(new APIResponse<>(new ErrorResponse(e.getMessage())));
        } catch (RelevancyServiceException e) {
            routeContext.json().status(e.getStatusCode()).send(
                    new APIResponse<>(new ErrorResponse(e.getMessage(), e.getErrorCode()))
            );
        }

    }


    private String getOrDefaultParam(String paramName, String defaultParam, RouteContext routeContext) {
        ParameterValue param = routeContext.getParameter(paramName);
        if(!param.isNull()) {
            return param.getValues()[0];
        } else {
            return defaultParam;
        }
    }

    private String getRouteParam(String paramName, RouteContext routeContext) {
        ParameterValue param = routeContext.getParameter(paramName);
        if(!param.isNull()) {
            return param.getValues()[0];
        }
        log.info("Param missing: " + paramName);
        ErrorResponse errorResponse = new ErrorResponse(paramName + " param missing!");
        routeContext.json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
        return null;
    }

    private APIResponse<Map<String,String>> getSuccessResponse(String successMessage){
        Map<String,String> msg = new HashMap<>();
        msg.put("message",successMessage);
        return new APIResponse<>(msg);
    }

    private void fireEvent(String msg,
                           String siteKey,
                           EventTag eventTag,
                           String operationName) {
        eventFactory.createAndFireEvent(eventFactory.getEmail(EMPTY), siteKey,
                System.currentTimeMillis() * 1000, siteKey, msg,
                eventTag, operationName, emptyMap(), null);
    }



    private APIResponse<IndexingStatus> fetchIndexingStatus(String siteKey) throws IndexingException {
        IndexingStatus status = indexingService.getStatus(siteKey);
        List<ErrorResponse> errors = new ArrayList<>(2);
        if(nonNull(status)) {
            if (nonNull(status.getCatalog()) && status.getCatalog().getCode() != 0) {
                errors.add(new ErrorResponse(status.getCatalog().getMessage(), status.getCatalog().getCode()));
            }
            if (nonNull(status.getAutosuggest()) && status.getAutosuggest().getCode() != 200) {
                errors.add(new ErrorResponse(status.getAutosuggest().getMessage(), status.getAutosuggest().getCode()));
            }
        }
        APIResponse<IndexingStatus> response = new APIResponse<>(status);
        if(!errors.isEmpty()) response.setErrors(errors);
        return response;
    }
}
