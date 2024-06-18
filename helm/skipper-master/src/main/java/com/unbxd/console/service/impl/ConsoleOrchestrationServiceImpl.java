package com.unbxd.console.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.*;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.console.service.ConsoleRemoteService;
import com.unbxd.console.service.FacetRemoteService;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.SiteKeyCred;
import com.unbxd.field.service.FieldService;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.controller.model.response.APIResponse;
import com.unbxd.skipper.controller.model.response.ErrorResponse;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.StateContext;
import lombok.extern.log4j.Log4j2;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unbxd.console.service.FacetRemoteService.FACET_AUTH_KEY;
import static java.util.Objects.isNull;
import static okhttp3.MediaType.parse;
import static okhttp3.MultipartBody.Part.createFormData;
import static okhttp3.RequestBody.create;

@Log4j2
public class ConsoleOrchestrationServiceImpl implements ConsoleOrchestrationService {

    private SiteService siteService;
    private FacetRemoteService facetRemoteService;
    private ConsoleRemoteService consoleRemoteService;
    private FieldService fieldService;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String ID = "id";
    private static final String SUCCESS = "success";
    private String facetAuthToken;

    @Inject
    public ConsoleOrchestrationServiceImpl(Config config,
                                           SiteService siteService,
                                           FacetRemoteService facetRemoteService,
                                           ConsoleRemoteService consoleRemoteService,
                                           FieldService fieldService) {
        this.siteService = siteService;
        this.facetRemoteService = facetRemoteService;
        this.consoleRemoteService = consoleRemoteService;
        this.facetAuthToken = config.getProperty(FACET_AUTH_KEY, null);
        if(facetAuthToken == null)
            throw new IllegalArgumentException(FACET_AUTH_KEY + " property is not defined");
        this.fieldService = fieldService;
    }

    @Override
    public ConsoleResponse createSite(String name,
                                      String region,
                                      String tag,
                                      String email,
                                      String language) throws Exception {
        ConsoleResponse consoleResponse = new ConsoleResponse();
        JsonObject request = createRequest(name, region, tag, email, language);

        Call<JsonObject> createSiteResponse = consoleRemoteService.
                createSite(facetAuthToken, "application/json", request);

        parseCreateSiteResponse(name, consoleResponse, createSiteResponse);
        return consoleResponse;
    }

    private void parseCreateSiteResponse(String name, ConsoleResponse consoleResponse,
                                      Call<JsonObject> createSiteResponse) throws Exception {
        Response<JsonObject> response = createSiteResponse.execute();
        consoleResponse.setStatus(response.code());
        if(response.code() == 422) {

            String errString = response.errorBody().string();
            Map<String, String> body = new Gson().fromJson(errString, Map.class);
            if(body.containsKey("status_text")) {
                log.error("Error while making request to console create site :" + name + " code:"
                        + response.code() + " reason: " + body.get("status_text"));
                consoleResponse.setError(body.get("status_text"));
            } else {
                log.error("Error while making request to console create site :" + name + " code:"
                        + response.code() + " reason: " + errString);
                consoleResponse.setError("Server error occured while creating search index");
            }
        } else if(response.code() != 200) {
           log.error("Error while making request to console create site :" + name + " code:"
                    + response.code() + " reason: " + response.errorBody().string());
            consoleResponse.setError("Server error occured while creating search index");
        } else {
            JsonObject responseContent = response.body();
            if(responseContent.getAsJsonObject(SITE).isJsonNull() ||
                    responseContent.getAsJsonObject(SITE).get(SITEKEY).isJsonNull() ||
                    responseContent.getAsJsonObject(SITE).get(ID).isJsonNull()
            ) {
                log.error("siteKey or ID is not returned in the response while creating the site in console name:" + name);
                consoleResponse.setError("Incorrect data has returned while creating search index");
            }
            String siteKey = responseContent.getAsJsonObject(SITE).get(SITEKEY).getAsString();
            consoleResponse.setId(responseContent.getAsJsonObject(SITE).get(ID).getAsString());
            consoleResponse.setSiteKey(siteKey);
        }

    }

    private JsonObject createRequest(String name,
                                     String region,
                                     String tag,
                                     String email,
                                     String language) {
        JsonObject rootObject = new JsonObject();
        JsonObject siteObject = new JsonObject();

        siteObject.addProperty(TAG, tag);
        siteObject.addProperty(NAME, name);
        siteObject.addProperty(REGION, region);
        siteObject.addProperty(SELF_SERVE_TAG, Boolean.TRUE.toString());
        siteObject.addProperty(LANGUAGE, language);

        rootObject.add(SITE, siteObject);
        rootObject.addProperty(EMAIL, email);

        return rootObject;
    }

