package com.unbxd.skipper.controller;

import com.google.inject.Inject;
import com.unbxd.pim.workflow.service.PIMService;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.feed.service.FeedService;
import com.unbxd.skipper.feed.exception.FeedException;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.core.route.RouteContext;

import java.util.Collections;

import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class FeedController extends Controller {

    FeedService feedService;
    PIMService pimService;

    @Inject
    public FeedController(FeedService feedService, PIMService pimService) {
        this.feedService = feedService;
        this.pimService = pimService;
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/feed/status")
    public void getSites() {
        RouteContext routeContext = getRouteContext();
        String countString = routeContext.getParameter("count").toString();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        String feedId = routeContext.getParameter("feedId").toString();
        String type = routeContext.getParameter("type").toString();
        countString = (isEmpty(countString)) ? "10": countString;
        type = (isEmpty(type)) ? "full" : type;
        Integer count = parseInt(countString);

        try {

            FeedIndexingStatus feedStatus = feedService.status(siteKey, feedId, count, type);
            routeContext.json().send(new APIResponse<>(feedStatus));
        } catch (FeedException e) {
            routeContext.json().status(500).send(
                    new APIResponse<>(Collections.singletonList(new ErrorResponse(e.getMessage()))));
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/feed/indexing")
    public void triggerIndexing() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            String feedId = feedService.reIndexSearchFeed(siteKey);
            routeContext.json().send(new APIResponse<>(Collections.singletonMap("feedId", feedId)));
        } catch (FeedException e) {
            routeContext.json().status(e.getStatusCode()).send(
                    new APIResponse<>(Collections.singletonList(new ErrorResponse(e.getMessage(),e.getErrorCode()))));
        }
    }
}

