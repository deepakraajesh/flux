package com.unbxd.field.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.*;
import com.unbxd.field.service.FieldService;
import com.unbxd.field.service.GimliRemoteService;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.autosuggest.service.HagridRemoteService;
import com.unbxd.skipper.relevancy.model.FieldAliasMapping;
import com.unbxd.skipper.relevancy.model.PageRequest;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;
import ro.pippo.core.HttpConstants;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

@Log4j2
public class GimliFieldService implements FieldService {

    private GimliRemoteService service;
    private HagridRemoteService hagridRemoteService;
    private LoadingCache<String, SiteKeyCred> credentialsCache;

    private static final String RELEVANCY = "relevancy";
    private static final String GIMLI_FAILURE_STATUS = "Failure";
    private static final String SEARCH_WEIGHTAGE = "searchWeightage";
    private static final String GIMLI_INVALID_REQUEST_CODE = "InValidRequest";
    private static final String GIMLI_ERROR_CODE_FOR_INVALID_SEARCHABLE_FIELD = "InValidFieldNameInRelevancy";

    @Inject
    public GimliFieldService(GimliRemoteService gimliRemoteService,
                             HagridRemoteService hagridRemoteService) {
        this.service = gimliRemoteService;
        this.credentialsCache = getCredentialsCache();
        this.hagridRemoteService = hagridRemoteService;
    }

