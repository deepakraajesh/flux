package com.unbxd.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.inject.Inject;
import com.unbxd.skipper.dictionary.exception.AssetException;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

import static com.unbxd.skipper.dictionary.validator.ErrorCode.S3_ERROR;

@Log4j2
public class AmazonS3Client {

    AmazonS3 s3Client;

    @Inject
    public AmazonS3Client(AmazonS3 s3Client) {
         this.s3Client = s3Client;
    }

    public AmazonS3 getClient() {
        return s3Client;
    }

    public String get(String bucketPath) {
        AmazonS3URI amazonS3URI = new AmazonS3URI(bucketPath);
        return s3Client.getObjectAsString(amazonS3URI.getBucket(), amazonS3URI.getKey());
    }

    public File downloadFile(String bucketPath) {
        if(bucketPath == null || bucketPath.isEmpty())
            throw new AssetException("S3 bucket URL is empty");
        AmazonS3URI amazonS3URI = new AmazonS3URI(bucketPath);
        String[] splittedTokens = bucketPath.split("/");
        String fileName = splittedTokens[splittedTokens.length - 1];
        File file = null;
        try {
            file = File.createTempFile(fileName, "csv");
        } catch (IOException e) {
            log.error("Internal error, cannot create file in temp directory");
            throw new AssetException("Internal error, cannot create file in temp directory");
        }
        ObjectMetadata response = s3Client.
                getObject(new GetObjectRequest(amazonS3URI.getBucket(), amazonS3URI.getKey()), file);
        if(response == null) {
            String msg = "Error while downloading data from s3";
            log.error(msg);
            throw new AssetException(msg, S3_ERROR.getCode());
        }
        return file;
    }
}
