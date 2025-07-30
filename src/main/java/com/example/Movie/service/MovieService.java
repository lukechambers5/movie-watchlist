package com.example.Movie.service;

import com.example.Movie.dto.MovieDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.stream.Collectors;

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
    public List<Map<String, Object>> searchMovies(String title) {
        String url = String.format("%s/search/movie?query=%s&api_key=%s", apiUrl, title, apiKey);
        RestTemplate restTemplate = new RestTemplate();
        Map result = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> movies = (List<Map<String, Object>>) result.get("results");

        if (movies == null || movies.isEmpty()) {
            return Collections.emptyList();
        }

        // Return a simplified list (id, title, release_date, poster)
        List<Map<String, Object>> simplifiedResults = new ArrayList<>();
        for (Map<String, Object> movie : movies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", movie.get("id"));
            item.put("title", movie.get("title"));
            item.put("release_date", movie.get("release_date"));
            item.put("poster_path", movie.get("poster_path"));
            simplifiedResults.add(item);
        }

        return simplifiedResults;
    }

    public String addMovieToWatchlistById(String movieId, String userId) {
        String movieUrl = String.format("%s/movie/%s?api_key=%s", apiUrl, movieId, apiKey);
        String creditsUrl = String.format("%s/movie/%s/credits?api_key=%s", apiUrl, movieId, apiKey);
        String genreUrl = String.format("%s/genre/movie/list?api_key=%s", apiUrl, apiKey);

        RestTemplate restTemplate = new RestTemplate();

        Map movie = restTemplate.getForObject(movieUrl, Map.class);
        Map creditsResponse = restTemplate.getForObject(creditsUrl, Map.class);
        Map genreResponse = restTemplate.getForObject(genreUrl, Map.class);

        List<Map<String, Object>> genresList = (List<Map<String, Object>>) genreResponse.get("genres");
        Map<Integer, String> genreMap = new HashMap<>();
        for (Map<String, Object> genre : genresList) {
            genreMap.put((Integer) genre.get("id"), (String) genre.get("name"));
        }

        List<Map<String, Object>> genreObjects = (List<Map<String, Object>>) movie.get("genres");
        List<String> genreNames = new ArrayList<>();
        for (Map<String, Object> genre : genreObjects) {
            genreNames.add((String) genre.get("name"));
        }

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
        dto.setRating(((Number) movie.get("vote_average")).doubleValue());

        String s3PosterUrl = "";
        try {
            s3PosterUrl = s3Service.uploadPoster(dto.getMovieId(), dto.getPosterUrl());
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to upload poster to S3.";
        }

        dynamoDbService.saveMovie(dto);
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

    public List<MovieDto> fetchRecommendations(Set<String> genres, Set<String> actors, List<MovieDto> excludeList) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Convert genre names to IDs
            String genreUrl = String.format("%s/genre/movie/list?api_key=%s", apiUrl, apiKey);
            Map genreResponse = restTemplate.getForObject(genreUrl, Map.class);
            List<Map<String, Object>> genreList = (List<Map<String, Object>>) genreResponse.get("genres");

            Map<String, String> genreNameToId = new HashMap<>();
            for (Map<String, Object> genre : genreList) {
                genreNameToId.put(((String) genre.get("name")).toLowerCase(), String.valueOf(genre.get("id")));
            }

            Set<String> genreIds = new HashSet<>();
            for (String g : genres) {
                String id = genreNameToId.get(g.trim().toLowerCase());
                if (id != null) genreIds.add(id);
            }

            // Build TMDb discover URL using genre filters
            String discoverUrl = String.format("%s/discover/movie?api_key=%s&sort_by=popularity.desc", apiUrl, apiKey);
            if (!genreIds.isEmpty()) {
                discoverUrl += "&with_genres=" + String.join(",", genreIds);
            }

            Map result = restTemplate.getForObject(discoverUrl, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");

            // Exclude movies already in the user's watchlist
            Set<String> excludeIds = excludeList.stream()
                    .map(MovieDto::getMovieId)
                    .collect(Collectors.toSet());

            List<MovieDto> suggestions = new ArrayList<>();
            for (Map<String, Object> movie : results) {
                String id = String.valueOf(movie.get("id"));
                if (excludeIds.contains(id)) continue;

                MovieDto dto = new MovieDto();
                dto.setMovieId(id);
                dto.setTitle((String) movie.get("title"));
                dto.setPosterUrl("https://image.tmdb.org/t/p/w500" + movie.get("poster_path"));
                dto.setOverview((String) movie.get("overview"));
                dto.setReleaseDate((String) movie.get("release_date"));
                dto.setRating(movie.get("vote_average") instanceof Number ? ((Number) movie.get("vote_average")).doubleValue() : 0.0);
                suggestions.add(dto);
            }

            return suggestions;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();  // Return empty list on error to avoid 500
        }
    }



}
