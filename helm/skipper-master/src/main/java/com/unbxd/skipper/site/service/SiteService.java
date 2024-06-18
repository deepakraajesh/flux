package com.unbxd.skipper.site.service;

import com.unbxd.skipper.model.RequestContext;
import com.unbxd.skipper.model.SiteRequest;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.exception.ValidationException;
import com.unbxd.skipper.site.model.*;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;

import java.util.List;
import java.util.Map;

public interface SiteService {

    SiteMeta getSiteMeta();

    DataCenterData getDataCenterData(Map<String, String> providers);

    void start(RequestContext requestContext);

    void setVerticals(List<Vertical> verticals);

    void setPlatforms(List<Platform> platforms);

    void setLanguages(List<Language> languages);

    void setEnvironments(List<Environment> environments);

    void setDataCenterData(DataCenterData dataCenterData);

    StateContext getSiteById(String id) throws SiteNotFoundException;

    StateContext getSiteStatus(String siteKey) throws SiteNotFoundException;

    StateContext setTemplateId(String siteKey, String templateId) throws SiteNotFoundException;

    StateContext getSiteStatus(String fieldName, String fieldValue) throws SiteNotFoundException;

    StateContext updateContext(String siteKey,
                               String cookie,
                               String errors,
                               ServeStateType serveStateType,
                               Map<String, String> requestParams) throws SiteNotFoundException;

    void validateSiteRequest(SiteRequest siteRequest) throws ValidationException;

    void appendStateData(String siteKey, Map<String, String> data) throws SiteNotFoundException;

    void removeData(String siteKey, String key, String val) throws SiteNotFoundException;

    void deleteSite(String siteKey);

    void reset(String siteKey, ServeState state);

    void setVariants(String siteKey, Boolean enableVariants) throws SiteNotFoundException;

    void setSiteStateProperty(String siteKey, String property, String value) throws SiteNotFoundException, ValidationException;
}

