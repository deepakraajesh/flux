package com.unbxd.console.service;

import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.*;
import com.unbxd.field.exception.FieldException;
import com.unbxd.skipper.controller.model.response.APIResponse;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface ConsoleOrchestrationService {

    String TAG = "tag";
    String NAME = "name";
    String SITE = "site";
    String EMAIL = "email";
    String REGION = "region";
    String SITEKEY = "site_key";
    String SELF_SERVE_TAG = "is_self_serve";
    String LANGUAGE = "language";

    ConsoleResponse createSite(String name,
                               String region,
                               String tag,
                               String email ,
                               String language) throws Exception;

    APIResponse<FacetResponse> fetchGlobalFacets(String cookie,
                                                 String siteKey,
                                                 String page);

    APIResponse<FacetResponse> fetchSiteRuleFacets(String auth,
                                                   String cookie,
                                                   String siteKey,
                                                   String page,
                                                   String sort,
                                                   String query,
                                                   String perPage,
                                                   ProductType productType);

    APIResponse<FacetResponse> updateFacetPosition(String cookie, String siteKey,
                                                   String fromgetSiteProductsResponsePos, String toPos, ProductType productType);

    APIResponse<FacetResponse> updateSiteRule(String cookie, String siteKey, ConsoleFacetFieldRequest facetFieldRequest);

    APIResponse<FacetResponse> updateGlobalFacets(String authToken, String cookie, String siteKey, ConsoleFacetFieldRequest facetFieldRequest);

    APIResponse<FacetResponse> deleteGlobalFacets(String cookie, String siteKey, ConsoleFacetFieldRequest consoleFacetFieldRequest);

    APIResponse<FacetResponse> deleteSiteRuleFacets(String authToken, String cookie, String siteKey,
                                                    ConsoleFacetFieldRequest consoleFacetFieldRequest);

    APIResponse<SingleFacetResponse> enableFacetInSiteRule(String authToken, String cookie, String siteKey, ConsoleFacetFieldRequest consoleFacetFieldRequest);

    List<SiteDetails> getSites(String email, String region) throws ConsoleOrchestrationServiceException;

    Map<String, Object> getFeatures(String cookie, String siteKey) throws ConsoleOrchestrationServiceException;

    SiteProductsResponse getSiteProductsResponse(String cookie, String siteKey);

    InputStream getFrontEndSynonyms(String siteKey, String cookie) throws ConsoleOrchestrationServiceException;

    void deleteSite(String siteKey, String cookie) throws FieldException;

    void uploadFacets(File file, String sitekey, String product) throws ConsoleOrchestrationServiceException;

    Set<String> getQueriesUsedInQueryRules(String siteId, String cookie)
            throws ConsoleOrchestrationServiceException, FieldException;
}
