package com.unbxd.search.feed.model;

import lombok.Data;

@Data
public class FeedMetaInfo {
    private String name;
    private String path;
    private S3BucketInfo args;
}
