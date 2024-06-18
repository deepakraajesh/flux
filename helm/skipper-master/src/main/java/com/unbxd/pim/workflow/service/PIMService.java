package com.unbxd.pim.workflow.service;

import com.unbxd.pim.exception.PIMException;

public interface PIMService {

    String APP_ID = "UNBXD_PIM_SEARCH_APP";

    String triggerFullUpload(String siteKey) throws PIMException;

    void setGroupByParentProperty(String siteKey,
                                  Boolean value) throws PIMException;
}
