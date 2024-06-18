package com.unbxd.skipper.variants.service;

import com.unbxd.skipper.variants.exception.VariantsConfigException;

public interface VariantConfigService {
    void setVariantsInSearch(String siteKey,
                                   Boolean enableVariants) throws VariantsConfigException;
    void setVariantsInPim(String siteKey,
                                   Boolean enableVariants) throws VariantsConfigException;
}
