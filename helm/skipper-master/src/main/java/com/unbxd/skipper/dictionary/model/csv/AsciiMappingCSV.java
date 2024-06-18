package com.unbxd.skipper.dictionary.model.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AsciiMappingCSV implements CSVData {
    @JsonProperty("Mapping")
    private String mapping;
    @JsonProperty("Key")
    private String asciiString;
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
