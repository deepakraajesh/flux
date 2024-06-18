package com.unbxd.skipper.controller;

import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.core.route.RouteContext;

import java.util.Collections;

@Log4j2
public class SiteConfigController extends Controller {

    @GET("/sites")
    public void getSites() {
        RouteContext routeContext = getRouteContext();
        routeContext.json().send(Collections.singletonMap("msg", "hello world"));
    }

}

