package com.unbxd.skipper.site.controller;

import com.google.inject.Guice;
import com.unbxd.skipper.SkipperLauncher;
import com.unbxd.skipper.SkipperRule;
import com.unbxd.skipper.SkipperTest;
import com.unbxd.skipper.SkipperTestModule;
import io.restassured.response.Response;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RelevancyTest extends SkipperTest {

    private static final String SKIPPER_WORKFLOW_CREATION_RESPONSE = "{\"data\":{\"statusCode\":200,\"workflow_id\":\"ABC-1234\"}}";

    @Rule
    public SkipperRule pippoRule = new SkipperRule(
            Guice.createInjector(new SkipperTestModule()).getInstance(SkipperLauncher.class));

    @Test
    public void testWorkflowTrigger() {
//        Response response = given().cookie("_un_sso_uid", "abc").post("/skipper/site/sitekey/relevancy/trigger");
//        assertEquals(SKIPPER_WORKFLOW_CREATION_RESPONSE, response.getBody().asString());
//        assertEquals(200, response.getStatusCode());
    }

}
