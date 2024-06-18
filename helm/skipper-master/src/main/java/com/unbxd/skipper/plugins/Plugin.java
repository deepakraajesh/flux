package com.unbxd.skipper.plugins;

import com.unbxd.skipper.plugins.exception.PluginException;

public interface Plugin {

    String redirectURL(String siteKey, String region, String dbDocId,
                       String shopName, String plugin) throws PluginException;

    void install(String siteId, String shopName, String siteKey, String region, String plugin) throws PluginException;

    void install(String dbDocId, String siteId, String shopName,
                 String siteKey, String region, String plugin, String appToken)
            throws PluginException;

    void setVariants(String siteKey, Boolean enableVariants) throws PluginException;
}
