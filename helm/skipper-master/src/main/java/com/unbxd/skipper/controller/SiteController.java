package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.SiteProductsResponse;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.SiteKeyCred;
import com.unbxd.field.service.FieldService;
import com.unbxd.pim.workflow.model.WorkflowContext;
import com.unbxd.pim.workflow.service.PIMOrchestrationService;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import com.unbxd.skipper.controller.model.request.RequestParam;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.dictionary.service.DictionaryService;
import com.unbxd.skipper.model.Request;
import com.unbxd.skipper.model.RequestContext;
import com.unbxd.skipper.model.SiteRequest;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.exception.ValidationException;
import com.unbxd.skipper.site.model.Site;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import retrofit2.Response;
import ro.pippo.controller.*;
import ro.pippo.controller.extractor.Param;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;

import java.io.IOException;
import java.util.*;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.APP_ID_KEY;
import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static com.unbxd.skipper.controller.model.response.APIResponse.getInstance;
import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static com.unbxd.skipper.model.Constants.WORKFLOW_STATE;
import static com.unbxd.skipper.states.ServeState.PIM;
import static com.unbxd.skipper.states.model.ServeStateType.valueOf;
import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static ro.pippo.controller.Produces.JSON;
import static ro.pippo.core.HttpConstants.ContentType.APPLICATION_JSON;

@Log4j2
public class SiteController extends BaseController {

    private static final String COOKIE = "Cookie";
    private static final String PRODUCTS = "products";
    private static final String FEATURES = "features";
    private static final String VERTICAL = "vertical";
    private static final String LANGUAGE = "language";

    private static final String VIRTUAL_STATE_PARAM = "virtualstate";

    private ObjectMapper mapper;
    private SiteService siteService;
    private FieldService fieldService;
    private PIMRemoteService pimRemoteService;
    private PIMOrchestrationService pimOrchestrationService;
    private ConsoleOrchestrationService consoleOrchestrationService;
    private DictionaryService dictionaryService;

    @Inject
    public SiteController(SiteService siteService,
                          FieldService fieldService,
                          PIMRemoteService pimRemoteService,
                          PIMOrchestrationService pimOrchestrationService,
                          ConsoleOrchestrationService consoleOrchestrationService,
                          DictionaryService dictionaryService) {
        this.siteService = siteService;
        this.mapper = new ObjectMapper();
        this.fieldService  = fieldService;
        this.pimRemoteService = pimRemoteService;
        this.pimOrchestrationService = pimOrchestrationService;
        this.consoleOrchestrationService = consoleOrchestrationService;
        this.dictionaryService = dictionaryService;
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/status")
    public void getStatus() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getOrDefaultParam(SITEKEY_PARAM, StringUtils.EMPTY ,routeContext);
        String cookie = UN_SSO_UID + "=" + routeContext.getRequest().getCookie(UN_SSO_UID).getValue();
        boolean fetchProducts = parseBoolean(getOrDefaultParam(PRODUCTS,"false", routeContext));
        boolean fetchFeatures  = parseBoolean(getOrDefaultParam(FEATURES,"false", routeContext));

        List<ErrorResponse> errorResponses = null;
        HashMap<String, Object> responseData = new HashMap<>();
        APIResponse<Map<String, Object>> response = new APIResponse<>(responseData);

        if(fetchProducts) {
            SiteProductsResponse siteProductsResponse = consoleOrchestrationService
                    .getSiteProductsResponse(cookie, siteKey);
            if (siteProductsResponse.isSuccessful()) {
                routeContext.status(200);
                responseData.put("products", siteProductsResponse.getProducts());
            } else {
                errorResponses = new ArrayList<>();
                response.setErrors(errorResponses);
                errorResponses.add(ErrorResponse.getInstance(siteProductsResponse.getErrors()));
                response.setCode(500);
            }
        }

        try {
            if(fetchFeatures) {
                Map<String, Object> features = consoleOrchestrationService.getFeatures(cookie, siteKey);
                responseData.put("features", features);
            }
            StateContext siteStatus = siteService.getSiteStatus(siteKey);
            response.setCode(siteStatus.getCode());
            responseData.put("status", siteStatus);
        } catch (SiteNotFoundException siteNotFoundException) {
            if(CollectionUtils.isEmpty(errorResponses)) {
                errorResponses = new ArrayList<>();
                response.setErrors(errorResponses);
            }
            errorResponses.add(ErrorResponse.getInstance("Site Not found"));
            response.setCode(404);
        } catch (ConsoleOrchestrationServiceException e) {
            response.setCode(e.getStatusCode());
            errorResponses.add(ErrorResponse.getInstance(e.getMessage()));
            response.setErrors(errorResponses);
        }
        routeContext.status(response.getCode());
        routeContext.json().send(response);
    }

