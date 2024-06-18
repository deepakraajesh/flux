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
public class Account implements Comparable<Account> {
    private AccountMeta accountMeta;
    @JsonIgnore private long updateTime;
    private com.google.cloudcommerceprocurement.v1.model.Account accountInfo;

    private Account(com.google.cloudcommerceprocurement
                            .v1.model.Account accountInfo) {
        this.accountInfo = accountInfo;
        this.updateTime = parseRfc3339(accountInfo.getUpdateTime())
                .getValue();
    }

    public static Account getInstance(com.google.cloudcommerceprocurement
                                              .v1.model.Account accountInfo) {
        return new Account(accountInfo);
    }

    @Override
    public int compareTo(Account o) { return compare(o.updateTime, this.updateTime); }
}