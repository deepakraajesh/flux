package com.unbxd.recommend.dao;

import com.unbxd.recommend.exception.RecommendException;
import com.unbxd.recommend.model.QueryStats;
import com.unbxd.recommend.model.RecommendContext;

import java.io.File;
import java.util.List;

public interface ContentDao {
    String ROWS = "rows";
    String HITS = "hits";
    String QUERY = "query";
    String AFTER = "after";
    String BEFORE = "before";

    String SITEKEY = "sitekey";
    String JOBTYPE = "jobtype";
    String COMMA_DELIMITER = ",";
    String WORKFLOW_ID = "workflowId";
    String OR_RECTIFIED = "orRectified";
    String SPELL_CHECKED = "spellChecked";
    String CONCEPT_CORRECTED = "conceptCorrected";
    String SECONDARY_LANGUAGES = "secondaryLanguages";
    String QUERY_CONTENT_COLLECTION = "skipper-recommended-queries";

    void deleteRowIds(String rowId,
                      String sitekey) throws RecommendException;

    void flushQueryStats(String sitekey,
                         String jobType) throws RecommendException;

    List<String> getRowIds(String filter,
                           String sitekey) throws RecommendException;

    void storeQueryStats(File file,
                         String sitekey,
                         String jobType,
                         String workflowId) throws RecommendException;

    List<String> getLanguages(String sitekey) throws RecommendException;

    long countQueryStats(RecommendContext recommendContext) throws RecommendException;

    List<QueryStats> getQueryStats(RecommendContext recommendContext) throws RecommendException;
}
