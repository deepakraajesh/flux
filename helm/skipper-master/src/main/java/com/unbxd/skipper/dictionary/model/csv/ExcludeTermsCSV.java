package com.unbxd.skipper.dictionary.model.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcludeTermsCSV implements CSVData{
    @JsonProperty("Keyword")
    private String keyword;
    @JsonProperty("Exclusions")
    private List<String> excludeTerms;
    @JsonProperty("Id")
    private String id;
    @JsonProperty("Good")
    private String good;
    @JsonProperty("Bad")
    private String bad;
    @JsonProperty("Ok")
    private String ok;
    @JsonProperty("Best")
    private String best;
}
