package com.unbxd.skipper.variants.service;

import com.google.inject.Inject;
import com.unbxd.pim.exception.PIMException;
import com.unbxd.skipper.plugins.Plugin;
import com.unbxd.skipper.plugins.exception.PluginException;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.variants.exception.VariantsConfigException;
import com.unbxd.pim.workflow.service.PIMService;
import com.unbxd.search.config.SearchConfigService;
import com.unbxd.search.exception.SearchConfigException;
import lombok.extern.log4j.Log4j2;

import static java.util.Objects.isNull;

@Log4j2
public class VariantConfigServiceImpl implements VariantConfigService {
    private SearchConfigService searchConfigService;
    private PIMService pimService;
    private SiteService  siteService;
    private Plugin plugin;

    @Inject
    public VariantConfigServiceImpl(SearchConfigService searchConfigService,
                                    SiteService siteService,
                                    PIMService pimService, Plugin plugin) {
        this.searchConfigService = searchConfigService;
        this.siteService = siteService;
        this.pimService = pimService;
        this.plugin = plugin;
    }

    @Override
    public void setVariantsInSearch(String siteKey,
                                          Boolean enableVariants) throws VariantsConfigException {
        try {
            searchConfigService.setVariants(siteKey, enableVariants);
            plugin.setVariants(siteKey, enableVariants);
            saveConfig(siteKey, enableVariants);
        } catch (SearchConfigException e) {
            throw new VariantsConfigException(e.getCode(), e.getMessage());
        } catch (PluginException e) {
            throw new VariantsConfigException(e.getCode(), e.getMessage());
        } catch (SiteNotFoundException e) {
            throw new VariantsConfigException(404, "Site not found");
        }
    }

    @Override
    public void setVariantsInPim(String siteKey,
                                          Boolean enableVariants) throws VariantsConfigException {
        try {
            log.info("variants config set in pim and search for siteKey: " + siteKey);
            pimService.setGroupByParentProperty(siteKey, enableVariants);
        }  catch (PIMException e) {
            log.error("Error while setting variants config in pim for siteKey: " + siteKey);
            throw new VariantsConfigException(e.getCode(), e.getMessage());
        }
    }

    private void saveConfig(String siteKey,
                            Boolean enableVariants) throws SiteNotFoundException {
            siteService.setVariants(siteKey,enableVariants);
    }

    private Boolean isDiffFoundInConfig(String siteKey,
                                        Boolean enableVariants) throws SiteNotFoundException {
        StateContext stateContext = siteService.getSiteStatus(siteKey);
        if(isNull(stateContext.getVariantsEnabled()))
            return Boolean.TRUE;
        else
            return !(enableVariants.equals(stateContext.getVariantsEnabled()));
    }
}
