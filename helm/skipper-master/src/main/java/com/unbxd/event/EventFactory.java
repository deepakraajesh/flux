package com.unbxd.event;

import com.google.inject.Singleton;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.toucan.eventfactory.Event;
import com.unbxd.toucan.eventfactory.EventBuilder;
import com.unbxd.toucan.eventfactory.EventTag;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static com.unbxd.skipper.model.Constants.EMAIL;
import static com.unbxd.skipper.model.Constants.EVENT_BUILDER;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections4.SetUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Log4j2
@Singleton
public class EventFactory {

    public static final String FACET = "facet";
    public static final String PIM_CALLBACK = "pim_callback";
    public static final String STATE_TRANSITION = "state_transition";
    public static final String SEARCHABLE_FIELDS = "searchable_fields";


    public void createAndFireEvent(String user,
                                   String siteKey,
                                   long timestamp,
                                   String eventId,
                                   String message,
                                   EventTag eventTag,
                                   String operationName,
                                   Map<String, String> tags,
                                   StateContext stateContext) {
        EventBuilder eventBuilder = getEventBuilder(stateContext);
        String thisInstant = Date.from(Instant.now()).toString();

        try {
            eventBuilder.withUser(user);
            eventBuilder.withSiteKey(siteKey);
            eventBuilder.withLogLevel(eventTag);
            eventBuilder.withStartTimestamp(timestamp);
            eventBuilder.withOperationName(operationName);
            eventBuilder.withEventId(eventId + "_" + thisInstant);
            if(isNotEmpty(message)) { eventBuilder.withMessage(message); }
            for(Map.Entry<String, String> tagEntry: emptyIfNull(tags.entrySet())) {
                eventBuilder.withTag(tagEntry.getKey(), tagEntry.getValue());
            }

            Event event = eventBuilder.start();
            event.finish();
        } catch (Exception e) {
            log.error("Error while trying to dispatch event for siteKey: " + siteKey
                    + " reason: " + e.getMessage());
        }
    }

    private EventBuilder getEventBuilder(StateContext stateContext) {
        RouteContext routeContext = RouteDispatcher.getRouteContext();

        if(routeContext != null) {
            EventBuilder eventBuilder = routeContext.getLocal(EVENT_BUILDER);
            if(stateContext != null) { stateContext.setEventBuilder(eventBuilder); }
            return eventBuilder;
        } else {
            return stateContext.getEventBuilder();
        }
    }

    public String getEmail(String defaultEmail) {
        RouteContext routeContext = RouteDispatcher.getRouteContext();
        String email = routeContext.getLocal(EMAIL);

        if(StringUtils.isEmpty(email)) { return defaultEmail; }
        return email;
    }

    public EventBuilder getEvent(String user,
                                 String message,
                                 String siteKey,
                                 EventTag eventTag,
                                 String operationName) {
        try {
            EventBuilder builder = getEventBuilder();
            builder.withEventId(siteKey + "_" + randomUUID().toString());
            builder.withStartTimestamp(currentTimeMillis() * 1000L);
            builder.withOperationName(operationName);
            builder.withLogLevel(eventTag);
            builder.withSiteKey(siteKey);
            builder.withMessage(message);
            builder.withUser(user);

            return builder;
        } catch (Exception e) {
            log.error("Exception while trying to create event for core "
                    + siteKey + " : " + e.getMessage());
        }
        return null;
    }

    private EventBuilder getEventBuilder() {
        RouteContext routeContext = RouteDispatcher.getRouteContext();
        if (routeContext != null) { return routeContext.getLocal(EVENT_BUILDER); }

        return new EventBuilder().withTraceId(randomUUID().toString());
    }

    public void sendEvent(EventBuilder eventBuilder) {
        Event event = eventBuilder.start();
        try {
            event.finish();
        } catch (Exception e) {
            log.error("Exception while trying to send event for core "
                    + event.getSiteKey() + " : " + e.getMessage());
        }
    }
}
