package com.example.Movie.controller;

import com.example.Movie.dto.MovieDto;
import com.example.Movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/watchlist")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @PostMapping
    public String addMovie(@RequestParam String title) {
        String userId = getCurrentUserEmail();
        return movieService.addMovieToWatchlist(title, userId);
    }

    @GetMapping
    public List<MovieDto> getWatchlist() {
        String userId = getCurrentUserEmail();
        return movieService.getWatchlist(userId);
    }

    @DeleteMapping("/{movieId}")
    public String deleteMovie(@PathVariable String movieId) {
        String userId = getCurrentUserEmail();
        return movieService.removeMovieFromWatchlist(movieId, userId);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // the username/email extracted from JWT by JwtAuthFilter
    }
}
