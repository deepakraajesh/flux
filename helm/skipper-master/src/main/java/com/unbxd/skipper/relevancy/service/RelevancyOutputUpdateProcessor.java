package com.unbxd.skipper.relevancy.service;

import com.unbxd.console.model.ProductType;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.JobType;

public interface RelevancyOutputUpdateProcessor {
    int update(String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException;

    void reset(String cookie, String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException;
}
