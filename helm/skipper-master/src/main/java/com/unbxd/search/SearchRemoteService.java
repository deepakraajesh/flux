package com.unbxd.search;

import com.unbxd.search.model.AutosuggestResponse;
import com.unbxd.search.model.FacetDetailResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;


public interface SearchRemoteService {

    @GET("{apiKey}/{siteKey}/search?q=*&omitmetadata=true&promotion=false&rows=0&analytics=false&multiselect=false" +
            "&analyzer=false&enablePopularity=false&enableTrendingProducts=false&version=V2")
    Call<FacetDetailResponse> getFacetDetails(@Path("apiKey") String apiKey ,
                                              @Path("siteKey") String siteKey,
                                              @QueryMap Map<String,String> facetQueries);


    @GET("{apiKey}/{siteKey}/autosuggest?q=*&omitmetadata=true&promotion=false&filter=doctype:POPULAR_PRODUCTS")
    Call<AutosuggestResponse> getPopularProductSuggestion(@Path("apiKey") String apiKey ,
                                                          @Path("siteKey") String siteKey,
                                                          @Query("popularProducts.filter") String filter);

    @GET("{apiKey}/{siteKey}/autosuggest?q=*&omitmetadata=true&promotion=false&filter=doctype:POPULAR_PRODUCTS")
    Call<AutosuggestResponse> getPopularProductSuggestion(@Path("apiKey") String apiKey ,
                                                          @Path("siteKey") String siteKey);

    @GET("{apiKey}/{siteKey}/search?q=*&omitmetadata=true&promotion=false&analytics=false" +
            "&analyzer=false&enablePopularity=false&enableTrendingProducts=false")
    Call<String> getProducts(@Path("siteKey") String siteKey, @Path("apiKey") String apiKey,
                             @QueryMap Map<String,String> filterQueries);
}
