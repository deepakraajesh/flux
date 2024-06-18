package com.unbxd.skipper.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.autosuggest.model.KeywordSuggestion;
import lombok.Data;
import ro.pippo.core.route.RouteContext;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Suggestions {
    private List<KeywordSuggestion> keywordSuggestions;
    private List<String> inFields;
    private PopularProducts popularProducts;
    private TopQueriesConfig topQueries;
    @JsonIgnore
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Suggestions getSuggestionsFromRouteContext(RouteContext routeContext)
            throws JsonProcessingException {
        return MAPPER.readValue(routeContext.getRequest().getBody(),Suggestions.class);
    }

}
