package com.unbxd.gcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloudcommerceprocurement.v1.model.ListEntitlementsResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.unbxd.gcp.model.Entitlement.getInstance;
import static java.util.Collections.sort;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkEntitlementResponse {
    private String nextPageToken;
    private List<Entitlement> entitlements;

    public static BulkEntitlementResponse from
            (ListEntitlementsResponse listEntitlementsResponse) {
        BulkEntitlementResponse bulkEntitlementResponse = new BulkEntitlementResponse();
        bulkEntitlementResponse.nextPageToken = listEntitlementsResponse.getNextPageToken();
        List<com.google.cloudcommerceprocurement.v1.model.Entitlement> entitlementsInfo
                = listEntitlementsResponse.getEntitlements();

        if (isNotEmpty(entitlementsInfo)) {
            List<Entitlement> entitlements = new ArrayList<>();
            for (com.google.cloudcommerceprocurement.v1.model
                    .Entitlement entitlementInfo: entitlementsInfo) {
                entitlements.add(getInstance(entitlementInfo));
            }
            bulkEntitlementResponse.setEntitlements(entitlements);
            sort(entitlements);
        }
        return bulkEntitlementResponse;
    }
}
