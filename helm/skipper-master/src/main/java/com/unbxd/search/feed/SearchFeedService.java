package com.unbxd.search.feed;

import com.unbxd.search.feed.model.FeedFileStatus;
import com.unbxd.search.feed.model.FeedStatus;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SearchFeedService {
    @GET("v2.0/sites/{siteKey}/status")
    Call<List<FeedStatus>> feedStatus(@Header("Authorization") String authorization,
                                      @Path("siteKey") String siteKey,
                                      @Query("count") Integer count,
                                      @Query("type") String type);

    @GET("api/{siteKey}/catalog/status")
    Call<List<FeedStatus>> feedStatus(@Path("siteKey") String siteKey);

    @GET("api/{siteKey}/catalog/{feedId}/status")
    Call<FeedStatus> feedStatus(@Path("siteKey") String siteKey, @Path("feedId") String feedId);

    @GET("api/{siteKey}/catalog/status")
    Call<List<FeedStatus>> feedStatus(@Path("siteKey") String siteKey, @Query("count") int count);

    @GET("v2.0/sites/{siteKey}/status/{feedId}")
    Call<FeedStatus> feedStatus(@Header("Authorization") String authorization,
                                @Path("siteKey") String siteKey,
                                @Path("feedId") String feedId,
                                @Query("count") Integer count,
                                @Query("type") String type);

    @GET("v2.0/sites/{siteKey}/file")
    Call<List<FeedFileStatus>> feedStatus(@Header("Authorization") String authorization,
                                          @Path("siteKey") String siteKey,
                                          @Query("count") Integer count);

    @Multipart
    @POST("v2.0/sites/{siteKey}/file")
    Call<FeedStatus> feedIndex(@Header("Authorization") String authorization,
                               @Path("siteKey") String siteKey,
                               @Part("description") RequestBody description,
                               @Part MultipartBody.Part file);

}

