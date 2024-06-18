package com.unbxd.skipper.site.service.impl;

import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.skipper.model.RequestContext;
import com.unbxd.skipper.model.SiteRequest;
import com.unbxd.skipper.site.DAO.SiteDAO;
import com.unbxd.skipper.site.exception.SiteNotFoundException;
import com.unbxd.skipper.site.exception.ValidationException;
import com.unbxd.skipper.site.model.*;
import com.unbxd.skipper.site.service.SiteService;
import com.unbxd.skipper.states.ServeState;
import com.unbxd.skipper.states.dao.StateDao;
import com.unbxd.skipper.states.model.ServeStateType;
import com.unbxd.skipper.states.model.StateContext;
import com.unbxd.skipper.states.statemanager.StateManager;
import lombok.extern.log4j.Log4j2;

import java.util.*;

import static com.unbxd.pim.workflow.service.WorkflowProcessor.UN_SSO_UID;
import static com.unbxd.skipper.states.dao.StateDao.MONGO_ID;
import static com.unbxd.skipper.states.model.ServeStateType.CREATE_SITE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Log4j2
public class SiteServiceImpl implements SiteService {
    private SiteDAO siteDAO;
    private StateDao stateDao;
    private StateManager stateManager;
    private int maximumLengthForSiteName;

    @Inject
    public SiteServiceImpl(Config config, StateDao stateDao,
                           StateManager stateManager,
                           SiteDAO siteDAO) {
        this.siteDAO = siteDAO;
        this.stateDao = stateDao;
        this.stateManager = stateManager;
        // 40 is kept considering the maximum length of database name in mongo
        maximumLengthForSiteName = Integer.parseInt(
                config.getProperty("maximumLengthForSiteName", "40"));
    }

    @Override
    public DataCenterData getDataCenterData(Map<String, String> providers) {
        DataCenterData datacenters = siteDAO.getDataCenterData();
        List<DataCenter> dcs = new ArrayList<>();
        if(providers.size() == 0)
            return datacenters;
        for(String provider: providers.keySet()) {
            for(DataCenter availableDC: datacenters.getDataCenters()) {
                if(availableDC.getName().equals(provider)
                        && availableDC.getType().toString().equals(providers.get(provider)))
                    dcs.add(availableDC);
            }
        }
        return new DataCenterData(dcs);
    }

    @Override
    public void setDataCenterData(DataCenterData dataCenterData) {
            siteDAO.setDataCenterData(dataCenterData);
    }

    @Override
    public SiteMeta getSiteMeta() {
        return siteDAO.getSiteMeta();
    }

    @Override
    public void setVerticals(List<Vertical> verticals){
        siteDAO.setVerticals(verticals);
    }

    @Override
    public void setPlatforms(List<Platform> platforms) {
        siteDAO.setPlatforms(platforms);
    }

    @Override
    public void setEnvironments(List<Environment> environments) {
        siteDAO.setEnvironments(environments);
    }

    @Override
    public void setLanguages(List<Language> languages) {
        siteDAO.setLanguages(languages);
    }


    @Override
    public StateContext getSiteStatus(String siteKey) throws SiteNotFoundException {
        String errMsg = "Error fetching state for key[" + siteKey + "]";
        try {
            StateContext stateContext = stateDao.fetchState(siteKey);
            return stateContext;
        } catch(NoSuchElementException e) {
            errMsg = "siteKey not found.";
            log.error(errMsg + " : ", e);
            throw new SiteNotFoundException();
        }
    }

    @Override
    public StateContext getSiteStatus(String fieldName,
                                      String fieldValue) throws SiteNotFoundException {
        try {
            return stateDao.fetchState(fieldName, fieldValue);
        } catch (NoSuchElementException e) {
            log.error("Error while fetching status: " + e.getMessage());
            throw new SiteNotFoundException(e.getMessage());
        }
    }

    @Override
    public StateContext getSiteById(String id) throws SiteNotFoundException {
        String errMsg = "Error fetching state for key[" + id + "]";
        try {
            StateContext stateContext = stateDao.fetchState(MONGO_ID, id);
            return stateContext;
        } catch(NoSuchElementException e) {
            errMsg = "Id not found.";
            log.error(errMsg + " : ", e);
            throw new IllegalArgumentException(errMsg);
        }
    }

    @Override
    public StateContext setTemplateId(String siteKey,
                                      String templateId) throws SiteNotFoundException {
        try {
            StateContext stateContext = stateDao.fetchState(siteKey);
            stateContext.setTemplateId(templateId);
            stateDao.saveState(stateContext);
            return stateContext;

        } catch (NoSuchElementException e) {
            log.error("Exception while trying to set" +
                    " template Id for sitekey[" + siteKey + "]: "
                    + e.getMessage());
            throw new SiteNotFoundException("Exception while trying to set" +
                    " template Id for sitekey[" + siteKey + "]: "
                    + e.getMessage());
        }
    }

