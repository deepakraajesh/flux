package com.unbxd.pim.workflow.service.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.unbxd.pim.exception.PIMException;
import com.unbxd.pim.workflow.service.PimSearchApp;
import com.unbxd.pim.workflow.service.PIMService;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;

@Log4j2
public class PimServiceImpl implements PIMService {

    PimSearchApp pimSearchApp;
    @Inject
    public PimServiceImpl(PimSearchApp pimSearchApp) {
        this.pimSearchApp = pimSearchApp;
    }

    @Override
    public String triggerFullUpload(String siteKey) throws PIMException {
        JsonObject req = new JsonObject();
        req.addProperty("identifier", siteKey);
        req.addProperty("appCustomId", APP_ID);
        try {
            Response<JsonObject> response = pimSearchApp.triggerFullUpload(req).execute();
            if(!response.isSuccessful()) {
                String msg = "Internal Error while triggering full indexing with PimSearchApp";
                log.error(msg + " for site: " + siteKey + " code:" + response.code()
                        + " reason: " + response.errorBody().string());
                throw new PIMException(msg);
            }
            if(response.body().has("feed_id")) {
                return response.body().getAsJsonPrimitive("feed_id").getAsString();
            }
            return null;
        } catch (IOException e) {
            String msg = "Internal Error while triggering full indexing with PimSearchApp";
            log.error(msg + " for site " + siteKey + " reason: " + e.getMessage());
            throw new PIMException(msg);
        }
    }

    @Override
    public void setGroupByParentProperty(String siteKey,
                                         Boolean value) throws PIMException {
        JsonObject request = new JsonObject();
        request.addProperty("identifier", siteKey);
        request.addProperty("appCustomId", APP_ID);
        request.addProperty("group_by_parent",value);
        try {
            Response<JsonObject> response = pimSearchApp.setGroupByParent(request).execute();
            if(!response.isSuccessful()) {
                String msg = "Internal Error while enabling variants in  PimSearchApp";
                log.error(msg + " for site: " + siteKey + " code:" + response.code()
                        + " reason: " + response.errorBody().string());
                // ignore if status code is 404
                if(response.code() != 404)
                    throw new PIMException(msg);
            }
        } catch (IOException e) {
            String msg = "Internal Error while enabling variants in  PimSearchApp";
            log.error(msg + " for site " + siteKey + " reason: " + e.getMessage());
            throw new PIMException(msg);
        }
    }
}

