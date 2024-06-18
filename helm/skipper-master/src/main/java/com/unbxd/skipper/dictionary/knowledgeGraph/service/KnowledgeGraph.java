package com.unbxd.skipper.dictionary.knowledgeGraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbxd.skipper.dictionary.knowledgeGraph.model.FeedBackRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface KnowledgeGraph {
    @POST("/giraffe/{dictionaryName}/feedback")
    Call<JsonNode> sendFeedback(@Path("dictionaryName") String dictionaryName, @Body FeedBackRequest req);
}
