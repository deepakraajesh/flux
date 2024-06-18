package com.unbxd.search.config;

import com.unbxd.search.exception.SearchConfigException;

import java.util.List;

public interface SearchConfigService {
     void setVariants(String siteKey,
                      Boolean enableVariant) throws SearchConfigException;

     void enableNER(String siteKey, List<String> vertical, String cookie) throws SearchConfigException;
}
