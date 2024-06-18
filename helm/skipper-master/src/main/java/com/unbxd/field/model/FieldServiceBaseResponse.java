package com.unbxd.field.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FieldServiceBaseResponse {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String status;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ErrorResponse> errors;
}
