package com.unbxd.skipper.site.controller;


import com.google.inject.Guice;
import com.unbxd.skipper.SkipperLauncher;
import com.unbxd.skipper.SkipperRule;
import com.unbxd.skipper.SkipperTest;
import com.unbxd.skipper.SkipperTestModule;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


public class SiteControllerTest extends SkipperTest {
    @Rule
    public SkipperRule pippoRule = new SkipperRule(
            Guice.createInjector(new SkipperTestModule()).getInstance(SkipperLauncher.class));
    @Test
    public void testGetDataCenterData() {
        Response response = given().cookie("_un_sso_uid", "abc").get("/skipper/datacenter");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("{\"data\":{\"regions\":[]}}", response.getBody().asString());
    }

    @Ignore
    @Test
    public void testGetSiteMeta() {
        Response response = given().cookie("_un_sso_uid", "abc").get("/skipper/meta");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("{\"data\":{\"environment\":[],\"vertical\":[],\"platform\":[],\"language\":[]}}",
                response.getBody().asString());
    }

    @Test
    public void testSetDataCenterData() {
        Response response = given().request().body("{\"data\":{\"regions\":[{\"name\":\"bangalore\"," +
                "\"lat_long\":\"1234355654,23544356\",\"id\":\"hello\"}]}}")
                .with().contentType(ContentType.JSON).when().post("/admin/site/datacenter");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("DataCenter data is updated successfully",response.getBody().asString());
    }

    @Test
    public void testSetVerticals() {
        Response response = given().request().body("[{\"id\":\"1\",\"name\":\"Electronics\"}]")
                .with().contentType(ContentType.JSON).when().post("/admin/site/meta/verticals");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("Verticals data is updated successfully",response.getBody().asString());
    }

    @Test
    public void testSetPlatforms() {
        Response response = given().request().body("[{\"id\":\"1\",\"name\":\"Fashion\"}," +
                "{\"id\":\"1\",\"name\":\"Electronics\"}]")
                .with().contentType(ContentType.JSON).when().post("/admin/site/meta/platforms");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("Platforms data is updated successfully",response.getBody().asString());
    }

    @Test
    public void testSetEnvironments() {
        Response response = given().request().body("[{\"id\":\"1\",\"name\":\"Fashion\"}," +
                "{\"id\":\"1\",\"name\":\"Electronics\"}]")
                .with().contentType(ContentType.JSON).when().post("/admin/site/meta/environments");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("Environments data is updated successfully",response.getBody().asString());
    }

    @Test
    public void testSetLanguages() {
        Response response = given().request().body("[{\"id\":\"1\",\"name\":\"Fashion\"}," +
                "{\"id\":\"2\",\"name\":\"Electronics\"}]")
                .with().contentType(ContentType.JSON).when().post("/admin/site/meta/languages");
        assertEquals(200,response.getStatusCode());
        assertEquals("application/json; charset=UTF-8",response.getContentType());
        assertEquals("Languages data is updated successfully",response.getBody().asString());
    }

}
