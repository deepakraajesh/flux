package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportNodeConfig {
    @JsonProperty(value = "check_readiness")
    private Boolean checkReadiness;

    private String channelId;

    @JsonProperty(value = "org_app_id")
    private String orgAppId;

    @JsonProperty(value = "adapter_id")
    private String adapterId;

    @JsonProperty(value = "export_type")
    private String exportType;

    @JsonProperty(value = "job_action_identifier")
    private String jobActionIdentifier;

    private String orgId;
}

