package com.unbxd.skipper.relevancy.model;

import com.unbxd.skipper.feed.dim.model.DimensionMap;
import com.unbxd.skipper.feed.dim.model.VariantConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelevancyOutputModel<T extends Field> {

    public static final Map<JobType, Class> typeToClass =  new HashMap<>() {
        {
            put(JobType.dimensionMap, DimensionMap.class);
            put(JobType.facets, RelevancyFacetField.class);
            put(JobType.synonyms, RelevancyStringField.class);
            put(JobType.suggestedSynonyms, RelevancyStringField.class);
            put(JobType.enrichSynonyms, RelevancyStringField.class);
            put(JobType.recommendPhrases, RelevancyStringField.class);
            put(JobType.recommendConcepts, RelevancyStringField.class);
            put(JobType.recommendSynonyms, RelevancyStringField.class);
            put(JobType.enrichSuggestedSynonyms, RelevancyStringField.class);
            put(JobType.autosuggest, AutosuggestConfig.class);
            put(JobType.multiwords, RelevancyStringField.class);
            put(JobType.suggestedMultiwords, RelevancyStringField.class);
            put(JobType.noStemWords, RelevancyStringField.class);
            put(JobType.suggestedNoStemWords, RelevancyStringField.class);
            put(JobType.searchableFields, SearchableField.class);
            put(JobType.suggestedSearchableFields, SearchableField.class);
            put(JobType.mandatoryTerms, RelevancyStringField.class);
            put(JobType.suggestedMandatoryTerms, RelevancyStringField.class);
            put(JobType.variants, VariantConfig.class);
        }};

    private String siteKey;
    private String workflowId;
    private String s3Location;
    private RelevancyJobMetric feedStats;
    private RelevancyJobMetric queryStats;
    private List<Object> data = new ArrayList<>();

    public RelevancyOutputModel(String siteKey, String workflowId,
                                String bucketPath, RelevancyJobMetric feedMetric, RelevancyJobMetric queryMetrics) {
        this.siteKey = siteKey;
        this.workflowId = workflowId;
        this.feedStats = feedMetric;
        this.queryStats = queryMetrics;
        this.s3Location = bucketPath;
        data = new ArrayList<>();
    }
}
