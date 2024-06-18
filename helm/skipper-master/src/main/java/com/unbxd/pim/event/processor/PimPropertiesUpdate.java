package com.unbxd.pim.event.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.unbxd.pim.event.EventProcessor;
import com.unbxd.pim.event.exception.EventException;
import com.unbxd.pim.event.model.Event;
import com.unbxd.pim.event.model.EventResponse;
import com.unbxd.pim.workflow.service.PIMRemoteService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Log4j2
public class PimPropertiesUpdate implements EventProcessor {

    private PIMRemoteService pimRemoteService;

    private static final String DATA = "data";
    private static final String UNIQUE_ID = "uniqueId";
    private static final String PIM_PROPERTY_ID = "pim_property_id";
    private static final String ADAPTER_PROPERTY_NAME = "adapter_property_name";
    private static final String ADAPTER_PROPERTY_ID = "adapter_property_id";
    private static final String PIM_PROPERTIES_UPLOAD = "pimPropertiesUpload";
    private static final String NETWORK_ADAPTER_SUMMARY = "network_adapter_summary";
    private static final String PROPERTY_DETAILS_WITH_MAPPINGS = "property_details_with_mappings";

    @Inject
    public PimPropertiesUpdate(PIMRemoteService pimRemoteService) {
        this.pimRemoteService = pimRemoteService;
    }

    @Override
    public void process(Event event) throws EventException {
        String cookieValue = event.getCookie();
        JsonObject mappingRequest = getMappingRequest();


        try {
            Response<JsonObject> adapterPropertiesResponse = pimRemoteService.getAdapterProperties(
                    UN_SSO_UID + "=" +  cookieValue, event.getOrgId(), event.getAdapterId()).execute();
            if(!adapterPropertiesResponse.isSuccessful()) {
                String msg = "PIM request to fetch adapter failed with non 200 status";
                log.error(msg + " for site: " + event.getSiteKey() + " status:" + adapterPropertiesResponse.code() +
                        " reason:" + adapterPropertiesResponse.errorBody().string());
                throw new EventException(msg);
            }
            JsonObject adapterResponse = adapterPropertiesResponse.body();
            String adapterPropertiesId = null;
            try {
                adapterPropertiesId = getAdapterPropertiesId(event.getSiteKey(), adapterResponse);
            } catch(IllegalArgumentException e) {
                EventResponse eventResponse = new EventResponse();
                eventResponse.setErrorMessage(e.getMessage());
                event.setEventResponse(eventResponse);
                eventResponse.setStatusCode(400);
                return;
            }

            setAdapterPropertiesMapping(mappingRequest, adapterPropertiesId);
            Call<JsonObject> mappingPropertiesCall = pimRemoteService
                    .saveAdapterProperties(UN_SSO_UID + "=" + cookieValue,
                            event.getOrgId(), event.getAdapterId(), mappingRequest);

            Response<JsonObject> mappingPropertiesResponse = mappingPropertiesCall.execute();
            int statusCode = mappingPropertiesResponse.code();
            if(statusCode != 200) {
                EventResponse eventResponse = new EventResponse();
                eventResponse.setErrorMessage(mappingPropertiesResponse.errorBody().string());
                eventResponse.setStatusCode(statusCode);
                event.setEventResponse(eventResponse);
                return;
            }
        } catch(IOException e) {
            log.error("Error while trying to fetch adapterId: ", e);
        }
    }

    private String getAdapterPropertiesId(String siteKey, JsonObject adapterResponse) {
        if(adapterResponse.isJsonNull()) {
            String msg = "Incorrect response received with channel propety fetch API ";
            log.error(msg + " for site: "+ siteKey);
            throw new IllegalArgumentException(msg);
        }
        JsonObject dataObject = getDataObject(adapterResponse);
        if(dataObject == null || dataObject.isJsonNull()) {
            String msg = "Incorrect response received with channel propety fetch API" + adapterResponse.getAsString();
            log.error(msg + " for site: "+ siteKey);
            throw new IllegalArgumentException(msg);
        }
        JsonElement propertyDetails = dataObject.get(PROPERTY_DETAILS_WITH_MAPPINGS);
        if(propertyDetails == null || propertyDetails.isJsonNull()) {
            String msg = "Incorrect response received with channel propety fetch API" + adapterResponse.getAsString();
            log.error(msg + " for site: "+ siteKey);
            throw new IllegalArgumentException(msg);
        }
        JsonArray propertiesArray = propertyDetails.getAsJsonArray();
        if(propertiesArray.isJsonNull() || !propertiesArray.isJsonArray()) {
            String msg = "Incorrect response received with channel propety fetch API" + adapterResponse.getAsString();
            log.error(msg + " for site: "+ siteKey);
            throw new IllegalArgumentException(msg);
        }
        String adapterPropertiesId = null;

        for (JsonElement propertiesElement : propertiesArray) {
            JsonObject propertiesObject = propertiesElement.getAsJsonObject();
            if(!propertiesObject.has(ADAPTER_PROPERTY_NAME)) {
                String msg = "Incorrect response received as " + adapterResponse.getAsString();
                log.error(msg + " for site: "+ siteKey);
                throw new IllegalArgumentException(msg);
            }
            JsonElement adapterPropertyName = propertiesObject.get(ADAPTER_PROPERTY_NAME);
            if (equalsIgnoreCase(adapterPropertyName.getAsString(), UNIQUE_ID)) {
                adapterPropertiesId = propertiesObject.get(ADAPTER_PROPERTY_ID).getAsString();
                break;
            }
        }
        return adapterPropertiesId;
    }

    private void setAdapterPropertiesMapping(JsonObject mappingRequest, String adapterPropertiesId) {
        JsonObject networkAdapterProperty = mappingRequest.getAsJsonObject(NETWORK_ADAPTER_SUMMARY);
        JsonArray propertyDetailsArray = networkAdapterProperty.getAsJsonArray(PROPERTY_DETAILS_WITH_MAPPINGS);
        JsonObject uniqueIdObject = propertyDetailsArray.iterator().next().getAsJsonObject();
        uniqueIdObject.addProperty(ADAPTER_PROPERTY_ID, adapterPropertiesId);
    }

    private JsonObject getDataObject(JsonObject adapterResponse) {
        return adapterResponse.get(DATA).getAsJsonObject();
    }

    private JsonObject getMappingRequest() {
        String req = "{\"network_adapter_summary\": {\"property_details_with_mappings\": " +
                "[{\"adapter_property_name\": \"uniqueId\", " +
                " \"mapping_type\": \"SIMPLE\",\"editor_type\": \"CODE\"," +
                "\"adapter_property_id\": \"5e70870cf43daa0008cf52e7\", " +
                " \"is_editable\": true," +
                "\"preSelectedMappingData\": " +
                "{\"id\": \"1748\",\"name\": \"uniqueId\",\"field_id\": \"uniqueId\"," +
                "\"data_type\": \"text\"},\"last_modified_time\": 0," +
                "\"pim_property_id\": \"uniqueId\",\"required\": true}]},\"clone_adapter\": false}";
        return JsonParser.parseString(req).getAsJsonObject();
    }
}

