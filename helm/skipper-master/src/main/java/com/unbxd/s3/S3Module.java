package com.unbxd.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;

import java.util.Properties;

import static com.amazonaws.regions.Regions.US_WEST_1;

public class S3Module extends AbstractModule {

    private static final String S3_REGION = "s3.region";


    @Provides
    @Inject
    public AmazonS3 getS3Client(Properties properties) {
        String regionProp = properties.getProperty(S3_REGION, US_WEST_1.name());
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.valueOf(regionProp))
                .withForceGlobalBucketAccessEnabled(true)
                .build();

    }

    @Provides
    @Inject
    public AmazonS3Client getS3Client(AmazonS3 s3Client) {
        return new AmazonS3Client(s3Client);
    }
}
