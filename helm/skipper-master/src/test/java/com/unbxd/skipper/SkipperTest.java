package com.unbxd.skipper;

import io.restassured.RestAssured;

public abstract class SkipperTest extends RestAssured {

    static {
        System.setProperty("pippo.mode", "test");
    }

}
