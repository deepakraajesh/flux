package com.unbxd.console.model;

import com.fasterxml.jackson.annotation.*;
import com.unbxd.skipper.states.model.StateContext;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteDetails extends StateContext {
    @JsonAlias("region_url")
    private String regionURL;

    public void setStatusDetails(StateContext stateContext) {
        this.setLanguage(stateContext.getLanguage());
        this.setPlatform(stateContext.getPlatform());
        this.setVertical(stateContext.getVertical());
        this.setImportId(stateContext.getImportId());
        this.setAdapterId(stateContext.getAdapterId());
        this.setChannelId(stateContext.getChannelId());
        this.setEnvironment(stateContext.getEnvironment());
        this.setAppId(getAppId());
        this.setWorkflowId(stateContext.getWorkflowId());
        this.setServeState(stateContext.getServeState());
        this.setTimestamp(stateContext.getTimestamp());
        this.setOrgId(stateContext.getOrgId());
        this.setId(stateContext.getId());
        this.setErrors(stateContext.getErrors());
        this.setCode(stateContext.getCode());
    }
}
