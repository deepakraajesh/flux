package com.unbxd.skipper.relevancy.service.output.update;

import com.google.inject.Inject;
import com.unbxd.console.model.ProductType;
import com.unbxd.field.service.FieldService;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.JobType;

public class SearchableFieldStatsUpdateProcessor extends SearchableFieldUpdateProcessor {

    @Inject
    public SearchableFieldStatsUpdateProcessor(RelevancyDao relevancyDao, FieldService fieldService, AmazonS3Client s3Client) {
        super(relevancyDao, fieldService, s3Client);
    }

    @Override
    public int update(String siteKey,
                      JobType jobType,
                      ProductType productType) throws RelevancyServiceException {
        return update(siteKey, jobType, productType, Boolean.FALSE);
    }
}
