package com.unbxd.skipper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.unbxd.auth.Auth;
import com.unbxd.auth.exception.AuthSystemException;
import com.unbxd.auth.exception.UnAuthorizedException;
import com.unbxd.skipper.site.model.*;
import com.unbxd.skipper.site.service.SiteService;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.controller.extractor.Param;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unbxd.skipper.site.model.DataCenterData.getDataCenterDataFromRouteContext;
import static com.unbxd.skipper.site.model.Environment.getEnvironmentsFromRouteContext;
import static com.unbxd.skipper.site.model.Language.getLanguagesFromRouteContext;
import static com.unbxd.skipper.site.model.Platform.getPlatformsFromRouteContext;
import static com.unbxd.skipper.site.model.Vertical.getVerticalsFromRouteContext;


public class SiteMetaController extends Controller {
    SiteService siteService;
    private static final String AUTH_COOKIE_HEADER = "_un_sso_uid";
    Auth auth;

    @Inject
    public SiteMetaController(SiteService siteService, Auth auth) {
        this.siteService = siteService;
        this.auth = auth;
    }

    @GET("/skipper/datacenter")
    public void getDataCenterData(@Param("provider") boolean provider) {
        RouteContext routeContext = getRouteContext();
        try {
            Cookie authToken = getRouteContext().getRequest().getCookie(AUTH_COOKIE_HEADER);
            Map<String, String> providers = new HashMap<>();
            if (provider) {
                Map<String, Object> userData = auth.verify(authToken.getValue());
                if (userData.containsKey("providers")) {
                    providers = (Map<String, String>) userData.get("providers");
                }
            }
            DataCenterData dataCenterData = siteService.getDataCenterData(providers);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send(dataCenterData.asJSONString());
        } catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.INTERNAL_ERROR);
            routeContext.json().send("Unable to serialize JSON due to" + e.getMessage());
        } catch (UnAuthorizedException e) {
            getRouteContext().status(HttpConstants.StatusCode.UNAUTHORIZED);
            getRouteContext().send("UnAuthorized call");
        } catch (AuthSystemException e) {
            getRouteContext().status(HttpConstants.StatusCode.INTERNAL_ERROR);
            getRouteContext().send("Internal error in Auth system");
        }
    }

    @POST("/admin/site/datacenter")
    public void setDataCenterData() {
        RouteContext routeContext = getRouteContext();
        try {
            DataCenterData dataCenterData = getDataCenterDataFromRouteContext(routeContext);
            siteService.setDataCenterData(dataCenterData);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send("DataCenter data is updated successfully");
        }
        catch (JsonProcessingException e) {
           routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
           routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }
    }

    @GET("/skipper/meta")
    public void getSiteMetaData() {
        RouteContext routeContext = getRouteContext();
        try {
            SiteMeta siteMeta = siteService.getSiteMeta();
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send(siteMeta.asJSONString());
        }
        catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.INTERNAL_ERROR);
            routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }
    }


    @POST("/admin/site/meta/verticals")
    public void setVerticals()
    {
        RouteContext routeContext = getRouteContext();
        try {
            List<Vertical> verticals = getVerticalsFromRouteContext(routeContext);
            System.out.println(verticals);
            siteService.setVerticals(verticals);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send("Verticals data is updated successfully");
        }
        catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.NOT_FOUND);
            routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }

    }

    @POST("/admin/site/meta/platforms")
    public void setPlatforms() {
        RouteContext routeContext = getRouteContext();
        try {
            List<Platform> platforms = getPlatformsFromRouteContext(routeContext);
            siteService.setPlatforms(platforms);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send("Platforms data is updated successfully");
        }
        catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
            routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }
    }

    @POST("/admin/site/meta/environments")
    public void setEnvironments() {
        RouteContext routeContext = getRouteContext();
        try {
            List<Environment> platforms = getEnvironmentsFromRouteContext(routeContext);
            siteService.setEnvironments(platforms);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send("Environments data is updated successfully");
        }
        catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
            routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }
    }

    @POST("/admin/site/meta/languages")
    public void setLanguages() {
        RouteContext routeContext = getRouteContext();
        try {
            List<Language> languages = getLanguagesFromRouteContext(routeContext);
            siteService.setLanguages(languages);
            routeContext.status(HttpConstants.StatusCode.OK);
            routeContext.json().send("Languages data is updated successfully");
        }
        catch (JsonProcessingException e) {
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
            routeContext.json().send("Unable to serialize JSON due to " + e.getMessage());
        }
    }
}
