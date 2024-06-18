package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.field.model.FSSearchableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.pippo.core.route.RouteContext;

import java.util.List;

import static java.util.Objects.isNull;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableField implements Field {
    private String fieldName;
    private Float productCoverage;
    private Float catalogCoverage;
    private Float queryCoverage;
    private SearchWeightage searchWeightage;
    private SearchWeightage aiRecc;

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    public static SearchableField getSearchableField(FSSearchableField searchableFieldFromFieldService){
        SearchableField searchableField = new SearchableField();
        searchableField.setFieldName( searchableFieldFromFieldService.getFieldName());
        searchableField.setSearchWeightageFromFieldServiceData( searchableFieldFromFieldService.getSearchWeightage());
        return searchableField;
    }

    public static List<SearchableField> getSearchableFieldsFromRouteContext(RouteContext routeContext)
    throws JsonProcessingException {
        return mapper.readValue(routeContext.getRequest().getBody(), new TypeReference<>() {});
    }

    @JsonIgnore
    public void setSearchWeightageFromFieldServiceData(Integer value){
        if(!isNull(value))
            this.searchWeightage = SearchWeightage.get(value);
    }

    @JsonIgnore
    public Integer getSearchWeightageForFieldService(){
        return this.searchWeightage.getValue();
    }
}
