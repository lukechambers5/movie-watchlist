package com.example.Movie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TrendingService {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map getTrendingMovies() {
        String url = "https://api.themoviedb.org/3/trending/movie/day?api_key=" + tmdbApiKey;
        return restTemplate.getForObject(url, Map.class);
    }
}