    @Override
    public void start(RequestContext requestContext) {
        StateContext stateContext = requestContext.getStateContext();
        ServeState createSiteState = this.stateManager.getStateInstance(CREATE_SITE);

        createSiteState.setStateManager(stateManager);
        createSiteState.setStateContext(stateContext);
        stateContext.setServeState(createSiteState);
        stateManager.executeState(createSiteState);
    }

    @Override
    public StateContext updateContext(String siteKey,
                                      String cookie,
                                      String errors,
                                      ServeStateType serveStateType,
                                      Map<String, String> requestParams) throws SiteNotFoundException {
        try {
            ServeState serveState = this.stateManager.getStateInstance(serveStateType);
            StateContext stateContext = stateDao.fetchState(siteKey);
            stateContext.getCookie().put(UN_SSO_UID, cookie);
            serveState.setStateManager(stateManager);
            serveState.setStateContext(stateContext);
            serveState.setStateData(requestParams);
            stateContext.setErrors(errors);

            stateManager.executeState(serveState);
            return stateContext;
        } catch (NoSuchElementException e) {
            log.error("Site not found with sitekey: " + siteKey);
            throw new SiteNotFoundException();
        }
    }

    @Override
    public void setVariants(String siteKey,
                                      Boolean enableVariants) throws SiteNotFoundException {
        try {
            StateContext stateContext = stateDao.fetchState(siteKey);
            stateContext.setVariantsEnabled(enableVariants);
            stateDao.saveState(stateContext);
        } catch (NoSuchElementException e) {
            log.error("Site not found with sitekey: " + siteKey);
            throw new SiteNotFoundException();
        }
    }


    @Override
    public void validateSiteRequest(SiteRequest siteRequest) throws ValidationException {
        if(siteRequest.getName().length() > maximumLengthForSiteName) {
            throw new
                    ValidationException("Maximum site name length is " + maximumLengthForSiteName + " characters");
        }
        validateDataCenter(siteRequest.getRegions());
        validateVertical(siteRequest.getVertical());
        validateEnvironment(siteRequest.getEnvironment());
        validateLanguage(siteRequest.getLanguage());
    }

    @Override
    public void appendStateData(String siteKey, Map<String, String> data) throws SiteNotFoundException {
        StateContext stateContext = getSiteStatus(siteKey);
        if(stateContext == null || stateContext.getServeState() == null) {
            return;
        }
        if(stateContext.getServeState().getStateData() == null) {
            stateContext.getServeState().setStateData(new HashMap<>());
        }
        stateContext.getServeState().getStateData().putAll(data);
        stateDao.saveState(stateContext);
    }

    @Override
    public void removeData(String siteKey, String key, String value) throws SiteNotFoundException {
        StateContext stateContext = getSiteStatus(siteKey);
        if(stateContext.getServeState() == null ||
                !stateContext.getServeState().getStateData().containsKey(key) ||
                value == null || value.equals(stateContext.getServeState().getStateData().get(key))) {
            return;
        }
        stateContext.getServeState().getStateData().remove(key);
        stateDao.saveState(stateContext);
    }

    @Override
    public void deleteSite(String siteKey) {
        log.info("Deleting site:" + siteKey);
        stateDao.deleteSite(siteKey);
    }

    @Override
    public void reset(String siteKey, ServeState state) {
        stateDao.reset(siteKey, state);
    }

    @Override
    public void setSiteStateProperty(String siteKey,
                                     String property,
                                     String value) throws SiteNotFoundException, ValidationException {
        if (isEmpty(siteKey) || isEmpty(property) || isEmpty(value)) {
            throw new ValidationException("invalid parameters has been passed");
        }
        log.info("Updating site state: " + siteKey);
        stateDao.updateState(siteKey,
                Collections.singletonMap(property, value));

    }

    private void validateDataCenter(String region ) throws ValidationException{
        if(!siteDAO.validateDataCenter(region))
        throw new ValidationException("region field is not valid");
    }

    private void validateVertical(String vertical ) throws ValidationException {
        if(!siteDAO.validateVertical(vertical))
            throw new ValidationException("vertical field is not valid");
    }

    private void validateEnvironment(String environmentId)throws ValidationException {
        if(!siteDAO.validateEnvironment(environmentId))
            throw new ValidationException("environment field is not valid");
    }

    private void validateLanguage(String languageId)throws ValidationException {
        if(!siteDAO.validateLanguage(languageId))
            throw new ValidationException("language field is not valid");
    }

    private void validatePlatform(String platformId)throws ValidationException {
        if(!siteDAO.validatePlatform(platformId))
            throw new ValidationException("platform field is not valid");
    }
}
