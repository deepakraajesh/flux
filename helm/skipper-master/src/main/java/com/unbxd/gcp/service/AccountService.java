package com.unbxd.gcp.service;

import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.RejectAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.ResetAccountRequest;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.config.Config;
import com.unbxd.gcp.GCPConstants;
import com.unbxd.gcp.dao.ProcurementDao;
import com.unbxd.gcp.exception.GCPException;
import com.unbxd.gcp.model.Account;
import com.unbxd.gcp.model.AccountMeta;
import com.unbxd.gcp.model.BulkAccountResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;
import static com.google.auth.oauth2.GoogleCredentials.getApplicationDefault;
import static com.unbxd.gcp.GCPConstants.CLOUD_SCOPE;
import static com.unbxd.gcp.model.Account.getInstance;
import static com.unbxd.gcp.model.BulkAccountResponse.from;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Singleton
public class AccountService {

    private final String PROJECT_ID;
    private final String ACCOUNT_NAME_PREFIX;
    private final String PROVIDER_NAME_PREFIX;
    private final String GCP_PROJECT_ID = "gcp.project.Id";

    private ProcurementDao procurementDao;
    private CloudCommercePartnerProcurementService procurementService;

    @Inject
    public AccountService(Config config,
                          ProcurementDao procurementDao)
            throws GeneralSecurityException, IOException {
        PROJECT_ID = config.getProperty(GCP_PROJECT_ID);
        PROVIDER_NAME_PREFIX = "providers/" + PROJECT_ID;
        ACCOUNT_NAME_PREFIX = PROVIDER_NAME_PREFIX + "/accounts/";

        this.procurementDao = procurementDao;
        Boolean isGCP = Boolean.valueOf(config.getProperty(GCPConstants.IS_GCP, String.valueOf(Boolean.FALSE)));
        if(isGCP)
            this.procurementService = new CloudCommercePartnerProcurementService
                .Builder(newTrustedTransport(), new GsonFactory(),
                new HttpCredentialsAdapter(getApplicationDefault()
                        .createScoped(singletonList(CLOUD_SCOPE))))
                        .setApplicationName(PROJECT_ID).build();
    }

    public void saveActivationData(AccountMeta accountMeta)
            throws GCPException {
        procurementDao.saveAccountMeta(accountMeta);
    }

    public Account getAccount(String accountId) throws GCPException {
        try {
            String accountName = getAccountName(accountId);
            return populateAccountMeta(getInstance
                    (procurementService.providers()
                    .accounts().get(accountName)
                    .execute()));
        } catch (IOException e) {
            throw new GCPException("Exception while trying to fetch " +
                    "account[" + accountId + "] from procurement API: "
                    + e.getMessage());
        }
    }

    public BulkAccountResponse getAccounts(int pageSize,
                                           String accounts,
                                           String pageToken) throws GCPException {
        try {
            BulkAccountResponse accountResponse = from(procurementService
                    .providers().accounts().list(PROVIDER_NAME_PREFIX)
                    .setPageSize(pageSize).setPageToken(pageToken)
                    .execute());

            filter(accounts, accountResponse);
            return populateAccountMeta(accountResponse);
        } catch (IOException e) {
            throw new GCPException("Exception while trying to bulk fetch " +
                    "accounts from procurement API: " + e.getMessage());
        }
    }

    public void approveAccount(String accountId,
                               ApproveAccountRequest approvalRequest) throws GCPException {
        try {
            String accountName = getAccountName(accountId);
            procurementService.providers().accounts().approve
                    (accountName, approvalRequest).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to approve account["
                    + accountId +"]: " + e.getMessage());
        }
    }

    public void rejectAccount(String accountId,
                              RejectAccountRequest rejectRequest) throws GCPException {
        try {
            String accountName = getAccountName(accountId);
            procurementService.providers().accounts().reject
                    (accountName, rejectRequest).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to reject account["
                    + accountId +"]: " + e.getMessage());
        }
    }

    public void resetAccount(String accountId,
                             ResetAccountRequest request) throws GCPException {
        try {
            String accountName = getAccountName(accountId);
            procurementService.providers().accounts().reset
                    (accountName, request).execute();
        } catch (IOException e) {
            throw new GCPException("Exception while trying to reset account["
                    + accountId +"]: " + e.getMessage());
        }
    }

    private BulkAccountResponse populateAccountMeta
            (BulkAccountResponse response) throws GCPException {
        for (Account account: emptyIfNull(response.getAccounts())) {
            populateAccountMeta(account);
        }
        return response;
    }

    private Account populateAccountMeta(Account account) throws GCPException {
        account.setAccountMeta(procurementDao.getAccountMeta(account
                .getAccountInfo().getName()));
        return account;
    }

    private void filter(String accounts,
                        BulkAccountResponse accountResponse) {
        if (isNotEmpty(accounts)) {
            accountResponse.getAccounts().removeIf(account -> !accounts
                    .contains(account.getAccountInfo().getName()));
        }
    }

    public String getAccountName(String id) {
        return ACCOUNT_NAME_PREFIX + id;
    }

    public String getAccountId(String accountName) {
        return accountName.substring(accountName
                .lastIndexOf("/") + 1);
    }
}
