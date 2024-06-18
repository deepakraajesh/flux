package com.unbxd.skipper.relevancy.service.output.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.console.model.ProductType;
import com.unbxd.s3.AmazonS3Client;
import com.unbxd.skipper.autosuggest.exception.SuggestionServiceException;
import com.unbxd.skipper.autosuggest.service.SuggestionService;
import com.unbxd.skipper.relevancy.dao.RelevancyDao;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.AutosuggestConfig;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.relevancy.model.RelevancyOutputModel;
import com.unbxd.skipper.relevancy.service.RelevancyOutputUpdateProcessor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class AutosuggestConfigUpdate implements RelevancyOutputUpdateProcessor {
    protected RelevancyDao relevancyDao;
    protected SuggestionService suggestionService;
    protected AmazonS3Client s3Client;
    protected static ObjectMapper mapper = new ObjectMapper();

    @Inject
    public AutosuggestConfigUpdate(RelevancyDao relevancyDao,
                                   SuggestionService suggestionService,
                                   AmazonS3Client s3Client){
        this.relevancyDao = relevancyDao;
        this.suggestionService = suggestionService;
        this.s3Client = s3Client;
    }

    @Override
    public int update(String siteKey,
                      JobType jobType,
                      ProductType productType) throws RelevancyServiceException {
        if(!JobType.autosuggest.equals(jobType)) {
            String msg = "Unsupported jobType specified " + jobType;
            log.error(msg + " for site:" + siteKey);
            throw new RelevancyServiceException(400, msg);
        }
        RelevancyOutputModel output = relevancyDao.fetchRelevancyOutput(jobType, siteKey);
        if(output == null)
            return 0;
        try {
            AutosuggestConfig config = mapper.readValue(s3Client.get(output.getS3Location()), AutosuggestConfig.class);
            suggestionService.addSuggestions(siteKey, config, true);
        } catch (SuggestionServiceException e) {
            log.error("Error while updating the autosuggest config for site:" + siteKey + " reason:"
                    + e.getMessage());
            throw new RelevancyServiceException(e.getStatusCode(), e.getMessage());
        } catch (JsonProcessingException e) {
            String msg = "Error while reading autosuggest config data from the jobs output, reason: " + e.getMessage();
            log.error(msg);
            throw new RelevancyServiceException(500, msg);
        }
        return 1;
    }


    @Override
    public void reset(String cookie,
                      String siteKey,
                      JobType jobType,
                      ProductType productType) throws RelevancyServiceException {

    }
}
