package com.unbxd.skipper.feed.dim.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DimensionFields {
    private List<DimensionIndividualMap> essential;
    private List<DimensionIndividualMap> vertical;

    private static final String UNBXD_NA = "unbxd_NA";

    public void addMapping(Map<String, String> mapping) {
        if(this.essential != null) {
            for (DimensionIndividualMap field : essential) {
                if (mapping.containsKey(field.getId()) && mapping.get(field.getId()) != UNBXD_NA)
                    field.setMapping(mapping.get(field.getId()));
            }
        }
        if (this.vertical != null) {
            for (DimensionIndividualMap field : vertical) {
                if (mapping.containsKey(field.getId()) && mapping.get(field.getId()) != UNBXD_NA)
                    field.setMapping(mapping.get(field.getId()));
            }
         }
    }

    public Map<String, String> getMapping() {
        Map<String, String> mapping = new HashMap<>();
        for(DimensionIndividualMap field: essential) {
            if(field.getMapping() != null) {
                mapping.put(field.getId(), field.getMapping());
            } else {
                mapping.put(field.getId(), UNBXD_NA);
            }
        }
        for(DimensionIndividualMap field: vertical) {
            if(field.getMapping() != null) {
                mapping.put(field.getId(), field.getMapping());
            } else {
                mapping.put(field.getId(), UNBXD_NA);
            }
        }
        return mapping;
    }
}

