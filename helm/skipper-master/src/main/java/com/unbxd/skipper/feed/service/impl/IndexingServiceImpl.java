package com.unbxd.skipper.feed.service.impl;

import com.google.inject.Inject;
import com.unbxd.autosuggest.AutosuggestService;
import com.unbxd.autosuggest.exception.AutosuggestException;
import com.unbxd.autosuggest.model.AutosuggestIndexResponse;
import com.unbxd.config.Config;
import com.unbxd.search.feed.SearchFeedService;
import com.unbxd.search.feed.model.FeedStatus;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.feed.DAO.IndexingStatusDAO;
import com.unbxd.skipper.feed.exception.IndexingException;
import com.unbxd.skipper.feed.model.IndexingStatusData;
import com.unbxd.skipper.feed.model.IndexingStatus;
import com.unbxd.skipper.feed.service.IndexingService;
import com.unbxd.skipper.feed.service.FeedService;
import com.unbxd.skipper.feed.exception.FeedException;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public class IndexingServiceImpl implements IndexingService {
    private AutosuggestService autosuggestService;
    private IndexingStatusDAO indexingStatusDAO;
    private SearchFeedService searchFeedService;
    private FeedService feedService;
    private Config config;

    private static final String INDEXING = "INDEXING";
    private static final String INDEXED = "INDEXED";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";
    private static final String AUTOSUGGEST_POLLING_THRESHOLD_PROPERTY_NAME = "autosuggestThreshold";


    @Inject
    public IndexingServiceImpl(AutosuggestService autosuggestService,
                               IndexingStatusDAO indexingStatusDAO,
                               SearchFeedService searchFeedService,
                               FeedService feedService,
                               Config config) {
        this.autosuggestService = autosuggestService;
        this.indexingStatusDAO = indexingStatusDAO;
        this.searchFeedService = searchFeedService;
        this.feedService = feedService;
        this.config = config;
    }

    @Override
    public void indexAutosuggest(String siteKey) throws IndexingException {
        try {
            IndexingStatus status = getStatus(siteKey);
            String autosuggestPollingThreshold = config.getProperty(AUTOSUGGEST_POLLING_THRESHOLD_PROPERTY_NAME);
            if (isNull(status)) {
                AutosuggestIndexResponse response = nonNull(autosuggestPollingThreshold) ?
                        autosuggestService.indexSuggestions(siteKey, autosuggestPollingThreshold) :
                        autosuggestService.indexSuggestions(siteKey);
                addStatus(siteKey, response);
            } else if (nonNull(status.getAutosuggest())) {
                if (!status.getAutosuggest().isCompleted()) {
                    throw new IndexingException(400, ErrorCode.AutosuggestIndexingInProgress.getCode(),
                            "autosuggest indexing is in progress");
                } else {
                    AutosuggestIndexResponse response = autosuggestService.indexSuggestions(siteKey);
                    addStatus(siteKey, response);
                }
            } else if (nonNull(status.getCatalog())) {
                AutosuggestIndexResponse response = nonNull(autosuggestPollingThreshold) ?
                        autosuggestService.indexSuggestions(siteKey, autosuggestPollingThreshold) :
                        autosuggestService.indexSuggestions(siteKey) ;
                updateStatus(response, status);
            } else {
                log.error("indexing is not triggered reason: status object does not have any previous indexing status"
                        + " data for siteKey: " + siteKey + ", something has went wrong while updating previous indexing "
                        + "status");
                throw new IndexingException(500, ErrorCode.StatusObjectParsingError.getCode(), INTERNAL_ERROR_MESSAGE);
            }
        } catch (AutosuggestException e) {
            throw new IndexingException(e.getStatusCode(), e.getMessage());
        }
    }

    @Override
    public void indexCatalog(String siteKey) throws IndexingException {
        IndexingStatus status = getStatus(siteKey);
        if (isNull(status)) {
            String feedId = triggerFullUpload(siteKey);
            addStatus(siteKey, feedId);
        } else if (nonNull(status.getCatalog())) {
            if (!status.getCatalog().isCompleted()) {
                throw new IndexingException(400, ErrorCode.CatalogIndexingInProgress.getCode(),
                        "Already catalog reindexing process has been triggered from console");
            } else {
                String feedId = triggerFullUpload(siteKey);
                addStatus(siteKey, feedId);
            }
        } else if (nonNull(status.getAutosuggest())) {
            String feedId = triggerFullUpload(siteKey);
            updateStatus(feedId, status);
        } else {
            log.error("indexing is not triggered reason: status object does have any previous indexing status " +
                    "data for siteKey: " + siteKey + ", something has went wrong while updating previous indexing "
                    + "status");
            throw new IndexingException(500, ErrorCode.StatusObjectParsingError.getCode(), INTERNAL_ERROR_MESSAGE);
        }
    }

    private String triggerFullUpload(String siteKey) throws IndexingException {
        try {
            String feedId = feedService.reIndexSearchFeed(siteKey);
            log.info("catalog indexing triggered feedId:" + feedId);
            return feedId;
        } catch (FeedException e) {
            throw new IndexingException(500, e.getErrorCode(), e.getMessage());
        }
    }

    private void addStatus(String siteKey,
                           AutosuggestIndexResponse response) throws IndexingException, AutosuggestException {
        validateAutosuggestIndexResponse(siteKey, response);
        IndexingStatusData statusData = new IndexingStatusData();
        statusData.setStatus(response.getStatus());
        statusData.setFeedId(response.getFeedId());
        statusData.setTriggeredAt(response.getCreatedAt());
        statusData.setCode(response.getCode());
        if (nonNull(response.getUpdates()))
            statusData.setMessage(response.getUpdates().toString());
        statusData.setPreviousIndexingTime(autosuggestService.fetchLatestIndexingTime(siteKey));
        IndexingStatus status = new IndexingStatus();
        status.setAutosuggest(statusData);
        status.setSiteKey(siteKey);
        indexingStatusDAO.addStatus(status);
    }

    private void addStatus(String siteKey,
                           String catalogFeedId) throws IndexingException {
        IndexingStatusData statusData = new IndexingStatusData();
        statusData.setStatus(INDEXING);
        statusData.setFeedId(catalogFeedId);
        statusData.setCode(200);
        statusData.setPreviousIndexingTime(fetchPreviousCatalogIndexingTime(siteKey));
        IndexingStatus status = new IndexingStatus();
        status.setCatalog(statusData);
        status.setSiteKey(siteKey);
        indexingStatusDAO.addStatus(status);
    }

    @Override
    public IndexingStatus getStatus(String siteKey) throws IndexingException {
        IndexingStatus status = indexingStatusDAO.fetchStatus(siteKey);
        if (nonNull(status)) updateStatus(status);
        return status;
    }

    private void updateStatus(IndexingStatus status) throws IndexingException {
        String siteKey = status.getSiteKey();
        try {
            boolean needToUpdateStatus = false;
            if (nonNull(status.getAutosuggest()) && !status.getAutosuggest().isCompleted()) {
                needToUpdateStatus = true;
                com.unbxd.autosuggest.model.AutosuggestIndexingStatus autosuggestIndexingStatus =
                        autosuggestService.getIndexingStatus(siteKey,
                                status.getAutosuggest().getFeedId());
                status.getAutosuggest().setStatus(autosuggestIndexingStatus.getStatus());
                status.getAutosuggest().setCode(autosuggestIndexingStatus.getCode());
                if(nonNull(autosuggestIndexingStatus.getUpdates()))
                status.getAutosuggest().setMessage(autosuggestIndexingStatus.getUpdates().toString());
            }
            if (nonNull(status.getCatalog()) && !status.getCatalog().isCompleted()) {
                needToUpdateStatus = true;
                Response<FeedStatus> feedStatusResponse = searchFeedService.feedStatus(siteKey,
                        status.getCatalog().getFeedId()).execute();
                if (!feedStatusResponse.isSuccessful()) {
                    log.error("Error while fetching feed status for site: " + siteKey
                            + " reason:" + feedStatusResponse.errorBody().string());
                    throw new IndexingException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                            "Error while fetching feed status from search service");
                }
                FeedStatus feedStatus = feedStatusResponse.body();
                if(isNull(feedStatus)) {
                    log.error("empty response from feed service (autosuggest) for site:" + siteKey);
                    throw new IndexingException(500, ErrorCode.EmptyResponseFromDownStream.getCode(),
                            "Error while fetching feed status from search service");
                }
                status.getCatalog().setStatus(feedStatus.getStatus());
                status.getCatalog().setTriggeredAt(feedStatus.getTriggeredAt());
                int errorCode = (feedStatus.getErrors() != null && feedStatus.getErrors().size() > 0) ? 1 : 0;
                status.getCatalog().setCode(errorCode);
                status.getCatalog().setMessage(feedStatusResponse.body().getMessage());
            }
            if (needToUpdateStatus) indexingStatusDAO.updateStatus(status);
        } catch (AutosuggestException e) {
            throw new IndexingException(e.getStatusCode(), e.getMessage());
        } catch (IOException e) {
            log.error("Error while fetching feed status for site: " + siteKey + " reason:" + e.getMessage());
            throw new IndexingException(500, ErrorCode.IOError.getCode(),
                    "Error while fetching feed status from search service");
        }
    }


    private Long fetchPreviousCatalogIndexingTime(String siteKey) throws IndexingException {
        Response<List<FeedStatus>> feedStatusResponse = null;
        try {
            feedStatusResponse = searchFeedService.feedStatus(siteKey, 10).execute();

            if (!feedStatusResponse.isSuccessful()) {
                log.error("Error while fetching feed status for site: " + siteKey + "statusCode:" +
                        feedStatusResponse.code() + " reason:" + feedStatusResponse.errorBody().string());
                throw new IndexingException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                        "Error while fetching feed status from search service");
            } else {
                if (nonNull(feedStatusResponse.body())) {
                    for (FeedStatus status : feedStatusResponse.body()) {
                        if (status.getStatus().equals(INDEXED)) {
                            long result = getIndexingTimeInMilliSeconds(siteKey, status.getMessage());
                            return result != 0 ? result : null;
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            log.error("IO Error while fetching feed status for site: " + siteKey + " reason:" + e.getMessage());
            throw new IndexingException(500, ErrorCode.IOError.getCode(), "Error while fetching feed status " +
                    "from search service");
        }
    }

    private long getIndexingTimeInMilliSeconds(String siteKey, String message) {
        // sample message : "success time-taken: [ 1 minute 58 seconds 834 milliseconds 399 microseconds ]"
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(message);
        List<Integer> temp = new ArrayList<>(5);
        while (matcher.find()) {
            temp.add(Integer.parseInt(matcher.group()));
        }
        // ex: temp = [1, 58, 834, 399]
        if (temp.size() < 2 || temp.size() > 5) {
            log.info("unable to get indexing time for " + siteKey + " from message: " + message);
            return 0;
        }
        int secInMilliSeconds = 1000;
        int minuteInMilliSeconds = 60 * secInMilliSeconds;
        int hourInMilliSeconds = 60 * minuteInMilliSeconds;
        long result = temp.get(temp.size() - 2); // ignoring microseconds
        if (temp.size() >= 3) result += temp.get(temp.size() - 3) * secInMilliSeconds;
        if (temp.size() >= 4) result += temp.get(temp.size() - 4) * minuteInMilliSeconds;
        if (temp.size() == 5) result += temp.get(temp.size() - 5) * hourInMilliSeconds;
        return result;
    }

    private void validateAutosuggestIndexResponse(String siteKey,
                                                  AutosuggestIndexResponse response)
            throws IndexingException {
        if (isNull(response.getCode())
                || isNull(response.getCreatedAt())
                || isNull(response.getStatus())
                || isNull(response.getFeedId())) {
            log.error("missing expected fields in the autosuggestIndex response object:" + response.toString() +
                    " for siteKey:" + siteKey);
            throw new IndexingException(500, ErrorCode.IncorrectAutosuggestIndexResponse.getCode(),
                    "Error while autosuggest indexing");
        }
    }

    private void updateStatus(AutosuggestIndexResponse response,
                              IndexingStatus status) throws IndexingException {
        validateAutosuggestIndexResponse(status.getSiteKey(), response);
        IndexingStatusData statusData = new IndexingStatusData();
        statusData.setStatus(response.getStatus());
        statusData.setFeedId(response.getFeedId());
        statusData.setTriggeredAt(response.getCreatedAt());
        statusData.setCode(response.getCode());
        if (nonNull(response.getUpdates())) statusData.setMessage(response.getUpdates().toString());
        status.setAutosuggest(statusData);
        indexingStatusDAO.updateStatus(status);
    }

    private void updateStatus(String catalogFeedId,
                              IndexingStatus status) {
        IndexingStatusData statusData = new IndexingStatusData();
        statusData.setStatus(INDEXING);
        statusData.setFeedId(catalogFeedId);
        statusData.setCode(200);
        status.setCatalog(statusData);
        indexingStatusDAO.updateStatus(status);
    }

}
