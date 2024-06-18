package com.unbxd.skipper.site;

import com.unbxd.skipper.site.exception.InvalidRegionException;

public interface MultiRegionRouter {

    String redirect(String region) throws InvalidRegionException;
}
