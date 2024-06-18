package com.unbxd.pim.channel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportPropertiesRequest {

    @JsonProperty(value="network_adapter_summary")
    private PIMExportMapping properties;

    @JsonProperty(value = "clone_adapter")
    private Boolean cloneAdapter;

    public ExportPropertiesRequest(Set<Map<String, Object>> properties, Boolean isImportProperty) {
        setProperties(properties, isImportProperty);
    }

    public void setProperties(Set<Map<String, Object>> properties, Boolean isImportProperty) {
        this.properties = new PIMExportMapping(properties, isImportProperty);
        this.cloneAdapter = Boolean.FALSE;
    }

    public void updateProperties(Set<Map<String, Object>> properties, Boolean isImportProperty) {
        this.properties.updateProperties(properties, isImportProperty);
        this.cloneAdapter = Boolean.FALSE;
    }
}

