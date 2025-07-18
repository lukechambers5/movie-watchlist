package com.example.Movie.controller;

import com.example.Movie.dto.MovieDto;
import com.example.Movie.service.MovieService;
import com.example.Movie.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private JwtUtils jwtUtils; // ✅ Inject JwtUtils

    @PostMapping
    public String addMovie(@RequestParam String title, @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromJwt(authHeader);
        return movieService.addMovieToWatchlist(title, userId);
    }

    @GetMapping
    public List<MovieDto> getWatchlist(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromJwt(authHeader);
        return movieService.getWatchlist(userId);
    }

    @DeleteMapping("/{movieId}")
    public String deleteMovie(@PathVariable String movieId, @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromJwt(authHeader);
        return movieService.removeMovieFromWatchlist(movieId, userId);
    }

    private String extractUserIdFromJwt(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtils.extractUsername(token);
    }
}
