package com.example.Movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.regions.Region;

@Configuration
public class AwsConfig {

    private static final Region AWS_REGION = Region.US_EAST_2;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(AWS_REGION)
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(AWS_REGION)
                .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(AWS_REGION)
                .build();
    }
}
