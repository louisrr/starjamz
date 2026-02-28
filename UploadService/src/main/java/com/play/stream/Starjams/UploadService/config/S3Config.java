package com.play.stream.Starjams.UploadService.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    // Leave blank in production to fall back to instance profile / env vars
    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(region);
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))
            );
        } else {
            builder.withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        }
        return builder.build();
    }

    /**
     * TransferManager handles multipart uploads automatically for files above the
     * threshold (16 MB here). This is required for large video files — a single
     * PutObject call is capped at 5 GB and is much slower for large payloads.
     */
    @Bean
    public TransferManager transferManager(AmazonS3 amazonS3) {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold(16 * 1024 * 1024L) // 16 MB
                .build();
    }
}
