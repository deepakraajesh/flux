package com.unbxd.gcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.unbxd.config.Config;
import com.unbxd.gcp.dao.ProcurementDao;
import com.unbxd.gcp.model.Entitlement;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

import static com.google.api.client.util.DateTime.parseRfc3339;
import static com.google.pubsub.v1.ProjectSubscriptionName.of;
import static com.unbxd.gcp.GCPConstants.*;
import static com.unbxd.toucan.eventfactory.EventBuilder.dispatch;

@Log4j2
@Singleton
public class Subscriber {

    private final String PROJECT_ID;
    private final String SUBSCRIPTION_ID;

    private final ObjectMapper mapper;
    private final ProcurementDao procurementDao;
    private final AccountService accountService;
    private final EntitlementService entitlementService;

    @Inject
    public Subscriber(Config config,
                      AccountService accountService,
                      ProcurementDao procurementDao,
                      EntitlementService entitlementService) {
        SUBSCRIPTION_ID = config.getProperty(GCP_SUBSCRIPTION_ID);
        PROJECT_ID = config.getProperty(GCP_PROJECT_ID);
        this.entitlementService = entitlementService;
        this.procurementDao = procurementDao;
        this.accountService = accountService;
        this.mapper = new ObjectMapper();

        subscribeAsync();
    }

    private void subscribeAsync() {
        try {
            ProjectSubscriptionName subscriptionName = of(PROJECT_ID, SUBSCRIPTION_ID);
            com.google.cloud.pubsub.v1.Subscriber.newBuilder(subscriptionName, buildReceiver()).build()
                    .startAsync().awaitRunning();
            log.info("Listening for messages on: " + subscriptionName.toString());
        } catch (Exception e) {
            log.error("Error while trying to subscribe to pub/sub: " + e.getMessage());
        }
    }

    private MessageReceiver buildReceiver() {
        MessageReceiver receiver = (PubsubMessage message,
                                    AckReplyConsumer consumer) -> {
            String messageId = message.getMessageId();
            String data = message.getData().toStringUtf8();
            log.info("Message with id[" + messageId + "]" +
                    " received from pub/sub subscription: " + data);

            sendEvent(data);
            consumer.ack();
        };
        return receiver;
    }

    private void sendEvent(String data) {
        try {
            JsonNode jsonNode = mapper.readTree(data);
            String eventType = jsonNode.get(EVENT_TYPE).asText();

            if (eventType.contains(ACCOUNTS_PREFIX)) {
                sendAccountEvent(jsonNode);
            } else if (eventType.contains(ENTITLEMENTS_PREFIX)) {
                sendEntitlementEvent(jsonNode);
            } else {
                log.error("Unrecognizable event Type: " + eventType);
            }

        } catch (Exception e) {
            log.error("Exception while sending gcp event: " + e.getMessage());
        }
    }

    private void sendAccountEvent(JsonNode eventNode) {
        String eventId = eventNode.get(EVENT_ID).asText();
        String eventType = eventNode.get(EVENT_TYPE).asText();
        String accountId = eventNode.get(ACCOUNT).get(ID).asText();
        String updateTime = eventNode.get(ACCOUNT).get(UPDATE_TIME).asText();
        String user = procurementDao.getAccountMeta(accountService.getAccountName
                (accountId)).getEmail();

        /* construct message */
        String message = "Received event: " + eventType + " for accountId: "
                + accountId + " at time: " + updateTime;

        /* tags map for event */
        Map<String, String> tags = new HashMap<>();
        tags.put(OPERATION_NAME, ACCOUNTS_OPERATION);
        tags.put(EVENT_TYPE, eventType);
        tags.put(ACCOUNT_ID, accountId);
        tags.put(USER, user);

        /* dispatch event to toucan */
        dispatch(user, GCP_INDEX, message, eventId, eventId, accountId,
                ACCOUNTS_OPERATION, parseRfc3339(updateTime).getValue(), tags);
    }

    private void sendEntitlementEvent(JsonNode eventNode) {
        String eventId = eventNode.get(EVENT_ID).asText();
        String eventType = eventNode.get(EVENT_TYPE).asText();
        String entitlementId = eventNode.get(ENTITLEMENT).get(ID).asText();
        String updateTime = eventNode.get(ENTITLEMENT).get(UPDATE_TIME).asText();

        Entitlement entitlement = entitlementService.getEntitlement(entitlementId);
        String accountName =  entitlement.getEntitlementInfo().getAccount();
        String user = procurementDao.getAccountMeta(accountName).getEmail();
        String accountId = accountService.getAccountId(accountName);
        String plan = entitlement.getEntitlementInfo().getPlan();

        /* construct message */
        String message = "Received event: " + eventType + " for accountId: "
                + accountId + " and entitlement Id: " + entitlementId +
                " at time: " + updateTime + " with plan: " + plan;

        /* tags map for event */
        Map<String, String> tags = new HashMap<>();
        tags.put(OPERATION_NAME, ENTITLEMENTS_OPERATION);
        tags.put(ENTITLEMENT_ID, entitlementId);
        tags.put(EVENT_TYPE, eventType);
        tags.put(ACCOUNT_ID, accountId);
        tags.put(USER, user);
        tags.put(PLAN, plan);

        /* dispatch event to toucan */
        dispatch(user, GCP_INDEX, message, eventId, eventId, accountId,
             ENTITLEMENTS_OPERATION, parseRfc3339(updateTime).getValue(), tags);
    }
}
