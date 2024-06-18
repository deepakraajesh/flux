package com.unbxd.search;


import lombok.extern.log4j.Log4j2;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.rules.ExternalResource;
import ro.pippo.core.HttpConstants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Log4j2
public class MockHagridServer extends ExternalResource {

    private final MockWebServer server = new MockWebServer();

    private boolean isStarted;

    public MockHagridServer() throws MalformedURLException {
        startup();
    }

    @Override
    protected void before() throws Throwable {
        startup();
    }

    public String getHostName() {
        return server.getHostName();
    }

    public int getPort() {
        return server.getPort();
    }

    @Override
    protected void after() {
        shutdown();
    }

    static String readFile(String path)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    public void startup() throws MalformedURLException {
        if(isStarted)
            return;
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse()
                        .setBody("{}")
                        .setHeader(HttpConstants.Header.CONTENT_TYPE, HttpConstants.ContentType.APPLICATION_JSON);
            }
        };
        server.setDispatcher(dispatcher);
        try {
            server.start();
            log.info("Hagrid server is started at port " + getPort());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not start the mock webserver");
            throw new RuntimeException(e);
        }
        isStarted = Boolean.TRUE;
    }

    public void shutdown() {
        try {
            server.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}