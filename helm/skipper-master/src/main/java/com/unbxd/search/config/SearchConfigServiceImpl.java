package com.unbxd.search.config;

import com.unbxd.search.config.model.NEREnablingConfig;
import com.unbxd.search.exception.SearchConfigException;
import lombok.extern.log4j.Log4j2;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SearchConfigServiceImpl implements SearchConfigService {

    private interface HagridRemoteService {
        @POST("/v1.0/config/sites/{siteKey}")
        Call<Map<String, Object>> setConfig(@Path("siteKey") String siteKey,
                                   @Header("Content-Type") String contentType,
                                   @Body Map<String, Object> request);

        @POST("/skipper/v1.0/site/{siteKey}/ner/settings")
        Call<Map<String, Object>> enableNER(@Path("siteKey") String siteKey,
                                   @Header("Content-Type") String contentType,
                                   @Body NEREnablingConfig request, @Header("Cookie") String cookie);
    }
    private HagridRemoteService hagridRemoteService;

    public SearchConfigServiceImpl(String hagridUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(58, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

       Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(hagridUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build();

       hagridRemoteService  = retrofit.create(HagridRemoteService.class);
    }

    public void setVariants(String siteKey,
                             Boolean enableVariant) throws SearchConfigException {
        Map<String, Object> request = createRequest(enableVariant);
        try {
            Response<Map<String, Object>> response = hagridRemoteService.setConfig(siteKey,
                    "application/json", request).execute();
            if(!response.isSuccessful()) {
                log.error("Unable to configure variants in Hagrid , statusCode: " + response.code()
                        + " , reason: "+response.errorBody().string() + " for siteKey: " + siteKey);
                throw new SearchConfigException(response.code(),"Unable to configure variants");
            }
            String msg = enableVariant? "Variants is enabled": "Variants is disabled";
            log.info(msg);
        } catch (IOException e) {
            log.error("Error while configuring variants in Hagrid  for siteKey: " + siteKey +
                    " ,error message: "+ e.getMessage());
            throw new SearchConfigException(500,"Unable to configure variants");
        }
    }

    @Override
    public void enableNER(String siteKey, List<String> vertical, String cookie) throws SearchConfigException {
        try {
            Response<Map<String, Object>> response = hagridRemoteService.
                    enableNER(siteKey, "application/json", new NEREnablingConfig(vertical), cookie).
                    execute();
            if(!response.isSuccessful()) {
                log.error("Unable to enable NER in Hagrid , statusCode: " + response.code()
                        + " , reason: "+response.errorBody().string() + " for siteKey: " + siteKey);
                throw new SearchConfigException(response.code(), "Unable to enable NER");
            }
            log.info("NER enabled successfully for site:" + siteKey);
        } catch (IOException e) {
            log.error("Error while enable NER in Hagrid  for siteKey: " + siteKey +
                    " ,error message: "+ e.getMessage());
            throw new SearchConfigException(500,"Unable to configure NER");
        }
    }


    private Map<String, Object> createRequest(Boolean enableVariants) {
        /*
            {
                "default": [{
                    "variants": true
                }]
            }
        */
        Map<String, Object> rootObject = new HashMap<>();
        List<Object> defaultVariantsConfig = new ArrayList<>();
        Map<String, Object>  variantsConfig = new HashMap<>();
        variantsConfig.put("variants", enableVariants);
        variantsConfig.put("variantType", "search");
        defaultVariantsConfig.add(variantsConfig);
        rootObject.put("global",defaultVariantsConfig);
        return rootObject;
    }

}
