package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryRuleWrapper {

    @JsonProperty(value = "query_rules")
    private List<QueryRule> queryRules;

    public Set<String> getQueries() {
        if(queryRules == null)
            return Collections.emptySet();
        return queryRules.stream().map(queryRule -> queryRule.getQuery()).collect(Collectors.toSet());
    }
}
