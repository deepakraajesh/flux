package com.unbxd.config.controller;

import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.config.model.HealthCheckResponse;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.core.route.RouteContext;

public class HealthCheckController extends Controller {

    private final Config config;

    @Inject
    public HealthCheckController(Config config) { this.config = config; }

    @GET("/services/healthcheck")
    public void healthCheck() {
        RouteContext routeContext = getRouteContext();

        HealthCheckResponse healthCheckResponse = config.checkServiceHealth();
        routeContext.status(healthCheckResponse.getCode());
        routeContext.json().send(healthCheckResponse);
    }
}
