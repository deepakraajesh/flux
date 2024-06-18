package com.unbxd.gcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.cloudcommerceprocurement.v1.model.ListAccountsResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.unbxd.gcp.model.Account.getInstance;
import static java.util.Collections.sort;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkAccountResponse {
    private String nextPageToken;
    private List<Account> accounts;

    public static BulkAccountResponse from
            (ListAccountsResponse listAccountsResponse) {
        BulkAccountResponse bulkAccountResponse = new BulkAccountResponse();
        bulkAccountResponse.nextPageToken = listAccountsResponse.getNextPageToken();
        List<com.google.cloudcommerceprocurement.v1.model.Account> accountsInfo
                = listAccountsResponse.getAccounts();

        if (isNotEmpty(accountsInfo)) {
            ArrayList<Account> accounts = new ArrayList<>();
            for (com.google.cloudcommerceprocurement.v1.model.Account
                    accountInfo: accountsInfo) {
                accounts.add(getInstance(accountInfo));
            }
            bulkAccountResponse.setAccounts(accounts);
            sort(accounts);
        }
        return bulkAccountResponse;
    }
}
