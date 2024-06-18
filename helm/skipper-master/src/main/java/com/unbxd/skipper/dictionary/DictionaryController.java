package com.unbxd.skipper.dictionary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.unbxd.analyser.exception.AnalyserException;
import com.unbxd.analyser.service.AnalyserService;
import com.unbxd.cbo.response.Response;
import com.unbxd.event.EventFactory;
import com.unbxd.skipper.dictionary.exception.AssetException;
import com.unbxd.skipper.dictionary.knowledgeGraph.service.KnowledgeGraphService;
import com.unbxd.skipper.dictionary.model.*;
import com.unbxd.skipper.dictionary.service.DictionaryService;
import com.unbxd.toucan.eventfactory.EventBuilder;
import ro.pippo.controller.*;
import ro.pippo.controller.extractor.Param;

import java.io.File;
import java.util.Map;

import static com.unbxd.skipper.dictionary.model.DictionaryType.*;
import static com.unbxd.toucan.eventfactory.EventTag.INFO;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

public class DictionaryController extends Controller {
    private static final String USER = "user";
    private static final String STATUS = "status";
    private static final String DICT_OP = "dictionary";
    private static final String UPLOAD_OP = "bulk_upload";
    private static final String EVENT_BUILDER = "eventBuilder";
    private static final String REQUEST_ID = "Unbxd-Request-Id";
    private static final String DICTIONARY_USER = "dictionary-user";
    private static final String DICTIONARY_TYPE = "dictionary_type";
    private static final String CODE_TAG_NAME = "code";
    private static final String ENTRY_TAG_NAME = "entry";
    private static final String TYPE = "type";
    private static final String AI = "ai";
    private static final String SUGGESTED = "suggested";
    private static final String BACK = "bck";
    private static final String FRONT = "front";


    private EventFactory factory;
    private static final ObjectMapper mapper = new ObjectMapper();
    private DictionaryService dictionaryService;
    private AnalyserService analyserService;
    private KnowledgeGraphService knowledgeGraphService;