    private LoadingCache<String, SiteKeyCred> getCredentialsCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build(new CacheLoader<>() {
                    @Override
                    public SiteKeyCred load(String siteKey) throws FieldException {
                        Call<SiteKeyCred> credentialsCallObj = service.getSiteDetails(siteKey);
                        try {
                            Response<SiteKeyCred> credentialsResponse = credentialsCallObj.execute();
                            int statusCode = credentialsResponse.code();
                            if (statusCode == 200) {
                                return credentialsResponse.body();
                            } else {
                                log.error("Fetching credentials failed for siteKey: " + siteKey + " with code: "
                                        + statusCode + " , error from gimli: " + credentialsResponse
                                        .errorBody().string());
                                throw new FieldException(statusCode, "Unable to fetch credentials for siteKey: " + siteKey);
                            }
                        } catch (IOException e) {
                            log.error("Error while trying to update credentials cache for siteKey: "
                                    + siteKey, e);
                            throw new FieldException(500, "Error while trying to update credentials" +
                                    " for siteKey: " + siteKey);
                        }
                    }
                });
    }

    @Override
    public FieldMapping getFieldMapping(String siteKey) throws FieldException{
        try {
            Response<FieldMappingWrapper> fieldMappingResponse = service.getDimensionMap(siteKey).execute();
            if (fieldMappingResponse.isSuccessful()){
                FieldMappingWrapper wrapper = fieldMappingResponse.body();
                if(wrapper != null) {
                    return wrapper.getData();
                }
                return null;
            } else {
                log.error("Error while making request for fieldMapping for siteKey:" + siteKey +
                        " threw statusCode:" +  fieldMappingResponse.code() +
                        " reason:" + fieldMappingResponse.message());
                throw new FieldException(fieldMappingResponse.code(), "GetDimensionMap API is not working");
            }
        } catch (IOException e) {
            log.error("Error while making request for fieldMapping for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new FieldException(ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                    "Error with Gimli service");
        }
    }

    @Override
    public void saveDimensionMap(String siteKey, Map<String, String> mapping) throws FieldException{
        try {
            Response<FieldServiceCommonResponse> fieldMappingResponse =
                    service.saveDimensionMap(siteKey, mapping).execute();
            if (fieldMappingResponse.isSuccessful()){
                FieldServiceCommonResponse resp = fieldMappingResponse.body();
                if(resp != null && !"true".equals(resp.getSuccess())) {
                    log.error("Error from Field service: "+ resp.getMessage() + " for site: " + siteKey);
                    throw new FieldException(500, "Error : "+ resp.getMessage());
                }
            } else {
                log.error("Error while making request for fieldMapping for siteKey:" + siteKey +
                        " threw statusCode:" +  fieldMappingResponse.code() +
                                " reason:" + fieldMappingResponse.message());
                throw new FieldException(fieldMappingResponse.code(), "Gimli API is not working");
            }
        } catch (IOException e) {
            log.error("Error while making request for fieldMapping for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new FieldException(ErrorCode.UnsuccessfulResponseFromDownStream.getCode(),
                    "Error with Gimli service");
        }
    }

    @Override
    public List<Fields> getFields(String siteKey,   String fieldType) throws FieldException {
        try {
            Response<List<Fields>> fieldResponse = service.getFields(siteKey, fieldType).execute();

            if(fieldResponse.isSuccessful()) {
                return fieldResponse.body();
            } else {
                if (fieldResponse.code() == 500) {
                    throw new FieldException(fieldResponse.code(), "SiteKey is missing!!!");
                } else {
                    throw new FieldException(fieldResponse.code(), "GetIndexFields API is not working");
                }
            }

        } catch (IOException e) {
            throw new FieldException(500, "Error with Gimli service.");
        }
    }

    @Override
    public SiteKeyCred getSiteDetails(String siteKey) throws FieldException {
        try {
            return this.credentialsCache.get(siteKey);
        } catch(ExecutionException e) {
            log.error("Error while fetching site details for site: " + siteKey + " reason: " + e.getMessage());
            throw new FieldException(500, "Error while fetching site details for site: " + siteKey);
        }
    }

    @Override
    public void deleteSite(String siteKey) throws FieldException {
        try {
            SiteKeyCred cred = getSiteDetails(siteKey);
            if(cred != null) {
                Response<JsonObject> deleteResp = hagridRemoteService
                        .deleteSite(cred.getSecretKey(), cred.getApiKey(), siteKey).execute();
                if(!deleteResp.isSuccessful()) {
                    String msg = "Error while deleting site in search";
                    log.error(msg + " for site:" + siteKey
                            + " code:" + deleteResp.code() + " reason:" + deleteResp.errorBody().string());
                    throw new FieldException(500, msg);
                }
            }
        } catch(IOException e) {
            String msg = "Error with Hagrid service.";
            log.error(msg + " for site:" + siteKey + " reason:" + e.getMessage());
            throw new FieldException(500, msg);
        }
    }

    @Override
    public SearchableFieldsResponse getSearchableFields(String siteKey,
                                                        PageRequest request) throws FieldException {
        try {
            // transform sortBy value to gimli service compatible
            if(SEARCH_WEIGHTAGE.equals(request.getSortBy())) request.setSortBy(RELEVANCY);

            Response<SearchableFieldsResponse> response = service.getSearchableFields(siteKey,request).execute();
            if(!response.isSuccessful()){
                log.error("Error while fetching searchable fields from gimli service for siteKey: "+siteKey+
                        ", statusCode: " + response.code() + (isNull(response.errorBody())? "" : "reason: "+
                        response.errorBody().string()));
                throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get searchable fields");
            }
            else {
                SearchableFieldsResponse fieldServiceSearchableFieldsResponse = response.body();
                if (GIMLI_FAILURE_STATUS.equals(fieldServiceSearchableFieldsResponse.getStatus())) {
                    ErrorResponse errorResponse = fieldServiceSearchableFieldsResponse.getErrors().iterator().next();
                    if (GIMLI_INVALID_REQUEST_CODE.equals(errorResponse.getCode()))
                        throw new FieldException(HttpConstants.StatusCode.BAD_REQUEST, errorResponse.getMessage());
                    else{
                        log.error("Error while fetching searchable fields from gimli service for siteKey:" + siteKey +
                                ", errorCode:" + errorResponse.getCode() + " and errorMessage: "+ errorResponse.getMessage());
                        throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR, "Unable to get searchable fields");
                    }
                }
                else
                     return fieldServiceSearchableFieldsResponse;
            }
        } catch (IOException e){
            log.error("Unable to fetch searchable fields from gimli service for sitekey:"+siteKey +"due to:"+e.getMessage());
            throw new FieldException(500,"Unable to get searchable fields");
        }
    }

    @Override
    public void updateSearchableFields(String siteKey, List<FSSearchableField> searchableFields)
            throws FieldException{
        try {
            Response<FieldServiceBaseResponse> response = service.
                    updateSearchableFields(siteKey,searchableFields).execute();
            if(!response.isSuccessful()){
                log.error("Error while updating searchable fields in gimli service for siteKey: "+siteKey+
                        ", statusCode: " + response.code() + (isNull(response.errorBody())? "" : "reason: "+
                        response.errorBody().string()));
                throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to update searchable fields");
            }
            else {
                FieldServiceBaseResponse fieldServiceUpdateResponse = response.body();
                if (GIMLI_FAILURE_STATUS.equals(fieldServiceUpdateResponse.getStatus())) {
                    ErrorResponse errorResponse = fieldServiceUpdateResponse.getErrors().iterator().next();
                    if(GIMLI_ERROR_CODE_FOR_INVALID_SEARCHABLE_FIELD.equals(errorResponse.getCode())) {
                        throw new FieldException(HttpConstants.StatusCode.BAD_REQUEST, errorResponse.getMessage());
                    }
                    else {
                        log.error("Error while updating searchable fields in gimli service for siteKey:" + siteKey +
                                ", errorCode:" + errorResponse.getCode() + " and errorMessage: "+ errorResponse.getMessage());
                        throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR, "Unable to update searchable fields");
                    }
                }
            }
        } catch (IOException e){
            log.error("Unable to update searchable fields in gimli service for sitekey:"+siteKey +"due to:"+e.getMessage());
            throw new FieldException(500,"Unable to update searchable fields");
        }

    }

    @Override
    public AttributesResponse getAttributes(String siteKey, PageRequest request) throws FieldException {
        try{
          Response<AttributesResponse> response =  service.getAttributes(siteKey,request).execute();
          if(!response.isSuccessful()){
              log.error("Error while fetching attributes from gimli service for siteKey: "+siteKey+
                      ", statusCode: " + response.code() + (isNull(response.errorBody())? "" : "reason: "+
                      response.errorBody().string()));
              throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
          }
          AttributesResponse attributesResponse = response.body();
          if(attributesResponse.getStatus().equals(GIMLI_FAILURE_STATUS)) {
              ErrorResponse errorResponse =  attributesResponse.getErrors().iterator().next();
              log.error("Error while fetching attributes from gimli service for siteKey:"+siteKey+", errorCode:"
                      + errorResponse.getCode() + " and errorMessage: "+ errorResponse.getMessage());
              throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
          }
          if(isNull(attributesResponse.getAttributes()))
              log.info("No data found while fetching attributes from gimli service for siteKey:"+siteKey);
          return attributesResponse;
        } catch (IOException e){
            log.error("Unable to fetch attributes from gimli service for sitekey:"+siteKey +"due to:"+e.getMessage());
            throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
        }
    }

    @Override
    public FieldServiceBaseResponse updateMapping(String siteKey,
                                                  List<FieldAliasMapping> request) throws FieldException {
        try{
            Response<FieldServiceBaseResponse> response =  service.updateMapping(siteKey,request).execute();
            if(!response.isSuccessful()){
                log.error("Error while fetching attributes from gimli service for siteKey: "+siteKey+
                        ", statusCode: " + response.code() + (isNull(response.errorBody())? "" : "reason: "+
                        response.errorBody().string()));
                throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
            }
            FieldServiceBaseResponse responseBody = response.body();
            if(responseBody.getStatus() != null && responseBody.getStatus().equals(GIMLI_FAILURE_STATUS)) {
                ErrorResponse errorResponse =  responseBody.getErrors().iterator().next();
                log.error("Error while fetching attributes from gimli service for siteKey:"+siteKey+", errorCode:"
                        + errorResponse.getCode() + " and errorMessage: "+ errorResponse.getMessage());
                throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
            }
            return responseBody;
        } catch (IOException e){
            log.error("Unable to fetch attributes from gimli service for sitekey:"+siteKey +"due to:"+e.getMessage());
            throw new FieldException(HttpConstants.StatusCode.INTERNAL_ERROR,"Unable to get attributes");
        }
    }

    @Override
    public void validateFieldNames(String siteKey,
                                   List<String> fieldNamesToBeValidated) throws FieldException {
        Set<String> fieldNames = getFieldNames(siteKey);
        for(String fieldName: fieldNamesToBeValidated)
            if(!fieldNames.contains(fieldName))
                throw new FieldException(400,"Undefined fieldName :"+ fieldName);
    }

    private Set<String> getFieldNames(String siteKey) throws FieldException {
        PageRequest request = new PageRequest();
        request.setPage(1);
        request.setCount(1);
            AttributesResponse attributesResponse = getAttributes(siteKey,request);
            Set<String> fieldNames = new HashSet<>(attributesResponse.getTotal());
            request.setCount(attributesResponse.getTotal());
            attributesResponse = getAttributes(siteKey,request);
            attributesResponse.getAttributes().forEach(field -> fieldNames.add(field.getFieldName()));
            return fieldNames;
    }
}
