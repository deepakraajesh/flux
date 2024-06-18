package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tolerance {
    private int retries = 1;
    private Timeout timeout;
    public Tolerance() { this.timeout = new Timeout(); }
}
