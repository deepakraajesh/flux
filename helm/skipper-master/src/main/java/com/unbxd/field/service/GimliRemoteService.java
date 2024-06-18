package com.unbxd.field.service;

import com.unbxd.field.model.*;
import com.unbxd.skipper.relevancy.model.FieldAliasMapping;
import com.unbxd.skipper.relevancy.model.PageRequest;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface GimliRemoteService {
    @GET("api/getDimensionMap")
    Call<FieldMappingWrapper> getDimensionMap(@Query("iSiteName") String siteKey);

    @POST("api/saveDimensionMap/{siteKey}")
    Call<FieldServiceCommonResponse> saveDimensionMap(@Path("siteKey") String siteKey,
                                                      @Body Map<String, String> mapping);

    @GET("api/site/{siteKey}")
    Call<SiteKeyCred> getSiteDetails(@Path("siteKey") String siteKey);

    @GET("api/{siteKey}/field")
    Call<List<Fields>> getFields(@Path("siteKey") String siteKey, @Query("fieldtype.filter") String fieldType);

    @POST("api/ruleset/site/{siteKey}/searchable")
    Call<SearchableFieldsResponse> getSearchableFields(@Path("siteKey") String siteKey, @Body PageRequest request);

    @DELETE("api/site/{siteKey}")
    Call<Map<String, Object>> deletSite(@Header("Authorization") String auth, @Path("siteKey") String siteKey);

    @PUT("api/ruleset/site/{siteKey}/searchable")
    Call<FieldServiceBaseResponse> updateSearchableFields(@Path("siteKey") String siteKey, @Body List<FSSearchableField> request);

    @POST("api/{siteKey}/fields")
    Call<AttributesResponse> getAttributes(@Path("siteKey") String siteKey, @Body PageRequest request);

    @PUT("api/{siteKey}/fields")
    Call<FieldServiceBaseResponse> updateMapping(@Path("siteKey") String siteKey, @Body List<FieldAliasMapping> request);
}
