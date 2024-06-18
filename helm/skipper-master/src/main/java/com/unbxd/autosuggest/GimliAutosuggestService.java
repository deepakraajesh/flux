package com.unbxd.autosuggest;

import com.unbxd.autosuggest.exception.AutosuggestException;
import com.unbxd.autosuggest.model.*;
import com.unbxd.autosuggest.model.InFieldsResponse;
import com.unbxd.autosuggest.model.KeywordSuggestionsResponse;
import com.unbxd.skipper.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class GimliAutosuggestService implements AutosuggestService {

    private static final String KEYWORD_SUGGESTION = "keywordSuggestion";
    private static final String IN_FIELD = "inField";
    private static final String PP_SEARCHABLE_FIELD = "PopularProductSearchableField";
    private static final String POPULAR_PRODUCT_FIELD = "PopularProductField";
    private static final String SAVE = "saving";
    private static final String FETCH = "fetching";
    private static final String DELETE = "deleting";
    private static final String SITE_DETAILS = "siteDetails";
    private static final String TOP_QUERIES_COUNT = "topQueriesCount";
    private static final String INDEXED = "INDEXED";
    private AutosuggestConfService fieldService;

    private AutosuggestService autosuggestService;

    private interface AutosuggestConfService {
        @POST("api/{siteKey}/autosuggest/keyword")
        Call<GimliBaseResponse> setKeywordSuggestion(@Path("siteKey") String siteKey,
                                                     @Body KeywordSuggestionRequest req);
        @POST("api/{siteKey}/autosuggest/inFields")
        Call<GimliBaseResponse> setInfield(@Path("siteKey") String siteKey,
                                           @Body InFieldsRequest request);
        @POST("api/{siteKey}/autosuggest/searchable")
        Call<PPSearchableFieldsResponse> setPPSearchableFields(@Path("siteKey") String siteKey,
                                                               @Body PPSearchableFieldRequest request);

        @POST("api/{siteKey}/autosuggest/popularProducts")
        Call<GimliBaseResponse> setPopularProductField(@Path("siteKey") String siteKey ,
                                                       @Body PPFieldRequest request);

        @GET("api/{siteKey}/autosuggest/popularProducts")
        Call<PPFieldsResponse> getPopularProductFields(@Path("siteKey") String siteKey);

        @GET("api/{siteKey}/autosuggest/keyword")
        Call<KeywordSuggestionsResponse> getKeywordSuggestions(@Path("siteKey") String siteKey);

        @GET("api/{siteKey}/autosuggest/inFields")
        Call<InFieldsResponse> getInFields(@Path("siteKey") String siteKey);

        @GET("api/{siteKey}/autosuggest/searchable")
        Call<PPSearchableFieldsResponse> getPPSearchableFields(@Path("siteKey") String siteKey);

        @DELETE("api/{siteKey}/autosuggest/keyword/{name}")
        Call<GimliBaseResponse> deleteKeywordSuggestion(@Path("siteKey") String siteKey,
                                                        @Path("name") String name);

        @DELETE("api/{siteKey}/autosuggest/inFields/{fieldName}")
        Call<GimliBaseResponse> deleteInField(@Path("siteKey") String siteKey,
                                              @Path("fieldName") String name);

        @DELETE("api/{siteKey}/autosuggest/popularProducts/{fieldName}")
        Call<GimliBaseResponse> deletePopularProductField(@Path("siteKey") String siteKey,
                                                      @Path("fieldName") String fieldName);

        @DELETE("api/{siteKey}/autosuggest/searchable/{fieldName}")
        Call<GimliBaseResponse> deletePPSearchableField(@Path("siteKey") String siteKey,
                                                          @Path("fieldName") String fieldName);

        @GET("api/site/{siteKey}")
        Call<SiteDetails> getSiteDetails(@Path("siteKey") String siteKey);

        @PUT("api/{siteKey}/autocomplete/topsearch/count/{topQueriesCount}")
        Call<GimliBaseResponse> setTopQueriesCount(@Path("siteKey") String siteKey,
                                                   @Path("topQueriesCount") Integer topQueriesCount);
    }

    private interface AutosuggestService {
        @GET("{siteKey}/autosuggest/status")
        Call<List<AutosuggestIndexingStatus>> suggestionIndexingstatus(@Path("siteKey") String siteKey);

        @GET("{siteKey}/autosuggest/{feedId}/status")
        Call<AutosuggestIndexingStatus> suggestionIndexingstatus(@Path("siteKey") String siteKey,
                                                                       @Path("feedId") String feedId);

        @GET("{siteKey}/autosuggest/status")
        Call<List<AutosuggestIndexingStatus>> suggestionIndexingStatus(@Path("siteKey") String siteKey,
                                                                       @Query("count") int count);

        @POST("{siteKey}/autosuggest?skipHerusticValidation=true")
        Call<AutosuggestIndexResponse> indexSuggestion(@Path("siteKey") String siteKey);

        @POST("{siteKey}/autosuggest?skipHerusticValidation=true")
        Call<AutosuggestIndexResponse> indexSuggestion(@Path("siteKey") String siteKey,
                                                       @Query("autosuggestThreshold") String autosuggestThreshold);
    }



    public GimliAutosuggestService(String fieldServiceBaseURL, String autosuggestBaseURL) {
        OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

        fieldService = new Retrofit.Builder()
                .baseUrl(fieldServiceBaseURL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build().create(GimliAutosuggestService.AutosuggestConfService.class);

        autosuggestService = new Retrofit.Builder()
                .baseUrl(autosuggestBaseURL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build().create(GimliAutosuggestService.AutosuggestService.class);
    }

    @Override
    public void setKeywordSuggestion(String siteKey,
                                     KeywordSuggestion keywordSuggestion,
                                     boolean ignoreDuplicateFieldError)
            throws AutosuggestException {
            KeywordSuggestionRequest req = new KeywordSuggestionRequest();
            req.setKeywordSuggestion(keywordSuggestion);
            execute(fieldService.setKeywordSuggestion(siteKey, req), siteKey, KEYWORD_SUGGESTION, SAVE,
                    ignoreDuplicateFieldError);
    }
    
    protected GimliBaseResponse execute(Call<?> caller,
                                        String siteKey,
                                        String name,
                                        String action,
                                        boolean ignoreDuplicateFieldError) throws AutosuggestException {
        try {
            Response<?> response = caller.execute();
            if (response.isSuccessful()) {
                GimliBaseResponse status = (GimliBaseResponse)response.body();
                if(status != null && status.getErrors() == null) {
                    //All Good
                    return status;
                } else {
                    int statusCode = 500;
                    log.error("Error while making request " + status.getErrors() + " for siteKey:" + siteKey);
                    if(GimliBaseResponse.GIMLI_DUPLICATE_ADDITION_ERROR_CODE.equals(status.getErrorCode())){
                        if(ignoreDuplicateFieldError) return new GimliBaseResponse();
                        statusCode = 400;
                    }
                    throw new AutosuggestException(statusCode, ErrorCode.DuplicateSuggestionsAddition.getCode(),
                            status.getErrors());
                }
            } else {
                log.error("Error while making request for  " + name + " siteKey:" + siteKey +
                    " threw statusCode:" +  response.code() + " reason:" + response.message());
                throw new AutosuggestException(500,"Error while " + action + " " + name);
            }
        } catch (IOException e) {
            log.error("Error while "+ action + " " + name +" for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new AutosuggestException(500,"Error while "+ action + " " + name);
        }
    }

    @Override
    public void setInfield(String siteKey,
                           String fieldName,
                           boolean ignoreDuplicateFieldError) throws AutosuggestException {
        InFieldsRequest request = new InFieldsRequest();
        request.setFieldName(fieldName);
        execute(fieldService.setInfield(siteKey,request), siteKey, IN_FIELD, SAVE, ignoreDuplicateFieldError);
    }

    @Override
    public void setPPSearchableField(String siteKey,
                                     String fieldName,
                                     boolean ignoreDuplicateFieldError) throws AutosuggestException {
        PPSearchableFieldRequest request = new PPSearchableFieldRequest();
        request.setFieldName(fieldName);
        execute(fieldService.setPPSearchableFields(siteKey,request), siteKey, PP_SEARCHABLE_FIELD, SAVE,
                ignoreDuplicateFieldError);
    }

    @Override
    public void setPopularProductField(String siteKey,
                                       PopularProductField popularProductField,
                                       boolean ignoreDuplicateFieldError) throws AutosuggestException {
        PPFieldRequest request = new PPFieldRequest();
        request.setPopularProductField(popularProductField);
        execute(fieldService.setPopularProductField(siteKey,request), siteKey, POPULAR_PRODUCT_FIELD, SAVE,
                ignoreDuplicateFieldError);
    }

    @Override
    public  List<KeywordSuggestion> getKeywordSuggestions(String siteKey) throws AutosuggestException {
        KeywordSuggestionsResponse response =  (KeywordSuggestionsResponse) execute(
                fieldService.getKeywordSuggestions(siteKey), siteKey, KEYWORD_SUGGESTION, FETCH, false);
        return response.getKeywordSuggestions();
    }

    @Override
    public List<InField> getInFields(String siteKey) throws AutosuggestException {
        InFieldsResponse response = (InFieldsResponse) execute(
                fieldService.getInFields(siteKey), siteKey, IN_FIELD, FETCH, false);
        return response.getInFields();
    }

    @Override
    public  List<PopularProductField> getPopularProducts(String siteKey) throws AutosuggestException {
        PPFieldsResponse response =  (PPFieldsResponse) execute(
                fieldService.getPopularProductFields(siteKey), siteKey, POPULAR_PRODUCT_FIELD, FETCH, false);
        return response.getPopularProductFields();
    }

    @Override
    public List<String> getPPSearchableFields(String siteKey) throws AutosuggestException {
        PPSearchableFieldsResponse response =  (PPSearchableFieldsResponse) execute(
                fieldService.getPPSearchableFields(siteKey), siteKey, PP_SEARCHABLE_FIELD, FETCH, false);
        return response.getPopularProductSearchableFields();
    }

    @Override
    public void deleteKeywordSuggestion(String siteKey,
                                        String suggestionName) throws AutosuggestException {
        execute(fieldService.deleteKeywordSuggestion(siteKey,suggestionName), siteKey, KEYWORD_SUGGESTION, DELETE, false);
    }

    @Override
    public void deleteInField(String siteKey, String fieldName) throws AutosuggestException {
        execute(fieldService.deleteInField(siteKey,fieldName), siteKey, IN_FIELD, DELETE, false);
    }

    @Override
    public void deletePopularProductField(String siteKey , String fieldName) throws  AutosuggestException {
        execute(fieldService.deletePopularProductField(siteKey,fieldName), siteKey, POPULAR_PRODUCT_FIELD, DELETE, false);
    }

    @Override
    public void deletePPSearchableField(String siteKey , String fieldName) throws AutosuggestException {
        execute(fieldService.deletePPSearchableField(siteKey,fieldName), siteKey, PP_SEARCHABLE_FIELD, DELETE, false);
    }

    @Override
    public Integer getTopQueriesCount(String siteKey) throws AutosuggestException {
        SiteDetails response = (SiteDetails)  execute(fieldService.getSiteDetails(siteKey), siteKey, SITE_DETAILS, FETCH, false);
        return isNull(response.getAutosuggestConf()) ? null : response.getAutosuggestConf().getTopQueriesCount() ;
    }

    @Override
    public void setTopQueriesCount(String siteKey, Integer topQueriesCount) throws AutosuggestException {
        execute(fieldService.setTopQueriesCount(siteKey, topQueriesCount), siteKey, TOP_QUERIES_COUNT , SAVE, false);
    }

    @Override
    public AutosuggestIndexResponse indexSuggestions(String siteKey) throws  AutosuggestException {
        return indexSuggestions(siteKey, null);
    }

    @Override
    public AutosuggestIndexResponse indexSuggestions(String siteKey,
                                                     String autosuggestThreshold) throws AutosuggestException {
        try {
            Response<AutosuggestIndexResponse> response;
            if(nonNull(autosuggestThreshold) && !autosuggestThreshold.isEmpty())
                response = autosuggestService.indexSuggestion(siteKey, autosuggestThreshold).execute();
            else
                response = autosuggestService.indexSuggestion(siteKey).execute();
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    if(200 == response.body().getCode())
                        return response.body();
                    else
                        throw new AutosuggestException(response.body().getCode(),"Error while indexing suggestions");
                }  else {
                    log.error("Empty response from Feed api for siteKey" +siteKey +
                            " , statusCode:" +  response.code() + " reason:" + response.message());
                    throw new AutosuggestException(500, "Error while indexing suggestions");
                }
            } else {
                log.error("Error while indexing suggestions for  siteKey:" + siteKey +
                        " , threw statusCode:" +  response.code() + " reason:" + response.message());
                throw new AutosuggestException(500, "Error while indexing suggestions");
            }
        } catch (IOException e) {
            log.error("Error while indexing suggestions for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new AutosuggestException(500,"Error while indexing suggestions :" + e.getMessage());
        }
    }

    @Override
    public AutosuggestIndexingStatus getIndexingStatus(String siteKey) throws  AutosuggestException{
        try {
            Response<List<AutosuggestIndexingStatus>> response = autosuggestService.suggestionIndexingstatus(siteKey).execute();
            if (response.isSuccessful()) {
                List<AutosuggestIndexingStatus> statuses = response.body();
                if(statuses != null && statuses.size() > 0)
                    return statuses.get(0);
            } else {
                log.error("Error while fetching suggestionIndexingStatus for  siteKey:" + siteKey +
                        " , threw statusCode:" +  response.code() + " reason:" + response.message());
                throw new AutosuggestException(500,"Error while fetching suggestionIndexingStatus ");
            }
        } catch (IOException e) {
            log.error("Error while fetching suggestionIndexingStatus for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new AutosuggestException(500,"Error while fetch suggestionIndexingStatus :" + e.getMessage());
        }
        return null;
    }

    @Override
    public AutosuggestIndexingStatus getIndexingStatus(String siteKey,String feedId) throws  AutosuggestException {
        try {
            Response<AutosuggestIndexingStatus> response = autosuggestService.suggestionIndexingstatus(siteKey,feedId)
                    .execute();
            if (response.isSuccessful()) {
               return response.body();
            } else {
                log.error("Error while fetching suggestionIndexingStatus for  siteKey:" + siteKey +
                        " , threw statusCode:" +  response.code() + " reason:" + response.message());
                throw new AutosuggestException(500,"Error while fetching suggestionIndexingStatus ");
            }
        } catch (IOException e) {
            log.error("Error while fetching suggestionIndexingStatus for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new AutosuggestException(500,"Error while fetch suggestionIndexingStatus :" + e.getMessage());
        }
    }

    @Override
    public Long fetchLatestIndexingTime(String siteKey) throws  AutosuggestException {
        try {
            Response<List<AutosuggestIndexingStatus>> response = autosuggestService.suggestionIndexingStatus(siteKey,
                    10).execute();
            if (response.isSuccessful()) {
                List<AutosuggestIndexingStatus> statuses = response.body();
                if(nonNull(statuses)) {
                    for(AutosuggestIndexingStatus status : statuses) {
                        if(status.getStatus().equals(INDEXED))
                            return status.getTotalTime();
                    }
                }
                return null;
            } else {
                log.error("Error while fetching Previous suggestion indexing time for  siteKey:" + siteKey +
                        " , threw statusCode:" +  response.code() + " reason:" + response.message());
                throw new AutosuggestException(500,"Error while fetching Previous suggestion indexing time  ");
            }
        } catch (IOException e) {
            log.error("Error while fetching Previous suggestion indexing time  for siteKey:" + siteKey +
                    " reason:" +  e.getMessage());
            throw new AutosuggestException(500,"Error while fetch Previous suggestion indexing time :" + e.getMessage());
        }
    }

}
