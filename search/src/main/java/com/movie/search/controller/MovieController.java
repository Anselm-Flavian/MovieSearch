package com.movie.search.controller;

import com.movie.search.model.SearchHistory;
import com.movie.search.model.User;
import com.movie.search.repository.SearchHistoryRepository;
import com.movie.search.repository.UserRepository;
import com.movie.search.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private static final Logger LOGGER = Logger.getLogger(MovieController.class.getName());

    @Autowired
    private MovieService movieService;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User ensureUserExists(String auth0Id) {
        if (auth0Id == null || auth0Id.isEmpty()) {
            LOGGER.warning("No auth0Id provided for user creation");
            return null;
        }
        LOGGER.info("Ensuring user exists for auth0Id=" + auth0Id);

        try {
            List<User> invalidUsers = userRepository.findByUsername("tempUsername");
            if (!invalidUsers.isEmpty()) {
                LOGGER.info("Found " + invalidUsers.size() + " invalid users with username=tempUsername for auth0Id=" + auth0Id + ", deleting...");
                for (User invalidUser : invalidUsers) {
                    if (!invalidUser.getAuth0Id().equals(auth0Id)) {
                        userRepository.delete(invalidUser);
                        LOGGER.info("Deleted invalid user with userId=" + invalidUser.getUserId() + " for auth0Id=" + invalidUser.getAuth0Id());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to clean up invalid users for auth0Id=" + auth0Id + ": " + e.getMessage(), e);
        }

        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
        if (existingUser.isPresent()) {
            LOGGER.info("Found existing user for auth0Id=" + auth0Id + ", userId=" + existingUser.get().getUserId());
            return existingUser.get();
        }

        User user = new User();
        user.setAuth0Id(auth0Id);
        String username = "user_" + UUID.randomUUID().toString().replace("-", "");
        user.setUsername(username);
        user.setEmail("user_" + UUID.randomUUID().toString().replace("-", "") + "@example.com");
        try {
            User savedUser = userRepository.save(user);
            LOGGER.info("Created new user: auth0Id=" + auth0Id + ", userId=" + savedUser.getUserId() + ", username=" + username);
            return savedUser;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create user for auth0Id=" + auth0Id + ": " + e.getMessage(), e);
            try {
                Optional<User> retryUser = userRepository.findByAuth0Id(auth0Id);
                if (retryUser.isPresent()) {
                    LOGGER.info("Retry found user for auth0Id=" + auth0Id + ", userId=" + retryUser.get().getUserId());
                    return retryUser.get();
                }
                LOGGER.warning("No user found after retry for auth0Id=" + auth0Id);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Retry user lookup failed for auth0Id=" + auth0Id + ": " + ex.getMessage(), ex);
            }
            return null;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMovies(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "director", required = false) String director,
            @RequestParam(value = "year", required = false) String year,
            Authentication authentication) {

        LOGGER.info("Authentication object: " + (authentication != null ? authentication.toString() : "null"));
        LOGGER.info("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));

        String auth0Id = (authentication != null && authentication.getPrincipal() instanceof Jwt)
                ? ((Jwt) authentication.getPrincipal()).getSubject()
                : null;
        LOGGER.info("Received search request: query=" + query + ", genre=" + genre + ", director=" + director + ", year=" + year + ", auth0Id=" + auth0Id);

        if (query == null && genre == null && director == null && year == null) {
            LOGGER.warning("No search parameters provided");
            return ResponseEntity.badRequest().body("At least one search parameter (query, genre, director, or year) must be provided");
        }

        try {
            List<Map<String, Object>> results = movieService.searchMovies(query, genre, director, year, auth0Id);
            LOGGER.info("Search completed, returning " + results.size() + " movies for query=" + query + ", genre=" + genre + ", director=" + director + ", year=" + year + ", auth0Id=" + auth0Id);
            if (results.isEmpty()) {
                LOGGER.warning("No movies returned from MovieService for query=" + query + ", genre=" + genre + ", director=" + director + ", year=" + year + ", auth0Id=" + auth0Id);
            }
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error in searchMovies for auth0Id=" + auth0Id + ": " + e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetails(@PathVariable Integer movieId) {
        LOGGER.info("Received request for movie details: movieId=" + movieId);

        if (movieId == null || movieId <= 0) {
            LOGGER.warning("Invalid movie ID: " + movieId);
            return ResponseEntity.badRequest().body("Invalid movie ID: " + movieId);
        }

        try {
            Map<String, Object> result = movieService.getMovieDetails(movieId);
            if (result == null || result.isEmpty()) {
                LOGGER.warning("No movie found for movieId=" + movieId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Movie not found");
            }
            LOGGER.info("Movie details retrieved successfully for movieId=" + movieId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getMovieDetails: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get movie details: " + e.getMessage());
        }
    }

    @PostMapping("/select")
    public ResponseEntity<?> selectMovie(
            @RequestBody Map<String, Object> movieData,
            Authentication authentication) {
        String auth0Id = (authentication != null && authentication.getPrincipal() instanceof Jwt)
                ? ((Jwt) authentication.getPrincipal()).getSubject()
                : null;
        LOGGER.info("Received select movie request: auth0Id=" + auth0Id + ", movieData=" + movieData);

        if (auth0Id == null) {
            LOGGER.warning("User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (movieData == null || !movieData.containsKey("id") || !movieData.containsKey("title")) {
            LOGGER.warning("Invalid movie data provided");
            return ResponseEntity.badRequest().body("Movie data must include id and title");
        }

        User user = ensureUserExists(auth0Id);
        if (user == null) {
            LOGGER.warning("Failed to ensure user exists for auth0Id=" + auth0Id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process user");
        }

        Integer movieId = (Integer) movieData.get("id");
        try {
            Optional<SearchHistory> existing = searchHistoryRepository.findByUserUserIdAndMovieId(user.getUserId(), movieId);
            if (existing.isPresent()) {
                LOGGER.info("Movie already in history for userId=" + user.getUserId() + ", movieId=" + movieId + ", updating timestamp");
                SearchHistory history = existing.get();
                history.setSearchTimestamp(LocalDateTime.now());
                searchHistoryRepository.save(history);
            } else {
                SearchHistory history = new SearchHistory();
                history.setUser(user);
                history.setMovieId(movieId);
                history.setTitle((String) movieData.get("title"));
                history.setPosterPath(movieData.get("poster_path") != null ? (String) movieData.get("poster_path") : null);
                history.setSearchQuery(null);
                history.setSearchTimestamp(LocalDateTime.now());
                searchHistoryRepository.save(history);
                LOGGER.info("Saved selected movie for userId=" + user.getUserId() + ", auth0Id=" + auth0Id + ", movieId=" + movieId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save selected movie for auth0Id=" + auth0Id + ": " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save selected movie: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory(Authentication authentication) {
        String auth0Id = (authentication != null && authentication.getPrincipal() instanceof Jwt)
                ? ((Jwt) authentication.getPrincipal()).getSubject()
                : null;
        LOGGER.info("Received request for user history: auth0Id=" + auth0Id);

        if (auth0Id == null) {
            LOGGER.warning("User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        try {
            User user = ensureUserExists(auth0Id);
            if (user == null) {
                LOGGER.warning("Failed to ensure user exists for auth0Id=" + auth0Id);
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<SearchHistory> history = searchHistoryRepository.findByUserUserIdOrderBySearchTimestampDesc(user.getUserId());
            Map<Integer, SearchHistory> uniqueHistory = new LinkedHashMap<>();
            for (SearchHistory h : history) {
                if (h.getMovieId() != null) {
                    uniqueHistory.putIfAbsent(h.getMovieId(), h);
                }
            }
            List<Map<String, Object>> historyMovies = uniqueHistory.values().stream()
                    .map(h -> {
                        Map<String, Object> movie = new HashMap<>();
                        movie.put("movieId", h.getMovieId());
                        movie.put("title", h.getTitle());
                        movie.put("poster_path", h.getPosterPath());
                        movie.put("searchTimestamp", h.getSearchTimestamp());
                        return movie;
                    })
                    .collect(Collectors.toList());
            LOGGER.info("Retrieved " + historyMovies.size() + " history entries for auth0Id=" + auth0Id + ", userId=" + user.getUserId());
            return ResponseEntity.ok(historyMovies);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching user history for auth0Id=" + auth0Id + ": " + e.getMessage(), e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<?> getAutocompleteSuggestions(
            @RequestParam(value = "prefix", required = false) String prefix,
            Authentication authentication) {
        LOGGER.info("Received autocomplete request: prefix=" + prefix);

        if (prefix == null || prefix.trim().isEmpty()) {
            LOGGER.warning("No prefix provided for autocomplete");
            return ResponseEntity.badRequest().body("Prefix parameter is required");
        }

        String auth0Id = (authentication != null && authentication.getPrincipal() instanceof Jwt)
                ? ((Jwt) authentication.getPrincipal()).getSubject()
                : null;
        LOGGER.info("Autocomplete request for auth0Id=" + auth0Id);

        try {
            List<String> suggestions = movieService.getAutocompleteSuggestions(prefix);
            LOGGER.info("Retrieved " + suggestions.size() + " autocomplete suggestions for prefix=" + prefix + ", auth0Id=" + auth0Id);
            if (suggestions.isEmpty()) {
                LOGGER.info("No suggestions found for prefix=" + prefix);
            }
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching autocomplete suggestions for prefix=" + prefix + ", auth0Id=" + auth0Id + ": " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch autocomplete suggestions: " + e.getMessage());
        }
    }
}