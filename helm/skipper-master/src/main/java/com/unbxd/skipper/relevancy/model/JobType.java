package com.unbxd.skipper.relevancy.model;

import com.google.common.collect.Lists;
import com.unbxd.analyser.service.impl.AsterixService;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public enum JobType {
    synonyms, spellcheck, multiwords, facets, noStemWords,
    mandatoryTerms, searchableFields, autosuggest, dimensionMap, variants, autoNer,
    suggestedSynonyms, suggestedMultiwords, suggestedMandatoryTerms, suggestedNoStemWords,
    enrichSynonyms, suggestedSearchableFields, enrichSuggestedSynonyms, recommendSynonyms,
    recommendPhrases, recommendConcepts;

    public static JobType[] suggestedDictionaryJobs = {
            JobType.suggestedSynonyms,
            JobType.suggestedMandatoryTerms,
            JobType.suggestedMultiwords,
            JobType.suggestedNoStemWords,
            JobType.enrichSuggestedSynonyms
    };

    public static List<JobType> suggestedDictionaries = Arrays.asList(suggestedDictionaryJobs);

    public static Map<JobType, JobType> suggestedJobMap = new HashMap<>() {
        {
            put(searchableFields, suggestedSearchableFields);
            put(synonyms, suggestedSynonyms);
            put(mandatoryTerms, suggestedMandatoryTerms);
            put(multiwords, suggestedMultiwords);
            put(noStemWords, suggestedNoStemWords);
        }
    };

    /** Jobs for recommend tab */
    public static List<JobType> getRecommendJobs() {
        return newArrayList(recommendPhrases, recommendConcepts,
                recommendSynonyms);
    }
    /** Jobs responsible for enrichment */
    public static List<JobType> getEnrichmentJobs() {
        return newArrayList(enrichSynonyms, enrichSuggestedSynonyms);
    }

    /** Jobs after enrichment is done */
    public static List<JobType> getEnrichedJobs() {
        return newArrayList(enrichSynonyms, enrichSuggestedSynonyms,
                recommendSynonyms, recommendPhrases, recommendConcepts);
    }

}
