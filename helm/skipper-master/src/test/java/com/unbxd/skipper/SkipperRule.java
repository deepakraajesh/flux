package com.unbxd.skipper;

import io.restassured.RestAssured;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.Pippo;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.test.AvailablePortFinder;

/**
 * Start Pippo prior to test execution and stop Pippo after the tests have completed.
 *
 * @author Decebal Suiu
 */
public class SkipperRule implements TestRule {

    private final Pippo pippo;
    /**
     * This constructor dynamically allocates a free port.
     */
    public SkipperRule(Application application) {
        this(application, AvailablePortFinder.findAvailablePort());
    }

    public SkipperRule(Application application, Integer port) {
        this(new Pippo(application), port);
    }

    public SkipperRule(Pippo pippo) {
        this(pippo, AvailablePortFinder.findAvailablePort());
    }

    public SkipperRule(Pippo pippo, Integer port) {
        this.pippo = pippo;
        pippo.getServer().setPort(port);
    }

    /**
     * Useful in case that you want to mock some services via setters.
     */
    public Application getApplication() {
        return pippo.getApplication();
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        // decorate statement
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                startPippo(pippo);

                try {
                    statement.evaluate();
                } finally {
                    stopPippo(pippo);
                }
            }

        };
    }

    protected void startPippo(Pippo pippo) {
        pippo.start();
        initRestAssured();
    }

    protected void stopPippo(Pippo pippo) {
        pippo.stop();
    }

    protected void initRestAssured() {
        // port
        RestAssured.port = pippo.getServer().getPort();

        ObjectMapperConfig config = new ObjectMapperConfig(new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                ContentTypeEngine engine = pippo.getApplication().getContentTypeEngine(context.getContentType());
                if (engine == null) {
                    throw new PippoRuntimeException("No ContentTypeEngine registered for {}", context.getContentType());
                }

                return engine.fromString(context.getDataToDeserialize().asString(), ObjectDeserializationContext.class);
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                ContentTypeEngine engine = pippo.getApplication().getContentTypeEngine(context.getContentType());
                if (engine == null) {
                    throw new PippoRuntimeException("No ContentTypeEngine registered for {}", context.getContentType());
                }

                return engine.toString(context.getObjectToSerialize());
            }
        });
        RestAssured.config().objectMapperConfig(config);
    }

}
