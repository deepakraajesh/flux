package com.unbxd.mongo;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import lombok.extern.log4j.Log4j2;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by antz on 30/01/18.
 */
@Log4j2
public class MockMongoServer extends ExternalResource {

    private int port;

    private boolean isStarted;

    public MockMongoServer() throws IOException {
        start();
    }

    MongodExecutable mongodExecutable = null;

    @Override
    public void before() throws IOException {
        start();
    }

    @Override
    public void after() {
        shutdown();
    }

    public int getPort() {
        return port;
    }

    public void shutdown() {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }

    public void start() throws IOException {
        if(isStarted)
            return;
        MongodStarter starter = MongodStarter.getDefaultInstance();

        String bindIp = "localhost";
        port = getFreePort();
        ImmutableMongodConfig mongodConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .build();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
        log.info("Mongo started with port number " + getPort());
        isStarted = Boolean.TRUE;
    }

    protected int getFreePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        s.close();
        return port;
    }
}

