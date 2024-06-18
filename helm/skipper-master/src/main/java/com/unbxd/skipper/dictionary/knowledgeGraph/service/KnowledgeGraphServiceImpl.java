package com.unbxd.skipper.dictionary.knowledgeGraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.knowledgeGraph.model.FeedBack;
import com.unbxd.skipper.dictionary.knowledgeGraph.model.FeedBackRequest;
import com.unbxd.skipper.dictionary.model.DictionaryContext;
import com.unbxd.skipper.dictionary.model.DictionaryEntry;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import static com.unbxd.skipper.dictionary.service.Constants.*;
import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.EXCLUDE_TERMS;
import static com.unbxd.skipper.dictionary.transformer.AssetTransformer.SYNONYMS;
import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@Log4j2
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {
    private KnowledgeGraph knowledgeGraph;
    private static final String DEFAULT_KG_POSITIVE_FEEDBACK = "relevant"; // KG == Knowledge graph
    private static final List<String> ASSET_TYPES_ALLOWED_IN_FEEDBACK =
            Arrays.asList(FRONT_TYPE_NAME, BACK_TYPE_NAME, BLACKLIST_TYPE_NAME);

    @Inject
    public KnowledgeGraphServiceImpl(KnowledgeGraph knowledgeGraph) {
        this.knowledgeGraph = knowledgeGraph;
    }

    @Override
    public void sendFeedback(DictionaryContext dictionaryContext) {
        if(!ASSET_TYPES_ALLOWED_IN_FEEDBACK.contains(dictionaryContext.getType()))
            return;
        List<FeedBack> feedBackList = new ArrayList<>();
        if (SYNONYMS.equals(dictionaryContext.getDictionaryName()))
            feedBackList = getFeedBackForSynonyms(dictionaryContext);
        else if (EXCLUDE_TERMS.equals(dictionaryContext.getDictionaryName()))
            feedBackList = getFeedBackForExcludeTerms(dictionaryContext);
        else
            feedBackList = getFeedBack(dictionaryContext);
        if(feedBackList.isEmpty()) return;
        FeedBackRequest request = new FeedBackRequest();
        request.setSitekey(dictionaryContext.getSiteKey());
        request.setFeedback(feedBackList);
        sendFeedbackToKnowledgeGraph(dictionaryContext.getDictionaryName(),request);
    }


    private void sendFeedbackToKnowledgeGraph(String dictionaryName, FeedBackRequest request)  {
        knowledgeGraph.sendFeedback(dictionaryName, request).enqueue(new Callback<JsonNode>() {
            @Override
            public void onResponse(Call<JsonNode> call, Response<JsonNode> response) {
                if(response.isSuccessful()) {
                    log.info("feedback call to knowledge graph succeeded for siteKey: " + request.getSitekey());
                } else {
                    log.error("feedback call to knowledge graph failed for siteKey: " + request.getSitekey() +
                            "error: " + response.errorBody());
                }
            }
            @Override
            public void onFailure(Call<JsonNode> call, Throwable t) {
                log.error("feedback call to knowledge graph failed for siteKey: " + request.getSitekey() +
                        "error: "  + t.getMessage());
            }
        });
    }



    private List<FeedBack> getFeedBackForSynonyms(DictionaryContext dictionaryContext) {
        if(!BLACKLIST_TYPE_NAME.equals(dictionaryContext.getType()))
            return getDefaultFeedBackForSynonyms(dictionaryContext);
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        for(DictionaryEntry entry: dictionaryContext.getDictionaryData().getEntries()) {
            if(isNull(entry.getReasons())) continue;
            if(entry.getReasons().containsKey(ONE_WAY_SYNONYM_NAME)) {
                Map<String,String> reasons = entry.getReasons().get(ONE_WAY_SYNONYM_NAME);
                for (String synonym : emptyIfNull(entry.getOneWay())) {
                    FeedBack feedBack = new FeedBack();
                    feedBack.setTerm1(entry.getName());
                    feedBack.setTerm2(synonym);
                    feedBack.setReason(reasons.get(synonym));
                    feedBackList.add(feedBack);
                }
            }
            if (entry.getReasons().containsKey(TWO_WAY_SYNONYM_NAME)) {
                Map<String,String> reasons = entry.getReasons().get(TWO_WAY_SYNONYM_NAME);
                for (String synonym : emptyIfNull(entry.getTwoWay())) {
                    FeedBack feedBack1 = new FeedBack();
                    feedBack1.setTerm1(entry.getName());
                    feedBack1.setTerm2(synonym);
                    feedBack1.setReason(reasons.get(synonym));
                    feedBackList.add(feedBack1);

                    FeedBack feedBack2 = new FeedBack();
                    feedBack2.setTerm1(synonym);
                    feedBack2.setTerm2(entry.getName());
                    feedBack1.setReason(reasons.get(synonym));
                    feedBackList.add(feedBack2);
                }
            }
        }
        return feedBackList;
    }

    private List<FeedBack> getDefaultFeedBackForSynonyms(DictionaryContext dictionaryContext) {
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        for(DictionaryEntry entry: dictionaryContext.getDictionaryData().getEntries()) {
            for (String synonym : emptyIfNull(entry.getOneWay())) {
                FeedBack feedBack = new FeedBack();
                feedBack.setTerm1(entry.getName());
                feedBack.setTerm2(synonym);
                feedBack.setReason(DEFAULT_KG_POSITIVE_FEEDBACK);
                feedBackList.add(feedBack);
            }
            for (String synonym : emptyIfNull(entry.getTwoWay())) {
                FeedBack feedBack1 = new FeedBack();
                feedBack1.setTerm1(entry.getName());
                feedBack1.setTerm2(synonym);
                feedBack1.setReason(DEFAULT_KG_POSITIVE_FEEDBACK);
                feedBackList.add(feedBack1);

                FeedBack feedBack2 = new FeedBack();
                feedBack2.setTerm1(synonym);
                feedBack2.setTerm2(entry.getName());
                feedBack1.setReason(DEFAULT_KG_POSITIVE_FEEDBACK);
                feedBackList.add(feedBack2);
            }
        }
        return feedBackList;
    }

    private List<FeedBack> getFeedBackForExcludeTerms(DictionaryContext dictionaryContext) {
        if(!BLACKLIST_TYPE_NAME.equals(dictionaryContext.getType()))
            return getDefaultFeedBackForExcludeTerms(dictionaryContext);
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        for(DictionaryEntry entry: dictionaryContext.getDictionaryData().getEntries()) {
            if(isNull(entry.getReasons())) continue;
            if(!entry.getReasons().containsKey(EXCLUDE_TERMS)) continue;
            Map<String,String> reasons = entry.getReasons().get(EXCLUDE_TERMS);
            for (String term : entry.getExcludeTerms()) {
                FeedBack feedBack = new FeedBack();
                feedBack.setTerm1(entry.getName());
                feedBack.setTerm2(term);
                feedBack.setReason(reasons.get(term));
                feedBackList.add(feedBack);
            }
        }
        return feedBackList;
    }

    private List<FeedBack>  getDefaultFeedBackForExcludeTerms(DictionaryContext dictionaryContext) {
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        for(DictionaryEntry entry: dictionaryContext.getDictionaryData().getEntries()) {
            for (String term : entry.getExcludeTerms()) {
                FeedBack feedBack = new FeedBack();
                feedBack.setTerm1(entry.getName());
                feedBack.setTerm2(term);
                feedBack.setReason(DEFAULT_KG_POSITIVE_FEEDBACK);
                feedBackList.add(feedBack);
            }
        }
        return feedBackList;
    }

    private List<FeedBack>  getFeedBack(DictionaryContext dictionaryContext) {
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        if(!BLACKLIST_TYPE_NAME.equals(dictionaryContext.getType())) {
            return getDefaultFeedback(dictionaryContext);
        }
        for(DictionaryEntry entry: dictionaryContext.getDictionaryData().getEntries()) {
            FeedBack feedBack = new FeedBack();
            feedBack.setTerm1(entry.getName());
            feedBack.setReason(entry.getReason());
            feedBackList.add(feedBack);
        }
        return feedBackList;
    }

    private List<FeedBack>  getDefaultFeedback(DictionaryContext dictionaryContext) {
        List<FeedBack> feedBackList = new ArrayList<>(dictionaryContext.getDictionaryData().getEntries().size());
        for (DictionaryEntry entry : dictionaryContext.getDictionaryData().getEntries()) {
            FeedBack feedBack = new FeedBack();
            feedBack.setTerm1(entry.getName());
            feedBack.setReason(DEFAULT_KG_POSITIVE_FEEDBACK);
            feedBackList.add(feedBack);
        }
        return feedBackList;
    }
}
