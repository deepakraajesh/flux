package com.unbxd.skipper.site.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.skipper.site.model.Utility.ObjectMapperUtility;
import lombok.Data;
import ro.pippo.core.route.RouteContext;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(value = "data")
public class DataCenterData {
    @JsonProperty(value = "regions")
    private List<DataCenter> dataCenters;
    @JsonIgnore
    private static ObjectMapper mapper = ObjectMapperUtility.getObjectMapper();

    @JsonCreator
    public DataCenterData(@JsonProperty(value = "regions") List<DataCenter> dataCenters) {
        this.dataCenters = dataCenters;
    }

    public static DataCenterData getDataCenterDataFromRouteContext(RouteContext routeContext)
            throws JsonProcessingException {
        return mapper.readValue(routeContext.getRequest().getBody(), DataCenterData.class);
    }

    public String asJSONString() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}

