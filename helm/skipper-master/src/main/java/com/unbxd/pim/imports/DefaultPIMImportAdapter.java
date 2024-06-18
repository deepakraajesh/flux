package com.unbxd.pim.imports;

import com.google.inject.Inject;
import com.unbxd.pim.AbstractPIMResponse;
import com.unbxd.pim.exception.PIMException;
import com.unbxd.pim.imports.model.ImportProperties;
import com.unbxd.pim.imports.model.PageParam;
import lombok.extern.log4j.Log4j2;
import retrofit2.Response;

import java.io.IOException;

@Log4j2
public class DefaultPIMImportAdapter implements PIMImportAdapter {

    private PIMRemoteImportService importService;


    @Inject
    public DefaultPIMImportAdapter(PIMRemoteImportService importService) {
        this.importService = importService;
    }

    @Override
    public ImportProperties importProperties(String cookie, String orgId, String adapterId, long page, long count)
            throws PIMException {
        try {
            Response<AbstractPIMResponse<ImportProperties>> response =
                    importService.importProperties(cookie,orgId, adapterId, new PageParam(page, count)).execute();
            if(!response.isSuccessful() || !response.body().isDataCorrect()) {
                String msg = "Error while fetching import properties";
                log.error(msg + " orgId: " + orgId + " adapaterId: " + adapterId +
                        " status: " + response.code() + " msg: " + response.errorBody().string());
                throw new PIMException(msg);
            }
            return response.body().getData();
        } catch (IOException e) {
            String msg = "Error while fetching import properties";
            log.error(msg + " orgId: " + orgId + " adapaterId: " + adapterId + " reason: " + e.getMessage());
            throw new PIMException(msg);
        }
    }
}