    @Inject
    public DictionaryController(DictionaryService dictionaryService,
                                EventFactory factory,
                                AnalyserService analyserService,
                                KnowledgeGraphService knowledgeGraphService) {
        this.factory = factory;
        this.dictionaryService = dictionaryService;
        this.analyserService = analyserService;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/skipper/site/{siteKey}/dictionary/{dictionaryName}")
    public Response<Map<String, String>> addDictionaryData(@Param String type,
                                                           @Param String version,
                                                           @Param String siteKey,
                                                           @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, DictionaryType.valueOf(type), siteKey, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        if(isAI(dictionaryContext.getType())){
            throw new AssetException("Unsupported operation adding dictionary data to"
                    + dictionaryContext.getType() +" set");
        }
        analyserService.addDictionaryData(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @GET("/skipper/site/{siteKey}/dictionary/{dictionaryName}/id/{id}")
    public Response<DictionaryAnalysis> getAnalysisOfDictionary(@Param int page,
                                                                @Param int count,
                                                                @Param String type,
                                                                @Param String siteKey,
                                                                @Param String dictionaryName,
                                                                @Param String id,
                                                                @Param String sortBy,
                                                                @Param DictionaryContext.ORDER_BY sortOrder,
                                                                @Param String analysisType) {
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(page,
                count, DictionaryType.valueOf(type), siteKey, dictionaryName, null, sortBy, sortOrder);
        return new Response.Builder<DictionaryAnalysis>().
                withData(dictionaryService.getAnalysisOfDictionary(dictionaryContext, id, analysisType)).build();
    }

    @Produces(Produces.JSON)
    @GET("/skipper/site/{siteKey}/dictionary/{dictionaryName}")
    public Response<DictionaryData> getDictionaryData(@Param int page,
                                                      @Param int count,
                                                      @Param String type,
                                                      @Param String search,
                                                      @Param String version,
                                                      @Param String siteKey,
                                                      @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(page,
                count, DictionaryType.valueOf(type), siteKey, dictionaryName, null);
        dictionaryContext.setVersion(version);
        dictionaryContext.setSearch(search);

        DictionaryData data;
        if (isAI(dictionaryContext.getType())) {
            data = dictionaryService.getDictionaryData(dictionaryContext);
        } else {
            data = analyserService.getDictionaryData(dictionaryContext);
        }
        return new Response.Builder<DictionaryData>().withData(data).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @DELETE("/skipper/site/{siteKey}/dictionary/{dictionaryName}")
    public Response<Map<String, String>> deleteDictionaryData(@Param String type,
                                                              @Param String version,
                                                              @Param String siteKey,
                                                              @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, DictionaryType.valueOf(type), siteKey, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        if(isAI(dictionaryContext.getType())){
            throw new AssetException("Unsupported operation deleting dictionary data from "
                    + dictionaryContext.getType() + " set");
        }
        analyserService.deleteDictionaryData(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @PATCH("/skipper/site/{siteKey}/dictionary/{dictionaryName}")
    public Response<Map<String, String>> updateDictionaryData(@Param String type,
                                                              @Param String version,
                                                              @Param String siteKey,
                                                              @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, DictionaryType.valueOf(type), siteKey, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        if(isAI(dictionaryContext.getType())){
            throw new AssetException("Unsupported operation updating dictionary data in "
                    + dictionaryContext.getType() + " set");
        }
        analyserService.updateDictionaryData(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/skipper/site/{siteKey}/dictionary/{dictionaryName}/bulk")
    public Response<DictionaryData> bulkUploadData(@Param String type,
                                                   @Param String version,
                                                   @Param String siteKey,
                                                   @Param boolean flushAll,
                                                   @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        BulkUploadRequest uploadRequest = mapper.readValue(getRequest().getBody(),
                BulkUploadRequest.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, DictionaryType.valueOf(type), siteKey, dictionaryName, null);
        dictionaryContext.setS3fileUrl(uploadRequest.getFileUrl());
        dictionaryContext.setFlushAll(flushAll);
        dictionaryContext.setVersion(version);

        analyserService.bulkUploadData(dictionaryContext);
        if (isAI(dictionaryContext.getType())) {
            dictionaryService.bulkUploadDictionary(dictionaryContext);
            sendOmissionEvents(dictionaryContext, siteKey);
        }
        return new Response.Builder<DictionaryData>().withData(dictionaryContext
                .getDictionaryData()).build();
    }

    @GET("/site/{siteKey}/dictionary/{dictionaryName}/download")
    public File bulkDownload(@Param String type,
                             @Param String version,
                             @Param String siteKey,
                             @Param boolean includeId,
                             @Param String dictionaryName) throws Exception {
        return bulkDownloadData(type, version, siteKey, includeId, dictionaryName);
    }


    @GET("/skipper/site/{siteKey}/dictionary/{dictionaryName}/download")
    public File bulkDownloadData(@Param String type,
                                 @Param String version,
                                 @Param String siteKey,
                                 @Param boolean includeId,
                                 @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, DictionaryType.valueOf(type), siteKey, dictionaryName, null);
        dictionaryContext.setVersion(version);
        dictionaryContext.setGetId(includeId);
        File file;
        if (isAI(dictionaryContext.getType())) {
            file = dictionaryService.bulkDownloadDictionary(dictionaryContext);
        } else {
            file = analyserService.download(siteKey, dictionaryName,
                    dictionaryContext.getType().toString(), Boolean.FALSE);
        }
        return file;
    }

    /*         Blacklist API's         */
    @Produces(Produces.JSON)
    @GET("/skipper/site/{coreName}/dictionary/{dictionaryName}/blacklist")
    public Response<DictionaryData> getBlacklist(@Param int page,
                                                 @Param int count,
                                                 @Param String search,
                                                 @Param String version,
                                                 @Param String coreName,
                                                 @Param String dictionaryName) throws AssetException{
        addLocal(coreName);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(page,
                count, blacklist, coreName, dictionaryName, null);
        dictionaryContext.setVersion(version);
        dictionaryContext.setSearch(search);

        DictionaryData data = dictionaryService.getDictionaryData(dictionaryContext);
        return new Response.Builder<DictionaryData>().withData(data).build();
    }

    /***
     * This api is used by UI to delete single terms in synonyms or excludeTermsSet.
     * Deleted terms in synonyms or excludeTermsSet of blacklist set are added to AI set.
     * TODO: don't support delete in PATCH request, this change will be done in next release
     * */

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @PATCH("/skipper/site/{coreName}/dictionary/{dictionaryName}/blacklist")
    public Response<Map<String, String>> updateBlacklist(@Param String version,
                                                         @Param String coreName,
                                                         @Param String dictionaryName) throws Exception {
        addLocal(coreName);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, blacklist, coreName, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        dictionaryService.updateBlackList(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @PUT("/skipper/site/{coreName}/dictionary/{dictionaryName}/blacklist")
    public Response<Map<String, String>> appendBlacklist(@Param String version,
                                                         @Param String type,
                                                         @Param String coreName,
                                                         @Param String dictionaryName) throws Exception {
        addLocal(coreName);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, blacklist, coreName, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        dictionaryService.appendBlackList(dictionaryContext, type);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @DELETE("/skipper/site/{coreName}/dictionary/{dictionaryName}/blacklist")
    public Response<Map<String, String>> deleteBlacklist(@Param String version,
                                                         @Param String coreName,
                                                         @Param String dictionaryName) throws Exception {
        addLocal(coreName);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, blacklist, coreName, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);

        dictionaryService.deleteBlackList(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @GET("/skipper/site/{coreName}/dictionary/{dictionaryName}/blacklist/reasons")
    public Response<BlacklistReasonsData> getBlacklistReasons(@Param String version,
                                                              @Param String coreName,
                                                              @Param String dictionaryName) throws Exception {
        addLocal(coreName);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, null, coreName, dictionaryName, null);
        dictionaryContext.setVersion(version);

        BlacklistReasonsData data = dictionaryService.getBlacklistReasons(dictionaryContext);
        return new Response.Builder<BlacklistReasonsData>().withData(data).build();
    }

    @Produces(Produces.JSON)
    @GET("/skipper/site/{coreName}/dictionary/{dictionaryName}/count")
    public Response<DictionaryCount> getDictionaryCount(@Param String version,
                                                        @Param String coreName,
                                                        @Param String dictionaryName) throws Exception {
        addLocal(coreName);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, null, coreName, dictionaryName, null);
        dictionaryContext.setVersion(version);

        DictionaryCount data = dictionaryService.getDictionaryCount(dictionaryContext);
        DictionaryCount asterixData = analyserService.getDictionaryCount(dictionaryContext);
        data.setBck(asterixData.getBck());
        data.setFront(asterixData.getFront());
        return new Response.Builder<DictionaryCount>().withData(data).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/skipper/site/{siteKey}/dictionary/{dictionaryName}/sync")
    public Response<Map<String, String>> synchronize(@Param String type,
                                                   @Param String siteKey,
                                                   @Param String dictionaryName) throws AnalyserException {
        File file = analyserService.download(siteKey, dictionaryName, type, Boolean.TRUE);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(siteKey, dictionaryName,
                DictionaryType.valueOf(type));
        dictionaryContext.setFlushAll(Boolean.TRUE);
        dictionaryService.bulkUploadDictionary(file, dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    @Produces(Produces.JSON)
    @Consumes(Consumes.JSON)
    @POST("/skipper/site/{siteKey}/dictionary/{dictionaryName}/accept")
    public Response<Map<String, String>> acceptSuggested(@Param String version,
                                                         @Param String siteKey,
                                                         @Param String dictionaryName) throws Exception {
        addLocal(siteKey);
        DictionaryData dictionaryData = mapper.readValue(getRequest().getBody(),
                DictionaryData.class);
        DictionaryContext dictionaryContext = DictionaryContext.getInstance(0,
                0, suggested, siteKey, dictionaryName, dictionaryData);
        dictionaryContext.setVersion(version);
        dictionaryService.acceptSuggested(dictionaryContext);
        return new Response.Builder<Map<String, String>>().withData(singletonMap(STATUS, "success")).build();
    }

    /*         Utility Methods         */

    private void addLocal(String siteKey) {
        EventBuilder builder = new EventBuilder();
        String user = defaultString(getRequest().getHeader(USER),
                DICTIONARY_USER);
        String requestId = defaultString(getRequest().getHeader(REQUEST_ID),
                randomUUID().toString());

        builder.withTraceId(requestId);
        getRouteContext().setLocal(USER, user);
        getRouteContext().setLocal(TYPE, DICT_OP);
        getRouteContext().setLocal(REQUEST_ID, requestId);
        getRouteContext().setLocal(REQUEST_ID, requestId);
        getRouteContext().setHeader(REQUEST_ID, requestId);
        getRouteContext().setLocal(EVENT_BUILDER, builder);
    }

    private void sendOmissionEvents(DictionaryContext dictionaryContext, String siteKey) {
        if (dictionaryContext.getDictionaryData().getOmittedCount() > 0) {
            for (Omission omission : dictionaryContext.getDictionaryData()
                    .getOmissions()) {
                EventBuilder eventBuilder = factory.getEvent(getRouteContext()
                        .getLocal(USER), EMPTY, dictionaryContext.getSiteKey(), INFO, UPLOAD_OP);
                eventBuilder.withMessage(omission.getMessage());
                eventBuilder.withTag(DICTIONARY_TYPE, dictionaryContext.getDictionaryName());
                eventBuilder.withTag(CODE_TAG_NAME, omission.getCode());
                try {
                    eventBuilder.withTag(ENTRY_TAG_NAME, mapper.writeValueAsString(omission.getEntry()));
                } catch (JsonProcessingException e) {
                }

                factory.sendEvent(eventBuilder);
            }
        }
    }
}
