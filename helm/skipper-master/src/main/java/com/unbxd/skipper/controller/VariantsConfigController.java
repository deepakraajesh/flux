package com.unbxd.skipper.controller;

import com.google.inject.Inject;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.variants.exception.VariantsConfigException;
import com.unbxd.skipper.variants.service.VariantConfigService;
import com.unbxd.toucan.eventfactory.EventTag;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.PUT;
import ro.pippo.core.route.RouteContext;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static java.util.Collections.emptyMap;

@Log4j2
public class VariantsConfigController extends Controller {

    private VariantConfigService variantConfigService;
    private EventFactory eventFactory;
    private SiteService siteService;

    private static final String ENABLE = "enable";
    private static final String SITE_KEY_PARAM = "siteKey";
    private static final String SET_VARIANTS = "set_variants";



    @Inject
    public VariantsConfigController(VariantConfigService variantConfigService,
                                    SiteService siteService,
                                    EventFactory eventFactory) {
        this.variantConfigService = variantConfigService;
        this.siteService = siteService;
        this.eventFactory = eventFactory;
    }

    @PUT("/skipper/site/{"+ SITE_KEY_PARAM + "}/variants")
    public void setVariantConfig() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITE_KEY_PARAM).toString();
        Boolean enableVariants = routeContext.getParameter(ENABLE).toBoolean();
        String cookie = UN_SSO_UID + "=" + routeContext.getRequest().getCookie(UN_SSO_UID).getValue();
        try {
            StateContext site = siteService.getSiteStatus(siteKey);
            variantConfigService.setVariantsInSearch(siteKey, enableVariants);
            if(site.getOrgId() != null) {
                variantConfigService.setVariantsInPim(siteKey,enableVariants);
            }
            eventFactory.createAndFireEvent(eventFactory.getEmail("unbxd-bot"),
                    siteKey, System.currentTimeMillis() * 1000, siteKey,
                    "variants set successfully", EventTag.SUCCESS, SET_VARIANTS, emptyMap(),
                    null);
            routeContext.status(200).json().send(new APIResponse<>());
        }  catch (VariantsConfigException e) {
            log.error(e.getMessage() + " siteKey: " + siteKey);
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            routeContext.status(e.getCode()).json().send(APIResponse.getInstance(errorResponse, e.getCode()));
            eventFactory.createAndFireEvent(eventFactory.getEmail("unbxd-bot") ,
                    siteKey, System.currentTimeMillis() * 1000, siteKey,
                    e.getMessage(), EventTag.ERROR, SET_VARIANTS, emptyMap(), null);
        } catch (SiteNotFoundException e) {
            String message = "No Site found";
            log.error(message + " sitekey:" + siteKey);
            ErrorResponse errorResponse = new ErrorResponse(message);
            routeContext.status(400).json().send(APIResponse.getInstance(errorResponse, 400));
            eventFactory.createAndFireEvent(eventFactory.getEmail("unbxd-bot") ,
                    siteKey, System.currentTimeMillis() * 1000, siteKey,
                    e.getMessage(), EventTag.ERROR, SET_VARIANTS, emptyMap(), null);
        }
    }
}
