package com.unbxd.skipper.variants;

import com.google.inject.AbstractModule;
import com.unbxd.skipper.variants.service.VariantConfigService;
import com.unbxd.skipper.variants.service.VariantConfigServiceImpl;

public class VariantsModule extends AbstractModule {

    @Override
    public void configure() {
        bindVariantConfigService();
    }


    protected void bindVariantConfigService() {
        bind(VariantConfigService.class).to(VariantConfigServiceImpl.class);
    }

}