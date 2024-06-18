package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import ro.pippo.core.route.RouteContext;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageRequest {
    @JsonIgnore
    private static ObjectMapper mapper = new ObjectMapper();
    Integer page;
    Integer count;
    String  sortBy;
    SortOrder sortOrder;
    String search;
    List<String> fieldTypeFilters;

    public static PageRequest getPageRequestFromRouteContext(
            RouteContext routeContext) throws JsonProcessingException {
        return mapper.readValue(routeContext.getRequest().getBody() , PageRequest.class);
    }

}
