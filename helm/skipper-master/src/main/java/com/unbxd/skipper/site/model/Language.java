package com.unbxd.skipper.site.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.unbxd.skipper.site.model.converter.ObjectIdToStringConverter;
import lombok.Data;
import org.bson.types.ObjectId;
import ro.pippo.core.route.RouteContext;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Language {
    @JsonIgnore
    private static ObjectMapper mapper =  new ObjectMapper();
    private String id;
    private String name;
    public static List<Language> getLanguagesFromRouteContext(RouteContext routeContext)
            throws JsonProcessingException {
        return mapper.readValue(routeContext.getRequest().getBody() , new TypeReference<List<Language>>(){});
    }
}
