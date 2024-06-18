package com.unbxd.skipper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.route.RouteContext;

import java.util.HashMap;
import java.util.Map;

import static com.unbxd.skipper.model.Constants.requestParams;

@Data
@Log4j2
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {

    private Map<String, String> params;

    public Request() { params = new HashMap<>(); }

    /** method to create Request instance, expected one time call at the
     *  beginning of the controller, all subsequent access to Request
     *  object must be through <code>RequestContext.getRequest()<code/>
     */
    public static Request getInstance(RouteContext context) {
        Request request = new Request();
        setParams(request, context);
        return request;
    }

    public static void setParams(Request request, RouteContext context) {
        for(String param: requestParams) {
            ParameterValue parameter = context.getParameter(param);
            if(!parameter.isEmpty()) { request.params.put(param, parameter.getValues()[0]); }
        }
    }
}
