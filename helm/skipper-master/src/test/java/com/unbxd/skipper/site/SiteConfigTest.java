package com.unbxd.skipper.site;

import com.google.inject.Guice;
import com.unbxd.skipper.SkipperLauncher;
import com.unbxd.skipper.SkipperRule;
import com.unbxd.skipper.SkipperTest;
import com.unbxd.skipper.SkipperTestModule;
import io.restassured.response.Response;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SiteConfigTest extends SkipperTest {

    @Rule
    public SkipperRule pippoRule = new SkipperRule(
            Guice.createInjector(new SkipperTestModule()).getInstance(SkipperLauncher.class));

    @Test
    public void testGetSites() {
        Response response = get("/sites");
        assertEquals("{\"msg\":\"hello world\"}", response.getBody().asString());
    }
}

