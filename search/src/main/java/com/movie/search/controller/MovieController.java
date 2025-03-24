package com.movie.search.controller;

import com.movie.search.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private static final Logger LOGGER = Logger.getLogger(MovieController.class.getName());

    @Autowired
    private MovieService movieService;

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> searchMovies(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "director", required = false) String director,
            @RequestParam(value = "year", required = false) String year) {
        LOGGER.info("Received search request: query=" + query + ", genre=" + genre + ", director=" + director + ", year=" + year);

        if (query == null && genre == null && director == null && year == null) {
            LOGGER.warning("No search parameters provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one search parameter (query, genre, director, or year) must be provided");
        }

        try {
            List<Map<String, Object>> results = movieService.searchMovies(query, genre, director, year);
            LOGGER.info("Search completed successfully, returning " + results.size() + " movies");
            return results;
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid input: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error in searchMovies: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search movies: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getMovieDetails(@PathVariable Integer movieId) {
        LOGGER.info("Received request for movie details: movieId=" + movieId);

        if (movieId == null || movieId <= 0) {
            LOGGER.warning("Invalid movie ID: " + movieId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid movie ID: " + movieId);
        }

        try {
            Map<String, Object> result = movieService.getMovieDetails(movieId);
            LOGGER.info("Movie details retrieved successfully for movieId=" + movieId);
            return result;
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid input: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error in getMovieDetails: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get movie details: " + e.getMessage(), e);
        }
    }
}