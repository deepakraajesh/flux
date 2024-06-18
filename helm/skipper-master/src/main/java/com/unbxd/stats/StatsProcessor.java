package com.unbxd.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import com.unbxd.config.Config;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Singleton
public class StatsProcessor {

    private StatsDClient statsd;
    private static final String STATSD_PREFIX = "skipper";
    private static final String STATSD_HOST = "statsd-host";
    private static final String STATSD_PORT = "statsd-port";

    @Inject
    public StatsProcessor(Config config) {
        String hostName = config.getProperty(STATSD_HOST, "localhost");
        int port = Integer.parseInt(config.getProperty(STATSD_PORT, "8125"));
        try {
            statsd = new NonBlockingStatsDClient(STATSD_PREFIX,
                    hostName, port, "tag:" + STATSD_PREFIX);
        } catch (StatsDClientException e) {
            log.error("Error while initializing the statsd collector: ", e);
        }
    }

    public void logExecutionTime(String label, String siteKey, long timestamp) {
        if(statsd == null) { return; }
        statsd.time(label, timestamp, siteKey);
    }
}
