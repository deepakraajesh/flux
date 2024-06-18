package com.unbxd.skipper.site.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.skipper.site.model.Utility.ObjectMapperUtility;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(value = "data")
public class SiteMeta {
    @JsonIgnore
    private static ObjectMapper mapper = ObjectMapperUtility.getObjectMapper();
    @JsonProperty(value = "environment")
    private List<Environment> environments;
    @JsonProperty(value = "vertical")
    private List<Vertical> verticals;
    @JsonProperty(value = "platform")
    private List<Platform> platforms;
    @JsonProperty(value = "language")
    private List<Language> languages;

    public String asJSONString() throws JsonProcessingException
    {
        return mapper.writeValueAsString(this);
    }
}
