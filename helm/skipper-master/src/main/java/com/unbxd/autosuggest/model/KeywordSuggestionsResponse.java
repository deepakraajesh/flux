package com.unbxd.autosuggest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeywordSuggestionsResponse extends GimliBaseResponse {
    List<KeywordSuggestion> keywordSuggestions;
}
