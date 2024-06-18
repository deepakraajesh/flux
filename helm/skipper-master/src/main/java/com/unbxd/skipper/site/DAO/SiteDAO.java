package com.unbxd.skipper.site.DAO;

import com.unbxd.skipper.site.model.*;

import java.util.List;

public interface SiteDAO {
    DataCenter getDataCenter(String region);
    DataCenterData getDataCenterData();
    void setDataCenterData(DataCenterData dataCenterData);
    SiteMeta getSiteMeta();
    void setVerticals(List<Vertical> verticals);
    void setPlatforms(List<Platform> platforms);
    void setEnvironments(List<Environment> environments);
    void setLanguages(List<Language> languages);
    boolean validateDataCenter(String regionName);
    boolean validateVertical(String verticalName);
    boolean validateEnvironment(String environmentId);
    boolean validateLanguage(String languageId);
    boolean validatePlatform(String platformId);

}
