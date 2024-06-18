package com.unbxd.field.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldMapping {

    Map<String, String> properties = new LinkedHashMap<>();

    @JsonAnySetter
    void setProperties(String key, Object value) {
        String keyVal = String.valueOf(value);
        if(isSet(keyVal))
            properties.put(key, keyVal);
    }

    private boolean isSet(String val) {
        return (val != null && !"unbxd_NA".equals(val));
    }

    @JsonAnyGetter
    public Map<String, String> getProperties(){
        return properties;
    }
}
