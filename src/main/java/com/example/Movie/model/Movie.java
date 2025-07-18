package com.example.Movie.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private String movieId;
    private String title;
    private String userId;
    private String posterUrl;
    private String genre;
    private String actors;
}
