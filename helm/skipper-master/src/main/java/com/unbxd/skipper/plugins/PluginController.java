package com.unbxd.skipper.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.plugins.exception.PluginException;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.POST;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import java.util.Collections;
import java.util.Map;

import static com.unbxd.skipper.ErrorCode.SiteNotFound;
import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;

@Log4j2
public class PluginController extends Controller {

    private Plugin plugin;
    private SiteService siteService;
    private ObjectMapper mapper;

    @Inject
    public PluginController(Plugin plugin, SiteService siteService) {
        mapper = new ObjectMapper();
        this.plugin = plugin;
        this.siteService = siteService;
    }

    @GET("/skipper/site/{" + SITEKEY_PARAM + "}/{plugin}/redirect")
    public void redirect() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        String shopName = routeContext.getParameter("shop").toString();
        String pluginName = routeContext.getParameter("plugin").toString();
        try {
            StateContext site = siteService.getSiteStatus(siteKey);
            String redirectURL = plugin.redirectURL(siteKey, site.getRegion(), site.getId(), shopName, pluginName);
            routeContext.status(HttpConstants.StatusCode.OK).json()
                    .send(new APIResponse<>(Collections.singletonMap("redirect", redirectURL)));
        } catch (SiteNotFoundException e) {
            String msg = "Site Not found";
            log.error(msg + " site: " + siteKey);
            APIResponse errResp =
                    new APIResponse(Collections.singletonList(new ErrorResponse(msg, SiteNotFound.getCode())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (PluginException e) {
            String msg = "Error while interacting with plugin: ";
            log.error(msg + " site: " + siteKey + " reason:" + e.getMessage());
            APIResponse errResp =
                    new APIResponse(Collections.singletonList(new ErrorResponse(msg, e.code)));
            if(String.valueOf(e.code).startsWith("400")) {
                errResp =
                        new APIResponse(Collections.singletonList(new ErrorResponse(e.getMessage(), e.code)));
                routeContext.status(HttpConstants.StatusCode.BAD_REQUEST);
            } else {
                routeContext.status(HttpConstants.StatusCode.INTERNAL_ERROR);
            }
            routeContext.json().send(errResp);
        }
    }

    @POST("/skipper/site/{" + SITEKEY_PARAM + "}/{plugin}/install")
    public void install() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();

        try {
            Map<String, String> req = mapper.readValue(routeContext.getRequest().getBody(), Map.class);
            String shopName = req.get("shop");
            String pluginName = routeContext.getParameter("plugin").toString();
            if(shopName == null) {
                String msg = "Shop name is not passed";
                log.error(msg + " site: " + siteKey);
                APIResponse errResp =
                        new APIResponse(Collections.singletonList(new ErrorResponse(msg, 400)));
                routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
            }
            StateContext site = siteService.getSiteStatus(siteKey);
            plugin.install(site.getSiteId(), shopName, siteKey, site.getRegion(), pluginName);
            routeContext.status(HttpConstants.StatusCode.OK).json()
                    .send(new APIResponse<>());
        } catch (SiteNotFoundException e) {
            String msg = "Site Not found";
            log.error(msg + " site: " + siteKey);
            APIResponse errResp =
                    new APIResponse(Collections.singletonList(new ErrorResponse(msg, SiteNotFound.getCode())));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (PluginException e) {
            String msg = "Error while interacting with plugin: ";
            log.error(msg + " site: " + siteKey + " reason:" + e.getMessage());
            APIResponse errResp =
                    new APIResponse(Collections.singletonList(new ErrorResponse(msg, e.code)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        } catch (JsonProcessingException e) {
            String msg = "Error while parsing the request";
            log.error(msg + " site: " + siteKey);
            APIResponse errResp =
                    new APIResponse(Collections.singletonList(new ErrorResponse(msg, 400)));
            routeContext.status(HttpConstants.StatusCode.BAD_REQUEST).json().send(errResp);
        }
    }
}

