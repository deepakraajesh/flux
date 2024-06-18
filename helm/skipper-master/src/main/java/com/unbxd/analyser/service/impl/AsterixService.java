package com.unbxd.analyser.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.model.Concepts;
import com.unbxd.analyser.model.StopWords;
import com.unbxd.analyser.model.UpdateConceptsRequest;
import com.unbxd.analyser.model.UpdateStopWordsRequest;
import com.unbxd.analyser.model.core.CoreConfig;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.cbo.response.DefaultResponse;
import com.unbxd.cbo.response.Error;
import com.unbxd.skipper.ErrorCode;
import com.unbxd.skipper.dictionary.model.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.*;
import retrofit2.http.Query;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;

import java.awt.image.DataBuffer;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public class AsterixService implements AnalyserService {

    private Retrofit retrofit;
    private AsterixRemoteService service;
    public static final String MANDATORY_TERMS_ASSET_NAME = "mandatory.txt";
    public static final String STOP_WORDS_ASSET_NAME = "stopwords.txt";
    public static final String SYNONYMS_ASSET_NAME = "synonyms.txt";
    public static final String MULTIWORDS_ASSET_NAME = "multiwords.txt";
    public static final String NO_STEM_ASSET_NAME = "stemdict.txt";

    public static final String MANDATORY_TERMS_ASSET_NAME_V2 = "mandatory";
    public static final String STOP_WORDS_ASSET_NAME_V2 = "stopwords";
    public static final String SYNONYMS_ASSET_NAME_V2 = "synonyms";
    public static final String MULTIWORDS_ASSET_NAME_V2 = "multiwords";
    public static final String NO_STEM_ASSET_NAME_V2 = "stemdict";

    public static final String FILE_URL = "fileUrl";

    private static final String DEFAULT_CORE_NAME = "default_core"; // default core name of english
    private static final String UNBXD_REQUEST_ID_HEADER_NAME = "Unbxd-Request-Id";
    private static final String SUGGESTED = "suggested";

    private interface AsterixRemoteService {
        @GET("/cores/{coreName}/assets/{assetName}")
        Call<String> getAssetContents(@Path("coreName") String siteKey, @Path("assetName") String assetName);

        @GET("/cores/{coreName}/assets/{assetName}")
        Call<ResponseBody> bulkDownloadAsset(@Path("coreName") String siteKey, @Path("assetName") String assetName);

        @POST("/cores/{coreName}/assets/{assetName}")
        Call<String> updateAsset(@Path("coreName") String siteKey, @Path("assetName") String assetName, @Body String requestBody);

        @POST("/site/{coreName}/dictionary/{assetName}/bulk?version=v2&flushAll=true")
        Call<String> bulkUpdateAsset(@Path("coreName") String siteKey, @Path("assetName") String assetName,
                                     @Query("type") String type,
                                     @Body String requestBody,
                                     @Header("Content-Type") String contentType,
                                     @Header(UNBXD_REQUEST_ID_HEADER_NAME) String requestId);

        @PUT("/cores/{coreName}")
        Call<String> createAsterixCore(@Path("coreName") String siteKey, @Query("defaultCorename") String defaultCoreName);

        @GET("/cores/{coreName}/asterixConfig")
        Call<String> getAsterixConfig(@Path("coreName") String siteKey);

        @GET("/cores/{coreName}/config")
        Call<CoreConfig> getConfig(@Path("coreName") String siteKey);

        @POST("/cores/{coreName}/coreConfig")
        Call<String> updateConfig(@Path("coreName") String siteKey, @Body CoreConfig coreConfig);

        @POST("/cores/{coreName}/version/{version}")
        Call<String> updateVersion(@Path("coreName") String siteKey, @Path("version") String version);

        @GET("/cores/{coreName}/analyzers/search")
        Call<String> analyse(@Path("coreName") String siteKey, @Query("query") String query);

        @GET("/site/{coreName}/dictionary/{dictionaryName}/download")
        Call<ResponseBody> download(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                    @Query("type") String type, @Query("includeId") boolean isIdNeeded);

        @POST("/site/{coreName}/dictionary/{dictionaryName}")
        Call<DefaultResponse<DictionaryData>> addDictionaryData(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                                                @Query("type") DictionaryType type, @Body DictionaryData requestBody);

        @GET("/site/{coreName}/dictionary/{dictionaryName}")
        Call<DefaultResponse<DictionaryData>> getDictionaryData(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                                                @Query("page") int page, @Query("count") int count, @Query("type") DictionaryType type,
                                                                @Query("search") String search);

        @HTTP(method = "DELETE", path = "/site/{coreName}/dictionary/{dictionaryName}", hasBody = true)
        Call<DefaultResponse<DictionaryData>> deleteDictionaryData(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                                                   @Query("type") DictionaryType type, @Body DictionaryData requestBody);

        @PATCH("/site/{coreName}/dictionary/{dictionaryName}")
        Call<DefaultResponse<DictionaryData>> updateDictionaryData(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                                                   @Query("type") DictionaryType type, @Body DictionaryData requestBody);

        @POST("/site/{coreName}/dictionary/{dictionaryName}/bulk")
        Call<DefaultResponse<DictionaryData>> bulkUploadData(@Path("coreName") String siteKey, @Path("dictionaryName") String dictionaryName,
                                                             @Query("type") DictionaryType type, @Query("flushAll") boolean flushAll,
                                                             @Body BulkUploadRequest requestBody);

        @GET("/site/{coreName}/dictionary/{dictionaryName}/count")
        Call<DefaultResponse<DictionaryCount>> getDictionaryCount(@Path("coreName") String siteKey,
                                                                  @Path("dictionaryName") String dictionaryName);
    }

    @Inject
    public AsterixService(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client).build();

        service = retrofit.create(AsterixService.AsterixRemoteService.class);
    }

    @Override
    public Concepts getConcepts(String siteKey) throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(siteKey, MANDATORY_TERMS_ASSET_NAME).execute();
            if (response.isSuccessful()) {
                List<String> parsedConcepts = parseAsset(response.body());
                Concepts result = new Concepts();
                result.setNoOfDefaultConcepts(getDefaultConcepts().size());
                result.setManualConcepts(parsedConcepts);
                return result;
            } else {
                log.error("Get concepts failed to due to: " + response.errorBody() + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get concepts");
            }
        } catch (IOException e) {
            String msg = "Unable to get concepts";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void updateConcepts(String siteKey, UpdateConceptsRequest updateConceptsRequest) throws AnalyserException {
        try {
            String requestBody;
            if (updateConceptsRequest.isAddDefaultConcepts())
                requestBody = buildUpdateAssetRequest(updateConceptsRequest.getManualConcepts(), getDefaultConceptsInString());
            else
                requestBody = buildUpdateAssetRequest(updateConceptsRequest.getManualConcepts());
            Response<String> response = service.updateAsset(siteKey, MANDATORY_TERMS_ASSET_NAME, requestBody).execute();
            if (!response.isSuccessful()) {
                log.error("Error while updating concepts/mandatory terms in Asterix , statusCode: " + response.code()
                        + " , reason: " + response.errorBody() + " for siteKey: " + siteKey);
                throw new AnalyserException(response.code(), "Unable to update concepts");
            }
        } catch (IOException e) {
            String msg = "Unable to update concepts";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public StopWords getStopWords(String siteKey) throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(siteKey, STOP_WORDS_ASSET_NAME).execute();
            if (response.isSuccessful()) {
                List<String> parsedStopWords = parseAsset(response.body());
                StopWords result = new StopWords();
                result.setNoOfDefaultStopWords(getDefaultStopWords().size());
                result.setManualStopWords(parsedStopWords);
                return result;
            } else {
                log.error("Get stopWords failed with code " + response.code()
                        + " reason: " + response.errorBody().string() + " siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get stopWords");
            }
        } catch (IOException e) {
            String msg = "Unable to get stopWords";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void updateStopWords(String siteKey, UpdateStopWordsRequest updateStopWordsRequest) throws AnalyserException {
        try {
            String requestBody;
            if (updateStopWordsRequest.isAddDefaultStopWords())
                requestBody = buildUpdateAssetRequest(updateStopWordsRequest.getManualStopWords(), getDefaultStopWordsInString());
            else
                requestBody = buildUpdateAssetRequest(updateStopWordsRequest.getManualStopWords());
            Response<String> response = service.updateAsset(siteKey, STOP_WORDS_ASSET_NAME, requestBody).execute();
            if (!response.isSuccessful()) {
                log.error("Error while updating stopWords terms in Asterix , statusCode: " + response.code()
                        + " , reason: " + response.errorBody() + " for siteKey: " + siteKey);
                throw new AnalyserException(response.code(), "Unable to update stopWords");
            }
            log.info("update stop words , response from asterix: " + response.body());
        } catch (IOException e) {
            String msg = "Unable to update stopWords";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void createAnalyserCore(String siteKey) throws AnalyserException {
        try {
            Response response = service.createAsterixCore(siteKey, DEFAULT_CORE_NAME).execute();
            if (!response.isSuccessful()) {
                log.error("Error while creating Asterix core , statusCode: " + response.code()
                        + " , reason: " + response.errorBody() + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to create Asterix core");
            }
        } catch (IOException e) {
            log.error("Error while creating Asterix core , error message:" + e.getMessage()
                    + " for site:" + siteKey);
            throw new AnalyserException(500, "Unable to create Asterix core");
        }
    }

    @Override
    public List<String> getDefaultConcepts() throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(DEFAULT_CORE_NAME, MANDATORY_TERMS_ASSET_NAME).execute();
            return parseAsset(response.body());
        } catch (IOException e) {
            log.error("Error while fetching default mandatory terms from asterix , ", e);
            throw new AnalyserException(500, "Unable to get concepts");
        }
    }

    @Override
    public List<String> getDefaultStopWords() throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(DEFAULT_CORE_NAME, STOP_WORDS_ASSET_NAME).execute();
            return parseAsset(response.body());
        } catch (IOException e) {
            log.error("Error while fetching default stop words from asterix , ", e);
            throw new AnalyserException(500, "Unable to get stopWords");
        }
    }

    private String getDefaultConceptsInString() throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(DEFAULT_CORE_NAME, MANDATORY_TERMS_ASSET_NAME).execute();
            return response.body();
        } catch (IOException e) {
            log.error("Error while fetching default mandatory terms from asterix , ", e);
            throw new AnalyserException(500, "Unable to get concepts");
        }
    }

    private String getDefaultStopWordsInString() throws AnalyserException {
        try {
            Response<String> response = service.getAssetContents(DEFAULT_CORE_NAME, STOP_WORDS_ASSET_NAME).execute();
            return response.body();
        } catch (IOException e) {
            log.error("Error while fetching default stop words from asterix , ", e);
            throw new AnalyserException(500, "Unable to get stopWords");
        }
    }

    private List<String> parseAsset(String assetString) {
        List<String> parsedAsset = Arrays.asList(assetString.split("\n"));
        parsedAsset.removeIf(str -> str.matches("#(.*)"));
        return parsedAsset;
    }

    private String buildUpdateAssetRequest(List<String> assetContents) {
        StringBuilder result = new StringBuilder();
        assetContents.forEach(s -> result.append(s).append("\n"));
        result.setLength(result.length() - 1); // remove "\n" at the end
        return result.toString();
    }

    private String buildUpdateAssetRequest(List<String> assetContents, String defaultAssetContents) {
        StringBuilder result = new StringBuilder().append(defaultAssetContents);
        result.setLength(result.length() - 1);  // remove "\n" at the end
        assetContents.forEach(s -> result.append("\n").append(s));
        return result.toString();
    }

    @Override
    public void updateAsset(String siteKey, String assetName, String request) throws AnalyserException {
        try {
            Response<String> response = service.updateAsset(siteKey, assetName, request).execute();
            if (!response.isSuccessful()) {
                log.error("Error while updating assets in Asterix , statusCode: " + response.code()
                        + " , reason: " + response.errorBody().string()
                        + " for siteKey:" + siteKey + " asset " + assetName);
                throw new AnalyserException(response.code(), "Unable to update " + assetName);
            }
        } catch (IOException e) {
            log.error("Error while updating for siteKey:" + siteKey
                    + " with reason " + e.getMessage() + " assetName:" + assetName);
            throw new AnalyserException(500, "Error while updating " + assetName);
        }

    }

    @Override
    public void bulkUpdateAsset(String siteKey,
                                String assetName,
                                String type,
                                String s3Location) throws AnalyserException {
        try {
            RouteContext routeContext = RouteDispatcher.getRouteContext();
            Response<String> response = service.bulkUpdateAsset(siteKey, assetName, type, createRequest(s3Location),
                    "application/json", routeContext.getResponse().getHeader(UNBXD_REQUEST_ID_HEADER_NAME)).execute();
            if (!response.isSuccessful()) {
                String reason = nonNull(response.errorBody()) ? response.errorBody().string() : "";
                log.error("Error while bulk updating assets in Asterix ,statusCode: " + response.code()
                        + " , reason: " + reason
                        + " for siteKey:" + siteKey + " asset " + assetName);
                throw new AnalyserException(response.code(), "Unable to update " + assetName);
            }
            JsonObject responseInJson = JsonParser.parseString(response.body()).getAsJsonObject();
            if (responseInJson.has("errors")) {
                log.error("Error while bulk updating assets in Asterix , error response:" +
                        responseInJson.toString() + " siteKey:" + siteKey + " asset: " + assetName);
                throw new AnalyserException(500, "Unable to update " + assetName);
            }
        } catch (IOException e) {
            log.error("Error while bulk updating assets for siteKey:" + siteKey
                    + " with reason " + e.getMessage() + " assetName:" + assetName);
            throw new AnalyserException(500, "Error while updating " + assetName);
        }
    }

    private String createRequest(String s3Location) {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty(FILE_URL, s3Location);
        return rootObject.toString();
    }

    @Override
    public InputStream bulkDownloadAsset(String siteKey, String assetName) throws AnalyserException {
        try {
            Response<ResponseBody> response = service.bulkDownloadAsset(siteKey, assetName).execute();
            if (response.isSuccessful()) {
                return response.body().byteStream();
            } else {
                String reason = nonNull(response.errorBody()) ? response.errorBody().string() : "";
                log.error("Get asests failed to due to: " + reason + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Get asests failed");
            }
        } catch (IOException e) {
            String msg = "Get asests failed";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public String getVersion(String siteKey) throws AnalyserException {
        try {
            Response<String> response = service.getAsterixConfig(siteKey).execute();
            if (response.isSuccessful()) {
                JsonObject res = JsonParser.parseString(response.body()).getAsJsonObject();
                return res.getAsJsonObject("data").get("version").getAsString();
            } else {
                String reason = nonNull(response.errorBody()) ? response.errorBody().string() : "";
                log.error("Get config failed to due to: " + reason + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get asterix config");
            }

        } catch (IOException e) {
            String msg = "Error while fetching  asterix version";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public CoreConfig getConfig(String siteKey) throws AnalyserException {
        try {
            Response<CoreConfig> response = service.getConfig(siteKey).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                log.error("Get config failed to due to: " + response.errorBody() + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get asterix config");
            }

        } catch (IOException e) {
            String msg = "Error while fetching analyser config";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void updateConfig(String siteKey, CoreConfig coreConfig) throws AnalyserException {
        try {
            Response<String> response = service.updateConfig(siteKey, coreConfig).execute();
            if (!response.isSuccessful()) {
                log.error("Update config failed to due to: " + response.errorBody().string() + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get asterix config");
            }
        } catch (IOException e) {
            String msg = "Error while updating analyser config";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void updateVersion(String siteKey, String version) throws AnalyserException {
        try {
            Response<String> response = service.updateVersion(siteKey, version).execute();
            if (!response.isSuccessful()) {
                log.error("Update config failed to due to: " + response.errorBody() + " for siteKey:" + siteKey);
                throw new AnalyserException(response.code(), "Unable to get asterix config");
            }
        } catch (IOException e) {
            String msg = "Error while updating analyser config";
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public String analyse(String siteKey, String query) throws AnalyserException {
        try {
            Response<String> response = service.analyse(siteKey, query).execute();
            if (!response.isSuccessful()) {
                log.error("Analysing query failed to due to: " + response.errorBody().string());
                throw new AnalyserException(response.code(), ErrorCode.UnsuccessfulResponseFromDownStream.getCode()
                        , "Analysing query failed");
            }
            if (isNull(response.body())) {
                log.error("Empty response from asterix service");
                throw new AnalyserException(response.code(), ErrorCode.EmptyResponseFromDownStream.getCode()
                        , ErrorCode.EmptyResponseFromDownStream.getMessage());
            }
            return response.body();
        } catch (IOException e) {
            String msg = "Error while analysing query :" + query;
            log.error(msg + " for siteKey:" + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public File download(String siteKey, String dictionaryName, String type, boolean isIdNeeded)
            throws AnalyserException {
        try {
            Response<ResponseBody> response = service.download(siteKey, dictionaryName, type, isIdNeeded).execute();
            if (!response.isSuccessful()) {
                log.error("Analysing query failed to due to: " + response.errorBody().string());
                throw new AnalyserException(response.code(), ErrorCode.UnsuccessfulResponseFromDownStream.getCode()
                        , "Analysing query failed");
            }
            return writeResponseBodyToDisk(response.body());
        } catch (IOException e) {
            String msg = "Error while downloading dictionary file from asterix";
            log.error(msg + " siteKey: " + siteKey + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    @Override
    public void addDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException {
        String msg = "Error while adding dictionary data into asterix siteKey: "
                + dictionaryContext.getSiteKey() + " reason: ";
        try {
            Response<DefaultResponse<DictionaryData>> response = service.addDictionaryData(
                    dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName(),
                    dictionaryContext.getType(),
                    dictionaryContext.getDictionaryData()).execute();

            //Ignoring the return value not needed for this call
            validateResponse(response, msg);
        } catch (IOException e) {
            log.error(msg + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(),
                    "Error while adding dictionary data");
        }
    }

    @Override
    public DictionaryData getDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException {
        String msg = "Error while getting dictionary data from asterix siteKey: "
                + dictionaryContext.getSiteKey() + " reason: ";
        try {
            Response<DefaultResponse<DictionaryData>> response = service.getDictionaryData(
                    dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName(),
                    dictionaryContext.getPage(),
                    dictionaryContext.getCount(),
                    dictionaryContext.getType(),
                    dictionaryContext.getSearch()).execute();

            return validateResponse(response, msg);
        } catch (IOException e) {
            log.error(msg + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(),
                    "Error while getting dictionary data");
        }
    }

    @Override
    public void deleteDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException {
        String msg = "Error while deleting dictionary data from asterix siteKey: "
                + dictionaryContext.getSiteKey() + " reason: ";
        try {
            Response<DefaultResponse<DictionaryData>> response = service.deleteDictionaryData(
                    dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName(),
                    dictionaryContext.getType(),
                    dictionaryContext.getDictionaryData()).execute();

            //Ignoring the return value not needed for this call
            validateResponse(response, msg);
        } catch (IOException e) {
            log.error(msg + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(),
                    "Error while deleting dictionary data");
        }
    }

    @Override
    public void updateDictionaryData(DictionaryContext dictionaryContext) throws AnalyserException {
        String msg = "Error while updating dictionary data into asterix siteKey: "
                + dictionaryContext.getSiteKey() + " reason: ";
        try {
            Response<DefaultResponse<DictionaryData>> response = service.updateDictionaryData(
                    dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName(),
                    dictionaryContext.getType(),
                    dictionaryContext.getDictionaryData()).execute();

            //Ignoring the return value not needed for this call
            validateResponse(response, msg);
        } catch (IOException e) {
            log.error(msg + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(),
                    "Error while updating dictionary data");
        }
    }

    @Override
    public void bulkUploadData(DictionaryContext dictionaryContext) throws AnalyserException {
        String msg = "Error while bulk uploading dictionary data to asterix siteKey: "
                + dictionaryContext.getSiteKey() + " reason: ";
        try {
            BulkUploadRequest bulkUploadRequest = new BulkUploadRequest() {{
                setFileUrl(dictionaryContext.getS3fileUrl());
            }};
            Response<DefaultResponse<DictionaryData>> response = service.bulkUploadData(
                    dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName(),
                    dictionaryContext.getType(),
                    dictionaryContext.isFlushAll(),
                    bulkUploadRequest).execute();

            //Ignoring the return value not needed for this call
            validateResponse(response, msg);
        } catch (IOException e) {
            log.error(msg + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(),
                    "Error while bulk uploading dictionary data");
        }
    }

    @Override
    public DictionaryCount getDictionaryCount(DictionaryContext dictionaryContext) throws AnalyserException {
        try {
            Response<DefaultResponse<DictionaryCount>> response = service.getDictionaryCount(dictionaryContext.getSiteKey(),
                    dictionaryContext.getDictionaryName()).execute();
            if (!response.isSuccessful() || !isNull(response.body().getErrors())) {
                DefaultResponse<DictionaryCount> errorBody = response.body();
                AnalyserException e;
                if (isNull(errorBody)) {
                    e = new AnalyserException(response.code(), response.message());
                } else {
                    e = new AnalyserException(errorBody.getErrors().get(0));
                }
                log.error("Getting dictionary data count failed due to: " + e.getMessage());
                throw e;
            }
            return response.body().getData();
        } catch (IOException e) {
            String msg = "Error while getting dictionary data count from asterix";
            log.error(msg + "siteKey: " + dictionaryContext.getSiteKey() + " reason: " + e.getMessage());
            throw new AnalyserException(ErrorCode.IOError.getCode(), msg);
        }
    }


    private File writeResponseBodyToDisk(ResponseBody body) throws AnalyserException {
        File dictionaryFile = null;
        try {
            dictionaryFile = File.createTempFile("dict-", ".csv");
        } catch (IOException e) {
            String msg = "Error while writing dictionary file to disk";
            log.error(msg + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }

        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(dictionaryFile);
        ) {
            byte[] fileReader = new byte[4096];

            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                outputStream.write(fileReader, 0, read);
            }

            outputStream.flush();
            return dictionaryFile;
        } catch (IOException e) {
            String msg = "Error while writing dictionary file to disk";
            log.error(msg + " reason: " + e.getMessage());
            throw new AnalyserException(500, msg);
        }
    }

    private DictionaryData validateResponse(Response<DefaultResponse<DictionaryData>> response,
                                            String msg) throws AnalyserException {

        AnalyserException e;
        if (response.isSuccessful()) {
            List<Error> errors = response.body().getErrors();
            if (isNull(errors)) {
                return response.body().getData();
            }
            e = new AnalyserException(errors.get(0));
        } else {
            e = new AnalyserException(response.code(), response.message());
        }
        log.error(msg + e.getMessage());
        throw e;
    }
}
