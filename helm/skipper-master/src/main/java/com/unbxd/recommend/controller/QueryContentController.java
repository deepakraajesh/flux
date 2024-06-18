package com.unbxd.recommend.controller;

import com.google.inject.Inject;
import com.unbxd.cbo.response.Error;
import com.unbxd.cbo.response.Response;
import com.unbxd.cbo.response.Response.Builder;
import com.unbxd.recommend.model.NavigateResponse;
import com.unbxd.recommend.model.RectifyResponse;
import com.unbxd.recommend.service.QueryContentService;
import com.unbxd.skipper.dictionary.model.DictionaryData;
import lombok.extern.log4j.Log4j2;
import ro.pippo.controller.Controller;
import ro.pippo.controller.GET;
import ro.pippo.controller.Path;
import ro.pippo.controller.Produces;
import ro.pippo.controller.extractor.Param;

import static com.unbxd.recommend.model.Operation.*;
import static com.unbxd.recommend.model.RecommendContext.getInstance;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Path("/skipper/site/{sitekey}/algo/recommend/content")
public class QueryContentController extends Controller {

    private final QueryContentService queryContentService;

    @Inject
    public QueryContentController(QueryContentService service) {
        this.queryContentService = service;
    }

    @GET("/navigate")
    @Produces(Produces.JSON)
    public Response<NavigateResponse> getNavigateResponse(@Param String sitekey) {
        Builder<NavigateResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService.getNavigateResponse(sitekey));
        } catch (Exception e) {
            String error = "Error while trying to fetch navigate" +
                    " tabs for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/rectify-zero")
    public Response<RectifyResponse> getRectifyQueries(@Param int page,
                                                       @Param int count,
                                                       @Param String sitekey) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, EMPTY, ZERO_RECTIFIED)));
        } catch (Exception e) {
            String error = "Error while trying to fetch rectify " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/improve-recall")
    public Response<RectifyResponse> getRecallQueries(@Param int page,
                                                      @Param int count,
                                                      @Param String sitekey) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, EMPTY, IMPROVE_RECALL)));
        } catch (Exception e) {
            String error = "Error while trying to fetch improve recall " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/rectify-spellcheck")
    public Response<RectifyResponse> getSpellCheckRectified(@Param int page,
                                                            @Param int count,
                                                            @Param String sitekey) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, EMPTY, SPELL_CHECKED)));
        } catch (Exception e) {
            String error = "Error while trying to fetch spellcheck rectified " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/rectify-or")
    public Response<RectifyResponse> getOrRectified(@Param int page,
                                                    @Param int count,
                                                    @Param String sitekey) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, EMPTY, OR_RECTIFIED)));
        } catch (Exception e) {
            String error = "Error while trying to fetch OR rectified " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/rectify-concepts")
    public Response<RectifyResponse> getConceptRectified(@Param int page,
                                                         @Param int count,
                                                         @Param String sitekey) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, EMPTY, CONCEPT_CORRECTED)));
        } catch (Exception e) {
            String error = "Error while trying to fetch OR rectified " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @Produces(Produces.JSON)
    @GET("/queries/language/{language}")
    public Response<RectifyResponse> getLanguageBased(@Param int page,
                                                      @Param int count,
                                                      @Param String sitekey,
                                                      @Param String language) {
        Builder<RectifyResponse> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getRectifiableQueries(getInstance(page,
                            count, sitekey, language, LANGUAGE_BASED)));
        } catch (Exception e) {
            String error = "Error while trying to fetch OR rectified " +
                    "queries data for sitekey[" + sitekey + "]: " +
                    e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }

    @GET("/synonyms")
    @Produces(Produces.JSON)
    public Response<DictionaryData> getSynonymsData(@Param int page,
                                                    @Param int count,
                                                    @Param String filter,
                                                    @Param String sitekey) {
        Builder<DictionaryData> builder = new Builder<>();

        try {
            builder.withData(queryContentService
                    .getDictionaryData(page, count, filter,
                            sitekey, "synonyms"));
        } catch (Exception e) {
            String error = "Error while trying to fetch dictionary data" +
                    " for sitekey[" + sitekey + "]:" + e.getMessage();
            builder.withError(new Error.Builder().withCode(500)
                    .withMessage(error)
                    .build());
            log.error(error);
        }
        return builder.build();
    }
}
