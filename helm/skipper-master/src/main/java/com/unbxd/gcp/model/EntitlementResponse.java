package com.unbxd.gcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntitlementResponse {
    private Entitlement entitlement;

    private EntitlementResponse(Entitlement entitlement) {
        this.entitlement = entitlement;
    }

    public static EntitlementResponse getInstance(Entitlement entitlement) {
        return new EntitlementResponse(entitlement);
    }
}
