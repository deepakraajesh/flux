package com.unbxd.pim.event.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.event.EventFactory;
import com.unbxd.pim.event.EventProcessManager;
import com.unbxd.pim.event.EventProcessor;
import com.unbxd.pim.event.exception.EventException;
import com.unbxd.pim.event.model.*;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.model.Site;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.POST;
import ro.pippo.core.route.RouteContext;

import java.util.Collections;
import java.util.Map;

import static com.unbxd.event.EventFactory.PIM_CALLBACK;
import static com.unbxd.skipper.model.Constants.SITEKEY_PARAM;
import static com.unbxd.toucan.eventfactory.EventTag.INFO;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Log4j2
public class EventController extends Controller {

    private ObjectMapper mapper;
    private SiteService siteService;
    private EventFactory eventFactory;
    private EventProcessManager eventProcessor;
    private Map<EventType, EventProcessor> eventProcessors;

    public static final String FEED_OPERATION = "feed";
    private static final String IMPORT_ID = "importId";

    @Inject
    public EventController(SiteService siteService,
                           EventFactory eventFactory,
                           EventProcessManager eventProcessor,
                           Map<EventType, EventProcessor> eventProcessors) {
        this.siteService = siteService;
        this.mapper = new ObjectMapper();
        this.eventFactory = eventFactory;
        this.eventProcessor = eventProcessor;
        this.eventProcessors = eventProcessors;
    }

    @POST("/skipper/event/trigger")
    public void triggerEvent() {
        RouteContext routeContext = getRouteContext();
        try {
            Event event = mapper.readValue(routeContext.getRequest().getBody(), Event.class);
            eventProcessors.get(event.getName()).process(event);
            routeContext.status(200).json().send(new APIResponse<>(event.getEventResponse()));
        } catch(JsonProcessingException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            APIResponse<Site> siteResponse = new APIResponse<>(Collections.singletonList(errorResponse));
            routeContext.status(400).json().send(siteResponse);
        } catch (EventException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            APIResponse<Site> siteResponse = new APIResponse<>(Collections.singletonList(errorResponse));
            routeContext.status(500).json().send(siteResponse);
        }
    }

    @POST("/admin/sites/{" + SITEKEY_PARAM + "}/event/feed/callback")
    public void triggerCallback() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            PimCallbackRequest callbackRequest = mapper.readValue(routeContext
                    .getRequest().getBody(), PimCallbackRequest.class);
            PimMetaInfo pimMetaInfo = callbackRequest.getPimMetaInfo();
            String orgId = pimMetaInfo.getOrgId();

            eventFactory.createAndFireEvent(PIM_CALLBACK, siteKey,
                    pimMetaInfo.getTimestampInMillis() * 1000, orgId,
                    EMPTY, INFO, FEED_OPERATION, pimMetaInfo.getAsTags(),
                    null);

            routeContext.status(200).json().send(new APIResponse<>());
        } catch (JsonProcessingException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            APIResponse<Site> siteResponse = new APIResponse<>(Collections.singletonList(errorResponse));
            routeContext.status(400).json().send(siteResponse);
        }
    }

    @POST("/admin/sites/{" + SITEKEY_PARAM + "}/event/feed/pimUploadComplete")
    public void triggerPIMUploadComplete() {
        RouteContext routeContext = getRouteContext();
        String siteKey = routeContext.getParameter(SITEKEY_PARAM).toString();
        try {
            PimCallbackRequest callbackRequest = mapper.readValue(routeContext
                    .getRequest().getBody(), PimCallbackRequest.class);
            PimMetaInfo pimMetaInfo = callbackRequest.getPimMetaInfo();
            String orgId = pimMetaInfo.getOrgId();

//            eventFactory.createAndFireEvent(PIM_CALLBACK, siteKey,
//                    pimMetaInfo.getTimestampInMillis() * 1000, orgId,
//                    EMPTY, INFO, FEED_OPERATION, pimMetaInfo.getAsTags(),
//                    null);
            StateContext site = siteService.getSiteStatus(siteKey);
            String exportAdapterId = site.getAdapterId();
            processEvent(orgId, siteKey, exportAdapterId, EventType.PIM_UPLOAD_COMPLETE, pimMetaInfo);
            routeContext.status(200).json().send(new APIResponse<>());
        } catch (JsonProcessingException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            APIResponse<Site> siteResponse = new APIResponse<>(Collections.singletonList(errorResponse));
            routeContext.status(400).json().send(siteResponse);
        }  catch (EventException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            log.error("Error while trying to transition state: " + e.getMessage());

            //TODO : CHANGE IT TO NON-200 AFTER PIM CHANGES
            routeContext.status(200).json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
        } catch (SiteNotFoundException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            log.error("Error while trying to transition state: " + e.getMessage());
            routeContext.status(500).json().send(new APIResponse<>(Collections.singletonList(errorResponse)));
        }
    }

    private void processEvent(String orgId,
                              String siteKey,
                              String exportAdapterId,
                              EventType eventType,
                              PimMetaInfo pimMetaInfo) throws EventException {
        isPimMetaValid(pimMetaInfo, siteKey, orgId);

        Event event = new Event(orgId, eventType, siteKey, exportAdapterId,
                Collections.singletonMap(IMPORT_ID, pimMetaInfo.getImportId()),
                null, new EventResponse());
        eventProcessor.trigger(event);

    }

    private void isPimMetaValid(PimMetaInfo pimMetaInfo, String siteKey, String orgId) throws EventException {
        EventDetails eventDetails = pimMetaInfo.getEventDetails();

        if (eventDetails == null) {
            String msg = "Empty eventDetails from pimMetaInfo(PIM workflow callback)";
            log.error(msg + " for siteKey: " + siteKey + " for orgId: " + orgId);
            throw new EventException(msg);
        }
        if(isEmpty(eventDetails.getId())) {
            String msg = "Empty event.id(importId) from pimMetaInfo(PIM workflow callback)";
            log.error(msg + " for siteKey: " + siteKey + " for orgId: " + orgId);
            throw new EventException(msg);
        }
    }
}
