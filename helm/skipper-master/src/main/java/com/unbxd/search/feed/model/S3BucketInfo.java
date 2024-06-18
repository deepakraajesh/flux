package com.unbxd.search.feed.model;

import lombok.Data;

@Data
public class S3BucketInfo {
    private String s3bucket;
    private String s3folder;
    private String s3fullPath;
    private String s3location;
    private String s3regionId;
}
