package com.unbxd.gcp.service;

import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.model.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.gcp.GCPConstants;
import com.unbxd.gcp.dao.ProcurementDao;
import com.unbxd.gcp.exception.GCPException;
import com.unbxd.gcp.model.BulkEntitlementResponse;
import com.unbxd.gcp.model.Entitlement;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;
import static com.google.auth.oauth2.GoogleCredentials.getApplicationDefault;
import static com.unbxd.gcp.GCPConstants.CLOUD_SCOPE;
import static com.unbxd.gcp.model.BulkEntitlementResponse.from;
import static com.unbxd.gcp.model.Entitlement.getInstance;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@Singleton
public class EntitlementService {

    private final String PROJECT_ID;
    private final String PROVIDER_NAME_PREFIX;
    private final String ENTITLEMENT_NAME_PREFIX;
    private final String GCP_PROJECT_ID = "gcp.project.Id";

    private ProcurementDao procurementDao;
    private CloudCommercePartnerProcurementService procurementService;


    @Inject
    public EntitlementService(Config config,
                              ProcurementDao procurementDao)
            throws GeneralSecurityException, IOException {
        PROJECT_ID = config.getProperty(GCP_PROJECT_ID);
        PROVIDER_NAME_PREFIX = "providers/" + PROJECT_ID;
        ENTITLEMENT_NAME_PREFIX = PROVIDER_NAME_PREFIX + "/entitlements/";

        this.procurementDao = procurementDao;
        Boolean isGCP = Boolean.valueOf(config.getProperty(GCPConstants.IS_GCP, String.valueOf(Boolean.FALSE)));
        if(isGCP)
            this.procurementService = new CloudCommercePartnerProcurementService
                .Builder(newTrustedTransport(), new GsonFactory(),
                new HttpCredentialsAdapter(getApplicationDefault()
                        .createScoped(singletonList(CLOUD_SCOPE))))
                .setApplicationName(PROJECT_ID).build();
    }

    public BulkEntitlementResponse getEntitlements(int pageSize,
                                                    String filter,
                                                    String pageToken) throws GCPException {
        try {
            return populateAccountMeta(from(procurementService.providers().entitlements()
                    .list(PROVIDER_NAME_PREFIX).setPageSize(pageSize)
                    .setPageToken(pageToken).setFilter(filter)
                    .execute()));
        } catch (IOException e) {
            throw new GCPException("Exception while trying to bulk fetch " +
                    "entitlements from procurement API: " + e.getMessage());
        }
    }

    public Entitlement getEntitlement(String entitlementId) throws GCPException {
        try {
            String entitlementName = getEntitlementName(entitlementId);
            return populateAccountMeta(getInstance(procurementService
                    .providers().entitlements().get(entitlementName)
                    .execute()));
        } catch (IOException e) {
            throw new GCPException("Exception while trying to fetch " +
                    "entitlement[" + entitlementId + "] from procurement" +
                    " API: " + e.getMessage());
        }
    }

    public void approveEntitlement(String entitlementId,
                                   ApproveEntitlementRequest request)
            throws GCPException {
        try {
            String entitlementName = getEntitlementName(entitlementId);
            procurementService.providers().entitlements().approve
                    (entitlementName, request).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to approve " +
                    "entitlement[" + entitlementId + "] from procurement " +
                    "API: " + e.getMessage());
        }
    }

    public void rejectEntitlement(String entitlementId,
                                  RejectEntitlementRequest request)
            throws GCPException {
        try {
            String entitlementName = getEntitlementName(entitlementId);
            procurementService.providers().entitlements().reject
                    (entitlementName, request).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to reject " +
                    "entitlement[" + entitlementId + "] from procurement " +
                    "API: " + e.getMessage());
        }
    }

    public void approvePlanChange(String entitlementId,
                                  ApproveEntitlementPlanChangeRequest request)
            throws GCPException {
        try {
            String entitlementName = getEntitlementName(entitlementId);
            procurementService.providers().entitlements().approvePlanChange
                    (entitlementName, request).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to approve " +
                    "entitlement[" + entitlementId + "] plan change from" +
                    " procurement API: " + e.getMessage());
        }
    }

    public void rejectPlanChange(String entitlementId,
                                 RejectEntitlementPlanChangeRequest request)
            throws GCPException {
        try {
            String entitlementName = getEntitlementName(entitlementId);
            procurementService.providers().entitlements().rejectPlanChange
                    (entitlementName, request).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to reject " +
                    "entitlement[" + entitlementId + "] plan change from" +
                    " procurement API: " + e.getMessage());
        }
    }

    private BulkEntitlementResponse populateAccountMeta
            (BulkEntitlementResponse response) throws GCPException {
        for (Entitlement entitlement : emptyIfNull(response
                .getEntitlements())) {
            populateAccountMeta(entitlement);
        }
        return response;
    }

    private Entitlement populateAccountMeta
            (Entitlement entitlement) throws GCPException {
        entitlement.setAccountMeta(procurementDao.getAccountMeta(entitlement
                .getEntitlementInfo().getAccount()));
        return entitlement;
    }

    private String getEntitlementName(String id) {
        return ENTITLEMENT_NAME_PREFIX + id;
    }
}
