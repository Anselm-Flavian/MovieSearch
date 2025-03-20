package com.movie.search.controller;

import com.movie.search.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:3000") // Add this
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/search")
    public List<Map<String, Object>> searchMovies(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "director", required = false) String director,
            @RequestParam(value = "year", required = false) String year) {
        return movieService.searchMovies(query, genre, director, year);
    }

    @GetMapping("/{movieId}")
    public Map<String, Object> getMovieDetails(@PathVariable Integer movieId) {
        return movieService.getMovieDetails(movieId);
    }

    @RequestMapping(value = "/error")
    public String error() {
        return "Error handling";
    }
}