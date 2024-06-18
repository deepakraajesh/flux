package com.unbxd.pim.workflow.service.pimworkflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Header {
    private String key;
    private String value;
    private String description;

    private Header(String key,
                   String value,
                   String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    public static Header getInstance(String key,
                                     String value,
                                     String description) {
        return new Header(key, value, description);
    }
}
