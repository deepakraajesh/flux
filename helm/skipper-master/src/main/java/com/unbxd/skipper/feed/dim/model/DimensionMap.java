package com.unbxd.skipper.feed.dim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unbxd.skipper.relevancy.model.Field;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class DimensionMap implements Field {
    private String vertical;
    private DimensionFields parent;
    private DimensionFields variant;

    public void addMapping(Map<String, String> mapping) {
        parent.addMapping(mapping);
        variant.addMapping(mapping);
    }

    public Map<String, String> getMapping() {
        Map<String, String> mapping = parent.getMapping();
        mapping.putAll(variant.getMapping());
        return mapping;
    }

    public void setVerticalSpecificMapping(DimensionMap dimensionMap) {
        if(dimensionMap == null)
            return      ;
        this.parent.setVertical(dimensionMap.getParent().getVertical());
        this.variant.setVertical(dimensionMap.getVariant().getVertical());
        return;
    }

}

