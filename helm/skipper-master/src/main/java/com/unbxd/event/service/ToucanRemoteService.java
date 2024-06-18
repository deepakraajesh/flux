package com.unbxd.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbxd.event.model.ReportRequest;
import com.unbxd.toucan.eventfactory.EventHistory;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ToucanRemoteService {

    @POST("/toucan/index/event_index/events")
    Call<EventHistory> getReport(@Body ReportRequest request);

    @POST("/toucan/index/event_index/events/file")
    Call<ResponseBody> bulkDownloadReport(@Body ReportRequest request);
}