    @Override
    public void uploadFacets(File file,
                             String sitekey,
                             String product)
            throws ConsoleOrchestrationServiceException {
            MultipartBody.Part partBody = createFormData("infile", file
                    .getName(), create(parse("text/csv"),
                    file));
            uploadFacets(sitekey, product, partBody);
    }

    private void uploadFacets(String sitekey,
                              String product,
                              MultipartBody.Part partBody)
            throws ConsoleOrchestrationServiceException {
        try {
            Response<Map<String, String>> response = facetRemoteService.uploadCsv
                    (facetAuthToken, sitekey, product, partBody)
                    .execute();
            validateResponse(response);
        } catch (IOException e) {
            throw new ConsoleOrchestrationServiceException(500,
                    "Error from console:" + e.getMessage());
        }
    }

    private void validateResponse(Response<Map<String, String>> response)
            throws ConsoleOrchestrationServiceException, IOException {
        if (!response.isSuccessful()) {
            throw new ConsoleOrchestrationServiceException(500, "Error "
                    + "while trying to upload facets to console: "
                    + response.errorBody().string());
        }
    }

    @Override
    public APIResponse<SingleFacetResponse> enableFacetInSiteRule(String authToken,
                                                                  String cookie,
                                                                  String siteKey,
                                                                  ConsoleFacetFieldRequest consoleFacetFieldRequest) {
        if(cookie != null && !cookie.isEmpty())
            authToken = null;
        for(ConsoleFacetField facet: consoleFacetFieldRequest.getFacets()) {
            if (consoleFacetFieldRequest.getFacet() == null) {
                consoleFacetFieldRequest.setFacet(SingleConsoleFacetField
                        .fromOther(facet));
                consoleFacetFieldRequest.setFacets(null);
            }
            Call<SingleFacetResponse> enableCallObj = facetRemoteService.enableFacetInSiteRule(authToken,
                    cookie, siteKey, consoleFacetFieldRequest);
            try {
                Response<SingleFacetResponse> enableResponse = enableCallObj.execute();
                if (enableResponse.code() != 200) {
                    String errorString = enableResponse.errorBody().string();
                    log.error("Error while updating facet to console for site:" + siteKey
                            + " code:" + enableResponse.code()
                            + " reason: "+ errorString);
                    return APIResponse.getInstance(mapErrorResponse(siteKey, errorString), enableResponse.code());
                }
            } catch (IOException e) {
                log.error("Error while calling facets API for siteKey: " + siteKey + " : reason: " + e.getMessage());
                return APIResponse.getInstance(ErrorResponse.getInstance("Error while requesting to console service."), 500);
            }
        }
        Call<FacetResponse> caller = facetRemoteService.publishSiteRule(authToken, cookie, siteKey,
                consoleFacetFieldRequest.getProductType());
        return makeFacetRequest(siteKey, caller);
    }

    @Override
    public APIResponse<FacetResponse> updateFacetPosition(String cookie, String siteKey,
                                                          String fromPos, String toPos, ProductType productType) {
        Call<FacetResponse> updateCallObj = facetRemoteService.updateFacetPosition(cookie, siteKey, fromPos,
                toPos, productType.name());
        APIResponse response = makeFacetRequest(siteKey, updateCallObj);
        if(response.isSuccessful()) {
            updateCallObj = facetRemoteService.publishSiteRule(null,
                    cookie, siteKey, productType);
            return makeFacetRequest(siteKey, updateCallObj);
        }
        return response;
    }

    @Override
    public APIResponse<FacetResponse> updateGlobalFacets(String authToken,
                                                         String cookie,
                                                         String sitekey,
                                                         ConsoleFacetFieldRequest facetFieldRequest) {
        if(cookie != null && !cookie.isEmpty())
            authToken = null;
        Call<FacetResponse> updateCallObj = facetRemoteService
                .updateGlobalFacets(authToken,cookie, sitekey, facetFieldRequest);
        return makeFacetRequest(sitekey, updateCallObj);
    }

