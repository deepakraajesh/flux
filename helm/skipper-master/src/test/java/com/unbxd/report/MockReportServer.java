package com.unbxd.report;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.rules.ExternalResource;
import ro.pippo.core.HttpConstants;

import java.io.IOException;

public class MockReportServer extends ExternalResource {

    private boolean isStarted;
    private final MockWebServer server;

    public MockReportServer() {
        this.server = new MockWebServer();
        this.server.setDispatcher(getDispatcher());

        start();
    }

    private Dispatcher getDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setBody("{}")
                        .setHeader(HttpConstants.Header.CONTENT_TYPE,
                                HttpConstants.ContentType.APPLICATION_JSON);
            }
        };
    }

    public void start() {
        if(!isStarted) {
            try {
                this.server.start();
                isStarted = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        try {
            this.server.shutdown();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void before() { start(); }

    @Override
    protected void after() { shutdown(); }

    public int getPort() { return this.server.getPort(); }

    public String getHost() { return this.server.getHostName(); }

}
