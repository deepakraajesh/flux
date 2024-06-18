package com.unbxd.skipper.relevancy.service.output.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.console.model.ProductType;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.feed.dim.DimException;
import com.unbxd.skipper.feed.dim.DimensionMappingService;
import com.unbxd.skipper.feed.dim.model.DimensionMap;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.AutosuggestConfig;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class DimensionMappingUpdate implements RelevancyOutputUpdateProcessor {

    private RelevancyDao relevancyDao;
    private DimensionMappingService mappingService;
    protected AmazonS3Client s3Client;
    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    public DimensionMappingUpdate(RelevancyDao relevancyDao,
                                  DimensionMappingService mappingService,
                                  AmazonS3Client s3Client) {
        this.relevancyDao = relevancyDao;
        this.mappingService = mappingService;
        this.s3Client = s3Client;
    }

    @Override
    public int update(String siteKey,
                      JobType jobType,
                      ProductType productType) throws RelevancyServiceException {
        RelevancyOutputModel relevancyOutput = relevancyDao.fetchRelevancyOutput(jobType, siteKey);
        if (relevancyOutput == null )  return 0;

        DimensionMap[] data = {};
        try {
            data = mapper.readValue(s3Client.get(relevancyOutput.getS3Location()),
                    DimensionMap[].class);
            for (DimensionMap dimMap: data) {
                mappingService.save(siteKey, dimMap);
            }
        } catch (DimException e) {
            throw new RelevancyServiceException(e.getCode(), e.getMessage());
        } catch (JsonProcessingException e) {
            String msg = "Error while reading dimension map output, reason: " + e.getMessage();
            log.error(msg);
        }
        return data.length;
    }

    @Override
    public void reset(String cookie,
                      String siteKey,
                      JobType jobType,
                      ProductType productType) throws RelevancyServiceException {
        update(siteKey, jobType, productType);
    }
}
