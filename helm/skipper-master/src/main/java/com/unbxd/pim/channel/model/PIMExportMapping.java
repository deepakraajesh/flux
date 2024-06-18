package com.unbxd.pim.channel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PIMExportMapping {
    public static final String DATA_TYPE = "data_type";
    public static final String MAPPING_TYPE = "mapping_type";
    public static final String MAPPING_TYPE_DO_NOT_MAP = "DO_NOT_MAP";
    public static final String MAPPING_TYPE_SIMPLE = "SIMPLE";
    public static final String MAPPING_TYPE_CODE = "CODE";
    private static final String DATA_TYPE_DECIMAL = "decimal";
    private static final String DATA_TYPE_TEXT = "text";

    private static final String[] importBlackistedPropArray = {"adapter_property_id", "last_modified_time",
            "is_editable", "searchable", "transformation_code", "validMetaInfo"};
    private static final String[] exportBlackistedPropArray = {"last_modified_time",
            "is_editable", "searchable", "transformation_code", "validMetaInfo"};

    @JsonProperty(value = "property_details_with_mappings")
    private List<Map<String, Object>> exportProperties;

    public static List<String> getExportBlackistedProperties() {
        return Arrays.asList(exportBlackistedPropArray);
    }

    public static List<String> getImportBlackistedProperties() {
        return Arrays.asList(importBlackistedPropArray);
    }

    public PIMExportMapping(Set<Map<String, Object>> properties, boolean isImportProperty) {
        setProperties(properties, isImportProperty);
    }

    public static String getDatatype(String datatype, boolean isImportProperty) {
        if (isImportProperty) {
            if (datatype.equals("decimal")) {
                return DATA_TYPE_DECIMAL;
            } else {
                return DATA_TYPE_TEXT;
            }
        }
        return datatype;
    }

    public void setProperties(Set<Map<String, Object>> properties, boolean isImportProperty) {
        setProperties(properties, isImportProperty, Boolean.FALSE);
    }

    public void updateProperties(Set<Map<String, Object>> properties, boolean isImportProperty) {
        setProperties(properties, isImportProperty, Boolean.TRUE);
    }

    public void setProperties(Set<Map<String, Object>> properties, boolean isImportProperty, boolean update) {
        if(!update)
            this.exportProperties = new ArrayList<>();
        List<String> blacklistedProps = null;
        if(isImportProperty) {
            blacklistedProps = getImportBlackistedProperties();
        } else {
            blacklistedProps = getExportBlackistedProperties();
        }
        for(Map<String, Object> property: properties) {
            property.keySet().removeAll(blacklistedProps);
            if(property.containsKey(DATA_TYPE)) {
                String updatedDatatype = getDatatype(String.valueOf(property.get(DATA_TYPE)), isImportProperty);
                property.put(DATA_TYPE, updatedDatatype);
            }
            exportProperties.add(property);
        }
    }

}

