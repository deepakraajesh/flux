package com.unbxd.gcp.dao;

import com.unbxd.gcp.exception.GCPException;
import com.unbxd.gcp.model.AccountMeta;

public interface ProcurementDao {

    void saveAccountMeta(AccountMeta meta) throws GCPException;

    AccountMeta getAccountMeta(String accountName) throws GCPException;
}
