package com.unbxd.skipper.states;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.unbxd.skipper.states.dao.StateDao;
import com.unbxd.skipper.states.dao.impl.AutosuggestStateDaoImpl;
import com.unbxd.skipper.states.dao.impl.StateDaoImpl;
import com.unbxd.skipper.states.impl.*;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.statemanager.AutosuggestStateManager;
import com.unbxd.skipper.states.statemanager.StateManager;

public class StateModule extends AbstractModule {

    @Override
    public void configure() {
        bind(StateDao.class).to(StateDaoImpl.class).asEagerSingleton();
        bind(StateDao.class).annotatedWith(Names.named("autosuggest")).to(AutosuggestStateDaoImpl.class).asEagerSingleton();
        bind(StateManager.class).annotatedWith(Names.named("autosuggest")).to(AutosuggestStateManager.class).asEagerSingleton();
        bindStates();
    }

    public void bindStates() {
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PLATFORM_UPLOAD_COMPLETE.name())).to(PlatformUploadCompleteVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.RELEVANCY_JOB_COMPLETE.name())).to(RelevancyCompleteVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PIM_SELECT.name())).to(PIMSelectVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PIM_PROPERTIES_COMPLETE.name())).to(PIMPropertiesVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PIM_UPLOAD_COMPLETE.name())).to(PIMUploadCompleteVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.DIMENSION_UPDATE.name())).to(DimensionUpdate.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.API_UPLOAD_COMPLETE.name())).to(ApiUploadCompleteVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.FILE_FEED_UPLOAD_COMPLETE.name())).to(FileFeedUploadCompleteVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PLATFORM_UPLOAD_ERROR.name())).to(PlatformUploadErrorState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.DIMENSION_MAPPING_START.name())).to(DimensionMappingStart.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PLATFORM_SELECT.name())).to(PlatformSelectVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PLATFORM_UPLOAD.name())).to(PlatformUploadVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.RELEVANCY_ERROR_STATE.name())).to(RelevancyErrorState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.CREATE_SITE_CONSOLE.name())).to(CreateSiteInConsole.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.AI_SETUP.name())).to(AISetup.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.MANUAL_SETUP.name())).to(ManualSetup.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.RELEVANCY_JOB_START.name())).to(RelevancyJobStart.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PIM_UPLOAD_START.name())).to(PIMUploadVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.API_UPLOAD_ERROR.name())).to(ApiUploadErrorState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.FILE_FEED_UPLOAD_ERROR.name())).to(FileFeedUploadErrorState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.SETUP_SEARCH.name())).to(SetupSearchVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.CONFIGURE_TEMPLATE.name())).to(ConfigureTemplate.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.API_SELECT.name())).to(ApiSelectVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.FILE_FEED_SELECT.name())).to(FileFeedSelectVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.API_UPLOAD.name())).to(ApiUploadVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.FILE_FEED_UPLOAD.name())).to(FileFeedUploadVirtualState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.CONSOLE_ERROR.name())).to(ConsoleErrorState.class);

        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.SELECT_TEMPLATE.name())).to(SelectTemplate.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.SITE_CREATED.name())).to(CreatedSiteState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.CREATE_SITE.name())).to(CreateSiteState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.PIM_ERROR.name())).to(PIMErrorState.class);
        bind(ServeState.class).annotatedWith(Names.named(ServeStateType.INDEXING_STATE.name())).
                to(IndexingVirtualState.class);
    }
}