    @Override
    public APIResponse<FacetResponse> updateSiteRule(String cookie, String sitekey, ConsoleFacetFieldRequest facetFieldRequest) {
        removeDisabledFacets(facetFieldRequest.getFacets());
        Call<FacetResponse> updateCallObj = facetRemoteService.updateSiteRuleFacets(cookie,
                sitekey, facetFieldRequest);
        APIResponse response = makeFacetRequest(sitekey, updateCallObj);
        if(response.isSuccessful()) {
            updateCallObj = facetRemoteService.publishSiteRule(null,
                    cookie, sitekey, facetFieldRequest.getProductType());
            return makeFacetRequest(sitekey, updateCallObj);
        }
        return response;
    }

    protected APIResponse makeFacetRequest(String sitekey, Call<FacetResponse> facetCallObj)  {
        try {
            Response<FacetResponse> updateResponse = facetCallObj.execute();
            int statusCode = updateResponse.code();
            if(statusCode == 200) {
                FacetResponse facetResponse = updateResponse.body();
                Map<String, String> facetErrors = facetResponse.getError();

                if(MapUtils.isNotEmpty(facetErrors)) {
                    log.error("Error while creating/updating facets in global bank: " +
                            mapper.writeValueAsString(facetErrors) + " siteKey: " + sitekey);
                    return APIResponse.getInstance(ErrorResponse.getInstance(facetErrors), 400);
                }
                return new APIResponse<>(facetResponse);
            } else if(statusCode == 404) {
                log.error("Error while updating facet to console for site:" + sitekey
                        + " code:" + statusCode
                        + " reason: "+ updateResponse.errorBody().string());
                return APIResponse.getInstance(ErrorResponse.getInstance("Error while requesting to console service."), statusCode);
            } else if (statusCode == 504) {
                log.error("Error while updating facet to console for site:" + sitekey
                        + " code:" + statusCode
                        + " reason: "+ updateResponse.errorBody().string());
                return APIResponse.getInstance(ErrorResponse.getInstance("Timeout occured within internal microservice"), statusCode);
            } else {
                String errorString = updateResponse.errorBody().string();
                log.error("Error while updating facet to console for site:" + sitekey
                        + " code:" + statusCode
                        + " reason: "+ errorString);
                return APIResponse.getInstance(mapErrorResponse(sitekey, errorString), statusCode);
            }
        } catch(IOException e) {
            log.error("Error while calling facets API for siteKey: " + sitekey + " : reason: "  + e.getMessage());
            return APIResponse.getInstance(ErrorResponse.getInstance("Error while requesting to internal microservice"), 500);
        }
    }

    @Override
    public APIResponse<FacetResponse> deleteGlobalFacets(String cookie, String siteKey, ConsoleFacetFieldRequest consoleFacetFieldRequest) {
        Call<FacetResponse> deleteCallObj = facetRemoteService.deleteGlobalFacets(cookie, siteKey, consoleFacetFieldRequest);
        return makeFacetRequest(siteKey, deleteCallObj);
    }

    @Override
    public APIResponse<FacetResponse> deleteSiteRuleFacets(String authToken, String cookie, String siteKey,
                                                           ConsoleFacetFieldRequest consoleFacetFieldRequest) {
        if(cookie != null && !cookie.isEmpty())
            authToken = null;
        Call<FacetResponse> deleteCallObj = facetRemoteService.deleteSiteRuleFacets(authToken, cookie,
                siteKey, consoleFacetFieldRequest);
        APIResponse response = makeFacetRequest(siteKey, deleteCallObj);
        if(response.isSuccessful()) {
            Call<FacetResponse> publishCallObj = facetRemoteService.publishSiteRule(null,
                    cookie, siteKey, consoleFacetFieldRequest.getProductType());
            return makeFacetRequest(siteKey, publishCallObj);
        }
        return response;
    }

    @Override
    public APIResponse<FacetResponse> fetchGlobalFacets(String cookie, String siteKey, String page) {
        Call<FacetResponse> fetchCallObject = facetRemoteService.fetchGlobalFacets(cookie,
                        siteKey, page, "50");
        return makeFacetRequest(siteKey, fetchCallObject);
    }

    @Override
    public APIResponse<FacetResponse> fetchSiteRuleFacets(String auth, String cookie, String siteKey, String page,
                                                        String sort, String query, String perPage, ProductType productType) {
        if(cookie != null && !cookie.isEmpty())
            auth = null;
        Call<FacetResponse> fetchCallObject = facetRemoteService.fetchSiteRuleFacets(auth, cookie,
                siteKey, page, sort, query, perPage, productType);
        APIResponse apiResponse = makeFacetRequest(siteKey, fetchCallObject);
        if(apiResponse.isSuccessful()) {
            FacetResponse facetResponse = (FacetResponse) apiResponse.getData();
            facetResponse.setPage(Integer.parseInt(page));
        }
        return apiResponse;
    }

