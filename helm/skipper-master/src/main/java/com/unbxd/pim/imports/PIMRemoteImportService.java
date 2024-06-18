package com.unbxd.pim.imports;

import com.unbxd.pim.AbstractPIMResponse;
import com.unbxd.pim.imports.model.ImportProperties;
import com.unbxd.pim.imports.model.PageParam;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PIMRemoteImportService {
    // api/v4/internal/:orgId/imports/:importId/mappings
    @POST("api/v4/internal/{orgId}/imports/{importId}/mappings")
    Call<AbstractPIMResponse<ImportProperties>> importProperties(@Header("Cookie") String cookie,
                                                                 @Path("orgId") String orgId,
                                                                 @Path("importId") String importId,
                                                                 @Body PageParam page);
}

