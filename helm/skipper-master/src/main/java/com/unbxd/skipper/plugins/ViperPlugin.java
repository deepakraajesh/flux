package com.unbxd.skipper.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbxd.field.exception.FieldException;
import com.unbxd.field.model.SiteKeyCred;
import com.unbxd.field.service.FieldService;
import com.unbxd.skipper.plugins.exception.PluginException;
import com.unbxd.skipper.plugins.model.PluginInstallReq;
import com.unbxd.skipper.plugins.model.PluginVariant;
import com.unbxd.skipper.plugins.model.ViperRedirectResp;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.unbxd.skipper.ErrorCode.EmptyResponseFromDownStream;
import static com.unbxd.skipper.ErrorCode.UnsuccessfulResponseFromDownStream;

@Log4j2
public class ViperPlugin implements Plugin {

    private interface ViperRemoteService {
        @GET("/setup/{plugin}/install/")
        Call<ViperRedirectResp> redirectURL(@Path("plugin") String plugin,
                                            @Query("site_key") String siteKey,
                                            @Query("region") String region,
                                            @Query("db_id") String dbId,
                                            @Query("shop") String query);

        @POST("/setup/{plugin}/update-search-props/")
        Call<Void> install(@Path("plugin") String plugin, @Body PluginInstallReq req);
        @POST("/setup/{plugin}/update-search-/")
        Call<Void> linkToUnbxd(@Path("plugin") String plugin, @Body PluginInstallReq req);

        @PUT("/setup/app/api/v2/search/{siteKey}/variants")
        Call<Void> setVariants(@Path("siteKey") String siteKey, @Body PluginVariant req);


    }

    private ViperRemoteService viperService;

    private FieldService fieldService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String OAUTH_REDIRECT_URL_PROPERTY_NAME = "oauth_endpoint";

    public ViperPlugin(String viperUrl, FieldService fieldService) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(58, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(viperUrl)
                .client(okHttpClient)
                .build();

        viperService = retrofit.create(ViperRemoteService.class);
        this.fieldService = fieldService;
    }

    @Override
    public String redirectURL(String siteKey, String region,
                              String dbDocId, String shopName, String plugin) throws PluginException {
        try {
            Response<ViperRedirectResp> response = viperService.redirectURL(plugin, siteKey, region, dbDocId, shopName)
                    .execute();
            if(!response.isSuccessful()) {
                String msg = "Error reaching shopify plugin";
                String errString = response.errorBody().string();
                log.error(msg + " statusCode: " + response.code() + " reason: " + errString);
                if(response.code() == 400) {
                    ViperRedirectResp errorBody = mapper.readValue(errString, ViperRedirectResp.class);
                    StringBuilder sb = new StringBuilder();
                    for(String error: errorBody.getErrors()) {
                        sb.append(error);
                    }
                    String errorMsg = sb.toString();
                    throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), errorMsg);
                }
                throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
            }
            Map<String, String> respBody = response.body().getData();

            if(respBody == null || !respBody.containsKey(OAUTH_REDIRECT_URL_PROPERTY_NAME)) {
                String msg = "Improper response from plugin";
                log.error(msg + " reason: " + OAUTH_REDIRECT_URL_PROPERTY_NAME + " is not present in response ");
                throw new PluginException(EmptyResponseFromDownStream.getCode(), msg);
            }
            return respBody.get(OAUTH_REDIRECT_URL_PROPERTY_NAME);
        } catch (IOException e) {
            String msg = "Error reaching shopify plugin";
            log.error(msg + " reason: " + e.getMessage());
            throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
        }
    }

    private void install(String plugin, PluginInstallReq req) throws PluginException {
        try {
            Response<Void> response = viperService.install(plugin, req)
                    .execute();
            if(!response.isSuccessful()) {
                String msg = "Error reaching " + plugin + " plugin";
                log.error(msg + " statusCode: " + response.code() + " reason: " + response.errorBody().string());
                throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
            }
        } catch (IOException e) {
            String msg = "Error reaching " + plugin + " plugin";
            log.error(msg + " reason: " + e.getMessage());
            throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
        }
    }

    @Override
    public void install(String siteId, String shopName, String siteKey, String region, String plugin)
            throws PluginException {
        install(null,
                siteId, shopName, siteKey, region, plugin, null);
    }

    @Override
    public void install(String dbDocId, String siteId, String shopName, String siteKey, String region, String plugin, String appToken)
            throws PluginException {
        try {
            SiteKeyCred cred = fieldService.getSiteDetails(siteKey);
            PluginInstallReq req = new PluginInstallReq(dbDocId, siteId, shopName, plugin, siteKey,
                    region, cred.getApiKey(), cred.getSecretKey(), appToken);
            install(plugin, req);
        } catch (FieldException e) {
            String msg = "Error reaching " + plugin + " plugin";
            log.error(msg + " reason: " + e.getMessage());
            throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
        }
    }

    @Override
    public void setVariants(String siteKey, Boolean enableVariants) throws PluginException {
        try {
            Response<Void> response = viperService.setVariants(siteKey, new PluginVariant(enableVariants)).execute();
            if(!response.isSuccessful()) {
                String msg = "Unable to set variants in plugin";
                log.error(msg + " statusCode: " + response.code() + " reason: " + response.errorBody().string());
                throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
            }
        } catch (IOException e) {
            String msg = "Error reaching viper plugin";
            log.error(msg + " reason: " + e.getMessage());
            throw new PluginException(UnsuccessfulResponseFromDownStream.getCode(), msg);
        }
    }
}

