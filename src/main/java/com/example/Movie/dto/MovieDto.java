package com.example.Movie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private String movieId;       // TMDb ID
    private String title;
    private String posterUrl;
    private String overview;
    private String genres;        // Optional: comma-separated names
    private String releaseDate;
    private String userId;        // Added after decoding JWT
    private String actors;
}
