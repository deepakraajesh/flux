package com.unbxd.pim.workflow.service;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface PimSearchApp {

    @PATCH("pim/register/")
    Call<JsonObject> registerPIM(@Header("Cookie") String cookie, @Body JsonObject registerPIMRequest);

    @POST("app/unbxd_pim_search_app/v1/install")
    Call<JsonObject> registerSearch(@Header("Cookie") String cookie, @Body JsonObject registerSearchRequest);

    @POST("app/api/v2/product/import/")
    Call<JsonObject> triggerFullUpload(@Body JsonObject req);

    @PUT("app/api/v2/group-by-parent")
    Call<JsonObject> setGroupByParent(@Body JsonObject request);
}
