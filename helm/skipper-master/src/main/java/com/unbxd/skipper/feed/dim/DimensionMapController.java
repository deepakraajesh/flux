package com.unbxd.skipper.feed.dim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.feed.dim.model.DimensionMap;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.controller.extractor.Param;

import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;

public class DimensionMapController extends Controller {

    DimensionMappingService dimService;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public DimensionMapController(DimensionMappingService dimService) {
        this.dimService = dimService;
    }


    /*
     * TODO: Add /site/{siteKey}, Changed it for testing
     */
    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/dimensions")
    public void getDimension(@Param String siteKey, @Param String vertical) {
        if(vertical == null) {
            String msg = "No vertical passed";
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.RequestIsNone.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }
        DimensionMap dimMap = null;
        try {
            dimMap = dimService.get(siteKey, vertical);
        } catch (DimException e) {
            APIResponse resp = new APIResponse(new ErrorResponse(e.getMessage(), e.getCode()));
            getRouteContext().getResponse().status(500).json().send(resp);
            return;
        }
        if(dimMap == null) {
            String msg = "No dimension map exists";
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.NoEntryFound.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }
        getRouteContext().getResponse().json().send(new APIResponse<>(dimMap));
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/dimensions")
    public void setDimension(@Param String siteKey, @Param String vertical) {
        if(vertical == null) {
            String msg = "No vertical passed";
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.RequestIsNone.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }

        DimensionMap dimMap = null;
        try {
            String body = getRouteContext().getRequest().getBody();
            if(body != null) {
                dimMap = mapper.readValue(body, DimensionMap.class);
            }
        } catch (JsonProcessingException e) {
            String msg = "Illegal argument with body: " + e.getMessage();
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.RequestIsNone.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }
        if(dimMap == null) {
            String msg = "No data passed";
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.RequestIsNone.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }

        DimensionMap dimMapAfterSave = null;
        try {
            // set the dimensions
            dimService.save(siteKey, dimMap);
            dimMapAfterSave = dimService.get(siteKey, vertical);
        } catch (DimException e) {
            APIResponse resp = new APIResponse(new ErrorResponse(e.getMessage(), e.getCode()));
            getRouteContext().getResponse().status(500).json().send(resp);
            return;
        }
        if(dimMapAfterSave == null) {
            String msg = "No dimension map exists";
            APIResponse resp = new APIResponse(new ErrorResponse(msg, ErrorCode.NoEntryFound.getCode()));
            getRouteContext().getResponse().status(400).json().send(resp);
            return;
        }
        getRouteContext().getResponse().json().send(new APIResponse<>(dimMapAfterSave));
    }
}

