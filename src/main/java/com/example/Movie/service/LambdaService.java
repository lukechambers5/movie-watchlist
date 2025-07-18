package com.example.Movie.service;

import com.example.Movie.dto.MovieDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import java.util.*;

@Service
public class LambdaService {

    private final LambdaClient lambdaClient;

    public LambdaService(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    public void invokeMetadataEnrichment(MovieDto dto, String s3PosterUrl) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", dto.getUserId());
            payload.put("movie_id", dto.getMovieId());
            payload.put("title", dto.getTitle());
            payload.put("rating", 8.8); // optionally fetch this dynamically
            payload.put("genre", dto.getGenres().split(",")[0]);
            payload.put("release_year", Integer.parseInt(dto.getReleaseDate().substring(0, 4)));
            payload.put("actors", Arrays.asList(dto.getActors().split(", ")));
            payload.put("thumbnail_url", s3PosterUrl);

            String json = new ObjectMapper().writeValueAsString(payload);

            InvokeRequest request = InvokeRequest.builder()
                    .functionName("cinescope-analyze-movie")
                    .payload(SdkBytes.fromUtf8String(json))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);
            System.out.println("Lambda response: " + response.payload().asUtf8String());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
