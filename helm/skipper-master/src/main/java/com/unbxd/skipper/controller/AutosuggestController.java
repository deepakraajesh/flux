package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.feed.exception.IndexingException;
import com.unbxd.skipper.autosuggest.exception.AutosuggestStateException;
import com.unbxd.skipper.autosuggest.exception.SuggestionServiceException;
import com.unbxd.skipper.autosuggest.exception.ValidationException;
import com.unbxd.skipper.feed.model.IndexingStatus;
import com.unbxd.skipper.autosuggest.model.Suggestions;
import com.unbxd.skipper.autosuggest.model.Template;
import com.unbxd.skipper.feed.service.IndexingService;
import com.unbxd.skipper.autosuggest.service.AutosuggestStateService;
import com.unbxd.skipper.autosuggest.service.SuggestionService;
import com.unbxd.skipper.autosuggest.service.TemplateService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.states.ServeState;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.DELETE;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import java.util.*;

import static com.unbxd.skipper.autosuggest.model.Suggestions.getSuggestionsFromRouteContext;
import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static java.util.Objects.nonNull;

@Log4j2
public class AutosuggestController extends Controller {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private TemplateService templateService;
    private SuggestionService suggestionService;
    private AutosuggestStateService autosuggestStateService;
    private IndexingService indexingService;

    @Inject
    public AutosuggestController(TemplateService templateService,
                                 SuggestionService suggestionService,
                                 AutosuggestStateService autosuggestStateService,
                                 IndexingService indexingService){
        this.templateService = templateService;
        this.suggestionService = suggestionService;
        this.autosuggestStateService = autosuggestStateService;
        this.indexingService = indexingService;
    }

    @POST("admin/autosuggest/template")
    public void addTemplate(){
        RouteContext routeContext = getRouteContext();
        try {
            Template template = MAPPER.readValue(routeContext.getRequest().getBody(), Template.class);
            templateService.addTemplate(template);
            routeContext.status(HttpConstants.StatusCode.OK)
                    .json().send(getSuccessResponse("templates updated successfully"));
        } catch (JsonProcessingException  e) {
            String message = "Unable to parse request due to "+ e.getMessage();
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(message)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (ValidationException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        }
    }

    @GET("/skipper/autosuggest/templates")
    public void getTemplates(){
        RouteContext routeContext = getRouteContext();
        routeContext.status(HttpConstants.StatusCode.OK)
                .json().send(new APIResponse<>(templateService.getTemplates()));
    }

    @GET("/skipper/autosuggest/templates/{templateId}")
    public void getTemplate(){
        RouteContext routeContext = getRouteContext();
        String templateId = getRouteContext().getParameter(Template.TEMPLATE_ID).toString();
        routeContext.status(HttpConstants.StatusCode.OK)
                .json().send(new APIResponse<>(templateService.getTemplate(templateId)));
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/config")
    public void getSuggestions() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            routeContext.status(HttpConstants.StatusCode.OK)
                    .json().send(new APIResponse<>(suggestionService.getSuggestions(siteKey)));
        } catch (SuggestionServiceException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/config")
    public void addSuggestions() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            Suggestions suggestions = getSuggestionsFromRouteContext(routeContext);
            suggestionService.addSuggestions(siteKey,suggestions, false);
            Suggestions response = suggestionService.getSuggestions(siteKey);
            routeContext.status(HttpConstants.StatusCode.OK).json().send(new APIResponse<>(response));
        } catch (JsonProcessingException e) {
            String message = "Unable to parse request due to "+ e.getMessage();
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(message)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (SuggestionServiceException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage(),
                    e.getErrorCode())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @DELETE("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/config")
    public void deleteSuggestions() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            Suggestions suggestions = getSuggestionsFromRouteContext(routeContext);
            suggestionService.deleteSuggestions(siteKey,suggestions);
            Suggestions response = suggestionService.getSuggestions(siteKey);
            routeContext.status(HttpConstants.StatusCode.OK).json().send(new APIResponse<>(response));
        } catch (JsonProcessingException e) {
            String message = "Unable to parse request due to "+ e.getMessage();
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(message)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (SuggestionServiceException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/state")
    public void setAutosuggestConfigState(){
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            ServeState serveState = MAPPER.readValue(routeContext.getRequest().getBody(),ServeState.class);
            routeContext.status(HttpConstants.StatusCode.OK)
                    .json().send(
                            new APIResponse<>(
                                    autosuggestStateService.setAutosuggestState(siteKey,serveState)
                            )
            );
        } catch (JsonProcessingException e) {
            String message = "Unable to parse request due to "+ e.getMessage();
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(message)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (AutosuggestStateException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getCode()).json().send(errResp);
        }
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/state")
    public void getAutosuggestConfigState(){
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            routeContext.status(HttpConstants.StatusCode.OK)
                    .json().send(new APIResponse<>(autosuggestStateService.getAutosuggestState(siteKey)));
        } catch (AutosuggestStateException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/index/trigger")
    public void indexSuggestions(){
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            indexingService.indexAutosuggest(siteKey);
            routeContext.status(HttpConstants.StatusCode.OK)
                    .json().send(getSuccessResponse("triggered indexing"));
        } catch (IndexingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @GET("/skipper/site/{"+SITEKEY_PARAM+"}/autosuggest/index/status")
    public void getIndexingStatus() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            IndexingStatus status = indexingService.getStatus(siteKey);
            List<ErrorResponse> errors = new ArrayList<>(2);
            //TODO: should errors of indexing be projected in error block or data block ?
            if(nonNull(status.getCatalog()) && status.getCatalog().getCode() != 200) {
                errors.add(ErrorResponse.getInstance(status.getCatalog().getCode()));
            }
            if(nonNull(status.getAutosuggest()) && status.getAutosuggest().getCode() != 200) {
                errors.add(ErrorResponse.getInstance(status.getAutosuggest().getCode()));
            }
            APIResponse<IndexingStatus> response = new APIResponse<>(status);
            if(!errors.isEmpty()) response.setErrors(errors);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send(response);
        } catch (IndexingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    private APIResponse<Map<String,String>> getSuccessResponse(String successMessage){
        Map<String,String> msg = new HashMap<>();
        msg.put("message",successMessage);
        return new APIResponse<>(msg);
    }
}
