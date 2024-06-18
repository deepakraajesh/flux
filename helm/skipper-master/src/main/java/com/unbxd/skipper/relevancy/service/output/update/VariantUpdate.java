package com.unbxd.skipper.relevancy.service.output.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.console.model.ProductType;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.feed.dim.model.VariantConfig;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import com.unbxd.skipper.variants.exception.VariantsConfigException;
import com.unbxd.skipper.variants.service.VariantConfigService;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.List;

@Log4j2
public class VariantUpdate implements RelevancyOutputUpdateProcessor {

    protected RelevancyDao relevancyDao;
    protected VariantConfigService variantConfigService;
    protected AmazonS3Client s3Client;
    protected static ObjectMapper mapper = new ObjectMapper();

    @Inject
    public VariantUpdate(RelevancyDao relevancyDao,
                         VariantConfigService variantConfigService,
                         AmazonS3Client s3Client) {
        this.relevancyDao = relevancyDao;
        this.variantConfigService = variantConfigService;
        this.s3Client = s3Client;
    }

    @Override
    public int update(String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException {
        RelevancyOutputModel relevancyOutput = relevancyDao.fetchRelevancyOutput(jobType, siteKey);
        if (relevancyOutput == null) { return 0; }
        File file = s3Client.downloadFile(relevancyOutput.getS3Location());
        try {
            VariantConfig[] data = mapper.
                    readValue(s3Client.get(relevancyOutput.getS3Location()), VariantConfig[].class);

            if(data != null && data.length > 0)
                variantConfigService.setVariantsInSearch(siteKey, data[0].getVariants());
        } catch (VariantsConfigException e) {
                throw new RelevancyServiceException(e.getCode(), e.getMessage());
        } catch (JsonProcessingException e) {
            String msg = "Error while parsing variants configuration, reason " + e.getMessage();
            log.error(msg);
            throw new RelevancyServiceException(500, msg);
        }
        return 0;
    }

    @Override
    public void reset(String cookie, String siteKey, JobType jobType, ProductType productType)
            throws RelevancyServiceException {
        update(siteKey, jobType, productType);
    }
}

