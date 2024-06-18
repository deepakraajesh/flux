package com.unbxd.gcp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.config.Config;
import com.unbxd.gcp.exception.GCPException;
import com.unbxd.gcp.model.AccountMeta;
import com.unbxd.gcp.service.AccountService;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.*;
import ro.pippo.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.unbxd.gcp.GCPConstants.*;
import static com.unbxd.toucan.eventfactory.EventBuilder.dispatch;
import static java.lang.System.currentTimeMillis;
import static java.util.Base64.getDecoder;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.logging.log4j.ThreadContext.get;

@Log4j2
@Path("/gcp/activation")
public class ActivationController extends Controller {

    private ObjectMapper mapper;
    private final String redirectionUrl;
    private final AccountService accountService;

    private final String SIGNUP = "signup";
    private final String PAYLOAD_KEY = "sub";
    private final String ACCOUNT_NAME = "accountName";
    private final String REDIRECTION_PATH = "/gcp-ui/activation";
    private final String REDIRECTION_DOMAIN = "gcp.activation.url";
    private final String MARKETPLACE_JWT_HEADER = "x-gcp-marketplace-token";

    @Inject
    public ActivationController(Config config,
                                ObjectMapper mapper,
                                AccountService accountService) {
        this.redirectionUrl = config.getProperty(REDIRECTION_DOMAIN)
                + REDIRECTION_PATH;
        this.accountService = accountService;
        this.mapper = mapper;
    }

    @POST("/activate")
    public void activate() {
        try {
            String encodedPayload = getRequest().getParameter
                    (MARKETPLACE_JWT_HEADER).toString().split(quote("."))[1];
            String payload = new String(getDecoder().decode(encodedPayload));
            String accountName = mapper.readTree(payload).get(PAYLOAD_KEY)
                    .asText();

            if (isEmpty(accountName)) {
                throw new GCPException("Received empty account name in JWT token.");
            }

            accountName = accountService.getAccountName(accountName);
            log.info("Received JWT payload after sign-up: " + payload);
            Response.get().redirect(redirectionUrl + "?" + ACCOUNT_NAME
                    + "=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error while parsing JWT token for account activation: "
                    + e.getMessage());
        }
    }

    @POST("/signup")
    @Consumes(Consumes.JSON)
    @Produces(Produces.JSON)
    public com.unbxd.cbo.response.Response<Map<String, String>> signup() {
        com.unbxd.cbo.response.Response.Builder<Map<String, String>> builder
                = new com.unbxd.cbo.response.Response.Builder<>();
        try {
            AccountMeta accountMeta = mapper.readValue(getRequest().getBody(),
                    AccountMeta.class);
            accountService.approveAccount(accountService.getAccountId(accountMeta
                    .getAccountName()), new ApproveAccountRequest());
            accountService.saveActivationData(accountMeta);
            sendEvent(accountMeta);

            builder.withData(singletonMap(MESSAGE, "Data submitted successfully"));
        } catch (Exception e) {
            log.error("Error while trying to save activation data: " + e.getMessage());
            builder.withError(new Error.Builder().withCode(500).withMessage(e.getMessage())
                    .build());
        }
        return builder.build();
    }

    private void sendEvent(AccountMeta accountMeta) {
        String accountId = accountService.getAccountId(accountMeta.getAccountName());
        String message = "Received signup form submission for accountId: " + accountId
                + " - " + accountMeta.toString();

        /* tags map for event */
        Map<String, String> tags = mapper.convertValue
                (accountMeta, new TypeReference<>() {});
        tags.put(OPERATION_NAME, ACCOUNTS_OPERATION);
        tags.put(USER, accountMeta.getEmail());
        tags.put(ACCOUNT_ID, accountId);
        tags.put(EVENT_TYPE, SIGNUP);

        /* dispatch event to toucan */
        dispatch(accountMeta.getEmail(), GCP_INDEX, message, get(TRACE_HEADER),
                randomUUID().toString(), accountId, ACCOUNTS_OPERATION,
                currentTimeMillis(), tags);
    }
}
