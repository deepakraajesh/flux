package com.unbxd.skipper.dictionary.exceptionHandler;

import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.skipper.dictionary.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import java.util.HashMap;

import static com.unbxd.toucan.eventfactory.EventTag.INFO;

@Log4j2
public class AnalyserExceptionHandler implements ExceptionHandler{

    @Override
    public Class <? extends Exception> exception() { return AnalyserException.class; }

    @Override
    public ro.pippo.core.ExceptionHandler handler(){
        return (Exception ex, RouteContext route) -> {
            int status = HttpConstants.StatusCode.BAD_REQUEST;
            route.status(status);
            route.json().send(new HashMap<String, String>() {{
                put("error", ErrorCode.ANALYZER_ERROR);
                put("message", ex.getMessage());
            }});
            sendEvent("Error while performing dictionary operation in skipper: ", ex.getMessage(), INFO, DICT_OP);
            log.error("analyzer operation failed with status: {} and error: {}", status, ex.getMessage());
        };
    }
}
