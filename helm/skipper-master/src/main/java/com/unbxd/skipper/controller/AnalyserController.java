package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.model.Concepts;
import com.unbxd.analyser.model.StopWords;
import com.unbxd.analyser.model.UpdateConceptsRequest;
import com.unbxd.analyser.model.UpdateStopWordsRequest;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.skipper.analyser.migration.AnalyserMigrationException;
import com.unbxd.skipper.analyser.migration.MigrationService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;


import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;

@Log4j2
public class AnalyserController extends Controller {
    private ObjectMapper mapper;
    private AnalyserService analyserService;
    private MigrationService migrationService;
    private String UN_SSO_UID = "_un_sso_uid";

    @Inject
    public AnalyserController(AnalyserService analyserService,
                              MigrationService migrationService) {
        this.analyserService = analyserService;
        this.migrationService = migrationService;
        this.mapper = new ObjectMapper();
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/analyser/concepts")
    public void getConcepts() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        boolean getOnlyDefaultConcepts = getRouteContext().getParameter("default").toBoolean();
        APIResponse response;
        try {
            if (getOnlyDefaultConcepts) {
                List<String> defaultConcepts = analyserService.getDefaultConcepts();
                response = new APIResponse<>(defaultConcepts);
            } else {
                Concepts concepts = analyserService.getConcepts(siteKey);
                response = new APIResponse<>(concepts);
            }
            routeContext.status(200).json().send(response);
        } catch (AnalyserException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/analyser/concepts")
    public void updateConcepts() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            UpdateConceptsRequest request = mapper.readValue(routeContext.getRequest().getBody()
                    , UpdateConceptsRequest.class);
            analyserService.updateConcepts(siteKey, request);
            routeContext.status(200).json().send(
                    getSuccessResponse("updated successfully")
            );
        } catch (JsonProcessingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (AnalyserException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/analyser/stopwords")
    public void getStopWords() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        boolean getOnlyDefaultStopWords = getRouteContext().getParameter("default").toBoolean();
        APIResponse response;
        try {
            if (getOnlyDefaultStopWords) {
                List<String> defaultStopWords = analyserService.getDefaultStopWords();
                response = new APIResponse<>(defaultStopWords);
            } else {
                StopWords stopWords = analyserService.getStopWords(siteKey);
                response = new APIResponse<>(stopWords);
            }
            routeContext.status(200).json().send(response);
        } catch (AnalyserException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/analyser/stopwords")
    public void updateStopWords() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            UpdateStopWordsRequest request = mapper.readValue(routeContext.getRequest().getBody()
                    , UpdateStopWordsRequest.class);
            analyserService.updateStopWords(siteKey, request);
            routeContext.status(200).json().send(
                    getSuccessResponse("updated successfully")
            );
        } catch (JsonProcessingException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (AnalyserException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errResp);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/analyser/core")
    public void createAnalyserCore() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            analyserService.createAnalyserCore(siteKey);
            routeContext.status(200).json().send(
                    getSuccessResponse("core created successfully")
            );
        } catch (AnalyserException e) {
            APIResponse errorResponse = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errorResponse);
        }
    }

    @POST("/admin/site/{" + SITEKEY_PARAM + "}/analyser/migrate")
    public void migrateToSelfServe() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        Cookie cookie = routeContext.getRequest().getCookie(UN_SSO_UID);
        ThreadContext.put(SITEKEY_PARAM, siteKey);
        try {
            Map<String, Object> response = migrationService.migrateToSelfServe(siteKey, UN_SSO_UID
                    + "=" + cookie.getValue());
            routeContext.status(200).json().send(response);
        } catch (AnalyserMigrationException e) {
            APIResponse errorResponse = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getStatusCode()).json().send(errorResponse);
        }
    }


    private APIResponse<Map<String, String>> getSuccessResponse(String successMessage) {
        Map<String, String> msg = new HashMap<>();
        msg.put("message", successMessage);
        return new APIResponse<>(msg);
    }
}
