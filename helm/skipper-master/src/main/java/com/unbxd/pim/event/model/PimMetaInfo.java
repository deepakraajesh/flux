package com.unbxd.pim.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PimMetaInfo {
    private String orgId;
    private String instanceId;
    private String workflowId;
    private long timestampInMillis;
    private EventDetails eventDetails;

    private static final String EVENT_ID = "event_id";
    private static final String IMPORT_ID = "import_id";
    private static final String EVENT_TYPE = "event_type";
    private static final String ADAPTER_ID = "adapter_id";
    private static final String WORKFLOW_ID = "workflow_id";
    private static final String INSTANCE_ID = "instance_id";

    public Map<String, String> getAsTags() {
        Map<String, String> tags = new HashMap<>();
        if(isNotEmpty(orgId)) { tags.put(IMPORT_ID, orgId); }
        if(isNotEmpty(instanceId)) { tags.put(INSTANCE_ID, instanceId); }
        if(isNotEmpty(workflowId)) { tags.put(WORKFLOW_ID, workflowId); }

        if(eventDetails != null) {
            if(isNotEmpty(eventDetails.getId())) { tags.put(EVENT_ID, eventDetails.getId()); }
            if(nonNull(eventDetails.getType())) { tags.put(EVENT_TYPE, eventDetails.getType().name()); }
            if(isNotEmpty(eventDetails.getAdapterId())) { tags.put(ADAPTER_ID, eventDetails.getAdapterId()); }
        }
        return tags;
    }

    public String getImportId() {
        if(eventDetails != null) {
            return eventDetails.getId();
        }
        return null;
    }

    public String getAdapterId() {
        if(eventDetails != null) {
            return eventDetails.getAdapterId();
        }
        return null;
    }
}