    @Override
    public List<SiteDetails> getSites(String email, String regions)   throws ConsoleOrchestrationServiceException  {
        try{
            Response<JsonObject> consoleResponse = consoleRemoteService.getSites(email, regions).execute();
            if(consoleResponse.isSuccessful() && consoleResponse.body().get(SUCCESS).getAsBoolean() ) {
                SitesResponse sitesResponse = mapper.readValue(consoleResponse.body().toString()
                        ,SitesResponse.class);
                List<SiteDetails> siteDetailsList = sitesResponse.getData();
                return addStatusDetails(siteDetailsList);
            }
            else {
                log.error("Unable to fetch sites  for user email ID : "+ email + ", code: "+ consoleResponse.code() +
                        ",  errorMessage: " + ( isNull(consoleResponse.errorBody())?
                        consoleResponse.body().toString() : consoleResponse.errorBody().string() )
                );
                  throw new  ConsoleOrchestrationServiceException(500,"Unable to get sites");
            }
        } catch (IOException e) {
            log.error("Unable to fetch sites for user email ID: "+ email +" due to "+e.getMessage());
            throw new ConsoleOrchestrationServiceException(500,"Unable to get sites");
         }
    }

    @Override
    public Map<String, Object> getFeatures(String cookie, String siteKey) throws ConsoleOrchestrationServiceException {
        try {
            Response<FeatureWrapper> feature = facetRemoteService.fetchFeature(cookie, siteKey).execute();
            if (feature.isSuccessful()) {
                if(feature.body().getData() != null && feature.body().getData().getConfig() != null) {
                    return feature.body().getData().getConfig();
                }
                return new HashMap<>();
            } else {
                String msg = "Unable to fetch features";
                log.error(msg + " siteKey: " + siteKey + " code: " + feature.code()
                        + " reason: " + feature.errorBody().string());
                throw new ConsoleOrchestrationServiceException(feature.code(), msg);
            }
        } catch (IOException e) {
            String msg = "Unable to fetch features";
            log.error(msg + " siteKey: " + siteKey + " reason:" + e.getMessage());
            throw new ConsoleOrchestrationServiceException(500, msg);
        }
    }


    @Override
    public SiteProductsResponse getSiteProductsResponse(String cookie, String siteKey) {
        Call<SiteProductsResponse> siteProductsCallObj = facetRemoteService.fetchSiteProducts(cookie, siteKey);
        try {
            Response<SiteProductsResponse> siteProductsResponse = siteProductsCallObj.execute();
            int statusCode = siteProductsResponse.code();
            if(statusCode == 200) {
                return siteProductsResponse.body();
            } else {
                String errorString = siteProductsResponse.errorBody().string();
                if(StringUtils.isNotEmpty(errorString)) {
                    log.error("Error while trying to fetch site products" +
                            " for siteKey: " + siteKey + " response from console: " + errorString);
                    SiteProductsResponse siteProductsErrorResponse = mapper
                            .readValue(errorString, SiteProductsResponse.class);
                    siteProductsErrorResponse.setCode(statusCode);
                    return siteProductsErrorResponse;
                } else {
                    log.error("Error while trying to fetch site products" +
                            " for siteKey: " + siteKey + " response from console: " + statusCode);
                    return SiteProductsResponse.getInstance("Error while trying to fetch site products" +
                            " for siteKey: " + siteKey, statusCode);
                }
            }
        } catch(IOException e) {
            log.error("Error while trying to fetch site products for siteKey : " + siteKey, e);
            return SiteProductsResponse.getInstance("Error while trying to fetch site products" +
                    " for siteKey: " + siteKey, 500);
        }
    }

