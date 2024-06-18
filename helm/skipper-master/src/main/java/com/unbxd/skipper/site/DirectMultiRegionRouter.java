package com.unbxd.skipper.site;

import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.skipper.site.DAO.SiteDAO;
import com.unbxd.skipper.site.exception.InvalidRegionException;
import com.unbxd.skipper.site.model.DataCenter;

public class DirectMultiRegionRouter implements MultiRegionRouter {

    private String region;
    private SiteDAO siteDAO;

    @Inject
    public void DirectMultiRegionRouter(Config config, SiteDAO siteDAO) {
        String region = config.getProperty("region");
        if(region == null)
            throw new IllegalArgumentException("'region' property is not set, Add it in System.property");

        this.region = region;
        this.siteDAO = siteDAO;
    }

    public String getSkipperEndpoint(String region) throws InvalidRegionException {
        DataCenter dataCenter = siteDAO.getDataCenter(region);
        if(dataCenter == null)
            throw new InvalidRegionException("No region exists with " + region);
        return dataCenter.getSkipperEndPoint();
    }

    @Override
    public String redirect(String region) throws InvalidRegionException {
        if(region == null) {
            throw new InvalidRegionException("region property is null");
        }
        if(this.region.equals(region))
            return null;
        String skipperEndpoint = getSkipperEndpoint(region);
        if(skipperEndpoint == null) {
            throw new IllegalStateException("skipper endpoint not set with region : " + region);
        }
        return skipperEndpoint;
    }
}

