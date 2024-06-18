package com.unbxd.skipper.site.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import ro.pippo.core.route.RouteContext;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vertical {
    @JsonIgnore
    private static  ObjectMapper mapper = new ObjectMapper();
    private String  id;
    private String name;

    public static List<Vertical> getVerticalsFromRouteContext(RouteContext routeContext)
            throws JsonProcessingException {
        return mapper.readValue(routeContext.getRequest().getBody() , new TypeReference<List<Vertical>>(){});
    }
}

