package com.unbxd.search.feed;

import com.unbxd.search.feed.model.FeedReindexStatus;
import retrofit2.Call;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface MozartService {

    @PATCH("v2.1/sites/{siteKey}/indexes/catalog/rebuild")
    Call<FeedReindexStatus> reindex(@Path("siteKey") String siteKey);
}
