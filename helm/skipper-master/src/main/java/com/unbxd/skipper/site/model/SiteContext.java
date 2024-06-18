package com.unbxd.skipper.site.model;

import com.unbxd.skipper.model.Request;
import lombok.Data;
import ro.pippo.core.route.RouteContext;

@Data
public class SiteContext {

    private Request request;
    private RouteContext routeContext;
}