    @Produces(JSON)
    @DELETE("/skipper/site/{"+SITEKEY_PARAM+"}")
    public com.unbxd.cbo.response.Response
            <Map<String, String>> deleteSite(@Param String apiKey,
                                             @Param String siteKey,
                                             @Param String secretKey) {
        List<Error> errors = new ArrayList<>();
        Map<String, String> responseMap = new HashMap<>();
        com.unbxd.cbo.response.Response.Builder<Map<String, String>>
                responseBuilder = new com.unbxd.cbo.response.Response.Builder<>();

        RouteContext routeContext = getRouteContext();
        String cookie = UN_SSO_UID + "=" + routeContext.getRequest().getCookie(UN_SSO_UID).getValue();
        deleteFromConsole(siteKey, errors, responseMap, cookie);

        deleteFromHagrid(siteKey, errors, responseMap);
        deleteFromSkipper(siteKey, errors, responseMap);

        if (MapUtils.isNotEmpty(responseMap)) { responseBuilder.withData(responseMap); }
        if (CollectionUtils.isNotEmpty(errors)) { responseBuilder.withErrors(errors); }
        return responseBuilder.build();
    }

    private void deleteFromHagrid(String siteKey,
                                  List<Error> errors,
                                  Map<String, String> responseMap) {
        try {
            fieldService.deleteSite(siteKey);
            responseMap.put("search-platform", "site deleted successfully.");
        } catch (FieldException e) {
            errors.add(new Error.Builder().withCode(500).withMessage("Error while" +
                    " deleting site from search platform: " + e.getMessage()).build());
        }
    }

    private void deleteFromSkipper(String siteKey,
                                   List<Error> errors,
                                   Map<String, String> responseMap) {
        try {
            StateContext stateContext = siteService.getSiteStatus(siteKey);
            deleteFromPIM(stateContext, errors, responseMap);
            siteService.deleteSite(siteKey);
            dictionaryService.delete(siteKey);

            responseMap.put("skipper", "site deleted successfully.");
        } catch (SiteNotFoundException e) {
            errors.add(new Error.Builder().withCode(500).withMessage("Error while" +
                    " deleting site from skipper: " + e.getMessage()).build());
        }
    }

    private void deleteFromConsole(String siteKey,
                                   List<Error> errors,
                                   Map<String, String> responseMap,
                                   String cookie){
        try {
            consoleOrchestrationService.deleteSite(siteKey, cookie);
            responseMap.put("console", "site deleted successfully.");
        } catch (FieldException e) {
            errors.add(new Error.Builder().withCode(500).withMessage("Error while" +
                    " deleting site from console: " + e.getMessage()).build());
        }

    }

    private void deleteFromPIM(StateContext state,
                               List<Error> errors,
                               Map<String, String> responseMap) {
        if (equalsIgnoreCase(state.getFeedPlatform(), PIM)) {
            try {
                String cookie = UN_SSO_UID + "=" + getRequest().getCookie(UN_SSO_UID).getValue();
                Response<JsonObject> response = pimRemoteService.deleteOrg(cookie,
                        APPLICATION_JSON, state.getOrgId()).execute();

                if (response.isSuccessful()) {
                    responseMap.put(PIM, "org deleted successfully.");
                } else {
                    errors.add(new Error.Builder().withCode(500).withMessage("Error while" +
                            " deleting org from PIM: " + response.errorBody().string()).build());
                }
            } catch (IOException e) {
                errors.add(new Error.Builder().withCode(500).withMessage("Error while" +
                        " deleting org from PIM: " + e.getMessage()).build());
            }
        }
    }

    private void sendErrorResponse(String errorMsg) {
        ErrorResponse errorResponse = ErrorResponse.getInstance(errorMsg);
        RouteDispatcher.getRouteContext().status(500).json()
                .send(getInstance(errorResponse, 500));
        log.error(errorMsg);
    }

