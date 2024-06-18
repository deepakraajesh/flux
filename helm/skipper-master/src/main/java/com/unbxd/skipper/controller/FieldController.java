package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.AttributesResponse;
import com.unbxd.field.model.FieldMapping;
import com.unbxd.field.model.FieldServiceBaseResponse;
import com.unbxd.field.service.FieldService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.relevancy.model.FieldAliasMappingWrapper;
import com.unbxd.skipper.relevancy.model.PageRequest;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.controller.PUT;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import java.util.Collections;

import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static com.unbxd.skipper.relevancy.model.PageRequest.getPageRequestFromRouteContext;

@Log4j2
public class FieldController extends BaseController {
    private FieldService fieldService;

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/field/dimensionMap")
    public void getDimensionMap() {
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            FieldMapping mapping = fieldService.getFieldMapping(siteKey);
            APIResponse resp = new APIResponse<FieldMapping>(mapping);
            getRouteContext().status(200).json().send(resp);
        } catch (FieldException e) {
            APIResponse errResp = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            getRouteContext().status(500).json().send(errResp);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/field/attributes/filter")
    public void getAttributes() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            PageRequest request = getPageRequestFromRouteContext(routeContext);
            AttributesResponse attributesResponse = fieldService.getAttributes(siteKey,request);
            APIResponse<AttributesResponse> response = new APIResponse<>(attributesResponse);
            routeContext.status(HttpConstants.StatusCode.OK).json().send(response);
        } catch (JsonProcessingException e){
            log.error("Error while parsing request json: " , e);
            APIResponse response = new APIResponse(Collections.singletonList(
                    new ErrorResponse("Error while parsing request json due to: "+e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(response);
        } catch (FieldException e) {
            APIResponse response = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getCode()).json().send(response);
        }
    }

    @PUT("/skipper/site/{" + SITEKEY_PARAM + "}/field/attributes")
    public void updateMapping() {
        RouteContext routeContext = getRouteContext();
        String siteKey = getRouteContext().getParameter(SITEKEY_PARAM).toString();
        try {
            FieldAliasMappingWrapper request = mapper.
                    readValue(routeContext.getRequest().getBody(), FieldAliasMappingWrapper.class);

            FieldServiceBaseResponse attributesResponse = fieldService.updateMapping(siteKey, request.getData());
            APIResponse<FieldServiceBaseResponse> response = new APIResponse<>(attributesResponse);
            routeContext.status(HttpConstants.StatusCode.OK).json().send(response);
        } catch (JsonProcessingException e){
            log.error("Error while parsing request json: " , e);
            APIResponse response = new APIResponse(Collections.singletonList(
                    new ErrorResponse("Error while parsing request json due to: "+e.getMessage())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(response);
        } catch (FieldException e) {
            APIResponse response = new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage())));
            routeContext.status(e.getCode()).json().send(response);
        }
    }
}

