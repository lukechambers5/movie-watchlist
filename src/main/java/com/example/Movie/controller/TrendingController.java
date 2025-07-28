package com.example.Movie.controller;

import com.example.Movie.service.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/trending")
public class TrendingController {

    @Autowired
    private TrendingService trendingService;

    @GetMapping
    public ResponseEntity<Map> getTrendingMovies() {
        return ResponseEntity.ok(trendingService.getTrendingMovies());
    }
}
