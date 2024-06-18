package com.unbxd.skipper.relevancy.service;

import com.unbxd.skipper.relevancy.model.RelevancyRequest;
import com.unbxd.skipper.relevancy.model.RelevancyResponse;
import com.unbxd.skipper.relevancy.model.WorkflowStatus;
import retrofit2.Call;
import retrofit2.http.*;

public interface RelevancyRemoteService {

    String APPLICATION_JSON_HEADER = "application/json";

    @POST("/wo/argo/site/{sitekey}/submit")
    Call<RelevancyResponse> triggerRelevancyJob(@Header("Content-Type") String contentTypeHeader,
                                                @Path("sitekey") String siteKey, @Body RelevancyRequest relevancyRequest);

    @GET("/wo/argo/site/{sitekey}/workflow/{workflowId}/status")
    Call<WorkflowStatus> getWorkflowStatus(@Path("sitekey") String sitekey, @Path("workflowId") String workflowId);
}
