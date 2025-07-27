package com.example.Movie.service;

import com.example.Movie.dto.MovieDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.util.*;

@Service
public class MovieService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url}")
    private String apiUrl;

    private final DynamoDbService dynamoDbService;
    private final S3Service s3Service;
    private final LambdaService lambdaService;

    public MovieService(DynamoDbService dynamoDbService, S3Service s3Service, LambdaService lambdaService) {
        this.dynamoDbService = dynamoDbService;
        this.s3Service = s3Service;
        this.lambdaService = lambdaService;
    }

    public String addMovieToWatchlist(String title, String userId) {
        String url = String.format("%s/search/movie?query=%s&api_key=%s", apiUrl, title, apiKey);
        RestTemplate restTemplate = new RestTemplate();
        Map result = restTemplate.getForObject(url, Map.class);
        List<Map> movies = (List<Map>) result.get("results");

        if (movies == null || movies.isEmpty()) {
            return "Movie not found.";
        }

        Map<String, Object> movie = movies.get(0);
        String movieId = String.valueOf(movie.get("id"));

        List<Integer> genreIds = (List<Integer>) movie.get("genre_ids");
        String genreUrl = String.format("%s/genre/movie/list?api_key=%s", apiUrl, apiKey);
        Map genreResponse = restTemplate.getForObject(genreUrl, Map.class);
        List<Map<String, Object>> genresList = (List<Map<String, Object>>) genreResponse.get("genres");

        Map<Integer, String> genreMap = new HashMap<>();
        for (Map<String, Object> genre : genresList) {
            genreMap.put((Integer) genre.get("id"), (String) genre.get("name"));
        }

        List<String> genreNames = new ArrayList<>();
        for (Integer id : genreIds) {
            if (genreMap.containsKey(id)) {
                genreNames.add(genreMap.get(id));
            }
        }

        String creditsUrl = String.format("%s/movie/%s/credits?api_key=%s", apiUrl, movieId, apiKey);
        Map creditsResponse = restTemplate.getForObject(creditsUrl, Map.class);
        List<Map<String, Object>> cast = (List<Map<String, Object>>) creditsResponse.get("cast");

        List<String> actors = new ArrayList<>();
        for (int i = 0; i < Math.min(5, cast.size()); i++) {
            actors.add((String) cast.get(i).get("name"));
        }

        MovieDto dto = new MovieDto();
        dto.setMovieId(String.valueOf(movie.get("id")));
        dto.setTitle((String) movie.get("title"));
        dto.setPosterUrl("https://image.tmdb.org/t/p/w500" + movie.get("poster_path"));
        dto.setOverview((String) movie.get("overview"));
        dto.setGenres(String.join(", ", genreNames));
        dto.setReleaseDate((String) movie.get("release_date"));
        dto.setUserId(userId);
        dto.setActors(String.join(", ", actors));

        // Upload to S3 and get the new URL
        String s3PosterUrl = "";
        try {
            s3PosterUrl = s3Service.uploadPoster(dto.getMovieId(), dto.getPosterUrl());
        } catch (IOException e) {
            e.printStackTrace(); // or log.error(...) — up to you
            return "Failed to upload poster to S3.";
        }


        // Save the movie to DynamoDB
        dynamoDbService.saveMovie(dto);

        // Call Lambda to enrich metadata
        lambdaService.invokeMetadataEnrichment(dto, s3PosterUrl);

        MovieDto enriched = null;
        int attempts = 0;
        int maxAttempts = 5;

        while (attempts < maxAttempts) {
            enriched = dynamoDbService.getMovieForUser(userId, dto.getMovieId());
            if (enriched.getActorClassification() != null && enriched.getThumbnailUrl() != null) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }
        return enriched != null ? "Movie added with enrichment." : "Movie added (partial).";
    }

    public List<MovieDto> getWatchlist(String userId) {
        return dynamoDbService.getMoviesForUser(userId);
    }

    public String removeMovieFromWatchlist(String movieId, String userId) {
        return dynamoDbService.deleteMovie(userId, movieId);
    }
}
