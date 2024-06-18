package com.unbxd.skipper.states.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.unbxd.skipper.states.model.StateContext;

@Singleton
public class StateDocumentUtils {

    private ObjectMapper mapper;

    public StateDocumentUtils() {
        mapper = new ObjectMapper();
    }

    public String getContextAsJson(StateContext stateContext) throws JsonProcessingException {
        return mapper.writeValueAsString(stateContext);
    }
}
