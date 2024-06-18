package com.unbxd.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.unbxd.config.model.ConfigResponse;
import com.unbxd.config.model.HealthCheckResponse;
import com.unbxd.config.service.HealthCheckRemoteService;
import com.unbxd.mongo.MongoModule;
import com.unbxd.skipper.SkipperModule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Singleton
public class Config extends Properties {

    private List<String> serviceNames;
    private AmazonS3 s3Client;
    private String awsRegion;
    private MongoModule mongoModule;
    private final HealthCheckRemoteService remoteService;
    private static final String URL_SUFFIX = ".url";
    private static final String S3_BUCKET_SUFFIX = ".s3Bucket";
    private static final String HEALTHCHECK_SUFFIX = ".healthcheck";
    private static final String S3_BUCKET = "s3Bucket";
    private static final String MONGO = "mongoDB";
    public static final String AWS_DEFAULT_REGION = "AWS_DEFAULT_REGION";


    @Inject
    private Config(HealthCheckRemoteService remoteService, MongoModule mongoModule) {
        this.remoteService = remoteService;
        loadApplicationProperties();
        loadSystemProperties();
        loadServiceUrls();
        awsRegion = getProperty(AWS_DEFAULT_REGION);
        if(isNull(awsRegion))
            throw new IllegalArgumentException(AWS_DEFAULT_REGION + " property not set");
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion).build();
        this.mongoModule = mongoModule;
    }

    private void loadApplicationProperties() {
        String configFileName = "conf/application.properties";
        InputStream inputStream =  SkipperModule.class.getClassLoader()
                .getResourceAsStream(configFileName);

        try {
            if (inputStream != null) {
                super.load(inputStream);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSystemProperties() {
        for(Map.Entry<Object, Object> entry: System.getProperties().entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    private void loadServiceUrls() {
        serviceNames = getServiceNames();
        if(CollectionUtils.isNotEmpty(serviceNames)) {
            for(String serviceName: serviceNames) {
                String url = System.getProperty(serviceName.concat(URL_SUFFIX));
                String healthCheckUrl = System.getProperty(serviceName.concat(HEALTHCHECK_SUFFIX));

                if(StringUtils.isEmpty(healthCheckUrl)) { this.put(serviceName.concat(HEALTHCHECK_SUFFIX), url); }
                else { this.put(serviceName.concat(HEALTHCHECK_SUFFIX), url + healthCheckUrl); }
                if(isNotEmpty(url)) { this.put(serviceName.concat(URL_SUFFIX), url); }
            }
        }
    }

    public HealthCheckResponse checkServiceHealth() {
        if (CollectionUtils.isEmpty(serviceNames)) {
            return HealthCheckResponse.getInstance(500, "Service names not found in System properties.");
        }
        HealthCheckResponse healthCheckResponse = HealthCheckResponse.getInstance();

        for (String serviceName : serviceNames) {
            int statusCode = 200;
            StringBuilder messageBuffer = new StringBuilder();
            String healthCheckUrl = getProperty(serviceName.concat(HEALTHCHECK_SUFFIX));

            if (isNotEmpty(healthCheckUrl)) {
                try {
                    Call<Void> healthCheckCallObj = remoteService.doHealthCheck(healthCheckUrl);
                    Response<Void> healthCheckCallResponse = healthCheckCallObj.execute();
                    statusCode = healthCheckCallResponse.code();
                    if (statusCode >= 200 && statusCode < 300) {
                        messageBuffer.append("success");
                    } else {
                        healthCheckResponse.setCode(statusCode);
                        messageBuffer.append(healthCheckCallResponse.errorBody().string());
                    }
                } catch (IOException e) {
                    statusCode = 500;
                    healthCheckResponse.setCode(500);
                    messageBuffer.append("HealthCheck failed for ")
                            .append(serviceName).append(" with exception: ").append(e.getMessage());
                }
            } else {
                statusCode = 404;
                messageBuffer.append("HealthCheck url not defined for service: ").append(serviceName).append(".");
            }
            healthCheckResponse.getHealthCheck().add(ConfigResponse
                    .getInstance(statusCode, serviceName, messageBuffer.toString(), healthCheckUrl));
            messageBuffer.delete(0, messageBuffer.length());
        }
        checks3Buckets(healthCheckResponse);
        checkMongoHealth(healthCheckResponse);
        return healthCheckResponse;
    }

    private List<String> getServiceNames() {
        List<String> serviceNames = new ArrayList<>();
        for(String propertyName: CollectionUtils.emptyIfNull(this.stringPropertyNames())) {
            if(propertyName.endsWith(URL_SUFFIX)) {
                /* exclude all properties with java prefix */
                if(!propertyName.startsWith("java")) {
                    serviceNames.add(propertyName.substring(0, propertyName.length() - 4));
                }
            }
        }
        return serviceNames;
    }

    private void checks3Buckets(HealthCheckResponse healthCheckResponse) {
        for (String propertyName : CollectionUtils.emptyIfNull(this.stringPropertyNames())) {
            if (propertyName.startsWith("java") || !propertyName.endsWith(S3_BUCKET_SUFFIX))
                continue;
            String bucketName = getProperty(propertyName);
            StringBuilder response = new StringBuilder().append("bucket: ").append(bucketName);
            if (s3Client.doesBucketExistV2(bucketName)) {
                response.append(" exists in region ").append(awsRegion);
                healthCheckResponse.getHealthCheck().add(
                        ConfigResponse.getInstance(200, S3_BUCKET, response.toString(), StringUtils.EMPTY)
                );
            } else {
                healthCheckResponse.setCode(500);
                response.append(" does not exists in region ").append(awsRegion);
                healthCheckResponse.getHealthCheck().add(
                        ConfigResponse.getInstance(404, S3_BUCKET, response.toString(), StringUtils.EMPTY)
                );
            }
        }
    }

    private void checkMongoHealth(HealthCheckResponse healthCheckResponse) {
        String message = "working!!";
        int statusCode =  200;
        try {
            mongoModule.getDatabase(this);
        } catch (Exception e) {
            statusCode = 500;
            message =  e.getMessage();
            healthCheckResponse.setCode(statusCode);
        }
        healthCheckResponse.getHealthCheck().add(
                ConfigResponse.getInstance(statusCode, MONGO, message, StringUtils.EMPTY)
        );
    }

}
