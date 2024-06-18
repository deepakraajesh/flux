package com.unbxd.skipper.dictionary.model.csv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhrasesCSV implements CSVData{
    @JsonProperty("Type")
    private DictionaryEntry.Type type;
    @JsonProperty("Phrase")
    private String phrase;
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
