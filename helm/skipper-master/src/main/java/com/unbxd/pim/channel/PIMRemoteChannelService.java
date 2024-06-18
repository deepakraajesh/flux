package com.unbxd.pim.channel;

import com.unbxd.pim.AbstractPIMResponse;
import com.unbxd.pim.channel.model.ExportPropertiesRequest;
import com.unbxd.pim.channel.model.PIMExportMapping;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface PIMRemoteChannelService {

    @GET("api/v1/internal/{orgId}/networks/adapters/{adapterId}")
    Call<AbstractPIMResponse<PIMExportMapping>> fetchMapping(@Header("Cookie") String cookie,
                                                            @Path("orgId") String orgId,
                                                            @Path("adapterId") String adapterId);

    @PATCH("api/v2/internal/{orgId}/networks/adapters/{adapterId}")
    Call<AbstractPIMResponse<Map<String, Object>>> updateMapping(@Header("Cookie") String cookie,
                                                                 @Path("orgId") String orgId,
                                                                 @Path("adapterId") String adapter,
                                                                 @Body ExportPropertiesRequest request);
}

