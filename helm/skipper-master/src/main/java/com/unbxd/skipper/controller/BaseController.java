package com.unbxd.skipper.controller;

import com.unbxd.skipper.model.Request;
import com.unbxd.skipper.model.RequestContext;
import org.apache.commons.lang3.ArrayUtils;
import ro.pippo.controller.Controller;
import ro.pippo.core.route.RouteContext;

import static com.unbxd.skipper.model.Constants.*;

public abstract class BaseController extends Controller {

    protected RequestContext initializeRequestContext(RouteContext routeContext) {
        RequestContext requestContext = new RequestContext();
        Request request = Request.getInstance(routeContext);

        requestContext.setSiteKey(request.getParams().get(SITEKEY_PARAM));
        requestContext.setAuthToken(routeContext.getHeader(AUTH_HEADER));
        requestContext.setAppId(request.getParams().get(APP_ID_PARAM));
        requestContext.setRoutContext(routeContext);
        requestContext.setRequest(request);

        setMongoId(routeContext, requestContext);
        return requestContext;
    }

    private void setMongoId(RouteContext routeContext, RequestContext requestContext) {
        String[] values = routeContext.getParameter(ID_PARAM).getValues();
        if(ArrayUtils.isNotEmpty(values)) { requestContext.setMongoId(values[0]); }
    }
}
