package com.unbxd.skipper.site.model.Utility;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public  class ObjectMapperUtility {
    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .enable(SerializationFeature.WRAP_ROOT_VALUE);
    }
}
