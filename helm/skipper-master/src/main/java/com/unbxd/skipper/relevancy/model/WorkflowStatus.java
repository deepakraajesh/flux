package com.unbxd.skipper.relevancy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowStatus {
    private Job[] jobs;
    private Status status;
    public static final String SUCCESS_CODE = "SUCCESS";
}