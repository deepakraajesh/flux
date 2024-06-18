package com.unbxd.gcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.google.api.client.util.DateTime.parseRfc3339;
import static java.lang.Long.compare;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entitlement implements Comparable<Entitlement> {
    private AccountMeta accountMeta;
    @JsonIgnore private long updateTime;
    private com.google.cloudcommerceprocurement
            .v1.model.Entitlement entitlementInfo;

    private Entitlement(com.google.cloudcommerceprocurement.v1
                                .model.Entitlement entitlementInfo) {
        this.entitlementInfo = entitlementInfo;
        this.updateTime = parseRfc3339(entitlementInfo.getUpdateTime())
                .getValue();
    }

    public static Entitlement getInstance
            (com.google.cloudcommerceprocurement.v1
                     .model.Entitlement entitlementInfo) {
        return new Entitlement(entitlementInfo);
    }

    @Override
    public int compareTo(Entitlement o) { return compare(o.updateTime, this.updateTime); }
}
