package com.unbxd.skipper.feed.service.impl;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.SiteKeyCred;
import com.unbxd.field.service.FieldService;
import com.unbxd.pim.exception.PIMException;
import com.unbxd.pim.workflow.service.PIMService;
import com.unbxd.search.feed.MozartService;
import com.unbxd.search.feed.SearchFeedService;
import com.unbxd.search.feed.model.FeedReindexStatus;
import com.unbxd.search.feed.model.FeedStatus;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.feed.exception.FeedException;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;
import com.unbxd.skipper.feed.service.FeedService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

import static com.unbxd.config.Config.AWS_DEFAULT_REGION;
import static java.util.Objects.isNull;

@Log4j2
public class SearchFeedServiceImpl implements FeedService {

    protected SearchFeedService searchFeedService;
    protected MozartService mozartService;
    protected FieldService fieldService;
    protected PIMService pimService;
    protected AmazonS3 s3Client;

    @Inject
    public SearchFeedServiceImpl(SearchFeedService searchFeedService,
                                 MozartService mozartService,
                                 FieldService fieldService,
                                 PIMService pimService,
                                 Config config) {
        this.searchFeedService = searchFeedService;
        this.mozartService = mozartService;
        this.fieldService = fieldService;
        this.pimService = pimService;
        String awsDefaultRegion = config.getProperty(AWS_DEFAULT_REGION);
        if(isNull(awsDefaultRegion))
            throw new IllegalArgumentException(AWS_DEFAULT_REGION + " property not set");
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(awsDefaultRegion).build();
    }

    @Override
    public FeedIndexingStatus status(String siteKey,
                                     Integer count,
                                     String type) throws FeedException {
        try {
            Response<List<FeedStatus>> feedStatusList = searchFeedService
                    .feedStatus(fieldService.getSiteDetails(siteKey).getApiKey(),
                            siteKey, count, type).execute();
            if(!feedStatusList.isSuccessful()) {
                log.error("Error while fetching feed status for site: " + siteKey
                        + " reason:" + feedStatusList.errorBody().string());
                throw new FeedException("Error while fetching feed status from search service");
            }
            List<FeedStatus> statuses = feedStatusList.body();
            if(statuses == null || statuses.size() == 0) {
                return new FeedIndexingStatus();
            }
            FeedStatus feedStatus = statuses.get(0);
            return new FeedIndexingStatus(feedStatus.getStatus(),
                    feedStatus.getMessage(), feedStatus.getDuration(), feedStatus.getErrors());
        } catch (IOException | FieldException e) {
            log.error("Error while fetching feed status for site: " + siteKey  + " reason:" + e.getMessage());
            throw new FeedException("Error while fetching feed status from search service");
        }
    }

    @Override
    public FeedIndexingStatus status(String siteKey,
                                     String feedId,
                                     Integer count,
                                     String type) throws FeedException {
        try {
            FeedStatus feedStatus = null;
            SiteKeyCred siteDetails = fieldService.getSiteDetails(siteKey);
            if(feedId != null) {
                Response<FeedStatus> feedStatusResponse = searchFeedService
                        .feedStatus(siteDetails.getApiKey(), siteKey,
                                feedId, count, type).execute();
                if(!feedStatusResponse.isSuccessful()) {
                    log.error("Error while fetching feed status for site: " + siteKey
                            + " reason:" + feedStatusResponse.errorBody().string());
                    throw new FeedException("Error while fetching feed status from search service");
                }
                feedStatus = feedStatusResponse.body();
            } else {
                Response<List<FeedStatus>> feedStatusResponse = searchFeedService
                        .feedStatus(siteDetails.getApiKey(), siteKey,
                                count, type).execute();
                if(!feedStatusResponse.isSuccessful()) {
                    log.error("Error while fetching feed status for site: " + siteKey
                            + " reason:" + feedStatusResponse.errorBody().string());
                    throw new FeedException("Error while fetching feed status from search service");
                }
                List<FeedStatus> feedStatuses = feedStatusResponse.body();
                if(feedStatuses != null && feedStatuses.size() > 0)
                    feedStatus = feedStatuses.get(0);
            }

            if(feedStatus == null) {
                return null;
            }
            return new FeedIndexingStatus(feedStatus.getStatus(),
                    feedStatus.getMessage(), feedStatus.getDuration(), feedStatus.getErrors());
        } catch (IOException | FieldException e) {
            log.error("Error while fetching feed status for site: " + siteKey  + " reason:" + e.getMessage());
            throw new FeedException("Error while fetching feed status from search service");
        }
    }

    @Override
    public String triggerFullUpload(String siteKey) throws FeedException {
        try {
            String feedId = pimService.triggerFullUpload(siteKey);
            return feedId;
        } catch (PIMException e) {
            throw new FeedException(e.getMessage());
        }
    }

    @Override
    public String reIndexSearchFeed(String siteKey) throws FeedException {
        try {
            Response<FeedReindexStatus> response = mozartService.reindex(siteKey).execute();
            if(!response.isSuccessful()) {
                String msg = "Error with triggering reindex to search";
                log.error(msg +  " for siteKey: " + siteKey + " with code: " + response.code()
                        + " reason:" + response.errorBody().string());
                if (response.code() >= 400 && response.code() < 500)
                    msg = response.errorBody().string();
                throw new FeedException(response.code(), ErrorCode.InvalidResponseFromDownStream.getCode(),
                        msg);
            }
            return response.body().getRequestId();
        } catch (IOException e) {
            log.error("Error while triggering feed reindex for site: " + siteKey  + " reason:" + e.getMessage());
            throw new FeedException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                    "Error while fetching feed status from search service");
        }
    }


}

