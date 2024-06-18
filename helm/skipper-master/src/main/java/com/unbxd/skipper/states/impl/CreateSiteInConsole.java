package com.unbxd.skipper.states.impl;

import com.google.inject.Inject;
import com.unbxd.console.model.ConsoleResponse;
import com.unbxd.console.service.ConsoleOrchestrationService;
import com.unbxd.event.EventFactory;
import com.unbxd.search.config.SearchConfigService;
import com.unbxd.search.exception.SearchConfigException;
import com.unbxd.skipper.plugins.Plugin;
import com.unbxd.skipper.plugins.exception.PluginException;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;
import com.unbxd.skipper.variants.exception.VariantsConfigException;
import com.unbxd.skipper.variants.service.VariantConfigService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static com.unbxd.skipper.states.model.ServeStateType.*;

@Slf4j
@NoArgsConstructor
public class CreateSiteInConsole extends CreateSiteState {


    private String facetAuthToken;
    private static final String SITEKEY = "siteKey";

    private static final String PLATFORM = "platform";

    @Inject
    private ConsoleOrchestrationService consoleOrchestrationService;

    @Inject
    private EventFactory eventFactory;

    @Inject
    private Plugin plugin;

    @Inject
    private SearchConfigService searchConfigService;

    @Inject
    private VariantConfigService variantConfigService;

    public CreateSiteInConsole(StateManager stateManager, StateContext stateContext) {
        super(stateManager, stateContext);
    }

    @Override
    public ServeStateType getStateType() { return CREATE_SITE_CONSOLE; }

    @Override
    public ServeStateType nextStateType() {
        if(stateContext.getCode() == 200) {
            return ServeStateType.SITE_CREATED;
        } else {
            return ServeStateType.CONSOLE_ERROR;
        }
    }

    @Override
    public void nextState() {
    }

    public void nextState(ServeStateType nextStateType, Map<String, String> stateData) {
        ServeState nextServeState = stateManager.getStateInstance(nextStateType);

        if(stateData != null && !stateData.isEmpty()) {
            nextServeState.setStateData(stateData);
        }
        nextServeState.setStateContext(stateContext);
        nextServeState.setStateManager(stateManager);

        if(stateContext.getCode() == 200) {
            stateManager.transitionState(stateContext, nextServeState);
        } else {
            stateManager.transitionErrorState(stateContext, nextServeState);
        }
    }

    @Override
    public void processState() {
        try {
            ConsoleResponse consoleResponse = consoleOrchestrationService
                    .createSite(stateContext.getSiteName(), stateContext.getRegion(), stateContext.getEnvironment(),
                            stateContext.getEmail(), stateContext.getLanguage());
            log.info("Successfully created site: " + stateContext.getSiteName());
            int statusCode = consoleResponse.getStatus();
            stateContext.setCode(statusCode);
            if (statusCode == 200) {
                ThreadContext.put(SITEKEY, consoleResponse.getSiteKey());
                stateContext.setSiteKey(consoleResponse.getSiteKey());
                stateContext.setSiteId(consoleResponse.getId());
            } else {
                stateContext.setErrors(consoleResponse.getError());
            }

        } catch (SocketTimeoutException e) {
            stateContext.setCode(504);
            log.error("Timeout Exception while trying to create site in console: ", e);
            stateContext.setErrors("Timeout Exception while trying to create site in console: " + e.getMessage());

        } catch (Exception e) {
            stateContext.setCode(500);
            log.error("Exception while trying to create site in console: ", e);
            stateContext.setErrors("Exception while trying to create site in console: " + e.getMessage());
        }
        if(stateContext.getCode() == 200) {
            ServeStateType stateType = installPlugin();
            if ((stateType != null)) {
                nextState(stateType, Collections.singletonMap(PLATFORM, stateContext.getFeedPlatform()));
            } else {
                nextState(ServeStateType.SITE_CREATED, null);
            }
            enableDefaultConfig();
            return;
        }
        nextState(CONSOLE_ERROR, null);
    }

    private ServeStateType installPlugin() {
        if(stateContext.getFeedPlatform() != null && stateContext.getAppToken() != null) {
            try {
                plugin.install(stateContext.getId(), stateContext.getSiteId(), stateContext.getShopName(),
                        stateContext.getSiteKey(), stateContext.getRegion(),
                        stateContext.getFeedPlatform(), stateContext.getAppToken());
                log.info("Plugin installed for siteKey:" + stateContext.getSiteKey()
                        + " for shop:" + stateContext.getShopName());
                return PLATFORM_UPLOAD;
            } catch (PluginException e) {
                log.info("Error while installing plugin for siteKey:" + stateContext.getSiteKey()
                        + " reason: " + e.getMessage());
                stateContext.setCode(e.getCode());
                stateContext.setErrors(e.getMessage());
                return CONSOLE_ERROR;
            }
        }
        return null;
    }

    private void enableDefaultConfig() {
        enableNERConfig();
        enableVariants();
    }

    private void enableVariants() {
        String cookie = UN_SSO_UID + "=" + stateContext.getCookie().get(UN_SSO_UID);
        String siteKey = stateContext.getSiteKey();
        try {
            variantConfigService.setVariantsInSearch(siteKey, Boolean.TRUE);
        } catch (VariantsConfigException e) {
            log.error("Error while enabling variants : " + e.getMessage() + " for siteKey " + siteKey);
        }
    }

    private void enableNERConfig() {
        String cookie = UN_SSO_UID + "=" + stateContext.getCookie().get(UN_SSO_UID);
        String siteKey = stateContext.getSiteKey();
        List<String> verticals = Collections.singletonList(stateContext.getVertical());
        try {
            searchConfigService.enableNER(siteKey, verticals, cookie);
        } catch (SearchConfigException e) {
            log.error("Error while enabling NER: " + e.getMessage() + " for siteKey " + siteKey);
            // send toucan an event if something is failed but dont fail the site creation
        }

    }
}
