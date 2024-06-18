package com.unbxd.skipper.autosuggest.service;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface HagridRemoteService {
    @POST("/v1.0/config/sites/{siteKey}")
    Call<JsonObject> setConfig(@Path("siteKey") String siteKey,
                               @Header("Content-Type") String contentType,
                               @Body JsonObject request);

    @DELETE("/api/v1/site/{siteKey}")
    Call<JsonObject> deleteSite(@Header("Secret") String secretKey,
                                @Header("Authorization") String authorization,
                                @Path("siteKey") String siteKey);

}