    @Override
    public InputStream getFrontEndSynonyms(String siteKey, String cookie) throws ConsoleOrchestrationServiceException {
        try {
            SiteKeyCred cred = fieldService.getSiteDetails(siteKey);
            if (isNull(cred) || isNull(cred.getSiteId())) {
                log.error("empty response from fieldService for siteKey : " + siteKey);
                throw new ConsoleOrchestrationServiceException(500, ErrorCode.EmptyResponseFromDownStream.getCode(),
                        ErrorCode.EmptyResponseFromDownStream.getMessage());
            }
            Response<ResponseBody> response = consoleRemoteService.getFrontEndSynonyms(cookie, cred.getSiteId()).execute();
            if (!response.isSuccessful()) {
                log.error("Error while  fetching front end synonyms from console backend service for siteKey : "
                        + siteKey);
                throw new ConsoleOrchestrationServiceException(500, ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                        ErrorCode.UnsuccessfulResponseFromDownStream.getMessage());
            }
            if (isNull(response.body())) {
                log.error("empty/null response from console for siteKey : " + siteKey);
                throw new ConsoleOrchestrationServiceException(500, ErrorCode.EmptyResponseFromDownStream.getCode(),
                        ErrorCode.EmptyResponseFromDownStream.getMessage());
            }
            return response.body().byteStream();
        } catch (IOException e) {
            String errorMessage = "Error while fetching front end synonyms from console backend service";
            log.error(errorMessage + " siteKey: " + siteKey + " reason: " + e.getMessage());
            throw new ConsoleOrchestrationServiceException(500, ErrorCode.IOError.getCode(), errorMessage);
        } catch (FieldException e) {
            String errorMessage = "Error while fetching site details from field service";
            log.error(errorMessage + " siteKey: " + siteKey + " reason: " + e.getMessage());
            throw new ConsoleOrchestrationServiceException(500, errorMessage);
        }
    }

    private ErrorResponse mapErrorResponse(String siteKey, String errorBody) {
        try {
            return mapper.readValue(errorBody, ErrorResponse.class);
        } catch(IOException e) {
            log.error("Error while parsing request from console for site:" + siteKey + " reason: " + e.getMessage());
            return ErrorResponse.getInstance(errorBody);
        }
    }

    private void removeDisabledFacets(List<ConsoleFacetField> consoleFacetFields) {
        consoleFacetFields.removeIf(facetField -> facetField.getEnabled() == null || !facetField.getEnabled());
    }

    private List<SiteDetails> addStatusDetails(List<SiteDetails> siteDetailsList) {
        for(SiteDetails siteDetails : siteDetailsList) {
            try {
                StateContext statusDetails = siteService.getSiteStatus(siteDetails.getSiteKey());
                siteDetails.setStatusDetails(statusDetails);
            } catch (SiteNotFoundException e) {
                String message = "no details found for the siteKey:"+siteDetails.getSiteKey()+" in skipper";
                log.error(message);
            }
        }
        return siteDetailsList;
    }


    @Override
    public void deleteSite(String siteKey, String cookie) throws FieldException {
        try {
            Call<Map<String, Object>> deleteCallToConsole = facetRemoteService.deleteSite(cookie, siteKey);
            Response<Map<String, Object>> responseFromConsole = deleteCallToConsole.execute();
            if(!responseFromConsole.isSuccessful()) {
                    String msg = "Error while deleting site from console";
                    log.error(msg + " for site:" + siteKey
                            + " code:" + responseFromConsole.code() + " reason:" + responseFromConsole.errorBody().string());
                    throw new FieldException(500, msg);
            }
        } catch(IOException e) {
            String msg = "Error with Console service.";
            log.error(msg + " for site:" + siteKey + " reason:" + e.getMessage());
            throw new FieldException(500, msg);
        }
    }

    @Override
    public Set<String> getQueriesUsedInQueryRules(String siteKey, String cookie)
            throws ConsoleOrchestrationServiceException, FieldException {
        SiteKeyCred cred = fieldService.getSiteDetails(siteKey);
        if (isNull(cred) || isNull(cred.getSiteId())) {
            log.error("empty response from fieldService for siteKey : " + siteKey);
            throw new ConsoleOrchestrationServiceException(500, ErrorCode.EmptyResponseFromDownStream.getCode(),
                    ErrorCode.EmptyResponseFromDownStream.getMessage());
        }
        String siteId = cred.getSiteId();
        try {
            Call<QueryRuleWrapper> queryRuleCall = facetRemoteService.getQueryRules(siteId, cookie);
            Response<QueryRuleWrapper> queryRuleResponse = queryRuleCall.execute();
            if(!queryRuleResponse.isSuccessful()) {
                String msg = "Error while fetching query rule from console";
                log.error(msg + " for site:" + siteId
                        + " code:" + queryRuleResponse.code() + " reason:" + queryRuleResponse.errorBody().string());
                throw new ConsoleOrchestrationServiceException(500, msg);
            }
            return queryRuleResponse.body().getQueries();
        } catch(IOException e) {
            String msg = "Error while fetching query rule from console";
            log.error(msg + " for site:" + siteId + " reason:" + e.getMessage());
            throw new ConsoleOrchestrationServiceException(500, msg);
        }
    }
}
