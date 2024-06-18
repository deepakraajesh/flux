package com.unbxd.event.controller;

import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.cbo.response.Response;
import com.unbxd.event.exception.ReportException;
import com.unbxd.event.model.ReportRequest;
import com.unbxd.event.model.ReportResponse;
import com.unbxd.event.service.ToucanRemoteService;
import com.unbxd.toucan.eventfactory.Event;
import com.unbxd.toucan.eventfactory.EventHistory;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.Produces;
import ro.pippo.controller.extractor.Param;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static java.io.File.createTempFile;


@Log4j2
public class ReportController extends Controller {

    private final String NEWLINE = "\n";
    private ToucanRemoteService toucanService;

    @Inject
    public ReportController(ToucanRemoteService toucanService) {
        this.toucanService = toucanService;
    }

    @Produces(Produces.JSON)
    @GET("/skipper/site/{siteKey}/request/{requestId}")
    public Response<ReportResponse> getReport(@Param int page,
                                              @Param int count,
                                              @Param String key,
                                              @Param String siteKey,
                                              @Param String requestId,
                                              @Param String operationName) {
        Response.Builder<ReportResponse> builder = new Response.Builder<>();

        try {
            ReportResponse response = ReportResponse.getInstance(page,
                    getEventHistory(page, count, key, siteKey, requestId, operationName));
            builder.withData(response);

        } catch (Exception e) {
            log.error("Error while trying to fetch event from toucan: " + e.getMessage());
            builder.withError(new Error.Builder().withCode(500).withStatus(e.getMessage()).build());
        }
        return builder.build();
    }

    @GET("/skipper/site/{siteKey}/request/{requestId}/bulk")
    public File getBulkReport(@Param String key,
                              @Param String siteKey,
                              @Param String requestId,
                              @Param String operationName) throws ReportException {
        return getBulkHistory(key, siteKey, requestId, operationName);
    }

    private EventHistory getEventHistory(int page,
                                         int count,
                                         String key,
                                         String siteKey,
                                         String requestId,
                                         String operationName) throws ReportException {
        try {
            ReportRequest request = ReportRequest.getInstance(page, count, key,
                    siteKey, requestId, operationName);
            Call<EventHistory> reportCall = toucanService.getReport(request);
            retrofit2.Response<EventHistory> response = reportCall.execute();

            if (response.isSuccessful()) {
                return response.body();
            } else {
                log.error("Exception while fetching events for request id:" + requestId
                        + " with message: " + response.errorBody().string());
                throw new ReportException("Unable to fetch events for siteKey: "
                        + siteKey + " and request id" + requestId);
            }
        } catch (IOException e) {
            log.error("Exception while fetching events for request id:"
                    + requestId + " with message: " + e.getMessage());
            throw new ReportException("Unable to fetch events for siteKey: "
                    + siteKey + " and request id" + requestId);
        }
    }

    private File getBulkHistory(String key,
                                String siteKey,
                                String requestId,
                                String operationName) throws ReportException {
        try {
            File reportFile = createTempFile("report", ".log");
            EventHistory eventHistory = getEventHistory(0, 10000,
                    key, siteKey, requestId, operationName);

            FileWriter fileWriter = new FileWriter(reportFile);
            for (Event event: eventHistory.getEventHistory()) {
                fileWriter.write(event.getTags().get("entry_tag")
                        .toString() + " - ");
                fileWriter.write(event.getMessage());
                fileWriter.write(NEWLINE);
            }
            fileWriter.close();
            return reportFile;
        } catch (IOException e) {
            String error = "Exception while bulk downloading report for siteKey["
                    + siteKey + "] and requestId[" + requestId + "] : "
                    + e.getMessage();
            log.error(error);
            throw new ReportException(error);
        }
    }
}
