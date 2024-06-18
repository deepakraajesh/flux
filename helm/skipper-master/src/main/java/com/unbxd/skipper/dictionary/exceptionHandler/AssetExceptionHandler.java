package com.unbxd.skipper.dictionary.exceptionHandler;

import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpStatus;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.RouteContext;

import java.util.HashMap;

import static com.unbxd.toucan.eventfactory.EventTag.INFO;

@Log4j2
public class AssetExceptionHandler implements ExceptionHandler{

    @Override
    public Class<? extends Exception> exception() { return AssetException.class; }

    @Override
    public ro.pippo.core.ExceptionHandler handler(){
        return (Exception ex, RouteContext route) -> {
            route.status(200);
            route.json().send(new HashMap<String, String>() {{
                put("error", ErrorCode.ASSET_ERROR);
                put("message", ex.getMessage());
            }});
            sendEvent("Error while performing dictionary operation in skipper: ", ex.getMessage(), INFO, DICT_OP);
            log.error("Asset operation failed with status: {} and error: {}", 200, ex.getMessage());
        };
    }
}
