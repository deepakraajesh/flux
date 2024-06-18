package com.unbxd.gcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountResponse {
    private Account account;

    private AccountResponse(Account account) {
        this.account = account;
    }

    public static AccountResponse getInstance(Account account) {
        return new AccountResponse(account);
    }
}
