package com.unbxd.console.service;

import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;


public interface ConsoleRemoteService {

    @POST("createSite")
    Call<JsonObject> createSite(@Header("Authorization") String authToken,
                                @Header("Content-Type") String contentType,
                                @Body JsonObject consoleRequest);
    @GET("sites")
    Call<JsonObject> getSites(@Query("email") String email ,
                                 @Query("regions") String regions);

    @GET("synonyms/csv_download?site_id=320")
    Call<ResponseBody> getFrontEndSynonyms(@Header("Cookie") String cookie, @Query("site_id") String siteId);

}
