package com.unbxd.gcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.RejectAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.ResetAccountRequest;
import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.cbo.response.Response;
import com.unbxd.gcp.model.AccountResponse;
import com.unbxd.gcp.model.BulkAccountResponse;
import com.unbxd.gcp.service.AccountService;
import com.unbxd.gcp.service.Subscriber;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.*;
import ro.pippo.controller.extractor.Param;

import java.util.HashMap;
import java.util.Map;

import static com.unbxd.gcp.GCPConstants.*;
import static com.unbxd.gcp.model.AccountResponse.getInstance;
import static com.unbxd.toucan.eventfactory.EventBuilder.dispatch;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.logging.log4j.ThreadContext.get;

@Log4j2
@Path("/skipper/gcp")
public class AccountsController extends Controller {

    private final ObjectMapper mapper;
    private final Subscriber subscriber;
    private final AccountService accountService;

    private final String RESET_MSG = "Successfully reset account.";
    private final String REJECT_MSG = "Successfully rejected account.";
    private final String APPROVE_MSG = "Successfully approved account.";

    @Inject
    public AccountsController(ObjectMapper mapper,
                              Subscriber subscriber,
                              AccountService accountService) {
        this.accountService = accountService;
        this.subscriber = subscriber;
        this.mapper = mapper;
    }

    @GET("/accounts")
    @Produces(Produces.JSON)
    public Response<BulkAccountResponse> getAccounts(@Param("pageSize") int pageSize,
                                                     @Param("accounts") String accounts,
                                                     @Param("pageToken") String pageToken,
                                                     @Param("providerId") String providerId) {
        Response.Builder<BulkAccountResponse> builder = new Response.Builder<>();

        try {
            builder.withData(accountService.getAccounts(pageSize, accounts, pageToken));
        } catch (Exception e) {
            String error = "Error while trying to bulk fetch accounts: " + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e.getMessage())
                    .build());
            sendEvent(error, EMPTY);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/accounts/{accountId}")
    public Response<AccountResponse> getAccount(@Param("accountId") String accountId,
                                                @Param("providerId") String providerId) {
        Response.Builder<AccountResponse> builder = new Response.Builder<>();

        try {
            builder.withData(getInstance(accountService.getAccount(accountId)));
        } catch (Exception e) {
            String error = "Error while trying to fetch account[" + accountId + "]: "
                    + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, accountId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/accounts/{accountId}/approve")
    public Response<Map<String, String>> approveAccount(@Param("accountId") String accountId,
                                                        @Param("providerId") String providerId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            accountService.approveAccount(accountId, mapper.readValue(getRequest()
                    .getBody(), ApproveAccountRequest.class));
            builder.withData(singletonMap(MESSAGE, APPROVE_MSG));
            sendEvent(APPROVE_MSG, accountId);
        } catch (Exception e) {
            String error = "Error while trying to approve account[" + accountId + "]: "
                    + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, accountId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/accounts/{accountId}/reject")
    public Response<Map<String, String>> rejectAccount(@Param("accountId") String accountId,
                                                       @Param("providerId") String providerId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            accountService.rejectAccount(accountId, mapper.readValue(getRequest()
                    .getBody(), RejectAccountRequest.class));
            builder.withData(singletonMap(MESSAGE, REJECT_MSG));
            sendEvent(REJECT_MSG, accountId);
        } catch (Exception e) {
            String error = "Error while trying to reject account[" + accountId + "]: "
                    + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, accountId);
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @POST("/accounts/{accountId}/reset")
    public Response<Map<String, String>> resetAccount(@Param("accountId") String accountId,
                                                      @Param("providerId") String providerId) {
        Response.Builder<Map<String, String>> builder = new Response.Builder<>();

        try {
            accountService.resetAccount(accountId, mapper
                    .readValue(getRequest().getBody(), ResetAccountRequest.class));
            builder.withData(singletonMap(MESSAGE, RESET_MSG));
            sendEvent(RESET_MSG, accountId);
        } catch (Exception e) {
            String error = "Error while trying to reset account[" + accountId + "]: "
                    + e.getMessage();
            builder.withError(new Error.Builder().withCode(500).withMessage(e
                    .getMessage()).build());
            sendEvent(error, accountId);
            log.error(error);
        }
        return builder.build();
    }

    private void sendEvent(String message,
                           String accountId) {
        String user = getRouteContext().getLocal(EMAIL);

        /* tags map for event */
        Map<String, String> tags = new HashMap<>();
        tags.put(OPERATION_NAME, ACCOUNTS_OPERATION);
        tags.put(ACCOUNT_ID, accountId);
        tags.put(EVENT_TYPE, message);
        tags.put(USER, user);

        /* dispatch event to toucan */
        dispatch(user, GCP_INDEX, message, get(TRACE_HEADER), randomUUID().toString(),
                accountId, ACCOUNTS_OPERATION, currentTimeMillis(), tags);
    }



}
