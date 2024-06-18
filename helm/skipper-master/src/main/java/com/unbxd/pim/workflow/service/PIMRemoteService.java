package com.unbxd.pim.workflow.service;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface PIMRemoteService {

    @POST("/pim-rails/api/v1/createAccount")
    Call<JsonObject> getOrganisation(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                     @Body JsonObject req);

    @PUT("/api/v1/{orgId}/configs/data/reset")
    Call<JsonObject> resetOrg(@Header("Cookie") String cookie, @Path("orgId") String orgId, @Body JsonObject req);

    @DELETE("/paprika/api/v1/orgs/{orgId}")
    Call<JsonObject> deleteOrg(@Header("Cookie") String cookie, @Header("Content-Type") String contentType,
                               @Path("orgId") String orgId);

    @POST("/api/v1/{orgId}/properties")
    Call<JsonObject> addProperty(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                 @Path("orgId") String orgId, @Body JsonObject propertyRequest);

    @POST("/paprika/api/v3/{orgId}/register")
    Call<JsonObject> generateAPIKey(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                    @Path("orgId") String orgId, @Body JsonObject keyGenRequest);

    @PUT("/api/v2/{orgId}/workflows/status")
    Call<JsonObject> startWorkflow(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                   @Path("orgId") String orgId, @Body JsonObject workflowRequest);

    @POST("/api/v2/{orgId}/workflows")
    Call<JsonObject> createWorkflow(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                    @Path("orgId") String orgId, @Body JsonObject workflowRequest);

    @PATCH("/api/v2/{orgId}/workflows/{workflowId}")
    Call<JsonObject> updateWorkflow(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                    @Path("orgId") String orgId, @Path("workflowId") String workflowId,
                                    @Body JsonObject updateRequest);

    @POST("/api/v2/{orgId}/workflows/{workflowId}/nodes/validate")
    Call<JsonObject> addWorkflowNode(@Header("Authorization") String authToken, @Header("Cookie") String cookie,
                                     @Path("orgId") String orgId, @Path("workflowId") String workflowId,
                                     @Body JsonObject node);

    @GET("/api/v1/{orgId}/networks/adapters/{adapterId}")
    Call<JsonObject> getAdapterProperties(@Header("Cookie") String cookie, @Path("orgId") String orgId,
                                          @Path("adapterId") String adapterId);

    @PATCH("/api/v2/{orgId}/networks/adapters/{adapterId}")
    Call<JsonObject> saveAdapterProperties(@Header("Cookie") String cookie, @Path("orgId") String orgId,
                                           @Path("adapterId") String adapterId, @Body JsonObject mappingRequest);

}
