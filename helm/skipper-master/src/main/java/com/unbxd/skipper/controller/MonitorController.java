package com.unbxd.skipper.controller;

import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.core.route.RouteContext;

@Log4j2
public class MonitorController  extends Controller {

    @GET("/monitor")
    public void monitor() {
        RouteContext routeContext = getRouteContext();
        routeContext.json().status(200).send("Chug!! Chug!!");
    }
}

