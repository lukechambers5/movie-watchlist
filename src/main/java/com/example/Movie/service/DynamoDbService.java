package com.example.Movie.service;

import com.example.Movie.dto.MovieDto;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Service
public class DynamoDbService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = "CineScopeWatchlist";

    public DynamoDbService(DynamoDbClient client) {
        this.dynamoDbClient = client;
    }

    public void saveMovie(MovieDto movie) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("user_id", AttributeValue.fromS(movie.getUserId()));
        item.put("movie_id", AttributeValue.fromS(movie.getMovieId()));
        item.put("title", AttributeValue.fromS(movie.getTitle()));
        item.put("poster_url", AttributeValue.fromS(movie.getPosterUrl()));
        item.put("overview", AttributeValue.fromS(movie.getOverview()));
        item.put("genres", AttributeValue.fromS(movie.getGenres()));
        item.put("release_date", AttributeValue.fromS(movie.getReleaseDate()));
        item.put("actors", AttributeValue.fromS(movie.getActors()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public String deleteMovie(String userId, String movieId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("user_id", AttributeValue.fromS(userId));
        key.put("movie_id", AttributeValue.fromS(movieId));

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
        return "Movie deleted.";
    }

    public List<MovieDto> getMoviesForUser(String userId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":uid", AttributeValue.fromS(userId));

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("user_id = :uid")
                .expressionAttributeValues(eav)
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);
        List<MovieDto> movies = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            MovieDto movie = new MovieDto();
            movie.setMovieId(item.get("movie_id").s());
            movie.setTitle(item.get("title").s());
            movie.setPosterUrl(item.get("poster_url").s());
            movie.setOverview(item.get("overview").s());
            movie.setGenres(item.get("genres").s());
            movie.setReleaseDate(item.get("release_date").s());
            movie.setUserId(userId);
            movie.setActors(item.getOrDefault("actors", AttributeValue.fromS("")) != null
                    ? item.get("actors").s() : "");

            if (item.containsKey("thumbnail_url") && item.get("thumbnail_url").s() != null) {
                movie.setThumbnailUrl(item.get("thumbnail_url").s());
            }
            if (item.containsKey("popularity_score") && item.get("popularity_score").n() != null) {
                try {
                    movie.setPopularityScore(Double.parseDouble(item.get("popularity_score").n()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid popularity_score: " + item.get("popularity_score").n());
                }
            }
            if (item.containsKey("actor_classification") && item.get("actor_classification").s() != null) {
                movie.setActorClassification(item.get("actor_classification").s());
            }

            movies.add(movie);
        }
        return movies;
    }

    public MovieDto getMovieForUser(String userId, String movieId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("user_id", AttributeValue.fromS(userId));
        key.put("movie_id", AttributeValue.fromS(movieId));

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
        if (item == null || item.isEmpty()) return null;

        MovieDto movie = new MovieDto();
        movie.setMovieId(movieId);
        movie.setUserId(userId);
        movie.setTitle(item.get("title").s());
        movie.setPosterUrl(item.get("poster_url").s());
        movie.setOverview(item.get("overview").s());
        movie.setGenres(item.get("genres").s());
        movie.setReleaseDate(item.get("release_date").s());
        movie.setActors(item.getOrDefault("actors", AttributeValue.fromS("")).s());

        if (item.containsKey("thumbnail_url")) {
            movie.setThumbnailUrl(item.get("thumbnail_url").s());
        }
        if (item.containsKey("popularity_score")) {
            try {
                movie.setPopularityScore(Double.parseDouble(item.get("popularity_score").n()));
            } catch (NumberFormatException e) {
                movie.setPopularityScore(0.0);
            }
        }
        if (item.containsKey("actor_classification")) {
            movie.setActorClassification(item.get("actor_classification").s());
        }

        return movie;
    }

}
