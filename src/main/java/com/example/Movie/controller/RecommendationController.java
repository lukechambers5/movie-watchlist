package com.example.Movie.controller;

import com.example.Movie.dto.MovieDto;
import com.example.Movie.service.MovieService;
import com.example.Movie.model.User;
import com.example.Movie.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/recommendation")
public class RecommendationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieService movieService;

    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggestions(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) return ResponseEntity.status(404).build();

            String userId = getCurrentUserEmail();
            List<MovieDto> watchlist = movieService.getWatchlist(userId);

            Set<String> genres = new HashSet<>();
            Set<String> actors = new HashSet<>();

            for (MovieDto movie : watchlist) {
                if (movie.getGenres() != null)
                    genres.addAll(Arrays.asList(movie.getGenres().split(",")));
                if (movie.getActors() != null)
                    actors.addAll(Arrays.asList(movie.getActors().split(",")));
            }

            List<MovieDto> suggestions = movieService.fetchRecommendations(genres, actors, watchlist);
            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // the username/email extracted from JWT by JwtAuthFilter
    }
}
