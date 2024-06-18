package com.unbxd.pim.channel;

import com.unbxd.pim.exception.PIMException;

import java.util.List;
import java.util.Map;

public interface PIMChannelAdapter {

    void updateChannelMapping(String cookie, String orgId, String adapterId,
                              List<Map<String, Object>> properties, String siteKey)
             throws PIMException;
}
