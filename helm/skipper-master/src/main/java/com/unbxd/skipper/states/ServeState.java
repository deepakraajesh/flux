package com.unbxd.skipper.states;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.unbxd.skipper.relevancy.model.Job;
import com.unbxd.skipper.relevancy.model.JobType;
import com.unbxd.skipper.states.impl.SelectTemplate;
import com.unbxd.skipper.states.impl.ConfigureTemplate;
import com.unbxd.skipper.states.impl.*;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "stateType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateSiteState.class, name = "CREATE_SITE"),
        @JsonSubTypes.Type(value = CreatedSiteState.class, name = "SITE_CREATED"),
        @JsonSubTypes.Type(value = PIMErrorState.class, name = "PIM_ERROR_STATE"),
        @JsonSubTypes.Type(value = ConsoleErrorState.class, name = "CONSOLE_ERROR"),
        @JsonSubTypes.Type(value = ApiUploadVirtualState.class, name = "API_UPLOAD"),
        @JsonSubTypes.Type(value = ApiSelectVirtualState.class, name = "API_SELECT"),
        @JsonSubTypes.Type(value = ApiUploadErrorState.class, name = "API_UPLOAD_ERROR"),
        @JsonSubTypes.Type(value = FileFeedUploadVirtualState.class, name = "FILE_FEED_UPLOAD"),
        @JsonSubTypes.Type(value = FileFeedSelectVirtualState.class, name = "FILE_FEED_SELECT"),
        @JsonSubTypes.Type(value = FileFeedUploadErrorState.class, name = "FILE_FEED_UPLOAD_ERROR"),
        @JsonSubTypes.Type(value = SetupSearchVirtualState.class, name = "SETUP_SEARCH"),
        @JsonSubTypes.Type(value = AISetup.class, name = "AI_SETUP"),
        @JsonSubTypes.Type(value = DimensionUpdate.class, name = "DIMENSION_UPDATE"),
        @JsonSubTypes.Type(value = ManualSetup.class, name = "MANUAL_SETUP"),
        @JsonSubTypes.Type(value = RelevancyJobStart.class, name = "RELEVANCY_JOB_START"),
        @JsonSubTypes.Type(value = PIMSelectVirtualState.class, name = "PIM_SELECT"),
        @JsonSubTypes.Type(value = PIMUploadVirtualState.class, name = "PIM_UPLOAD_START"),
        @JsonSubTypes.Type(value = CreateSiteInConsole.class, name = "CREATE_SITE_CONSOLE"),
        @JsonSubTypes.Type(value = RelevancyErrorState.class, name = "RELEVANCY_ERROR_STATE"),
        @JsonSubTypes.Type(value = PlatformUploadVirtualState.class, name = "PLATFORM_UPLOAD"),
        @JsonSubTypes.Type(value = PlatformUploadVirtualState.class, name = "PLATFORM_INSTALLED"),
        @JsonSubTypes.Type(value = PlatformSelectVirtualState.class, name = "PLATFORM_SELECT"),
        @JsonSubTypes.Type(value = DimensionMappingStart.class, name = "DIMENSION_MAPPING_START"),
        @JsonSubTypes.Type(value = PlatformUploadErrorState.class, name = "PLATFORM_UPLOAD_ERROR"),
        @JsonSubTypes.Type(value = PIMUploadCompleteVirtualState.class, name = "PIM_UPLOAD_COMPLETE"),
        @JsonSubTypes.Type(value = PIMPropertiesVirtualState.class, name = "PIM_PROPERTIES_COMPLETE"),
        @JsonSubTypes.Type(value = ApiUploadCompleteVirtualState.class, name = "API_UPLOAD_COMPLETE"),
        @JsonSubTypes.Type(value = FileFeedUploadCompleteVirtualState.class, name = "FILE_FEED_UPLOAD_COMPLETE"),
        @JsonSubTypes.Type(value = RelevancyCompleteVirtualState.class, name = "RELEVANCY_JOB_COMPLETE"),
        @JsonSubTypes.Type(value = PlatformUploadCompleteVirtualState.class, name = "PLATFORM_UPLOAD_COMPLETE"),
        @JsonSubTypes.Type(value = IndexingVirtualState.class, name = "INDEXING_STATE"),

        @JsonSubTypes.Type(value = SelectTemplate.class, name = "SELECT_TEMPLATE"),
        @JsonSubTypes.Type(value = ConfigureTemplate.class, name = "CONFIGURE_TEMPLATE")
})
public interface ServeState extends Runnable {

    String PIM = "pim";
    String RELEVANCY_STATUS = "relevancy_status";

    void nextState();

    /** every processState() must call nextState() unless it's the last state */
    void processState();

    @JsonIgnore
    ServeStateType getStateType();

    ServeStateType nextStateType();

    Map<String, String> getStateData();

    void setStateData(Map<String, String> stateData);

    /* auxiliary methods */
    void setStateManager(StateManager stateManager);

    void setStateContext(StateContext stateContext);

    void updateRelevancyJobOutput(StateContext stateContext, List<JobType> jobType);
}

