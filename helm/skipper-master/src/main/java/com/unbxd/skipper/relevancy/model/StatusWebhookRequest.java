package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown =  true)
public class StatusWebhookRequest {
    private String sitekey;

    @JsonProperty("workflow_id")
    private String workflowId;

    private String workflow = "get-relevance-configs";
}
