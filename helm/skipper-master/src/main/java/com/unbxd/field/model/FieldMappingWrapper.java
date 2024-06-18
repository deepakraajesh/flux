package com.unbxd.field.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by gani on 4/23/19.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldMappingWrapper {
    private FieldMapping data;
}