    @GET("/skipper/statusById/{id}")
    public void getStatusById() {
        RequestContext requestContext = initializeRequestContext(getRouteContext());
        try {
            StateContext siteStatus = siteService.getSiteById(requestContext.getMongoId());

            APIResponse<StateContext> statusResponse = new APIResponse<>(siteStatus);
            if(isNotEmpty(siteStatus.getErrors())) {
                statusResponse.setErrors(singletonList(ErrorResponse.getInstance(siteStatus.getErrors())));
            }
            statusResponse.setCode(siteStatus.getCode());
            requestContext.getRouteContext().json().send(statusResponse);
        } catch (SiteNotFoundException siteNotFoundException) {
            requestContext.getRouteContext().json().status(400)
                    .send(new APIResponse<>(new ErrorResponse("Site Not found")));
        }
    }

    @Produces(JSON)
    @GET("/skipper/site/data")
    public APIResponse<StateContext> getStatus(@Param String fieldName,
                                               @Param String fieldValue) {
        try {
            StateContext siteStatus = siteService.getSiteStatus(fieldName, fieldValue);
            return new APIResponse<>(siteStatus);
        } catch (SiteNotFoundException e) {
            return getInstance((ErrorResponse.getInstance("Site Not Found")), 400);
        }
    }

    @Produces(JSON)
    @POST("/skipper/site/{siteKey}/template/{templateId}")
    public APIResponse<StateContext> setTemplate(@Param String siteKey,
                                                 @Param String templateId) {
        try {
            return new APIResponse<>(siteService.setTemplateId(siteKey, templateId));

        } catch (SiteNotFoundException e) {
            return getInstance((ErrorResponse.getInstance("Site Not Found")), 400);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/onboard/state/{virtualstate}")
    public void postVirtualState() {
        RouteContext routeContext = getRouteContext();
        ro.pippo.core.Request request = routeContext.getRequest();

        try {
            String cookie = request.getCookie(UN_SSO_UID).getValue();
            String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
            String virtualState = routeContext.getParameter(VIRTUAL_STATE_PARAM).toString();
            StateContext stateContext = siteService.updateContext(siteKey, cookie, null,
                    valueOf(virtualState), mapper.readValue(request.getBody(), Map.class));

            APIResponse<StateContext> response;
            String errors = stateContext.getErrors();
            if(isNotEmpty(errors)) {
                response = new APIResponse<>(singletonList(ErrorResponse.getInstance(errors)));
            } else {
                response = new APIResponse<>(stateContext);
            }
            routeContext.json().send(response);
        } catch(Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            log.error("Exception during virtual state transition: " + e.getMessage());
            APIResponse<Site> siteResponse = new APIResponse<>(singletonList(errorResponse));
            routeContext.status(500).json().send(siteResponse);
        }
    }

    @POST("/skipper/site")
    public void createSite() {
        long startTimeInMillis = System.currentTimeMillis();
        RequestContext requestContext = initializeRequestContext(getRouteContext());
        ro.pippo.core.Request request = getRouteContext().getRequest();
        StateContext stateContext = requestContext.getStateContext();
        stateContext.setTimestamp(startTimeInMillis);
        APIResponse  siteResponse;
        try {
            SiteRequest siteRequest = mapper.readValue(request.getBody(), SiteRequest.class);
            siteService.validateSiteRequest(siteRequest);
            String email = getRouteContext().getLocal(RequestParam.LOCAL_PARAM_NAME_EMAIL);
            if(siteRequest.getEmail() != null) {
                email = siteRequest.getEmail();
            }
            setStateContextFields(stateContext, siteRequest, email);
            stateContext.getCookie().put(UN_SSO_UID, request.getCookie(UN_SSO_UID).getValue());
        } catch(IOException e) {
            log.error("Unable to parse json request: ", e);
        }
        catch (ValidationException e){
            log.error("Unable to createSite due to :"+e.getMessage());
            ErrorResponse error = new ErrorResponse(e.getMessage());
            siteResponse = getInstance(error, 400);
            requestContext.getRouteContext().status(400).json().send(siteResponse);
            return;
        }
        siteService.start(requestContext);

        Site data = new Site();
        data.setDbDocId(stateContext.getId());
        siteResponse = new APIResponse<Site>(data);

        try {
            if(stateContext.getErrors() != null && !stateContext.getAppId().isEmpty()) {
                ErrorResponse error = new ErrorResponse(stateContext.getErrors());
                siteResponse.setErrors(singletonList(error));
            }
        } catch(Exception e) {
            log.error("Error in setting response: ", e);
        }

        long endTimeInMillis = System.currentTimeMillis();
        requestContext.getRouteContext().json().send(siteResponse);
        log.info("Time taken to create site: " + (endTimeInMillis - startTimeInMillis));
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/cred")
    public void getSiteCred() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            SiteKeyCred siteCred = fieldService.getSiteDetails(siteKey);
            APIResponse resp = new APIResponse<>(siteCred);
            routeContext.status(200).json().send(resp);
        } catch (FieldException e) {
            APIResponse errResp = new APIResponse(singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(500).json().send(errResp);
        }
    }

    @POST("/admin/site/{" + SITEKEY_PARAM + "}/ftu/reset")
    public void resetForAdmin() {
        reset();
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/ftu/reset")
    public void reset() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            ServeState state = mapper.readValue(routeContext.getRequest().getBody(),ServeState.class);
            siteService.reset(siteKey, state);
            routeContext.status(200).json().send(new APIResponse<>());
        } catch (JsonProcessingException e) {
            APIResponse errResp = new APIResponse(singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(500).json().send(errResp);
        }
    }



    private void setStateContextFields(StateContext stateContext, SiteRequest siteRequest, String email) {
        stateContext.setAppId("UNBXD_PIM_SEARCH_APP");
        stateContext.setSiteName(siteRequest.getName());
        stateContext.setRegion(siteRequest.getRegions());
        stateContext.setVertical(siteRequest.getVertical());
        stateContext.setPlatform(siteRequest.getPlatform());
        stateContext.setLanguage(siteRequest.getLanguage());
        stateContext.setEnvironment(siteRequest.getEnvironment());
        stateContext.setFeedPlatform(siteRequest.getFeedPlatform());
        stateContext.setAppToken(siteRequest.getAppToken());
        stateContext.setShopName(siteRequest.getShopName());
        stateContext.setEmail(email);
    }

    @POST("skipper/site/{" + SITEKEY_PARAM + "}/orchestrate/trigger")
    public void trigger() {
        RequestContext requestContext = initializeRequestContext(getRouteContext());

        Request request = requestContext.getRequest();
        String siteKey = requestContext.getRouteContext().getParameter(SITEKEY_PARAM).toString();
        WorkflowContext workflowContext = null;
        try {
            workflowContext = mapper.
                    readValue(requestContext.getRouteContext().getRequest().getBody(), WorkflowContext.class);
        } catch (JsonProcessingException e) {
            requestContext.getRouteContext().status(400).json().
                    send(new APIResponse<String>(
                            singletonList(
                                    new ErrorResponse("Expected request body"))));
        }
        workflowContext.setSiteKey(siteKey);
        workflowContext.setAuthToken(requestContext.getAuthToken());
        workflowContext.setCookie(UN_SSO_UID + "=" + requestContext.getAuthToken());
        String workflowState = request.getParams().get(WORKFLOW_STATE);

        if(workflowState == null|| workflowContext.getAppId() == null) {
            requestContext.getRouteContext().status(400).json().
                    send(new APIResponse<String>(
                            singletonList(
                                    new ErrorResponse(APP_ID_KEY + " and workflowState is mandatory"))));
            return;
        }

        try {
            workflowContext = pimOrchestrationService.triggerWorkflow(workflowContext, workflowState);
            requestContext.getRouteContext().json().send(new APIResponse<WorkflowContext>(workflowContext));
        } catch (Exception e) {
            log.error("Error executing workflow in PIM : ", e);
            requestContext.getRouteContext().status(500).json().send(new APIResponse<String>(
                    singletonList(new ErrorResponse(e.getMessage()))));
            return;
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

    private Map<String, String> getParamMap(String requestBody) {
        if(Strings.isNotEmpty(requestBody)) {
            JsonObject jsonParams = JsonParser.parseString(requestBody).getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entriesSet = jsonParams.entrySet();
            Map<String, String> params = new HashMap<>();

            for(Map.Entry<String, JsonElement> entry: entriesSet) {
                params.put(entry.getKey(), entry.getValue().getAsString());
            }
            return params;
        }
        return Collections.EMPTY_MAP;
    }

    @Produces(JSON)
    @PATCH("/skipper/site/{" + SITEKEY_PARAM + "}/config/{property}")
    public void updateVerticalOrLang(@Param String siteKey,
                                     @Param String property,
                                     @Param String value) {
        RouteContext routeContext = getRouteContext();
        try {
            siteService.setSiteStateProperty(siteKey, property, value);
            routeContext.status(200).json().send(new APIResponse<>(
                    "site " + property + " updated successfully"));
        } catch (Exception e) {
            log.error("Error while updating site state property "
                    + property + " of siteKey - " + siteKey + ", Reason: " + e.getMessage());
            routeContext.status(400).json().send(new APIResponse<String>(
                    singletonList(new ErrorResponse(e.getMessage()))));
        }
    }
}
