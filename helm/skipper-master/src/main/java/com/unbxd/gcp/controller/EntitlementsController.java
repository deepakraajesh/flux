package com.unbxd.gcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementPlanChangeRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementRequest;
import com.google.cloudcommerceprocurement.v1.model.RejectEntitlementPlanChangeRequest;
import com.google.cloudcommerceprocurement.v1.model.RejectEntitlementRequest;
import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.cbo.response.Response;
import com.unbxd.config.Config;
import com.unbxd.gcp.model.BulkEntitlementResponse;
import com.unbxd.gcp.model.Entitlement;
import com.unbxd.gcp.model.EntitlementResponse;
import com.unbxd.gcp.service.AccountService;
import com.unbxd.gcp.service.EntitlementService;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.*;
import ro.pippo.controller.extractor.Param;

import java.util.HashMap;
import java.util.Map;

import static com.unbxd.gcp.GCPConstants.*;
import static com.unbxd.gcp.model.EntitlementResponse.getInstance;
import static com.unbxd.toucan.eventfactory.EventBuilder.dispatch;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.logging.log4j.ThreadContext.get;

@Log4j2
@Path("/skipper/gcp")
public class EntitlementsController extends Controller {

    private final String providerId;
    private final ObjectMapper mapper;
    private final AccountService accountService;
    private final EntitlementService entitlementService;

    private final String PROVIDER_ID = "provider.Id";
    private final String REJECT_MSG = "Successfully rejected entitlement";
    private final String APPROVE_MSG = "Successfully approved entitlement";
    private final String REJECT_PLAN_MSG = "Successfully rejected plan change";
    private final String APPROVE_PLAN_MSG = "Successfully approved plan change";


    @Inject
    public EntitlementsController(Config config,
                                  ObjectMapper mapper,
                                  AccountService accountService,
                                  EntitlementService entitlementService) {
        this.providerId = config.getProperty(PROVIDER_ID);
        this.entitlementService = entitlementService;
        this.accountService = accountService;
        this.mapper = mapper;
    }

    @GET("/entitlements")
    @Produces(Produces.JSON)
    public Response<BulkEntitlementResponse> getEntitlements(@Param int pageSize,
                                                             @Param String filter,
                                                             @Param String pageToken,
                                                             @Param String providerId) {
        Response.Builder<BulkEntitlementResponse> builder = new Response.Builder<>();

        try {
            builder.withData(entitlementService.getEntitlements
                    (pageSize, filter, pageToken));
        } catch (Exception e) {
            String error = "Error while trying to bulk fetch entitlements: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e.getMessage())
                    .build());
            sendEvent(error, EMPTY);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/entitlements/{entitlementId}")
    public Response<EntitlementResponse> getEntitlement(@Param String providerId,
                                                        @Param String entitlementId) {
        Response.Builder<EntitlementResponse> builder = new Response.Builder<>();

        try {
            builder.withData(getInstance(entitlementService.getEntitlement
                    (entitlementId)));
        } catch (Exception e) {
            String error = "Error while trying to fetch entitlement[" + entitlementId
                    + "]: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, entitlementId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @POST("/entitlements/{entitlementId}/approve")
    public Response<Map<String, String>> approveEntitlement(@Param String providerId,
                                                            @Param String entitlementId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            entitlementService.approveEntitlement(entitlementId, mapper
                    .readValue(getRequest().getBody(), ApproveEntitlementRequest.class));
            builder.withData(singletonMap(MESSAGE, APPROVE_MSG));
            sendEvent(APPROVE_MSG, entitlementId);
        } catch (Exception e) {
            String error = "Error while trying to approve entitlement[" + entitlementId
                    + "]: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, entitlementId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @POST("/entitlements/{entitlementId}/reject")
    public Response<Map<String, String>> rejectEntitlement(@Param String providerId,
                                                           @Param String entitlementId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();
        try {
            entitlementService.rejectEntitlement(entitlementId, mapper
                    .readValue(getRequest().getBody(), RejectEntitlementRequest.class));
            builder.withData(singletonMap(MESSAGE, REJECT_MSG));
            sendEvent(REJECT_MSG, entitlementId);
        } catch (Exception e) {
            String error = "Error while trying to reject entitlement[" + entitlementId
                    + "]: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, entitlementId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/entitlements/{entitlementId}/planChange/approve")
    public Response<Map<String, String>> approvePlanChange(@Param String providerId,
                                                           @Param String entitlementId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            entitlementService.approvePlanChange(entitlementId, mapper
                    .readValue(getRequest().getBody(), ApproveEntitlementPlanChangeRequest.class));
            builder.withData(singletonMap(MESSAGE, APPROVE_PLAN_MSG));
            sendEvent(APPROVE_PLAN_MSG, entitlementId);
        } catch (Exception e) {
            String error = "Error while trying to approve plan change for entitlement["
                    + entitlementId + "]: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, entitlementId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/entitlements/{entitlementId}/planChange/reject")
    public Response<Map<String, String>> rejectPlanChange(@Param String providerId,
                                                          @Param String entitlementId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            entitlementService.rejectPlanChange(entitlementId, mapper
                    .readValue(getRequest().getBody(), RejectEntitlementPlanChangeRequest.class));
            builder.withData(singletonMap(MESSAGE, REJECT_PLAN_MSG));
            sendEvent(REJECT_PLAN_MSG, entitlementId);
        } catch (Exception e) {
            String error = "Error while trying to reject plan change for entitlement["
                    + entitlementId + "]: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, entitlementId);
            log.error(error);
        }
        return builder.build();
    }

    private void sendEvent(String message,
                           String entitlementId) {
        Entitlement entitlement = entitlementService.getEntitlement(entitlementId);
        String accountId = accountService.getAccountId(entitlement
                .getEntitlementInfo().getAccount());
        String plan = entitlement.getEntitlementInfo().getPlan();
        String user = getRouteContext().getLocal(EMAIL);

        /* tags map for event */
        Map<String, String> tags = new HashMap<>();
        tags.put(OPERATION_NAME, ENTITLEMENTS_OPERATION);
        tags.put(ENTITLEMENT_ID, entitlementId);
        tags.put(ACCOUNT_ID, accountId);
        tags.put(EVENT_TYPE, message);
        tags.put(USER, user);
        tags.put(PLAN, plan);

        /* dispatch event to toucan */
        message += " for entitlement Id: " + entitlementId + " with plan: " + plan;
        dispatch(user, GCP_INDEX, message, get(TRACE_HEADER), randomUUID().toString(),
                accountId, ENTITLEMENTS_OPERATION, currentTimeMillis(), tags);
    }

}
