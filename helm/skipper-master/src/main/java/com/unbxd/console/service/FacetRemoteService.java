package com.unbxd.console.service;

import com.unbxd.console.exception.ConsoleOrchestrationServiceException;
import com.unbxd.console.model.*;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface FacetRemoteService {

    String FACET_AUTH_KEY = "facet.authKey";

    @Multipart
    @POST("/search/api/v1/sites/{sitekey}/global_facets/import")
    Call<Map<String, String>> uploadCsv(@Header("Authorization") String authToken,
                                        @Path("sitekey") String sitekey,
                                        @Query("product") String product,
                                        @Part MultipartBody.Part file)
            throws ConsoleOrchestrationServiceException;

    @POST("/api/v1/sites/{sitekey}/global_facets")
    Call<FacetResponse> updateGlobalFacets(@Header("Authorization") String authToken,
                                           @Header("Cookie") String cookie,
                                           @Path("sitekey") String sitekey,
                                           @Body ConsoleFacetFieldRequest facetFieldRequest);

    @PATCH("api/v1/sites/{sitekey}/site_facets/")
    Call<SingleFacetResponse> enableFacetInSiteRule(@Header("Authorization") String authToken,
                                                    @Header("Cookie") String cookie,
                                                    @Path("sitekey") String siteKet,
                                                    @Body ConsoleFacetFieldRequest consoleFacetFieldRequest);

    @PATCH("/api/v1/sites/{sitekey}/site_facets/update_multiple")
    Call<FacetResponse> updateSiteRuleFacets(@Header("Cookie") String cookie,
                                      @Path("sitekey") String sitekey,
                                      @Body ConsoleFacetFieldRequest facetFieldRequest);

    @PATCH("api/v1/sites/{sitekey}/site_facets/update_position")
    Call<FacetResponse> updateFacetPosition(@Header("Cookie") String cookie,
                                            @Path("sitekey") String sitekey,
                                            @Query("from_pos") String fromPos,
                                            @Query("to_pos") String toPos,
                                            @Query("product_type") String productType);

    @HTTP(method = "DELETE", path = "/api/v1/sites/{sitekey}/global_facets/delete_multiple", hasBody = true)
    Call<FacetResponse> deleteGlobalFacets(@Header("Cookie") String cookie,
                                    @Path("sitekey") String sitekey,
                                    @Body ConsoleFacetFieldRequest facetFieldRequest);

    @HTTP(method = "DELETE", path = "/api/v1/sites/{sitekey}/site_facets/delete_multiple", hasBody = true)
    Call<FacetResponse> deleteSiteRuleFacets(@Header("Authorization") String authToken,
                                             @Header("Cookie") String cookie,
                                             @Path("sitekey") String sitekey,
                                             @Body ConsoleFacetFieldRequest facetFieldRequest);

    @GET("api/v1/sites/{sitekey}/global_facets")
    Call<FacetResponse> fetchGlobalFacets(@Header("Cookie") String cookie,
                                                     @Path("sitekey") String sitekey,
                                                     @Query("page") String page,
                                                     @Query("per_page") String perPage);

    @GET("api/v1/sites/{sitekey}/site_facets")
    Call<FacetResponse> fetchSiteRuleFacets(@Header("Authorization") String authToken,
                                            @Header("Cookie") String cookie,
                                            @Path("sitekey") String sitekey,
                                            @Query("page") String page,
                                            @Query("sort") String sort,
                                            @Query("query") String query,
                                            @Query("per_page") String perPage,
                                            @Query("product_type") ProductType productType);

    @POST("{productType}/api/v1/campaigns/{sitekey}/publish_site_rule")
    Call<FacetResponse> publishSiteRule(@Header("Authorization") String authToken,
                                        @Header("Cookie") String cookie,
                                        @Path("sitekey") String siteKey,
                                        @Path("productType") ProductType productType);

    @GET("/api/v1/sites/{siteKey}/validate_site/")
    Call<Object> validateSite(@Header("Cookie") String cookie,
                              @Path("siteKey") String siteKey);

    @GET("/api/v1/sites/{siteKey}/products")
    Call<SiteProductsResponse> fetchSiteProducts(@Header("Cookie") String cookie,
                                                 @Path("siteKey") String siteKey);

    @GET("/api/v1/sites/id/configs")
    Call<FeatureWrapper> fetchFeature(@Header("Cookie") String cookie,
                                      @Query("site_key") String siteKey);

    @DELETE("/api/v1/sites/{siteKey}/delete")
    Call<Map<String, Object>> deleteSite(@Header("Cookie") String cookie,
                                         @Path("siteKey") String siteKey);

    @GET("search/sites/{siteId}/query-rules.json")
    Call<QueryRuleWrapper> getQueryRules(@Path("siteId") String siteId, @Header("Cookie") String cookie);
}
