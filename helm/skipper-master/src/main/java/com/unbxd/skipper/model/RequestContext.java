package com.unbxd.skipper.model;

import com.google.inject.Singleton;
import com.unbxd.skipper.site.model.SiteContext;
import com.unbxd.skipper.states.model.StateContext;
import ro.pippo.core.route.RouteContext;

@Singleton
public class RequestContext {

    private SiteContext siteContext;
    private StateContext stateContext;

    public RequestContext() {
        siteContext = new SiteContext();
        stateContext = new StateContext();
    }

    public Request getRequest() {
        return siteContext.getRequest();
    }

    public void setAppId(String appId) {
        stateContext.setAppId(appId);
    }

    public void setSiteKey(String siteKey) {
        stateContext.setSiteKey(siteKey);
    }

    public void setAuthToken(String authToken) {
        stateContext.setAuthToken(authToken);
    }

    public String getAppId() { return stateContext.getAppId(); }

    public String getSiteKey() { return stateContext.getSiteKey(); }

    public String getMongoId() { return stateContext.getId(); }

    public void setMongoId(String id) {
        stateContext.setId(id);
        
    }

    public String getAuthToken() { return stateContext.getAuthToken(); }

    public void setRequest(Request request)  {
        siteContext.setRequest(request);
    }

    public RouteContext getRouteContext() {
        return siteContext.getRouteContext();
    }

    public void setRoutContext(RouteContext routeContext) {
        siteContext.setRouteContext(routeContext);
    }

    public SiteContext getSiteContext() { return siteContext; }

    public StateContext getStateContext () { return stateContext; }
}
