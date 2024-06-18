package com.unbxd.skipper.relevancy.service;

import com.unbxd.console.model.ProductType;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.feed.model.FeedIndexingStatus;
import com.unbxd.skipper.relevancy.expection.RelevancyServiceException;
import com.unbxd.skipper.relevancy.model.*;
import com.unbxd.skipper.site.exception.SiteNotFoundException;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface RelevancyService {

    String SEARCHABLE_FIELD_JOB = "searchableFields";

    void processRelevancyStatus(String siteKey, String workflowId, String workflowName)
            throws RelevancyServiceException, SiteNotFoundException;

    APIResponse<RelevancyResponse> triggerJob(String siteKey, RelevancyRequest jobRequest) throws SiteNotFoundException;

    APIResponse<RelevancyOutputModel> getFields(String siteKey, JobType type);

    WorkflowStatus fetchRelevancyStatus(String siteKey, String workflowId) throws RelevancyServiceException;

    GetSearchableFieldsResponse getSearchableFields(String siteKey, PageRequest request) throws RelevancyServiceException, SiteNotFoundException;

    void updateSearchableFieldsInFieldService(String siteKey,List<SearchableField> searchableFields)
            throws RelevancyServiceException;

    String triggerCatalogIndexing(String siteKey) throws RelevancyServiceException, SiteNotFoundException;

    FeedIndexingStatus getStatus(String siteKey, String feedId, Integer count, String type) throws RelevancyServiceException, SiteNotFoundException;

    void reset(String cookie, String siteKey, JobType jobType, ProductType productType) throws RelevancyServiceException;

    Map<String, String> uploadFileToS3(String siteKey, File file) throws RelevancyServiceException;

    Map<String, String> uploadFileToS3(String siteKey, String prefix, File file) throws RelevancyServiceException;

    boolean hasJobRun(String siteKey, JobType jobName) throws SiteNotFoundException;

}
