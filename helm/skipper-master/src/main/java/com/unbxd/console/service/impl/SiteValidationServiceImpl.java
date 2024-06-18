package com.unbxd.console.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.unbxd.config.Config;
import com.unbxd.console.exception.ValidationException;
import com.unbxd.console.service.FacetRemoteService;
import com.unbxd.console.service.SiteValidationService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

@Log4j2
public class SiteValidationServiceImpl implements SiteValidationService {

    private Config config;
    private FacetRemoteService facetRemoteService;
    private static final String KEY_DELIMITER = "#";
    private LoadingCache<String, Boolean> validationCache;

    @Inject
    public SiteValidationServiceImpl(Config config,
                                     FacetRemoteService facetRemoteService) {
        this.config = config;
        this.facetRemoteService = facetRemoteService;
        this.validationCache = getValidationCache();
    }

    @Override
    public boolean isSiteValid(String cookie, String siteKey) {
        try {
            return validationCache.get(formKey(cookie, siteKey));
        } catch(ExecutionException e) {
            log.error("Unable to validate siteKey: " + siteKey);
        }
        return false;
    }

    private LoadingCache<String, Boolean> getValidationCache() {
        int cacheSize = parseInt(config.getProperty("validation.cache.size", "10000"));
        return CacheBuilder
                .newBuilder()
                .maximumSize(cacheSize)
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build(new CacheLoader<>() {
                    @Override
                    public Boolean load(String validationKey) throws ValidationException {
                        int delimiterIndex = validationKey.indexOf(KEY_DELIMITER);
                        String cookie = validationKey.substring(0, delimiterIndex);
                        String siteKey = validationKey.substring(delimiterIndex + 1);
                        Call<Object> validationCallObj = facetRemoteService.validateSite(cookie, siteKey);
                        try {
                            Response<Object> validationResponse = validationCallObj.execute();
                            int statusCode = validationResponse.code();
                            if (statusCode == 200) {
                                return Boolean.TRUE;
                            } else {
                                log.error("Validation failed for siteKey: " + siteKey + " with code: "
                                        + statusCode + " , error from console: " + validationResponse
                                        .errorBody().string());
                                throw new ValidationException("SiteKey: " + siteKey +
                                        " not found in validation cache");
                            }
                        } catch (IOException e) {
                            log.error("Error while trying to update validation cache for siteKey: "
                                    + siteKey, e);
                            throw new ValidationException("Error while trying to update validation" +
                                    " cache for siteKey: " + siteKey, e);
                        }
                    }
                });
    }

    private String formKey(String cookie, String siteKey) {
        return cookie.concat(KEY_DELIMITER).concat(siteKey);
    }
}
