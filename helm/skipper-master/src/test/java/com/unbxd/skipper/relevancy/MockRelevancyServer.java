package com.unbxd.skipper.relevancy;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.rules.ExternalResource;

import java.io.IOException;

public class MockRelevancyServer extends ExternalResource {

    private boolean isStarted;
    private final MockWebServer server;
    private final String WORKFLOW_TRIGGER_URL = "/v1.0/relevancy/sites/sitekey/workflows";
    private static final String WORKFLOW_CREATION_RESPONSE = "{\n" + "  \"workflow_id\" : \"ABC-1234\"\n" + "}\n";

    public MockRelevancyServer() {
        this.server = new MockWebServer();
        this.server.setDispatcher(getDispatcher());

        start();
    }

    private Dispatcher getDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (WORKFLOW_TRIGGER_URL.equals(request.getPath())) {
                    return new MockResponse().setBody(WORKFLOW_CREATION_RESPONSE).setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
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
