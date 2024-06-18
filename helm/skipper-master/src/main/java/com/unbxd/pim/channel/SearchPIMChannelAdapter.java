package com.unbxd.pim.channel;

import com.google.inject.Inject;
import com.unbxd.pim.AbstractPIMResponse;
import com.unbxd.pim.channel.model.ExportPropertiesRequest;
import com.unbxd.pim.channel.model.PIMExportMapping;
import com.unbxd.pim.exception.PIMException;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

@Log4j2
public class SearchPIMChannelAdapter implements PIMChannelAdapter {

    PIMRemoteChannelService channelService;

    @Inject
    public SearchPIMChannelAdapter(PIMRemoteChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public void updateChannelMapping(String cookie, String orgId, String adapterId,
                                     List<Map<String, Object>> properties, String siteKey)
            throws PIMException {
        try {
            List<Map<String, Object>> exportProperties = fetchProperties(cookie, orgId, adapterId);

            Map<String, Map<String, Object>> exportLHSNameToIdMapping = new HashMap<>();
            Set<Map<String, Object>> exportPropertiesToBeUpdated = new HashSet<>();
            for(Map<String, Object> property: exportProperties) {
                // if uniqueId is not set, Then set the uniqueId property
                if(String.valueOf(property.get("adapter_property_name")).equals("uniqueId")
                        && property.get("pim_property_id" ) == null) {
                    property.put("pim_property_id", "uniqueId");
                    property.put("data_type", "text");
                    property.put("is_multivalued", false);
                    property.put("mapping_type", "SIMPLE");
                    exportPropertiesToBeUpdated.add(property);
                }
                exportLHSNameToIdMapping.put(String.valueOf(property.get("adapter_property_name")),
                        property);
            }

            // iterate over the properties
            //      if the field the does not exist in export LHS
            // then property should be added to the export
            Set<Map<String, Object>> importPropertiesToBeUpdated = new HashSet<>();
            for(Map<String, Object> importProperty: properties) {
                if(importProperty.containsKey(PIMExportMapping.MAPPING_TYPE)) {
                    if(String.valueOf(importProperty.get(PIMExportMapping.MAPPING_TYPE)).
                            equals(PIMExportMapping.MAPPING_TYPE_DO_NOT_MAP)) {
                        continue;
                    }
                    if(String.valueOf(importProperty.get(PIMExportMapping.MAPPING_TYPE)).
                            equals(PIMExportMapping.MAPPING_TYPE_CODE)) {
                        importProperty.put(PIMExportMapping.MAPPING_TYPE, PIMExportMapping.MAPPING_TYPE_SIMPLE);
                    }
                }
                if (!exportLHSNameToIdMapping.containsKey(importProperty.get("adapter_property_name"))) {
                    importPropertiesToBeUpdated.add(importProperty);
                } else {
                    Map<String, Object> exportProperty =
                            exportLHSNameToIdMapping.get(importProperty.get("adapter_property_name"));
                    Map<String, Object> updatedProperty = updateExportProperty(exportProperty, importProperty);
                    if(updatedProperty != null)
                        exportPropertiesToBeUpdated.add(updatedProperty);
                }
            }
            // if nothing is modified return
            if(importPropertiesToBeUpdated.size() == 0 && exportPropertiesToBeUpdated.size() == 0)
                return;
            ExportPropertiesRequest req = new ExportPropertiesRequest(importPropertiesToBeUpdated, Boolean.TRUE);
            req.updateProperties(exportPropertiesToBeUpdated, Boolean.FALSE);
            Response<AbstractPIMResponse<Map<String, Object>>> resp = channelService.
                    updateMapping(cookie, orgId, adapterId, req).execute();
            if(!resp.isSuccessful()) {
                String msg = " Error while updating network channel adapter";
                log.error(msg + " for orgId:" + orgId + " adapterId:" + adapterId
                        + " with code:" + resp.code() + " reason: " + resp.errorBody().string());
                throw new PIMException(msg);
            }




        } catch (IOException e) {
            String msg = "Internal network error while updating uniqueId to the channel adapter mapping";
            log.error( msg + " for org:" + orgId +  " adapterId:" + adapterId + " reason:" + e.getMessage());
            throw new PIMException(msg);
        }

    }

    /**
     * returns exportProperty if it is modified, else returns null
     * @param exportProperty
     * @param importProperty
     * @return
     */
    public Map<String, Object> updateExportProperty(Map<String, Object> exportProperty,
                                                    Map<String, Object> importProperty) {
        HashMap<String, Object> updatedExportProperty = new HashMap<>(exportProperty);
        boolean isExportPropertyModified = Boolean.FALSE;
        for(String importPropertyProp: importProperty.keySet()) {
            // Check if any property of the exportProperty has changed from importProperty,
            // Then, Copy properties from import to export
            if(!PIMExportMapping.getImportBlackistedProperties().contains(importPropertyProp)) {
                // if it is data_type, convert the value and then check
                if(importPropertyProp.equals(PIMExportMapping.DATA_TYPE) ) {
                    if(exportProperty.get(importPropertyProp) == null || !exportProperty.get(importPropertyProp).equals(
                        PIMExportMapping.getDatatype(String.valueOf(importProperty.get(importPropertyProp)),
                                Boolean.TRUE))) {
                        updatedExportProperty.put(importPropertyProp,
                                PIMExportMapping.
                                        getDatatype(String.valueOf(importProperty.get(importPropertyProp)),
                                                Boolean.TRUE));
                        isExportPropertyModified = Boolean.TRUE;
                    }
                } else if(!exportProperty.containsKey(importPropertyProp)
                        || !exportProperty.get(importPropertyProp).equals(
                        importProperty.get(importPropertyProp))) {
                    updatedExportProperty.put(importPropertyProp, importProperty.get(importPropertyProp));
                    isExportPropertyModified = Boolean.TRUE;
                }
            }
        }
        if(!isExportPropertyModified)
            return null;
        return updatedExportProperty;
    }

    public List<Map<String, Object>> fetchProperties(String cookie, String orgId, String adapterId)
            throws IOException, PIMException {
        Response<AbstractPIMResponse<PIMExportMapping>> propertiesResponse = channelService.fetchMapping(cookie, orgId, adapterId).execute();
        if(!propertiesResponse.isSuccessful()) {
            String msg = "Internal network error while updating uniqueId to the channel adapter mapping";
            log.error(msg + " for org:" + orgId + " adapterId:" + adapterId
                    + " code:" + propertiesResponse.code()
                    + " reason:" + propertiesResponse.errorBody().string());
            throw new PIMException(msg);
        }
        AbstractPIMResponse<PIMExportMapping> exportProperties = propertiesResponse.body();
        if(!exportProperties.isDataCorrect() || exportProperties.getData().getExportProperties() == null) {
            String msg = "Incorrect data received while fetching channel adapter mapping from PIM";
            log.error(msg + " for org:" + orgId + " adapterId:" + adapterId);
            throw new PIMException(msg);
        }
        return exportProperties.getData().getExportProperties();
    }
}

