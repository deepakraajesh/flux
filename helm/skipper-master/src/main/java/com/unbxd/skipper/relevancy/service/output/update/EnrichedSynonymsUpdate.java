package com.unbxd.skipper.relevancy.service.output.update;

import com.unbxd.console.model.ProductType;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;

public class EnrichedSynonymsUpdate implements RelevancyOutputUpdateProcessor {
    @Override
    public int update(String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException {
        return 0;
    }

    @Override
    public void reset(String cookie, String siteKey, JobType jobType, ProductType productType)
            throws RelevancyServiceException {

    }
}
