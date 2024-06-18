package com.unbxd.skipper.dictionary.exceptionHandler;

import com.google.inject.Inject;
import com.unbxd.event.EventFactory;
import com.unbxd.toucan.eventfactory.EventTag;

import static java.util.Collections.emptyMap;
import static ro.pippo.core.route.RouteDispatcher.getRouteContext;

public interface ExceptionHandler {
    EventFactory factory = new EventFactory();
    String DICT_OP = "dictionary";
    String USER = "user";

    Class<? extends Exception> exception();

    ro.pippo.core.ExceptionHandler handler();

    default void sendEvent(String msg,
                           String siteKey,
                           EventTag eventTag,
                           String operationName) {
        factory.createAndFireEvent(getRouteContext().getLocal(USER), siteKey,
                System.currentTimeMillis() * 1000, siteKey, msg,
                eventTag, operationName, emptyMap(), null);
    }
}
