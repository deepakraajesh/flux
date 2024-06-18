package com.unbxd.skipper.dictionary.model.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class SynonymsCSV implements CSVData{
    @JsonProperty("Keyword")
    private String keyword;
    @JsonProperty("Bidirectional")
    private String bidirectional;
    @JsonProperty("Unidirectional")
    private String unidirectional;
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

    public SynonymsCSV(String keyword, String twoWayCSV, String oneWayCSV) {
        this.keyword = keyword;
        this.bidirectional = twoWayCSV;
        this.unidirectional = oneWayCSV;
    }
}
